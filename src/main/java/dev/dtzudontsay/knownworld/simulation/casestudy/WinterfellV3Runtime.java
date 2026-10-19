
package dev.dtzudontsay.knownworld.simulation.casestudy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.profile.*;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Winterfell V3 persistent NPC simulation and physical projection.
 *
 * Worldgen owns blocks. This runtime does not alter the buildings.
 * Villagers are temporary physical representations of persistent
 * simulation characters.
 *
 * Improvements:
 * - Resident import advances in small batches EVERY server tick.
 * - Physical projections update regularly and immediately when
 *   the census finishes importing.
 * - Blocked initial spawn columns can use nearby walkable ground.
 * - Dialogue actors remain paused even when projection quality
 *   and AI activity are refreshed.
 * - Existing right-click inspection and GM diagnostics remain.
 */
public final class WinterfellV3Runtime {

    public static final int FULL_BUDGET = 128;
    public static final int REDUCED_BUDGET = 128;

    private static final int FULL_RADIUS = 140;
    private static final int REDUCED_RADIUS = 260;

    private static final int IMPORT_BATCH_PER_TICK = 125;
    private static final int PROJECTION_INTERVAL_TICKS = 20;

    private static final int RESIDENTS = 5000;

    private static final int ORIGIN_X = -4_050_173;
    private static final int ORIGIN_Z = -1_976_496;

    private static final String DATA =
            "/data/knownworld/case_study/winterfell_v2.json";

    private static final String TAG = "kwf3_actor";
    private static final String ID_TAG = "kwf3_id_";

    private static final String[] VISITORS = {
            "robert_baratheon",
            "cersei_lannister",
            "jaime_lannister",
            "tyrion_lannister",
            "benjen_stark"
    };

    private record Home(
            String id,
            int x,
            int z,
            int w,
            int d
    ) { }

    private record Person(
            String id,
            String name,
            int birthYear,
            String sex,
            String homeId,
            String householdId,
            String job,
            String culture,
            String religion,
            String status,
            JsonObject raw
    ) { }

    private record Candidate(
            Person person,
            double distSq
    ) { }

    private static final Map<String, Home> HOMES =
            new HashMap<>();

    private static final List<Person> PEOPLE =
            new ArrayList<>();

    private static final Map<String, Person> BY_ID =
            new HashMap<>();

    private static final Map<String, UUID> ENTITIES =
            new HashMap<>();

    private static final Set<UUID> NEW_ENTITIES =
            new HashSet<>();

    private static boolean rosterReady;
    private static MinecraftServer server;

    private static int cursor;
    private static int ticks;
    private static int fullCount;
    private static int reducedCount;

    private static long controllerNanos;

    private static boolean projectionsEnabled = true;
    private static boolean initializeNamedPositions;
    private static boolean importModeKnown;

    private WinterfellV3Runtime() { }

    public static void register() {

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, access, environment) ->
                        dispatcher.register(
                                Commands.literal("kwinterfell3npc")
                                        .then(
                                                Commands.literal("status")
                                                        .executes(c ->
                                                                status(c.getSource()))
                                        )
                                        .then(
                                                Commands.literal("pause")
                                                        .executes(c ->
                                                                toggle(c.getSource(), false))
                                        )
                                        .then(
                                                Commands.literal("resume")
                                                        .executes(c ->
                                                                toggle(c.getSource(), true))
                                        )
                        )
        );

        ServerTickEvents.END_SERVER_TICK.register(
                WinterfellV3Runtime::tick
        );

        ServerEntityEvents.ENTITY_LOAD.register(
                (entity, world) -> {

                    // Saved physical projections are not authoritative.
                    // Discard stale entities from an earlier server run.
                    if (entity.entityTags().contains(TAG)
                            && !NEW_ENTITIES.remove(entity.getUUID())) {

                        entity.discard();
                    }
                }
        );

        UseEntityCallback.EVENT.register(
                (player, level, hand, entity, hit) -> {

                    if (hand != InteractionHand.MAIN_HAND) {
                        return InteractionResult.PASS;
                    }

                    if (level.isClientSide()) {

                        // Entity tags may not be visible on the client.
                        return entity instanceof Villager
                                && entity.getCustomName() != null
                                ? InteractionResult.SUCCESS
                                : InteractionResult.PASS;
                    }

                    if (!entity.entityTags().contains(TAG)) {
                        return InteractionResult.PASS;
                    }

                    if (player instanceof ServerPlayer sp) {
                        inspect(sp, entity);
                    }

                    return InteractionResult.SUCCESS;
                }
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {

            if (s == server) {

                ENTITIES.clear();
                NEW_ENTITIES.clear();

                server = null;
                cursor = 0;
                ticks = 0;
                fullCount = 0;
                reducedCount = 0;
            }
        });
    }

    private static boolean isKnownWorld(MinecraftServer s) {

        ServerLevel overworld =
                s.getLevel(Level.OVERWORLD);

        return overworld != null
                && overworld.getChunkSource().getGenerator()
                instanceof KnownWorldChunkGenerator;
    }

    private static void readRoster() {

        if (rosterReady) {
            return;
        }

        try (
                InputStream in =
                        WinterfellV3Runtime.class.getResourceAsStream(DATA)
        ) {

            if (in == null) {
                throw new IllegalStateException(
                        "Missing winterfell_v2.json; run build_winterfell_v2.py"
                );
            }

            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            for (JsonElement el : root.getAsJsonArray("homes")) {

                JsonObject v = el.getAsJsonObject();

                Home h = new Home(
                        string(v, "id"),
                        number(v, "x"),
                        number(v, "z"),
                        number(v, "w"),
                        number(v, "d")
                );

                if (HOMES.putIfAbsent(h.id, h) != null) {
                    throw new IllegalStateException(
                            "Duplicate home " + h.id
                    );
                }
            }

            for (JsonElement el : root.getAsJsonArray("residents")) {

                JsonObject v = el.getAsJsonObject();

                Person p = new Person(
                        string(v, "id"),
                        string(v, "name"),
                        number(v, "birthYear"),
                        string(v, "sex"),
                        string(v, "homeId"),
                        string(v, "householdId"),
                        string(v, "job"),
                        string(v, "culture"),
                        string(v, "religion"),
                        string(v, "status"),
                        v
                );

                if (!HOMES.containsKey(p.homeId)
                        || BY_ID.putIfAbsent(p.id, p) != null) {

                    throw new IllegalStateException(
                            "Duplicate or homeless resident " + p.id
                    );
                }

                PEOPLE.add(p);
            }

            if (PEOPLE.size() != RESIDENTS
                    || HOMES.size() != 1136) {

                throw new IllegalStateException(
                        "Expected 5000 residents / 1136 households"
                );
            }

            for (String guest : VISITORS) {

                BY_ID.put(
                        guest,
                        new Person(
                                guest,
                                guest.replace('_', ' '),
                                -1,
                                "UNKNOWN",
                                "castle_home_0001",
                                "visiting_royal_party",
                                "VISITOR",
                                "",
                                "",
                                "SCENARIO_VISITOR",
                                new JsonObject()
                        )
                );
            }

            rosterReady = true;

            KnownWorld.LOGGER.info(
                    "Winterfell V3 NPC runtime: {} residents / {} occupied homes",
                    PEOPLE.size(),
                    HOMES.size()
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Winterfell V3 roster validation failed",
                    e
            );
        }
    }

    private static String string(JsonObject j, String k) {

        JsonElement e = j.get(k);

        return e == null || e.isJsonNull()
                ? ""
                : e.getAsString();
    }

    private static int number(JsonObject j, String k) {
        return j.get(k).getAsInt();
    }

    private static double numberOr(
            JsonObject j,
            String k,
            double fallback
    ) {

        JsonElement e = j == null ? null : j.get(k);

        return e == null || e.isJsonNull()
                ? fallback
                : e.getAsDouble();
    }

    /**
     * Stark residents belong to the V3 Great Keep rather than
     * the old cottage plot used during authoring.
     */
    private static double doorX(Home home) {

        return ORIGIN_X
                + ("castle_home_0001".equals(home.id)
                ? 17.5
                : home.x + home.w / 2.0);
    }

    private static double doorZ(Home home) {

        return ORIGIN_Z
                + ("castle_home_0001".equals(home.id)
                ? 54.5
                : home.z + home.d + 2.5);
    }

    private static int yFromPristineMap(double x, double z) {

        return (int) Math.round(
                GeographicDataManager.getInstance()
                        .sample(
                                WorldProjection.fromMinecraft(x, z)
                        )
                        .elevationMetres()
        ) + 1;
    }

    private static void tick(MinecraftServer s) {

        if (server != s) {

            server = s;

            cursor = 0;
            ticks = 0;
            fullCount = 0;
            reducedCount = 0;

            ENTITIES.clear();
            NEW_ENTITIES.clear();

            projectionsEnabled = true;
            importModeKnown = false;
            initializeNamedPositions = false;
        }

        ticks++;

        if (!isKnownWorld(s)) {
            return;
        }

        /*
         * On reload the previous implementation advanced the roster
         * just once per second. At 125 people per second, 5000
         * entries required approximately 40 seconds.
         *
         * Import now advances every server tick, in bounded batches.
         */
        long start = System.nanoTime();

        try {

            readRoster();

            NpcSimulation sim = NpcSimulation.getNullable();

            if (sim == null) {
                return;
            }

            if (!importModeKnown) {

                initializeNamedPositions =
                        sim.authoredIds()
                                .npcs()
                                .keySet()
                                .stream()
                                .noneMatch(k ->
                                        k.startsWith("wf2_gen_")
                                                || k.startsWith("wf2_book_")
                                );

                importModeKnown = true;
            }

            boolean finishedImportThisTick = false;

            if (cursor < PEOPLE.size()) {

                importBatch(sim, IMPORT_BATCH_PER_TICK);

                if (cursor == PEOPLE.size()) {

                    applyModeledParentage(sim);
                    sim.save();

                    finishedImportThisTick = true;
                }
            }

            /*
             * Do not repeatedly erase projections while the roster
             * is still importing. Wait until identity registration
             * is complete, then project immediately.
             */
            if (cursor == PEOPLE.size()) {

                if (!projectionsEnabled) {

                    if (ticks % PROJECTION_INTERVAL_TICKS == 0) {
                        clear(s);
                    }

                } else if (finishedImportThisTick
                        || ticks % PROJECTION_INTERVAL_TICKS == 0) {

                    project(s, sim);
                }
            }

        } catch (RuntimeException e) {

            KnownWorld.LOGGER.error(
                    "Winterfell V3 NPC runtime stopped after an error; buildings are unaffected",
                    e
            );

            projectionsEnabled = false;

        } finally {

            controllerNanos = System.nanoTime() - start;
        }
    }

    private static void importBatch(
            NpcSimulation sim,
            int max
    ) {

        int count = 0;
        int added = 0;

        while (cursor < PEOPLE.size() && count++ < max) {

            Person p = PEOPLE.get(cursor++);

            NpcId id =
                    sim.authoredIds()
                            .findNpc(p.id)
                            .orElse(null);

            Home h = HOMES.get(p.homeId);

            double x = doorX(h);
            double z = doorZ(h);

            if (id == null) {

                JsonObject traits =
                        p.raw.getAsJsonObject("personality");

                NpcPersonality personality = new NpcPersonality(
                        numberOr(traits, "courage", 0),
                        numberOr(traits, "ambition", 0),
                        numberOr(traits, "compassion", 0),
                        numberOr(traits, "honor", 0),
                        numberOr(traits, "patience", 0),
                        numberOr(traits, "sociability", 0)
                );

                String[] names = p.name.split(" ", 2);

                NpcState n = sim.registry().create(
                        names[0],
                        names.length > 1 ? names[1] : "",
                        "FEMALE".equals(p.sex)
                                ? NpcSex.FEMALE
                                : NpcSex.MALE,
                        p.birthYear,
                        SimulationPosition.overworld(
                                x,
                                yFromPristineMap(x, z),
                                z
                        ),
                        personality
                );

                id = n.id();

                sim.authoredIds().registerNpc(p.id, id);

                CharacterProfile cp =
                        sim.profiles().getOrCreate(id);

                cp.setCulture(p.culture);
                cp.setReligion(p.religion);
                cp.setEducation("northern_household");

                JsonArray tags = p.raw.getAsJsonArray("traits");

                if (tags != null) {

                    for (JsonElement e : tags) {
                        cp.addTrait(e.getAsString());
                    }
                }

                if (!Set.of(
                        "CHILD",
                        "DEPENDENT",
                        "WARD",
                        "LORD",
                        "LADY"
                ).contains(p.job)) {

                    cp.addOccupation(
                            p.job.toLowerCase(Locale.ROOT)
                    );
                }

                JsonObject skills =
                        p.raw.getAsJsonObject("skills");

                if (skills != null) {

                    for (var e : skills.entrySet()) {

                        try {

                            cp.setSkill(
                                    CharacterSkill.valueOf(
                                            e.getKey().toUpperCase(Locale.ROOT)
                                    ),
                                    e.getValue().getAsDouble()
                            );

                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }

                Random random =
                        new Random(2985000L ^ p.id.hashCode());

                for (CharacterAptitude a
                        : CharacterAptitude.values()) {

                    cp.setAptitude(
                            a,
                            .25 + random.nextDouble() * .52
                    );
                }

                for (CharacterDisposition d
                        : CharacterDisposition.values()) {

                    cp.setDisposition(
                            d,
                            random.nextDouble() * 1.2 - .6
                    );
                }

                for (CharacterValue v
                        : CharacterValue.values()) {

                    cp.setCharacterValue(
                            v,
                            random.nextDouble() * 1.1 - .55
                    );
                }

                for (CharacterSocialNorm v
                        : CharacterSocialNorm.values()) {

                    cp.setSocialNorm(
                            v,
                            random.nextDouble() * 1.1 - .55
                    );
                }

                added++;
            }

            if (!"AUTHORED_EXISTING".equals(p.status)) {

                OrganizationId hid =
                        sim.authoredIds()
                                .findOrganization(p.householdId)
                                .orElse(null);

                if (hid == null) {

                    Organization household =
                            sim.organizations().create(
                                    "Winterfell: " + p.householdId,
                                    OrganizationType.HOUSEHOLD,
                                    null
                            );

                    hid = household.id();

                    sim.authoredIds().registerOrganization(
                            p.householdId,
                            hid
                    );
                }

                sim.affiliations().setHousehold(id, hid);

                var residence =
                        sim.authoredIds().findSettlement("winterfell");

                if (residence.isPresent()) {

                    sim.affiliations().setResidence(
                            id,
                            residence.get()
                    );
                }

            } else if (initializeNamedPositions) {

                sim.registry().move(
                        id,
                        SimulationPosition.overworld(
                                x,
                                yFromPristineMap(x, z),
                                z
                        )
                );
            }
        }

        // Existing identities and existing profiles are preserved.
        if (cursor % 1000 == 0 || cursor == RESIDENTS) {

            if (added > 0) {
                sim.save();
            }

            KnownWorld.LOGGER.info(
                    "Winterfell V3 NPC import {}/{}",
                    cursor,
                    RESIDENTS
            );
        }
    }

    private static void applyModeledParentage(NpcSimulation sim) {

        for (Person child : PEOPLE) {

            if (!"GENERATED_MOD_CALIBRATION".equals(child.status)) {
                continue;
            }

            String mother =
                    string(child.raw, "motherId");

            String father =
                    string(child.raw, "fatherId");

            if (mother.isBlank() && father.isBlank()) {
                continue;
            }

            NpcId c =
                    sim.authoredIds()
                            .findNpc(child.id)
                            .orElse(null);

            if (c == null
                    || sim.genealogy().parentsOf(c).isPresent()) {
                continue;
            }

            NpcId m = mother.isBlank()
                    ? null
                    : sim.authoredIds().findNpc(mother).orElse(null);

            NpcId f = father.isBlank()
                    ? null
                    : sim.authoredIds().findNpc(father).orElse(null);

            if (m != null || f != null) {

                sim.genealogy().registerBirth(
                        c,
                        m,
                        f,
                        0L
                );
            }
        }
    }

    private static double nearbyDistanceSq(
            MinecraftServer s,
            Home h
    ) {

        double x = doorX(h);
        double z = doorZ(h);

        double closest = Double.POSITIVE_INFINITY;

        for (ServerPlayer player
                : s.getPlayerList().getPlayers()) {

            if (!player.level().dimension().equals(Level.OVERWORLD)) {
                continue;
            }

            double dx = player.getX() - x;
            double dz = player.getZ() - z;

            closest = Math.min(
                    closest,
                    dx * dx + dz * dz
            );
        }

        return closest;
    }

    /**
     * Find a physically usable point near the residence entrance.
     *
     * A single authored doorway position can be obstructed by
     * decorations or building geometry. Searching a small area
     * prevents that from silently suppressing the NPC projection.
     *
     * Only positions in currently loaded chunks are checked.
     * No blocks are changed by this method.
     */
    private static BlockPos findSpawn(
            ServerLevel level,
            int baseX,
            int baseZ
    ) {

        for (int radius = 0; radius <= 3; radius++) {

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    if (radius > 0
                            && Math.max(
                            Math.abs(dx),
                            Math.abs(dz)
                    ) != radius) {
                        continue;
                    }

                    int x = baseX + dx;
                    int z = baseZ + dz;

                    if (!level.isLoaded(new BlockPos(x, 64, z))) {
                        continue;
                    }

                    int y = level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

                    if (y < level.getMinY() + 2
                            || y >= level.getMaxY() - 3) {
                        continue;
                    }

                    BlockPos feet = new BlockPos(x, y, z);
                    BlockPos floor = feet.below();
                    BlockPos head = feet.above();

                    var ground = level.getBlockState(floor);
                    var footState = level.getBlockState(feet);
                    var headState = level.getBlockState(head);

                    if (ground.getCollisionShape(level, floor).isEmpty()
                            || !ground.getFluidState().isEmpty()) {
                        continue;
                    }

                    if (!footState.getCollisionShape(level, feet).isEmpty()
                            || !footState.getFluidState().isEmpty()) {
                        continue;
                    }

                    if (!headState.getCollisionShape(level, head).isEmpty()
                            || !headState.getFluidState().isEmpty()) {
                        continue;
                    }

                    return feet;
                }
            }
        }

        return null;
    }

    private static void project(
            MinecraftServer s,
            NpcSimulation sim
    ) {

        ServerLevel level = s.getLevel(Level.OVERWORLD);

        if (level == null) {
            return;
        }

        List<Candidate> candidates = new ArrayList<>();

        for (Person p : PEOPLE) {

            Home h = HOMES.get(p.homeId);

            double d = nearbyDistanceSq(s, h);

            if (d < (double) REDUCED_RADIUS * REDUCED_RADIUS) {

                candidates.add(
                        new Candidate(p, d)
                );
            }
        }

        for (String name : VISITORS) {

            Person p = BY_ID.get(name);
            Home h = HOMES.get(p.homeId);

            double d = nearbyDistanceSq(s, h);

            if (d < (double) REDUCED_RADIUS * REDUCED_RADIUS) {

                candidates.add(
                        new Candidate(p, d)
                );
            }
        }

        candidates.sort(
                Comparator.comparingDouble(Candidate::distSq)
        );

        Set<String> wanted = new HashSet<>();

        int full = 0;
        int reduced = 0;

        int campaignYear = sim.campaignCalendar().year();

        for (Candidate candidate : candidates) {

            Person p = candidate.person();

            NpcId npcId =
                    sim.authoredIds()
                            .findNpc(p.id)
                            .orElse(null);

            if (npcId == null) {
                continue;
            }

            NpcState npc =
                    sim.registry().find(npcId).orElse(null);

            if (npc == null || !npc.isAlive()) {
                continue;
            }

            Home h = HOMES.get(p.homeId);

            int x = (int) Math.floor(doorX(h));
            int z = (int) Math.floor(doorZ(h));

            // Do not generate distant chunks merely to spawn NPCs.
            if (!level.isLoaded(new BlockPos(x, 64, z))) {
                continue;
            }

            boolean detailed =
                    candidate.distSq()
                            <= (double) FULL_RADIUS * FULL_RADIUS
                            && full < FULL_BUDGET;

            boolean simplified =
                    !detailed
                            && reduced < REDUCED_BUDGET;

            if (!detailed && !simplified) {
                continue;
            }

            UUID uuid = ENTITIES.get(p.id);

            Entity existing =
                    uuid == null
                            ? null
                            : level.getEntity(uuid);

            int age = Math.max(
                    0,
                    campaignYear - npc.identity().birthYear()
            );

            if (existing instanceof Villager mob) {

                // A conversation actor stays paused even if the
                // ordinary full/reduced projection tier changes.
                mob.setNoAi(
                        !detailed
                                || WinterfellConversationPackets
                                .isConversationActor(mob.getUUID())
                );

                mob.setBaby(age < 16);

                wanted.add(p.id);

                if (detailed) {
                    full++;
                } else {
                    reduced++;
                }

                continue;
            }

            ENTITIES.remove(p.id);

            BlockPos spawn = findSpawn(level, x, z);

            if (spawn == null) {
                continue;
            }

            Villager mob =
                    new Villager(EntityTypes.VILLAGER, level);

            mob.snapTo(
                    spawn.getX() + 0.5,
                    spawn.getY(),
                    spawn.getZ() + 0.5,
                    0.0F,
                    0.0F
            );

            mob.setCustomName(
                    Component.literal(displayName(p, npc))
            );

            mob.setCustomNameVisible(false);
            mob.setBaby(age < 16);
            mob.setPersistenceRequired();
            mob.setNoAi(!detailed);

            mob.addTag(TAG);
            mob.addTag(ID_TAG + p.id);

            NEW_ENTITIES.add(mob.getUUID());

            if (!level.addFreshEntity(mob)) {

                NEW_ENTITIES.remove(mob.getUUID());
                continue;
            }

            ENTITIES.put(p.id, mob.getUUID());

            wanted.add(p.id);

            if (detailed) {
                full++;
            } else {
                reduced++;
            }
        }

        // Projections that are no longer near an active player are removed.
        for (
                Iterator<Map.Entry<String, UUID>> it =
                ENTITIES.entrySet().iterator();
                it.hasNext();
        ) {

            var entry = it.next();

            if (wanted.contains(entry.getKey())) {
                continue;
            }

            Entity e = level.getEntity(entry.getValue());

            if (e != null) {
                e.discard();
            }

            it.remove();
        }

        fullCount = 0;
        reducedCount = 0;

        for (UUID uuid : ENTITIES.values()) {

            Entity entity = level.getEntity(uuid);

            if (entity instanceof Villager villager) {

                if (villager.isNoAi()) {
                    reducedCount++;
                } else {
                    fullCount++;
                }
            }
        }
    }

    /** Read-only character role lookup for dialogue. */
    public static String dialogueRole(String authoredId) {

        if (!rosterReady) {
            readRoster();
        }

        Person person = BY_ID.get(authoredId);

        return person == null ? "" : person.job;
    }

    /** Read-only household lookup for dialogue. */
    public static String dialogueHousehold(String authoredId) {

        if (!rosterReady) {
            readRoster();
        }

        Person person = BY_ID.get(authoredId);

        return person == null
                ? ""
                : publicHouseholdLabel(person);
    }

    private static String displayName(
            Person p,
            NpcState state
    ) {

        if (!"SCENARIO_VISITOR".equals(p.status)) {
            return p.name;
        }

        return state.identity().givenName()
                + " "
                + state.identity().familyName();
    }

    /**
     * Existing right-click character inspection.
     * Sends the complete authorized character sheet data to its UI.
     */
    private static void inspect(
            ServerPlayer player,
            Entity entity
    ) {

        try {

            if (!rosterReady) {
                readRoster();
            }

            String authored = null;

            for (String tag : entity.entityTags()) {

                if (tag.startsWith(ID_TAG)) {

                    authored = tag.substring(ID_TAG.length());
                    break;
                }
            }

            if (authored == null
                    || !BY_ID.containsKey(authored)) {

                player.sendSystemMessage(Component.literal(
                        "[Winterfell] No person record for this actor."
                ));

                return;
            }

            NpcSimulation sim = NpcSimulation.getNullable();

            NpcId npcId = sim == null
                    ? null
                    : sim.authoredIds()
                    .findNpc(authored)
                    .orElse(null);

            if (npcId == null) {

                player.sendSystemMessage(Component.literal(
                        "[Winterfell] Actor has no persisted simulation identity: "
                                + authored
                ));

                return;
            }

            NpcState npc =
                    sim.registry().find(npcId).orElse(null);

            if (npc == null) {

                player.sendSystemMessage(Component.literal(
                        "[Winterfell] Missing NPC " + npcId
                ));

                return;
            }

            CharacterProfile profile =
                    sim.profiles().getOrCreate(npcId);

            Person p = BY_ID.get(authored);

            JsonObject sheet = new JsonObject();

            sheet.addProperty("name", displayName(p, npc));
            sheet.addProperty("id", authored);
            sheet.addProperty("runtimeId", npcId.toString());

            sheet.addProperty(
                    "age",
                    Math.max(
                            0,
                            sim.campaignCalendar().year()
                                    - npc.identity().birthYear()
                    )
            );

            sheet.addProperty("sex", npc.identity().sex().name());
            sheet.addProperty("occupation", p.job);
            sheet.addProperty("culture", profile.culture());
            sheet.addProperty("religion", profile.religion());
            sheet.addProperty("household", p.householdId);
            sheet.addProperty("residence", p.homeId);
            sheet.addProperty("workplace", string(p.raw, "workId"));
            sheet.addProperty("lord", "Eddard Stark");
            sheet.addProperty("status", p.status);
            sheet.addProperty("reputation", profile.reputation());
            sheet.addProperty("wealth", profile.wealth());
            sheet.addProperty("socialStatus", profile.socialStatus());

            JsonArray traits = new JsonArray();

            for (String trait : profile.traits()) {
                traits.add(trait);
            }

            sheet.add("traits", traits);

            JsonObject skills = new JsonObject();

            for (var e : profile.skills().entrySet()) {

                skills.addProperty(
                        e.getKey().name().toLowerCase(Locale.ROOT),
                        e.getValue()
                );
            }

            sheet.add("skills", skills);

            JsonObject personality = new JsonObject();

            personality.addProperty(
                    "courage",
                    npc.personality().courage()
            );

            personality.addProperty(
                    "ambition",
                    npc.personality().ambition()
            );

            personality.addProperty(
                    "compassion",
                    npc.personality().compassion()
            );

            personality.addProperty(
                    "honor",
                    npc.personality().honor()
            );

            personality.addProperty(
                    "patience",
                    npc.personality().patience()
            );

            personality.addProperty(
                    "sociability",
                    npc.personality().sociability()
            );

            sheet.add("personality", personality);

            sheet.addProperty("education", profile.education());
            sheet.addProperty("birthplace", profile.birthplaceLocationId());
            sheet.addProperty("upbringing", profile.upbringingLocationId());
            sheet.addProperty("health", profile.healthState().name());
            sheet.addProperty("legalStatus", profile.legalStatus().name());
            sheet.addProperty("appearance", profile.appearanceDescription());
            sheet.addProperty("hair", profile.hairDescription());
            sheet.addProperty("eyes", profile.eyeDescription());
            sheet.addProperty("build", profile.buildDescription());

            sheet.addProperty(
                    "householdLabel",
                    publicHouseholdLabel(p)
            );

            sheet.addProperty(
                    "homeLabel",
                    publicHomeLabel(p)
            );

            sheet.addProperty(
                    "workplaceLabel",
                    publicWorkplaceLabel(p)
            );

            sheet.addProperty(
                    "wealth",
                    String.format(
                            Locale.ROOT,
                            "%.2f",
                            profile.wealth()
                    )
            );

            sheet.addProperty(
                    "socialStatus",
                    String.format(
                            Locale.ROOT,
                            "%.2f",
                            profile.socialStatus()
                    )
            );

            sheet.addProperty(
                    "reputation",
                    String.format(
                            Locale.ROOT,
                            "%.2f",
                            profile.reputation()
                    )
            );

            jsonScores(sheet, "aptitudes", profile.aptitudes());
            jsonScores(sheet, "dispositions", profile.dispositions());
            jsonScores(sheet, "characterValues", profile.characterValues());
            jsonScores(sheet, "socialNorms", profile.socialNorms());
            jsonScores(sheet, "politics", profile.politicalPreferences());
            jsonScores(sheet, "languages", profile.languages());
            jsonScores(sheet, "customValues", profile.values());

            jsonStrings(sheet, "occupations", profile.occupations());
            jsonStrings(sheet, "offices", profile.offices());
            jsonStrings(sheet, "courtRoles", profile.courtRoles());
            jsonStrings(sheet, "militaryRoles", profile.militaryRoles());
            jsonStrings(sheet, "combatSpecialties", profile.combatSpecialties());
            jsonStrings(sheet, "aliases", profile.aliases());
            jsonStrings(sheet, "motivations", profile.motivations());
            jsonStrings(sheet, "goals", profile.goals());
            jsonStrings(sheet, "fears", profile.fears());
            jsonStrings(sheet, "desires", profile.desires());

            jsonStringMap(
                    sheet,
                    "publicFacts",
                    profile.publicFacts()
            );

            if (profile.dialoguePersona() != null) {

                JsonObject persona = new JsonObject();

                persona.addProperty(
                        "formality",
                        profile.dialoguePersona().formality()
                );

                persona.addProperty(
                        "verbosity",
                        profile.dialoguePersona().verbosity()
                );

                persona.addProperty(
                        "warmth",
                        profile.dialoguePersona().warmth()
                );

                persona.addProperty(
                        "directness",
                        profile.dialoguePersona().directness()
                );

                sheet.add("dialogueStyle", persona);
            }

            boolean gm =
                    player.isCreative()
                            && net.minecraft.commands.Commands
                            .LEVEL_GAMEMASTERS
                            .check(
                                    player.createCommandSourceStack()
                                            .permissions()
                            );

            sheet.addProperty("gamemaster", gm);

            /*
             * Secrets and private facts are hidden from ordinary players.
             * GM inspection remains permission-gated.
             */
            if (gm) {

                sheet.addProperty(
                        "orientation",
                        profile.orientation().name()
                );

                sheet.addProperty(
                        "dialoguePreferredAddress",
                        profile.dialoguePersona().preferredAddress()
                );

                sheet.addProperty(
                        "dialogueGuidance",
                        profile.dialoguePersona().guidance()
                );

                jsonStrings(
                        sheet,
                        "secrets",
                        profile.secrets()
                );

                jsonStrings(
                        sheet,
                        "knownSecrets",
                        profile.knownSecrets()
                );

                jsonStringMap(
                        sheet,
                        "privateFacts",
                        profile.privateFacts()
                );
            }

            ServerPlayNetworking.send(
                    player,
                    new KnownWorldUiPackets.CharacterSheet(
                            sheet.toString()
                    )
            );

        } catch (Exception e) {

            KnownWorld.LOGGER.error(
                    "Cannot inspect Winterfell NPC",
                    e
            );

            player.sendSystemMessage(Component.literal(
                    "[Winterfell] Unable to open this character sheet; check latest.log."
            ));
        }
    }

    private static void jsonScores(
            JsonObject target,
            String key,
            Map<?, Double> scores
    ) {

        JsonObject object = new JsonObject();

        for (var entry : scores.entrySet()) {

            object.addProperty(
                    entry.getKey().toString().toLowerCase(Locale.ROOT),
                    entry.getValue()
            );
        }

        target.add(key, object);
    }

    private static void jsonStrings(
            JsonObject target,
            String key,
            Collection<String> values
    ) {

        JsonArray array = new JsonArray();

        for (String value : values) {
            array.add(value);
        }

        target.add(key, array);
    }

    private static void jsonStringMap(
            JsonObject target,
            String key,
            Map<String, String> values
    ) {

        JsonObject object = new JsonObject();

        for (var entry : values.entrySet()) {

            object.addProperty(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        target.add(key, object);
    }

    private static String publicHouseholdLabel(Person p) {

        if ("castle_home_0001".equals(p.homeId)) {
            return "House Stark • Winterfell";
        }

        String[] parts = p.name.split(" ");

        String surname = parts.length > 1
                ? parts[parts.length - 1]
                : "Northern";

        return surname + " household";
    }

    private static String publicHomeLabel(Person p) {

        if ("castle_home_0001".equals(p.homeId)) {
            return "Great Keep, Winterfell";
        }

        String settlement = p.homeId;

        if (settlement.startsWith("wintertown_")) {
            return "Winter Town residence";
        }

        if (settlement.startsWith("village5_")) {
            return "Village residence near Winterfell";
        }

        if (settlement.startsWith("hamlet_")) {
            return "Rural homestead near Winterfell";
        }

        if (settlement.startsWith("castle_")) {
            return "Winterfell castle ward";
        }

        return "Residence near Winterfell";
    }

    private static String publicWorkplaceLabel(Person p) {

        String work = string(p.raw, "workId");

        if (work.isBlank()) {
            return "Not assigned";
        }

        if ("castle_keep".equals(work)) {
            return "Winterfell castle";
        }

        String job = p.job
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ');

        return job.substring(0, 1).toUpperCase(Locale.ROOT)
                + job.substring(1)
                + " workplace";
    }

    private static void clear(MinecraftServer s) {

        ServerLevel level = s.getLevel(Level.OVERWORLD);

        if (level != null) {

            for (UUID uuid : ENTITIES.values()) {

                Entity entity = level.getEntity(uuid);

                if (entity != null) {
                    entity.discard();
                }
            }
        }

        ENTITIES.clear();

        fullCount = 0;
        reducedCount = 0;
    }

    private static int status(
            net.minecraft.commands.CommandSourceStack src
    ) {

        int registered = 0;

        NpcSimulation sim = NpcSimulation.getNullable();

        if (sim != null && rosterReady) {

            for (Person p : PEOPLE) {

                if (sim.authoredIds()
                        .findNpc(p.id)
                        .isPresent()) {

                    registered++;
                }
            }
        }

        final int actual = registered;

        src.sendSuccess(
                () -> Component.literal(
                        "[Winterfell V3 NPC] "
                                + actual
                                + "/5000 persisted IDs"
                                + " | imported this session "
                                + cursor
                                + "/5000"
                                + " | physical full "
                                + fullCount
                                + "/128 reduced "
                                + reducedCount
                                + "/128"
                                + " | controller "
                                + String.format(
                                Locale.ROOT,
                                "%.2f",
                                controllerNanos / 1_000_000.0
                        )
                                + " ms"
                                + " | projections "
                                + (projectionsEnabled
                                ? "ON"
                                : "PAUSED")
                ),
                false
        );

        return 1;
    }

    private static int toggle(
            net.minecraft.commands.CommandSourceStack src,
            boolean enabled
    ) {

        projectionsEnabled = enabled;

        src.sendSuccess(
                () -> Component.literal(
                        "[Winterfell V3 NPC] Projections "
                                + (enabled
                                ? "resumed"
                                : "paused")
                ),
                false
        );

        return 1;
    }
}

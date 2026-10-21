package dev.dtzudontsay.knownworld.simulation.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.KnownWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * C1 blueprint/demand registry. Read-only by design: never touches chunk blocks,
 * saved NPCs, family identities, existing Winterfell, or the active job roster.
 */
public final class KnownWorldSettlementPresets implements ModInitializer {
    private static final String RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/settlement_presets_c1_v1.json";

    public record Site(String siteId, String name, String tier, String archetype,
                       String mode, String palette, int anchorX, int anchorZ,
                       int planningPopulation, int planningWorkforce,
                       int householdDemand, int housingPlots,
                       int housingBacklog, boolean physicalConstructionEnabled,
                       List<String> markerIds, List<String> politicalHoldingIds,
                       Map<String, Integer> workplaceCounts) {
        public Site {
            markerIds = List.copyOf(markerIds);
            politicalHoldingIds = List.copyOf(politicalHoldingIds);
            workplaceCounts = Map.copyOf(workplaceCounts);
        }
    }

    public record Holding(String id, String name, String tier, String archetype,
                          String geoId, List<String> siteIds, String ownerId,
                          String holderNpcId, boolean unplacedSeat) {
        public Holding {
            siteIds = List.copyOf(siteIds);
        }
    }

    public record Workplace(String name, int maxStaff, int width, int depth,
                            boolean requiresShore, boolean requiresMountain,
                            boolean requiresPolicy) { }

    public record Index(Map<String, Site> sites, Map<String, Holding> holdings,
                        Map<String, String> markerToSite,
                        Map<String, Workplace> workplaces) { }

    private static volatile Index cached;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("kwpresets");
            root.then(Commands.literal("status")
                    .executes(context -> status(context.getSource())));
            root.then(Commands.literal("marker")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(context -> marker(context.getSource(),
                                    StringArgumentType.getString(context, "id")))));
            root.then(Commands.literal("site")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(context -> site(context.getSource(),
                                    StringArgumentType.getString(context, "id")))));
            root.then(Commands.literal("holding")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(context -> holding(context.getSource(),
                                    StringArgumentType.getString(context, "id")))));
            root.then(Commands.literal("workplace")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(context -> workplace(context.getSource(),
                                    StringArgumentType.getString(context, "id")))));
            dispatcher.register(root);
        });
        KnownWorld.LOGGER.info("C1 settlement blueprints registered; no site worldgen activated");
    }

    public static Index index() {
        Index v = cached;
        if (v != null) return v;
        synchronized (KnownWorldSettlementPresets.class) {
            if (cached == null) cached = read();
            return cached;
        }
    }

    public static Optional<Site> findSite(String id) {
        return Optional.ofNullable(index().sites().get(id));
    }

    public static Optional<Site> findMarker(String markerId) {
        String siteId = index().markerToSite().get(markerId);
        return siteId == null ? Optional.empty() : findSite(siteId);
    }

    public static Optional<Holding> findHolding(String id) {
        return Optional.ofNullable(index().holdings().get(id));
    }

    private static int status(CommandSourceStack out) {
        Index i = index();
        Map<String, Long> counts = i.sites().values().stream()
                .collect(Collectors.groupingBy(Site::tier, Collectors.counting()));
        say(out, "[C1 Presets] markers=" + i.markerToSite().size()
                + " | physical sites=" + i.sites().size()
                + " | political holdings=" + i.holdings().size()
                + " | types=" + counts
                + " | workplace kinds=" + i.workplaces().size()
                + " | construction=OFF");
        return 1;
    }

    private static int marker(CommandSourceStack out, String markerId) {
        Optional<Site> found = findMarker(markerId);
        if (found.isEmpty()) {
            out.sendFailure(Component.literal("Unknown marker: " + markerId));
            return 0;
        }
        return showSite(out, found.get());
    }

    private static int site(CommandSourceStack out, String siteId) {
        Optional<Site> found = findSite(siteId);
        if (found.isEmpty()) {
            out.sendFailure(Component.literal("Unknown physical site: " + siteId));
            return 0;
        }
        return showSite(out, found.get());
    }

    private static int showSite(CommandSourceStack out, Site s) {
        say(out, "[C1 Site] " + s.name() + " (" + s.siteId() + ")"
                + " | " + s.tier() + "/" + s.archetype()
                + " | mode=" + s.mode()
                + " | anchor X=" + s.anchorX() + " Z=" + s.anchorZ());
        say(out, "Markers=" + s.markerIds()
                + " | palette=" + s.palette()
                + " | political holdings=" + s.politicalHoldingIds());
        say(out, "PLANNING ONLY: residents=" + s.planningPopulation()
                + " | worker demand=" + s.planningWorkforce()
                + " | homes needed=" + s.householdDemand()
                + " | first-phase housing plots=" + s.housingPlots()
                + " | unmet housing plots=" + s.housingBacklog()
                + " | worldgen OFF");
        String jobs = s.workplaceCounts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(12)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
        say(out, "Provisional workplace demand: " + (jobs.isEmpty() ? "none" : jobs)
                + (s.workplaceCounts().size() > 12 ? " ... (see JSON)" : ""));
        return 1;
    }

    private static int holding(CommandSourceStack out, String id) {
        Holding h = index().holdings().get(id);
        if (h == null) {
            out.sendFailure(Component.literal("Unknown holding: " + id));
            return 0;
        }
        say(out, "[C1 Holding] " + h.name() + " " + h.id() + " | "
                + h.tier() + "/" + h.archetype()
                + " | geo=" + h.geoId()
                + " | site IDs=" + h.siteIds()
                + " | unplaced seat=" + h.unplacedSeat());
        say(out, "Owner dynasty=" + h.ownerId() + " | individual holder="
                + h.holderNpcId() + " | no extra population or house placement");
        return 1;
    }

    private static int workplace(CommandSourceStack out, String id) {
        Workplace w = index().workplaces().get(id.toUpperCase(java.util.Locale.ROOT));
        if (w == null) {
            out.sendFailure(Component.literal("Unknown workplace archetype: " + id));
            return 0;
        }
        say(out, "[C1 Workplace] " + w.name() + " | max staff=" + w.maxStaff()
                + " | planned footprint=" + w.width() + "x" + w.depth()
                + " | needs shoreline=" + w.requiresShore()
                + " | needs mountain=" + w.requiresMountain()
                + " | requires policy=" + w.requiresPolicy());
        return 1;
    }

    private static void say(CommandSourceStack out, String message) {
        out.sendSuccess(() -> Component.literal(message), false);
    }

    private static Index read() {
        try (InputStream input = KnownWorldSettlementPresets.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing C1 generated resource: "
                    + RESOURCE + "; run py tools/geodata/build_c1_settlement_presets.py");
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != 1
                    || !root.getAsJsonObject("policy").get("noWorldgenActivation").getAsBoolean())
                throw new IllegalStateException("Unsafe/unknown C1 preset resource");
            JsonObject count = root.getAsJsonObject("summary");
            if (count.get("markerCount").getAsInt() != 253 || count.get("holdingCount").getAsInt() != 259)
                throw new IllegalStateException("Incomplete C1 registry: expected 253 markers, 259 holdings");

            Map<String, Site> sites = new LinkedHashMap<>();
            Map<String, String> byMarker = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("physicalSitePresets")) {
                JsonObject s = element.getAsJsonObject();
                JsonObject d = s.getAsJsonObject("buildingDemand");
                JsonObject xy = s.getAsJsonObject("workingAnchor");
                JsonObject enabled = s.getAsJsonObject("generation");
                boolean worldgen = enabled.get("physicalConstructionEnabled").getAsBoolean();
                if (worldgen || enabled.get("terrainApproved").getAsBoolean())
                    throw new IllegalStateException("C1 must never mark a site build-approved");
                List<String> ids = strings(s.getAsJsonArray("markerIds"));
                Map<String, Integer> counts = integers(d.getAsJsonObject("workplaceCounts"));
                Site site = new Site(s.get("siteId").getAsString(),
                        s.get("name").getAsString(), s.get("tier").getAsString(),
                        s.get("archetype").getAsString(), s.get("blueprintMode").getAsString(),
                        s.get("paletteId").getAsString(),
                        xy.get("x").getAsInt(), xy.get("z").getAsInt(),
                        d.get("peopleTarget").getAsInt(), d.get("workforceTarget").getAsInt(),
                        d.get("housingBuildingsDemand").getAsInt(),
                        d.get("firstPhaseHousingPlots").getAsInt(),
                        d.get("housingBuildingBacklog").getAsInt(), worldgen,
                        ids, strings(s.getAsJsonArray("linkedPoliticalHoldingIds")), counts);
                if (sites.putIfAbsent(site.siteId(), site) != null)
                    throw new IllegalStateException("Duplicated C1 physical site " + site.siteId());
                for (String marker : ids) {
                    if (byMarker.putIfAbsent(marker, site.siteId()) != null)
                        throw new IllegalStateException("Duplicated C1 marker " + marker);
                }
            }
            Map<String, Holding> holdings = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("politicalHoldingPresets")) {
                JsonObject h = element.getAsJsonObject();
                Holding holding = new Holding(h.get("holdingId").getAsString(),
                        h.get("name").getAsString(), h.get("tier").getAsString(),
                        h.get("archetype").getAsString(), h.get("worldLocationId").getAsString(),
                        strings(h.getAsJsonArray("physicalSiteIds")),
                        nullable(h, "ownerDynastyId"), nullable(h, "individualHolderNpcId"),
                        h.get("requiresSeparateSeatPlacement").getAsBoolean());
                if (holdings.putIfAbsent(holding.id(), holding) != null)
                    throw new IllegalStateException("Duplicated C1 holding " + holding.id());
                for (String linkedId : holding.siteIds()) {
                    if (!sites.containsKey(linkedId))
                        throw new IllegalStateException("Unknown C1 holding site " + linkedId);
                }
            }
            Map<String, Workplace> workplaces = new LinkedHashMap<>();
            JsonObject jobs = root.getAsJsonObject("workplaceCatalog");
            for (Map.Entry<String, JsonElement> e : jobs.entrySet()) {
                JsonObject w = e.getValue().getAsJsonObject();
                workplaces.put(e.getKey(), new Workplace(e.getKey(),
                        w.get("staff").getAsInt(), w.get("w").getAsInt(),
                        w.get("d").getAsInt(), flag(w,"shore"), flag(w,"mountain"),
                        flag(w,"requiresPolicy")));
            }
            if (byMarker.size() != 253 || holdings.size() != 259
                    || sites.size() != count.get("physicalSiteCount").getAsInt())
                throw new IllegalStateException("C1 marker/holding/site coverage mismatch");
            return new Index(Collections.unmodifiableMap(sites),
                    Collections.unmodifiableMap(holdings),
                    Collections.unmodifiableMap(byMarker),
                    Collections.unmodifiableMap(workplaces));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read C1 settlement blueprint database", e);
        }
    }

    private static boolean flag(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).getAsBoolean();
    }

    private static String nullable(JsonObject obj, String key) {
        JsonElement v = obj.get(key);
        return v == null || v.isJsonNull() ? null : v.getAsString();
    }

    private static List<String> strings(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement e : array) result.add(e.getAsString());
        return result;
    }

    private static Map<String, Integer> integers(JsonObject obj) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) result.put(e.getKey(), e.getValue().getAsInt());
        return result;
    }
}

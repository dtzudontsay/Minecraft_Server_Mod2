package dev.dtzudontsay.knownworld.simulation.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageRecord;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.scenario.CharacterScenarioState;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class NamedCharacterAuthoringRuntime {

    private static final Gson GSON =
            new Gson();

    private static final String DEFAULT_SCENARIO =
            ScenarioBootstrapper.DEFAULT_SCENARIO;

    private static final String SAVE_HEADER =
            "KNOWNWORLD_CHARACTER_SCENARIO_STATE\t1";

    private static final String SAVE_FILE =
            "character_scenario_state.tsv";

    private static final String TEMP_FILE =
            "character_scenario_state.tsv.tmp";

    private static NamedCharacterAuthoringRuntime instance;

    private final MinecraftServer server;

    private final NpcSimulation simulation;

    private final Path saveDirectory;

    private final Path saveFile;

    private final Map<NpcId, CharacterScenarioState> states =
            new LinkedHashMap<>();

    private AuditReport lastAudit =
            new AuditReport(
                    0,
                    0,
                    0,
                    List.of()
            );

    private NamedCharacterAuthoringRuntime(
            MinecraftServer server,
            NpcSimulation simulation
    ) {

        this.server =
                Objects.requireNonNull(
                        server,
                        "server"
                );

        this.simulation =
                Objects.requireNonNull(
                        simulation,
                        "simulation"
                );

        this.saveDirectory =
                server.getWorldPath(
                                LevelResource.ROOT
                        )
                        .resolve(
                                "knownworld"
                        )
                        .resolve(
                                "npc"
                        );

        this.saveFile =
                saveDirectory.resolve(
                        SAVE_FILE
                );
    }

    public static void registerLifecycle() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> {

                    NpcSimulation simulation =
                            NpcSimulation.getNullable();

                    if (simulation == null) {

                        throw new IllegalStateException(
                                "Cannot initialize named-character authoring runtime before NPC simulation"
                        );
                    }

                    NamedCharacterAuthoringRuntime runtime =
                            new NamedCharacterAuthoringRuntime(
                                    server,
                                    simulation
                            );

                    instance =
                            runtime;

                    runtime.loadAndApply();

                    KnownWorld.LOGGER.info(
                            "Named-character authoring runtime started with {} scenario-state records. Audit: errors={}, warnings={}, info={}.",
                            runtime.states.size(),
                            runtime.lastAudit.errorCount(),
                            runtime.lastAudit.warningCount(),
                            runtime.lastAudit.infoCount()
                    );
                }
        );

        ServerLifecycleEvents.BEFORE_SAVE.register(
                (
                        server,
                        flush,
                        force
                ) -> {

                    NamedCharacterAuthoringRuntime runtime =
                            instance;

                    if (runtime != null
                            && runtime.server == server) {

                        runtime.saveQuietly();
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                server -> {

                    NamedCharacterAuthoringRuntime runtime =
                            instance;

                    if (runtime != null
                            && runtime.server == server) {

                        runtime.saveQuietly();
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> {

                    if (instance != null
                            && instance.server == server) {

                        instance =
                                null;
                    }
                }
        );
    }

    public static NamedCharacterAuthoringRuntime get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Named-character authoring runtime is not running"
            );
        }

        return instance;
    }

    public static NamedCharacterAuthoringRuntime getNullable() {
        return instance;
    }

    public Optional<CharacterScenarioState> find(
            NpcId npc
    ) {

        Objects.requireNonNull(
                npc,
                "npc"
        );

        return Optional.ofNullable(
                states.get(
                        npc
                )
        );
    }

    public Optional<CharacterScenarioState> findAuthored(
            String authoredNpcId
    ) {

        return simulation.authoredIds()
                .findNpc(
                        authoredNpcId
                )
                .flatMap(
                        this::find
                );
    }

    public Collection<CharacterScenarioState> all() {

        return List.copyOf(
                states.values()
        );
    }

    public AuditReport lastAudit() {
        return lastAudit;
    }

    public AuditReport audit() {

        lastAudit =
                buildAudit();

        return lastAudit;
    }

    public void setCurrentWorldLocation(
            NpcId npc,
            String worldLocationId
    ) {

        CharacterScenarioState state =
                states.get(
                        npc
                );

        if (state == null) {

            throw new IllegalArgumentException(
                    "NPC has no scenario-state record: "
                            + npc
            );
        }

        requireWorldLocation(
                worldLocationId
        );

        state.setCurrentWorldLocationId(
                worldLocationId
        );
    }

    private void loadAndApply() {

        try {

            loadPersistence();

            ScenarioIndex scenario =
                    readJson(
                            DEFAULT_SCENARIO,
                            ScenarioIndex.class
                    );

            PopulationPackCatalog.Plan plan =
                    PopulationPackCatalog.load(
                            DEFAULT_SCENARIO,
                            scenario.populationPacks
                    );

            List<String> characterResources =
                    collectCharacterResources(
                            parentPath(
                                    DEFAULT_SCENARIO
                            ),
                            scenario.characters,
                            plan.characterResources()
                    );

            applyCharacterScenarioStates(
                    characterResources
            );

            seedFamilyRelationships();

            applyPackRelationshipResources(
                    plan.relationshipResources()
            );

            lastAudit =
                    buildAudit();

            save();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to initialize named-character authoring runtime",
                    exception
            );
        }
    }

    private void applyCharacterScenarioStates(
            List<String> resources
    ) throws IOException {

        for (
                String resource :
                resources
        ) {

            CharacterData data =
                    readJson(
                            resource,
                            CharacterData.class
                    );

            if (!hasText(
                    data.id
            )
                    || data.scenarioState == null) {

                continue;
            }

            NpcId npc =
                    simulation.authoredIds()
                            .findNpc(
                                    data.id
                            )
                            .orElse(
                                    null
                            );

            if (npc == null) {

                KnownWorld.LOGGER.warn(
                        "Scenario-state authoring references NPC '{}' which is not present.",
                        data.id
                );

                continue;
            }

            ScenarioStateData authored =
                    data.scenarioState;

            int version =
                    authored.authoringVersion <= 0
                            ? 1
                            : authored.authoringVersion;

            CharacterScenarioState.Presence presence =
                    enumValue(
                            CharacterScenarioState.Presence.class,
                            authored.presence,
                            "scenario presence"
                    );

            CharacterScenarioState.Continuity continuity =
                    hasText(
                            authored.continuity
                    )
                            ? enumValue(
                            CharacterScenarioState.Continuity.class,
                            authored.continuity,
                            "scenario continuity"
                    )
                            : CharacterScenarioState.Continuity.UNKNOWN;

            CharacterScenarioState.Confidence confidence =
                    hasText(
                            authored.confidence
                    )
                            ? enumValue(
                            CharacterScenarioState.Confidence.class,
                            authored.confidence,
                            "scenario confidence"
                    )
                            : CharacterScenarioState.Confidence.UNKNOWN;

            String startingLocation =
                    emptyToNull(
                            authored.startingWorldLocationId
                    );

            if (startingLocation != null) {

                requireWorldLocation(
                        startingLocation
                );
            }

            CharacterScenarioState existing =
                    states.get(
                            npc
                    );

            if (existing == null) {

                CharacterScenarioState state =
                        new CharacterScenarioState(
                                npc,
                                version,
                                presence,
                                continuity,
                                confidence,
                                startingLocation,
                                startingLocation,
                                authored.note
                        );

                states.put(
                        npc,
                        state
                );

            } else {

                existing.applyAuthoredRevision(
                        version,
                        presence,
                        continuity,
                        confidence,
                        startingLocation,
                        authored.note
                );
            }

            NpcState npcState =
                    simulation.registry()
                            .find(
                                    npc
                            )
                            .orElseThrow();

            if (presence
                    == CharacterScenarioState.Presence.DEAD_BEFORE_START
                    && npcState.isAlive()) {

                npcState.markDead();
            }
        }
    }

    private void seedFamilyRelationships() {

        for (
                Parentage parentage :
                simulation.genealogy()
                        .all()
        ) {

            if (parentage.mother() != null) {

                ensureRelationship(
                        parentage.mother(),
                        parentage.child(),
                        0.76,
                        0.68,
                        0.48,
                        0.04,
                        1.00
                );

                ensureRelationship(
                        parentage.child(),
                        parentage.mother(),
                        0.78,
                        0.74,
                        0.66,
                        0.08,
                        1.00
                );
            }

            if (parentage.father() != null) {

                ensureRelationship(
                        parentage.father(),
                        parentage.child(),
                        0.74,
                        0.66,
                        0.56,
                        0.04,
                        1.00
                );

                ensureRelationship(
                        parentage.child(),
                        parentage.father(),
                        0.76,
                        0.72,
                        0.72,
                        0.10,
                        1.00
                );
            }
        }

        Map<NpcId, List<NpcId>> childrenByParent =
                new LinkedHashMap<>();

        for (
                Parentage parentage :
                simulation.genealogy()
                        .all()
        ) {

            if (parentage.mother() != null) {

                childrenByParent
                        .computeIfAbsent(
                                parentage.mother(),
                                ignored ->
                                        new ArrayList<>()
                        )
                        .add(
                                parentage.child()
                        );
            }

            if (parentage.father() != null) {

                childrenByParent
                        .computeIfAbsent(
                                parentage.father(),
                                ignored ->
                                        new ArrayList<>()
                        )
                        .add(
                                parentage.child()
                        );
            }
        }

        Set<String> siblingPairs =
                new LinkedHashSet<>();

        for (
                List<NpcId> children :
                childrenByParent.values()
        ) {

            for (
                    int i = 0;
                    i < children.size();
                    i++
            ) {

                for (
                        int j = i + 1;
                        j < children.size();
                        j++
                ) {

                    NpcId first =
                            children.get(
                                    i
                            );

                    NpcId second =
                            children.get(
                                    j
                            );

                    String key =
                            Math.min(
                                    first.value(),
                                    second.value()
                            )
                                    + ":"
                                    + Math.max(
                                    first.value(),
                                    second.value()
                            );

                    if (!siblingPairs.add(
                            key
                    )) {
                        continue;
                    }

                    ensureRelationship(
                            first,
                            second,
                            0.58,
                            0.55,
                            0.42,
                            0.02,
                            1.00
                    );

                    ensureRelationship(
                            second,
                            first,
                            0.58,
                            0.55,
                            0.42,
                            0.02,
                            1.00
                    );
                }
            }
        }

        for (
                MarriageRecord marriage :
                simulation.marriages()
                        .all()
        ) {

            if (!marriage.isActive()) {
                continue;
            }

            ensureRelationship(
                    marriage.first(),
                    marriage.second(),
                    0.64,
                    0.64,
                    0.58,
                    0.03,
                    1.00
            );

            ensureRelationship(
                    marriage.second(),
                    marriage.first(),
                    0.64,
                    0.64,
                    0.58,
                    0.03,
                    1.00
            );
        }
    }

    private void applyPackRelationshipResources(
            List<String> resources
    ) throws IOException {

        for (
                String resource :
                resources
        ) {

            RelationshipData[] entries =
                    readJson(
                            resource,
                            RelationshipData[].class
                    );

            Set<String> seen =
                    new LinkedHashSet<>();

            for (
                    RelationshipData entry :
                    entries
            ) {

                requireText(
                        entry.subject,
                        resource + ".subject"
                );

                requireText(
                        entry.target,
                        resource + ".target"
                );

                int version =
                        entry.authoringVersion <= 0
                                ? 1
                                : entry.authoringVersion;

                String duplicateKey =
                        normalizeId(
                                entry.subject
                        )
                                + "->"
                                + normalizeId(
                                entry.target
                        );

                if (!seen.add(
                        duplicateKey
                )) {

                    throw new IOException(
                            "Duplicate relationship in "
                                    + resource
                                    + ": "
                                    + duplicateKey
                    );
                }

                NpcId subject =
                        simulation.authoredIds()
                                .requireNpc(
                                        entry.subject
                                );

                NpcId target =
                        simulation.authoredIds()
                                .requireNpc(
                                        entry.target
                                );

                if (subject.equals(
                        target
                )) {

                    throw new IOException(
                            "Relationship cannot target itself: "
                                    + entry.subject
                    );
                }

                CharacterProfile profile =
                        simulation.profiles()
                                .getOrCreate(
                                        subject
                                );

                String marker =
                        "authoring.relationship."
                                + normalizeId(
                                entry.target
                        )
                                + ".v"
                                + version;

                if (profile.values()
                        .containsKey(
                                marker
                        )) {

                    continue;
                }

                simulation.relationships()
                        .registerLoaded(
                                new NpcRelationship(
                                        subject,
                                        target,
                                        entry.affection,
                                        entry.trust,
                                        entry.respect,
                                        entry.fear,
                                        entry.familiarity
                                )
                        );

                profile.setValue(
                        marker,
                        1.0
                );
            }
        }
    }

    private void ensureRelationship(
            NpcId subject,
            NpcId target,
            double affection,
            double trust,
            double respect,
            double fear,
            double familiarity
    ) {

        if (subject.equals(
                target
        )
                || simulation.relationships()
                .find(
                        subject,
                        target
                )
                .isPresent()) {

            return;
        }

        simulation.relationships()
                .registerLoaded(
                        new NpcRelationship(
                                subject,
                                target,
                                affection,
                                trust,
                                respect,
                                fear,
                                familiarity
                        )
                );
    }

    private AuditReport buildAudit() {

        List<AuditIssue> issues =
                new ArrayList<>();

        int authoredNpcCount =
                simulation.authoredIds()
                        .npcs()
                        .size();

        for (
                Map.Entry<String, NpcId> entry :
                simulation.authoredIds()
                        .npcs()
                        .entrySet()
        ) {

            CharacterScenarioState state =
                    states.get(
                            entry.getValue()
                    );

            if (state == null) {

                issues.add(
                        new AuditIssue(
                                Severity.WARNING,
                                "MISSING_SCENARIO_STATE",
                                entry.getKey(),
                                "Named character has no scenarioState metadata yet."
                        )
                );

                continue;
            }

            if (state.presence()
                    == CharacterScenarioState.Presence.NOT_YET_BORN) {

                issues.add(
                        new AuditIssue(
                                Severity.ERROR,
                                "NOT_YET_BORN_NPC_IN_ACTIVE_SCENARIO",
                                entry.getKey(),
                                "A NOT_YET_BORN character must not be instantiated as an active scenario NPC."
                        )
                );
            }

            NpcState npc =
                    simulation.registry()
                            .find(
                                    entry.getValue()
                            )
                            .orElse(
                                    null
                            );

            if (npc == null) {

                issues.add(
                        new AuditIssue(
                                Severity.ERROR,
                                "AUTHORED_NPC_MISSING",
                                entry.getKey(),
                                "Authored NPC ID is mapped but NPC is missing from the registry."
                        )
                );

                continue;
            }

            if (state.presence()
                    == CharacterScenarioState.Presence.DEAD_BEFORE_START
                    && npc.isAlive()) {

                issues.add(
                        new AuditIssue(
                                Severity.ERROR,
                                "DEAD_BEFORE_START_STILL_ALIVE",
                                entry.getKey(),
                                "Scenario metadata says DEAD_BEFORE_START but NPC remains alive."
                        )
                );
            }

            if (state.presence()
                    == CharacterScenarioState.Presence.ALIVE
                    && !npc.isAlive()) {

                issues.add(
                        new AuditIssue(
                                Severity.INFO,
                                "AUTHORED_ALIVE_CHARACTER_NOW_DEAD",
                                entry.getKey(),
                                "Character was alive at Scenario Day 1 but is currently dead. This is valid after gameplay has progressed."
                        )
                );
            }

            if (state.startingWorldLocationId() == null) {

                issues.add(
                        new AuditIssue(
                                Severity.WARNING,
                                "MISSING_STARTING_WORLD_LOCATION",
                                entry.getKey(),
                                "Scenario-state metadata has no semantic starting world location."
                        )
                );
            }
        }

        for (
                Parentage parentage :
                simulation.genealogy()
                        .all()
        ) {

            auditRelationshipPair(
                    issues,
                    parentage.child(),
                    parentage.mother(),
                    "PARENT_CHILD_RELATIONSHIP_MISSING"
            );

            auditRelationshipPair(
                    issues,
                    parentage.child(),
                    parentage.father(),
                    "PARENT_CHILD_RELATIONSHIP_MISSING"
            );
        }

        Set<String> siblingPairs =
                new LinkedHashSet<>();

        List<NpcId> authored =
                new ArrayList<>(
                        simulation.authoredIds()
                                .npcs()
                                .values()
                );

        for (
                int i = 0;
                i < authored.size();
                i++
        ) {

            for (
                    int j = i + 1;
                    j < authored.size();
                    j++
            ) {

                NpcId first =
                        authored.get(
                                i
                        );

                NpcId second =
                        authored.get(
                                j
                        );

                if (!simulation.genealogy()
                        .areSiblings(
                                first,
                                second
                        )) {

                    continue;
                }

                String key =
                        first.value()
                                + ":"
                                + second.value();

                if (!siblingPairs.add(
                        key
                )) {
                    continue;
                }

                auditRelationshipPair(
                        issues,
                        first,
                        second,
                        "SIBLING_RELATIONSHIP_MISSING"
                );
            }
        }

        for (
                MarriageRecord marriage :
                simulation.marriages()
                        .all()
        ) {

            if (marriage.isActive()) {

                auditRelationshipPair(
                        issues,
                        marriage.first(),
                        marriage.second(),
                        "SPOUSE_RELATIONSHIP_MISSING"
                );
            }
        }

        issues.sort(
                Comparator
                        .comparing(
                                AuditIssue::severity
                        )
                        .thenComparing(
                                AuditIssue::code
                        )
                        .thenComparing(
                                AuditIssue::subject
                        )
        );

        long covered =
                states.keySet()
                        .stream()
                        .filter(
                                simulation.authoredIds()
                                        .npcs()
                                        .values()::contains
                        )
                        .count();

        return new AuditReport(
                authoredNpcCount,
                (int) covered,
                simulation.relationships()
                        .size(),
                issues
        );
    }

    private void auditRelationshipPair(
            List<AuditIssue> issues,
            NpcId first,
            NpcId second,
            String code
    ) {

        if (first == null
                || second == null) {

            return;
        }

        if (simulation.relationships()
                .find(
                        first,
                        second
                )
                .isEmpty()) {

            issues.add(
                    new AuditIssue(
                            Severity.ERROR,
                            code,
                            npcName(
                                    first
                            ),
                            "Missing directional relationship toward "
                                    + npcName(
                                    second
                            )
                    )
            );
        }

        if (simulation.relationships()
                .find(
                        second,
                        first
                )
                .isEmpty()) {

            issues.add(
                    new AuditIssue(
                            Severity.ERROR,
                            code,
                            npcName(
                                    second
                            ),
                            "Missing directional relationship toward "
                                    + npcName(
                                    first
                            )
                    )
            );
        }
    }

    private String npcName(
            NpcId id
    ) {

        return simulation.registry()
                .find(
                        id
                )
                .map(
                        npc ->
                                npc.identity()
                                        .fullName()
                                        + " [#"
                                        + id.value()
                                        + "]"
                )
                .orElse(
                        "NPC #"
                                + id.value()
                );
    }

    private void requireWorldLocation(
            String locationId
    ) {

        if (WorldReferenceCatalog.get()
                .location(
                        locationId
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown semantic world location: "
                            + locationId
            );
        }
    }

    private void loadPersistence() throws IOException {

        if (!Files.exists(
                saveFile
        )) {

            return;
        }

        try (
                BufferedReader reader =
                        Files.newBufferedReader(
                                saveFile,
                                StandardCharsets.UTF_8
                        )
        ) {

            String header =
                    reader.readLine();

            if (!SAVE_HEADER.equals(
                    header
            )) {

                throw new IOException(
                        "Unsupported character scenario-state format: "
                                + header
                );
            }

            String line;

            int lineNumber =
                    1;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {

                    CharacterScenarioState state =
                            decodeState(
                                    line
                            );

                    if (simulation.registry()
                            .find(
                                    state.npc()
                            )
                            .isEmpty()) {

                        throw new IllegalArgumentException(
                                "Unknown NPC "
                                        + state.npc()
                        );
                    }

                    validateOptionalWorldLocation(
                            state.startingWorldLocationId()
                    );

                    validateOptionalWorldLocation(
                            state.currentWorldLocationId()
                    );

                    if (states.putIfAbsent(
                            state.npc(),
                            state
                    ) != null) {

                        throw new IllegalStateException(
                                "Duplicate scenario-state record for "
                                        + state.npc()
                        );
                    }

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid character scenario-state at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    private void save() throws IOException {

        Files.createDirectories(
                saveDirectory
        );

        Path temporary =
                saveDirectory.resolve(
                        TEMP_FILE
                );

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                temporary,
                                StandardCharsets.UTF_8
                        )
        ) {

            writer.write(
                    SAVE_HEADER
            );

            writer.newLine();

            for (
                    CharacterScenarioState state :
                    states.values()
                            .stream()
                            .sorted(
                                    Comparator.comparing(
                                            CharacterScenarioState::npc
                                    )
                            )
                            .toList()
            ) {

                writer.write(
                        encodeState(
                                state
                        )
                );

                writer.newLine();
            }
        }

        try {

            Files.move(
                    temporary,
                    saveFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } catch (
                IOException exception
        ) {

            Files.move(
                    temporary,
                    saveFile,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void saveQuietly() {

        try {

            save();

        } catch (
                IOException exception
        ) {

            KnownWorld.LOGGER.error(
                    "Failed to save named-character scenario state.",
                    exception
            );
        }
    }

    private static String encodeState(
            CharacterScenarioState state
    ) {

        return String.join(
                "\t",
                "STATE",
                Long.toString(
                        state.npc()
                                .value()
                ),
                Integer.toString(
                        state.authoredVersion()
                ),
                state.presence()
                        .name(),
                state.continuity()
                        .name(),
                state.confidence()
                        .name(),
                escape(
                        nullToEmpty(
                                state.startingWorldLocationId()
                        )
                ),
                escape(
                        nullToEmpty(
                                state.currentWorldLocationId()
                        )
                ),
                escape(
                        state.note()
                )
        );
    }

    private static CharacterScenarioState decodeState(
            String line
    ) {

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 9
                || !"STATE".equals(
                parts[0]
        )) {

            throw new IllegalArgumentException(
                    "Expected STATE record with 9 columns"
            );
        }

        return new CharacterScenarioState(
                new NpcId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                Integer.parseInt(
                        parts[2]
                ),
                CharacterScenarioState.Presence.valueOf(
                        parts[3]
                ),
                CharacterScenarioState.Continuity.valueOf(
                        parts[4]
                ),
                CharacterScenarioState.Confidence.valueOf(
                        parts[5]
                ),
                emptyToNull(
                        unescape(
                                parts[6]
                        )
                ),
                emptyToNull(
                        unescape(
                                parts[7]
                        )
                ),
                unescape(
                        parts[8]
                )
        );
    }

    private void validateOptionalWorldLocation(
            String value
    ) {

        if (value != null) {

            requireWorldLocation(
                    value
            );
        }
    }

    private static List<String> collectCharacterResources(
            String base,
            List<String> legacy,
            List<String> packs
    ) {

        LinkedHashSet<String> result =
                new LinkedHashSet<>();

        if (legacy != null) {

            for (
                    String entry :
                    legacy
            ) {

                if (hasText(
                        entry
                )) {

                    result.add(
                            resolve(
                                    base,
                                    entry
                            )
                    );
                }
            }
        }

        result.addAll(
                packs
        );

        return List.copyOf(
                result
        );
    }

    private static String resolve(
            String base,
            String child
    ) {

        if (child.startsWith(
                "data/"
        )) {

            return child;
        }

        return base
                + "/"
                + child;
    }

    private static String parentPath(
            String resource
    ) {

        int separator =
                resource.lastIndexOf(
                        '/'
                );

        return separator < 0
                ? ""
                : resource.substring(
                0,
                separator
        );
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value,
            String description
    ) {

        requireText(
                value,
                description
        );

        try {

            return Enum.valueOf(
                    type,
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new IllegalArgumentException(
                    "Unknown "
                            + description
                            + ": "
                            + value,
                    exception
            );
        }
    }

    private static String normalizeId(
            String value
    ) {

        requireText(
                value,
                "id"
        );

        return value.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private static void requireText(
            String value,
            String description
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be blank"
            );
        }
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static String emptyToNull(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? null
                : value.trim();
    }

    private static String nullToEmpty(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    private static String escape(
            String value
    ) {

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\t",
                        "\\t"
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }

    private static String unescape(
            String value
    ) {

        StringBuilder result =
                new StringBuilder();

        boolean escaped =
                false;

        for (
                char character :
                value.toCharArray()
        ) {

            if (escaped) {

                switch (character) {

                    case 't' ->
                            result.append(
                                    '\t'
                            );

                    case 'n' ->
                            result.append(
                                    '\n'
                            );

                    case 'r' ->
                            result.append(
                                    '\r'
                            );

                    case '\\' ->
                            result.append(
                                    '\\'
                            );

                    default ->
                            result.append(
                                    character
                            );
                }

                escaped =
                        false;

            } else if (
                    character == '\\'
            ) {

                escaped =
                        true;

            } else {

                result.append(
                        character
                );
            }
        }

        if (escaped) {

            result.append(
                    '\\'
            );
        }

        return result.toString();
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        NamedCharacterAuthoringRuntime.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Named-character resource not found: "
                                + resource
                );
            }

            try (
                    Reader reader =
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
            ) {

                T result =
                        GSON.fromJson(
                                reader,
                                type
                        );

                if (result == null) {

                    throw new IOException(
                            "Named-character resource produced null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid named-character JSON: "
                            + resource,
                    exception
            );
        }
    }

    public enum Severity {

        ERROR,

        WARNING,

        INFO
    }

    public record AuditIssue(
            Severity severity,
            String code,
            String subject,
            String message
    ) {
    }

    public record AuditReport(
            int authoredNpcCount,
            int scenarioStateCount,
            int relationshipCount,
            List<AuditIssue> issues
    ) {

        public AuditReport {

            issues =
                    List.copyOf(
                            issues
                    );
        }

        public long errorCount() {

            return issues.stream()
                    .filter(
                            issue ->
                                    issue.severity()
                                            == Severity.ERROR
                    )
                    .count();
        }

        public long warningCount() {

            return issues.stream()
                    .filter(
                            issue ->
                                    issue.severity()
                                            == Severity.WARNING
                    )
                    .count();
        }

        public long infoCount() {

            return issues.stream()
                    .filter(
                            issue ->
                                    issue.severity()
                                            == Severity.INFO
                    )
                    .count();
        }

        public List<AuditIssue> errors() {

            return issues.stream()
                    .filter(
                            issue ->
                                    issue.severity()
                                            == Severity.ERROR
                    )
                    .toList();
        }

        public List<AuditIssue> warnings() {

            return issues.stream()
                    .filter(
                            issue ->
                                    issue.severity()
                                            == Severity.WARNING
                    )
                    .toList();
        }

        public List<AuditIssue> info() {

            return issues.stream()
                    .filter(
                            issue ->
                                    issue.severity()
                                            == Severity.INFO
                    )
                    .toList();
        }
    }

    private static final class ScenarioIndex {

        String populationPacks;

        List<String> characters;
    }

    private static final class CharacterData {

        String id;

        ScenarioStateData scenarioState;
    }

    private static final class ScenarioStateData {

        int authoringVersion;

        String presence;

        String continuity;

        String confidence;

        String startingWorldLocationId;

        String note;
    }

    private static final class RelationshipData {

        int authoringVersion;

        String subject;

        String target;

        double affection;

        double trust;

        double respect;

        double fear;

        double familiarity;

        String sourceNote;
    }
}

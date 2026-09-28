package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventId;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcIdentity;
import dev.dtzudontsay.knownworld.simulation.npc.NpcLifeState;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoal;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalId;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalStatus;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalType;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcBelief;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemory;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryId;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeedManager;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeeds;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoleType;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutine;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineManager;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;
import dev.dtzudontsay.knownworld.simulation.world.settlement.Settlement;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;

public final class NpcPersistence {

    private static final String HEADER_PREFIX =
            "KNOWNWORLD_NPCS\t";

    private static final int CURRENT_VERSION =
            8;

    private static final String FILE_NAME =
            "npcs.tsv";

    private static final String TEMP_FILE_NAME =
            "npcs.tsv.tmp";

    private final Path directory;

    private final Path file;

    public NpcPersistence(
            Path directory
    ) {
        this.directory =
                directory;

        this.file =
                directory.resolve(
                        FILE_NAME
                );
    }

    public void save(
            SimulationClock clock,
            NpcRegistry registry,
            SettlementManager settlements,
            OrganizationManager organizations,
            NpcAffiliationManager affiliations,
            NpcRelationshipManager relationships,
            NpcKnowledgeManager knowledge,
            NpcMemoryManager memories,
            NpcGoalManager goals,
            NpcNeedManager needs,
            NpcRoutineManager routines,
            WorldEventManager events
    ) throws IOException {

        Files.createDirectories(
                directory
        );

        Path temporary =
                directory.resolve(
                        TEMP_FILE_NAME
                );

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                temporary,
                                StandardCharsets.UTF_8
                        )
        ) {
            writer.write(
                    HEADER_PREFIX
                            + CURRENT_VERSION
            );

            writer.newLine();

            writer.write(
                    "CLOCK\t"
                            + clock.tick()
            );

            writer.newLine();

            /*
             * NPCs load before any records which reference NPC IDs.
             */
            for (
                    NpcState npc :
                    registry.all()
                            .stream()
                            .sorted(
                                    Comparator.comparing(
                                            NpcState::id
                                    )
                            )
                            .toList()
            ) {
                writer.write(
                        encodeNpc(
                                npc
                        )
                );

                writer.newLine();
            }

            /*
             * Settlements must appear before organizations because an
             * organization may reference a settlement as its seat.
             */
            for (
                    Settlement settlement :
                    settlements.all()
                            .stream()
                            .sorted(
                                    Comparator.comparing(
                                            Settlement::id
                                    )
                            )
                            .toList()
            ) {
                writer.write(
                        encodeSettlement(
                                settlement
                        )
                );

                writer.newLine();
            }

            /*
             * Organizations must appear before affiliations because
             * affiliations validate organization references.
             */
            for (
                    Organization organization :
                    organizations.all()
                            .stream()
                            .sorted(
                                    Comparator.comparing(
                                            Organization::id
                                    )
                            )
                            .toList()
            ) {
                writer.write(
                        encodeOrganization(
                                organization
                        )
                );

                writer.newLine();
            }

            for (
                    NpcAffiliation affiliation :
                    affiliations.all()
            ) {
                writer.write(
                        encodeAffiliation(
                                affiliation
                        )
                );

                writer.newLine();
            }

            for (
                    Map.Entry<NpcId, NpcNeeds> entry :
                    needs.all()
                            .entrySet()
            ) {
                writer.write(
                        encodeNeeds(
                                entry.getKey(),
                                entry.getValue()
                        )
                );

                writer.newLine();
            }

            for (
                    NpcRoutine routine :
                    routines.all()
            ) {
                writer.write(
                        encodeRoutine(
                                routine
                        )
                );

                writer.newLine();
            }

            for (
                    NpcRelationship relationship :
                    relationships.all()
            ) {
                writer.write(
                        encodeRelationship(
                                relationship
                        )
                );

                writer.newLine();
            }

            for (
                    NpcBelief belief :
                    knowledge.all()
            ) {
                writer.write(
                        encodeBelief(
                                belief
                        )
                );

                writer.newLine();
            }

            for (
                    NpcMemory memory :
                    memories.all()
            ) {
                writer.write(
                        encodeMemory(
                                memory
                        )
                );

                writer.newLine();
            }

            for (
                    NpcGoal goal :
                    goals.all()
            ) {
                writer.write(
                        encodeGoal(
                                goal
                        )
                );

                writer.newLine();
            }

            for (
                    WorldEvent event :
                    events.all()
            ) {
                writer.write(
                        encodeEvent(
                                event
                        )
                );

                writer.newLine();
            }
        }

        try {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } catch (IOException exception) {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    public void loadInto(
            SimulationClock clock,
            NpcRegistry registry,
            SettlementManager settlements,
            OrganizationManager organizations,
            NpcAffiliationManager affiliations,
            NpcRelationshipManager relationships,
            NpcKnowledgeManager knowledge,
            NpcMemoryManager memories,
            NpcGoalManager goals,
            NpcNeedManager needs,
            NpcRoutineManager routines,
            WorldEventManager events
    ) throws IOException {

        if (!Files.exists(
                file
        )) {
            return;
        }

        try (
                BufferedReader reader =
                        Files.newBufferedReader(
                                file,
                                StandardCharsets.UTF_8
                        )
        ) {
            String header =
                    reader.readLine();

            if (header == null
                    || !header.startsWith(
                    HEADER_PREFIX
            )) {

                throw new IOException(
                        "Unsupported NPC save format: "
                                + header
                );
            }

            final int version;

            try {

                version =
                        Integer.parseInt(
                                header.substring(
                                        HEADER_PREFIX.length()
                                )
                        );

            } catch (NumberFormatException exception) {

                throw new IOException(
                        "Invalid NPC save version header: "
                                + header,
                        exception
                );
            }

            if (version < 1
                    || version > CURRENT_VERSION) {

                throw new IOException(
                        "Unsupported NPC save version: "
                                + version
                );
            }

            if (version == 1) {

                loadVersion1(
                        reader,
                        registry
                );

                return;
            }

            loadStructured(
                    reader,
                    clock,
                    registry,
                    settlements,
                    organizations,
                    affiliations,
                    relationships,
                    knowledge,
                    memories,
                    goals,
                    needs,
                    routines,
                    events,
                    version
            );
        }
    }

    private static void loadVersion1(
            BufferedReader reader,
            NpcRegistry registry
    ) throws IOException {

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

                registry.registerLoaded(
                        decodeNpcV1(
                                line
                        )
                );

            } catch (RuntimeException exception) {

                throw new IOException(
                        "Invalid version 1 NPC data at line "
                                + lineNumber,
                        exception
                );
            }
        }
    }

    private static void loadStructured(
            BufferedReader reader,
            SimulationClock clock,
            NpcRegistry registry,
            SettlementManager settlements,
            OrganizationManager organizations,
            NpcAffiliationManager affiliations,
            NpcRelationshipManager relationships,
            NpcKnowledgeManager knowledge,
            NpcMemoryManager memories,
            NpcGoalManager goals,
            NpcNeedManager needs,
            NpcRoutineManager routines,
            WorldEventManager events,
            int version
    ) throws IOException {

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

                if (line.startsWith(
                        "CLOCK\t"
                )) {

                    if (version >= 6) {

                        clock.setTick(
                                Long.parseLong(
                                        line.substring(
                                                "CLOCK\t".length()
                                        )
                                )
                        );
                    }

                    continue;
                }

                if (line.startsWith(
                        "NPC\t"
                )) {

                    registry.registerLoaded(
                            decodeNpc(
                                    line
                            )
                    );

                    continue;
                }

                if (
                        line.startsWith(
                                "SETTLEMENT\t"
                        )
                                && version >= 8
                ) {

                    settlements.registerLoaded(
                            decodeSettlement(
                                    line
                            )
                    );

                    continue;
                }

                if (
                        line.startsWith(
                                "ORG\t"
                        )
                                && version >= 8
                ) {

                    organizations.registerLoaded(
                            decodeOrganization(
                                    line
                            )
                    );

                    continue;
                }

                if (
                        line.startsWith(
                                "AFFILIATION\t"
                        )
                                && version >= 8
                ) {

                    affiliations.registerLoaded(
                            decodeAffiliation(
                                    line
                            )
                    );

                    continue;
                }

                if (
                        line.startsWith(
                                "NEED\t"
                        )
                                && version >= 6
                ) {

                    decodeNeeds(
                            line,
                            needs
                    );

                    continue;
                }

                if (
                        line.startsWith(
                                "ROUTINE\t"
                        )
                                && version >= 7
                ) {

                    routines.registerLoaded(
                            decodeRoutine(
                                    line
                            )
                    );

                    continue;
                }

                if (line.startsWith(
                        "REL\t"
                )) {

                    relationships.registerLoaded(
                            decodeRelationship(
                                    line
                            )
                    );

                    continue;
                }

                if (line.startsWith(
                        "BELIEF\t"
                )) {

                    knowledge.registerLoaded(
                            decodeBelief(
                                    line
                            )
                    );

                    continue;
                }

                if (
                        line.startsWith(
                                "MEM\t"
                        )
                                && version >= 3
                ) {

                    memories.registerLoaded(
                            decodeMemory(
                                    line
                            )
                    );

                    continue;
                }

                if (
                        line.startsWith(
                                "GOAL\t"
                        )
                                && version >= 5
                ) {

                    goals.registerLoaded(
                            decodeGoal(
                                    line
                            )
                    );

                    continue;
                }

                if (
                        line.startsWith(
                                "EVENT\t"
                        )
                                && version >= 4
                ) {

                    events.registerLoaded(
                            decodeEvent(
                                    line
                            )
                    );

                    continue;
                }

                throw new IllegalArgumentException(
                        "Unexpected record type"
                );

            } catch (RuntimeException exception) {

                throw new IOException(
                        "Invalid version "
                                + version
                                + " NPC data at line "
                                + lineNumber
                                + ": "
                                + line,
                        exception
                );
            }
        }
    }

    // ---------------------------------------------------------------------
    // Settlement persistence
    // ---------------------------------------------------------------------

    private static String encodeSettlement(
            Settlement settlement
    ) {
        return String.join(
                "\t",
                "SETTLEMENT",
                Long.toString(
                        settlement.id()
                                .value()
                ),
                escape(
                        settlement.name()
                ),
                settlement.type()
                        .name(),
                escape(
                        settlement.center()
                                .dimension()
                ),
                Double.toString(
                        settlement.center()
                                .x()
                ),
                Double.toString(
                        settlement.center()
                                .y()
                ),
                Double.toString(
                        settlement.center()
                                .z()
                )
        );
    }

    private static Settlement decodeSettlement(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 8) {
            throw new IllegalArgumentException(
                    "Expected 8 settlement columns, got "
                            + p.length
            );
        }

        return new Settlement(
                new SettlementId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                unescape(
                        p[2]
                ),
                SettlementType.valueOf(
                        p[3]
                ),
                new SimulationPosition(
                        unescape(
                                p[4]
                        ),
                        Double.parseDouble(
                                p[5]
                        ),
                        Double.parseDouble(
                                p[6]
                        ),
                        Double.parseDouble(
                                p[7]
                        )
                )
        );
    }

    // ---------------------------------------------------------------------
    // Organization persistence
    // ---------------------------------------------------------------------

    private static String encodeOrganization(
            Organization organization
    ) {
        return String.join(
                "\t",
                "ORG",
                Long.toString(
                        organization.id()
                                .value()
                ),
                escape(
                        organization.name()
                ),
                organization.type()
                        .name(),
                organization.seatSettlement()
                        == null
                        ? ""
                        : Long.toString(
                        organization.seatSettlement()
                                .value()
                )
        );
    }

    private static Organization decodeOrganization(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 5) {
            throw new IllegalArgumentException(
                    "Expected 5 organization columns, got "
                            + p.length
            );
        }

        SettlementId seat =
                p[4].isEmpty()
                        ? null
                        : new SettlementId(
                        Long.parseLong(
                                p[4]
                        )
                );

        return new Organization(
                new OrganizationId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                unescape(
                        p[2]
                ),
                OrganizationType.valueOf(
                        p[3]
                ),
                seat
        );
    }

    // ---------------------------------------------------------------------
    // NPC affiliation persistence
    // ---------------------------------------------------------------------

    private static String encodeAffiliation(
            NpcAffiliation affiliation
    ) {
        return String.join(
                "\t",
                "AFFILIATION",
                Long.toString(
                        affiliation.owner()
                                .value()
                ),
                affiliation.residenceSettlement()
                        == null
                        ? ""
                        : Long.toString(
                        affiliation.residenceSettlement()
                                .value()
                ),
                affiliation.household()
                        == null
                        ? ""
                        : Long.toString(
                        affiliation.household()
                                .value()
                ),
                affiliation.nobleHouse()
                        == null
                        ? ""
                        : Long.toString(
                        affiliation.nobleHouse()
                                .value()
                ),
                affiliation.faction()
                        == null
                        ? ""
                        : Long.toString(
                        affiliation.faction()
                                .value()
                )
        );
    }

    private static NpcAffiliation decodeAffiliation(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 6) {
            throw new IllegalArgumentException(
                    "Expected 6 affiliation columns, got "
                            + p.length
            );
        }

        SettlementId residence =
                p[2].isEmpty()
                        ? null
                        : new SettlementId(
                        Long.parseLong(
                                p[2]
                        )
                );

        OrganizationId household =
                p[3].isEmpty()
                        ? null
                        : new OrganizationId(
                        Long.parseLong(
                                p[3]
                        )
                );

        OrganizationId nobleHouse =
                p[4].isEmpty()
                        ? null
                        : new OrganizationId(
                        Long.parseLong(
                                p[4]
                        )
                );

        OrganizationId faction =
                p[5].isEmpty()
                        ? null
                        : new OrganizationId(
                        Long.parseLong(
                                p[5]
                        )
                );

        return new NpcAffiliation(
                new NpcId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                residence,
                household,
                nobleHouse,
                faction
        );
    }

    // ---------------------------------------------------------------------
    // Needs persistence
    // ---------------------------------------------------------------------

    private static String encodeNeeds(
            NpcId npc,
            NpcNeeds needs
    ) {
        return String.join(
                "\t",
                "NEED",
                Long.toString(
                        npc.value()
                ),
                Double.toString(
                        needs.fatigue()
                ),
                Double.toString(
                        needs.hunger()
                ),
                Double.toString(
                        needs.social()
                )
        );
    }

    private static void decodeNeeds(
            String line,
            NpcNeedManager manager
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 5) {
            throw new IllegalArgumentException(
                    "Expected 5 need columns, got "
                            + p.length
            );
        }

        manager.registerLoaded(
                new NpcId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                new NpcNeeds(
                        Double.parseDouble(
                                p[2]
                        ),
                        Double.parseDouble(
                                p[3]
                        ),
                        Double.parseDouble(
                                p[4]
                        )
                )
        );
    }

    // ---------------------------------------------------------------------
    // Routine persistence
    // ---------------------------------------------------------------------

    private static String encodeRoutine(
            NpcRoutine routine
    ) {
        SimulationPosition home =
                routine.homePosition();

        SimulationPosition work =
                routine.workPosition();

        return String.join(
                "\t",
                "ROUTINE",
                Long.toString(
                        routine.owner()
                                .value()
                ),
                routine.role()
                        .name(),

                home == null
                        ? ""
                        : escape(
                        home.dimension()
                ),

                home == null
                        ? ""
                        : Double.toString(
                        home.x()
                ),

                home == null
                        ? ""
                        : Double.toString(
                        home.y()
                ),

                home == null
                        ? ""
                        : Double.toString(
                        home.z()
                ),

                work == null
                        ? ""
                        : escape(
                        work.dimension()
                ),

                work == null
                        ? ""
                        : Double.toString(
                        work.x()
                ),

                work == null
                        ? ""
                        : Double.toString(
                        work.y()
                ),

                work == null
                        ? ""
                        : Double.toString(
                        work.z()
                ),

                Integer.toString(
                        routine.workStartTick()
                ),

                Integer.toString(
                        routine.workEndTick()
                )
        );
    }

    private static NpcRoutine decodeRoutine(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 13) {
            throw new IllegalArgumentException(
                    "Expected 13 routine columns, got "
                            + p.length
            );
        }

        SimulationPosition home =
                p[3].isEmpty()
                        ? null
                        : new SimulationPosition(
                        unescape(
                                p[3]
                        ),
                        Double.parseDouble(
                                p[4]
                        ),
                        Double.parseDouble(
                                p[5]
                        ),
                        Double.parseDouble(
                                p[6]
                        )
                );

        SimulationPosition work =
                p[7].isEmpty()
                        ? null
                        : new SimulationPosition(
                        unescape(
                                p[7]
                        ),
                        Double.parseDouble(
                                p[8]
                        ),
                        Double.parseDouble(
                                p[9]
                        ),
                        Double.parseDouble(
                                p[10]
                        )
                );

        return new NpcRoutine(
                new NpcId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                NpcRoleType.valueOf(
                        p[2]
                ),
                home,
                work,
                Integer.parseInt(
                        p[11]
                ),
                Integer.parseInt(
                        p[12]
                )
        );
    }

    // ---------------------------------------------------------------------
    // Goal persistence
    // ---------------------------------------------------------------------

    private static String encodeGoal(
            NpcGoal goal
    ) {
        SimulationPosition target =
                goal.targetPosition();

        return String.join(
                "\t",
                "GOAL",
                Long.toString(
                        goal.id()
                                .value()
                ),
                Long.toString(
                        goal.owner()
                                .value()
                ),
                goal.type()
                        .name(),
                goal.status()
                        .name(),
                escape(
                        goal.description()
                ),
                Double.toString(
                        goal.priority()
                ),
                Long.toString(
                        goal.createdTick()
                ),
                goal.targetNpc()
                        == null
                        ? ""
                        : Long.toString(
                        goal.targetNpc()
                                .value()
                ),
                target == null
                        ? ""
                        : escape(
                        target.dimension()
                ),
                target == null
                        ? ""
                        : Double.toString(
                        target.x()
                ),
                target == null
                        ? ""
                        : Double.toString(
                        target.y()
                ),
                target == null
                        ? ""
                        : Double.toString(
                        target.z()
                ),
                goal.factKey()
                        == null
                        ? ""
                        : escape(
                        goal.factKey()
                )
        );
    }

    private static NpcGoal decodeGoal(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 14) {
            throw new IllegalArgumentException(
                    "Expected 14 goal columns, got "
                            + p.length
            );
        }

        NpcId targetNpc =
                p[8].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                p[8]
                        )
                );

        SimulationPosition targetPosition =
                p[9].isEmpty()
                        ? null
                        : new SimulationPosition(
                        unescape(
                                p[9]
                        ),
                        Double.parseDouble(
                                p[10]
                        ),
                        Double.parseDouble(
                                p[11]
                        ),
                        Double.parseDouble(
                                p[12]
                        )
                );

        String factKey =
                p[13].isEmpty()
                        ? null
                        : unescape(
                        p[13]
                );

        return new NpcGoal(
                new NpcGoalId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                new NpcId(
                        Long.parseLong(
                                p[2]
                        )
                ),
                NpcGoalType.valueOf(
                        p[3]
                ),
                unescape(
                        p[5]
                ),
                Double.parseDouble(
                        p[6]
                ),
                Long.parseLong(
                        p[7]
                ),
                targetNpc,
                targetPosition,
                factKey,
                NpcGoalStatus.valueOf(
                        p[4]
                )
        );
    }

    // ---------------------------------------------------------------------
    // NPC persistence
    // ---------------------------------------------------------------------

    private static String encodeNpc(
            NpcState npc
    ) {
        NpcIdentity identity =
                npc.identity();

        SimulationPosition position =
                npc.position();

        NpcPersonality personality =
                npc.personality();

        return String.join(
                "\t",
                "NPC",
                Long.toString(
                        npc.id()
                                .value()
                ),
                escape(
                        identity.givenName()
                ),
                escape(
                        identity.familyName()
                ),
                identity.sex()
                        .name(),
                Integer.toString(
                        identity.birthYear()
                ),
                npc.lifeState()
                        .name(),
                npc.simulationLevel()
                        .name(),
                escape(
                        position.dimension()
                ),
                Double.toString(
                        position.x()
                ),
                Double.toString(
                        position.y()
                ),
                Double.toString(
                        position.z()
                ),
                Double.toString(
                        personality.courage()
                ),
                Double.toString(
                        personality.ambition()
                ),
                Double.toString(
                        personality.compassion()
                ),
                Double.toString(
                        personality.honor()
                ),
                Double.toString(
                        personality.patience()
                ),
                Double.toString(
                        personality.sociability()
                )
        );
    }

    private static NpcState decodeNpc(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 18) {
            throw new IllegalArgumentException(
                    "Expected 18 NPC columns, got "
                            + p.length
            );
        }

        return new NpcState(
                new NpcIdentity(
                        new NpcId(
                                Long.parseLong(
                                        p[1]
                                )
                        ),
                        unescape(
                                p[2]
                        ),
                        unescape(
                                p[3]
                        ),
                        NpcSex.valueOf(
                                p[4]
                        ),
                        Integer.parseInt(
                                p[5]
                        )
                ),
                new SimulationPosition(
                        unescape(
                                p[8]
                        ),
                        Double.parseDouble(
                                p[9]
                        ),
                        Double.parseDouble(
                                p[10]
                        ),
                        Double.parseDouble(
                                p[11]
                        )
                ),
                new NpcPersonality(
                        Double.parseDouble(
                                p[12]
                        ),
                        Double.parseDouble(
                                p[13]
                        ),
                        Double.parseDouble(
                                p[14]
                        ),
                        Double.parseDouble(
                                p[15]
                        ),
                        Double.parseDouble(
                                p[16]
                        ),
                        Double.parseDouble(
                                p[17]
                        )
                ),
                SimulationLevel.valueOf(
                        p[7]
                ),
                NpcLifeState.valueOf(
                        p[6]
                )
        );
    }

    private static NpcState decodeNpcV1(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 12) {
            throw new IllegalArgumentException(
                    "Expected 12 version-1 NPC columns, got "
                            + p.length
            );
        }

        return new NpcState(
                new NpcIdentity(
                        new NpcId(
                                Long.parseLong(
                                        p[0]
                                )
                        ),
                        unescape(
                                p[1]
                        ),
                        unescape(
                                p[2]
                        ),
                        NpcSex.valueOf(
                                p[3]
                        ),
                        Integer.parseInt(
                                p[4]
                        )
                ),
                new SimulationPosition(
                        unescape(
                                p[7]
                        ),
                        Double.parseDouble(
                                p[8]
                        ),
                        Double.parseDouble(
                                p[9]
                        ),
                        Double.parseDouble(
                                p[10]
                        )
                ),
                NpcPersonality.NEUTRAL,
                SimulationLevel.valueOf(
                        p[6]
                ),
                NpcLifeState.valueOf(
                        p[5]
                )
        );
    }

    // ---------------------------------------------------------------------
    // Relationship persistence
    // ---------------------------------------------------------------------

    private static String encodeRelationship(
            NpcRelationship relationship
    ) {
        return String.join(
                "\t",
                "REL",
                Long.toString(
                        relationship.subject()
                                .value()
                ),
                Long.toString(
                        relationship.target()
                                .value()
                ),
                Double.toString(
                        relationship.affection()
                ),
                Double.toString(
                        relationship.trust()
                ),
                Double.toString(
                        relationship.respect()
                ),
                Double.toString(
                        relationship.fear()
                ),
                Double.toString(
                        relationship.familiarity()
                )
        );
    }

    private static NpcRelationship decodeRelationship(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 8) {
            throw new IllegalArgumentException(
                    "Expected 8 relationship columns, got "
                            + p.length
            );
        }

        return new NpcRelationship(
                new NpcId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                new NpcId(
                        Long.parseLong(
                                p[2]
                        )
                ),
                Double.parseDouble(
                        p[3]
                ),
                Double.parseDouble(
                        p[4]
                ),
                Double.parseDouble(
                        p[5]
                ),
                Double.parseDouble(
                        p[6]
                ),
                Double.parseDouble(
                        p[7]
                )
        );
    }

    // ---------------------------------------------------------------------
    // Belief persistence
    // ---------------------------------------------------------------------

    private static String encodeBelief(
            NpcBelief belief
    ) {
        return String.join(
                "\t",
                "BELIEF",
                Long.toString(
                        belief.owner()
                                .value()
                ),
                escape(
                        belief.factKey()
                ),
                escape(
                        belief.value()
                ),
                Double.toString(
                        belief.confidence()
                ),
                belief.sourceNpc()
                        == null
                        ? ""
                        : Long.toString(
                        belief.sourceNpc()
                                .value()
                ),
                Long.toString(
                        belief.learnedTick()
                )
        );
    }

    private static NpcBelief decodeBelief(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 7) {
            throw new IllegalArgumentException(
                    "Expected 7 belief columns, got "
                            + p.length
            );
        }

        return new NpcBelief(
                new NpcId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                unescape(
                        p[2]
                ),
                unescape(
                        p[3]
                ),
                Double.parseDouble(
                        p[4]
                ),
                p[5].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                p[5]
                        )
                ),
                Long.parseLong(
                        p[6]
                )
        );
    }

    // ---------------------------------------------------------------------
    // Memory persistence
    // ---------------------------------------------------------------------

    private static String encodeMemory(
            NpcMemory memory
    ) {
        return String.join(
                "\t",
                "MEM",
                Long.toString(
                        memory.id()
                                .value()
                ),
                Long.toString(
                        memory.owner()
                                .value()
                ),
                memory.type()
                        .name(),
                escape(
                        memory.summary()
                ),
                Double.toString(
                        memory.importance()
                ),
                memory.relatedNpc()
                        == null
                        ? ""
                        : Long.toString(
                        memory.relatedNpc()
                                .value()
                ),
                memory.factKey()
                        == null
                        ? ""
                        : escape(
                        memory.factKey()
                ),
                Long.toString(
                        memory.createdTick()
                )
        );
    }

    private static NpcMemory decodeMemory(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 9) {
            throw new IllegalArgumentException(
                    "Expected 9 memory columns, got "
                            + p.length
            );
        }

        return new NpcMemory(
                new NpcMemoryId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                new NpcId(
                        Long.parseLong(
                                p[2]
                        )
                ),
                NpcMemoryType.valueOf(
                        p[3]
                ),
                unescape(
                        p[4]
                ),
                Double.parseDouble(
                        p[5]
                ),
                p[6].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                p[6]
                        )
                ),
                p[7].isEmpty()
                        ? null
                        : unescape(
                        p[7]
                ),
                Long.parseLong(
                        p[8]
                )
        );
    }

    // ---------------------------------------------------------------------
    // World event persistence
    // ---------------------------------------------------------------------

    private static String encodeEvent(
            WorldEvent event
    ) {
        return String.join(
                "\t",
                "EVENT",
                Long.toString(
                        event.id()
                                .value()
                ),
                event.type()
                        .name(),
                escape(
                        event.summary()
                ),
                escape(
                        event.position()
                                .dimension()
                ),
                Double.toString(
                        event.position()
                                .x()
                ),
                Double.toString(
                        event.position()
                                .y()
                ),
                Double.toString(
                        event.position()
                                .z()
                ),
                Long.toString(
                        event.occurredTick()
                ),
                Double.toString(
                        event.importance()
                ),
                event.actorNpc()
                        == null
                        ? ""
                        : Long.toString(
                        event.actorNpc()
                                .value()
                ),
                event.subjectNpc()
                        == null
                        ? ""
                        : Long.toString(
                        event.subjectNpc()
                                .value()
                ),
                event.factKey()
                        == null
                        ? ""
                        : escape(
                        event.factKey()
                ),
                event.factValue()
                        == null
                        ? ""
                        : escape(
                        event.factValue()
                )
        );
    }

    private static WorldEvent decodeEvent(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 14) {
            throw new IllegalArgumentException(
                    "Expected 14 event columns, got "
                            + p.length
            );
        }

        return new WorldEvent(
                new WorldEventId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                WorldEventType.valueOf(
                        p[2]
                ),
                unescape(
                        p[3]
                ),
                new SimulationPosition(
                        unescape(
                                p[4]
                        ),
                        Double.parseDouble(
                                p[5]
                        ),
                        Double.parseDouble(
                                p[6]
                        ),
                        Double.parseDouble(
                                p[7]
                        )
                ),
                Long.parseLong(
                        p[8]
                ),
                Double.parseDouble(
                        p[9]
                ),
                p[10].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                p[10]
                        )
                ),
                p[11].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                p[11]
                        )
                ),
                p[12].isEmpty()
                        ? null
                        : unescape(
                        p[12]
                ),
                p[13].isEmpty()
                        ? null
                        : unescape(
                        p[13]
                )
        );
    }

    // ---------------------------------------------------------------------
    // TSV escaping
    // ---------------------------------------------------------------------

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

                    default -> {

                        result.append(
                                '\\'
                        );

                        result.append(
                                character
                        );
                    }
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
}
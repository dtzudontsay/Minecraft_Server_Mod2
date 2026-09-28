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
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;

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
            7;

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

            int version =
                    Integer.parseInt(
                            header.substring(
                                    HEADER_PREFIX.length()
                            )
                    );

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

        while (
                (line = reader.readLine())
                        != null
        ) {

            if (line.isBlank()) {
                continue;
            }

            registry.registerLoaded(
                    decodeNpcV1(
                            line
                    )
            );
        }
    }

    private static void loadStructured(
            BufferedReader reader,
            SimulationClock clock,
            NpcRegistry registry,
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

        while (
                (line = reader.readLine())
                        != null
        ) {

            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith(
                    "CLOCK\t"
            )) {

                if (version >= 6) {

                    clock.setTick(
                            Long.parseLong(
                                    line.substring(
                                            6
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

            } else if (
                    line.startsWith(
                            "NEED\t"
                    )
                            && version >= 6
            ) {

                decodeNeeds(
                        line,
                        needs
                );

            } else if (
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

            } else if (line.startsWith(
                    "REL\t"
            )) {

                relationships.registerLoaded(
                        decodeRelationship(
                                line
                        )
                );

            } else if (line.startsWith(
                    "BELIEF\t"
            )) {

                knowledge.registerLoaded(
                        decodeBelief(
                                line
                        )
                );

            } else if (
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

            } else if (
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

            } else if (
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

            } else {

                throw new IOException(
                        "Unexpected record in version "
                                + version
                                + ": "
                                + line
                );
            }
        }
    }

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

    private static String encodeGoal(
            NpcGoal goal
    ) {

        SimulationPosition target =
                goal.targetPosition();

        return String.join(
                "\t",
                "GOAL",
                Long.toString(
                        goal.id().value()
                ),
                Long.toString(
                        goal.owner().value()
                ),
                goal.type().name(),
                goal.status().name(),
                escape(
                        goal.description()
                ),
                Double.toString(
                        goal.priority()
                ),
                Long.toString(
                        goal.createdTick()
                ),
                goal.targetNpc() == null
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
                goal.factKey() == null
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
                p[13].isEmpty()
                        ? null
                        : unescape(
                        p[13]
                ),
                NpcGoalStatus.valueOf(
                        p[4]
                )
        );
    }

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
                        npc.id().value()
                ),
                escape(
                        identity.givenName()
                ),
                escape(
                        identity.familyName()
                ),
                identity.sex().name(),
                Integer.toString(
                        identity.birthYear()
                ),
                npc.lifeState().name(),
                npc.simulationLevel().name(),
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

    private static String encodeRelationship(
            NpcRelationship relationship
    ) {

        return String.join(
                "\t",
                "REL",
                Long.toString(
                        relationship.subject().value()
                ),
                Long.toString(
                        relationship.target().value()
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

    private static String encodeBelief(
            NpcBelief belief
    ) {

        return String.join(
                "\t",
                "BELIEF",
                Long.toString(
                        belief.owner().value()
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
                belief.sourceNpc() == null
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

    private static String encodeMemory(
            NpcMemory memory
    ) {

        return String.join(
                "\t",
                "MEM",
                Long.toString(
                        memory.id().value()
                ),
                Long.toString(
                        memory.owner().value()
                ),
                memory.type().name(),
                escape(
                        memory.summary()
                ),
                Double.toString(
                        memory.importance()
                ),
                memory.relatedNpc() == null
                        ? ""
                        : Long.toString(
                        memory.relatedNpc()
                                .value()
                ),
                memory.factKey() == null
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

    private static String encodeEvent(
            WorldEvent event
    ) {

        return String.join(
                "\t",
                "EVENT",
                Long.toString(
                        event.id().value()
                ),
                event.type().name(),
                escape(
                        event.summary()
                ),
                escape(
                        event.position().dimension()
                ),
                Double.toString(
                        event.position().x()
                ),
                Double.toString(
                        event.position().y()
                ),
                Double.toString(
                        event.position().z()
                ),
                Long.toString(
                        event.occurredTick()
                ),
                Double.toString(
                        event.importance()
                ),
                event.actorNpc() == null
                        ? ""
                        : Long.toString(
                        event.actorNpc().value()
                ),
                event.subjectNpc() == null
                        ? ""
                        : Long.toString(
                        event.subjectNpc().value()
                ),
                event.factKey() == null
                        ? ""
                        : escape(
                        event.factKey()
                ),
                event.factValue() == null
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

    private static String escape(
            String value
    ) {

        return value
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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
                            result.append('\t');

                    case 'n' ->
                            result.append('\n');

                    case 'r' ->
                            result.append('\r');

                    case '\\' ->
                            result.append('\\');

                    default -> {
                        result.append('\\');
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
            result.append('\\');
        }

        return result.toString();
    }
}
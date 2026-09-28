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
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class NpcPersistence {

    private static final String HEADER_V1 =
            "KNOWNWORLD_NPCS\t1";

    private static final String HEADER_V2 =
            "KNOWNWORLD_NPCS\t2";

    private static final String HEADER_V3 =
            "KNOWNWORLD_NPCS\t3";

    private static final String HEADER_V4 =
            "KNOWNWORLD_NPCS\t4";

    private static final String HEADER_V5 =
            "KNOWNWORLD_NPCS\t5";

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
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            NpcKnowledgeManager knowledge,
            NpcMemoryManager memories,
            NpcGoalManager goals,
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
                    HEADER_V5
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

        } catch (IOException atomicMoveFailure) {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    public void loadInto(
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            NpcKnowledgeManager knowledge,
            NpcMemoryManager memories,
            NpcGoalManager goals,
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

            if (HEADER_V1.equals(
                    header
            )) {

                loadVersion1(
                        reader,
                        registry
                );

                return;
            }

            if (HEADER_V2.equals(
                    header
            )) {

                loadStructured(
                        reader,
                        registry,
                        relationships,
                        knowledge,
                        null,
                        null,
                        null,
                        2
                );

                return;
            }

            if (HEADER_V3.equals(
                    header
            )) {

                loadStructured(
                        reader,
                        registry,
                        relationships,
                        knowledge,
                        memories,
                        null,
                        null,
                        3
                );

                return;
            }

            if (HEADER_V4.equals(
                    header
            )) {

                loadStructured(
                        reader,
                        registry,
                        relationships,
                        knowledge,
                        memories,
                        null,
                        events,
                        4
                );

                return;
            }

            if (HEADER_V5.equals(
                    header
            )) {

                loadStructured(
                        reader,
                        registry,
                        relationships,
                        knowledge,
                        memories,
                        goals,
                        events,
                        5
                );

                return;
            }

            throw new IOException(
                    "Unsupported NPC save format: "
                            + header
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
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            NpcKnowledgeManager knowledge,
            NpcMemoryManager memories,
            NpcGoalManager goals,
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
                        "NPC\t"
                )) {

                    registry.registerLoaded(
                            decodeNpc(
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
                                && memories != null
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
                                && goals != null
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
                                && events != null
                ) {

                    events.registerLoaded(
                            decodeEvent(
                                    line
                            )
                    );

                } else {

                    throw new IllegalArgumentException(
                            "Unknown record type"
                    );
                }

            } catch (RuntimeException exception) {

                throw new IOException(
                        "Invalid version "
                                + version
                                + " NPC data at line "
                                + lineNumber,
                        exception
                );
            }
        }
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

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 14) {

            throw new IllegalArgumentException(
                    "Expected 14 goal columns, got "
                            + parts.length
            );
        }

        NpcId targetNpc =
                parts[8].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                parts[8]
                        )
                );

        SimulationPosition targetPosition =
                parts[9].isEmpty()
                        ? null
                        : new SimulationPosition(
                        unescape(
                                parts[9]
                        ),
                        Double.parseDouble(
                                parts[10]
                        ),
                        Double.parseDouble(
                                parts[11]
                        ),
                        Double.parseDouble(
                                parts[12]
                        )
                );

        String factKey =
                parts[13].isEmpty()
                        ? null
                        : unescape(
                        parts[13]
                );

        return new NpcGoal(
                new NpcGoalId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                new NpcId(
                        Long.parseLong(
                                parts[2]
                        )
                ),
                NpcGoalType.valueOf(
                        parts[3]
                ),
                unescape(
                        parts[5]
                ),
                Double.parseDouble(
                        parts[6]
                ),
                Long.parseLong(
                        parts[7]
                ),
                targetNpc,
                targetPosition,
                factKey,
                NpcGoalStatus.valueOf(
                        parts[4]
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

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 18) {

            throw new IllegalArgumentException(
                    "Expected 18 NPC columns, got "
                            + parts.length
            );
        }

        NpcIdentity identity =
                new NpcIdentity(
                        new NpcId(
                                Long.parseLong(
                                        parts[1]
                                )
                        ),
                        unescape(
                                parts[2]
                        ),
                        unescape(
                                parts[3]
                        ),
                        NpcSex.valueOf(
                                parts[4]
                        ),
                        Integer.parseInt(
                                parts[5]
                        )
                );

        SimulationPosition position =
                new SimulationPosition(
                        unescape(
                                parts[8]
                        ),
                        Double.parseDouble(
                                parts[9]
                        ),
                        Double.parseDouble(
                                parts[10]
                        ),
                        Double.parseDouble(
                                parts[11]
                        )
                );

        NpcPersonality personality =
                new NpcPersonality(
                        Double.parseDouble(
                                parts[12]
                        ),
                        Double.parseDouble(
                                parts[13]
                        ),
                        Double.parseDouble(
                                parts[14]
                        ),
                        Double.parseDouble(
                                parts[15]
                        ),
                        Double.parseDouble(
                                parts[16]
                        ),
                        Double.parseDouble(
                                parts[17]
                        )
                );

        return new NpcState(
                identity,
                position,
                personality,
                SimulationLevel.valueOf(
                        parts[7]
                ),
                NpcLifeState.valueOf(
                        parts[6]
                )
        );
    }

    private static NpcState decodeNpcV1(
            String line
    ) {

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 12) {

            throw new IllegalArgumentException(
                    "Expected 12 columns, got "
                            + parts.length
            );
        }

        NpcIdentity identity =
                new NpcIdentity(
                        new NpcId(
                                Long.parseLong(
                                        parts[0]
                                )
                        ),
                        unescape(
                                parts[1]
                        ),
                        unescape(
                                parts[2]
                        ),
                        NpcSex.valueOf(
                                parts[3]
                        ),
                        Integer.parseInt(
                                parts[4]
                        )
                );

        SimulationPosition position =
                new SimulationPosition(
                        unescape(
                                parts[7]
                        ),
                        Double.parseDouble(
                                parts[8]
                        ),
                        Double.parseDouble(
                                parts[9]
                        ),
                        Double.parseDouble(
                                parts[10]
                        )
                );

        return new NpcState(
                identity,
                position,
                NpcPersonality.NEUTRAL,
                SimulationLevel.valueOf(
                        parts[6]
                ),
                NpcLifeState.valueOf(
                        parts[5]
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

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 8) {

            throw new IllegalArgumentException(
                    "Expected 8 relationship columns, got "
                            + parts.length
            );
        }

        return new NpcRelationship(
                new NpcId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                new NpcId(
                        Long.parseLong(
                                parts[2]
                        )
                ),
                Double.parseDouble(
                        parts[3]
                ),
                Double.parseDouble(
                        parts[4]
                ),
                Double.parseDouble(
                        parts[5]
                ),
                Double.parseDouble(
                        parts[6]
                ),
                Double.parseDouble(
                        parts[7]
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

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 7) {

            throw new IllegalArgumentException(
                    "Expected 7 belief columns, got "
                            + parts.length
            );
        }

        NpcId sourceNpc =
                parts[5].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                parts[5]
                        )
                );

        return new NpcBelief(
                new NpcId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                unescape(
                        parts[2]
                ),
                unescape(
                        parts[3]
                ),
                Double.parseDouble(
                        parts[4]
                ),
                sourceNpc,
                Long.parseLong(
                        parts[6]
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

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 9) {

            throw new IllegalArgumentException(
                    "Expected 9 memory columns, got "
                            + parts.length
            );
        }

        NpcId relatedNpc =
                parts[6].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                parts[6]
                        )
                );

        String factKey =
                parts[7].isEmpty()
                        ? null
                        : unescape(
                        parts[7]
                );

        return new NpcMemory(
                new NpcMemoryId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                new NpcId(
                        Long.parseLong(
                                parts[2]
                        )
                ),
                NpcMemoryType.valueOf(
                        parts[3]
                ),
                unescape(
                        parts[4]
                ),
                Double.parseDouble(
                        parts[5]
                ),
                relatedNpc,
                factKey,
                Long.parseLong(
                        parts[8]
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
                event.actorNpc() == null
                        ? ""
                        : Long.toString(
                        event.actorNpc()
                                .value()
                ),
                event.subjectNpc() == null
                        ? ""
                        : Long.toString(
                        event.subjectNpc()
                                .value()
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

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 14) {

            throw new IllegalArgumentException(
                    "Expected 14 event columns, got "
                            + parts.length
            );
        }

        NpcId actor =
                parts[10].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                parts[10]
                        )
                );

        NpcId subject =
                parts[11].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                parts[11]
                        )
                );

        String factKey =
                parts[12].isEmpty()
                        ? null
                        : unescape(
                        parts[12]
                );

        String factValue =
                parts[13].isEmpty()
                        ? null
                        : unescape(
                        parts[13]
                );

        return new WorldEvent(
                new WorldEventId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                WorldEventType.valueOf(
                        parts[2]
                ),
                unescape(
                        parts[3]
                ),
                new SimulationPosition(
                        unescape(
                                parts[4]
                        ),
                        Double.parseDouble(
                                parts[5]
                        ),
                        Double.parseDouble(
                                parts[6]
                        ),
                        Double.parseDouble(
                                parts[7]
                        )
                ),
                Long.parseLong(
                        parts[8]
                ),
                Double.parseDouble(
                        parts[9]
                ),
                actor,
                subject,
                factKey,
                factValue
        );
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
                int index = 0;
                index < value.length();
                index++
        ) {

            char character =
                    value.charAt(
                            index
                    );

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
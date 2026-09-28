package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcIdentity;
import dev.dtzudontsay.knownworld.simulation.npc.NpcLifeState;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcBelief;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
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
            NpcKnowledgeManager knowledge
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
                    HEADER_V2
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
                        encodeNpcV2(
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
            NpcKnowledgeManager knowledge
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

            if (!HEADER_V2.equals(
                    header
            )) {
                throw new IOException(
                        "Unsupported NPC save format: "
                                + header
                );
            }

            loadVersion2(
                    reader,
                    registry,
                    relationships,
                    knowledge
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

    private static void loadVersion2(
            BufferedReader reader,
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            NpcKnowledgeManager knowledge
    ) throws IOException {

        String line;

        int lineNumber =
                1;

        /*
         * NPC records must load before relationships and beliefs
         * because those systems validate referenced NPC IDs.
         *
         * Save() always writes NPC records first.
         */
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
                            decodeNpcV2(
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

                } else {
                    throw new IllegalArgumentException(
                            "Unknown record type"
                    );
                }

            } catch (RuntimeException exception) {

                throw new IOException(
                        "Invalid version 2 NPC data at line "
                                + lineNumber,
                        exception
                );
            }
        }
    }

    private static String encodeNpcV2(
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

    private static NpcState decodeNpcV2(
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
package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcIdentity;
import dev.dtzudontsay.knownworld.simulation.npc.NpcLifeState;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

/**
 * Development persistence format for NPC state.
 *
 * Format version 1 is intentionally simple and human-readable.
 *
 * NPCs are stored independently from Minecraft entities so that a
 * simulated person can continue to exist while no physical entity
 * representing that person is loaded.
 */
public final class NpcPersistence {

    private static final String HEADER =
            "KNOWNWORLD_NPCS\t1";

    private static final String FILE_NAME =
            "npcs.tsv";

    private static final String TEMP_FILE_NAME =
            "npcs.tsv.tmp";

    private final Path directory;
    private final Path file;

    public NpcPersistence(
            Path directory
    ) {
        this.directory = directory;
        this.file =
                directory.resolve(
                        FILE_NAME
                );
    }

    /**
     * Saves the complete NPC registry.
     *
     * A temporary file is written first so that a partially written
     * save is less likely to destroy the previous valid save.
     */
    public void save(
            NpcRegistry registry
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
                    HEADER
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
                        encode(
                                npc
                        )
                );

                writer.newLine();
            }
        }

        /*
         * Prefer an atomic replacement when the underlying file
         * system supports it.
         *
         * Fall back to a normal replacement otherwise.
         */
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

    /**
     * Loads every persisted NPC into the supplied registry.
     */
    public void loadInto(
            NpcRegistry registry
    ) throws IOException {

        if (!Files.exists(file)) {
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

            if (!HEADER.equals(header)) {
                throw new IOException(
                        "Unsupported NPC save format: "
                                + header
                );
            }

            String line;
            int lineNumber = 1;

            while (
                    (line = reader.readLine())
                            != null
            ) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {
                    NpcState npc =
                            decode(
                                    line
                            );

                    registry.registerLoaded(
                            npc
                    );

                } catch (RuntimeException exception) {

                    throw new IOException(
                            "Invalid NPC data at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    private static String encode(
            NpcState npc
    ) {
        NpcIdentity identity =
                npc.identity();

        SimulationPosition position =
                npc.position();

        return String.join(
                "\t",

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

                /*
                 * Reserved extension column.
                 *
                 * Keeping this column in format version 1 gives us
                 * somewhere to add small metadata later without
                 * immediately changing the entire format.
                 */
                ""
        );
    }

    private static NpcState decode(
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

        NpcId id =
                new NpcId(
                        Long.parseLong(
                                parts[0]
                        )
                );

        String givenName =
                unescape(
                        parts[1]
                );

        String familyName =
                unescape(
                        parts[2]
                );

        NpcSex sex =
                NpcSex.valueOf(
                        parts[3]
                );

        int birthYear =
                Integer.parseInt(
                        parts[4]
                );

        NpcLifeState lifeState =
                NpcLifeState.valueOf(
                        parts[5]
                );

        SimulationLevel simulationLevel =
                SimulationLevel.valueOf(
                        parts[6]
                );

        String dimension =
                unescape(
                        parts[7]
                );

        double x =
                Double.parseDouble(
                        parts[8]
                );

        double y =
                Double.parseDouble(
                        parts[9]
                );

        double z =
                Double.parseDouble(
                        parts[10]
                );

        /*
         * parts[11] is currently reserved.
         */

        NpcIdentity identity =
                new NpcIdentity(
                        id,
                        givenName,
                        familyName,
                        sex,
                        birthYear
                );

        SimulationPosition position =
                new SimulationPosition(
                        dimension,
                        x,
                        y,
                        z
                );

        return new NpcState(
                identity,
                position,
                simulationLevel,
                lifeState
        );
    }

    /**
     * Escapes characters that would otherwise interfere with the
     * tab-separated save format.
     */
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

    /**
     * Reverses {@link #escape(String)}.
     */
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

        /*
         * Preserve a trailing backslash rather than silently losing
         * data from a malformed/legacy value.
         */
        if (escaped) {
            result.append(
                    '\\'
            );
        }

        return result.toString();
    }
}
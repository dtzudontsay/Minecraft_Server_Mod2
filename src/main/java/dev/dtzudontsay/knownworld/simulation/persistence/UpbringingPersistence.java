package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.formation.UpbringingManager;
import dev.dtzudontsay.knownworld.simulation.npc.formation.UpbringingRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class UpbringingPersistence {

    private static final String HEADER =
            "KNOWNWORLD_UPBRINGING\t1";

    private static final String FILE_NAME =
            "upbringing.tsv";

    private static final String TEMP_FILE_NAME =
            "upbringing.tsv.tmp";

    private final Path directory;

    private final Path file;

    public UpbringingPersistence(
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
            UpbringingManager manager
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
                    UpbringingRecord record :
                    manager.all()
                            .stream()
                            .sorted(
                                    Comparator.comparingLong(
                                            value ->
                                                    value.child()
                                                            .value()
                                    )
                            )
                            .toList()
            ) {

                writer.write(
                        encode(
                                record
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

        } catch (
                IOException exception
        ) {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    public void loadInto(
            UpbringingManager manager
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

            if (!HEADER.equals(
                    header
            )) {

                throw new IOException(
                        "Unsupported upbringing save format: "
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

                    manager.registerLoaded(
                            decode(
                                    line
                            )
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid upbringing data at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    private static String encode(
            UpbringingRecord record
    ) {

        return String.join(
                "\t",
                "UPBRINGING",
                Long.toString(
                        record.child()
                                .value()
                ),
                escape(
                        record.birthLocationId()
                ),
                escape(
                        record.upbringingLocationId()
                ),
                nullableNpc(
                        record.guardian()
                ),
                nullableNpc(
                        record.fosterParent()
                ),
                Integer.toString(
                        record.lastFormationAge()
                )
        );
    }

    private static UpbringingRecord decode(
            String line
    ) {

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 7) {

            throw new IllegalArgumentException(
                    "Expected 7 UPBRINGING columns"
            );
        }

        if (!"UPBRINGING".equals(
                parts[0]
        )) {

            throw new IllegalArgumentException(
                    "Unknown upbringing record type "
                            + parts[0]
            );
        }

        return new UpbringingRecord(
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
                parseNpc(
                        parts[4]
                ),
                parseNpc(
                        parts[5]
                ),
                Integer.parseInt(
                        parts[6]
                )
        );
    }

    private static String nullableNpc(
            NpcId npc
    ) {

        return npc == null
                ? ""
                : Long.toString(
                npc.value()
        );
    }

    private static NpcId parseNpc(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return new NpcId(
                Long.parseLong(
                        value
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
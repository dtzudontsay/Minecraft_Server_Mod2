package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.relationship.DynastyRelationship;
import dev.dtzudontsay.knownworld.simulation.social.relationship.DynastyRelationshipManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class DynastyRelationshipPersistence {

    private static final String HEADER =
            "KNOWNWORLD_DYNASTY_RELATIONSHIPS\t1";

    private static final String FILE_NAME =
            "dynasty_relationships.tsv";

    private static final String TEMP_FILE_NAME =
            "dynasty_relationships.tsv.tmp";

    private final Path directory;

    private final Path file;

    public DynastyRelationshipPersistence(
            Path directory
    ) {

        this.directory =
                directory;

        this.file =
                directory.resolve(
                        FILE_NAME
                );
    }

    public void loadInto(
            DynastyRelationshipManager manager
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
                        "Unsupported dynasty relationship format: "
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

                    String[] parts =
                            line.split(
                                    "\t",
                                    -1
                            );

                    if (parts.length != 9
                            || !"REL".equals(
                            parts[0]
                    )) {

                        throw new IllegalArgumentException(
                                "Expected REL record with 9 columns"
                        );
                    }

                    manager.registerLoaded(
                            new DynastyRelationship(
                                    new DynastyId(
                                            Long.parseLong(
                                                    parts[1]
                                            )
                                    ),
                                    new DynastyId(
                                            Long.parseLong(
                                                    parts[2]
                                            )
                                    ),
                                    Integer.parseInt(
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
                                    ),
                                    Double.parseDouble(
                                            parts[8]
                                    )
                            )
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid dynasty relationship at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    public void save(
            DynastyRelationshipManager manager
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
                    DynastyRelationship relationship :
                    manager.all()
                            .stream()
                            .sorted(
                                    Comparator
                                            .comparing(
                                                    DynastyRelationship::subject
                                            )
                                            .thenComparing(
                                                    DynastyRelationship::target
                                            )
                            )
                            .toList()
            ) {

                writer.write(
                        String.join(
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
                                Integer.toString(
                                        relationship.authoredVersion()
                                ),
                                Double.toString(
                                        relationship.affinity()
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
}

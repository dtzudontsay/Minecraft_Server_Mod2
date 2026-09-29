package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.family.GenealogyManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class GenealogyPersistence {

    private static final String HEADER =
            "KNOWNWORLD_GENEALOGY\t1";

    private static final String FILE_NAME =
            "genealogy.tsv";

    private static final String TEMP_FILE_NAME =
            "genealogy.tsv.tmp";

    private final Path directory;

    private final Path file;

    public GenealogyPersistence(
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
            GenealogyManager genealogy
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
                    Parentage parentage :
                    genealogy.all()
            ) {

                writer.write(
                        encode(
                                parentage
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
            GenealogyManager genealogy
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
                        "Unsupported genealogy save format: "
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

                    genealogy.registerLoaded(
                            decode(
                                    line
                            )
                    );

                } catch (RuntimeException exception) {

                    throw new IOException(
                            "Invalid genealogy data at line "
                                    + lineNumber
                                    + ": "
                                    + line,
                            exception
                    );
                }
            }
        }
    }

    private static String encode(
            Parentage parentage
    ) {
        return String.join(
                "\t",
                "PARENTAGE",
                Long.toString(
                        parentage.child()
                                .value()
                ),
                parentage.mother()
                        == null
                        ? ""
                        : Long.toString(
                        parentage.mother()
                                .value()
                ),
                parentage.father()
                        == null
                        ? ""
                        : Long.toString(
                        parentage.father()
                                .value()
                ),
                Long.toString(
                        parentage.birthTick()
                )
        );
    }

    private static Parentage decode(
            String line
    ) {
        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 5
                || !"PARENTAGE".equals(
                parts[0]
        )) {

            throw new IllegalArgumentException(
                    "Expected PARENTAGE record with 5 columns"
            );
        }

        NpcId mother =
                parts[2].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                parts[2]
                        )
                );

        NpcId father =
                parts[3].isEmpty()
                        ? null
                        : new NpcId(
                        Long.parseLong(
                                parts[3]
                        )
                );

        return new Parentage(
                new NpcId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                mother,
                father,
                Long.parseLong(
                        parts[4]
                )
        );
    }
}
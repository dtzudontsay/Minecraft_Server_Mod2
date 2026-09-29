package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.Pregnancy;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.PregnancyManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class PregnancyPersistence {

    private static final String HEADER =
            "KNOWNWORLD_PREGNANCIES\t1";

    private static final String FILE_NAME =
            "pregnancies.tsv";

    private static final String TEMP_FILE_NAME =
            "pregnancies.tsv.tmp";

    private final Path directory;

    private final Path file;

    public PregnancyPersistence(
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
            PregnancyManager manager
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
                    Pregnancy pregnancy :
                    manager.all()
            ) {

                writer.write(
                        String.join(
                                "\t",
                                "PREGNANCY",
                                Long.toString(
                                        pregnancy.mother()
                                                .value()
                                ),
                                Long.toString(
                                        pregnancy.father()
                                                .value()
                                ),
                                Long.toString(
                                        pregnancy.conceivedDay()
                                ),
                                Long.toString(
                                        pregnancy.dueDay()
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

        } catch (IOException exception) {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    public void loadInto(
            PregnancyManager manager
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
                        "Unsupported pregnancy save format"
                );
            }

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                if (line.isBlank()) {
                    continue;
                }

                String[] p =
                        line.split(
                                "\t",
                                -1
                        );

                if (p.length != 5
                        || !"PREGNANCY".equals(
                        p[0]
                )) {

                    throw new IOException(
                            "Invalid pregnancy record: "
                                    + line
                    );
                }

                manager.registerLoaded(
                        new Pregnancy(
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
                                Long.parseLong(
                                        p[3]
                                ),
                                Long.parseLong(
                                        p[4]
                                )
                        )
                );
            }
        }
    }
}
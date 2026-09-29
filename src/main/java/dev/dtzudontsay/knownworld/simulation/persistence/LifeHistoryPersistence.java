package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.LifeHistoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.NpcLifeRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LifeHistoryPersistence {

    private static final String HEADER =
            "KNOWNWORLD_LIFE_HISTORY\t1";

    private static final String FILE_NAME =
            "life_history.tsv";

    private static final String TEMP_FILE_NAME =
            "life_history.tsv.tmp";

    private final Path directory;

    private final Path file;

    public LifeHistoryPersistence(
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
            LifeHistoryManager manager
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
                    NpcLifeRecord record :
                    manager.all()
            ) {

                writer.write(
                        String.join(
                                "\t",
                                "LIFE",
                                Long.toString(
                                        record.owner()
                                                .value()
                                ),
                                Long.toString(
                                        record.birthDay()
                                ),
                                record.deathDay()
                                        == null
                                        ? ""
                                        : Long.toString(
                                        record.deathDay()
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
            LifeHistoryManager manager
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
                        "Unsupported life history save format"
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

                if (p.length != 4
                        || !"LIFE".equals(
                        p[0]
                )) {

                    throw new IOException(
                            "Invalid life history record: "
                                    + line
                    );
                }

                Long deathDay =
                        p[3].isEmpty()
                                ? null
                                : Long.parseLong(
                                p[3]
                        );

                manager.registerLoaded(
                        new NpcLifeRecord(
                                new NpcId(
                                        Long.parseLong(
                                                p[1]
                                        )
                                ),
                                Long.parseLong(
                                        p[2]
                                ),
                                deathDay
                        )
                );
            }
        }
    }
}
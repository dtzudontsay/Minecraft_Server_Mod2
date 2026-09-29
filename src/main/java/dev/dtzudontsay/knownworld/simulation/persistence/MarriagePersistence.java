package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.family.DynastyInheritanceRule;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageId;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageRecord;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageStatus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class MarriagePersistence {

    private static final String HEADER =
            "KNOWNWORLD_MARRIAGES\t1";

    private static final String FILE_NAME =
            "marriages.tsv";

    private static final String TEMP_FILE_NAME =
            "marriages.tsv.tmp";

    private final Path directory;

    private final Path file;

    public MarriagePersistence(
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
            MarriageManager marriages
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
                    MarriageRecord marriage :
                    marriages.all()
            ) {

                writer.write(
                        encode(
                                marriage
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
            MarriageManager marriages
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
                        "Unsupported marriage save format: "
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

                    marriages.registerLoaded(
                            decode(
                                    line
                            )
                    );

                } catch (RuntimeException exception) {

                    throw new IOException(
                            "Invalid marriage data at line "
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
            MarriageRecord marriage
    ) {
        return String.join(
                "\t",
                "MARRIAGE",
                Long.toString(
                        marriage.id()
                                .value()
                ),
                Long.toString(
                        marriage.first()
                                .value()
                ),
                Long.toString(
                        marriage.second()
                                .value()
                ),
                marriage.inheritanceRule()
                        .name(),
                Long.toString(
                        marriage.createdTick()
                ),
                marriage.status()
                        .name(),
                marriage.marriedTick()
                        == null
                        ? ""
                        : Long.toString(
                        marriage.marriedTick()
                ),
                marriage.endedTick()
                        == null
                        ? ""
                        : Long.toString(
                        marriage.endedTick()
                )
        );
    }

    private static MarriageRecord decode(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 9
                || !"MARRIAGE".equals(
                p[0]
        )) {

            throw new IllegalArgumentException(
                    "Expected MARRIAGE record with 9 columns"
            );
        }

        Long marriedTick =
                p[7].isEmpty()
                        ? null
                        : Long.parseLong(
                        p[7]
                );

        Long endedTick =
                p[8].isEmpty()
                        ? null
                        : Long.parseLong(
                        p[8]
                );

        return new MarriageRecord(
                new MarriageId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                new NpcId(
                        Long.parseLong(
                                p[2]
                        )
                ),
                new NpcId(
                        Long.parseLong(
                                p[3]
                        )
                ),
                DynastyInheritanceRule.valueOf(
                        p[4]
                ),
                Long.parseLong(
                        p[5]
                ),
                MarriageStatus.valueOf(
                        p[6]
                ),
                marriedTick,
                endedTick
        );
    }
}
package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.succession.ClaimId;
import dev.dtzudontsay.knownworld.simulation.social.succession.ClaimManager;
import dev.dtzudontsay.knownworld.simulation.social.succession.ClaimRecord;
import dev.dtzudontsay.knownworld.simulation.social.succession.ClaimStrength;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ClaimPersistence {

    private static final String HEADER =
            "KNOWNWORLD_CLAIMS\t1";

    private static final String FILE_NAME =
            "claims.tsv";

    private static final String TEMP_FILE_NAME =
            "claims.tsv.tmp";

    private final Path directory;

    private final Path file;

    public ClaimPersistence(
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
            ClaimManager claims
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
                    ClaimRecord claim :
                    claims.all()
            ) {

                writer.write(
                        String.join(
                                "\t",
                                "CLAIM",
                                Long.toString(
                                        claim.id()
                                                .value()
                                ),
                                Long.toString(
                                        claim.claimant()
                                                .value()
                                ),
                                Long.toString(
                                        claim.title()
                                                .value()
                                ),
                                claim.strength()
                                        .name(),
                                claim.inheritedFrom()
                                        == null
                                        ? ""
                                        : Long.toString(
                                        claim.inheritedFrom()
                                                .value()
                                ),
                                Long.toString(
                                        claim.createdTick()
                                ),
                                Boolean.toString(
                                        claim.active()
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
            ClaimManager claims
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
                        "Unsupported claim save format"
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

                if (p.length != 8
                        || !"CLAIM".equals(
                        p[0]
                )) {

                    throw new IOException(
                            "Invalid claim record: "
                                    + line
                    );
                }

                claims.registerLoaded(
                        new ClaimRecord(
                                new ClaimId(
                                        Long.parseLong(
                                                p[1]
                                        )
                                ),
                                new NpcId(
                                        Long.parseLong(
                                                p[2]
                                        )
                                ),
                                new TitleId(
                                        Long.parseLong(
                                                p[3]
                                        )
                                ),
                                ClaimStrength.valueOf(
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
                                ),
                                Boolean.parseBoolean(
                                        p[7]
                                )
                        )
                );
            }
        }
    }
}
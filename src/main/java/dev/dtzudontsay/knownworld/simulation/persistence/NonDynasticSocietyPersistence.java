package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSociety;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyManager;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyStatus;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyType;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

public final class NonDynasticSocietyPersistence {

    private static final String HEADER =
            "KNOWNWORLD_NON_DYNASTIC_SOCIETIES\t1";

    private static final String FILE_NAME =
            "non_dynastic_societies.tsv";

    private static final String TEMP_FILE_NAME =
            "non_dynastic_societies.tsv.tmp";

    private final Path directory;

    private final Path file;

    public NonDynasticSocietyPersistence(
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
            NonDynasticSocietyManager manager
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
                    NonDynasticSociety society :
                    manager.all()
            ) {

                writer.write(
                        String.join(
                                "\t",
                                "SOCIETY",
                                Long.toString(
                                        society.organizationId()
                                                .value()
                                ),
                                encode(
                                        society.authoredId()
                                ),
                                encode(
                                        society.name()
                                ),
                                society.type()
                                        .name(),
                                society.status()
                                        .name(),
                                encode(
                                        society.homeLocationId()
                                ),
                                encode(
                                        society.cultureId()
                                ),
                                encode(
                                        society.religionId()
                                ),
                                Boolean.toString(
                                        society.mobile()
                                ),
                                Integer.toString(
                                        society.populationEstimate()
                                ),
                                Double.toString(
                                        society.militaryStrength()
                                ),
                                Double.toString(
                                        society.cohesion()
                                ),
                                society.provenance()
                                        .name(),
                                encode(
                                        society.sourceNote()
                                ),
                                encode(
                                        society.primaryLeaderRole()
                                ),
                                encode(
                                        society.secondaryLeaderRole()
                                ),
                                npcOrZero(
                                        society.primaryLeader()
                                ),
                                npcOrZero(
                                        society.secondaryLeader()
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

    public void loadInto(
            NonDynasticSocietyManager manager
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
                        "Unsupported non-dynastic society format: "
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

                    if (parts.length != 19
                            || !"SOCIETY".equals(
                            parts[0]
                    )) {

                        throw new IllegalArgumentException(
                                "Expected SOCIETY record with 19 columns"
                        );
                    }

                    manager.registerLoaded(
                            new NonDynasticSociety(
                                    new OrganizationId(
                                            Long.parseLong(
                                                    parts[1]
                                            )
                                    ),
                                    decode(
                                            parts[2]
                                    ),
                                    requireDecoded(
                                            parts[3],
                                            "society name"
                                    ),
                                    NonDynasticSocietyType.valueOf(
                                            parts[4]
                                    ),
                                    NonDynasticSocietyStatus.valueOf(
                                            parts[5]
                                    ),
                                    decode(
                                            parts[6]
                                    ),
                                    decode(
                                            parts[7]
                                    ),
                                    decode(
                                            parts[8]
                                    ),
                                    Boolean.parseBoolean(
                                            parts[9]
                                    ),
                                    Integer.parseInt(
                                            parts[10]
                                    ),
                                    Double.parseDouble(
                                            parts[11]
                                    ),
                                    Double.parseDouble(
                                            parts[12]
                                    ),
                                    ReferenceProvenance.valueOf(
                                            parts[13]
                                    ),
                                    decodeOrEmpty(
                                            parts[14]
                                    ),
                                    decode(
                                            parts[15]
                                    ),
                                    decode(
                                            parts[16]
                                    ),
                                    npcOrNull(
                                            parts[17]
                                    ),
                                    npcOrNull(
                                            parts[18]
                                    )
                            )
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid non-dynastic society at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    private static String npcOrZero(
            NpcId npc
    ) {

        return npc == null
                ? "0"
                : Long.toString(
                npc.value()
        );
    }

    private static NpcId npcOrNull(
            String value
    ) {

        long id =
                Long.parseLong(
                        value
                );

        return id <= 0
                ? null
                : new NpcId(
                id
        );
    }

    private static String encode(
            String value
    ) {

        if (value == null
                || value.isEmpty()) {

            return "";
        }

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }

    private static String decode(
            String value
    ) {

        if (value == null
                || value.isEmpty()) {

            return null;
        }

        return new String(
                Base64.getUrlDecoder()
                        .decode(
                                value
                        ),
                StandardCharsets.UTF_8
        );
    }

    private static String decodeOrEmpty(
            String value
    ) {

        String decoded =
                decode(
                        value
                );

        return decoded == null
                ? ""
                : decoded;
    }

    private static String requireDecoded(
            String value,
            String description
    ) {

        String decoded =
                decode(
                        value
                );

        if (decoded == null
                || decoded.isBlank()) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be empty"
            );
        }

        return decoded;
    }
}
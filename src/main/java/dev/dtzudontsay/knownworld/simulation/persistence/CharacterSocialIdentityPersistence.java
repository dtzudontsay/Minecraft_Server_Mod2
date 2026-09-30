package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentity;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentityManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class CharacterSocialIdentityPersistence {

    private static final String HEADER =
            "KNOWNWORLD_CHARACTER_SOCIAL_IDENTITY\t1";

    private static final String FILE_NAME =
            "character_social_identity.tsv";

    private static final String TEMP_FILE_NAME =
            "character_social_identity.tsv.tmp";

    private final Path directory;

    private final Path file;

    public CharacterSocialIdentityPersistence(
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
            CharacterSocialIdentityManager manager
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
                    CharacterSocialIdentity identity :
                    manager.all()
            ) {

                writer.write(
                        String.join(
                                "\t",
                                "IDENTITY",
                                Long.toString(
                                        identity.npc()
                                                .value()
                                ),
                                dynastyOrZero(
                                        identity.birthDynasty()
                                ),
                                dynastyOrZero(
                                        identity.currentDynasty()
                                ),
                                dynastyOrZero(
                                        identity.marriedIntoDynasty()
                                ),
                                dynastyOrZero(
                                        identity.legalFamilyDynasty()
                                ),
                                npcOrZero(
                                        identity.legalMother()
                                ),
                                npcOrZero(
                                        identity.legalFather()
                                ),
                                organizationOrZero(
                                        identity.householdOrganization()
                                ),
                                organizationOrZero(
                                        identity.houseOrganization()
                                ),
                                organizationOrZero(
                                        identity.primaryAllegianceOrganization()
                                ),
                                Double.toString(
                                        identity.allegianceStrength()
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
            CharacterSocialIdentityManager manager
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
                        "Unsupported character social identity format: "
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

                    if (parts.length != 12
                            || !"IDENTITY".equals(
                            parts[0]
                    )) {

                        throw new IllegalArgumentException(
                                "Expected IDENTITY record with 12 columns"
                        );
                    }

                    manager.registerLoaded(
                            new CharacterSocialIdentity(
                                    new NpcId(
                                            Long.parseLong(
                                                    parts[1]
                                            )
                                    ),
                                    dynastyOrNull(
                                            parts[2]
                                    ),
                                    dynastyOrNull(
                                            parts[3]
                                    ),
                                    dynastyOrNull(
                                            parts[4]
                                    ),
                                    dynastyOrNull(
                                            parts[5]
                                    ),
                                    npcOrNull(
                                            parts[6]
                                    ),
                                    npcOrNull(
                                            parts[7]
                                    ),
                                    organizationOrNull(
                                            parts[8]
                                    ),
                                    organizationOrNull(
                                            parts[9]
                                    ),
                                    organizationOrNull(
                                            parts[10]
                                    ),
                                    Double.parseDouble(
                                            parts[11]
                                    )
                            )
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid character social identity at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    private static String dynastyOrZero(
            DynastyId dynasty
    ) {

        return dynasty == null
                ? "0"
                : Long.toString(
                dynasty.value()
        );
    }

    private static DynastyId dynastyOrNull(
            String raw
    ) {

        long value =
                Long.parseLong(
                        raw
                );

        return value <= 0
                ? null
                : new DynastyId(
                value
        );
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
            String raw
    ) {

        long value =
                Long.parseLong(
                        raw
                );

        return value <= 0
                ? null
                : new NpcId(
                value
        );
    }

    private static String organizationOrZero(
            OrganizationId organization
    ) {

        return organization == null
                ? "0"
                : Long.toString(
                organization.value()
        );
    }

    private static OrganizationId organizationOrNull(
            String raw
    ) {

        long value =
                Long.parseLong(
                        raw
                );

        return value <= 0
                ? null
                : new OrganizationId(
                value
        );
    }
}
package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembership;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipManager;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipStatus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class OrganizationMembershipPersistence {

    private static final String HEADER =
            "KNOWNWORLD_ORGANIZATION_MEMBERSHIPS\t1";

    private static final String FILE_NAME =
            "organization_memberships.tsv";

    private static final String TEMP_FILE_NAME =
            "organization_memberships.tsv.tmp";

    private final Path directory;

    private final Path file;

    public OrganizationMembershipPersistence(
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
            OrganizationMembershipManager manager
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
                    OrganizationMembership membership :
                    manager.all()
            ) {

                String roles =
                        membership.roles()
                                .stream()
                                .sorted()
                                .collect(
                                        Collectors.joining(
                                                ","
                                        )
                                );

                writer.write(
                        String.join(
                                "\t",
                                "MEMBER",
                                Long.toString(
                                        membership.npc()
                                                .value()
                                ),
                                Long.toString(
                                        membership.organization()
                                                .value()
                                ),
                                Long.toString(
                                        membership.joinedTick()
                                ),
                                Long.toString(
                                        membership.leftTick()
                                ),
                                membership.status()
                                        .name(),
                                Double.toString(
                                        membership.loyalty()
                                ),
                                roles
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
            OrganizationMembershipManager manager
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
                        "Unsupported organization membership format: "
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

                    if (parts.length != 8
                            || !"MEMBER".equals(
                            parts[0]
                    )) {

                        throw new IllegalArgumentException(
                                "Expected MEMBER record with 8 columns"
                        );
                    }

                    Set<String> roles =
                            parts[7].isBlank()
                                    ? Set.of()
                                    : Arrays.stream(
                                            parts[7].split(
                                                    ","
                                            )
                                    )
                                    .filter(
                                            value ->
                                                    !value.isBlank()
                                    )
                                    .collect(
                                            Collectors.toSet()
                                    );

                    manager.registerLoaded(
                            new OrganizationMembership(
                                    new NpcId(
                                            Long.parseLong(
                                                    parts[1]
                                            )
                                    ),
                                    new OrganizationId(
                                            Long.parseLong(
                                                    parts[2]
                                            )
                                    ),
                                    Long.parseLong(
                                            parts[3]
                                    ),
                                    Long.parseLong(
                                            parts[4]
                                    ),
                                    OrganizationMembershipStatus.valueOf(
                                            parts[5]
                                    ),
                                    Double.parseDouble(
                                            parts[6]
                                    ),
                                    roles
                            )
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid organization membership at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }
}
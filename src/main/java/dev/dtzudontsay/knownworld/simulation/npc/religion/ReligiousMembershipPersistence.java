package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousInstitutionRuntimeManager;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousMembership;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousMembershipManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;

public final class ReligiousMembershipPersistence {

    private static final String HEADER =
            "KNOWNWORLD_RELIGIOUS_STATE\t1";

    private static final String FILE_NAME =
            "religious_state.tsv";

    private static final String TEMP_FILE_NAME =
            "religious_state.tsv.tmp";

    private final Path directory;

    private final Path file;

    public ReligiousMembershipPersistence(
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
            ReligiousInstitutionRuntimeManager institutions,
            ReligiousMembershipManager memberships
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
                    Map.Entry<String, OrganizationId> entry :
                    institutions.bindings()
                            .entrySet()
                            .stream()
                            .sorted(
                                    Map.Entry.comparingByKey()
                            )
                            .toList()
            ) {

                writer.write(
                        "BINDING\t"
                                + escape(
                                entry.getKey()
                        )
                                + "\t"
                                + entry.getValue()
                                .value()
                );

                writer.newLine();
            }

            for (
                    ReligiousMembership membership :
                    memberships.all()
                            .stream()
                            .sorted(
                                    Comparator.comparing(
                                            ReligiousMembership::npc
                                    )
                            )
                            .toList()
            ) {

                writer.write(
                        encodeMembership(
                                membership
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
            ReligiousInstitutionRuntimeManager institutions,
            ReligiousMembershipManager memberships
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
                        "Unsupported religious state format: "
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

                    if (line.startsWith(
                            "BINDING\t"
                    )) {

                        decodeBinding(
                                institutions,
                                line
                        );

                        continue;
                    }

                    if (line.startsWith(
                            "MEMBER\t"
                    )) {

                        memberships.registerLoaded(
                                decodeMembership(
                                        line
                                )
                        );

                        continue;
                    }

                    throw new IllegalArgumentException(
                            "Unknown religious-state record"
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid religious state at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    private static String encodeMembership(
            ReligiousMembership membership
    ) {

        return String.join(
                "\t",
                "MEMBER",
                Long.toString(
                        membership.npc()
                                .value()
                ),
                escape(
                        membership.orderId()
                ),
                Long.toString(
                        membership.organizationId()
                                .value()
                ),
                escape(
                        membership.roleId()
                ),
                Double.toString(
                        membership.commitment()
                ),
                Boolean.toString(
                        membership.active()
                )
        );
    }

    private static void decodeBinding(
            ReligiousInstitutionRuntimeManager institutions,
            String line
    ) {

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 3) {

            throw new IllegalArgumentException(
                    "Expected 3 BINDING columns"
            );
        }

        institutions.registerBinding(
                unescape(
                        parts[1]
                ),
                new OrganizationId(
                        Long.parseLong(
                                parts[2]
                        )
                )
        );
    }

    private static ReligiousMembership decodeMembership(
            String line
    ) {

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 7) {

            throw new IllegalArgumentException(
                    "Expected 7 MEMBER columns"
            );
        }

        return new ReligiousMembership(
                new NpcId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                unescape(
                        parts[2]
                ),
                new OrganizationId(
                        Long.parseLong(
                                parts[3]
                        )
                ),
                unescape(
                        parts[4]
                ),
                Double.parseDouble(
                        parts[5]
                ),
                Boolean.parseBoolean(
                        parts[6]
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
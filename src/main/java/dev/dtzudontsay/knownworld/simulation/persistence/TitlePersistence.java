package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleAssignment;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleDefinition;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleType;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TitlePersistence {

    private static final String HEADER =
            "KNOWNWORLD_TITLES\t1";

    private static final String FILE_NAME =
            "titles.tsv";

    private static final String TEMP_FILE_NAME =
            "titles.tsv.tmp";

    private final Path directory;

    private final Path file;

    public TitlePersistence(
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
            TitleManager titles
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
                    TitleDefinition title :
                    titles.allDefinitions()
            ) {
                writer.write(
                        encodeDefinition(
                                title
                        )
                );

                writer.newLine();
            }

            for (
                    TitleAssignment assignment :
                    titles.allAssignments()
            ) {
                writer.write(
                        encodeAssignment(
                                assignment
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
            TitleManager titles
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
                        "Unsupported title save format: "
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
                            "TITLE\t"
                    )) {

                        titles.registerLoadedDefinition(
                                decodeDefinition(
                                        line
                                )
                        );

                    } else if (line.startsWith(
                            "TITLE_ASSIGNMENT\t"
                    )) {

                        titles.registerLoadedAssignment(
                                decodeAssignment(
                                        line
                                )
                        );

                    } else {

                        throw new IllegalArgumentException(
                                "Unknown title record"
                        );
                    }

                } catch (RuntimeException exception) {

                    throw new IOException(
                            "Invalid title data at line "
                                    + lineNumber
                                    + ": "
                                    + line,
                            exception
                    );
                }
            }
        }
    }

    private static String encodeDefinition(
            TitleDefinition title
    ) {
        return String.join(
                "\t",
                "TITLE",
                Long.toString(
                        title.id()
                                .value()
                ),
                escape(
                        title.name()
                ),
                title.type()
                        .name(),
                Integer.toString(
                        title.authority()
                ),
                Boolean.toString(
                        title.exclusive()
                ),
                title.organization()
                        == null
                        ? ""
                        : Long.toString(
                        title.organization()
                                .value()
                ),
                title.settlement()
                        == null
                        ? ""
                        : Long.toString(
                        title.settlement()
                                .value()
                )
        );
    }

    private static TitleDefinition decodeDefinition(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 8) {

            throw new IllegalArgumentException(
                    "Expected 8 title columns, got "
                            + p.length
            );
        }

        OrganizationId organization =
                p[6].isEmpty()
                        ? null
                        : new OrganizationId(
                        Long.parseLong(
                                p[6]
                        )
                );

        SettlementId settlement =
                p[7].isEmpty()
                        ? null
                        : new SettlementId(
                        Long.parseLong(
                                p[7]
                        )
                );

        return new TitleDefinition(
                new TitleId(
                        Long.parseLong(
                                p[1]
                        )
                ),
                unescape(
                        p[2]
                ),
                TitleType.valueOf(
                        p[3]
                ),
                Integer.parseInt(
                        p[4]
                ),
                Boolean.parseBoolean(
                        p[5]
                ),
                organization,
                settlement
        );
    }

    private static String encodeAssignment(
            TitleAssignment assignment
    ) {
        return String.join(
                "\t",
                "TITLE_ASSIGNMENT",
                Long.toString(
                        assignment.title()
                                .value()
                ),
                Long.toString(
                        assignment.holder()
                                .value()
                ),
                Long.toString(
                        assignment.grantedTick()
                ),
                assignment.revokedTick()
                        == null
                        ? ""
                        : Long.toString(
                        assignment.revokedTick()
                )
        );
    }

    private static TitleAssignment decodeAssignment(
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        if (p.length != 5) {

            throw new IllegalArgumentException(
                    "Expected 5 title assignment columns, got "
                            + p.length
            );
        }

        Long revokedTick =
                p[4].isEmpty()
                        ? null
                        : Long.parseLong(
                        p[4]
                );

        return new TitleAssignment(
                new TitleId(
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
                revokedTick
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
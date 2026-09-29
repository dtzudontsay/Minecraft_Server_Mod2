package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.bootstrap.AuthoredIdRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class AuthoredIdPersistence {

    private static final String HEADER =
            "KNOWNWORLD_AUTHORED_IDS\t1";

    private static final String FILE_NAME =
            "authored_ids.tsv";

    private static final String TEMP_FILE_NAME =
            "authored_ids.tsv.tmp";

    private final Path directory;

    private final Path file;

    public AuthoredIdPersistence(
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
            AuthoredIdRegistry ids
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

            if (ids.scenarioId() != null) {

                writer.write(
                        "SCENARIO\t"
                                + ids.scenarioId()
                );

                writer.newLine();
            }

            for (
                    Map.Entry<String, NpcId> entry :
                    ids.npcs()
                            .entrySet()
            ) {

                writer.write(
                        "NPC\t"
                                + entry.getKey()
                                + "\t"
                                + entry.getValue()
                                .value()
                );

                writer.newLine();
            }

            for (
                    Map.Entry<String, SettlementId> entry :
                    ids.settlements()
                            .entrySet()
            ) {

                writer.write(
                        "SETTLEMENT\t"
                                + entry.getKey()
                                + "\t"
                                + entry.getValue()
                                .value()
                );

                writer.newLine();
            }

            for (
                    Map.Entry<String, OrganizationId> entry :
                    ids.organizations()
                            .entrySet()
            ) {

                writer.write(
                        "ORG\t"
                                + entry.getKey()
                                + "\t"
                                + entry.getValue()
                                .value()
                );

                writer.newLine();
            }

            for (
                    Map.Entry<String, TitleId> entry :
                    ids.titles()
                            .entrySet()
            ) {

                writer.write(
                        "TITLE\t"
                                + entry.getKey()
                                + "\t"
                                + entry.getValue()
                                .value()
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
            AuthoredIdRegistry ids
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
                        "Unsupported authored ID save format: "
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

                    decodeLine(
                            ids,
                            line
                    );

                } catch (RuntimeException exception) {

                    throw new IOException(
                            "Invalid authored ID data at line "
                                    + lineNumber
                                    + ": "
                                    + line,
                            exception
                    );
                }
            }
        }
    }

    private static void decodeLine(
            AuthoredIdRegistry ids,
            String line
    ) {
        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        switch (parts[0]) {

            case "SCENARIO" -> {

                requireColumns(
                        parts,
                        2
                );

                ids.setScenarioId(
                        parts[1]
                );
            }

            case "NPC" -> {

                requireColumns(
                        parts,
                        3
                );

                ids.registerNpc(
                        parts[1],
                        new NpcId(
                                Long.parseLong(
                                        parts[2]
                                )
                        )
                );
            }

            case "SETTLEMENT" -> {

                requireColumns(
                        parts,
                        3
                );

                ids.registerSettlement(
                        parts[1],
                        new SettlementId(
                                Long.parseLong(
                                        parts[2]
                                )
                        )
                );
            }

            case "ORG" -> {

                requireColumns(
                        parts,
                        3
                );

                ids.registerOrganization(
                        parts[1],
                        new OrganizationId(
                                Long.parseLong(
                                        parts[2]
                                )
                        )
                );
            }

            case "TITLE" -> {

                requireColumns(
                        parts,
                        3
                );

                ids.registerTitle(
                        parts[1],
                        new TitleId(
                                Long.parseLong(
                                        parts[2]
                                )
                        )
                );
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unknown authored ID record: "
                                    + parts[0]
                    );
        }
    }

    private static void requireColumns(
            String[] parts,
            int expected
    ) {
        if (parts.length != expected) {

            throw new IllegalArgumentException(
                    "Expected "
                            + expected
                            + " columns, got "
                            + parts.length
            );
        }
    }
}
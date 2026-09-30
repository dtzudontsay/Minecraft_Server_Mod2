package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyAllegianceManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class DynastyAllegiancePersistence {

    private static final String HEADER =
            "KNOWNWORLD_DYNASTY_ALLEGIANCE\t1";

    private static final String FILE_NAME =
            "dynasty_allegiance.tsv";

    private static final String TEMP_FILE_NAME =
            "dynasty_allegiance.tsv.tmp";

    private final Path directory;

    private final Path file;

    public DynastyAllegiancePersistence(
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
            DynastyAllegianceManager manager
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
                    DynastyAllegianceManager.OverrideEntry entry :
                    manager.overrides()
            ) {

                writer.write(
                        "ALLEGIANCE"
                                + "\t"
                                + entry.vassal()
                                .value()
                                + "\t"
                                + (
                                entry.liege() == null
                                        ? "0"
                                        : Long.toString(
                                        entry.liege()
                                                .value()
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
            DynastyAllegianceManager manager
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
                        "Unsupported dynasty allegiance format: "
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

                    if (parts.length != 3
                            || !"ALLEGIANCE".equals(
                            parts[0]
                    )) {

                        throw new IllegalArgumentException(
                                "Expected ALLEGIANCE record with 3 columns"
                        );
                    }

                    DynastyId vassal =
                            new DynastyId(
                                    Long.parseLong(
                                            parts[1]
                                    )
                            );

                    long liegeValue =
                            Long.parseLong(
                                    parts[2]
                            );

                    DynastyId liege =
                            liegeValue <= 0
                                    ? null
                                    : new DynastyId(
                                    liegeValue
                            );

                    manager.registerLoadedOverride(
                            vassal,
                            liege
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid dynasty allegiance at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }
}
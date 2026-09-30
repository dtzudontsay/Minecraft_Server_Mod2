package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterBirthStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCivilStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCustodyStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterFreedomStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterLegalState;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterLegalStateManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class CharacterLegalStatePersistence {

    private static final String HEADER =
            "KNOWNWORLD_LEGAL_STATES\t1";

    private static final String FILE_NAME =
            "legal_states.tsv";

    private static final String TEMP_FILE_NAME =
            "legal_states.tsv.tmp";

    private final Path directory;

    private final Path file;

    public CharacterLegalStatePersistence(
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
            CharacterLegalStateManager manager
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
                    CharacterLegalState state :
                    manager.all()
                            .stream()
                            .sorted(
                                    Comparator.comparingLong(
                                            value ->
                                                    value.owner()
                                                            .value()
                                    )
                            )
                            .toList()
            ) {

                writer.write(
                        String.join(
                                "\t",
                                "LEGAL",
                                Long.toString(
                                        state.owner()
                                                .value()
                                ),
                                state.birthStatus()
                                        .name(),
                                state.freedomStatus()
                                        .name(),
                                state.custodyStatus()
                                        .name(),
                                state.civilStatus()
                                        .name()
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
            CharacterLegalStateManager manager
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
                        "Unsupported legal-state save format: "
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

                    if (parts.length != 6
                            || !"LEGAL".equals(
                            parts[0]
                    )) {

                        throw new IllegalArgumentException(
                                "Expected LEGAL record with 6 columns"
                        );
                    }

                    manager.registerLoaded(
                            new CharacterLegalState(
                                    new NpcId(
                                            Long.parseLong(
                                                    parts[1]
                                            )
                                    ),
                                    CharacterBirthStatus.valueOf(
                                            parts[2]
                                    ),
                                    CharacterFreedomStatus.valueOf(
                                            parts[3]
                                    ),
                                    CharacterCustodyStatus.valueOf(
                                            parts[4]
                                    ),
                                    CharacterCivilStatus.valueOf(
                                            parts[5]
                                    )
                            )
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid legal-state data at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }
}
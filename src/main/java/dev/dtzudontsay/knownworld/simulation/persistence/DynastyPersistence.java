package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyStatus;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyType;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

public final class DynastyPersistence {

    private static final String HEADER =
            "KNOWNWORLD_DYNASTIES\t1";

    private static final String FILE_NAME =
            "dynasties.tsv";

    private static final String TEMP_FILE_NAME =
            "dynasties.tsv.tmp";

    private final Path directory;

    private final Path file;

    public DynastyPersistence(
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
            DynastyManager manager
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
                    Dynasty dynasty :
                    manager.all()
            ) {

                writer.write(
                        String.join(
                                "\t",
                                "DYNASTY",
                                Long.toString(
                                        dynasty.id()
                                                .value()
                                ),
                                encodeText(
                                        dynasty.authoredId()
                                ),
                                encodeText(
                                        dynasty.name()
                                ),
                                dynasty.type()
                                        .name(),
                                dynasty.status()
                                        .name(),
                                Long.toString(
                                        dynasty.organizationId()
                                                .value()
                                ),
                                encodeText(
                                        dynasty.homeLocationId()
                                ),
                                encodeText(
                                        dynasty.cultureId()
                                ),
                                encodeText(
                                        dynasty.religionId()
                                ),
                                idOrZero(
                                        dynasty.parentDynasty()
                                ),
                                idOrZero(
                                        dynasty.liegeDynasty()
                                ),
                                npcOrZero(
                                        dynasty.head()
                                ),
                                npcOrZero(
                                        dynasty.heir()
                                ),
                                Double.toString(
                                        dynasty.prestige()
                                ),
                                Double.toString(
                                        dynasty.wealth()
                                ),
                                Double.toString(
                                        dynasty.militaryStrength()
                                ),
                                dynasty.provenance()
                                        .name(),
                                encodeText(
                                        dynasty.words()
                                ),
                                encodeText(
                                        dynasty.heraldry()
                                ),
                                encodeText(
                                        dynasty.sourceNote()
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
            DynastyManager manager
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
                        "Unsupported dynasty format: "
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

                    if (parts.length != 21
                            || !"DYNASTY".equals(
                            parts[0]
                    )) {

                        throw new IllegalArgumentException(
                                "Expected DYNASTY record with 21 columns"
                        );
                    }

                    manager.registerLoaded(
                            new Dynasty(
                                    new DynastyId(
                                            Long.parseLong(
                                                    parts[1]
                                            )
                                    ),
                                    decodeText(
                                            parts[2]
                                    ),
                                    decodeText(
                                            parts[3]
                                    ),
                                    DynastyType.valueOf(
                                            parts[4]
                                    ),
                                    DynastyStatus.valueOf(
                                            parts[5]
                                    ),
                                    new OrganizationId(
                                            Long.parseLong(
                                                    parts[6]
                                            )
                                    ),
                                    decodeText(
                                            parts[7]
                                    ),
                                    decodeText(
                                            parts[8]
                                    ),
                                    decodeText(
                                            parts[9]
                                    ),
                                    dynastyIdOrNull(
                                            parts[10]
                                    ),
                                    dynastyIdOrNull(
                                            parts[11]
                                    ),
                                    npcIdOrNull(
                                            parts[12]
                                    ),
                                    npcIdOrNull(
                                            parts[13]
                                    ),
                                    Double.parseDouble(
                                            parts[14]
                                    ),
                                    Double.parseDouble(
                                            parts[15]
                                    ),
                                    Double.parseDouble(
                                            parts[16]
                                    ),
                                    ReferenceProvenance.valueOf(
                                            parts[17]
                                    ),
                                    decodeText(
                                            parts[18]
                                    ),
                                    decodeText(
                                            parts[19]
                                    ),
                                    decodeText(
                                            parts[20]
                                    )
                            )
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid dynasty data at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    private static String idOrZero(
            DynastyId id
    ) {

        return id == null
                ? "0"
                : Long.toString(
                id.value()
        );
    }

    private static String npcOrZero(
            NpcId id
    ) {

        return id == null
                ? "0"
                : Long.toString(
                id.value()
        );
    }

    private static DynastyId dynastyIdOrNull(
            String value
    ) {

        long id =
                Long.parseLong(
                        value
                );

        return id <= 0
                ? null
                : new DynastyId(
                id
        );
    }

    private static NpcId npcIdOrNull(
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

    private static String encodeText(
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

    private static String decodeText(
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
}
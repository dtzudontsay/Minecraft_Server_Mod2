package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingStatus;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingManager;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class LandedHoldingPersistence {

    private static final String HEADER =
            "KNOWNWORLD_LANDED_HOLDINGS\t1";

    private static final String FILE_NAME =
            "landed_holdings.tsv";

    private static final String TEMP_FILE_NAME =
            "landed_holdings.tsv.tmp";

    private final Path directory;

    private final Path file;

    public LandedHoldingPersistence(
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
            LandedHoldingManager manager
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
                    LandedHolding holding :
                    manager.all()
                            .stream()
                            .sorted(
                                    Comparator.comparing(
                                            LandedHolding::id
                                    )
                            )
                            .toList()
            ) {

                writer.write(
                        encode(
                                holding
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
            LandedHoldingManager manager
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
                        "Unsupported landed holding format: "
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

                    manager.registerLoaded(
                            decode(
                                    line
                            )
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid landed holding at line "
                                    + lineNumber,
                            exception
                    );
                }
            }
        }
    }

    private static String encode(
            LandedHolding holding
    ) {

        return String.join(
                "\t",
                "HOLDING",
                Long.toString(
                        holding.id()
                                .value()
                ),
                escape(
                        nullToEmpty(
                                holding.authoredId()
                        )
                ),
                escape(
                        holding.name()
                ),
                holding.type()
                        .name(),
                escape(
                        holding.worldLocationId()
                ),
                dynastyOrZero(
                        holding.deJureDynastyId()
                ),
                holdingOrZero(
                        holding.parentHoldingId()
                ),
                holding.status()
                        .name(),
                dynastyOrZero(
                        holding.ownerDynastyId()
                ),
                npcOrZero(
                        holding.holderNpcId()
                ),
                organizationOrZero(
                        holding.governmentOrganizationId()
                ),
                titleOrZero(
                        holding.linkedTitleId()
                ),
                Boolean.toString(
                        holding.capital()
                ),
                Double.toString(
                        holding.taxBase()
                ),
                Double.toString(
                        holding.militaryValue()
                ),
                Double.toString(
                        holding.populationWeight()
                )
        );
    }

    private static LandedHolding decode(
            String line
    ) {

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        if (parts.length != 17
                || !"HOLDING".equals(
                parts[0]
        )) {

            throw new IllegalArgumentException(
                    "Expected HOLDING record with 17 columns"
            );
        }

        return new LandedHolding(
                new HoldingId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                emptyToNull(
                        unescape(
                                parts[2]
                        )
                ),
                unescape(
                        parts[3]
                ),
                HoldingType.valueOf(
                        parts[4]
                ),
                unescape(
                        parts[5]
                ),
                dynastyOrNull(
                        parts[6]
                ),
                holdingOrNull(
                        parts[7]
                ),
                HoldingStatus.valueOf(
                        parts[8]
                ),
                dynastyOrNull(
                        parts[9]
                ),
                npcOrNull(
                        parts[10]
                ),
                organizationOrNull(
                        parts[11]
                ),
                titleOrNull(
                        parts[12]
                ),
                Boolean.parseBoolean(
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
                )
        );
    }

    private static String dynastyOrZero(
            DynastyId id
    ) {

        return id == null
                ? "0"
                : Long.toString(
                id.value()
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

    private static String holdingOrZero(
            HoldingId id
    ) {

        return id == null
                ? "0"
                : Long.toString(
                id.value()
        );
    }

    private static HoldingId holdingOrNull(
            String raw
    ) {

        long value =
                Long.parseLong(
                        raw
                );

        return value <= 0
                ? null
                : new HoldingId(
                value
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
            OrganizationId id
    ) {

        return id == null
                ? "0"
                : Long.toString(
                id.value()
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

    private static String titleOrZero(
            TitleId id
    ) {

        return id == null
                ? "0"
                : Long.toString(
                id.value()
        );
    }

    private static TitleId titleOrNull(
            String raw
    ) {

        long value =
                Long.parseLong(
                        raw
                );

        return value <= 0
                ? null
                : new TitleId(
                value
        );
    }

    private static String nullToEmpty(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }

    private static String emptyToNull(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? null
                : value;
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

                    default ->
                            result.append(
                                    character
                            );
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
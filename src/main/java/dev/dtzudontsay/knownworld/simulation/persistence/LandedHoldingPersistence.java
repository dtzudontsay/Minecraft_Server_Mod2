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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

        List<LoadedHolding> loaded =
                new ArrayList<>();

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

                    LandedHolding decoded =
                            decode(
                                    line
                            );

                    loaded.add(
                            new LoadedHolding(
                                    lineNumber,
                                    decoded
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

        /*
         * ---------------------------------------------------------
         * PASS 1 — REGISTER EVERY HOLDING WITHOUT ITS PARENT
         * ---------------------------------------------------------
         *
         * Holding IDs are persistent runtime IDs.
         *
         * A child may have a parent whose numeric ID is greater than
         * the child's ID. Because the persistence file is saved in
         * numeric ID order, requiring the parent to already exist
         * while reading a child makes loading dependent on file order.
         *
         * Example:
         *
         * holding #40
         *     parent = holding #236
         *
         * #236 is perfectly valid, but has not been encountered yet.
         *
         * Therefore every holding is first registered with
         * parentHoldingId = null. All other references can already be
         * validated normally because NPCs, organizations, titles,
         * dynasties and world references exist before holdings load.
         */
        for (
                LoadedHolding entry :
                loaded
        ) {

            LandedHolding decoded =
                    entry.holding();

            try {

                manager.registerLoaded(
                        withoutParent(
                                decoded
                        )
                );

            } catch (
                    RuntimeException exception
            ) {

                throw new IOException(
                        "Invalid landed holding at line "
                                + entry.lineNumber()
                                + " during first-pass registration",
                        exception
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * PASS 2 — RESTORE THE TERRITORIAL HIERARCHY
         * ---------------------------------------------------------
         *
         * At this point every persisted HoldingId is registered.
         *
         * LandedHoldingManager.setParent() now performs the normal
         * existence and cycle validation without depending on the
         * serialization order of the file.
         */
        for (
                LoadedHolding entry :
                loaded
        ) {

            LandedHolding decoded =
                    entry.holding();

            HoldingId parent =
                    decoded.parentHoldingId();

            if (parent == null) {
                continue;
            }

            try {

                manager.setParent(
                        decoded.id(),
                        parent
                );

            } catch (
                    RuntimeException exception
            ) {

                throw new IOException(
                        "Invalid landed holding parent at line "
                                + entry.lineNumber()
                                + ": holding "
                                + decoded.id()
                                + " -> parent "
                                + parent,
                        exception
                );
            }
        }
    }

    private static LandedHolding withoutParent(
            LandedHolding holding
    ) {

        return new LandedHolding(
                holding.id(),
                holding.authoredId(),
                holding.name(),
                holding.type(),
                holding.worldLocationId(),
                holding.deJureDynastyId(),
                null,
                holding.status(),
                holding.ownerDynastyId(),
                holding.holderNpcId(),
                holding.governmentOrganizationId(),
                holding.linkedTitleId(),
                holding.capital(),
                holding.taxBase(),
                holding.militaryValue(),
                holding.populationWeight()
        );
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

    private record LoadedHolding(
            int lineNumber,
            LandedHolding holding
    ) {
    }
}
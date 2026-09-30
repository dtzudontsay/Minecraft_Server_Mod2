package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyContinuity;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class DynastyPersistence {

    private static final String HEADER_PREFIX =
            "KNOWNWORLD_DYNASTIES\t";

    private static final int CURRENT_VERSION =
            2;

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
                    HEADER_PREFIX
                            + CURRENT_VERSION
            );

            writer.newLine();

            for (
                    Dynasty dynasty :
                    manager.all()
            ) {

                writer.write(
                        encodeVersion2(
                                dynasty
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

            if (header == null
                    || !header.startsWith(
                    HEADER_PREFIX
            )) {

                throw new IOException(
                        "Unsupported dynasty format: "
                                + header
                );
            }

            final int version;

            try {

                version =
                        Integer.parseInt(
                                header.substring(
                                        HEADER_PREFIX.length()
                                )
                        );

            } catch (
                    NumberFormatException exception
            ) {

                throw new IOException(
                        "Invalid dynasty version header: "
                                + header,
                        exception
                );
            }

            if (version < 1
                    || version > CURRENT_VERSION) {

                throw new IOException(
                        "Unsupported dynasty version "
                                + version
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

                    Dynasty dynasty =
                            switch (version) {

                                case 1 ->
                                        decodeVersion1(
                                                line
                                        );

                                case 2 ->
                                        decodeVersion2(
                                                line
                                        );

                                default ->
                                        throw new IllegalStateException(
                                                "Unhandled dynasty version "
                                                        + version
                                        );
                            };

                    manager.registerLoaded(
                            dynasty
                    );

                } catch (
                        RuntimeException exception
                ) {

                    throw new IOException(
                            "Invalid dynasty data at line "
                                    + lineNumber
                                    + ": "
                                    + line,
                            exception
                    );
                }
            }
        }
    }

    /*
     * =========================================================
     * VERSION 2
     * =========================================================
     *
     * Column layout:
     *
     *  0  record type
     *  1  numeric dynasty id
     *  2  authored id
     *  3  display name
     *  4  dynasty type
     *  5  status
     *  6  organization id
     *  7  home location id
     *  8  culture id
     *  9  religion id
     * 10  parent dynasty id
     * 11  liege dynasty id
     * 12  predecessor dynasty id
     * 13  successor dynasty id
     * 14  head npc id
     * 15  heir npc id
     * 16  founded year
     * 17  extinct year
     * 18  active at scenario start
     * 19  continuities
     * 20  words
     * 21  heraldry
     * 22  prestige
     * 23  wealth
     * 24  military strength
     * 25  provenance
     * 26  source note
     *
     * Total: 27 columns.
     */

    private static String encodeVersion2(
            Dynasty dynasty
    ) {

        String continuities =
                dynasty.continuities()
                        .stream()
                        .map(
                                Enum::name
                        )
                        .sorted()
                        .collect(
                                Collectors.joining(
                                        ","
                                )
                        );

        return String.join(
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
                organizationOrZero(
                        dynasty.organizationId()
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
                dynastyIdOrZero(
                        dynasty.parentDynasty()
                ),
                dynastyIdOrZero(
                        dynasty.liegeDynasty()
                ),
                dynastyIdOrZero(
                        dynasty.predecessorDynasty()
                ),
                dynastyIdOrZero(
                        dynasty.successorDynasty()
                ),
                npcOrZero(
                        dynasty.head()
                ),
                npcOrZero(
                        dynasty.heir()
                ),
                integerOrBlank(
                        dynasty.foundedYear()
                ),
                integerOrBlank(
                        dynasty.extinctYear()
                ),
                Boolean.toString(
                        dynasty.activeAtScenarioStart()
                ),
                continuities,
                encodeText(
                        dynasty.words()
                ),
                encodeText(
                        dynasty.heraldry()
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
                        dynasty.sourceNote()
                )
        );
    }

    private static Dynasty decodeVersion2(
            String line
    ) {

        String[] parts =
                line.split(
                        "\t",
                        -1
                );

        /*
         * Batch 19.0B.1 fix:
         *
         * encodeVersion2 writes 27 columns (indexes 0..26).
         * The first implementation incorrectly expected 28 and also
         * read every field after activeAtScenarioStart one position
         * too far to the right.
         */
        if (parts.length != 27
                || !"DYNASTY".equals(
                parts[0]
        )) {

            throw new IllegalArgumentException(
                    "Expected DYNASTY v2 record with 27 columns, got "
                            + parts.length
            );
        }

        Set<DynastyContinuity> continuities =
                parseContinuities(
                        parts[19]
                );

        return new Dynasty(
                new DynastyId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                decodeText(
                        parts[2]
                ),
                requireDecodedText(
                        parts[3],
                        "dynasty name"
                ),
                DynastyType.valueOf(
                        parts[4]
                ),
                DynastyStatus.valueOf(
                        parts[5]
                ),
                organizationIdOrNull(
                        parts[6]
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
                dynastyIdOrNull(
                        parts[12]
                ),
                dynastyIdOrNull(
                        parts[13]
                ),
                npcIdOrNull(
                        parts[14]
                ),
                npcIdOrNull(
                        parts[15]
                ),
                integerOrNull(
                        parts[16]
                ),
                integerOrNull(
                        parts[17]
                ),
                Boolean.parseBoolean(
                        parts[18]
                ),
                continuities,
                decodeTextOrEmpty(
                        parts[20]
                ),
                decodeTextOrEmpty(
                        parts[21]
                ),
                Double.parseDouble(
                        parts[22]
                ),
                Double.parseDouble(
                        parts[23]
                ),
                Double.parseDouble(
                        parts[24]
                ),
                ReferenceProvenance.valueOf(
                        parts[25]
                ),
                decodeTextOrEmpty(
                        parts[26]
                )
        );
    }

    /*
     * =========================================================
     * VERSION 1 MIGRATION
     * =========================================================
     *
     * Batch 19.0B format.
     *
     * V1 did not contain:
     * - continuity information
     * - temporal metadata
     * - predecessor/successor
     * - optional organization IDs
     *
     * V1 therefore migrates into a conservative V2 interpretation.
     */

    private static Dynasty decodeVersion1(
            String line
    ) {

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
                    "Expected DYNASTY v1 record with 21 columns, got "
                            + parts.length
            );
        }

        DynastyStatus status =
                DynastyStatus.valueOf(
                        parts[5]
                );

        return new Dynasty(
                new DynastyId(
                        Long.parseLong(
                                parts[1]
                        )
                ),
                decodeText(
                        parts[2]
                ),
                requireDecodedText(
                        parts[3],
                        "dynasty name"
                ),
                DynastyType.valueOf(
                        parts[4]
                ),
                status,
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
                null,
                null,
                npcIdOrNull(
                        parts[12]
                ),
                npcIdOrNull(
                        parts[13]
                ),
                null,
                null,
                status
                        != DynastyStatus.EXTINCT,
                Set.of(
                        DynastyContinuity.BOOK
                ),
                decodeTextOrEmpty(
                        parts[18]
                ),
                decodeTextOrEmpty(
                        parts[19]
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
                decodeTextOrEmpty(
                        parts[20]
                )
        );
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private static Set<DynastyContinuity> parseContinuities(
            String raw
    ) {

        if (raw == null
                || raw.isBlank()) {

            throw new IllegalArgumentException(
                    "Dynasty continuity set cannot be empty"
            );
        }

        Set<DynastyContinuity> result =
                new LinkedHashSet<>();

        Arrays.stream(
                        raw.split(
                                ","
                        )
                )
                .map(
                        String::trim
                )
                .filter(
                        value ->
                                !value.isBlank()
                )
                .map(
                        DynastyContinuity::valueOf
                )
                .forEach(
                        result::add
                );

        if (result.isEmpty()) {

            throw new IllegalArgumentException(
                    "Dynasty continuity set cannot be empty"
            );
        }

        return Set.copyOf(
                result
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

    private static String dynastyIdOrZero(
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

    private static OrganizationId organizationIdOrNull(
            String value
    ) {

        long id =
                Long.parseLong(
                        value
                );

        return id <= 0
                ? null
                : new OrganizationId(
                id
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

    private static String integerOrBlank(
            Integer value
    ) {

        return value == null
                ? ""
                : Integer.toString(
                value
        );
    }

    private static Integer integerOrNull(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? null
                : Integer.parseInt(
                value
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

    private static String decodeTextOrEmpty(
            String value
    ) {

        String decoded =
                decodeText(
                        value
                );

        return decoded == null
                ? ""
                : decoded;
    }

    private static String requireDecodedText(
            String value,
            String description
    ) {

        String decoded =
                decodeText(
                        value
                );

        if (decoded == null
                || decoded.isBlank()) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be empty"
            );
        }

        return decoded;
    }
}
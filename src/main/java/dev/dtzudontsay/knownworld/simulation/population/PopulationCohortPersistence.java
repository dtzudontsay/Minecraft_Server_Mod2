
package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Versioned population checkpoint for settlement
 * and holding cohorts.
 *
 * Reads v1, v2 and v3.
 * Writes v3 to the original checkpoint filename.
 *
 * Demographic year progression is saved with the
 * population counts.
 *
 * Population and CampaignCalendar checkpoints are
 * not yet cross-domain atomic.
 */
public final class PopulationCohortPersistence {

    private static final String V1 =
            "KNOWNWORLD_POPULATION_COHORTS\t1";

    private static final String V2 =
            "KNOWNWORLD_POPULATION_COHORTS\t2";

    private static final String V3 =
            "KNOWNWORLD_POPULATION_COHORTS\t3";

    private static final String FILE =
            "population_cohorts_v1.tsv";

    private static final long MAX_BYTES =
            8L * 1024L * 1024L;

    private final Path file;
    private final Path temp;

    public PopulationCohortPersistence(Path directory) {
        Objects.requireNonNull(directory, "directory");

        file = directory.resolve(FILE);
        temp = directory.resolve(FILE + ".tmp");
    }

    public Path file() {
        return file;
    }

    public synchronized void save(
            long absoluteDay,
            PopulationCohortManager manager
    ) throws IOException {

        if (absoluteDay < 0L) {
            throw new IllegalArgumentException(
                    "Negative campaign day"
            );
        }

        Objects.requireNonNull(manager, "manager");

        if (Files.exists(temp)) {
            throw new IOException(
                    "Unresolved population checkpoint temporary file"
            );
        }

        var state = manager.extendedSnapshot();

        StringBuilder body = new StringBuilder(V3)
                .append('\n')
                .append("DAY\t")
                .append(absoluteDay)
                .append('\n')
                .append("DEMOGRAPHY\t")
                .append(
                        state.lastDemographicYear() == null
                                ? "NONE"
                                : state.lastDemographicYear()
                )
                .append('\n');

        /*
         * Settlement surveys are written first
         * in ascending SettlementId order.
         */
        state.settlements()
                .surveyed()
                .entrySet()
                .stream()
                .sorted(
                        Comparator.comparingLong(
                                entry -> entry.getKey().value()
                        )
                )
                .forEach(
                        entry -> body
                                .append("SURVEY\t")
                                .append(entry.getKey().value())
                                .append('\t')
                                .append(entry.getValue())
                                .append('\n')
                );

        /*
         * The population manager supplies its
         * settlement cohorts in deterministic order.
         */
        for (var row : state.settlements().cohorts()) {

            var key = row.key();

            body.append("COHORT\t")
                    .append(key.settlement().value())
                    .append('\t')
                    .append(key.age())
                    .append('\t')
                    .append(key.sex())
                    .append('\t')
                    .append(key.cultureId())
                    .append('\t')
                    .append(key.religionId())
                    .append('\t')
                    .append(key.socialClassId())
                    .append('\t')
                    .append(key.occupationId())
                    .append('\t')
                    .append(row.people())
                    .append('\n');
        }

        /*
         * Multiple holding surveys are valid.
         * The reader must accept every survey in
         * this sorted section.
         */
        state.holdingSurveys()
                .entrySet()
                .stream()
                .sorted(
                        Comparator.comparingLong(
                                entry -> entry.getKey().value()
                        )
                )
                .forEach(
                        entry -> body
                                .append("HOLDING_SURVEY\t")
                                .append(entry.getKey().value())
                                .append('\t')
                                .append(entry.getValue())
                                .append('\n')
                );

        for (var row : state.holdingCohorts()) {

            var key = row.key();

            body.append("HOLDING_COHORT\t")
                    .append(key.holding().value())
                    .append('\t')
                    .append(key.age())
                    .append('\t')
                    .append(key.sex())
                    .append('\t')
                    .append(key.cultureId())
                    .append('\t')
                    .append(key.religionId())
                    .append('\t')
                    .append(key.socialClassId())
                    .append('\t')
                    .append(key.occupationId())
                    .append('\t')
                    .append(row.people())
                    .append('\n');
        }

        String payload = body.toString();

        byte[] bytes = (
                payload
                        + "SHA256\t"
                        + sha(payload)
                        + "\n"
        ).getBytes(StandardCharsets.UTF_8);

        if (bytes.length > MAX_BYTES) {
            throw new IOException(
                    "Population checkpoint exceeds size limit"
            );
        }

        Files.createDirectories(file.getParent());

        try (FileChannel channel = FileChannel.open(
                temp,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        )) {

            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }

            channel.force(true);
        }

        try {
            Files.move(
                    temp,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException(
                    "Atomic population checkpoint writes unavailable",
                    exception
            );
        }

        /*
         * Validate the published checkpoint.
         */
        read(absoluteDay);
    }

    /**
     * Missing checkpoint returns false.
     *
     * Invalid checkpoints raise IOException rather
     * than silently initializing new population.
     */
    public synchronized boolean loadInto(
            long expectedDay,
            PopulationCohortManager manager
    ) throws IOException {

        Objects.requireNonNull(manager, "manager");

        var state = read(expectedDay);

        if (state == null) {
            return false;
        }

        try {
            manager.replaceExtended(state);
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Population checkpoint references invalid world state",
                    exception
            );
        }

        return true;
    }

    private PopulationCohortManager.ExtendedSnapshot read(
            long expectedDay
    ) throws IOException {

        if (expectedDay < 0L) {
            throw new IllegalArgumentException(
                    "Negative campaign day"
            );
        }

        if (Files.exists(temp)) {
            throw new IOException(
                    "Interrupted population checkpoint write"
            );
        }

        if (!Files.exists(file)) {
            return null;
        }

        if (!Files.isRegularFile(file)
                || Files.size(file) > MAX_BYTES) {
            throw new IOException(
                    "Invalid population checkpoint file"
            );
        }

        String data = Files.readString(
                file,
                StandardCharsets.UTF_8
        );

        if (!data.endsWith("\n")) {
            throw new IOException(
                    "Truncated population checkpoint"
            );
        }

        String[] lines = data.split("\n", -1);

        if (lines.length < 4
                || (!V1.equals(lines[0])
                && !V2.equals(lines[0])
                && !V3.equals(lines[0]))) {

            throw new IOException(
                    "Unsupported population checkpoint format"
            );
        }

        boolean legacy = V1.equals(lines[0]);

        /*
         * Verify the complete checkpoint content
         * before parsing any demographic records.
         */
        StringBuilder original = new StringBuilder();

        for (int i = 0; i < lines.length - 2; i++) {
            original.append(lines[i]).append('\n');
        }

        if (!(
                "SHA256\t" + sha(original.toString())
        ).equals(lines[lines.length - 2])) {

            throw new IOException(
                    "Population checkpoint checksum mismatch"
            );
        }

        try {
            String[] day = lines[1].split("\t", -1);

            if (day.length != 2
                    || !"DAY".equals(day[0])
                    || Long.parseLong(day[1]) != expectedDay) {

                throw new IOException(
                        "Population and campaign calendar checkpoints disagree"
                );
            }

            Integer lastDemographicYear = null;
            int firstRow = 2;

            /*
             * V3 adds annual demographic progression.
             * V1 and V2 have no such marker.
             */
            if (V3.equals(lines[0])) {

                if (lines.length < 5) {
                    throw new IOException(
                            "Missing demographic progress row"
                    );
                }

                String[] progress =
                        lines[2].split("\t", -1);

                if (progress.length != 2
                        || !"DEMOGRAPHY".equals(progress[0])) {

                    throw new IOException(
                            "Malformed demographic progress row"
                    );
                }

                if (!"NONE".equals(progress[1])) {

                    lastDemographicYear =
                            Integer.parseInt(progress[1]);

                    if (lastDemographicYear < 0) {
                        throw new IOException(
                                "Negative demographic progress year"
                        );
                    }
                }

                firstRow = 3;
            }

            Map<SettlementId, PopulationCohortManager.Basis>
                    settlementSurveys = new HashMap<>();

            List<PopulationCohortManager.Cohort>
                    settlementCohorts = new ArrayList<>();

            Map<HoldingId, PopulationCohortManager.Basis>
                    holdingSurveys = new HashMap<>();

            List<PopulationCohortManager.HoldingCohort>
                    holdingCohorts = new ArrayList<>();

            /*
             * Expected ordering:
             *
             * Phase 0: SURVEY
             * Phase 1: COHORT
             * Phase 2: HOLDING_SURVEY
             * Phase 3: HOLDING_COHORT
             *
             * Repeated rows in the same phase
             * are permitted when correctly sorted.
             * Returning to an earlier phase is not.
             */
            int phase = 0;

            long lastSurvey = 0L;
            long lastHoldingSurvey = 0L;

            PopulationCohortManager.Key lastSettlementKey =
                    null;

            PopulationCohortManager.HoldingKey lastHoldingKey =
                    null;

            for (int i = firstRow;
                 i < lines.length - 2;
                 i++) {

                String[] parts =
                        lines[i].split("\t", -1);

                switch (parts[0]) {

                    case "SURVEY" -> {

                        if (phase != 0
                                || parts.length != 3) {

                            throw new IOException(
                                    "Out-of-order settlement survey"
                            );
                        }

                        SettlementId id = new SettlementId(
                                Long.parseLong(parts[1])
                        );

                        if (id.value() <= lastSurvey
                                || settlementSurveys.putIfAbsent(
                                id,
                                PopulationCohortManager.Basis
                                        .valueOf(parts[2])
                        ) != null) {

                            throw new IOException(
                                    "Duplicate/unsorted settlement survey"
                            );
                        }

                        lastSurvey = id.value();
                    }

                    case "COHORT" -> {

                        if (phase > 1
                                || parts.length != 9) {

                            throw new IOException(
                                    "Out-of-order settlement cohort"
                            );
                        }

                        phase = 1;

                        var key =
                                new PopulationCohortManager.Key(
                                        new SettlementId(
                                                Long.parseLong(parts[1])
                                        ),
                                        PopulationCohortManager.AgeBand
                                                .valueOf(parts[2]),
                                        NpcSex.valueOf(parts[3]),
                                        parts[4],
                                        parts[5],
                                        parts[6],
                                        parts[7]
                                );

                        if (lastSettlementKey != null
                                && lastSettlementKey
                                .compareTo(key) >= 0) {

                            throw new IOException(
                                    "Duplicate/unsorted settlement cohort"
                            );
                        }

                        settlementCohorts.add(
                                new PopulationCohortManager.Cohort(
                                        key,
                                        Long.parseLong(parts[8])
                                )
                        );

                        lastSettlementKey = key;
                    }

                    case "HOLDING_SURVEY" -> {

                        /*
                         * FIX:
                         *
                         * The previous condition:
                         *
                         *     phase > 1
                         *
                         * rejected the SECOND holding
                         * survey, since the first one
                         * had already set phase = 2.
                         *
                         * phase == 2 must remain valid.
                         * Only phase == 3 is too late.
                         */
                        if (legacy
                                || phase > 2
                                || parts.length != 3) {

                            throw new IOException(
                                    "Out-of-order holding survey"
                            );
                        }

                        phase = 2;

                        HoldingId id = new HoldingId(
                                Long.parseLong(parts[1])
                        );

                        if (id.value() <= lastHoldingSurvey
                                || holdingSurveys.putIfAbsent(
                                id,
                                PopulationCohortManager.Basis
                                        .valueOf(parts[2])
                        ) != null) {

                            throw new IOException(
                                    "Duplicate/unsorted holding survey"
                            );
                        }

                        lastHoldingSurvey = id.value();
                    }

                    case "HOLDING_COHORT" -> {

                        if (legacy
                                || phase > 3
                                || parts.length != 9) {

                            throw new IOException(
                                    "Invalid holding cohort"
                            );
                        }

                        phase = 3;

                        var key =
                                new PopulationCohortManager.HoldingKey(
                                        new HoldingId(
                                                Long.parseLong(parts[1])
                                        ),
                                        PopulationCohortManager.AgeBand
                                                .valueOf(parts[2]),
                                        NpcSex.valueOf(parts[3]),
                                        parts[4],
                                        parts[5],
                                        parts[6],
                                        parts[7]
                                );

                        if (lastHoldingKey != null
                                && lastHoldingKey
                                .compareTo(key) >= 0) {

                            throw new IOException(
                                    "Duplicate/unsorted holding cohort"
                            );
                        }

                        holdingCohorts.add(
                                new PopulationCohortManager.HoldingCohort(
                                        key,
                                        Long.parseLong(parts[8])
                                )
                        );

                        lastHoldingKey = key;
                    }

                    default -> throw new IOException(
                            "Invalid population checkpoint row"
                    );
                }
            }

            return new PopulationCohortManager.ExtendedSnapshot(
                    new PopulationCohortManager.Snapshot(
                            settlementSurveys,
                            settlementCohorts
                    ),
                    holdingSurveys,
                    holdingCohorts,
                    lastDemographicYear
            );

        } catch (IllegalArgumentException
                 | NullPointerException exception) {

            throw new IOException(
                    "Invalid population checkpoint values",
                    exception
            );
        }
    }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(
                                    value.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable",
                    exception
            );
        }
    }
}

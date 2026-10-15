
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Batch 22.9.
 *
 * Persistent, fail-closed agricultural interval ledger.
 *
 * The ledger does not store resource balances.
 * InventoryManager remains authoritative for goods.
 *
 * A prepared interval may have been partly executed
 * before a crash and must not be automatically retried.
 *
 * This is not a cross-file atomic transaction.
 */
public final class AgriculturalIntervalLedger {

    private static final String HEADER =
            "KNOWNWORLD_AGRICULTURE_INTERVALS\t1";

    private static final String NAME =
            "agriculture_intervals_v1.tsv";

    private static final String TEMP =
            "agriculture_intervals_v1.tsv.tmp";

    private static final long MAX_BYTES =
            4L * 1024L * 1024L;

    public enum BeginResult {
        STARTED,
        ALREADY_COMPLETED
    }

    public record Progress(
            long lastCompletedEndDay,
            long pendingEndDayExclusive
    ) {
        public Progress {
            if (lastCompletedEndDay < 0L
                    || pendingEndDayExclusive < -1L
                    || (pendingEndDayExclusive != -1L
                    && pendingEndDayExclusive
                    <= lastCompletedEndDay)) {

                throw new IllegalArgumentException(
                        "Invalid agricultural interval progress"
                );
            }
        }

        public boolean pending() {
            return pendingEndDayExclusive != -1L;
        }
    }

    private final Path directory;
    private final Path file;
    private final Path temporary;

    public AgriculturalIntervalLedger(Path directory) {
        this.directory = Objects.requireNonNull(
                directory, "directory"
        );

        this.file = directory.resolve(NAME);
        this.temporary = directory.resolve(TEMP);
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    /**
     * Explicit initialization only.
     *
     * Ordinary world loading never calls this.
     */
    public synchronized void initialize()
            throws IOException {

        if (Files.exists(file)
                || Files.exists(temporary)) {

            throw new IOException(
                    "Agricultural interval ledger already "
                            + "exists or has an interrupted write"
            );
        }

        write(new TreeMap<>(), false);
    }

    public synchronized Optional<Progress> progress(
            HoldingId holding
    ) throws IOException {

        Objects.requireNonNull(holding, "holding");

        return Optional.ofNullable(
                read().get(holding.value())
        );
    }

    /**
     * Write PREPARED state before economic mutation.
     *
     * Existing pending state blocks all retries.
     *
     * Previously completed intervals are skipped.
     *
     * New intervals must be contiguous with
     * existing completed progression.
     */
    public synchronized BeginResult begin(
            HoldingId holding,
            AgriculturalInterval interval
    ) throws IOException {

        Objects.requireNonNull(holding, "holding");
        Objects.requireNonNull(interval, "interval");

        TreeMap<Long, Progress> state = read();

        Progress previous =
                state.get(holding.value());

        if (previous != null) {

            if (previous.pending()) {
                throw new IOException(
                        "Agricultural interval already PREPARED "
                                + "for holding " + holding
                                + "; manual recovery required"
                );
            }

            if (interval.endDayExclusive()
                    <= previous.lastCompletedEndDay()) {

                return BeginResult.ALREADY_COMPLETED;
            }

            if (interval.startDayInclusive()
                    != previous.lastCompletedEndDay()) {

                throw new IOException(
                        "Agricultural interval overlap or gap "
                                + "for holding " + holding
                );
            }
        }

        state.put(
                holding.value(),
                new Progress(
                        interval.startDayInclusive(),
                        interval.endDayExclusive()
                )
        );

        write(state, true);

        return BeginResult.STARTED;
    }

    /**
     * Mark an interval complete after the economy
     * snapshot has been committed and verified.
     */
    public synchronized void complete(
            HoldingId holding,
            AgriculturalInterval interval
    ) throws IOException {

        Objects.requireNonNull(holding, "holding");
        Objects.requireNonNull(interval, "interval");

        TreeMap<Long, Progress> state = read();

        Progress previous =
                state.get(holding.value());

        if (previous == null
                || !previous.pending()
                || previous.lastCompletedEndDay()
                != interval.startDayInclusive()
                || previous.pendingEndDayExclusive()
                != interval.endDayExclusive()) {

            throw new IOException(
                    "No matching PREPARED interval "
                            + "for holding " + holding
            );
        }

        state.put(
                holding.value(),
                new Progress(
                        interval.endDayExclusive(),
                        -1L
                )
        );

        write(state, true);
    }

    /**
     * Validate all persisted entries, ordering
     * and checksum before returning state.
     */
    private TreeMap<Long, Progress> read()
            throws IOException {

        if (Files.exists(temporary)) {
            throw new IOException(
                    "Interrupted agricultural ledger "
                            + "write detected"
            );
        }

        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    "Agricultural interval ledger "
                            + "is not initialized: " + file
            );
        }

        if (Files.size(file) > MAX_BYTES) {
            throw new IOException(
                    "Agricultural ledger exceeds size limit"
            );
        }

        String content = Files.readString(
                file, StandardCharsets.UTF_8
        );

        if (!content.endsWith("\n")) {
            throw new IOException(
                    "Truncated agricultural ledger"
            );
        }

        String[] lines =
                content.split("\n", -1);

        if (lines.length < 3
                || !HEADER.equals(lines[0])) {

            throw new IOException(
                    "Unknown agricultural ledger format"
            );
        }

        String checksumLine =
                lines[lines.length - 2];

        if (!checksumLine.startsWith("CHECKSUM\t")) {
            throw new IOException(
                    "Missing agricultural ledger checksum"
            );
        }

        StringBuilder payload = new StringBuilder();

        for (int i = 0; i < lines.length - 2; i++) {
            payload.append(lines[i]).append('\n');
        }

        String expectedChecksum =
                "CHECKSUM\t"
                        + checksum(payload.toString());

        if (!expectedChecksum.equals(checksumLine)) {
            throw new IOException(
                    "Agricultural ledger checksum mismatch"
            );
        }

        TreeMap<Long, Progress> state =
                new TreeMap<>();

        long previousHolding = 0L;

        try {
            for (int i = 1; i < lines.length - 2; i++) {

                String[] columns =
                        lines[i].split("\t", -1);

                if (columns.length != 4
                        || !"FARM".equals(columns[0])) {

                    throw new IOException(
                            "Invalid agricultural ledger row"
                    );
                }

                long holdingId =
                        Long.parseLong(columns[1]);

                long completed =
                        Long.parseLong(columns[2]);

                long pending =
                        Long.parseLong(columns[3]);

                if (holdingId <= previousHolding) {
                    throw new IOException(
                            "Duplicate or unsorted holding ID"
                    );
                }

                previousHolding = holdingId;

                state.put(
                        holdingId,
                        new Progress(completed, pending)
                );
            }

        } catch (IllegalArgumentException exception) {
            /*
             * NumberFormatException is a subclass
             * of IllegalArgumentException.
             *
             * A single catch is valid in Java 25.
             */
            throw new IOException(
                    "Invalid agricultural ledger contents",
                    exception
            );
        }

        return state;
    }

    /**
     * Write, atomically publish, and reread the file.
     *
     * On write interruption the temporary file remains
     * detectable as a fail-closed state.
     */
    private void write(
            Map<Long, Progress> state,
            boolean replace
    ) throws IOException {

        if (Files.exists(temporary)) {
            throw new IOException(
                    "Interrupted agricultural ledger "
                            + "write must be investigated"
            );
        }

        if (!replace && Files.exists(file)) {
            throw new IOException(
                    "Agricultural ledger already initialized"
            );
        }

        StringBuilder payload =
                new StringBuilder(HEADER)
                        .append('\n');

        for (Map.Entry<Long, Progress> entry :
                new TreeMap<>(state).entrySet()) {

            payload.append("FARM\t")
                    .append(entry.getKey())
                    .append('\t')
                    .append(entry.getValue()
                            .lastCompletedEndDay())
                    .append('\t')
                    .append(entry.getValue()
                            .pendingEndDayExclusive())
                    .append('\n');
        }

        String payloadText = payload.toString();

        String content =
                payloadText
                        + "CHECKSUM\t"
                        + checksum(payloadText)
                        + "\n";

        byte[] bytes = content.getBytes(
                StandardCharsets.UTF_8
        );

        if (bytes.length > MAX_BYTES) {
            throw new IOException(
                    "Agricultural ledger exceeds size limit"
            );
        }

        Files.createDirectories(directory);

        Files.write(temporary, bytes);

        try {
            if (replace) {

                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );

            } else {

                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.ATOMIC_MOVE
                );
            }

        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException(
                    "Atomic agricultural ledger writes "
                            + "are unsupported",
                    exception
            );
        }

        if (!read().equals(new TreeMap<>(state))) {
            throw new IOException(
                    "Agricultural ledger readback mismatch"
            );
        }
    }

    private static String checksum(String content) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            content.getBytes(
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

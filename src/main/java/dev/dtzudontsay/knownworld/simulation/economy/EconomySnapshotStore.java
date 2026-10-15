
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Batch 21.5: single-file economic snapshot prototype.
 *
 * Captures inventory balances and session journal
 * within the same serialization boundary.
 *
 * NOT YET integrated into EconomyRuntime.
 * NOT a replacement for the live inventory save.
 */
public final class EconomySnapshotStore {

    private static final String HEADER =
            "KNOWNWORLD_ECONOMY_SNAPSHOT\t1";

    private static final String NAME =
            "economy_snapshot_v1.tsv";

    private static final String TEMP =
            "economy_snapshot_v1.tsv.tmp";

    private static final long MAX_FILE_BYTES =
            64L * 1024L * 1024L;

    private final Path directory;
    private final Path file;

    public EconomySnapshotStore(Path directory) {
        this.directory = Objects.requireNonNull(
                directory, "directory"
        );
        this.file = directory.resolve(NAME);
    }

    public Path file() {
        return file;
    }

    public record InventoryRecord(
            InventoryId id,
            EconomicOwner owner,
            SettlementId location,
            Map<ResourceId, Long> balances
    ) {
        public InventoryRecord {
            Objects.requireNonNull(id);
            Objects.requireNonNull(owner);
            Objects.requireNonNull(location);
            Objects.requireNonNull(balances);

            Map<ResourceId, Long> copy = new TreeMap<>();

            for (var entry : balances.entrySet()) {
                Objects.requireNonNull(entry.getKey());

                long quantity = Objects.requireNonNull(
                        entry.getValue()
                );

                if (quantity <= 0L) {
                    throw new IllegalArgumentException(
                            "Snapshot balances must be positive"
                    );
                }

                copy.put(entry.getKey(), quantity);
            }

            balances = Map.copyOf(copy);
        }
    }

    public record Snapshot(
            long nextInventoryId,
            List<InventoryRecord> inventories,
            List<EconomicTransaction> transactions
    ) {
        public Snapshot {
            if (nextInventoryId <= 0L) {
                throw new IllegalArgumentException(
                        "Invalid next inventory ID"
                );
            }

            inventories = List.copyOf(inventories);
            transactions = List.copyOf(transactions);

            long previousId = 0L;

            for (InventoryRecord record : inventories) {
                if (record.id().value() <= previousId) {
                    throw new IllegalArgumentException(
                            "Inventories must have unique ascending IDs"
                    );
                }

                previousId = record.id().value();
            }

            if (previousId >= nextInventoryId) {
                throw new IllegalArgumentException(
                        "Next ID must exceed all inventory IDs"
                );
            }

            long sequence = 0L;

            for (EconomicTransaction transaction :
                    transactions) {

                if (transaction.sequence() != ++sequence) {
                    throw new IllegalArgumentException(
                            "Nonconsecutive transaction sequence"
                    );
                }
            }
        }
    }

    /**
     * Capture balances and journal under the same lock.
     */
    public static Snapshot capture(InventoryManager manager) {
        Objects.requireNonNull(manager, "manager");

        synchronized (manager) {
            List<InventoryRecord> records =
                    new ArrayList<>();

            for (ResourceInventory inventory :
                    manager.all()) {

                records.add(new InventoryRecord(
                        inventory.id(),
                        inventory.owner(),
                        inventory.location(),
                        inventory.balances()
                ));
            }

            records.sort(
                    java.util.Comparator.comparing(
                            InventoryRecord::id
                    )
            );

            return new Snapshot(
                    manager.nextId(),
                    records,
                    manager.transactions()
            );
        }
    }

    /**
     * Writes a single replacement file.
     *
     * Atomic replacement is required; an unsupported
     * filesystem causes an IOException.
     */
    public void write(Snapshot snapshot) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");

        String payload = serialize(snapshot);
        String content = payload
                + "CHECKSUM\t"
                + sha256(payload)
                + "\n";

        byte[] bytes = content.getBytes(
                StandardCharsets.UTF_8
        );

        if (bytes.length > MAX_FILE_BYTES) {
            throw new IOException(
                    "Economy snapshot exceeds size limit"
            );
        }

        Files.createDirectories(directory);

        Path temporary = directory.resolve(TEMP);

        try {
            Files.write(temporary, bytes);

            try {
                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ex) {
                throw new IOException(
                        "Atomic snapshot replacement unsupported",
                        ex
                );
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * Read and validate a snapshot.
     *
     * Missing files are errors: this method must not
     * silently interpret absent data as empty economy.
     */
    public Snapshot read(ResourceCatalog catalog)
            throws IOException {

        Objects.requireNonNull(catalog, "catalog");

        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    "Economy snapshot does not exist: " + file
            );
        }

        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException(
                    "Economy snapshot exceeds size limit"
            );
        }

        String content = Files.readString(
                file, StandardCharsets.UTF_8
        );

        try {
            return parse(content, catalog);
        } catch (RuntimeException ex) {
            throw new IOException(
                    "Invalid economy snapshot", ex
            );
        }
    }

    /**
     * Restore authoritative balances from a validated
     * snapshot.
     *
     * The returned InventoryManager begins a fresh
     * session journal. The historical session entries
     * remain available in Snapshot.transactions().
     */
    public static InventoryManager restore(
            Snapshot snapshot,
            ResourceCatalog catalog
    ) {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(catalog);

        InventoryManager manager = new InventoryManager(
                catalog
        );

        for (InventoryRecord record : snapshot.inventories()) {
            manager.restoreInventory(
                    record.id(),
                    record.owner(),
                    record.location(),
                    record.balances()
            );
        }

        manager.restoreNextId(snapshot.nextInventoryId());

        return manager;
    }

    private static String serialize(Snapshot snapshot) {
        StringBuilder out = new StringBuilder();

        out.append(HEADER).append('\n');
        out.append("NEXT_ID\t")
                .append(snapshot.nextInventoryId())
                .append('\n');

        for (InventoryRecord record :
                snapshot.inventories()) {

            out.append("INVENTORY\t")
                    .append(record.id().value()).append('\t')
                    .append(record.owner().kind()).append('\t')
                    .append(record.owner().id()).append('\t')
                    .append(record.location().value())
                    .append('\n');

            new TreeMap<>(record.balances())
                    .forEach((resource, quantity) -> {
                        out.append("BALANCE\t")
                                .append(record.id().value())
                                .append('\t')
                                .append(resource.value())
                                .append('\t')
                                .append(quantity)
                                .append('\n');
                    });
        }

        for (EconomicTransaction transaction :
                snapshot.transactions()) {

            out.append("TX\t")
                    .append(transaction.sequence()).append('\t')
                    .append(transaction.kind()).append('\t')
                    .append(idOrZero(transaction.source()))
                    .append('\t')
                    .append(idOrZero(transaction.destination()))
                    .append('\t')
                    .append(transaction.resource().value())
                    .append('\t')
                    .append(transaction.quantity())
                    .append('\t')
                    .append(Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    transaction.cause().getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            ))
                    .append('\n');
        }

        return out.toString();
    }

    private static long idOrZero(InventoryId id) {
        return id == null ? 0L : id.value();
    }

    private static InventoryId nullableId(long value) {
        return value == 0L ? null : new InventoryId(value);
    }

    private static Snapshot parse(
            String content,
            ResourceCatalog catalog
    ) throws IOException {

        if (!content.endsWith("\n")) {
            throw new IOException("Truncated economy snapshot");
        }

        String[] lines = content.split("\n", -1);

        if (lines.length < 4
                || !HEADER.equals(lines[0])) {
            throw new IOException(
                    "Unsupported economy snapshot header"
            );
        }

        int checksumIndex = lines.length - 2;

        String[] checksum = fields(
                lines[checksumIndex], 2, "CHECKSUM"
        );

        StringBuilder payload = new StringBuilder();

        for (int i = 0; i < checksumIndex; i++) {
            payload.append(lines[i]).append('\n');
        }

        if (!sha256(payload.toString()).equals(
                checksum[1]
        )) {
            throw new IOException(
                    "Economy snapshot checksum mismatch"
            );
        }

        String[] next = fields(lines[1], 2, "NEXT_ID");
        long nextId = positive(next[1]);

        Map<InventoryId, MutableRecord> records =
                new TreeMap<>();

        List<EconomicTransaction> journal =
                new ArrayList<>();

        boolean transactionsStarted = false;

        for (int i = 2; i < checksumIndex; i++) {
            String line = lines[i];

            if (line.startsWith("INVENTORY\t")) {
                if (transactionsStarted) {
                    throw new IOException(
                            "Inventory after transaction"
                    );
                }

                String[] p = fields(line, 5, "INVENTORY");

                InventoryId id = new InventoryId(
                        positive(p[1])
                );

                EconomicOwner owner = new EconomicOwner(
                        EconomicOwner.Kind.valueOf(p[2]),
                        positive(p[3])
                );

                SettlementId location =
                        new SettlementId(positive(p[4]));

                if (records.putIfAbsent(
                        id,
                        new MutableRecord(owner, location)
                ) != null) {
                    throw new IOException(
                            "Duplicate inventory ID"
                    );
                }

            } else if (line.startsWith("BALANCE\t")) {
                if (transactionsStarted) {
                    throw new IOException(
                            "Balance after transaction"
                    );
                }

                String[] p = fields(line, 4, "BALANCE");

                InventoryId id = new InventoryId(
                        positive(p[1])
                );

                MutableRecord record = records.get(id);

                if (record == null) {
                    throw new IOException(
                            "Balance references unknown inventory"
                    );
                }

                ResourceId resource = new ResourceId(p[2]);
                catalog.require(resource);

                if (record.balances.putIfAbsent(
                        resource, positive(p[3])
                ) != null) {
                    throw new IOException(
                            "Duplicate resource balance"
                    );
                }

            } else if (line.startsWith("TX\t")) {
                transactionsStarted = true;

                String[] p = fields(line, 8, "TX");

                ResourceId resource = new ResourceId(p[5]);
                catalog.require(resource);

                String cause = new String(
                        Base64.getUrlDecoder().decode(p[7]),
                        StandardCharsets.UTF_8
                );

                EconomicTransaction transaction =
                        new EconomicTransaction(
                                positive(p[1]),
                                EconomicTransaction.Kind.valueOf(
                                        p[2]
                                ),
                                nullableId(
                                        Long.parseLong(p[3])
                                ),
                                nullableId(
                                        Long.parseLong(p[4])
                                ),
                                resource,
                                positive(p[6]),
                                cause
                        );

                journal.add(transaction);

            } else {
                throw new IOException(
                        "Unknown economy record at line "
                                + (i + 1)
                );
            }
        }

        List<InventoryRecord> inventories =
                new ArrayList<>();

        for (var entry : records.entrySet()) {
            MutableRecord record = entry.getValue();

            inventories.add(new InventoryRecord(
                    entry.getKey(),
                    record.owner,
                    record.location,
                    record.balances
            ));
        }

        return new Snapshot(
                nextId, inventories, journal
        );
    }

    private static String[] fields(
            String line,
            int count,
            String type
    ) throws IOException {

        String[] fields = line.split("\t", -1);

        if (fields.length != count
                || !fields[0].equals(type)) {
            throw new IOException(
                    "Invalid " + type + " record"
            );
        }

        return fields;
    }

    private static long positive(String text)
            throws IOException {

        try {
            long value = Long.parseLong(text);

            if (value <= 0L) {
                throw new NumberFormatException();
            }

            return value;
        } catch (NumberFormatException ex) {
            throw new IOException(
                    "Expected positive integer: " + text,
                    ex
            );
        }
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256"
            );

            return HexFormat.of().formatHex(
                    digest.digest(
                            content.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    )
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 unavailable", ex
            );
        }
    }

    private static final class MutableRecord {
        final EconomicOwner owner;
        final SettlementId location;

        final Map<ResourceId, Long> balances =
                new TreeMap<>();

        MutableRecord(
                EconomicOwner owner,
                SettlementId location
        ) {
            this.owner = owner;
            this.location = location;
        }
    }
}


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
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Batch 21.1
 *
 * Versioned inventory persistence.
 *
 * Loading constructs a new manager so malformed
 * files cannot partially mutate an existing manager.
 *
 * Saves use a temporary file followed by replacement.
 *
 * This class does not coordinate transactions with
 * the other Known World persistence domains.
 */
public final class InventoryPersistence {

    private static final String FILE_NAME =
            "economy_inventories.tsv";

    private static final String TEMP_FILE_NAME =
            "economy_inventories.tsv.tmp";

    private static final String HEADER =
            "KNOWNWORLD_ECONOMY_INVENTORIES\t1";

    private final Path directory;
    private final Path file;

    public InventoryPersistence(Path directory) {
        this.directory = Objects.requireNonNull(
                directory,
                "directory"
        );
        this.file = directory.resolve(FILE_NAME);
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    /**
     * Save a consistent, quiescent inventory state.
     *
     * The caller must prevent concurrent economic
     * mutations during snapshot creation.
     */
    public void save(InventoryManager manager)
            throws IOException {

        Objects.requireNonNull(manager, "manager");

        List<String> lines = new ArrayList<>();

        lines.add(HEADER);
        lines.add("NEXT_ID\t" + manager.nextId());

        for (ResourceInventory inventory : manager.all()) {
            lines.add(
                    "INVENTORY\t"
                            + inventory.id().value()
                            + "\t"
                            + inventory.owner().kind().name()
                            + "\t"
                            + inventory.owner().id()
                            + "\t"
                            + inventory.location().value()
            );

            for (Map.Entry<ResourceId, Long> entry :
                    new TreeMap<>(
                            inventory.balances()
                    ).entrySet()) {

                lines.add(
                        "BALANCE\t"
                                + inventory.id().value()
                                + "\t"
                                + entry.getKey().value()
                                + "\t"
                                + entry.getValue()
                );
            }
        }

        String payload = String.join("\n", lines) + "\n";

        String content = payload
                + "CHECKSUM\t"
                + sha256(payload)
                + "\n";

        Files.createDirectories(directory);

        Path temporary = directory.resolve(
                TEMP_FILE_NAME
        );

        try {
            Files.writeString(
                    temporary,
                    content,
                    StandardCharsets.UTF_8
            );

            try {
                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * Load into a newly constructed manager.
     *
     * If no save exists, an empty manager is returned.
     *
     * IMPORTANT:
     * An absent file must not be interpreted as an
     * instruction to create arbitrary resources.
     */
    public InventoryManager load(ResourceCatalog catalog)
            throws IOException {

        Objects.requireNonNull(catalog, "catalog");

        if (!Files.exists(file)) {
            return new InventoryManager(catalog);
        }

        String content = Files.readString(
                file,
                StandardCharsets.UTF_8
        );

        try {
            return parse(content, catalog);
        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            throw new IOException(
                    "Invalid inventory save: " + file,
                    exception
            );
        }
    }

    private static InventoryManager parse(
            String content,
            ResourceCatalog catalog
    ) throws IOException {

        if (content.isEmpty() || !content.endsWith("\n")) {
            throw new IOException(
                    "Inventory save is empty or truncated"
            );
        }

        String[] lines = content.split("\n", -1);

        if (lines.length < 4) {
            throw new IOException(
                    "Inventory save has too few records"
            );
        }

        if (!HEADER.equals(lines[0])) {
            throw new IOException(
                    "Unsupported inventory save header"
            );
        }

        String[] next = fields(lines[1], 2, "NEXT_ID");

        long savedNextId = parsePositiveLong(next[1]);

        int checksumLine = lines.length - 2;

        String[] checksumFields = fields(
                lines[checksumLine],
                2,
                "CHECKSUM"
        );

        StringBuilder payload = new StringBuilder();

        for (int i = 0; i < checksumLine; i++) {
            payload.append(lines[i]).append('\n');
        }

        if (!sha256(payload.toString()).equals(
                checksumFields[1]
        )) {
            throw new IOException(
                    "Inventory save checksum mismatch"
            );
        }

        Map<InventoryId, InventoryRecord> records =
                new TreeMap<>();

        for (int i = 2; i < checksumLine; i++) {
            String line = lines[i];

            if (line.startsWith("INVENTORY\t")) {
                String[] p = fields(line, 5, "INVENTORY");

                InventoryId id = new InventoryId(
                        parsePositiveLong(p[1])
                );

                EconomicOwner.Kind kind =
                        EconomicOwner.Kind.valueOf(p[2]);

                EconomicOwner owner = new EconomicOwner(
                        kind,
                        parsePositiveLong(p[3])
                );

                SettlementId location = new SettlementId(
                        parsePositiveLong(p[4])
                );

                if (records.putIfAbsent(
                        id,
                        new InventoryRecord(owner, location)
                ) != null) {
                    throw new IOException(
                            "Duplicate inventory: " + id
                    );
                }

            } else if (line.startsWith("BALANCE\t")) {
                String[] p = fields(line, 4, "BALANCE");

                InventoryId id = new InventoryId(
                        parsePositiveLong(p[1])
                );

                InventoryRecord record = records.get(id);

                if (record == null) {
                    throw new IOException(
                            "Balance references missing inventory: " + id
                    );
                }

                ResourceId resource = new ResourceId(p[2]);
                catalog.require(resource);

                long quantity = parsePositiveLong(p[3]);

                if (record.balances.putIfAbsent(
                        resource,
                        quantity
                ) != null) {
                    throw new IOException(
                            "Duplicate resource balance: "
                                    + id + "/" + resource
                    );
                }

            } else {
                throw new IOException(
                        "Unknown inventory record at line "
                                + (i + 1)
                );
            }
        }

        InventoryManager manager = new InventoryManager(
                catalog
        );

        for (Map.Entry<InventoryId, InventoryRecord> entry :
                records.entrySet()) {

            InventoryRecord record = entry.getValue();

            manager.restoreInventory(
                    entry.getKey(),
                    record.owner,
                    record.location,
                    record.balances
            );
        }

        manager.restoreNextId(savedNextId);

        return manager;
    }

    private static String[] fields(
            String line,
            int expectedCount,
            String recordType
    ) throws IOException {

        String[] result = line.split("\t", -1);

        if (result.length != expectedCount
                || !result[0].equals(recordType)) {
            throw new IOException(
                    "Invalid " + recordType + " record"
            );
        }

        return result;
    }

    private static long parsePositiveLong(String value)
            throws IOException {

        try {
            long parsed = Long.parseLong(value);

            if (parsed <= 0L) {
                throw new NumberFormatException(
                        "Nonpositive value"
                );
            }

            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Invalid positive integer: " + value,
                    exception
            );
        }
    }

    private static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance(
                    "SHA-256"
            ).digest(
                    content.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(digest);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    private static final class InventoryRecord {
        private final EconomicOwner owner;
        private final SettlementId location;

        private final Map<ResourceId, Long> balances =
                new TreeMap<>();

        private InventoryRecord(
                EconomicOwner owner,
                SettlementId location
        ) {
            this.owner = owner;
            this.location = location;
        }
    }
}

package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.economy.CampaignSettlementAgricultureDispatcher;
import dev.dtzudontsay.knownworld.simulation.economy.InventoryId;
import dev.dtzudontsay.knownworld.simulation.economy.MultiFarmSettlementFoodCycle;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.IOException;
import java.math.BigInteger;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Batch 22.22. Durable evidence of anonymous settlement food shortages.
 *
 * Reads only COMPLETED agricultural results. A day with no committed evidence
 * is unknown, NOT a healthy day. Incomplete years cannot modify demography.
 *
 * Does not change people, needs, inventories or the economic journal.
 * Persistence is atomic per this file; NOT atomic across the economy/calendar.
 */
public final class CohortFoodStressLedger {
    private static final String HEADER = "KNOWNWORLD_COHORT_FOOD_STRESS\t1";
    private static final long MAX_BYTES = 8L * 1024L * 1024L;
    private static final String NAME = "cohort_food_stress_v1.tsv";

    private record Coverage(int year, long firstDay, long lastEndDay,
                            long observedDays, long deficitPermilleDays) {}
    private record FileState(int daysPerYear,
                             TreeMap<SettlementId, TreeMap<Integer, Coverage>> records) {}

    private final Path file;
    private final Path temp;
    private FileState state;
    private boolean blocked;

    public CohortFoodStressLedger(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        file = directory.resolve(NAME);
        temp = directory.resolve(NAME + ".tmp");
        state = load();
    }

    public Path file() { return file; }
    public synchronized boolean blocked() { return blocked; }

    /**
     * Execute the existing guarded campaign operation and retain its ACTUAL
     * anonymous household shortage in the durable food-stress file.
     *
     * A failure after economic commit must be manually reconciled. It is never
     * safe to rerun a committed agricultural day to 'repair' shortage history.
     */
    public synchronized CampaignSettlementAgricultureDispatcher.DayResult executeAndRecord(
            CampaignSettlementAgricultureDispatcher dispatcher,
            long startDay, long endDay, long simulationTick,
            Map<SettlementId, InventoryId> anonymousPantries,
            int daysPerYear) throws IOException {
        Objects.requireNonNull(dispatcher, "dispatcher");
        if (blocked) throw new IOException("Food-stress ledger blocked");
        CampaignSettlementAgricultureDispatcher.DayResult completed =
                dispatcher.executeDay(startDay, endDay, simulationTick);
        recordCommittedDay(completed, anonymousPantries, daysPerYear);
        return completed;
    }

    /**
     * Records only the consumer identified by each settlement's aggregate
     * pantry; shortages assigned to named NPC households are excluded.
     * All settlement entries and the file checkpoint commit together.
     */
    public synchronized void recordCommittedDay(
            CampaignSettlementAgricultureDispatcher.DayResult completed,
            Map<SettlementId, InventoryId> anonymousPantries,
            int daysPerYear) throws IOException {
        Objects.requireNonNull(completed, "completed");
        Objects.requireNonNull(anonymousPantries, "anonymousPantries");
        if (blocked) throw new IOException("Food-stress ledger blocked");
        if (daysPerYear <= 0 || completed.startDayInclusive() < 0
                || completed.startDayInclusive() == Long.MAX_VALUE
                || completed.endDayExclusive() != completed.startDayInclusive() + 1L
                || completed.simulationTick() < 0 || anonymousPantries.isEmpty()) {
            throw new IllegalArgumentException("Invalid completed food-stress day");
        }
        if (state.daysPerYear() != 0 && state.daysPerYear() != daysPerYear) {
            throw new IllegalStateException("Campaign year length changed for food stress");
        }
        int year = Math.toIntExact(completed.startDayInclusive() / daysPerYear);
        long yearStart = Math.multiplyExact((long) year, daysPerYear);
        TreeMap<SettlementId, MultiFarmSettlementFoodCycle.Result> results =
                new TreeMap<>();
        for (var row : completed.settlements()) {
            if (results.putIfAbsent(row.settlement(), row.food()) != null) {
                throw new IllegalArgumentException("Duplicate settlement in completed day");
            }
        }

        TreeMap<SettlementId, TreeMap<Integer, Coverage>> next = copy(state.records());
        Set<InventoryId> seenPantries = new HashSet<>();
        for (var entry : anonymousPantries.entrySet()) {
            SettlementId settlement = Objects.requireNonNull(entry.getKey());
            InventoryId pantry = Objects.requireNonNull(entry.getValue());
            if (!seenPantries.add(pantry)) {
                throw new IllegalArgumentException("Anonymous pantry assigned twice");
            }
            MultiFarmSettlementFoodCycle.Result food = results.get(settlement);
            if (food == null) {
                throw new IllegalArgumentException("Settlement absent from completed day: " + settlement);
            }
            MultiFarmSettlementFoodCycle.HouseholdResult matching = null;
            for (var household : food.households()) {
                if (pantry.equals(household.inventory())) {
                    if (matching != null) throw new IllegalArgumentException("Duplicate aggregate consumer");
                    matching = household;
                }
            }
            if (matching == null || matching.demandKg() <= 0L
                    || matching.shortageKg() < 0L
                    || matching.shortageKg() > matching.demandKg()) {
                throw new IllegalArgumentException("Missing or invalid aggregate food demand");
            }
            int deficit = BigInteger.valueOf(matching.shortageKg())
                    .multiply(BigInteger.valueOf(1000L))
                    .divide(BigInteger.valueOf(matching.demandKg()))
                    .intValueExact();

            TreeMap<Integer, Coverage> years =
                    next.computeIfAbsent(settlement, unused -> new TreeMap<>());
            Coverage prior = years.get(year);
            Coverage updated;
            if (prior == null) {
                if (completed.startDayInclusive() != yearStart) {
                    throw new IllegalStateException("Food-stress recording must start at year boundary");
                }
                if (!years.isEmpty()) {
                    int latest = years.lastKey();
                    Coverage previousYear = years.get(latest);
                    if (latest != year - 1 || previousYear.observedDays() != daysPerYear) {
                        throw new IllegalStateException("Earlier food-stress year incomplete");
                    }
                }
                updated = new Coverage(year, yearStart, completed.endDayExclusive(), 1L, deficit);
            } else {
                if (prior.lastEndDay() != completed.startDayInclusive()) {
                    throw new IllegalStateException("Missing or replayed food-stress day");
                }
                updated = new Coverage(year, prior.firstDay(), completed.endDayExclusive(),
                        Math.addExact(prior.observedDays(), 1L),
                        Math.addExact(prior.deficitPermilleDays(), deficit));
            }
            if (updated.observedDays() > daysPerYear) {
                throw new IllegalStateException("Too many observations in food-stress year");
            }
            years.put(year, updated);
            years.headMap(year - 1, false).clear(); // keep current and previous year
        }
        FileState candidate = new FileState(daysPerYear, next);
        try {
            save(candidate);
            state = candidate;
        } catch (IOException | RuntimeException failure) {
            blocked = true;
            throw failure;
        }
    }

    /**
     * A completed year must contain every campaign day. This method is used by
     * the EXISTING annual cohort-demography engine, not a second death model.
     */
    public synchronized int completedYearDeficitPermille(
            SettlementId settlement, int completedYear, int daysPerYear) {
        Objects.requireNonNull(settlement, "settlement");
        if (blocked || daysPerYear <= 0 || completedYear < 0
                || state.daysPerYear() != daysPerYear) {
            throw new IllegalStateException("Food-stress calendar missing or incompatible");
        }
        TreeMap<Integer, Coverage> years = state.records().get(settlement);
        Coverage coverage = years == null ? null : years.get(completedYear);
        long start = Math.multiplyExact((long) completedYear, daysPerYear);
        if (coverage == null || coverage.firstDay() != start
                || coverage.lastEndDay() != start + daysPerYear
                || coverage.observedDays() != daysPerYear) {
            throw new IllegalStateException("Incomplete food-stress year for " + settlement);
        }
        return Math.toIntExact(coverage.deficitPermilleDays() / daysPerYear);
    }

    private static TreeMap<SettlementId, TreeMap<Integer, Coverage>> copy(
            TreeMap<SettlementId, TreeMap<Integer, Coverage>> original) {
        TreeMap<SettlementId, TreeMap<Integer, Coverage>> result = new TreeMap<>();
        original.forEach((id, years) -> result.put(id, new TreeMap<>(years)));
        return result;
    }

    private void save(FileState candidate) throws IOException {
        if (Files.exists(temp)) throw new IOException("Interrupted food-stress write");
        StringBuilder body = new StringBuilder(HEADER).append('\n')
                .append("CALENDAR\t").append(candidate.daysPerYear()).append('\n');
        candidate.records().forEach((id, years) -> years.forEach((year, row) ->
                body.append("YEAR\t").append(id.value()).append('\t')
                        .append(year).append('\t').append(row.firstDay()).append('\t')
                        .append(row.lastEndDay()).append('\t').append(row.observedDays())
                        .append('\t').append(row.deficitPermilleDays()).append('\n')));
        String payload = body.toString();
        byte[] bytes = (payload + "SHA256\t" + sha(payload) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Food-stress file too large");
        Files.createDirectories(file.getParent());
        try (FileChannel channel = FileChannel.open(temp,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IOException("Atomic food-stress checkpoint unavailable", e);
        }
        if (!load().equals(candidate)) {
            throw new IOException("Published food-stress checkpoint differs from planned state");
        }
    }

    private FileState load() throws IOException {
        if (Files.exists(temp)) throw new IOException("Interrupted food-stress checkpoint");
        if (!Files.exists(file)) return new FileState(0, new TreeMap<>());
        if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) {
            throw new IOException("Invalid food-stress checkpoint file");
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (!text.endsWith("\n")) throw new IOException("Truncated food-stress file");
        String[] lines = text.split("\n", -1);
        if (lines.length < 4 || !HEADER.equals(lines[0])) {
            throw new IOException("Unsupported food-stress file");
        }
        StringBuilder signed = new StringBuilder();
        for (int i = 0; i < lines.length - 2; i++) signed.append(lines[i]).append('\n');
        if (!("SHA256\t" + sha(signed.toString())).equals(lines[lines.length - 2])) {
            throw new IOException("Food-stress checksum mismatch");
        }
        try {
            String[] calendar = lines[1].split("\t", -1);
            if (calendar.length != 2 || !"CALENDAR".equals(calendar[0])) {
                throw new IOException("Invalid food-stress calendar header");
            }
            int days = Integer.parseInt(calendar[1]);
            if (days <= 0) throw new IOException("Invalid food-stress year length");
            TreeMap<SettlementId, TreeMap<Integer, Coverage>> records = new TreeMap<>();
            SettlementId lastSettlement = null;
            int lastYear = -1;
            for (int i = 2; i < lines.length - 2; i++) {
                String[] parts = lines[i].split("\t", -1);
                if (parts.length != 7 || !"YEAR".equals(parts[0])) {
                    throw new IOException("Invalid food-stress record");
                }
                SettlementId id = new SettlementId(Long.parseLong(parts[1]));
                int year = Integer.parseInt(parts[2]);
                long first = Long.parseLong(parts[3]);
                long last = Long.parseLong(parts[4]);
                long observed = Long.parseLong(parts[5]);
                long sum = Long.parseLong(parts[6]);
                if (year < 0 || first != Math.multiplyExact((long) year, days)
                        || observed <= 0L || observed > days
                        || last != first + observed
                        || sum < 0L || sum > observed * 1000L) {
                    throw new IOException("Invalid food-stress coverage");
                }
                if (lastSettlement != null) {
                    int cmp = id.compareTo(lastSettlement);
                    if (cmp < 0 || (cmp == 0 && year <= lastYear)) {
                        throw new IOException("Unsorted or duplicate food-stress years");
                    }
                }
                records.computeIfAbsent(id, unused -> new TreeMap<>())
                        .put(year, new Coverage(year, first, last, observed, sum));
                lastSettlement = id;
                lastYear = year;
            }
            return new FileState(days, records);
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new IOException("Invalid food-stress checkpoint values", e);
        }
    }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
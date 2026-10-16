
package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
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
 * Versioned checkpoint for anonymous population only. This is deliberately
 * NOT an independent calendar: day must match the persisted campaign calendar
 * when loaded. Cross-domain atomic save coordination remains outstanding.
 *
 * Missing file means uninitialized, NOT proof that everyone is accounted for.
 */
public final class PopulationCohortPersistence {
    private static final String HEADER = "KNOWNWORLD_POPULATION_COHORTS\t1";
    private static final String FILE = "population_cohorts_v1.tsv";
    private static final long MAX_BYTES = 8L * 1024L * 1024L;
    private final Path file;
    private final Path temp;

    public PopulationCohortPersistence(Path directory) {
        Objects.requireNonNull(directory, "directory");
        this.file = directory.resolve(FILE);
        this.temp = directory.resolve(FILE + ".tmp");
    }

    public Path file() { return file; }

    public synchronized void save(long absoluteDay, PopulationCohortManager manager)
            throws IOException {
        if (absoluteDay < 0L) throw new IllegalArgumentException("Negative campaign day");
        Objects.requireNonNull(manager, "manager");
        if (Files.exists(temp)) throw new IOException("Unresolved population checkpoint temporary file");
        PopulationCohortManager.Snapshot state = manager.snapshot();
        StringBuilder body = new StringBuilder(HEADER).append('\n')
                .append("DAY\t").append(absoluteDay).append('\n');
        state.surveyed().entrySet().stream()
                .sorted(Comparator.comparingLong(x -> x.getKey().value()))
                .forEach(e -> body.append("SURVEY\t").append(e.getKey().value())
                        .append('\t').append(e.getValue()).append('\n'));
        for (var row : state.cohorts()) {
            var k = row.key();
            body.append("COHORT\t").append(k.settlement().value()).append('\t')
                    .append(k.age()).append('\t').append(k.sex()).append('\t')
                    .append(k.cultureId()).append('\t').append(k.religionId()).append('\t')
                    .append(k.socialClassId()).append('\t').append(k.occupationId()).append('\t')
                    .append(row.people()).append('\n');
        }
        String payload = body.toString();
        byte[] bytes = (payload + "SHA256\t" + sha(payload) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Population checkpoint exceeds limit");
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
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException("Atomic population checkpoint writes unavailable", exception);
        }
        // Verify bytes after publication; an unreadable checkpoint never passes silently.
        read(absoluteDay);
    }

    /** Returns false for a genuinely absent checkpoint; corrupt/ambiguous states throw. */
    public synchronized boolean loadInto(long expectedDay,
                                         PopulationCohortManager manager) throws IOException {
        Objects.requireNonNull(manager, "manager");
        PopulationCohortManager.Snapshot state = read(expectedDay);
        if (state == null) return false;
        try {
            manager.replaceAll(state);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IOException("Population checkpoint references invalid current world data", exception);
        }
        return true;
    }

    private PopulationCohortManager.Snapshot read(long expectedDay) throws IOException {
        if (expectedDay < 0L) throw new IllegalArgumentException("Negative campaign day");
        if (Files.exists(temp)) throw new IOException("Interrupted population checkpoint write");
        if (!Files.exists(file)) return null;
        if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) {
            throw new IOException("Invalid population checkpoint file");
        }
        String value = Files.readString(file, StandardCharsets.UTF_8);
        if (!value.endsWith("\n")) throw new IOException("Truncated population checkpoint");
        String[] lines = value.split("\n", -1);
        if (lines.length < 4 || !HEADER.equals(lines[0])) {
            throw new IOException("Unsupported population checkpoint format");
        }
        StringBuilder original = new StringBuilder();
        for (int i = 0; i < lines.length - 2; i++) original.append(lines[i]).append('\n');
        if (!("SHA256\t" + sha(original.toString())).equals(lines[lines.length - 2])) {
            throw new IOException("Population checkpoint checksum mismatch");
        }
        try {
            String[] day = lines[1].split("\t", -1);
            if (day.length != 2 || !"DAY".equals(day[0])
                    || Long.parseLong(day[1]) != expectedDay) {
                throw new IOException("Population and campaign calendar checkpoints disagree");
            }
            Map<SettlementId, PopulationCohortManager.Basis> surveyed = new HashMap<>();
            List<PopulationCohortManager.Cohort> cohorts = new ArrayList<>();
            boolean seenCohorts = false;
            long lastSurveyId = 0L;
            PopulationCohortManager.Key lastKey = null;
            for (int i = 2; i < lines.length - 2; i++) {
                String[] parts = lines[i].split("\t", -1);
                if (parts.length == 3 && "SURVEY".equals(parts[0]) && !seenCohorts) {
                    SettlementId id = new SettlementId(Long.parseLong(parts[1]));
                    if (id.value() <= lastSurveyId || surveyed.putIfAbsent(id,
                            PopulationCohortManager.Basis.valueOf(parts[2])) != null) {
                        throw new IOException("Unsorted or duplicate population survey");
                    }
                    lastSurveyId = id.value();
                } else if (parts.length == 9 && "COHORT".equals(parts[0])) {
                    seenCohorts = true;
                    PopulationCohortManager.Key key = new PopulationCohortManager.Key(
                            new SettlementId(Long.parseLong(parts[1])),
                            PopulationCohortManager.AgeBand.valueOf(parts[2]),
                            NpcSex.valueOf(parts[3]), parts[4], parts[5], parts[6], parts[7]);
                    long count = Long.parseLong(parts[8]);
                    if (lastKey != null && lastKey.compareTo(key) >= 0) {
                        throw new IOException("Unsorted or duplicate population cohorts");
                    }
                    cohorts.add(new PopulationCohortManager.Cohort(key, count));
                    lastKey = key;
                } else {
                    throw new IOException("Invalid population checkpoint row");
                }
            }
            return new PopulationCohortManager.Snapshot(surveyed, cohorts);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid population checkpoint values", exception);
        }
    }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}

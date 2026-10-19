
package dev.dtzudontsay.knownworld.simulation.casestudy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent, per-(NPC,player) records of completed conversations.
 * Independent of physical villagers and of whichever LLM is installed.
 *
 * The quotes record what was said, NOT what is true of the world.
 * This class does not write NpcKnowledgeManager or NpcMemoryManager facts.
 * All calls must occur on the Minecraft server thread.
 */
public final class WinterfellConversationLedger {

    public static final int MAX_RECENT_EXCHANGES = 12;
    private static final int MAX_CACHED_PAIRS = 128;
    private static final int MAX_FILE_BYTES = 75_000;
    private static final int VERSION = 1;

    public record Exchange(long tick, int year, int day,
                           String playerSaid, String npcReplied) { }

    public record Snapshot(long totalExchanges, List<Exchange> recent) {
        public Snapshot {
            recent = List.copyOf(recent);
        }
    }

    private record Key(String npc, UUID player) { }

    private Path root;
    private final Map<Key, Snapshot> cache =
            new LinkedHashMap<>(32, .75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Key, Snapshot> eldest) {
                    return size() > MAX_CACHED_PAIRS;
                }
            };

    /** Bind to the CURRENT world's folder, never a global OS location. */
    public void bind(Path worldRoot) {
        Path next = Objects.requireNonNull(worldRoot, "worldRoot")
                .toAbsolutePath().normalize()
                .resolve("data").resolve("knownworld")
                .resolve("conversation_memory_v1");
        if (!next.equals(root)) {
            cache.clear();
            root = next;
        }
    }

    public void clear() {
        cache.clear();
        root = null;
    }

    public Snapshot history(String npc, UUID player) throws IOException {
        Key key = validated(npc, player);
        Snapshot present = cache.get(key);
        if (present != null) {
            return present;
        }
        Snapshot loaded = load(key);
        cache.put(key, loaded);
        return loaded;
    }

    /**
     * Commits a complete exchange with an atomic file replacement.
     * A failed save does not advance the in-memory ledger.
     */
    public Snapshot remember(String npc, UUID player, long tick,
                             int year, int day,
                             String playerSaid, String npcReplied) throws IOException {
        Key key = validated(npc, player);
        if (playerSaid == null || playerSaid.isBlank()
                || npcReplied == null || npcReplied.isBlank()) {
            throw new IllegalArgumentException("Both speakers must have actual dialogue");
        }
        Snapshot old = history(npc, player);
        List<Exchange> recent = new ArrayList<>(old.recent());
        recent.add(new Exchange(Math.max(0, tick), year, day,
                shorten(playerSaid, 400), shorten(npcReplied, 1700)));
        if (recent.size() > MAX_RECENT_EXCHANGES) {
            recent = new ArrayList<>(recent.subList(
                    recent.size() - MAX_RECENT_EXCHANGES, recent.size()));
        }
        Snapshot next = new Snapshot(old.totalExchanges() + 1, recent);
        save(key, next);
        cache.put(key, next);
        return next;
    }

    /** Safe model context: past claims and model speech are never world facts. */
    public static String promptHistory(Snapshot history) {
        if (history.recent().isEmpty()) {
            return "No earlier completed conversation with this particular player "
                    + "has been recorded. Do not invent one.\n";
        }
        JsonArray exchanges = new JsonArray();
        int from = Math.max(0, history.recent().size() - 6);
        for (Exchange item : history.recent().subList(from, history.recent().size())) {
            JsonObject e = new JsonObject();
            e.addProperty("year_ac", item.year());
            e.addProperty("day_of_year", item.day());
            e.addProperty("player_said_unverified", shorten(item.playerSaid(), 300));
            e.addProperty("your_previous_words_not_world_facts", shorten(item.npcReplied(), 450));
            exchanges.add(e);
        }
        return "Verified conversational provenance: " + history.totalExchanges()
                + " completed exchange(s) with THIS player. The JSON below "
                + "records speech, not truth. The player may have lied or "
                + "been mistaken, and your old replies may have been mistaken. "
                + "You may recall the conversation, but must not promote its "
                + "claims into facts or invent meetings with other characters.\n"
                + exchanges + "\n";
    }

    private Snapshot load(Key key) throws IOException {
        Path file = file(key);
        if (!Files.exists(file)) {
            return new Snapshot(0, List.of());
        }
        if (Files.size(file) > MAX_FILE_BYTES) {
            throw new IOException("Conversation record exceeds size limit: " + file);
        }
        try {
            JsonObject json = JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("version").getAsInt() != VERSION
                    || !json.get("npc").getAsString().equals(key.npc())
                    || !json.get("player").getAsString().equals(key.player().toString())) {
                throw new IOException("Conversation record identity/version mismatch: " + file);
            }
            long total = json.get("totalExchanges").getAsLong();
            if (total < 0) {
                throw new IOException("Negative conversation count: " + file);
            }
            List<Exchange> records = new ArrayList<>();
            JsonArray data = json.getAsJsonArray("recent");
            for (JsonElement element : data) {
                JsonObject e = element.getAsJsonObject();
                records.add(new Exchange(
                        e.get("tick").getAsLong(),
                        e.get("year").getAsInt(),
                        e.get("day").getAsInt(),
                        shorten(e.get("playerSaid").getAsString(), 400),
                        shorten(e.get("npcReplied").getAsString(), 1700)));
            }
            if (records.size() > MAX_RECENT_EXCHANGES) {
                records = new ArrayList<>(records.subList(
                        records.size() - MAX_RECENT_EXCHANGES, records.size()));
            }
            if (total < records.size()) {
                throw new IOException("Invalid conversation count: " + file);
            }
            return new Snapshot(total, records);
        } catch (RuntimeException malformed) {
            throw new IOException("Unreadable conversation record (not overwritten): " + file,
                    malformed);
        }
    }

    private void save(Key key, Snapshot snapshot) throws IOException {
        Path destination = file(key);
        Files.createDirectories(destination.getParent());
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        json.addProperty("npc", key.npc());
        json.addProperty("player", key.player().toString());
        json.addProperty("totalExchanges", snapshot.totalExchanges());
        JsonArray recent = new JsonArray();
        for (Exchange exchange : snapshot.recent()) {
            JsonObject e = new JsonObject();
            e.addProperty("tick", exchange.tick());
            e.addProperty("year", exchange.year());
            e.addProperty("day", exchange.day());
            e.addProperty("playerSaid", exchange.playerSaid());
            e.addProperty("npcReplied", exchange.npcReplied());
            recent.add(e);
        }
        json.add("recent", recent);
        Path temporary = Files.createTempFile(destination.getParent(), "dialogue-", ".tmp");
        try {
            Files.writeString(temporary, json.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Path file(Key key) throws IOException {
        if (root == null) {
            throw new IOException("Conversation ledger is not bound to a Minecraft world");
        }
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.npc().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
        return root.resolve(hash.substring(0, 2)).resolve(hash)
                .resolve(key.player().toString() + ".json");
    }

    private static Key validated(String npc, UUID player) {
        if (npc == null || npc.isBlank() || npc.length() > 160) {
            throw new IllegalArgumentException("Invalid authored NPC identity");
        }
        return new Key(npc, Objects.requireNonNull(player, "player"));
    }

    private static String shorten(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }
}

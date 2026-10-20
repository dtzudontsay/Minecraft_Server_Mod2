
package dev.dtzudontsay.knownworld.simulation.casestudy;

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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A5: Persistent player-to-NPC relationship state.
 *
 * NPC-to-NPC relationships still belong to NpcRelationshipManager.
 * Every relationship here is indexed by a stable authored
 * NPC identifier and the actual Minecraft player UUID.
 *
 * This is an attitude store, not a repository of objective
 * world facts, and is accessed only on the server thread.
 */
public final class WinterfellPlayerRelationshipStore {

    private static final int VERSION = 1;
    private static final int MAX_CACHE = 256;
    private static final long ACTION_COOLDOWN_TICKS = 100;

    public enum Action {
        PRAISE,
        INSULT,
        THREATEN
    }

    public record Snapshot(
            double affection,
            double trust,
            double respect,
            double fear,
            double familiarity,
            long conversations,
            long syncedExchanges,
            long lastActionTick,
            String lastAction
    ) {
        public Snapshot {
            lastAction = lastAction == null ? "" : lastAction;
        }
    }

    public record ActionResult(
            boolean accepted,
            Snapshot relationship,
            String message
    ) { }

    private record Key(String npc, UUID player) { }

    private Path root;

    private final Map<Key, Snapshot> cache =
            new LinkedHashMap<>(32, 0.75f, true) {

                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<Key, Snapshot> eldest
                ) {
                    return size() > MAX_CACHE;
                }
            };

    public void bind(Path worldRoot) {

        Path next = Objects.requireNonNull(
                        worldRoot, "worldRoot"
                ).toAbsolutePath().normalize()
                .resolve("data")
                .resolve("knownworld")
                .resolve("player_relationships_v1");

        if (!next.equals(root)) {
            cache.clear();
            root = next;
        }
    }

    public void clear() {
        cache.clear();
        root = null;
    }

    public Snapshot view(
            String npc,
            UUID player,
            long conversationBaseline
    ) throws IOException {

        Key key = key(npc, player);

        Snapshot current = cache.get(key);

        if (current != null) {
            return current;
        }

        Snapshot loaded = load(
                key,
                Math.max(0, conversationBaseline)
        );

        cache.put(key, loaded);

        return loaded;
    }

    /**
     * Reconciles this store to the durable A3 conversation count.
     *
     * Calling this multiple times with the same count is
     * idempotent. Older A3 exchanges form an initial baseline,
     * not retroactive social rewards.
     */
    public Snapshot completedConversation(
            String npc,
            UUID player,
            long previousTotal,
            long newlySavedTotal
    ) throws IOException {

        Snapshot old = view(npc, player, previousTotal);

        if (newlySavedTotal <= old.syncedExchanges()) {
            return old;
        }

        long difference =
                newlySavedTotal - old.syncedExchanges();

        Snapshot next = new Snapshot(
                old.affection(),
                old.trust(),
                old.respect(),
                old.fear(),
                clamp01(
                        old.familiarity()
                                + 0.015 * Math.min(10, difference)
                ),
                old.conversations() + difference,
                newlySavedTotal,
                old.lastActionTick(),
                old.lastAction()
        );

        persist(key(npc, player), next);

        return next;
    }

    /**
     * Applies a deliberate social action.
     *
     * Caller must have already validated:
     * - the player's current conversation token;
     * - the NPC's identity and alive status;
     * - the physical distance between player and NPC;
     * - that this operation is not LLM-generated.
     */
    public ActionResult act(
            String npc,
            UUID player,
            long conversationBaseline,
            Action action,
            long simulationTick
    ) throws IOException {

        Objects.requireNonNull(action, "action");

        if (simulationTick < 0) {
            throw new IllegalArgumentException(
                    "simulationTick cannot be negative"
            );
        }

        Snapshot old = view(
                npc, player, conversationBaseline
        );

        if (old.lastActionTick() >= 0
                && simulationTick >= old.lastActionTick()
                && simulationTick - old.lastActionTick()
                < ACTION_COOLDOWN_TICKS) {

            return new ActionResult(
                    false,
                    old,
                    "Give the character a moment before another "
                            + "deliberate social action."
            );
        }

        double affection = old.affection();
        double trust = old.trust();
        double respect = old.respect();
        double fear = old.fear();

        switch (action) {

            case PRAISE -> {
                affection += 0.025;
                trust += 0.015;
                respect += 0.05;
            }

            case INSULT -> {
                affection -= 0.08;
                trust -= 0.04;
                respect -= 0.045;
            }

            case THREATEN -> {
                affection -= 0.08;
                trust -= 0.09;
                respect -= 0.04;
                fear += 0.12;
            }
        }

        Snapshot next = new Snapshot(
                clampSigned(affection),
                clampSigned(trust),
                clampSigned(respect),
                clamp01(fear),
                clamp01(old.familiarity() + 0.025),
                old.conversations(),
                old.syncedExchanges(),
                simulationTick,
                action.name()
        );

        persist(key(npc, player), next);

        return new ActionResult(
                true,
                next,
                "The character's attitude has changed."
        );
    }

    /**
     * Safe context for an LLM speech request.
     *
     * The numeric state affects speech tendencies,
     * not factual world authority or permissions.
     */
    public static String prompt(Snapshot state) {

        return """
                CURRENT RELATIONSHIP WITH THIS PLAYER

                These values describe how you presently feel
                toward the person standing in front of you.

                Affection: %s
                Trust: %s
                Respect: %s
                Fear: %s
                Familiarity: %s

                Completed conversations counted since A5: %d
                Latest deliberate social action: %s

                The relationship values do NOT establish
                objective truth about the player.
                They do NOT authorize secret disclosures,
                gifts, employment, political changes or promises.

                Low trust can make you cautious or evasive.
                Low affection can make you cold or hostile.
                Respect can make you more courteous.
                Fear can make you guarded or defensive.
                Familiarity means you recognize this person;
                it does not necessarily imply friendship.

                Do not recite numeric values in character.
                Do not invent interactions not in your history.
                """.formatted(
                format(state.affection()),
                format(state.trust()),
                format(state.respect()),
                format(state.fear()),
                format(state.familiarity()),
                state.conversations(),
                state.lastAction().isBlank()
                        ? "none recorded"
                        : state.lastAction()
        );
    }

    private Snapshot load(
            Key key,
            long baseline
    ) throws IOException {

        Path file = file(key);

        if (!Files.exists(file)) {
            return neutral(baseline);
        }

        if (Files.size(file) > 16_384) {
            throw new IOException(
                    "Relationship file exceeds size limit: " + file
            );
        }

        try {

            JsonObject json = JsonParser.parseString(
                    Files.readString(
                            file, StandardCharsets.UTF_8
                    )
            ).getAsJsonObject();

            if (json.get("version").getAsInt() != VERSION
                    || !json.get("npc").getAsString()
                    .equals(key.npc())
                    || !json.get("player").getAsString()
                    .equals(key.player().toString())) {

                throw new IOException(
                        "Relationship identity/version mismatch: " + file
                );
            }

            Snapshot result = new Snapshot(
                    json.get("affection").getAsDouble(),
                    json.get("trust").getAsDouble(),
                    json.get("respect").getAsDouble(),
                    json.get("fear").getAsDouble(),
                    json.get("familiarity").getAsDouble(),
                    json.get("conversations").getAsLong(),
                    json.get("syncedExchanges").getAsLong(),
                    json.get("lastActionTick").getAsLong(),
                    json.get("lastAction").getAsString()
            );

            if (!valid(result)) {
                throw new IOException(
                        "Invalid relationship values: " + file
                );
            }

            return result;

        } catch (RuntimeException error) {

            throw new IOException(
                    "Corrupt relationship file; refusing overwrite: "
                            + file,
                    error
            );
        }
    }

    private void persist(
            Key key,
            Snapshot snapshot
    ) throws IOException {

        if (!valid(snapshot)) {
            throw new IllegalArgumentException(
                    "Invalid relationship values"
            );
        }

        Path destination = file(key);

        Files.createDirectories(destination.getParent());

        JsonObject json = new JsonObject();

        json.addProperty("version", VERSION);
        json.addProperty("npc", key.npc());
        json.addProperty("player", key.player().toString());
        json.addProperty("affection", snapshot.affection());
        json.addProperty("trust", snapshot.trust());
        json.addProperty("respect", snapshot.respect());
        json.addProperty("fear", snapshot.fear());
        json.addProperty("familiarity", snapshot.familiarity());
        json.addProperty("conversations", snapshot.conversations());
        json.addProperty(
                "syncedExchanges", snapshot.syncedExchanges()
        );
        json.addProperty(
                "lastActionTick", snapshot.lastActionTick()
        );
        json.addProperty("lastAction", snapshot.lastAction());

        Path temp = Files.createTempFile(
                destination.getParent(),
                "relationship-",
                ".tmp"
        );

        try {

            Files.writeString(
                    temp,
                    json.toString(),
                    StandardCharsets.UTF_8
            );

            try {

                Files.move(
                        temp,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (AtomicMoveNotSupportedException ignored) {

                Files.move(
                        temp,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            cache.put(key, snapshot);

        } finally {

            Files.deleteIfExists(temp);
        }
    }

    private Path file(Key key) throws IOException {

        if (root == null) {
            throw new IOException(
                    "Player relationship store is not bound"
            );
        }

        try {

            String hash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(
                                    key.npc().getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            )
            );

            return root.resolve(hash.substring(0, 2))
                    .resolve(hash)
                    .resolve(
                            key.player().toString() + ".json"
                    );

        } catch (NoSuchAlgorithmException error) {

            throw new IllegalStateException(
                    "SHA-256 unavailable",
                    error
            );
        }
    }

    private static Key key(
            String npc,
            UUID player
    ) {

        if (npc == null || npc.isBlank()
                || npc.length() > 160) {

            throw new IllegalArgumentException(
                    "Invalid NPC identity"
            );
        }

        return new Key(
                npc,
                Objects.requireNonNull(player, "player")
        );
    }

    private static Snapshot neutral(long baseline) {

        return new Snapshot(
                0, 0, 0, 0, 0,
                0,
                baseline,
                -1,
                ""
        );
    }

    private static boolean valid(Snapshot state) {

        return signed(state.affection())
                && signed(state.trust())
                && signed(state.respect())
                && unit(state.fear())
                && unit(state.familiarity())
                && state.conversations() >= 0
                && state.syncedExchanges() >= 0
                && state.lastActionTick() >= -1
                && state.lastAction() != null;
    }

    private static boolean signed(double value) {

        return Double.isFinite(value)
                && value >= -1
                && value <= 1;
    }

    private static boolean unit(double value) {

        return Double.isFinite(value)
                && value >= 0
                && value <= 1;
    }

    private static double clampSigned(double value) {
        return Math.max(-1, Math.min(1, value));
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static String format(double value) {
        return String.format(
                java.util.Locale.ROOT,
                "%.3f",
                value
        );
    }
}


package dev.dtzudontsay.knownworld.simulation.casestudy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioStartCatalog;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-only Winterfell V3 conversations using local Ollama.
 *
 * Batch A:
 * - authoritative scenario and character context
 * - actual genealogy, marriage, titles and affiliations
 * - excludes unclassified private knowledge from prompts
 * - guards some major historical contradictions
 *
 * Does not alter NPC simulation state.
 * Does not add persistent conversation memories yet.
 *
 * Physical conversation locks remain owned by
 * WinterfellConversationPackets.
 */
public final class WinterfellConversationService {

    private static final String ACTOR_TAG = "kwf3_actor";
    private static final String ID_PREFIX = "kwf3_id_";

    private static final String DEFAULT_MODEL = "qwen3.5:9b";

    private static final String DEFAULT_ENDPOINT =
            "http://127.0.0.1:11434/api/chat";

    private static final double MAX_DISTANCE_SQUARED = 36.0;

    private static final int MAX_MESSAGE = 400;
    private static final int MAX_REPLY = 1700;
    private static final int MAX_HISTORY = 12;
    private static final int MAX_PARALLEL = 1;

    private static final long REQUEST_GAP_NS =
            1_200_000_000L;

    private static final long SESSION_IDLE_NS =
            30L * 60L * 1_000_000_000L;

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofMinutes(3);

    private static final HttpClient HTTP =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();

    private static final AtomicInteger IN_FLIGHT =
            new AtomicInteger();

    /**
     * Access only on Minecraft server thread.
     */
    private static final Map<UUID, Session> SESSIONS =
            new HashMap<>();

    private record Turn(String role, String content) { }

    private static final class Session {

        final String token = UUID.randomUUID().toString();

        final UUID playerUuid;
        final UUID actorUuid;
        final NpcId npcId;
        final String authoredId;

        final Deque<Turn> history = new ArrayDeque<>();

        long touched = System.nanoTime();
        long lastRequest;

        boolean busy;

        Session(
                UUID playerUuid,
                UUID actorUuid,
                NpcId npcId,
                String authoredId
        ) {

            this.playerUuid = playerUuid;
            this.actorUuid = actorUuid;
            this.npcId = npcId;
            this.authoredId = authoredId;
        }
    }

    private WinterfellConversationService() { }

    public static void register() {

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) ->
                        SESSIONS.remove(handler.player.getUUID())
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> SESSIONS.clear()
        );

        KnownWorld.LOGGER.info(
                "Winterfell historical-context dialogue ready. "
                        + "Ollama model={}, endpoint={}",
                modelName(),
                endpoint()
        );
    }

    public static void open(
            ServerPlayer player,
            int entityId
    ) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Entity actor = level.getEntity(entityId);

        if (!validActor(player, actor)) {

            player.sendSystemMessage(Component.literal(
                    "[Winterfell] Look at a nearby Winterfell NPC and press K."
            ));

            return;
        }

        String authoredId = authoredId(actor);

        NpcSimulation sim = NpcSimulation.getNullable();

        NpcId npcId = sim == null
                ? null
                : sim.authoredIds()
                .findNpc(authoredId)
                .orElse(null);

        NpcState npc = npcId == null
                ? null
                : sim.registry()
                .find(npcId)
                .orElse(null);

        if (npc == null || !npc.isAlive()) {

            player.sendSystemMessage(Component.literal(
                    "[Winterfell] This character is unavailable."
            ));

            return;
        }

        pruneSessions();

        Session session = new Session(
                player.getUUID(),
                actor.getUUID(),
                npcId,
                authoredId
        );

        SESSIONS.put(player.getUUID(), session);

        CharacterProfile profile =
                sim.profiles().getOrCreate(npcId);

        String name = limit(
                npc.identity().fullName(),
                128
        );

        String subtitle =
                WinterfellDialogueContext.nice(
                        WinterfellV3Runtime.dialogueRole(authoredId)
                )
                        + " | "
                        + WinterfellDialogueContext.referenceName(
                        profile.culture(),
                        true
                )
                        + " | "
                        + WinterfellDialogueContext.referenceName(
                        profile.religion(),
                        false
                );

        ServerPlayNetworking.send(
                player,
                new WinterfellConversationPackets.Opened(
                        session.token,
                        name,
                        limit(subtitle, 256),
                        actor.getId()
                )
        );

        systemLine(
                player,
                session,
                "Local AI: " + modelName()
                        + ". Historical and family context connected."
        );

        status(
                player,
                session,
                "Local AI ready - authoritative context enabled",
                false
        );
    }

    public static void send(
            ServerPlayer player,
            String token,
            String rawText
    ) {

        Session session = SESSIONS.get(player.getUUID());

        if (session == null
                || !session.token.equals(token)) {
            return;
        }

        long now = System.nanoTime();

        if (now - session.touched > SESSION_IDLE_NS) {

            SESSIONS.remove(player.getUUID());

            systemLine(
                    player,
                    session,
                    "Conversation expired. Press K to speak again."
            );

            status(player, session, "Conversation expired", false);
            return;
        }

        if (session.busy) {

            status(
                    player,
                    session,
                    "Still generating the previous reply...",
                    true
            );

            return;
        }

        if (session.lastRequest != 0L
                && now - session.lastRequest < REQUEST_GAP_NS) {

            status(
                    player,
                    session,
                    "A moment, please...",
                    false
            );

            return;
        }

        String words = limit(
                rawText == null ? "" : rawText.trim(),
                MAX_MESSAGE
        );

        if (words.isBlank()) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Entity actor = level.getEntity(session.actorUuid);

        if (!validActor(player, actor)
                || !session.authoredId.equals(authoredId(actor))) {

            SESSIONS.remove(player.getUUID());

            systemLine(
                    player,
                    session,
                    "This NPC is no longer in speaking distance."
            );

            status(player, session, "Conversation ended", false);
            return;
        }

        NpcSimulation sim = NpcSimulation.getNullable();

        NpcState npc = sim == null
                ? null
                : sim.registry()
                .find(session.npcId)
                .orElse(null);

        if (npc == null || !npc.isAlive()) {

            systemLine(
                    player,
                    session,
                    "This character is unavailable."
            );

            status(player, session, "Character unavailable", false);
            return;
        }

        CharacterProfile profile =
                sim.profiles().getOrCreate(session.npcId);

        session.touched = now;
        session.lastRequest = now;

        addTurn(session, "user", words);

        line(player, session, "player", words);

        if (IN_FLIGHT.incrementAndGet() > MAX_PARALLEL) {

            IN_FLIGHT.decrementAndGet();

            fallback(
                    player,
                    session,
                    sim,
                    npc,
                    words,
                    "The local AI is serving another conversation."
            );

            return;
        }

        final JsonObject body;

        try {

            body = buildRequest(
                    sim,
                    npc,
                    profile,
                    session
            );

        } catch (RuntimeException error) {

            IN_FLIGHT.decrementAndGet();

            KnownWorld.LOGGER.error(
                    "Failed to construct NPC historical context",
                    error
            );

            fallback(
                    player,
                    session,
                    sim,
                    npc,
                    words,
                    "Historical dialogue context could not be assembled."
            );

            return;
        }

        session.busy = true;

        status(
                player,
                session,
                "Local AI is generating a reply...",
                true
        );

        MinecraftServer server = level.getServer();

        try {

            HttpRequest request =
                    HttpRequest.newBuilder(
                                    URI.create(endpoint())
                            )
                            .timeout(REQUEST_TIMEOUT)
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            body.toString(),
                                            StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            HTTP.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            ).whenComplete((response, error) -> {

                IN_FLIGHT.decrementAndGet();

                String reply = "";
                String failure = null;

                if (error != null) {

                    failure =
                            "Ollama is unreachable or timed out.";

                    KnownWorld.LOGGER.warn(
                            "Winterfell Ollama request failed: {}",
                            unwrap(error).toString()
                    );

                } else if (response.statusCode() / 100 != 2) {

                    failure = providerError(response.statusCode());

                    KnownWorld.LOGGER.warn(
                            "Winterfell Ollama returned HTTP {}",
                            response.statusCode()
                    );

                } else {

                    try {

                        reply = parseReply(response.body());

                        if (reply.isBlank()) {

                            failure =
                                    "Ollama returned an empty reply.";
                        }

                    } catch (RuntimeException parseError) {

                        failure =
                                "Ollama returned an unreadable reply.";

                        KnownWorld.LOGGER.warn(
                                "Could not parse Ollama dialogue",
                                parseError
                        );
                    }
                }

                final String answer = reply;
                final String problem = failure;

                server.execute(() ->
                        complete(
                                server,
                                session,
                                words,
                                answer,
                                problem
                        )
                );
            });

        } catch (RuntimeException error) {

            IN_FLIGHT.decrementAndGet();

            session.busy = false;

            KnownWorld.LOGGER.warn(
                    "Could not submit Winterfell dialogue request",
                    error
            );

            fallback(
                    player,
                    session,
                    sim,
                    npc,
                    words,
                    "Local AI request could not be submitted."
            );
        }
    }

    private static JsonObject buildRequest(
            NpcSimulation sim,
            NpcState npc,
            CharacterProfile profile,
            Session session
    ) {

        JsonObject root = new JsonObject();

        root.addProperty("model", modelName());
        root.addProperty("stream", false);
        root.addProperty("think", false);
        root.addProperty("keep_alive", "5m");

        JsonObject options = new JsonObject();

        options.addProperty("num_ctx", contextLength());
        options.addProperty("num_predict", 384);
        options.addProperty("temperature", 0.55);
        options.addProperty("top_p", 0.90);

        root.add("options", options);

        JsonArray messages = new JsonArray();

        addMessage(
                messages,
                "system",
                WinterfellDialogueContext.build(
                        sim,
                        npc,
                        profile,
                        session.authoredId
                )
        );

        for (Turn turn : session.history) {

            addMessage(
                    messages,
                    turn.role(),
                    turn.content()
            );
        }

        root.add("messages", messages);

        return root;
    }

    private static void addMessage(
            JsonArray messages,
            String role,
            String content
    ) {

        JsonObject item = new JsonObject();

        item.addProperty("role", role);
        item.addProperty("content", content);

        messages.add(item);
    }

    /**
     * Runs on the authoritative Minecraft server thread.
     */
    private static void complete(
            MinecraftServer server,
            Session session,
            String playerWords,
            String generated,
            String failure
    ) {

        if (SESSIONS.get(session.playerUuid) != session) {
            return;
        }

        ServerPlayer player =
                server.getPlayerList().getPlayer(session.playerUuid);

        if (player == null) {
            return;
        }

        session.busy = false;
        session.touched = System.nanoTime();

        NpcSimulation sim = NpcSimulation.getNullable();

        NpcState npc = sim == null
                ? null
                : sim.registry()
                .find(session.npcId)
                .orElse(null);

        if (npc == null || !npc.isAlive()) {

            systemLine(
                    player,
                    session,
                    "The character is no longer available."
            );

            status(player, session, "Character unavailable", false);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Entity actor = level.getEntity(session.actorUuid);

        if (!validActor(player, actor)
                || !session.authoredId.equals(authoredId(actor))) {

            systemLine(
                    player,
                    session,
                    "The conversation ended before the answer arrived."
            );

            status(player, session, "Conversation ended", false);
            return;
        }

        if (failure != null || generated.isBlank()) {

            fallback(
                    player,
                    session,
                    sim,
                    npc,
                    playerWords,
                    failure == null
                            ? "No response was generated."
                            : failure
            );

            return;
        }

        String currentMonarch =
                WinterfellDialogueContext.currentMonarch(sim);

        if (currentMonarch == null
                && ScenarioStartCatalog.get()
                .daysSinceStart(sim.campaignCalendar()) == 0) {

            currentMonarch = "Robert Baratheon";
        }

        WinterfellDialogueGuard.Decision decision =
                WinterfellDialogueGuard.review(
                        session.authoredId,
                        generated,
                        currentMonarch
                );

        String spoken = limit(
                decision.reply(),
                MAX_REPLY
        );

        if (!decision.accepted()) {

            KnownWorld.LOGGER.warn(
                    "Rejected NPC dialogue for '{}': {}",
                    session.authoredId,
                    decision.reason()
            );

            systemLine(
                    player,
                    session,
                    "A conflicting or protected AI statement was filtered."
            );
        }

        /*
         * Only validated/corrected speech is retained.
         * The hallucinated original never enters session history.
         */
        addTurn(
                session,
                "assistant",
                spoken
        );

        line(
                player,
                session,
                "npc",
                spoken
        );

        status(
                player,
                session,
                decision.accepted()
                        ? "Local AI reply received"
                        : "AI reply corrected for consistency",
                false
        );
    }

    /**
     * Basic local fallback, not a second LLM.
     */
    private static void fallback(
            ServerPlayer player,
            Session session,
            NpcSimulation sim,
            NpcState npc,
            String words,
            String reason
    ) {

        session.busy = false;

        systemLine(
                player,
                session,
                reason + " Using basic dialogue."
        );

        String text = words.toLowerCase(Locale.ROOT);

        String answer;

        String monarch =
                WinterfellDialogueContext.currentMonarch(sim);

        if (monarch == null
                && ScenarioStartCatalog.get()
                .daysSinceStart(sim.campaignCalendar()) == 0) {

            monarch = "Robert Baratheon";
        }

        if (text.contains("king")
                || text.contains("iron throne")) {

            answer = monarch == null
                    ? "I cannot say for certain who presently "
                    + "holds the Iron Throne."
                    : monarch + " holds the royal title, "
                    + "so far as the present order stands.";

        } else if (text.contains("who are you")
                || text.contains("your name")) {

            answer = "I am " + npc.identity().fullName() + ".";

        } else if (text.contains("family")
                || text.contains("sister")
                || text.contains("mother")
                || text.contains("father")) {

            answer = "Family is a matter I would speak "
                    + "of with care, and not to every stranger.";

        } else if (text.contains("hello")
                || text.contains("greetings")
                || text.equals("hi")) {

            answer = npc.personality().sociability() > 0.2
                    ? "Well met. What brings you here?"
                    : "Aye? What would you have of me?";

        } else {

            answer = npc.personality().compassion() > 0.3
                    ? "Perhaps you could tell me more."
                    : "I cannot answer that with certainty.";
        }

        addTurn(session, "assistant", answer);

        line(player, session, "npc", answer);

        status(
                player,
                session,
                "Basic dialogue - local AI unavailable",
                false
        );
    }

    private static String parseReply(String json) {

        JsonObject root =
                JsonParser.parseString(json).getAsJsonObject();

        JsonObject message =
                root.getAsJsonObject("message");

        if (message == null) {
            return "";
        }

        JsonElement content = message.get("content");

        if (content == null || content.isJsonNull()) {
            return "";
        }

        String result = content.getAsString().trim();

        /*
         * Never render reasoning tags as character speech.
         */
        if (result.startsWith("<think>")) {

            int end = result.indexOf("</think>");

            if (end < 0) {
                return "";
            }

            result = result.substring(
                    end + "</think>".length()
            ).trim();
        }

        return limit(result, MAX_REPLY);
    }

    private static String providerError(int code) {

        if (code == 404) {
            return "Ollama model missing. Run: ollama pull "
                    + modelName();
        }

        if (code == 400) {
            return "Ollama rejected the request (HTTP 400). "
                    + "Check its version and model.";
        }

        return "Ollama request failed (HTTP " + code + ").";
    }

    private static void addTurn(
            Session session,
            String role,
            String content
    ) {

        session.history.addLast(
                new Turn(role, content)
        );

        while (session.history.size() > MAX_HISTORY) {
            session.history.removeFirst();
        }
    }

    private static void line(
            ServerPlayer player,
            Session session,
            String speaker,
            String text
    ) {

        ServerPlayNetworking.send(
                player,
                new WinterfellConversationPackets.Line(
                        session.token,
                        speaker,
                        limit(text, MAX_REPLY)
                )
        );
    }

    private static void systemLine(
            ServerPlayer player,
            Session session,
            String message
    ) {

        line(player, session, "system", message);
    }

    private static void status(
            ServerPlayer player,
            Session session,
            String text,
            boolean busy
    ) {

        ServerPlayNetworking.send(
                player,
                new WinterfellConversationPackets.Status(
                        session.token,
                        limit(text, 256),
                        busy
                )
        );
    }

    private static boolean validActor(
            ServerPlayer player,
            Entity actor
    ) {

        return actor != null
                && actor.isAlive()
                && actor.entityTags().contains(ACTOR_TAG)
                && player.distanceToSqr(actor)
                <= MAX_DISTANCE_SQUARED
                && authoredId(actor) != null;
    }

    private static String authoredId(Entity actor) {

        if (actor == null) {
            return null;
        }

        for (String tag : actor.entityTags()) {

            if (tag.startsWith(ID_PREFIX)) {
                return tag.substring(ID_PREFIX.length());
            }
        }

        return null;
    }

    private static void pruneSessions() {

        long now = System.nanoTime();

        SESSIONS.values().removeIf(
                session -> !session.busy
                        && now - session.touched > SESSION_IDLE_NS
        );
    }

    private static String modelName() {

        String value =
                System.getenv("KNOWNWORLD_OLLAMA_MODEL");

        return value == null || value.isBlank()
                ? DEFAULT_MODEL
                : value.trim();
    }

    private static String endpoint() {

        String value =
                System.getenv("KNOWNWORLD_OLLAMA_URL");

        return value == null || value.isBlank()
                ? DEFAULT_ENDPOINT
                : value.trim();
    }

    private static int contextLength() {

        String value =
                System.getenv("KNOWNWORLD_OLLAMA_CONTEXT");

        if (value == null || value.isBlank()) {
            return 8192;
        }

        try {

            int requested = Integer.parseInt(value.trim());

            return Math.max(
                    4096,
                    Math.min(16384, requested)
            );

        } catch (NumberFormatException ignored) {

            return 8192;
        }
    }

    private static String limit(String text, int max) {

        if (text == null) {
            return "";
        }

        return text.length() <= max
                ? text
                : text.substring(0, max);
    }

    private static Throwable unwrap(Throwable error) {

        return error instanceof CompletionException
                && error.getCause() != null
                ? error.getCause()
                : error;
    }
}

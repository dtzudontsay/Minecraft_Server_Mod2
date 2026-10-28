
package dev.dtzudontsay.knownworld.simulation.casestudy;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB41DialogueService;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Dialogue payloads and authoritative temporary villager movement locks. */
public final class WinterfellConversationPackets {

    private static final String ACTOR_TAG = "kwf3_actor";
    private static final String ID_PREFIX = "kwf3_id_";
    private static final double MAX_RANGE_SQUARED = 36.0;
    private static final long MAX_LOCK_AGE_NANOS = 30L * 60L * 1_000_000_000L;

    private record ConversationLock(UUID actor, int entityId, long createdNanos) { }

    /** Server thread only. */
    private static final Map<UUID, ConversationLock> PLAYER_LOCKS = new HashMap<>();
    private static final Map<UUID, Boolean> ORIGINAL_NO_AI = new HashMap<>();

    private WinterfellConversationPackets() { }

    public record OpenRequest(int entityId) implements CustomPacketPayload {
        public static final Type<OpenRequest> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT,
                        OpenRequest::entityId, OpenRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChatRequest(String session, String text) implements CustomPacketPayload {
        public static final Type<ChatRequest> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_send_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChatRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(64), ChatRequest::session,
                        ByteBufCodecs.stringUtf8(600), ChatRequest::text,
                        ChatRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Opened(String session, String name, String subtitle,
                         int entityId) implements CustomPacketPayload {
        public static final Type<Opened> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_opened_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Opened> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Opened::session,
                        ByteBufCodecs.stringUtf8(128), Opened::name,
                        ByteBufCodecs.stringUtf8(256), Opened::subtitle,
                        ByteBufCodecs.VAR_INT, Opened::entityId, Opened::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Speaker is player, npc or system. */
    public record Line(String session, String speaker, String text)
            implements CustomPacketPayload {
        public static final Type<Line> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_line_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Line> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Line::session,
                        ByteBufCodecs.stringUtf8(16), Line::speaker,
                        ByteBufCodecs.stringUtf8(2048), Line::text, Line::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Status(String session, String text, boolean busy)
            implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_status_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(64), Status::session,
                        ByteBufCodecs.stringUtf8(256), Status::text,
                        ByteBufCodecs.BOOL, Status::busy, Status::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CloseRequest(int entityId) implements CustomPacketPayload {
        public static final Type<CloseRequest> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_close_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CloseRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT,
                        CloseRequest::entityId, CloseRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerCommon() {
        // B4_1B_WORLDWIDE_DIALOGUE_ROUTING
        KnownWorldB41DialogueService.registerLifecycle();
        PayloadTypeRegistry.serverboundPlay().register(OpenRequest.TYPE, OpenRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChatRequest.TYPE, ChatRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CloseRequest.TYPE, CloseRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Opened.TYPE, Opened.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Line.TYPE, Line.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Status.TYPE, Status.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OpenRequest.TYPE, (request, context) -> {
            ServerPlayer player = context.player();
            KnownWorldB41DialogueService.invalidatePlayer(player);
            if (KnownWorldB41DialogueService.isWorldwideActor(player, request.entityId())) {
                // Worldwide B4.0 identities: separate authoritative conversation route.
                if (KnownWorldB41DialogueService.open(player, request.entityId())) {
                    lockActor(player, request.entityId());
                }
            } else {
                WinterfellConversationService.open(player, request.entityId());
                lockActor(player, request.entityId());
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ChatRequest.TYPE, (request, context) -> {
            if (KnownWorldB41DialogueService.hasSession(context.player(), request.session())) {
                KnownWorldB41DialogueService.send(context.player(), request.session(), request.text());
            } else {
                WinterfellConversationService.send(context.player(), request.session(), request.text());
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(CloseRequest.TYPE, (request, context) -> {
            ServerPlayer player = context.player();
            // IMPORTANT: invalidate outstanding Ollama replies before unlocking.
            KnownWorldB41DialogueService.close(player, request.entityId());
            WinterfellConversationService.close(player, request.entityId());
            ConversationLock lock = PLAYER_LOCKS.get(player.getUUID());
            if (lock != null && lock.entityId() == request.entityId()) {
                release(player.getUUID(), context.server());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                release(handler.player.getUUID(), server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PLAYER_LOCKS.clear();
            ORIGINAL_NO_AI.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(WinterfellConversationPackets::tickLocks);
    }

    public static boolean isConversationActor(UUID actorId) {
        return ORIGINAL_NO_AI.containsKey(actorId);
    }

    private static void lockActor(ServerPlayer player, int entityId) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Entity actor = level.getEntity(entityId);
        if (!(actor instanceof Villager villager)
                || !actor.isAlive()
                || player.distanceToSqr(actor) > MAX_RANGE_SQUARED) return;
        if (actor.entityTags().contains("kwb41_actor")) {
            // Never freeze a forged or stale projection. Its identity must be live.
            if (!KnownWorldB41DialogueService.validActor(player, actor)) return;
        } else {
            if (!actor.entityTags().contains(ACTOR_TAG)) return;
            String authored = null;
            for (String tag : actor.entityTags()) {
                if (tag.startsWith(ID_PREFIX)) {
                    authored = tag.substring(ID_PREFIX.length());
                    break;
                }
            }
            if (authored == null) return;
            NpcSimulation sim = NpcSimulation.getNullable();
            NpcId npcId = sim == null ? null :
                    sim.authoredIds().findNpc(authored).orElse(null);
            if (npcId == null || sim.registry().find(npcId)
                    .filter(npc -> npc.isAlive()).isEmpty()) return;
        }
        UUID playerId = player.getUUID();
        if (PLAYER_LOCKS.containsKey(playerId)) release(playerId, level.getServer());
        UUID actorId = actor.getUUID();
        ORIGINAL_NO_AI.putIfAbsent(actorId, villager.isNoAi());
        PLAYER_LOCKS.put(playerId,
                new ConversationLock(actorId, entityId, System.nanoTime()));
        freeze(villager);
    }

    private static void freeze(Villager villager) {
        villager.getNavigation().stop();
        villager.setNoAi(true);
    }

    private static void tickLocks(MinecraftServer server) {
        if (PLAYER_LOCKS.isEmpty()) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            PLAYER_LOCKS.clear();
            ORIGINAL_NO_AI.clear();
            return;
        }
        long now = System.nanoTime();
        Iterator<Map.Entry<UUID, ConversationLock>> iterator =
                PLAYER_LOCKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ConversationLock> entry = iterator.next();
            ConversationLock lock = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Entity actor = overworld.getEntity(lock.actor());
            boolean valid = player != null
                    && player.level() == overworld
                    && actor instanceof Villager
                    && actor.isAlive()
                    && player.distanceToSqr(actor) <= MAX_RANGE_SQUARED
                    && now - lock.createdNanos() < MAX_LOCK_AGE_NANOS;
            if (!valid) {
                iterator.remove();
                continue;
            }
            freeze((Villager) actor);
        }
        Iterator<Map.Entry<UUID, Boolean>> originals =
                ORIGINAL_NO_AI.entrySet().iterator();
        while (originals.hasNext()) {
            Map.Entry<UUID, Boolean> original = originals.next();
            boolean used = false;
            for (ConversationLock lock : PLAYER_LOCKS.values()) {
                if (lock.actor().equals(original.getKey())) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                Entity actor = overworld.getEntity(original.getKey());
                if (actor instanceof Villager villager) {
                    villager.setNoAi(original.getValue());
                }
                originals.remove();
            }
        }
    }

    private static void release(UUID playerId, MinecraftServer server) {
        ConversationLock removed = PLAYER_LOCKS.remove(playerId);
        if (removed == null) return;
        for (ConversationLock lock : PLAYER_LOCKS.values()) {
            if (lock.actor().equals(removed.actor())) return;
        }
        Boolean original = ORIGINAL_NO_AI.remove(removed.actor());
        if (original == null) return;
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;
        Entity actor = level.getEntity(removed.actor());
        if (actor instanceof Villager villager) villager.setNoAi(original);
    }
}

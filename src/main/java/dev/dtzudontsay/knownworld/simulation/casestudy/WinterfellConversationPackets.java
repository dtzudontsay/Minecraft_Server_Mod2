
package dev.dtzudontsay.knownworld.simulation.casestudy;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
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

/**
 * Networking for Winterfell conversations.
 *
 * Also maintains temporary server-authoritative movement locks for
 * physical villagers currently involved in a player conversation.
 *
 * The separate NPC simulation and the original right-click
 * inspection interface remain authoritative and unchanged.
 */
public final class WinterfellConversationPackets {

    private static final String ACTOR_TAG = "kwf3_actor";
    private static final String ID_PREFIX = "kwf3_id_";
    private static final double MAX_RANGE_SQUARED = 36.0;
    private static final long MAX_LOCK_AGE_NANOS =
            30L * 60L * 1_000_000_000L;

    private record ConversationLock(
            UUID actor,
            int entityId,
            long createdNanos
    ) { }

    /** All maps are accessed only from the Minecraft server thread. */
    private static final Map<UUID, ConversationLock> PLAYER_LOCKS =
            new HashMap<>();

    private static final Map<UUID, Boolean> ORIGINAL_NO_AI =
            new HashMap<>();

    private WinterfellConversationPackets() { }

    public record OpenRequest(int entityId)
            implements CustomPacketPayload {

        public static final Type<OpenRequest> TYPE =
                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                "knownworld", "npc_chat_open_v1"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf, OpenRequest> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        OpenRequest::entityId,
                        OpenRequest::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChatRequest(String session, String text)
            implements CustomPacketPayload {

        public static final Type<ChatRequest> TYPE =
                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                "knownworld", "npc_chat_send_v1"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf, ChatRequest> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64),
                        ChatRequest::session,
                        ByteBufCodecs.stringUtf8(600),
                        ChatRequest::text,
                        ChatRequest::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Opened(
            String session,
            String name,
            String subtitle,
            int entityId
    ) implements CustomPacketPayload {

        public static final Type<Opened> TYPE =
                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                "knownworld", "npc_chat_opened_v1"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf, Opened> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64),
                        Opened::session,
                        ByteBufCodecs.stringUtf8(128),
                        Opened::name,
                        ByteBufCodecs.stringUtf8(256),
                        Opened::subtitle,
                        ByteBufCodecs.VAR_INT,
                        Opened::entityId,
                        Opened::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Speaker is player, npc, or system. */
    public record Line(
            String session,
            String speaker,
            String text
    ) implements CustomPacketPayload {

        public static final Type<Line> TYPE =
                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                "knownworld", "npc_chat_line_v1"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf, Line> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64),
                        Line::session,
                        ByteBufCodecs.stringUtf8(16),
                        Line::speaker,
                        ByteBufCodecs.stringUtf8(2048),
                        Line::text,
                        Line::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Status(
            String session,
            String text,
            boolean busy
    ) implements CustomPacketPayload {

        public static final Type<Status> TYPE =
                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                "knownworld", "npc_chat_status_v1"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf, Status> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64),
                        Status::session,
                        ByteBufCodecs.stringUtf8(256),
                        Status::text,
                        ByteBufCodecs.BOOL,
                        Status::busy,
                        Status::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Closing a dialogue releases the movement lock.
     * The server matches the request to the calling player's actor.
     */
    public record CloseRequest(int entityId)
            implements CustomPacketPayload {

        public static final Type<CloseRequest> TYPE =
                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                "knownworld", "npc_chat_close_v1"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf, CloseRequest> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        CloseRequest::entityId,
                        CloseRequest::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerCommon() {

        PayloadTypeRegistry.serverboundPlay().register(
                OpenRequest.TYPE, OpenRequest.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                ChatRequest.TYPE, ChatRequest.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                CloseRequest.TYPE, CloseRequest.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                Opened.TYPE, Opened.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                Line.TYPE, Line.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                Status.TYPE, Status.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                OpenRequest.TYPE,
                (packet, context) -> {

                    ServerPlayer player = context.player();

                    WinterfellConversationService.open(
                            player,
                            packet.entityId()
                    );

                    // The same authoritative NPC validations are
                    // repeated before any movement lock is acquired.
                    lockActor(player, packet.entityId());
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ChatRequest.TYPE,
                (packet, context) ->
                        WinterfellConversationService.send(
                                context.player(),
                                packet.session(),
                                packet.text()
                        )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                CloseRequest.TYPE,
                (packet, context) -> {

                    UUID playerId = context.player().getUUID();
                    ConversationLock lock = PLAYER_LOCKS.get(playerId);

                    if (lock != null
                            && lock.entityId() == packet.entityId()) {

                        release(playerId, context.server());
                    }
                }
        );

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) ->
                        release(handler.player.getUUID(), server)
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PLAYER_LOCKS.clear();
            ORIGINAL_NO_AI.clear();
        });

        ServerTickEvents.END_SERVER_TICK.register(
                WinterfellConversationPackets::tickLocks
        );
    }

    /**
     * Called by the NPC projection controller to ensure its regular
     * full/reduced AI updates cannot restart a conversation actor.
     */
    public static boolean isConversationActor(UUID actorId) {
        return ORIGINAL_NO_AI.containsKey(actorId);
    }

    private static void lockActor(
            ServerPlayer player,
            int entityId
    ) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = level.getEntity(entityId);

        if (!(entity instanceof Villager villager)
                || !entity.isAlive()
                || !entity.entityTags().contains(ACTOR_TAG)
                || player.distanceToSqr(entity) > MAX_RANGE_SQUARED) {
            return;
        }

        String authoredId = null;

        for (String tag : entity.entityTags()) {
            if (tag.startsWith(ID_PREFIX)) {
                authoredId = tag.substring(ID_PREFIX.length());
                break;
            }
        }

        if (authoredId == null) {
            return;
        }

        NpcSimulation sim = NpcSimulation.getNullable();

        NpcId npcId = sim == null
                ? null
                : sim.authoredIds()
                .findNpc(authoredId)
                .orElse(null);

        if (npcId == null
                || sim.registry().find(npcId)
                .filter(npc -> npc.isAlive())
                .isEmpty()) {
            return;
        }

        UUID playerId = player.getUUID();
        ConversationLock previous = PLAYER_LOCKS.get(playerId);

        if (previous != null) {
            release(playerId, level.getServer());
        }

        UUID actorId = entity.getUUID();

        if (!ORIGINAL_NO_AI.containsKey(actorId)) {
            ORIGINAL_NO_AI.put(actorId, villager.isNoAi());
        }

        PLAYER_LOCKS.put(
                playerId,
                new ConversationLock(
                        actorId,
                        entityId,
                        System.nanoTime()
                )
        );

        freeze(villager);
    }

    private static void freeze(Villager villager) {

        villager.getNavigation().stop();
        villager.setNoAi(true);
    }

    /**
     * Reassert locks every tick. Also releases them when players
     * disconnect, travel too far, change dimension, or the
     * underlying physical NPC projection disappears.
     */
    private static void tickLocks(MinecraftServer server) {

        if (PLAYER_LOCKS.isEmpty()) {
            return;
        }

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

            Map.Entry<UUID, ConversationLock> entry =
                    iterator.next();

            ConversationLock lock = entry.getValue();

            ServerPlayer player =
                    server.getPlayerList().getPlayer(entry.getKey());

            Entity entity =
                    overworld.getEntity(lock.actor());

            boolean valid =
                    player != null
                            && player.level() == overworld
                            && entity instanceof Villager
                            && entity.isAlive()
                            && player.distanceToSqr(entity)
                            <= MAX_RANGE_SQUARED
                            && now - lock.createdNanos()
                            < MAX_LOCK_AGE_NANOS;

            if (!valid) {
                iterator.remove();
                continue;
            }

            freeze((Villager) entity);
        }

        // Restore normal AI only after the final conversation
        // involving each actor is over.
        Iterator<Map.Entry<UUID, Boolean>> originals =
                ORIGINAL_NO_AI.entrySet().iterator();

        while (originals.hasNext()) {

            Map.Entry<UUID, Boolean> original =
                    originals.next();

            boolean stillUsed = false;

            for (ConversationLock lock : PLAYER_LOCKS.values()) {
                if (lock.actor().equals(original.getKey())) {
                    stillUsed = true;
                    break;
                }
            }

            if (!stillUsed) {

                Entity entity =
                        overworld.getEntity(original.getKey());

                if (entity instanceof Villager villager) {
                    villager.setNoAi(original.getValue());
                }

                originals.remove();
            }
        }
    }

    private static void release(
            UUID playerId,
            MinecraftServer server
    ) {

        ConversationLock removed = PLAYER_LOCKS.remove(playerId);

        if (removed == null) {
            return;
        }

        // Other players may still be talking to the same villager.
        for (ConversationLock lock : PLAYER_LOCKS.values()) {
            if (lock.actor().equals(removed.actor())) {
                return;
            }
        }

        Boolean original = ORIGINAL_NO_AI.remove(removed.actor());

        if (original == null) {
            return;
        }

        ServerLevel level = server.getLevel(Level.OVERWORLD);

        if (level == null) {
            return;
        }

        Entity entity = level.getEntity(removed.actor());

        if (entity instanceof Villager villager) {
            villager.setNoAi(original);
        }
    }
}

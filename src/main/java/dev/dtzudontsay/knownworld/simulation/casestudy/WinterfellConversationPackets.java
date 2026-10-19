package dev.dtzudontsay.knownworld.simulation.casestudy;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Separate networking boundary for opt-in LLM conversations.
 * CharacterSheet packets and the existing right-click interaction are untouched.
 */
public final class WinterfellConversationPackets {
    private WinterfellConversationPackets() { }

    public record OpenRequest(int entityId) implements CustomPacketPayload {
        public static final Type<OpenRequest> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenRequest::entityId, OpenRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChatRequest(String session, String text) implements CustomPacketPayload {
        public static final Type<ChatRequest> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_send_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChatRequest> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64), ChatRequest::session,
                        ByteBufCodecs.stringUtf8(600), ChatRequest::text,
                        ChatRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Opened(String session, String name, String subtitle, int entityId) implements CustomPacketPayload {
        public static final Type<Opened> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_opened_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Opened> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64), Opened::session,
                        ByteBufCodecs.stringUtf8(128), Opened::name,
                        ByteBufCodecs.stringUtf8(256), Opened::subtitle,
                        ByteBufCodecs.VAR_INT, Opened::entityId,
                        Opened::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** speaker is one of player, npc, system; only the server may create lines. */
    public record Line(String session, String speaker, String text) implements CustomPacketPayload {
        public static final Type<Line> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_line_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Line> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64), Line::session,
                        ByteBufCodecs.stringUtf8(16), Line::speaker,
                        ByteBufCodecs.stringUtf8(2048), Line::text,
                        Line::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Status(String session, String text, boolean busy) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "npc_chat_status_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(64), Status::session,
                        ByteBufCodecs.stringUtf8(256), Status::text,
                        ByteBufCodecs.BOOL, Status::busy,
                        Status::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(OpenRequest.TYPE, OpenRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChatRequest.TYPE, ChatRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Opened.TYPE, Opened.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Line.TYPE, Line.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Status.TYPE, Status.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenRequest.TYPE,
                (packet, context) -> WinterfellConversationService.open(context.player(), packet.entityId()));
        ServerPlayNetworking.registerGlobalReceiver(ChatRequest.TYPE,
                (packet, context) -> WinterfellConversationService.send(
                        context.player(), packet.session(), packet.text()));
    }
}

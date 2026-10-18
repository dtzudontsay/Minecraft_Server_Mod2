package dev.dtzudontsay.knownworld.simulation.casestudy;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** One narrow, validated networking boundary for the character sheet and M-map teleport. */
public final class KnownWorldUiPackets {
    private KnownWorldUiPackets() { }
    private static final Map<UUID, Long> lastTeleportNanos = new ConcurrentHashMap<>();

    public record CharacterSheet(String json) implements CustomPacketPayload {
        public static final Type<CharacterSheet> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "character_sheet_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CharacterSheet> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(16_384), CharacterSheet::json,
                        CharacterSheet::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AtlasTeleportRequest(int x, int z) implements CustomPacketPayload {
        public static final Type<AtlasTeleportRequest> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath("knownworld", "atlas_teleport_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AtlasTeleportRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, AtlasTeleportRequest::x,
                        ByteBufCodecs.VAR_INT, AtlasTeleportRequest::z,
                        AtlasTeleportRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.clientboundPlay().register(CharacterSheet.TYPE, CharacterSheet.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AtlasTeleportRequest.TYPE, AtlasTeleportRequest.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AtlasTeleportRequest.TYPE,
                (packet, context) -> teleport(context.player(), packet.x(), packet.z()));
    }

    /** Creative + game-master permission: client-supplied positions are never trusted. */
    private static void teleport(ServerPlayer player, int requestedX, int requestedZ) {
        if (!player.isCreative() || !Commands.LEVEL_GAMEMASTERS.check(
                player.createCommandSourceStack().permissions())) {
            player.sendSystemMessage(Component.literal("[Atlas] Teleport requires Creative mode and gamemaster permissions."));
            return;
        }
        long now = System.nanoTime();
        Long previous = lastTeleportNanos.get(player.getUUID());
        if (previous != null && now - previous < 3_000_000_000L) {
            player.sendSystemMessage(Component.literal("[Atlas] Please wait three seconds between teleports."));
            return;
        }
        lastTeleportNanos.put(player.getUUID(),now);
        ServerLevel level = player.getServer().getLevel(Level.OVERWORLD);
        if (level == null) return;
        if (Math.abs((long) requestedX) > 29_999_980L ||
                Math.abs((long) requestedZ) > 29_999_980L ||
                !level.getWorldBorder().isWithinBounds(new BlockPos(requestedX, 64, requestedZ))) {
            player.sendSystemMessage(Component.literal("[Atlas] Destination is outside the world border."));
            return;
        }
        // Resolve destination on the SERVER. This may synchronously load/generate its chunk.
        // Never teleport into a fluid or an obstructed two-block headspace.
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                int x = requestedX + dx, z = requestedZ + dz;
                if (!level.getWorldBorder().isWithinBounds(new BlockPos(x,64,z))) continue;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (y < level.getMinY() + 2 || y >= level.getMaxY() - 3) continue;
                BlockPos ground = new BlockPos(x, y - 1, z);
                BlockPos feet = ground.above();
                BlockState below = level.getBlockState(ground);
                if (below.getCollisionShape(level, ground).isEmpty()
                        || !below.getFluidState().isEmpty()
                        || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                    continue;
                }
                if (player.teleportTo(level, x + 0.5, y, z + 0.5,
                        Set.<Relative>of(), player.getYRot(), player.getXRot(), false)) {
                    player.sendSystemMessage(Component.literal("[Atlas] Arrived at X=" + x + " Y=" + y + " Z=" + z));
                }
                return;
            }
        }
        player.sendSystemMessage(Component.literal("[Atlas] No safe landing surface near the selected point."));
    }
}

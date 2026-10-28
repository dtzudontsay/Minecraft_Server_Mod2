
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side networking for the character inspection screen
 * and Known World atlas teleportation.
 *
 * Teleportation resolves actual Minecraft terrain server-side.
 * It prefers safe, walkable natural ground.
 *
 * If there is no natural landing surface at the selected location,
 * it constructs a small elevated rescue platform as a final fallback.
 */
public final class KnownWorldUiPackets {

    private static final Map<UUID, Long> lastTeleportNanos =
            new ConcurrentHashMap<>();

    private static final int NATURAL_SEARCH_RADIUS = 12;
    private static final long TELEPORT_GAP_NANOS = 3_000_000_000L;

    private KnownWorldUiPackets() { }

    public record CharacterSheet(String json)
            implements CustomPacketPayload {

        public static final Type<CharacterSheet> TYPE =
                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                "knownworld",
                                "character_sheet_v3"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf, CharacterSheet> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(16_384),
                        CharacterSheet::json,
                        CharacterSheet::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AtlasTeleportRequest(int x, int z)
            implements CustomPacketPayload {

        public static final Type<AtlasTeleportRequest> TYPE =
                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                "knownworld",
                                "atlas_teleport_v1"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf, AtlasTeleportRequest> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        AtlasTeleportRequest::x,
                        ByteBufCodecs.VAR_INT,
                        AtlasTeleportRequest::z,
                        AtlasTeleportRequest::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerCommon() {

        PayloadTypeRegistry.clientboundPlay().register(
                CharacterSheet.TYPE,
                CharacterSheet.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                AtlasTeleportRequest.TYPE,
                AtlasTeleportRequest.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                AtlasTeleportRequest.TYPE,
                (packet, context) ->
                        teleport(
                                context.player(),
                                context.server(),
                                packet.x(),
                                packet.z()
                        )
        );
    }

    private static void teleport(
            ServerPlayer player,
            MinecraftServer server,
            int requestedX,
            int requestedZ
    ) {

        // Preserve existing gamemaster access restrictions.
        if (!player.isCreative()
                || !Commands.LEVEL_GAMEMASTERS.check(
                player.createCommandSourceStack().permissions()
        )) {

            player.sendSystemMessage(Component.literal(
                    "[Atlas] Teleport requires Creative mode and gamemaster permissions."
            ));

            return;
        }

        long now = System.nanoTime();

        Long previous =
                lastTeleportNanos.get(player.getUUID());

        if (previous != null
                && now - previous < TELEPORT_GAP_NANOS) {

            player.sendSystemMessage(Component.literal(
                    "[Atlas] Please wait three seconds between teleports."
            ));

            return;
        }

        ServerLevel level =
                server.getLevel(Level.OVERWORLD);

        if (level == null) {
            return;
        }

        if (Math.abs((long) requestedX) > 29_999_980L
                || Math.abs((long) requestedZ) > 29_999_980L
                || !withinBorder(
                level,
                requestedX,
                requestedZ
        )) {

            player.sendSystemMessage(Component.literal(
                    "[Atlas] Destination is outside the world border."
            ));

            return;
        }

        lastTeleportNanos.put(
                player.getUUID(),
                now
        );

        /*
         * Step 1: Search for a natural walkable surface.
         * Begin at the selected point and expand outward.
         */
        BlockPos landing = findNaturalLanding(
                level,
                requestedX,
                requestedZ
        );

        boolean rescuePlatform = false;

        /*
         * Step 2: If no natural landing surface exists,
         * make a small stone rescue platform just above the
         * highest local surface.
         *
         * This covers open water, lava, thick vegetation,
         * and other obstructed destinations.
         */
        if (landing == null) {

            landing = makeRescuePlatform(
                    level,
                    requestedX,
                    requestedZ
            );

            rescuePlatform = true;
        }

        if (landing == null) {

            player.sendSystemMessage(Component.literal(
                    "[Atlas] The destination could not be prepared safely."
            ));

            return;
        }

        int x = landing.getX();
        int y = landing.getY();
        int z = landing.getZ();

        boolean success = player.teleportTo(
                level,
                x + 0.5,
                y,
                z + 0.5,
                Set.<Relative>of(),
                player.getYRot(),
                player.getXRot(),
                false
        );

        if (success) {

            if (rescuePlatform) {

                player.sendSystemMessage(Component.literal(
                        "[Atlas] Arrived at X="
                                + x
                                + " Y="
                                + y
                                + " Z="
                                + z
                                + ". No safe natural ground was found; a 3x3 stone rescue platform was created."
                ));

            } else {

                player.sendSystemMessage(Component.literal(
                        "[Atlas] Arrived safely at ground level: X="
                                + x
                                + " Y="
                                + y
                                + " Z="
                                + z
                ));
            }

        } else {

            player.sendSystemMessage(Component.literal(
                    "[Atlas] Minecraft rejected the destination teleport."
            ));
        }
    }

    private static boolean withinBorder(
            ServerLevel level,
            int x,
            int z
    ) {

        return level.getWorldBorder().isWithinBounds(
                new BlockPos(x, 64, z)
        );
    }

    /**
     * Walk outward in rings, searching the real generated terrain.
     *
     * MOTION_BLOCKING_NO_LEAVES prevents tree leaves from being
     * selected as the walkable ground when the actual terrain
     * beneath them is available.
     */
    private static BlockPos findNaturalLanding(
            ServerLevel level,
            int requestedX,
            int requestedZ
    ) {

        for (int radius = 0;
             radius <= NATURAL_SEARCH_RADIUS;
             radius++) {

            for (int dx = -radius; dx <= radius; dx++) {

                for (int dz = -radius; dz <= radius; dz++) {

                    if (radius > 0
                            && Math.max(
                            Math.abs(dx),
                            Math.abs(dz)
                    ) != radius) {

                        continue;
                    }

                    int x = requestedX + dx;
                    int z = requestedZ + dz;

                    if (!withinBorder(level, x, z)) {
                        continue;
                    }

                    int y = level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            x,
                            z
                    );

                    BlockPos feet = new BlockPos(x, y, z);

                    if (isSafeLanding(level, feet)) {
                        return feet;
                    }
                }
            }
        }

        return null;
    }

    /**
     * A safe two-block-tall standing area with actual solid support.
     * Reject fluids, lava, water, fire and obstructed headspace.
     */
    private static boolean isSafeLanding(
            ServerLevel level,
            BlockPos feet
    ) {

        int y = feet.getY();

        if (y < level.getMinY() + 2
                || y >= level.getMaxY() - 3) {
            return false;
        }

        BlockPos ground = feet.below();
        BlockPos head = feet.above();

        BlockState belowState =
                level.getBlockState(ground);

        BlockState footState =
                level.getBlockState(feet);

        BlockState headState =
                level.getBlockState(head);

        if (!belowState.getFluidState().isEmpty()) {
            return false;
        }

        if (belowState.getCollisionShape(level, ground).isEmpty()) {
            return false;
        }

        if (!footState.getFluidState().isEmpty()
                || !headState.getFluidState().isEmpty()) {
            return false;
        }

        if (!footState.getCollisionShape(level, feet).isEmpty()
                || !headState.getCollisionShape(level, head).isEmpty()) {
            return false;
        }

        if (belowState.is(Blocks.CACTUS)
                || belowState.is(Blocks.MAGMA_BLOCK)
                || belowState.is(Blocks.CAMPFIRE)
                || belowState.is(Blocks.SOUL_CAMPFIRE)) {
            return false;
        }

        if (footState.is(Blocks.FIRE)
                || footState.is(Blocks.SOUL_FIRE)
                || footState.is(Blocks.POWDER_SNOW)
                || footState.is(Blocks.SWEET_BERRY_BUSH)) {
            return false;
        }

        if (headState.is(Blocks.FIRE)
                || headState.is(Blocks.SOUL_FIRE)) {
            return false;
        }

        return true;
    }

    /**
     * Last-resort landing solution.
     *
     * Constructs an elevated 3x3 stone surface in the air immediately
     * above the highest nearby surface. Existing terrain blocks are
     * not deliberately excavated.
     *
     * This is a permanent, small world modification. It is used
     * only when all nearby natural landing candidates failed.
     */
    private static BlockPos makeRescuePlatform(
            ServerLevel level,
            int x,
            int z
    ) {

        int highest = level.getMinY() + 2;

        for (int dx = -1; dx <= 1; dx++) {

            for (int dz = -1; dz <= 1; dz++) {

                if (!withinBorder(level, x + dx, z + dz)) {
                    continue;
                }

                int surface = level.getHeight(
                        Heightmap.Types.WORLD_SURFACE,
                        x + dx,
                        z + dz
                );

                highest = Math.max(highest, surface);
            }
        }

        int platformY = highest;

        if (platformY < level.getMinY() + 2
                || platformY >= level.getMaxY() - 4) {
            return null;
        }

        /*
         * Verify a clear platform and headspace before any edits.
         * This ensures we do not cut through an existing building.
         */
        for (int dx = -1; dx <= 1; dx++) {

            for (int dz = -1; dz <= 1; dz++) {

                int px = x + dx;
                int pz = z + dz;

                if (!withinBorder(level, px, pz)) {
                    continue;
                }

                BlockPos floor =
                        new BlockPos(px, platformY, pz);

                BlockPos feet = floor.above();
                BlockPos head = feet.above();

                if (!level.getBlockState(floor).isAir()
                        || !level.getBlockState(feet).isAir()
                        || !level.getBlockState(head).isAir()) {
                    return null;
                }
            }
        }

        // Only place platform blocks after full validation.
        for (int dx = -1; dx <= 1; dx++) {

            for (int dz = -1; dz <= 1; dz++) {

                int px = x + dx;
                int pz = z + dz;

                if (!withinBorder(level, px, pz)) {
                    continue;
                }

                BlockPos floor =
                        new BlockPos(px, platformY, pz);

                level.setBlock(
                        floor,
                        Blocks.STONE.defaultBlockState(),
                        3
                );
            }
        }

        return new BlockPos(
                x,
                platformY + 1,
                z
        );
    }
}

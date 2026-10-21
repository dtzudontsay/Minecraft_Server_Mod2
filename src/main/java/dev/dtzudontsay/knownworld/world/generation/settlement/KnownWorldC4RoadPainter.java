package dev.dtzudontsay.knownworld.world.generation.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.IntBinaryOperator;

/**
 * C4.2 bounded terrain-following, chunk-local street painter.
 * C2 continues to own terrain authority, plot surveys and site activation.
 */
public final class KnownWorldC4RoadPainter {
    public record Entrance(int x, int z, int floor) { }

    private KnownWorldC4RoadPainter() { }

    public static void paint(ChunkAccess chunk, int x, int z, String palette,
                             IntBinaryOperator naturalHeight,
                             BiPredicate<Integer, Integer> isLand,
                             List<Entrance> nearbyEntrances) {
        // Fail closed at the coast. C4 shore/dock authorization is separate.
        if (!isLand.test(x, z)) return;
        int natural = naturalHeight.applyAsInt(x, z);
        int[] cross = new int[] {
                natural,
                sample(x - 1, z, natural, naturalHeight, isLand),
                sample(x + 1, z, natural, naturalHeight, isLand),
                sample(x, z - 1, natural, naturalHeight, isLand),
                sample(x, z + 1, natural, naturalHeight, isLand)
        };
        int distance = -1, doorFloor = natural;
        for (Entrance door : nearbyEntrances) {
            int d = Math.abs(door.x() - x) + Math.abs(door.z() - z);
            if (d <= 5 && (distance < 0 || d < distance
                    || (d == distance && door.floor() < doorFloor))) {
                distance = d;
                doorFloor = door.floor();
            }
        }
        int y = KnownWorldC4StreetGrade.height(natural, cross, doorFloor, distance);
        // Severe natural cliffs are not carved into an unbounded trench.
        int lowestNeighbor = cross[0], highestNeighbor = cross[0];
        for (int h : cross) {
            lowestNeighbor = Math.min(lowestNeighbor, h);
            highestNeighbor = Math.max(highestNeighbor, h);
        }
        if (highestNeighbor - lowestNeighbor > 8 || y < chunk.getMinY() + 6
                || y >= chunk.getMaxY() - 13) return;

        String region = palette.toUpperCase(Locale.ROOT);
        BlockState surface = region.contains("DORN")
                ? Blocks.SANDSTONE.defaultBlockState()
                : region.contains("YITI") || region.contains("SHADOW")
                ? Blocks.STONE_BRICKS.defaultBlockState()
                : Blocks.GRAVEL.defaultBlockState();
        BlockState foundation = Blocks.COBBLESTONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        // The 4-block cut/fill limit avoids the kilometre-high pillars that
        // the previous Winterfell prototype exhibited on steep slopes.
        int first = Math.min(y, natural) - 4;
        for (int yy = first; yy <= y; yy++) {
            put(chunk, x, yy, z, yy == y ? surface : foundation);
        }
        // Make genuinely walkable headroom even on gentle uphill cuts.
        for (int yy = y + 1; yy <= Math.max(y, natural) + 8; yy++) {
            put(chunk, x, yy, z, air);
        }
        // Remove remnant lava and logs inside the reserved street column.
        for (int yy = Math.max(chunk.getMinY(), y - 12);
             yy <= Math.min(chunk.getMaxY() - 1, y + 18); yy++) {
            BlockPos position = new BlockPos(x, yy, z);
            BlockState state = chunk.getBlockState(position);
            if (state.getFluidState().is(FluidTags.LAVA))
                put(chunk, x, yy, z, yy <= y ? foundation : air);
            else if (yy >= y - 2 && (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)))
                put(chunk, x, yy, z, air);
        }
    }

    private static int sample(int x, int z, int fallback, IntBinaryOperator heights,
                              BiPredicate<Integer, Integer> isLand) {
        return isLand.test(x, z) ? heights.applyAsInt(x, z) : fallback;
    }

    private static void put(ChunkAccess chunk, int x, int y, int z, BlockState state) {
        if (y < chunk.getMinY() || y >= chunk.getMaxY()) return;
        if ((x >> 4) != chunk.getPos().x() || (z >> 4) != chunk.getPos().z()) return;
        chunk.setBlockState(new BlockPos(x, y, z), state, 0);
    }
}

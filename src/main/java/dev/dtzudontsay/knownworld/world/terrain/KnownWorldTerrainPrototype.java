package dev.dtzudontsay.knownworld.world.terrain;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class KnownWorldTerrainPrototype {

    public static final int SEA_LEVEL =
            63;

    /*
     * Winterfell's current calibrated Minecraft position.
     */
    private static final int TEST_CENTER_X =
            -4_045_150;

    private static final int TEST_CENTER_Z =
            -1_983_733;

    /*
     * Terrain override only applies inside this radius.
     *
     * 2,000 blocks in each direction gives us roughly a
     * 4 km x 4 km test area.
     */
    private static final int TEST_RADIUS_BLOCKS =
            2_000;

    private static final BlockState AIR =
            Blocks.AIR.defaultBlockState();

    private static final BlockState BEDROCK =
            Blocks.BEDROCK.defaultBlockState();

    private static final BlockState STONE =
            Blocks.STONE.defaultBlockState();

    private static final BlockState DIRT =
            Blocks.DIRT.defaultBlockState();

    private static final BlockState GRASS =
            Blocks.GRASS_BLOCK.defaultBlockState();

    private static final BlockState SAND =
            Blocks.SAND.defaultBlockState();

    private static final BlockState GRAVEL =
            Blocks.GRAVEL.defaultBlockState();

    private static final BlockState WATER =
            Blocks.WATER.defaultBlockState();

    private KnownWorldTerrainPrototype() {
    }

    public static void register() {

        ServerChunkEvents.CHUNK_GENERATE.register(
                KnownWorldTerrainPrototype::onChunkGenerated
        );

        KnownWorld.LOGGER.info(
                "Known World prototype terrain override registered around Winterfell only."
        );
    }

    private static void onChunkGenerated(
            ServerLevel level,
            LevelChunk chunk
    ) {

        if (
                !level.dimension().equals(
                        Level.OVERWORLD
                )
        ) {
            return;
        }

        ChunkPos chunkPos =
                chunk.getPos();

        int chunkCenterX =
                chunkPos.getMiddleBlockX();

        int chunkCenterZ =
                chunkPos.getMiddleBlockZ();

        if (
                !insideTestArea(
                        chunkCenterX,
                        chunkCenterZ
                )
        ) {
            return;
        }

        int startX =
                chunkPos.getMinBlockX();

        int startZ =
                chunkPos.getMinBlockZ();

        int minY =
                chunk.getMinY();

        int maxY =
                chunk.getMaxY();

        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();

        for (
                int localZ = 0;
                localZ < 16;
                localZ++
        ) {

            int worldZ =
                    startZ + localZ;

            for (
                    int localX = 0;
                    localX < 16;
                    localX++
            ) {

                int worldX =
                        startX + localX;

                if (
                        !insideTestArea(
                                worldX,
                                worldZ
                        )
                ) {
                    continue;
                }

                generateColumn(
                        chunk,
                        mutablePos,
                        worldX,
                        worldZ,
                        minY,
                        maxY
                );
            }
        }
    }

    private static boolean insideTestArea(
            int worldX,
            int worldZ
    ) {

        return Math.abs(
                worldX
                        - TEST_CENTER_X
        ) <= TEST_RADIUS_BLOCKS
                &&
                Math.abs(
                        worldZ
                                - TEST_CENTER_Z
                ) <= TEST_RADIUS_BLOCKS;
    }

    private static void generateColumn(
            LevelChunk chunk,
            BlockPos.MutableBlockPos mutablePos,
            int worldX,
            int worldZ,
            int minY,
            int maxY
    ) {

        KnownWorldGeoSample geography =
                KnownWorldGeoSampler.sampleMinecraft(
                        worldX + 0.5,
                        worldZ + 0.5
                );

        if (
                !geography.insideKnownWorldMap()
        ) {
            clearColumn(
                    chunk,
                    mutablePos,
                    worldX,
                    worldZ,
                    minY,
                    maxY
            );

            return;
        }

        WorldCoordinate worldCoordinate =
                WorldProjection.fromMinecraft(
                        worldX + 0.5,
                        worldZ + 0.5
                );

        TerrainSample terrain =
                GeographicDataManager
                        .getInstance()
                        .sample(
                                worldCoordinate
                        );

        int terrainY =
                (int) Math.round(
                        terrain.elevationMetres()
                );

        terrainY =
                clamp(
                        terrainY,
                        minY + 5,
                        maxY - 5
                );

        if (
                geography.land()
        ) {

            generateLandColumn(
                    chunk,
                    mutablePos,
                    worldX,
                    worldZ,
                    minY,
                    maxY,
                    terrainY,
                    geography
            );

        } else {

            generateOceanColumn(
                    chunk,
                    mutablePos,
                    worldX,
                    worldZ,
                    minY,
                    maxY,
                    terrainY
            );
        }
    }

    private static void generateLandColumn(
            LevelChunk chunk,
            BlockPos.MutableBlockPos mutablePos,
            int worldX,
            int worldZ,
            int minY,
            int maxY,
            int surfaceY,
            KnownWorldGeoSample geography
    ) {

        boolean coastal =
                Math.abs(
                        geography.coastDistanceMetres()
                ) <= 3_000.0;

        /*
         * Only rewrite the vertical range we actually need.
         *
         * We do not touch the entire world height anymore.
         */
        int clearTop =
                Math.min(
                        maxY,
                        Math.max(
                                surfaceY + 20,
                                SEA_LEVEL + 20
                        )
                );

        int stoneBase =
                Math.max(
                        minY + 1,
                        surfaceY - 24
                );

        for (
                int y = stoneBase;
                y <= clearTop;
                y++
        ) {

            BlockState state;

            if (
                    y < surfaceY - 3
            ) {

                state = STONE;

            } else if (
                    y < surfaceY
            ) {

                state =
                        coastal
                                ? SAND
                                : DIRT;

            } else if (
                    y == surfaceY
            ) {

                state =
                        coastal
                                ? SAND
                                : GRASS;

            } else {

                state = AIR;
            }

            setBlock(
                    chunk,
                    mutablePos,
                    worldX,
                    y,
                    worldZ,
                    state
            );
        }
    }

    private static void generateOceanColumn(
            LevelChunk chunk,
            BlockPos.MutableBlockPos mutablePos,
            int worldX,
            int worldZ,
            int minY,
            int maxY,
            int oceanFloorY
    ) {

        oceanFloorY =
                Math.min(
                        oceanFloorY,
                        SEA_LEVEL - 4
                );

        int clearTop =
                Math.min(
                        maxY,
                        SEA_LEVEL + 20
                );

        int stoneBase =
                Math.max(
                        minY + 1,
                        oceanFloorY - 24
                );

        for (
                int y = stoneBase;
                y <= clearTop;
                y++
        ) {

            BlockState state;

            if (
                    y < oceanFloorY - 3
            ) {

                state = STONE;

            } else if (
                    y <= oceanFloorY
            ) {

                state = GRAVEL;

            } else if (
                    y <= SEA_LEVEL
            ) {

                state = WATER;

            } else {

                state = AIR;
            }

            setBlock(
                    chunk,
                    mutablePos,
                    worldX,
                    y,
                    worldZ,
                    state
            );
        }
    }

    private static void clearColumn(
            LevelChunk chunk,
            BlockPos.MutableBlockPos mutablePos,
            int worldX,
            int worldZ,
            int minY,
            int maxY
    ) {

        int clearTop =
                Math.min(
                        maxY,
                        SEA_LEVEL + 20
                );

        for (
                int y = minY;
                y <= clearTop;
                y++
        ) {

            setBlock(
                    chunk,
                    mutablePos,
                    worldX,
                    y,
                    worldZ,
                    AIR
            );
        }
    }

    private static void setBlock(
            LevelChunk chunk,
            BlockPos.MutableBlockPos mutablePos,
            int x,
            int y,
            int z,
            BlockState state
    ) {

        mutablePos.set(
                x,
                y,
                z
        );

        chunk.setBlockState(
                mutablePos,
                state,
                0
        );
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {

        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }
}
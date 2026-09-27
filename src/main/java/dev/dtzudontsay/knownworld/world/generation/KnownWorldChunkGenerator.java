package dev.dtzudontsay.knownworld.world.generation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.terrain.TerrainSample;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class KnownWorldChunkGenerator extends ChunkGenerator {

    /*
     * Must match:
     *
     * data/knownworld/dimension_type/known_world.json
     *
     * min_y  = -64
     * height = 2096
     *
     * Therefore:
     *
     * lowest block = -64
     * highest block = 2031
     */
    public static final int MIN_Y =
            -64;

    public static final int GENERATION_DEPTH =
            2096;

    public static final int MAX_Y =
            MIN_Y
                    + GENERATION_DEPTH
                    - 1;

    public static final int SEA_LEVEL =
            63;

    public static final MapCodec<KnownWorldChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            BiomeSource.CODEC
                                                    .fieldOf(
                                                            "biome_source"
                                                    )
                                                    .forGetter(
                                                            KnownWorldChunkGenerator::getBiomeSource
                                                    )
                                    )
                                    .apply(
                                            instance,
                                            KnownWorldChunkGenerator::new
                                    )
            );

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

    public KnownWorldChunkGenerator(
            BiomeSource biomeSource
    ) {
        super(
                biomeSource
        );
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(
            ChunkAccess chunk,
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            BiomeManager biomeManager,
            WorldGenRegion carverBiomeRegion,
            Set<Holder<Biome>> possibleBiomes
    ) {

        int startX =
                chunk.getPos()
                        .getMinBlockX();

        int startZ =
                chunk.getPos()
                        .getMinBlockZ();

        BlockPos.MutableBlockPos position =
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

                generateColumn(
                        chunk,
                        position,
                        worldX,
                        worldZ
                );
            }
        }

        return CompletableFuture.completedFuture(
                chunk
        );
    }

    private void generateColumn(
            ChunkAccess chunk,
            BlockPos.MutableBlockPos position,
            int worldX,
            int worldZ
    ) {

        KnownWorldGeoSample geography =
                KnownWorldGeoSampler.sampleMinecraft(
                        worldX + 0.5,
                        worldZ + 0.5
                );

        if (
                !geography.insideKnownWorldMap()
        ) {
            return;
        }

        WorldCoordinate coordinate =
                WorldProjection.fromMinecraft(
                        worldX + 0.5,
                        worldZ + 0.5
                );

        TerrainSample terrain =
                GeographicDataManager
                        .getInstance()
                        .sample(
                                coordinate
                        );

        int terrainY =
                clamp(
                        (int) Math.round(
                                terrain.elevationMetres()
                        ),
                        MIN_Y + 1,
                        MAX_Y - 1
                );

        if (
                geography.land()
        ) {

            generateLandColumn(
                    chunk,
                    position,
                    worldX,
                    worldZ,
                    terrainY,
                    geography
            );

        } else {

            generateOceanColumn(
                    chunk,
                    position,
                    worldX,
                    worldZ,
                    terrainY
            );
        }
    }

    private void generateLandColumn(
            ChunkAccess chunk,
            BlockPos.MutableBlockPos position,
            int worldX,
            int worldZ,
            int surfaceY,
            KnownWorldGeoSample geography
    ) {

        boolean beach =
                geography.coastDistanceMetres()
                        <= 3_000.0;

        setBlock(
                chunk,
                position,
                worldX,
                MIN_Y,
                worldZ,
                BEDROCK
        );

        for (
                int y = MIN_Y + 1;
                y <= surfaceY;
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
                        beach
                                ? SAND
                                : DIRT;

            } else {

                state =
                        beach
                                ? SAND
                                : GRASS;
            }

            setBlock(
                    chunk,
                    position,
                    worldX,
                    y,
                    worldZ,
                    state
            );
        }
    }

    private void generateOceanColumn(
            ChunkAccess chunk,
            BlockPos.MutableBlockPos position,
            int worldX,
            int worldZ,
            int requestedFloorY
    ) {

        int floorY =
                Math.min(
                        requestedFloorY,
                        SEA_LEVEL - 4
                );

        setBlock(
                chunk,
                position,
                worldX,
                MIN_Y,
                worldZ,
                BEDROCK
        );

        for (
                int y = MIN_Y + 1;
                y <= floorY;
                y++
        ) {

            BlockState state;

            if (
                    y < floorY - 3
            ) {

                state = STONE;

            } else {

                state = GRAVEL;
            }

            setBlock(
                    chunk,
                    position,
                    worldX,
                    y,
                    worldZ,
                    state
            );
        }

        for (
                int y = floorY + 1;
                y <= SEA_LEVEL;
                y++
        ) {

            setBlock(
                    chunk,
                    position,
                    worldX,
                    y,
                    worldZ,
                    WATER
            );
        }
    }

    private static void setBlock(
            ChunkAccess chunk,
            BlockPos.MutableBlockPos position,
            int x,
            int y,
            int z,
            BlockState state
    ) {

        position.set(
                x,
                y,
                z
        );

        chunk.setBlockState(
                position,
                state,
                0
        );
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            Heightmap.Types type,
            LevelHeightAccessor heightAccessor,
            RandomState randomState
    ) {

        KnownWorldGeoSample geography =
                KnownWorldGeoSampler.sampleMinecraft(
                        x + 0.5,
                        z + 0.5
                );

        if (
                !geography.insideKnownWorldMap()
        ) {
            return MIN_Y;
        }

        if (
                !geography.land()
        ) {
            return SEA_LEVEL + 1;
        }

        WorldCoordinate coordinate =
                WorldProjection.fromMinecraft(
                        x + 0.5,
                        z + 0.5
                );

        TerrainSample terrain =
                GeographicDataManager
                        .getInstance()
                        .sample(
                                coordinate
                        );

        return clamp(
                (int) Math.round(
                        terrain.elevationMetres()
                ) + 1,
                MIN_Y + 1,
                MAX_Y
        );
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor heightAccessor,
            RandomState randomState
    ) {

        BlockState[] column =
                new BlockState[
                        GENERATION_DEPTH
                        ];

        for (
                int index = 0;
                index < column.length;
                index++
        ) {

            int y =
                    MIN_Y + index;

            column[
                    index
                    ] =
                    getBlockState(
                            x,
                            y,
                            z
                    );
        }

        return new NoiseColumn(
                MIN_Y,
                column
        );
    }

    private BlockState getBlockState(
            int x,
            int y,
            int z
    ) {

        KnownWorldGeoSample geography =
                KnownWorldGeoSampler.sampleMinecraft(
                        x + 0.5,
                        z + 0.5
                );

        if (
                !geography.insideKnownWorldMap()
        ) {
            return AIR;
        }

        WorldCoordinate coordinate =
                WorldProjection.fromMinecraft(
                        x + 0.5,
                        z + 0.5
                );

        TerrainSample terrain =
                GeographicDataManager
                        .getInstance()
                        .sample(
                                coordinate
                        );

        int terrainY =
                clamp(
                        (int) Math.round(
                                terrain.elevationMetres()
                        ),
                        MIN_Y + 1,
                        MAX_Y - 1
                );

        if (
                y == MIN_Y
        ) {
            return BEDROCK;
        }

        if (
                geography.land()
        ) {

            if (
                    y > terrainY
            ) {
                return AIR;
            }

            boolean beach =
                    geography.coastDistanceMetres()
                            <= 3_000.0;

            if (
                    y < terrainY - 3
            ) {
                return STONE;
            }

            if (
                    y < terrainY
            ) {
                return beach
                        ? SAND
                        : DIRT;
            }

            return beach
                    ? SAND
                    : GRASS;
        }

        int floorY =
                Math.min(
                        terrainY,
                        SEA_LEVEL - 4
                );

        if (
                y <= floorY - 4
        ) {
            return STONE;
        }

        if (
                y <= floorY
        ) {
            return GRAVEL;
        }

        if (
                y <= SEA_LEVEL
        ) {
            return WATER;
        }

        return AIR;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return GENERATION_DEPTH;
    }

    @Override
    public void spawnOriginalMobs(
            WorldGenRegion worldGenRegion
    ) {
        /*
         * Disabled during geography testing.
         */
    }

    @Override
    public void applyBiomeDecoration(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structureManager
    ) {
        /*
         * Intentionally disabled for now.
         *
         * No vanilla trees, ores, lakes, flowers, etc. until
         * geographic terrain generation itself is validated.
         */
    }

    @Override
    public void addDebugScreenInfo(
            List<String> result,
            RandomState randomState,
            BlockPos feetPos,
            SamplerContext samplerContext
    ) {

        KnownWorldGeoSample geography =
                KnownWorldGeoSampler.sampleMinecraft(
                        feetPos.getX() + 0.5,
                        feetPos.getZ() + 0.5
                );

        result.add(
                "Known World: "
                        + (
                        geography.insideKnownWorldMap()
                                ? geography.land()
                                ? "LAND"
                                : "OCEAN"
                                : "OUTSIDE MAP"
                )
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
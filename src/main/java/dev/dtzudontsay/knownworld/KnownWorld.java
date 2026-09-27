package dev.dtzudontsay.knownworld;

import dev.dtzudontsay.knownworld.debug.KnownWorldDebugCommand;
import dev.dtzudontsay.knownworld.debug.KnownWorldGeoDebugCommand;
import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerators;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoData;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.geography.regions.NorthEastEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.NorthEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.NorthFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.NorthWestEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SothoryosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SouthEastEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SouthEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SouthFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SouthWestEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SummerIslesFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.UlthosFeatureRegistry;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KnownWorld implements ModInitializer {

    public static final String MOD_ID =
            "knownworld";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(
                    MOD_ID
            );

    @Override
    public void onInitialize() {

        /*
         * Static world-generation types must exist before a
         * Known World world preset tries to decode them.
         */
        KnownWorldChunkGenerators.register();

        NorthFeatureRegistry.bootstrap();
        SouthFeatureRegistry.bootstrap();
        SummerIslesFeatureRegistry.bootstrap();

        SouthWestEssosFeatureRegistry.bootstrap();
        NorthWestEssosFeatureRegistry.bootstrap();
        NorthEssosFeatureRegistry.bootstrap();
        SouthEssosFeatureRegistry.bootstrap();
        NorthEastEssosFeatureRegistry.bootstrap();
        SouthEastEssosFeatureRegistry.bootstrap();

        SothoryosFeatureRegistry.bootstrap();
        UlthosFeatureRegistry.bootstrap();

        KnownWorldGeoData geodata =
                KnownWorldGeoData.getInstance();

        LOGGER.info(
                "Known World geodata ready: {}x{} px | {} m/px X | {} m/px Z",
                geodata.width(),
                geodata.height(),
                String.format(
                        "%.2f",
                        KnownWorldGeoSampler.metresPerRasterPixelX()
                ),
                String.format(
                        "%.2f",
                        KnownWorldGeoSampler.metresPerRasterPixelY()
                )
        );

        KnownWorldDebugCommand.register();
        KnownWorldGeoDebugCommand.register();

        /*
         * Do NOT register KnownWorldTerrainPrototype anymore.
         *
         * The proper ChunkGenerator replaces it.
         */

        LOGGER.info(
                "Known World loaded with {} geographic features.",
                GeographicFeatureRegistry.getFeatureCount()
        );
    }
}
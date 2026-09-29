package dev.dtzudontsay.knownworld;

import dev.dtzudontsay.knownworld.debug.BootstrapDebugCommand;
import dev.dtzudontsay.knownworld.debug.CharacterProfileDebugCommand;
import dev.dtzudontsay.knownworld.debug.FamilyDebugCommand;
import dev.dtzudontsay.knownworld.debug.KnownWorldDebugCommand;
import dev.dtzudontsay.knownworld.debug.KnownWorldGeoDebugCommand;
import dev.dtzudontsay.knownworld.debug.LifeCycleDebugCommand;
import dev.dtzudontsay.knownworld.debug.NpcNeedsDebugCommand;
import dev.dtzudontsay.knownworld.debug.NpcRoutineDebugCommand;
import dev.dtzudontsay.knownworld.debug.SocialDebugCommand;
import dev.dtzudontsay.knownworld.debug.SuccessionDebugCommand;
import dev.dtzudontsay.knownworld.debug.TitleDebugCommand;
import dev.dtzudontsay.knownworld.debug.WorldEventDebugCommand;
import dev.dtzudontsay.knownworld.debug.WorldReferenceDebugCommand;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcDebugCommand;
import dev.dtzudontsay.knownworld.world.biome.KnownWorldBiomeRasterData;
import dev.dtzudontsay.knownworld.world.biome.KnownWorldBiomeSources;
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
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceSettings;
import dev.dtzudontsay.knownworld.world.reference.spatial.LandmassZoneCatalog;
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
         * Register biome source codecs before custom world presets
         * or chunk generators attempt to decode them.
         */
        KnownWorldBiomeSources.register();

        KnownWorldChunkGenerators.register();

        /*
         * Bootstrap all previously-authored geographic features.
         */
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

        /*
         * Load configurable world-reference behavior.
         */
        WorldReferenceSettings.bootstrap();

        /*
         * Build hierarchical Known World reference definitions.
         */
        WorldReferenceCatalog.bootstrap();

        /*
         * Load the broad top-level landmass authoring zones.
         *
         * These polygons may extend through the sea.
         * Actual landmass resolution always additionally checks
         * KnownWorldGeoSampler, so water has no continent.
         */
        LandmassZoneCatalog.bootstrap();

        /*
         * Load runtime terrain geography.
         */
        KnownWorldGeoData geodata =
                KnownWorldGeoData.getInstance();

        /*
         * Force climate/biome raster validation during startup.
         */
        KnownWorldBiomeRasterData.getInstance();

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

        LOGGER.info(
                "Known World reference system ready with {} geographic features, {} hierarchical/reference locations and {} landmass zones.",
                GeographicFeatureRegistry.getFeatureCount(),
                WorldReferenceCatalog.get()
                        .locations()
                        .size(),
                LandmassZoneCatalog.get()
                        .size()
        );

        KnownWorldDebugCommand.register();

        KnownWorldGeoDebugCommand.register();

        WorldReferenceDebugCommand.register();

        NpcSimulation.registerLifecycle();

        NpcDebugCommand.register();

        NpcNeedsDebugCommand.register();

        NpcRoutineDebugCommand.register();

        SocialDebugCommand.register();

        TitleDebugCommand.register();

        BootstrapDebugCommand.register();

        FamilyDebugCommand.register();

        LifeCycleDebugCommand.register();

        SuccessionDebugCommand.register();

        CharacterProfileDebugCommand.register();

        WorldEventDebugCommand.register();

        LOGGER.info(
                "Known World loaded with {} geographic features.",
                GeographicFeatureRegistry.getFeatureCount()
        );

        LOGGER.info(
                "Known World simulation hooks registered."
        );
    }
}
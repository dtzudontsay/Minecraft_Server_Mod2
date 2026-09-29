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
import dev.dtzudontsay.knownworld.world.reference.spatial.TerritoryZoneCatalog;
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

        KnownWorldBiomeSources.register();

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

        WorldReferenceSettings.bootstrap();

        WorldReferenceCatalog.bootstrap();

        /*
         * Top-level landmass envelopes:
         *
         * Westeros / Essos / Sothoryos / Ulthos / Summer Isles.
         *
         * These may extend into sea on the authoring map. Runtime
         * resolution still requires the terrain sampler to report LAND.
         */
        LandmassZoneCatalog.bootstrap();

        /*
         * Major realm / regional territory layer.
         *
         * Every territory is restricted to its parent landmass,
         * preventing sea-side authoring envelopes from assigning
         * islands to an unrelated continent.
         */
        TerritoryZoneCatalog.bootstrap();

        KnownWorldGeoData geodata =
                KnownWorldGeoData.getInstance();

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
                "Known World reference system ready with {} geographic features, {} reference locations, {} landmass zones and {} territory zones.",
                GeographicFeatureRegistry.getFeatureCount(),
                WorldReferenceCatalog.get()
                        .locations()
                        .size(),
                LandmassZoneCatalog.get()
                        .size(),
                TerritoryZoneCatalog.get()
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
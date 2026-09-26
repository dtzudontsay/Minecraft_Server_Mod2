package dev.dtzudontsay.knownworld;

import dev.dtzudontsay.knownworld.debug.KnownWorldDebugCommand;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.NorthEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.NorthFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.NorthWestEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SouthEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SouthFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SouthWestEssosFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.regions.SummerIslesFeatureRegistry;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KnownWorld implements ModInitializer {

    public static final String MOD_ID =
            "knownworld";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        NorthFeatureRegistry.bootstrap();
        SouthFeatureRegistry.bootstrap();
        SummerIslesFeatureRegistry.bootstrap();
        SouthWestEssosFeatureRegistry.bootstrap();
        NorthWestEssosFeatureRegistry.bootstrap();
        NorthEssosFeatureRegistry.bootstrap();
        SouthEssosFeatureRegistry.bootstrap();

        KnownWorldDebugCommand.register();

        LOGGER.info(
                "Known World loaded with {} geographic features.",
                GeographicFeatureRegistry.getFeatureCount()
        );
    }
}
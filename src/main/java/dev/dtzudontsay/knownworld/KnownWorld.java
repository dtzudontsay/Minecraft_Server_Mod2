package dev.dtzudontsay.knownworld;

import dev.dtzudontsay.knownworld.debug.KnownWorldDebugCommand;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KnownWorld implements ModInitializer {
    public static final String MOD_ID = "knownworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        KnownWorldDebugCommand.register();
        LOGGER.info("Known World loaded successfully.");
    }
}

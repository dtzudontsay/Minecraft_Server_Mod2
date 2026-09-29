package dev.dtzudontsay.knownworld.world.biome;

import dev.dtzudontsay.knownworld.KnownWorld;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;


public final class KnownWorldBiomeSources {

    public static final Identifier KNOWN_WORLD_ID =
            Identifier.fromNamespaceAndPath(
                    KnownWorld.MOD_ID,
                    "known_world"
            );


    private static boolean registered =
            false;


    private KnownWorldBiomeSources() {
    }


    public static void register() {

        if (
                registered
        ) {

            return;
        }


        Registry.register(
                BuiltInRegistries.BIOME_SOURCE,
                KNOWN_WORLD_ID,
                KnownWorldBiomeSource.CODEC
        );


        registered =
                true;


        KnownWorld.LOGGER.info(
                "Registered Known World biome source: {}",
                KNOWN_WORLD_ID
        );
    }
}
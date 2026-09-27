package dev.dtzudontsay.knownworld.world.generation;

import dev.dtzudontsay.knownworld.KnownWorld;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class KnownWorldChunkGenerators {

    public static final Identifier KNOWN_WORLD_ID =
            Identifier.fromNamespaceAndPath(
                    KnownWorld.MOD_ID,
                    "known_world"
            );

    private static boolean registered =
            false;

    private KnownWorldChunkGenerators() {
    }

    public static void register() {

        if (
                registered
        ) {
            return;
        }

        Registry.register(
                BuiltInRegistries.CHUNK_GENERATOR,
                KNOWN_WORLD_ID,
                KnownWorldChunkGenerator.CODEC
        );

        registered =
                true;

        KnownWorld.LOGGER.info(
                "Registered Known World chunk generator: {}",
                KNOWN_WORLD_ID
        );
    }
}
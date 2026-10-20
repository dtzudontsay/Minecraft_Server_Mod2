
package dev.dtzudontsay.knownworld.simulation.integration;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * B0: Set Peaceful for newly initialized Known World saves.
 *
 * Does not change vanilla-generator worlds.
 * Does not override difficulty on an existing Known World save
 * with an existing NPC simulation calendar.
 *
 * Peaceful disables vanilla hostile spawning while permitting
 * passive mobs and the mod's regular NPC projections.
 */
public final class KnownWorldWorldDefaults
        implements ModInitializer {

    private static MinecraftServer activeServer;
    private static boolean eligibleNewWorld;

    @Override
    public void onInitialize() {

        /*
         * SERVER_STARTING runs before the existing NpcSimulation
         * SERVER_STARTED initialization creates its save files.
         *
         * Capture the "new simulation" condition here, not
         * during SERVER_STARTED.
         */
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {

            activeServer = server;

            Path root = server.getWorldPath(
                    LevelResource.ROOT
            );

            Path oldCalendar = root.resolve(
                    "knownworld/npc/campaign_calendar.tsv"
            );

            Path defaultsMarker = root.resolve(
                    "knownworld/world_defaults_v1.txt"
            );

            eligibleNewWorld =
                    !Files.exists(oldCalendar)
                            && !Files.exists(defaultsMarker);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {

            if (activeServer != server || !eligibleNewWorld) {
                return;
            }

            ServerLevel overworld =
                    server.getLevel(Level.OVERWORLD);

            if (overworld == null
                    || !(overworld.getChunkSource().getGenerator()
                    instanceof KnownWorldChunkGenerator)) {

                return;
            }

            /*
             * The normal difficulty command calls this same
             * MinecraftServer method in Minecraft 26.3.
             *
             * Never lock the difficulty. The player may
             * deliberately change it later.
             */
            server.setDifficulty(Difficulty.PEACEFUL, true);

            Path root = server.getWorldPath(
                    LevelResource.ROOT
            );

            Path marker = root.resolve(
                    "knownworld/world_defaults_v1.txt"
            );

            try {

                Files.createDirectories(marker.getParent());

                Files.writeString(
                        marker,
                        """
                        KNOWNWORLD_WORLD_DEFAULTS_V1
                        difficulty=peaceful
                        applied=first_knownworld_initialization
                        existing_worlds_not_overridden=true
                        """,
                        StandardCharsets.UTF_8
                );

            } catch (IOException exception) {

                KnownWorld.LOGGER.error(
                        "New Known World difficulty was set, "
                                + "but its defaults marker could not be saved.",
                        exception
                );
            }

            KnownWorld.LOGGER.info(
                    "New Known World initialized on PEACEFUL. "
                            + "Vanilla hostile mobs disabled; "
                            + "ordinary villagers and animals allowed."
            );
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {

            if (activeServer == server) {
                activeServer = null;
                eligibleNewWorld = false;
            }
        });

        KnownWorld.LOGGER.info(
                "Known World first-creation difficulty policy registered."
        );
    }
}


package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Batch 21.2
 *
 * Economy persistence lifecycle.
 *
 * Loads after the NPC simulation's registration,
 * validates references, and saves only when an
 * economy file already exists or runtime inventories
 * have explicitly been created.
 *
 * No resources or inventories are seeded here.
 */
public final class EconomyRuntime implements ModInitializer {

    private static EconomyRuntime active;

    private MinecraftServer server;
    private InventoryManager inventories;
    private InventoryPersistence persistence;
    private EconomyReferenceValidator validator;

    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                EconomyRuntime::start
        );

        ServerLifecycleEvents.BEFORE_SAVE.register(
                (server, flush, force) -> saveActive(server)
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                EconomyRuntime::saveActive
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> {
                    if (active != null
                            && active.server == server) {
                        active = null;
                    }
                }
        );
    }

    private static void start(MinecraftServer server) {
        if (active != null) {
            throw new IllegalStateException(
                    "Economy runtime already active"
            );
        }

        NpcSimulation simulation =
                NpcSimulation.getNullable();

        if (simulation == null
                || simulation.server() != server) {
            throw new IllegalStateException(
                    "NPC simulation must initialize "
                            + "before economy runtime"
            );
        }

        EconomyRuntime runtime = new EconomyRuntime();

        runtime.server = server;

        Path directory = server.getWorldPath(
                LevelResource.ROOT
        ).resolve("knownworld").resolve("npc");

        runtime.persistence =
                new InventoryPersistence(directory);

        runtime.validator =
                new EconomyReferenceValidator(
                        simulation.registry()::contains,
                        id -> simulation.settlements()
                                .find(id).isPresent(),
                        id -> simulation.organizations()
                                .find(id).isPresent(),
                        id -> false
                );

        try {
            runtime.inventories = runtime.persistence.load(
                    ResourceCatalog.benchmarkDefaults()
            );

            runtime.validator.validate(
                    runtime.inventories
            );

        } catch (IOException | RuntimeException exception) {
            KnownWorld.LOGGER.error(
                    "Economy initialization failed. "
                            + "Existing inventories were not reset.",
                    exception
            );

            throw new IllegalStateException(
                    "Unsafe economy save or references. "
                            + "World startup rejected to protect data.",
                    exception
            );
        }

        active = runtime;

        KnownWorld.LOGGER.info(
                "Known World economy ready: {} inventories; "
                        + "existing economy save: {}",
                runtime.inventories.all().size(),
                runtime.persistence.exists()
        );
    }

    public static EconomyRuntime getNullable() {
        return active;
    }

    public InventoryManager inventories() {
        if (inventories == null) {
            throw new IllegalStateException(
                    "Economy runtime is not initialized"
            );
        }

        return inventories;
    }

    /**
     * Validate before writing.
     *
     * Empty new economies are not automatically
     * persisted merely because a world was opened.
     */
    private static void saveActive(MinecraftServer server) {
        EconomyRuntime runtime = active;

        if (runtime == null || runtime.server != server) {
            return;
        }

        try {
            runtime.validator.validate(runtime.inventories);

            if (!runtime.persistence.exists()
                    && runtime.inventories.all().isEmpty()) {
                return;
            }

            runtime.persistence.save(runtime.inventories);

        } catch (IOException | RuntimeException exception) {
            KnownWorld.LOGGER.error(
                    "Failed to save Known World economy. "
                            + "Existing data must be preserved.",
                    exception
            );

            throw new IllegalStateException(
                    "Economy save failed",
                    exception
            );
        }
    }
}

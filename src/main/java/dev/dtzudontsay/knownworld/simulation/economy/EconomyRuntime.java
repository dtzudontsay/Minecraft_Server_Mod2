
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Batch 21.3.
 *
 * Economy lifecycle with validation against authoritative
 * NPC, settlement, organization and holding registries.
 *
 * No automatic inventory or resource initialization.
 */
public final class EconomyRuntime implements ModInitializer {

    private static EconomyRuntime active;

    private MinecraftServer server;
    private InventoryManager inventories;
    private InventoryPersistence persistence;
    private EconomyReferenceValidator validator;
    private EconomyInventoryProvisioner provisioner;

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

        SocietyStructureRuntime society =
                SocietyStructureRuntime.getNullable();

        if (simulation == null
                || simulation.server() != server) {
            throw new IllegalStateException(
                    "NPC simulation must initialize "
                            + "before the economy"
            );
        }

        if (society == null) {
            throw new IllegalStateException(
                    "Society structure runtime must initialize "
                            + "before the economy"
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

                        id -> society.holdings()
                                .find(id).isPresent()
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
                            + "Existing inventory data was not reset.",
                    exception
            );

            throw new IllegalStateException(
                    "Unsafe economy save or references. "
                            + "Initialization rejected.",
                    exception
            );
        }

        runtime.provisioner =
                new EconomyInventoryProvisioner(
                        runtime.inventories,
                        runtime.validator
                );

        active = runtime;

        KnownWorld.LOGGER.info(
                "Known World economy ready: {} inventories; "
                        + "existing economy save: {}; "
                        + "holding validation enabled",
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
     * The supported entry point for provisioning
     * a validated empty inventory.
     *
     * Does not add resource balances.
     */
    public EconomyInventoryProvisioner provisioner() {
        if (provisioner == null) {
            throw new IllegalStateException(
                    "Economy provisioner is not initialized"
            );
        }

        return provisioner;
    }

    private static void saveActive(MinecraftServer server) {
        EconomyRuntime runtime = active;

        if (runtime == null || runtime.server != server) {
            return;
        }

        try {
            runtime.validator.validate(
                    runtime.inventories
            );

            // Opening an old world must not by itself
            // generate an economy save file.
            if (!runtime.persistence.exists()
                    && runtime.inventories.all().isEmpty()) {
                return;
            }

            runtime.persistence.save(
                    runtime.inventories
            );

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

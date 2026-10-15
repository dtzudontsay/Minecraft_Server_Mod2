
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
 * Batch 22.0.
 *
 * Economy runtime with explicit persistent authority.
 *
 * Existing worlds use legacy compatibility unless
 * snapshot authority has been explicitly activated.
 *
 * Never seeds resources or migrates automatically.
 */
public final class EconomyRuntime implements ModInitializer {

    private static EconomyRuntime active;

    private MinecraftServer server;
    private Path saveDirectory;

    private ResourceCatalog catalog;
    private InventoryManager inventories;
    private EconomyReferenceValidator validator;
    private EconomyInventoryProvisioner provisioner;
    private EconomyAuthorityService.Loaded loaded;

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
                    "NPC simulation must initialize first"
            );
        }

        if (society == null) {
            throw new IllegalStateException(
                    "Society structures must initialize first"
            );
        }

        EconomyRuntime runtime = new EconomyRuntime();

        runtime.server = server;

        runtime.saveDirectory =
                server.getWorldPath(LevelResource.ROOT)
                        .resolve("knownworld")
                        .resolve("npc");

        runtime.catalog =
                ResourceCatalog.benchmarkDefaults();

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
            runtime.loaded =
                    EconomyAuthorityService.load(
                            runtime.saveDirectory,
                            runtime.catalog
                    );

            runtime.inventories =
                    runtime.loaded.manager();

            runtime.validator.validate(
                    runtime.inventories
            );

        } catch (IOException | RuntimeException exception) {
            KnownWorld.LOGGER.error(
                    "Economy authority or inventory validation "
                            + "failed. No economic save was reset.",
                    exception
            );

            throw new IllegalStateException(
                    "Unsafe economic state: startup rejected",
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
                        + "authority mode: {}; selection: {}; "
                        + "holding validation enabled",
                runtime.inventories.all().size(),
                runtime.loaded.mode(),
                runtime.loaded.description()
        );
    }

    public static EconomyRuntime getNullable() {
        return active;
    }

    public InventoryManager inventories() {
        if (inventories == null) {
            throw new IllegalStateException(
                    "Economy runtime not initialized"
            );
        }

        return inventories;
    }

    public EconomyInventoryProvisioner provisioner() {
        if (provisioner == null) {
            throw new IllegalStateException(
                    "Economy provisioner not initialized"
            );
        }

        return provisioner;
    }

    public EconomyAuthorityService.Mode authorityMode() {
        if (loaded == null) {
            throw new IllegalStateException(
                    "Economy authority not initialized"
            );
        }

        return loaded.mode();
    }

    private static void saveActive(MinecraftServer server) {

        EconomyRuntime runtime = active;

        if (runtime == null
                || runtime.server != server) {
            return;
        }

        try {
            runtime.validator.validate(
                    runtime.inventories
            );

            EconomyAuthorityService.save(
                    runtime.saveDirectory,
                    runtime.catalog,
                    runtime.loaded
            );

        } catch (IOException | RuntimeException exception) {
            KnownWorld.LOGGER.error(
                    "Economy save failed; existing economic "
                            + "files must be preserved.",
                    exception
            );

            throw new IllegalStateException(
                    "Economy save failed",
                    exception
            );
        }
    }
}

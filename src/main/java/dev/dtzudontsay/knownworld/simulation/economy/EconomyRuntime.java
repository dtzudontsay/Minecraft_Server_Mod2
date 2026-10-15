
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Batch 22.5.
 *
 * Economy runtime with checkpoint scheduling for
 * explicitly activated snapshot-primary worlds.
 *
 * Legacy worlds remain compatible and are not
 * automatically migrated.
 *
 * No automatic production, consumption or
 * inventory provisioning is activated here.
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
    private EconomyCheckpointScheduler scheduler;

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

        ServerTickEvents.END_SERVER_TICK.register(
                EconomyRuntime::tickActive
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
                    "Society structure runtime must initialize first"
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
                    "Economy initialization rejected. "
                            + "Existing economic files were not reset.",
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

        runtime.scheduler =
                new EconomyCheckpointScheduler(
                        runtime.loaded.mode(),
                        runtime.inventories,
                        runtime::saveEconomy
                );

        active = runtime;

        KnownWorld.LOGGER.info(
                "Known World economy ready: {} inventories; "
                        + "authority mode: {}; selection: {}; "
                        + "holding validation enabled; "
                        + "checkpoint interval: {} ticks; "
                        + "threshold: {} transactions",
                runtime.inventories.all().size(),
                runtime.loaded.mode(),
                runtime.loaded.description(),
                runtime.scheduler.checkIntervalTicks(),
                runtime.scheduler.checkpointThreshold()
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

    public EconomyCheckpointScheduler scheduler() {
        if (scheduler == null) {
            throw new IllegalStateException(
                    "Economy scheduler not initialized"
            );
        }

        return scheduler;
    }

    private static void tickActive(MinecraftServer server) {

        EconomyRuntime runtime = active;

        if (runtime == null || runtime.server != server) {
            return;
        }

        try {
            EconomyCheckpointScheduler.Decision result =
                    runtime.scheduler.tick();

            if (result ==
                    EconomyCheckpointScheduler.Decision
                            .CHECKPOINT_REQUIRED) {

                KnownWorld.LOGGER.info(
                        "Known World economic checkpoint committed "
                                + "and journal rotated; total scheduled "
                                + "checkpoints this session: {}",
                        runtime.scheduler.successfulCheckpoints()
                );
            }

        } catch (IOException | RuntimeException exception) {
            KnownWorld.LOGGER.error(
                    "Scheduled economic checkpoint failed. "
                            + "Economic transactions must not "
                            + "continue without a verified save.",
                    exception
            );

            /*
             * Fail closed instead of continuing with
             * an at-risk economic journal.
             *
             * Existing authoritative economic files
             * are not deliberately reset.
             */
            throw new IllegalStateException(
                    "Scheduled economic checkpoint failed",
                    exception
            );
        }
    }

    private static void saveActive(MinecraftServer server) {

        EconomyRuntime runtime = active;

        if (runtime == null || runtime.server != server) {
            return;
        }

        try {
            runtime.saveEconomy();

        } catch (IOException | RuntimeException exception) {
            KnownWorld.LOGGER.error(
                    "Economy save failed. Existing economic "
                            + "data must be preserved.",
                    exception
            );

            throw new IllegalStateException(
                    "Economy save failed",
                    exception
            );
        }
    }

    private void saveEconomy() throws IOException {

        /*
         * Every economy checkpoint validates current
         * owner and location references.
         */
        validator.validate(inventories);

        EconomyAuthorityService.save(
                saveDirectory,
                catalog,
                loaded
        );
    }
}

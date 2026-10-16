
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Batch 22.10.
 *
 * Economy runtime with:
 *
 * - Existing economy authority selection
 * - Inventory reference validation
 * - Existing checkpoint scheduling
 * - Shared campaign-day observation
 * - Fast-forward day-boundary integration
 *
 * Agricultural day observation is MONITOR_ONLY.
 *
 * No automatic agricultural inventory provisioning,
 * population consumption or resource generation.
 */
public final class EconomyRuntime implements ModInitializer {

    private static EconomyRuntime active;

    private MinecraftServer server;
    private NpcSimulation simulation;

    private Path saveDirectory;

    private ResourceCatalog catalog;
    private InventoryManager inventories;

    private EconomyReferenceValidator validator;
    private EconomyInventoryProvisioner provisioner;

    private EconomyAuthorityService.Loaded loaded;
    private EconomyCheckpointScheduler scheduler;

    private CampaignAgricultureDayCoordinator
            campaignDays;

    private Consumer<SimulationTimeContract>
            fastForwardObserver;

    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                EconomyRuntime::start
        );

        ServerLifecycleEvents.BEFORE_SAVE.register(
                (server, flush, force) ->
                        saveActive(server)
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                EconomyRuntime::saveActive
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                EconomyRuntime::stop
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
                    "Society structure runtime must "
                            + "initialize first"
            );
        }

        EconomyRuntime runtime = new EconomyRuntime();

        runtime.server = server;
        runtime.simulation = simulation;

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

        /*
         * Preserve the existing economic checkpoint
         * scheduler from Batch 22.5.
         */
        runtime.scheduler =
                new EconomyCheckpointScheduler(
                        runtime.loaded.mode(),
                        runtime.inventories,
                        runtime::saveEconomy
                );

        /*
         * NEW:
         * Monitor authoritative campaign progression.
         *
         * The initial day is captured from the real
         * campaign calendar. No startup callback
         * pretends that a new day has already passed.
         */
        runtime.campaignDays =
                CampaignAgricultureDayCoordinator
                        .monitorOnly(
                                simulation.campaignCalendar()
                                        .absoluteDay()
                        );

        /*
         * Reuse this exact same coordinator for
         * accelerated campaign progression.
         *
         * The observer does not execute agriculture
         * because the coordinator is MONITOR_ONLY.
         */
        runtime.fastForwardObserver =
                time -> runtime.campaignDays.observe(
                        time
                );

        simulation.fastForward()
                .registerCampaignDayObserver(
                        runtime.fastForwardObserver
                );

        active = runtime;

        KnownWorld.LOGGER.info(
                "Known World economy ready: {} inventories; "
                        + "authority mode: {}; selection: {}; "
                        + "holding validation enabled; "
                        + "checkpoint interval: {} ticks; "
                        + "threshold: {} transactions; "
                        + "campaign agriculture: MONITOR_ONLY",
                runtime.inventories.all().size(),
                runtime.loaded.mode(),
                runtime.loaded.description(),
                runtime.scheduler.checkIntervalTicks(),
                runtime.scheduler.checkpointThreshold()
        );
    }

    private static void stop(MinecraftServer server) {

        EconomyRuntime runtime = active;

        if (runtime == null
                || runtime.server != server) {
            return;
        }

        /*
         * Release the callback from the existing
         * fast-forward service on world shutdown.
         *
         * Prevent stale economy sessions from
         * receiving later campaign-day events.
         */
        if (runtime.simulation != null
                && runtime.fastForwardObserver != null) {

            runtime.simulation.fastForward()
                    .removeCampaignDayObserver(
                            runtime.fastForwardObserver
                    );
        }

        active = null;
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

    public CampaignAgricultureDayCoordinator campaignDays() {
        if (campaignDays == null) {
            throw new IllegalStateException(
                    "Campaign agriculture coordinator "
                            + "not initialized"
            );
        }

        return campaignDays;
    }

    private static void tickActive(
            MinecraftServer server
    ) {

        EconomyRuntime runtime = active;

        if (runtime == null
                || runtime.server != server) {
            return;
        }

        try {
            /*
             * Observe the existing NPC simulation's
             * time after its normal tick processing.
             *
             * Reconstructing a read-only SimulationClock
             * from the authoritative current tick does
             * not introduce another mutable timeline.
             */
            SimulationTimeContract time =
                    SimulationTimeContract.capture(
                            new SimulationClock(
                                    runtime.simulation
                                            .serverTickCounter()
                            ),
                            runtime.simulation
                                    .campaignCalendar()
                    );

            runtime.campaignDays.observe(time);

            /*
             * Existing transaction journal checkpoint
             * scheduling remains unchanged.
             */
            EconomyCheckpointScheduler.Decision result =
                    runtime.scheduler.tick();

            if (result ==
                    EconomyCheckpointScheduler.Decision
                            .CHECKPOINT_REQUIRED) {

                KnownWorld.LOGGER.info(
                        "Known World economic checkpoint committed "
                                + "and journal rotated; "
                                + "scheduled checkpoints: {}",
                        runtime.scheduler.successfulCheckpoints()
                );
            }

        } catch (IOException | RuntimeException exception) {

            KnownWorld.LOGGER.error(
                    "Economic tick processing failed. "
                            + "No economic state was deliberately reset.",
                    exception
            );

            throw new IllegalStateException(
                    "Economic tick processing failed",
                    exception
            );
        }
    }

    private static void saveActive(
            MinecraftServer server
    ) {

        EconomyRuntime runtime = active;

        if (runtime == null
                || runtime.server != server) {
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

        validator.validate(inventories);

        EconomyAuthorityService.save(
                saveDirectory,
                catalog,
                loaded
        );
    }
}

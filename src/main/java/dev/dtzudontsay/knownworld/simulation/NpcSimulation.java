package dev.dtzudontsay.knownworld.simulation;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.persistence.NpcPersistence;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Top-level owner of the persistent NPC simulation.
 *
 * There is one simulation instance for the currently running server.
 */
public final class NpcSimulation {

    private static final int ACTIVATION_INTERVAL_TICKS =
            20;

    private static NpcSimulation instance;

    private final MinecraftServer server;

    private final NpcRegistry registry;

    private final NpcPersistence persistence;

    private final NpcActivationManager activationManager;

    private long serverTickCounter;

    private NpcSimulation(
            MinecraftServer server
    ) {
        this.server =
                server;

        this.registry =
                new NpcRegistry();

        this.activationManager =
                new NpcActivationManager(
                        registry
                );

        Path savePath =
                server.getServerDirectory()
                        .resolve("knownworld")
                        .resolve("npc");

        this.persistence =
                new NpcPersistence(
                        savePath
                );

        this.serverTickCounter =
                0L;
    }

    public static void registerLifecycle() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> {
                    NpcSimulation simulation =
                            new NpcSimulation(
                                    server
                            );

                    simulation.load();

                    instance =
                            simulation;

                    /*
                     * Perform the first activation calculation
                     * immediately.
                     */
                    simulation.activationManager
                            .update(
                                    server
                            );

                    KnownWorld.LOGGER.info(
                            "NPC simulation started with {} NPCs.",
                            simulation.registry.size()
                    );
                }
        );

        ServerTickEvents.END_SERVER_TICK.register(
                server -> {
                    NpcSimulation simulation =
                            instance;

                    if (simulation != null
                            && simulation.server
                            == server) {

                        simulation.tick();
                    }
                }
        );

        ServerLifecycleEvents.BEFORE_SAVE.register(
                (
                        server,
                        flush,
                        force
                ) -> {
                    NpcSimulation simulation =
                            instance;

                    if (simulation != null
                            && simulation.server
                            == server) {

                        simulation.save();
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                server -> {
                    NpcSimulation simulation =
                            instance;

                    if (simulation != null
                            && simulation.server
                            == server) {

                        simulation.save();
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> {
                    if (instance != null
                            && instance.server
                            == server) {

                        instance =
                                null;
                    }
                }
        );
    }

    public static NpcSimulation get() {
        NpcSimulation simulation =
                instance;

        if (simulation == null) {
            throw new IllegalStateException(
                    "NPC simulation is not currently running"
            );
        }

        return simulation;
    }

    public static NpcSimulation getNullable() {
        return instance;
    }

    public MinecraftServer server() {
        return server;
    }

    public NpcRegistry registry() {
        return registry;
    }

    public NpcActivationManager activationManager() {
        return activationManager;
    }

    public long serverTickCounter() {
        return serverTickCounter;
    }

    /**
     * Main simulation heartbeat.
     *
     * Eventually this becomes the orchestration point for:
     *
     * - NPC needs
     * - goals/actions
     * - travel
     * - schedules
     * - information propagation
     * - relationships
     * - settlements
     * - factions
     * - armies
     * - battles
     */
    private void tick() {
        serverTickCounter++;

        if (
                serverTickCounter
                        % ACTIVATION_INTERVAL_TICKS
                        == 0
        ) {
            activationManager.update(
                    server
            );
        }
    }

    public void save() {
        try {
            persistence.save(
                    registry
            );

        } catch (IOException exception) {

            KnownWorld.LOGGER.error(
                    "Failed to save NPC simulation.",
                    exception
            );
        }
    }

    private void load() {
        try {
            persistence.loadInto(
                    registry
            );

        } catch (IOException exception) {

            KnownWorld.LOGGER.error(
                    "Failed to load NPC simulation.",
                    exception
            );
        }
    }
}
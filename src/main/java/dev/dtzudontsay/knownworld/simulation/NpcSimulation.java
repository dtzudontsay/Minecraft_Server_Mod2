package dev.dtzudontsay.knownworld.simulation;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.action.NpcActionProcessor;
import dev.dtzudontsay.knownworld.simulation.npc.communication.NpcCommunicationService;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.observation.NpcObservationService;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.persistence.NpcPersistence;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;

public final class NpcSimulation {

    private static final int ACTIVATION_INTERVAL_TICKS =
            20;

    private static final int ACTION_INTERVAL_TICKS =
            20;

    private static NpcSimulation instance;

    private final MinecraftServer server;

    private final NpcRegistry registry;

    private final NpcRelationshipManager relationshipManager;

    private final NpcKnowledgeManager knowledgeManager;

    private final NpcMemoryManager memoryManager;

    private final NpcGoalManager goalManager;

    private final WorldEventManager eventManager;

    private final NpcCommunicationService communicationService;

    private final NpcObservationService observationService;

    private final NpcActionProcessor actionProcessor;

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

        this.relationshipManager =
                new NpcRelationshipManager(
                        registry
                );

        this.knowledgeManager =
                new NpcKnowledgeManager(
                        registry
                );

        this.memoryManager =
                new NpcMemoryManager(
                        registry
                );

        this.goalManager =
                new NpcGoalManager(
                        registry
                );

        this.eventManager =
                new WorldEventManager(
                        registry
                );

        this.communicationService =
                new NpcCommunicationService(
                        registry,
                        relationshipManager,
                        knowledgeManager,
                        memoryManager
                );

        this.observationService =
                new NpcObservationService(
                        registry,
                        knowledgeManager,
                        memoryManager
                );

        this.actionProcessor =
                new NpcActionProcessor(
                        registry,
                        goalManager,
                        memoryManager,
                        eventManager
                );

        this.activationManager =
                new NpcActivationManager(
                        registry
                );

        Path savePath =
                server.getServerDirectory()
                        .resolve(
                                "knownworld"
                        )
                        .resolve(
                                "npc"
                        );

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

                    simulation.activationManager
                            .update(
                                    server
                            );

                    KnownWorld.LOGGER.info(
                            "NPC simulation started with {} NPCs, {} relationships, {} beliefs, {} memories, {} goals and {} world events.",
                            simulation.registry.size(),
                            simulation.relationshipManager.size(),
                            simulation.knowledgeManager.size(),
                            simulation.memoryManager.size(),
                            simulation.goalManager.size(),
                            simulation.eventManager.size()
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

    public NpcRelationshipManager relationships() {
        return relationshipManager;
    }

    public NpcKnowledgeManager knowledge() {
        return knowledgeManager;
    }

    public NpcMemoryManager memories() {
        return memoryManager;
    }

    public NpcGoalManager goals() {
        return goalManager;
    }

    public WorldEventManager events() {
        return eventManager;
    }

    public NpcCommunicationService communication() {
        return communicationService;
    }

    public NpcObservationService observation() {
        return observationService;
    }

    public NpcActivationManager activationManager() {
        return activationManager;
    }

    public long serverTickCounter() {
        return serverTickCounter;
    }

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

        if (
                serverTickCounter
                        % ACTION_INTERVAL_TICKS
                        == 0
        ) {

            actionProcessor.update(
                    serverTickCounter
            );
        }
    }

    public void save() {

        try {

            persistence.save(
                    registry,
                    relationshipManager,
                    knowledgeManager,
                    memoryManager,
                    goalManager,
                    eventManager
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
                    registry,
                    relationshipManager,
                    knowledgeManager,
                    memoryManager,
                    goalManager,
                    eventManager
            );

        } catch (IOException exception) {

            KnownWorld.LOGGER.error(
                    "Failed to load NPC simulation.",
                    exception
            );
        }
    }
}
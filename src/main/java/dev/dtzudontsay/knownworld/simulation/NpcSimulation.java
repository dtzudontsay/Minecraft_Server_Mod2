package dev.dtzudontsay.knownworld.simulation;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.bootstrap.AuthoredIdRegistry;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioBootstrapper;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.action.NpcActionProcessor;
import dev.dtzudontsay.knownworld.simulation.npc.communication.NpcCommunicationService;
import dev.dtzudontsay.knownworld.simulation.npc.decision.NpcDecisionService;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeedManager;
import dev.dtzudontsay.knownworld.simulation.npc.observation.NpcObservationService;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineManager;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineService;
import dev.dtzudontsay.knownworld.simulation.persistence.AuthoredIdPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.NpcPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.TitlePersistence;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.authority.AuthorityService;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;

public final class NpcSimulation {

    private static final int UPDATE_INTERVAL_TICKS =
            20;

    private static NpcSimulation instance;

    private final MinecraftServer server;

    private final SimulationClock clock;

    private final NpcRegistry registry;

    private final SettlementManager settlementManager;

    private final OrganizationManager organizationManager;

    private final NpcAffiliationManager affiliationManager;

    private final TitleManager titleManager;

    private final AuthorityService authorityService;

    private final AuthoredIdRegistry authoredIdRegistry;

    private final NpcRelationshipManager relationshipManager;

    private final NpcKnowledgeManager knowledgeManager;

    private final NpcMemoryManager memoryManager;

    private final NpcGoalManager goalManager;

    private final NpcNeedManager needManager;

    private final NpcRoutineManager routineManager;

    private final WorldEventManager eventManager;

    private final NpcCommunicationService communicationService;

    private final NpcObservationService observationService;

    private final NpcDecisionService decisionService;

    private final NpcRoutineService routineService;

    private final NpcActionProcessor actionProcessor;

    private final NpcPersistence persistence;

    private final TitlePersistence titlePersistence;

    private final AuthoredIdPersistence authoredIdPersistence;

    private final NpcActivationManager activationManager;

    private NpcSimulation(
            MinecraftServer server
    ) {
        this.server =
                server;

        this.clock =
                new SimulationClock();

        this.registry =
                new NpcRegistry();

        this.settlementManager =
                new SettlementManager();

        this.organizationManager =
                new OrganizationManager(
                        settlementManager
                );

        this.affiliationManager =
                new NpcAffiliationManager(
                        registry,
                        settlementManager,
                        organizationManager
                );

        this.titleManager =
                new TitleManager(
                        registry,
                        organizationManager,
                        settlementManager
                );

        this.authorityService =
                new AuthorityService(
                        organizationManager,
                        titleManager
                );

        this.authoredIdRegistry =
                new AuthoredIdRegistry();

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

        this.needManager =
                new NpcNeedManager(
                        registry
                );

        this.routineManager =
                new NpcRoutineManager(
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

        this.decisionService =
                new NpcDecisionService(
                        registry,
                        needManager,
                        goalManager
                );

        this.routineService =
                new NpcRoutineService(
                        registry,
                        routineManager,
                        goalManager
                );

        this.actionProcessor =
                new NpcActionProcessor(
                        registry,
                        goalManager,
                        needManager,
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

        this.titlePersistence =
                new TitlePersistence(
                        savePath
                );

        this.authoredIdPersistence =
                new AuthoredIdPersistence(
                        savePath
                );
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
                            "NPC simulation started at tick {} with {} NPCs, {} settlements, {} organizations, {} affiliations, {} titles and scenario '{}'.",
                            simulation.clock.tick(),
                            simulation.registry.size(),
                            simulation.settlementManager.size(),
                            simulation.organizationManager.size(),
                            simulation.affiliationManager.size(),
                            simulation.titleManager.definitionCount(),
                            simulation.authoredIdRegistry.scenarioId()
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

        if (instance == null) {

            throw new IllegalStateException(
                    "NPC simulation is not currently running"
            );
        }

        return instance;
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

    public SettlementManager settlements() {
        return settlementManager;
    }

    public OrganizationManager organizations() {
        return organizationManager;
    }

    public NpcAffiliationManager affiliations() {
        return affiliationManager;
    }

    public TitleManager titles() {
        return titleManager;
    }

    public AuthorityService authority() {
        return authorityService;
    }

    public AuthoredIdRegistry authoredIds() {
        return authoredIdRegistry;
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

    public NpcNeedManager needs() {
        return needManager;
    }

    public NpcRoutineManager routines() {
        return routineManager;
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
        return clock.tick();
    }

    private void tick() {

        clock.advance();

        long tick =
                clock.tick();

        if (
                tick
                        % UPDATE_INTERVAL_TICKS
                        != 0
        ) {

            return;
        }

        activationManager.update(
                server
        );

        needManager.update();

        decisionService.update(
                tick
        );

        routineService.update(
                tick
        );

        actionProcessor.update(
                tick
        );
    }

    public void save() {

        try {

            persistence.save(
                    clock,
                    registry,
                    settlementManager,
                    organizationManager,
                    affiliationManager,
                    relationshipManager,
                    knowledgeManager,
                    memoryManager,
                    goalManager,
                    needManager,
                    routineManager,
                    eventManager
            );

            titlePersistence.save(
                    titleManager
            );

            authoredIdPersistence.save(
                    authoredIdRegistry
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
                    clock,
                    registry,
                    settlementManager,
                    organizationManager,
                    affiliationManager,
                    relationshipManager,
                    knowledgeManager,
                    memoryManager,
                    goalManager,
                    needManager,
                    routineManager,
                    eventManager
            );

            titlePersistence.loadInto(
                    titleManager
            );

            authoredIdPersistence.loadInto(
                    authoredIdRegistry
            );

            if (shouldBootstrapScenario()) {

                KnownWorld.LOGGER.info(
                        "No existing NPC simulation state detected. Bootstrapping default authored scenario."
                );

                ScenarioBootstrapper.bootstrapDefault(
                        this,
                        authoredIdRegistry
                );

                save();

            } else if (
                    authoredIdRegistry.scenarioId()
                            == null
            ) {

                KnownWorld.LOGGER.warn(
                        "Existing NPC simulation data was found without authored scenario metadata. Automatic scenario bootstrap was skipped."
                );
            }

        } catch (IOException exception) {

            KnownWorld.LOGGER.error(
                    "Failed to load NPC simulation.",
                    exception
            );
        } catch (RuntimeException exception) {

            KnownWorld.LOGGER.error(
                    "Failed to bootstrap authored NPC scenario.",
                    exception
            );
        }
    }

    private boolean shouldBootstrapScenario() {

        return authoredIdRegistry.isEmpty()
                && registry.size() == 0
                && settlementManager.size() == 0
                && organizationManager.size() == 0
                && titleManager.definitionCount() == 0;
    }
}
package dev.dtzudontsay.knownworld.simulation;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.bootstrap.AuthoredIdRegistry;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioBootstrapper;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.action.NpcActionProcessor;
import dev.dtzudontsay.knownworld.simulation.npc.communication.NpcCommunicationService;
import dev.dtzudontsay.knownworld.simulation.npc.decision.NpcDecisionService;
import dev.dtzudontsay.knownworld.simulation.npc.family.CharacterGenerationService;
import dev.dtzudontsay.knownworld.simulation.npc.family.DynastyService;
import dev.dtzudontsay.knownworld.simulation.npc.family.GenealogyManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageService;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.FertilityService;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.GeneratedNameService;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.LifeCycleService;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.LifeHistoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.PregnancyManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeedManager;
import dev.dtzudontsay.knownworld.simulation.npc.observation.NpcObservationService;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileGenerationService;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousMembershipManager;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineManager;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineService;
import dev.dtzudontsay.knownworld.simulation.persistence.AuthoredIdPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.CampaignCalendarPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.CharacterProfilePersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.ClaimPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.GenealogyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.LifeHistoryPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.MarriagePersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.NpcPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.PregnancyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.SuccessionPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.TitlePersistence;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.authority.AuthorityService;
import dev.dtzudontsay.knownworld.simulation.social.succession.ClaimManager;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionRuleManager;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionService;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
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

    private final CampaignCalendar campaignCalendar;

    private final NpcRegistry registry;

    private final SettlementManager settlementManager;

    private final OrganizationManager organizationManager;

    private final NpcAffiliationManager affiliationManager;

    private final TitleManager titleManager;

    private final SuccessionRuleManager successionRuleManager;

    private final ClaimManager claimManager;

    private final AuthorityService authorityService;

    private final AuthoredIdRegistry authoredIdRegistry;

    private final GenealogyManager genealogyManager;

    private final MarriageManager marriageManager;

    private final DynastyService dynastyService;

    private final LifeHistoryManager lifeHistoryManager;

    private final PregnancyManager pregnancyManager;

    private final CharacterProfileManager profileManager;

    private final CharacterProfileGenerationService profileGenerationService;

    private final NpcRelationshipManager relationshipManager;

    /*
     * Batch 18D.2
     *
     * Runtime religious-order/clergy membership state.
     *
     * Persistence for this manager is added in 18D.3.
     */
    private final ReligiousMembershipManager religiousMembershipManager;

    private final NpcKnowledgeManager knowledgeManager;

    private final NpcMemoryManager memoryManager;

    private final NpcGoalManager goalManager;

    private final NpcNeedManager needManager;

    private final NpcRoutineManager routineManager;

    private final WorldEventManager eventManager;

    private final SuccessionService successionService;

    private final FertilityService fertilityService;

    private final GeneratedNameService generatedNameService;

    private final NpcCommunicationService communicationService;

    private final NpcObservationService observationService;

    private final NpcDecisionService decisionService;

    private final NpcRoutineService routineService;

    private final NpcActionProcessor actionProcessor;

    private final MarriageService marriageService;

    private final CharacterGenerationService characterGenerationService;

    private final LifeCycleService lifeCycleService;

    private final NpcPersistence persistence;

    private final TitlePersistence titlePersistence;

    private final AuthoredIdPersistence authoredIdPersistence;

    private final GenealogyPersistence genealogyPersistence;

    private final MarriagePersistence marriagePersistence;

    private final CampaignCalendarPersistence campaignCalendarPersistence;

    private final LifeHistoryPersistence lifeHistoryPersistence;

    private final PregnancyPersistence pregnancyPersistence;

    private final SuccessionPersistence successionPersistence;

    private final ClaimPersistence claimPersistence;

    private final CharacterProfilePersistence profilePersistence;

    private final NpcActivationManager activationManager;

    private NpcSimulation(
            MinecraftServer server
    ) {
        this.server =
                server;

        this.clock =
                new SimulationClock();

        this.campaignCalendar =
                new CampaignCalendar();

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

        this.successionRuleManager =
                new SuccessionRuleManager(
                        titleManager
                );

        this.claimManager =
                new ClaimManager(
                        registry,
                        titleManager
                );

        this.authorityService =
                new AuthorityService(
                        organizationManager,
                        titleManager
                );

        this.authoredIdRegistry =
                new AuthoredIdRegistry();

        this.genealogyManager =
                new GenealogyManager(
                        registry
                );

        this.marriageManager =
                new MarriageManager(
                        registry
                );

        this.dynastyService =
                new DynastyService(
                        marriageManager,
                        affiliationManager
                );

        this.lifeHistoryManager =
                new LifeHistoryManager(
                        registry
                );

        this.pregnancyManager =
                new PregnancyManager(
                        registry
                );

        this.profileManager =
                new CharacterProfileManager(
                        registry
                );

        this.profileGenerationService =
                new CharacterProfileGenerationService(
                        registry,
                        profileManager
                );

        this.relationshipManager =
                new NpcRelationshipManager(
                        registry
                );

        this.religiousMembershipManager =
                new ReligiousMembershipManager(
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

        this.successionService =
                new SuccessionService(
                        registry,
                        genealogyManager,
                        titleManager,
                        successionRuleManager,
                        claimManager,
                        eventManager
                );

        this.fertilityService =
                new FertilityService(
                        registry,
                        lifeHistoryManager,
                        campaignCalendar
                );

        this.generatedNameService =
                new GeneratedNameService();

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

        this.marriageService =
                new MarriageService(
                        registry,
                        marriageManager,
                        relationshipManager,
                        memoryManager,
                        eventManager
                );

        this.characterGenerationService =
                new CharacterGenerationService(
                        registry,
                        genealogyManager,
                        dynastyService,
                        affiliationManager,
                        relationshipManager,
                        memoryManager,
                        eventManager,
                        profileGenerationService
                );

        this.lifeCycleService =
                new LifeCycleService(
                        registry,
                        campaignCalendar,
                        lifeHistoryManager,
                        fertilityService,
                        pregnancyManager,
                        marriageManager,
                        characterGenerationService,
                        generatedNameService,
                        memoryManager,
                        eventManager,
                        successionService
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

        this.genealogyPersistence =
                new GenealogyPersistence(
                        savePath
                );

        this.marriagePersistence =
                new MarriagePersistence(
                        savePath
                );

        this.campaignCalendarPersistence =
                new CampaignCalendarPersistence(
                        savePath
                );

        this.lifeHistoryPersistence =
                new LifeHistoryPersistence(
                        savePath
                );

        this.pregnancyPersistence =
                new PregnancyPersistence(
                        savePath
                );

        this.successionPersistence =
                new SuccessionPersistence(
                        savePath
                );

        this.claimPersistence =
                new ClaimPersistence(
                        savePath
                );

        this.profilePersistence =
                new CharacterProfilePersistence(
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
                            "NPC simulation started at campaign year {} day {} with {} NPCs, {} profiles, {} genealogy records, {} unions, {} pregnancies, {} claims, {} settlements, {} organizations, {} religious memberships and {} titles.",
                            simulation.campaignCalendar.year(),
                            simulation.campaignCalendar.dayOfYear() + 1,
                            simulation.registry.size(),
                            simulation.profileManager.size(),
                            simulation.genealogyManager.size(),
                            simulation.marriageManager.size(),
                            simulation.pregnancyManager.size(),
                            simulation.claimManager.size(),
                            simulation.settlementManager.size(),
                            simulation.organizationManager.size(),
                            simulation.religiousMembershipManager.all()
                                    .size(),
                            simulation.titleManager.definitionCount()
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

    public CharacterProfileManager profiles() {
        return profileManager;
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

    public SuccessionRuleManager successionRules() {
        return successionRuleManager;
    }

    public SuccessionService succession() {
        return successionService;
    }

    public ClaimManager claims() {
        return claimManager;
    }

    public AuthorityService authority() {
        return authorityService;
    }

    public AuthoredIdRegistry authoredIds() {
        return authoredIdRegistry;
    }

    public GenealogyManager genealogy() {
        return genealogyManager;
    }

    public MarriageManager marriages() {
        return marriageManager;
    }

    public MarriageService marriageService() {
        return marriageService;
    }

    public DynastyService dynasty() {
        return dynastyService;
    }

    public LifeHistoryManager lifeHistory() {
        return lifeHistoryManager;
    }

    public PregnancyManager pregnancies() {
        return pregnancyManager;
    }

    public FertilityService fertility() {
        return fertilityService;
    }

    public LifeCycleService lifeCycle() {
        return lifeCycleService;
    }

    public CampaignCalendar campaignCalendar() {
        return campaignCalendar;
    }

    public CharacterGenerationService characterGeneration() {
        return characterGenerationService;
    }

    public NpcRelationshipManager relationships() {
        return relationshipManager;
    }

    public ReligiousMembershipManager religiousMemberships() {
        return religiousMembershipManager;
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

        boolean newCampaignDay =
                campaignCalendar.advanceTick();

        if (newCampaignDay) {

            lifeCycleService.onNewCampaignDay(
                    clock.tick()
            );
        }

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

    public void advanceCampaignDaysForDebug(
            int days
    ) {

        if (days <= 0) {

            throw new IllegalArgumentException(
                    "days must be positive"
            );
        }

        for (
                int index = 0;
                index < days;
                index++
        ) {

            campaignCalendar.advanceOneDay();

            lifeCycleService.onNewCampaignDay(
                    clock.tick()
            );
        }
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

            genealogyPersistence.save(
                    genealogyManager
            );

            marriagePersistence.save(
                    marriageManager
            );

            campaignCalendarPersistence.save(
                    campaignCalendar
            );

            lifeHistoryPersistence.save(
                    lifeHistoryManager
            );

            pregnancyPersistence.save(
                    pregnancyManager
            );

            successionPersistence.save(
                    successionRuleManager
            );

            claimPersistence.save(
                    claimManager
            );

            profilePersistence.save(
                    profileManager
            );

            /*
             * Religious memberships intentionally become persistent
             * in Batch 18D.3.
             */

        } catch (
                IOException exception
        ) {

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

            genealogyPersistence.loadInto(
                    genealogyManager
            );

            marriagePersistence.loadInto(
                    marriageManager
            );

            campaignCalendarPersistence.loadInto(
                    campaignCalendar
            );

            lifeHistoryPersistence.loadInto(
                    lifeHistoryManager
            );

            pregnancyPersistence.loadInto(
                    pregnancyManager
            );

            successionPersistence.loadInto(
                    successionRuleManager
            );

            claimPersistence.loadInto(
                    claimManager
            );

            profilePersistence.loadInto(
                    profileManager
            );

            if (shouldBootstrapScenario()) {

                KnownWorld.LOGGER.info(
                        "No existing NPC simulation state detected. Bootstrapping default authored scenario."
                );

                ScenarioBootstrapper.bootstrapDefault(
                        this,
                        authoredIdRegistry
                );
            }

            lifeHistoryManager.ensureAll(
                    campaignCalendar.daysPerYear()
            );

            /*
             * Migration path for NPCs created before Batch 17.
             */
            profileManager.ensureAll();

            save();

        } catch (
                IOException exception
        ) {

            KnownWorld.LOGGER.error(
                    "Failed to load NPC simulation.",
                    exception
            );

        } catch (
                RuntimeException exception
        ) {

            KnownWorld.LOGGER.error(
                    "Failed to initialize NPC simulation.",
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
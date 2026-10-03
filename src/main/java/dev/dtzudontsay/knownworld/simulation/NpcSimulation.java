package dev.dtzudontsay.knownworld.simulation;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.bootstrap.AuthoredIdRegistry;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioBootstrapper;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.action.NpcActionProcessor;
import dev.dtzudontsay.knownworld.simulation.npc.communication.NpcCommunicationService;
import dev.dtzudontsay.knownworld.simulation.npc.culture.CultureService;
import dev.dtzudontsay.knownworld.simulation.npc.decision.NpcDecisionService;
import dev.dtzudontsay.knownworld.simulation.npc.family.CharacterGenerationService;
import dev.dtzudontsay.knownworld.simulation.npc.family.DynastyService;
import dev.dtzudontsay.knownworld.simulation.npc.family.GenealogyManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageService;
import dev.dtzudontsay.knownworld.simulation.npc.formation.CharacterFormationIntegrationService;
import dev.dtzudontsay.knownworld.simulation.npc.formation.ChildFormationService;
import dev.dtzudontsay.knownworld.simulation.npc.formation.UpbringingManager;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterLegalStateManager;
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
import dev.dtzudontsay.knownworld.simulation.npc.profile.SkillPracticeService;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.RegionalCharacterInfluenceService;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousInstitutionRuntimeManager;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousMembershipManager;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineManager;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialActionDecisionService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialContextService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialEncounterService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialInteractionService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialReactionService;
import dev.dtzudontsay.knownworld.simulation.persistence.AuthoredIdPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.CampaignCalendarPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.CharacterLegalStatePersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.CharacterProfilePersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.ClaimPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.GenealogyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.LifeHistoryPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.MarriagePersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.NpcPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.PregnancyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.ReligiousMembershipPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.SuccessionPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.TitlePersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.UpbringingPersistence;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.authority.AuthorityService;
import dev.dtzudontsay.knownworld.simulation.social.succession.ClaimManager;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionRuleManager;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionService;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;
import dev.dtzudontsay.knownworld.simulation.testing.AbstractSocialSimulationService;
import dev.dtzudontsay.knownworld.simulation.testing.SimulationFastForwardService;
import dev.dtzudontsay.knownworld.simulation.testing.SimulationReportService;
import dev.dtzudontsay.knownworld.simulation.testing.SyntheticPopulationService;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

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
    private final ReligiousInstitutionRuntimeManager religiousInstitutionManager;
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
    private final CharacterLegalStateManager legalStateManager;
    private final CharacterProfileGenerationService profileGenerationService;
    private final SkillPracticeService skillPracticeService;
    private final NpcRelationshipManager relationshipManager;
    private final ReligiousMembershipManager religiousMembershipManager;
    private final UpbringingManager upbringingManager;
    private final CharacterPsychologyService psychologyService;
    private final RegionalCharacterInfluenceService regionalInfluenceService;
    private final CultureService cultureService;
    private final ReligionService religionService;
    private final CharacterFormationIntegrationService formationIntegrationService;
    private final ChildFormationService formationService;
    private final NpcKnowledgeManager knowledgeManager;
    private final NpcMemoryManager memoryManager;
    private final NpcGoalManager goalManager;
    private final NpcNeedManager needManager;
    private final NpcRoutineManager routineManager;
    private final WorldEventManager eventManager;

    private final SocialContextService socialContextService;
    private final SocialEncounterService socialEncounterService;
    private final SocialReactionService socialReactionService;
    private final SocialActionDecisionService socialActionDecisionService;
    private final SocialInteractionService socialInteractionService;

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

    private final SyntheticPopulationService syntheticPopulationService;
    private final AbstractSocialSimulationService abstractSocialSimulationService;
    private final SimulationFastForwardService fastForwardService;
    private final SimulationReportService simulationReportService;

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
    private final CharacterLegalStatePersistence legalStatePersistence;
    private final ReligiousMembershipPersistence religiousMembershipPersistence;
    private final UpbringingPersistence upbringingPersistence;

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

        this.religiousInstitutionManager =
                new ReligiousInstitutionRuntimeManager(
                        organizationManager
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

        this.legalStateManager =
                new CharacterLegalStateManager(
                        registry
                );

        this.profileGenerationService =
                new CharacterProfileGenerationService(
                        registry,
                        profileManager
                );

        this.skillPracticeService =
                new SkillPracticeService(
                        registry,
                        profileManager
                );

        this.relationshipManager =
                new NpcRelationshipManager(
                        registry
                );

        this.religiousMembershipManager =
                new ReligiousMembershipManager(
                        registry,
                        organizationManager
                );

        this.upbringingManager =
                new UpbringingManager(
                        registry
                );

        this.psychologyService =
                new CharacterPsychologyService(
                        registry,
                        profileManager,
                        relationshipManager
                );

        this.regionalInfluenceService =
                new RegionalCharacterInfluenceService(
                        psychologyService,
                        profileManager
                );

        this.cultureService =
                new CultureService(
                        registry,
                        profileManager,
                        genealogyManager,
                        relationshipManager,
                        psychologyService
                );

        this.religionService =
                new ReligionService(
                        registry,
                        profileManager,
                        relationshipManager,
                        psychologyService
                );

        this.formationIntegrationService =
                new CharacterFormationIntegrationService(
                        registry,
                        profileManager,
                        genealogyManager,
                        lifeHistoryManager,
                        campaignCalendar,
                        upbringingManager,
                        cultureService,
                        religionService
                );

        this.formationService =
                new ChildFormationService(
                        registry,
                        profileManager,
                        genealogyManager,
                        lifeHistoryManager,
                        campaignCalendar,
                        upbringingManager,
                        psychologyService,
                        regionalInfluenceService,
                        cultureService,
                        religionService
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

        /*
         * SIM 05 social topology layer.
         */
        this.socialContextService =
                new SocialContextService(
                        affiliationManager
                );

        this.socialEncounterService =
                new SocialEncounterService(
                        socialContextService,
                        relationshipManager
                );

        this.socialReactionService =
                new SocialReactionService(
                        registry,
                        profileManager,
                        psychologyService,
                        relationshipManager
                );

        this.socialActionDecisionService =
                new SocialActionDecisionService(
                        registry,
                        profileManager,
                        psychologyService,
                        relationshipManager,
                        memoryManager
                );

        this.socialInteractionService =
                new SocialInteractionService(
                        registry,
                        relationshipManager,
                        memoryManager,
                        eventManager,
                        socialReactionService,
                        skillPracticeService
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
                        formationService,
                        generatedNameService,
                        memoryManager,
                        eventManager,
                        successionService
                );

        this.syntheticPopulationService =
                new SyntheticPopulationService(
                        registry,
                        profileManager,
                        lifeHistoryManager,
                        organizationManager,
                        affiliationManager
                );

        this.abstractSocialSimulationService =
                new AbstractSocialSimulationService(
                        registry,
                        socialEncounterService,
                        socialInteractionService,
                        socialActionDecisionService
                );

        this.fastForwardService =
                new SimulationFastForwardService(
                        clock,
                        campaignCalendar,
                        lifeCycleService,
                        abstractSocialSimulationService
                );

        this.simulationReportService =
                new SimulationReportService(
                        registry,
                        relationshipManager,
                        profileManager,
                        socialContextService
                );

        this.activationManager =
                new NpcActivationManager(
                        registry
                );

        Path savePath =
                server.getWorldPath(
                                LevelResource.ROOT
                        )
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

        this.legalStatePersistence =
                new CharacterLegalStatePersistence(
                        savePath
                );

        this.religiousMembershipPersistence =
                new ReligiousMembershipPersistence(
                        savePath
                );

        this.upbringingPersistence =
                new UpbringingPersistence(
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
                            "NPC simulation started at campaign year {} day {} with {} NPCs, {} profiles, {} legal states, {} genealogy records, {} unions, {} pregnancies, {} upbringing records, {} claims, {} settlements, {} organizations, {} religious institutions, {} religious memberships and {} titles.",
                            simulation.campaignCalendar.year(),
                            simulation.campaignCalendar.dayOfYear() + 1,
                            simulation.registry.size(),
                            simulation.profileManager.size(),
                            simulation.legalStateManager.size(),
                            simulation.genealogyManager.size(),
                            simulation.marriageManager.size(),
                            simulation.pregnancyManager.size(),
                            simulation.upbringingManager.size(),
                            simulation.claimManager.size(),
                            simulation.settlementManager.size(),
                            simulation.organizationManager.size(),
                            simulation.religiousInstitutionManager.size(),
                            simulation.religiousMembershipManager.size(),
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

    public CharacterLegalStateManager legalStates() {
        return legalStateManager;
    }

    public SettlementManager settlements() {
        return settlementManager;
    }

    public OrganizationManager organizations() {
        return organizationManager;
    }

    public ReligiousInstitutionRuntimeManager religiousInstitutions() {
        return religiousInstitutionManager;
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

    public CharacterProfileGenerationService profileGeneration() {
        return profileGenerationService;
    }

    public SkillPracticeService skillPractice() {
        return skillPracticeService;
    }

    public NpcRelationshipManager relationships() {
        return relationshipManager;
    }

    public ReligiousMembershipManager religiousMemberships() {
        return religiousMembershipManager;
    }

    public UpbringingManager upbringing() {
        return upbringingManager;
    }

    public CharacterFormationIntegrationService formationIntegration() {
        return formationIntegrationService;
    }

    public ChildFormationService formation() {
        return formationService;
    }

    public CultureService culture() {
        return cultureService;
    }

    public ReligionService religion() {
        return religionService;
    }

    public CharacterPsychologyService psychology() {
        return psychologyService;
    }

    public RegionalCharacterInfluenceService regionalInfluence() {
        return regionalInfluenceService;
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

    public SocialContextService socialContexts() {
        return socialContextService;
    }

    public SocialEncounterService socialEncounters() {
        return socialEncounterService;
    }

    public SocialReactionService socialReactions() {
        return socialReactionService;
    }

    public SocialActionDecisionService socialActionDecisions() {
        return socialActionDecisionService;
    }

    public SocialInteractionService socialInteractions() {
        return socialInteractionService;
    }

    public SyntheticPopulationService syntheticPopulation() {
        return syntheticPopulationService;
    }

    public AbstractSocialSimulationService abstractSocialSimulation() {
        return abstractSocialSimulationService;
    }

    public SimulationFastForwardService fastForward() {
        return fastForwardService;
    }

    public SimulationReportService simulationReports() {
        return simulationReportService;
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

            clock.advanceBy(
                    campaignCalendar.ticksPerCampaignDay()
            );

            campaignCalendar.advanceOneDay();

            lifeCycleService.onNewCampaignDay(
                    clock.tick()
            );
        }
    }

    public void save() {

        try {

            formationIntegrationService.ensureAll();

            legalStateManager.ensureAll(
                    profileManager
            );

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

            legalStatePersistence.save(
                    legalStateManager
            );

            religiousMembershipPersistence.save(
                    religiousInstitutionManager,
                    religiousMembershipManager
            );

            upbringingPersistence.save(
                    upbringingManager
            );

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

            profileManager.ensureAll();

            legalStatePersistence.loadInto(
                    legalStateManager
            );

            legalStateManager.ensureAll(
                    profileManager
            );

            religiousMembershipPersistence.loadInto(
                    religiousInstitutionManager,
                    religiousMembershipManager
            );

            religiousInstitutionManager.ensureAll();

            upbringingPersistence.loadInto(
                    upbringingManager
            );

            formationIntegrationService.ensureAll();

            save();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load NPC simulation.",
                    exception
            );

        } catch (
                RuntimeException exception
        ) {

            throw new IllegalStateException(
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
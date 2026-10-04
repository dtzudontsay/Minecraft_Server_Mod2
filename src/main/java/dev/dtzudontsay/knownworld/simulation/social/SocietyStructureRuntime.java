package dev.dtzudontsay.knownworld.simulation.social;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.debug.DynastyHierarchyDebugCommand;
import dev.dtzudontsay.knownworld.debug.LandedHoldingDebugCommand;
import dev.dtzudontsay.knownworld.debug.PoliticalStructureAuditCommand;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.persistence.CharacterSocialIdentityPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.DynastyAllegiancePersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.DynastyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.LandedHoldingPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.NonDynasticSocietyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.OrganizationMembershipPersistence;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyActivationReport;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyAllegianceManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyHierarchyBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityReport;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityService;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyLeadershipReconciliationService;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyScenarioBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.holding.DeadHoldingHolderReconciliationService;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingManager;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentityBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentityManager;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentityService;
import dev.dtzudontsay.knownworld.simulation.social.integrity.PoliticalStructureIntegrityReport;
import dev.dtzudontsay.knownworld.simulation.social.integrity.PoliticalStructureIntegrityService;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipManager;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyManager;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyService;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;

public final class SocietyStructureRuntime {

    private static SocietyStructureRuntime instance;

    private final MinecraftServer server;

    private final NpcSimulation simulation;

    private final DynastyManager dynasties;

    private final DynastyAllegianceManager dynastyAllegiances;

    private final LandedHoldingManager holdings;

    private final OrganizationMembershipManager memberships;

    private final NonDynasticSocietyManager nonDynasticSocieties;

    private final NonDynasticSocietyService nonDynasticSocietyService;

    private final CharacterSocialIdentityManager characterSocialIdentities;

    private final CharacterSocialIdentityService characterSocialIdentityService;

    private final DynastyLeadershipReconciliationService dynastyLeadershipReconciliation;

    private final DynastyIntegrityService dynastyIntegrity;

    private final PoliticalStructureIntegrityService politicalStructureIntegrity;

    private final DeadHoldingHolderReconciliationService deadHoldingHolderReconciliation;

    private final DynastyPersistence dynastyPersistence;

    private final DynastyAllegiancePersistence dynastyAllegiancePersistence;

    private final LandedHoldingPersistence landedHoldingPersistence;

    private final OrganizationMembershipPersistence membershipPersistence;

    private final NonDynasticSocietyPersistence nonDynasticSocietyPersistence;

    private final CharacterSocialIdentityPersistence characterSocialIdentityPersistence;

    private DynastyActivationReport lastActivationReport;

    private DynastyIntegrityReport lastIntegrityReport;

    private DynastyHierarchyBootstrapService.Report lastHierarchyReport;

    private LandedHoldingBootstrapService.Report lastHoldingReport;

    private PoliticalStructureIntegrityReport lastPoliticalStructureReport;

    private DynastyLeadershipReconciliationService.Report lastDynastyLeadershipReconciliationReport;

    private DeadHoldingHolderReconciliationService.Report lastDeadHolderReconciliationReport;

    private SocietyStructureRuntime(
            MinecraftServer server,
            NpcSimulation simulation
    ) {

        this.server =
                server;

        this.simulation =
                simulation;

        this.dynasties =
                new DynastyManager(
                        simulation.organizations(),
                        simulation.registry()
                );

        this.dynastyAllegiances =
                new DynastyAllegianceManager(
                        dynasties
                );

        this.holdings =
                new LandedHoldingManager(
                        simulation.registry(),
                        simulation.organizations(),
                        simulation.titles(),
                        dynasties,
                        WorldReferenceCatalog.get()
                );

        this.memberships =
                new OrganizationMembershipManager(
                        simulation.registry(),
                        simulation.organizations()
                );

        this.nonDynasticSocieties =
                new NonDynasticSocietyManager(
                        simulation.organizations(),
                        simulation.registry()
                );

        this.nonDynasticSocietyService =
                new NonDynasticSocietyService(
                        nonDynasticSocieties,
                        memberships
                );

        this.characterSocialIdentities =
                new CharacterSocialIdentityManager(
                        simulation.registry(),
                        dynasties,
                        simulation.organizations()
                );

        this.characterSocialIdentityService =
                new CharacterSocialIdentityService(
                        simulation,
                        characterSocialIdentities,
                        dynasties,
                        memberships,
                        nonDynasticSocieties
                );

        this.dynastyLeadershipReconciliation =
                new DynastyLeadershipReconciliationService(
                        simulation,
                        dynasties,
                        characterSocialIdentities,
                        holdings
                );

        this.dynastyIntegrity =
                new DynastyIntegrityService(
                        simulation,
                        dynasties
                );

        this.politicalStructureIntegrity =
                new PoliticalStructureIntegrityService(
                        simulation,
                        dynasties,
                        dynastyAllegiances,
                        holdings,
                        WorldReferenceCatalog.get()
                );

        this.deadHoldingHolderReconciliation =
                new DeadHoldingHolderReconciliationService(
                        simulation,
                        dynasties,
                        holdings
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

        this.dynastyPersistence =
                new DynastyPersistence(
                        savePath
                );

        this.dynastyAllegiancePersistence =
                new DynastyAllegiancePersistence(
                        savePath
                );

        this.landedHoldingPersistence =
                new LandedHoldingPersistence(
                        savePath
                );

        this.membershipPersistence =
                new OrganizationMembershipPersistence(
                        savePath
                );

        this.nonDynasticSocietyPersistence =
                new NonDynasticSocietyPersistence(
                        savePath
                );

        this.characterSocialIdentityPersistence =
                new CharacterSocialIdentityPersistence(
                        savePath
                );
    }

    public static void registerLifecycle() {

        LandedHoldingDebugCommand.register();

        DynastyHierarchyDebugCommand.register();

        PoliticalStructureAuditCommand.register();

        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> {

                    NpcSimulation simulation =
                            NpcSimulation.getNullable();

                    if (simulation == null) {

                        throw new IllegalStateException(
                                "Cannot initialize society structures before NPC simulation"
                        );
                    }

                    SocietyStructureRuntime runtime =
                            new SocietyStructureRuntime(
                                    server,
                                    simulation
                            );

                    instance =
                            runtime;

                    runtime.loadAndReconcile();

                    KnownWorld.LOGGER.info(
                            "Society structure runtime started with {} dynasties, {} landed holdings, {} current allegiance overrides, {} non-dynastic societies, {} character social identities and {} organization memberships.",
                            runtime.dynasties.size(),
                            runtime.holdings.size(),
                            runtime.dynastyAllegiances.overrideCount(),
                            runtime.nonDynasticSocieties.size(),
                            runtime.characterSocialIdentities.size(),
                            runtime.memberships.size()
                    );
                }
        );

        ServerLifecycleEvents.BEFORE_SAVE.register(
                (
                        server,
                        flush,
                        force
                ) -> {

                    SocietyStructureRuntime runtime =
                            instance;

                    if (runtime != null
                            && runtime.server
                            == server) {

                        runtime.save();
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPING.register(
                server -> {

                    SocietyStructureRuntime runtime =
                            instance;

                    if (runtime != null
                            && runtime.server
                            == server) {

                        runtime.save();
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

    public static SocietyStructureRuntime get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Society structure runtime is not running"
            );
        }

        return instance;
    }

    public static SocietyStructureRuntime getNullable() {

        return instance;
    }

    public DynastyManager dynasties() {
        return dynasties;
    }

    public DynastyAllegianceManager dynastyAllegiances() {
        return dynastyAllegiances;
    }

    public LandedHoldingManager holdings() {
        return holdings;
    }

    public OrganizationMembershipManager memberships() {
        return memberships;
    }

    public NonDynasticSocietyManager nonDynasticSocieties() {
        return nonDynasticSocieties;
    }

    public NonDynasticSocietyService nonDynasticSocietyService() {
        return nonDynasticSocietyService;
    }

    public CharacterSocialIdentityManager characterSocialIdentities() {
        return characterSocialIdentities;
    }

    public CharacterSocialIdentityService characterSocialIdentityService() {
        return characterSocialIdentityService;
    }

    public DynastyActivationReport lastActivationReport() {
        return lastActivationReport;
    }

    public DynastyIntegrityReport lastIntegrityReport() {
        return lastIntegrityReport;
    }

    public DynastyHierarchyBootstrapService.Report lastHierarchyReport() {
        return lastHierarchyReport;
    }

    public LandedHoldingBootstrapService.Report lastHoldingReport() {
        return lastHoldingReport;
    }

    public PoliticalStructureIntegrityReport lastPoliticalStructureReport() {
        return lastPoliticalStructureReport;
    }

    public DynastyLeadershipReconciliationService.Report lastDynastyLeadershipReconciliationReport() {
        return lastDynastyLeadershipReconciliationReport;
    }

    public DeadHoldingHolderReconciliationService.Report lastDeadHolderReconciliationReport() {
        return lastDeadHolderReconciliationReport;
    }

    public DynastyIntegrityReport auditDynasties() {

        lastIntegrityReport =
                dynastyIntegrity.audit();

        return lastIntegrityReport;
    }

    public PoliticalStructureIntegrityReport auditPoliticalStructure() {

        lastPoliticalStructureReport =
                politicalStructureIntegrity.audit();

        return lastPoliticalStructureReport;
    }

    /**
     * Initializes society-side state for an NPC that has just been created
     * during runtime.
     *
     * The creation pipeline must call this only after basic genealogy and
     * affiliation data have been written. That allows identity inference to
     * obtain the correct:
     *
     * - birth dynasty;
     * - current dynasty;
     * - legal family dynasty;
     * - legal parents;
     * - household;
     * - house organization;
     * - primary allegiance;
     * - generic organization memberships.
     *
     * This is intentionally incremental. We do not run a world-wide
     * reconciliation merely because one child was born.
     *
     * That matters once the simulation contains tens or hundreds of
     * thousands of NPCs.
     */
    public void onNpcCreated(
            NpcId npc
    ) {

        characterSocialIdentityService.ensureIdentity(
                npc
        );
    }

    /**
     * Full reconciliation of society-side projections.
     *
     * Safe to call repeatedly.
     */
    public void reconcile() {

        memberships.ensurePrimaryAffiliations(
                simulation.affiliations(),
                simulation.serverTickCounter()
        );

        characterSocialIdentityService.ensureAll();

        lastDynastyLeadershipReconciliationReport =
                dynastyLeadershipReconciliation.reconcileAll(
                        simulation.serverTickCounter()
                );

        lastDeadHolderReconciliationReport =
                deadHoldingHolderReconciliation.reconcileAll();
    }

    /**
     * Immediate death-time bridge from the NPC lifecycle into political
     * state.
     */
    public void onNpcDeath(
            NpcId deceased,
            long tick
    ) {

        lastDynastyLeadershipReconciliationReport =
                dynastyLeadershipReconciliation.handleDeath(
                        deceased,
                        tick
                );

        lastDeadHolderReconciliationReport =
                deadHoldingHolderReconciliation.reconcileAll();

        if (lastDynastyLeadershipReconciliationReport.changedAnything()) {

            KnownWorld.LOGGER.info(
                    "Death-time dynasty reconciliation for NPC {}: examined={}, deadHeads={}, headsAdvanced={}, storedHeir={}, titleLaw={}, vacant={}, deadHeirsCleared={}, heirsRecomputed={}, events={}.",
                    deceased,
                    lastDynastyLeadershipReconciliationReport.dynastiesExamined(),
                    lastDynastyLeadershipReconciliationReport.deadHeadsFound(),
                    lastDynastyLeadershipReconciliationReport.headsAdvanced(),
                    lastDynastyLeadershipReconciliationReport.advancedFromStoredHeir(),
                    lastDynastyLeadershipReconciliationReport.advancedFromLinkedTitleLaw(),
                    lastDynastyLeadershipReconciliationReport.headsVacated(),
                    lastDynastyLeadershipReconciliationReport.deadHeirsCleared(),
                    lastDynastyLeadershipReconciliationReport.heirsRecomputed(),
                    lastDynastyLeadershipReconciliationReport.eventsCreated()
            );
        }
    }

    private void loadAndReconcile() {

        try {

            dynastyPersistence.loadInto(
                    dynasties
            );

            lastActivationReport =
                    DynastyScenarioBootstrapService.ensureDefault(
                            simulation,
                            dynasties
                    );

            lastHierarchyReport =
                    DynastyHierarchyBootstrapService.apply(
                            dynasties
                    );

            dynastyAllegiancePersistence.loadInto(
                    dynastyAllegiances
            );

            landedHoldingPersistence.loadInto(
                    holdings
            );

            lastHoldingReport =
                    LandedHoldingBootstrapService.ensureDefault(
                            simulation,
                            dynasties,
                            holdings
                    );

            nonDynasticSocietyPersistence.loadInto(
                    nonDynasticSocieties
            );

            membershipPersistence.loadInto(
                    memberships
            );

            NonDynasticSocietyBootstrapService.ensureDefault(
                    simulation,
                    nonDynasticSocieties,
                    memberships
            );

            characterSocialIdentityPersistence.loadInto(
                    characterSocialIdentities
            );

            CharacterSocialIdentityBootstrapService.ensureDefault(
                    simulation,
                    characterSocialIdentityService,
                    characterSocialIdentities,
                    dynasties,
                    memberships
            );

            /*
             * Existing saves may predate one or more runtime projection
             * systems. Reconciliation repairs those projections before
             * strict integrity validation.
             */
            reconcile();

            if (lastDynastyLeadershipReconciliationReport != null
                    && lastDynastyLeadershipReconciliationReport.changedAnything()) {

                KnownWorld.LOGGER.info(
                        "Dynasty leadership reconciliation: examined={}, deadHeads={}, headsAdvanced={}, storedHeir={}, titleLaw={}, vacant={}, deadHeirsCleared={}, heirsRecomputed={}, events={}.",
                        lastDynastyLeadershipReconciliationReport.dynastiesExamined(),
                        lastDynastyLeadershipReconciliationReport.deadHeadsFound(),
                        lastDynastyLeadershipReconciliationReport.headsAdvanced(),
                        lastDynastyLeadershipReconciliationReport.advancedFromStoredHeir(),
                        lastDynastyLeadershipReconciliationReport.advancedFromLinkedTitleLaw(),
                        lastDynastyLeadershipReconciliationReport.headsVacated(),
                        lastDynastyLeadershipReconciliationReport.deadHeirsCleared(),
                        lastDynastyLeadershipReconciliationReport.heirsRecomputed(),
                        lastDynastyLeadershipReconciliationReport.eventsCreated()
                );
            }

            if (lastDeadHolderReconciliationReport != null
                    && lastDeadHolderReconciliationReport.changedAnything()) {

                KnownWorld.LOGGER.info(
                        "Dead holding-holder reconciliation: activeExamined={}, deadFound={}, resolved={}, byTitle={}, byDynastyHeir={}, byDynastyHead={}, vacant={}, titleTransfers={}.",
                        lastDeadHolderReconciliationReport.activeHoldingsExamined(),
                        lastDeadHolderReconciliationReport.deadHoldersFound(),
                        lastDeadHolderReconciliationReport.resolved(),
                        lastDeadHolderReconciliationReport.resolvedFromLinkedTitle(),
                        lastDeadHolderReconciliationReport.resolvedFromDynastyHeir(),
                        lastDeadHolderReconciliationReport.resolvedFromDynastyHead(),
                        lastDeadHolderReconciliationReport.madeVacant(),
                        lastDeadHolderReconciliationReport.linkedTitleTransfers()
                );
            }

            lastIntegrityReport =
                    dynastyIntegrity.auditStrict();

            KnownWorld.LOGGER.info(
                    "Dynasty integrity audit passed: total={}, active={}, inactive={}, warnings={}.",
                    lastIntegrityReport.totalDynasties(),
                    lastIntegrityReport.activeAtScenarioStart(),
                    lastIntegrityReport.inactiveAtScenarioStart(),
                    lastIntegrityReport.warningCount()
            );

            lastPoliticalStructureReport =
                    politicalStructureIntegrity.auditStrict();

            KnownWorld.LOGGER.info(
                    "Political structure integrity audit passed: dynasties={}, holdings={}, allegianceOverrides={}, warnings={}, info={}.",
                    lastPoliticalStructureReport.dynastyCount(),
                    lastPoliticalStructureReport.holdingCount(),
                    lastPoliticalStructureReport.allegianceOverrideCount(),
                    lastPoliticalStructureReport.warningCount(),
                    lastPoliticalStructureReport.infoCount()
            );

            /*
             * Persist repaired migration state immediately.
             */
            simulation.save();

            save();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to initialize society structure runtime",
                    exception
            );
        }
    }

    private void save() {

        try {

            /*
             * Permanent safety boundary.
             */
            reconcile();

            dynastyPersistence.save(
                    dynasties
            );

            dynastyAllegiancePersistence.save(
                    dynastyAllegiances
            );

            landedHoldingPersistence.save(
                    holdings
            );

            membershipPersistence.save(
                    memberships
            );

            nonDynasticSocietyPersistence.save(
                    nonDynasticSocieties
            );

            characterSocialIdentityPersistence.save(
                    characterSocialIdentities
            );

        } catch (
                IOException exception
        ) {

            KnownWorld.LOGGER.error(
                    "Failed to save society structure runtime.",
                    exception
            );
        }
    }
}
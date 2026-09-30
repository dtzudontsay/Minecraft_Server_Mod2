package dev.dtzudontsay.knownworld.simulation.social;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.debug.LandedHoldingDebugCommand;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.persistence.CharacterSocialIdentityPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.DynastyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.LandedHoldingPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.NonDynasticSocietyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.OrganizationMembershipPersistence;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyActivationReport;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityReport;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityService;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyScenarioBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingManager;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentityBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentityManager;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentityService;
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

    private final LandedHoldingManager holdings;

    private final OrganizationMembershipManager memberships;

    private final NonDynasticSocietyManager nonDynasticSocieties;

    private final NonDynasticSocietyService nonDynasticSocietyService;

    private final CharacterSocialIdentityManager characterSocialIdentities;

    private final CharacterSocialIdentityService characterSocialIdentityService;

    private final DynastyIntegrityService dynastyIntegrity;

    private final DynastyPersistence dynastyPersistence;

    private final LandedHoldingPersistence landedHoldingPersistence;

    private final OrganizationMembershipPersistence membershipPersistence;

    private final NonDynasticSocietyPersistence nonDynasticSocietyPersistence;

    private final CharacterSocialIdentityPersistence characterSocialIdentityPersistence;

    private DynastyActivationReport lastActivationReport;

    private DynastyIntegrityReport lastIntegrityReport;

    private LandedHoldingBootstrapService.Report lastHoldingReport;

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

        this.dynastyIntegrity =
                new DynastyIntegrityService(
                        simulation,
                        dynasties
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

        /*
         * registerLifecycle() itself is called during mod initialization,
         * so command registration is still early enough here.
         */
        LandedHoldingDebugCommand.register();

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
                            "Society structure runtime started with {} dynasties, {} landed holdings, {} non-dynastic societies, {} character social identities and {} organization memberships.",
                            runtime.dynasties.size(),
                            runtime.holdings.size(),
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

    public LandedHoldingBootstrapService.Report lastHoldingReport() {
        return lastHoldingReport;
    }

    public DynastyIntegrityReport auditDynasties() {

        lastIntegrityReport =
                dynastyIntegrity.audit();

        return lastIntegrityReport;
    }

    public void reconcile() {

        memberships.ensurePrimaryAffiliations(
                simulation.affiliations(),
                simulation.serverTickCounter()
        );

        characterSocialIdentityService.ensureAll();
    }

    private void loadAndReconcile() {

        try {

            /*
             * -----------------------------------------------------
             * DYNASTIES
             * -----------------------------------------------------
             */

            dynastyPersistence.loadInto(
                    dynasties
            );

            lastActivationReport =
                    DynastyScenarioBootstrapService.ensureDefault(
                            simulation,
                            dynasties
                    );

            /*
             * -----------------------------------------------------
             * LANDED HOLDINGS
             * -----------------------------------------------------
             *
             * Holdings depend on:
             * - loaded NPCs
             * - organizations/titles
             * - reconciled dynasties
             */
            landedHoldingPersistence.loadInto(
                    holdings
            );

            lastHoldingReport =
                    LandedHoldingBootstrapService.ensureDefault(
                            simulation,
                            dynasties,
                            holdings
                    );

            /*
             * -----------------------------------------------------
             * NON-DYNASTIC SOCIETIES
             * -----------------------------------------------------
             */

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

            /*
             * -----------------------------------------------------
             * CHARACTER SOCIAL IDENTITY
             * -----------------------------------------------------
             */

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

            reconcile();

            lastIntegrityReport =
                    dynastyIntegrity.auditStrict();

            KnownWorld.LOGGER.info(
                    "Dynasty integrity audit passed: total={}, active={}, inactive={}, warnings={}.",
                    lastIntegrityReport.totalDynasties(),
                    lastIntegrityReport.activeAtScenarioStart(),
                    lastIntegrityReport.inactiveAtScenarioStart(),
                    lastIntegrityReport.warningCount()
            );

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

            reconcile();

            dynastyPersistence.save(
                    dynasties
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
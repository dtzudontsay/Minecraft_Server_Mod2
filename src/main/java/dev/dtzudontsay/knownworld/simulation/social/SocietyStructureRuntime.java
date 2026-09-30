package dev.dtzudontsay.knownworld.simulation.social;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.persistence.DynastyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.NonDynasticSocietyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.OrganizationMembershipPersistence;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyActivationReport;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityReport;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityService;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyScenarioBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipManager;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyManager;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;

public final class SocietyStructureRuntime {

    private static SocietyStructureRuntime instance;

    private final MinecraftServer server;

    private final NpcSimulation simulation;

    private final DynastyManager dynasties;

    private final OrganizationMembershipManager memberships;

    private final NonDynasticSocietyManager nonDynasticSocieties;

    private final NonDynasticSocietyService nonDynasticSocietyService;

    private final DynastyIntegrityService dynastyIntegrity;

    private final DynastyPersistence dynastyPersistence;

    private final OrganizationMembershipPersistence membershipPersistence;

    private final NonDynasticSocietyPersistence nonDynasticSocietyPersistence;

    private DynastyActivationReport lastActivationReport;

    private DynastyIntegrityReport lastIntegrityReport;

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

        this.dynastyIntegrity =
                new DynastyIntegrityService(
                        simulation,
                        dynasties
                );

        Path savePath =
                server.getServerDirectory()
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

        this.membershipPersistence =
                new OrganizationMembershipPersistence(
                        savePath
                );

        this.nonDynasticSocietyPersistence =
                new NonDynasticSocietyPersistence(
                        savePath
                );
    }

    public static void registerLifecycle() {

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
                            "Society structure runtime started with {} dynasties, {} non-dynastic societies and {} organization memberships.",
                            runtime.dynasties.size(),
                            runtime.nonDynasticSocieties.size(),
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

    public OrganizationMembershipManager memberships() {
        return memberships;
    }

    public NonDynasticSocietyManager nonDynasticSocieties() {
        return nonDynasticSocieties;
    }

    public NonDynasticSocietyService nonDynasticSocietyService() {
        return nonDynasticSocietyService;
    }

    public DynastyActivationReport lastActivationReport() {
        return lastActivationReport;
    }

    public DynastyIntegrityReport lastIntegrityReport() {
        return lastIntegrityReport;
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

            /*
             * Organizations already belong to the main NPC simulation
             * and therefore exist before society metadata is loaded.
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

            membershipPersistence.save(
                    memberships
            );

            nonDynasticSocietyPersistence.save(
                    nonDynasticSocieties
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
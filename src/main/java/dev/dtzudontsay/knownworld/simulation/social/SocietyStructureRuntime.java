package dev.dtzudontsay.knownworld.simulation.social;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.persistence.DynastyPersistence;
import dev.dtzudontsay.knownworld.simulation.persistence.OrganizationMembershipPersistence;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyActivationReport;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityReport;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityService;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyScenarioBootstrapService;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipManager;
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

    private final DynastyIntegrityService dynastyIntegrity;

    private final DynastyPersistence dynastyPersistence;

    private final OrganizationMembershipPersistence membershipPersistence;

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
    }

    public static void registerLifecycle() {

        /*
         * Must be registered after NpcSimulation.registerLifecycle().
         */
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
                            "Society structure runtime started with {} dynasties and {} organization memberships.",
                            runtime.dynasties.size(),
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

            /*
             * 1. Load previously persisted dynasty runtime state.
             */
            dynastyPersistence.loadInto(
                    dynasties
            );

            /*
             * 2. Reconcile persistent authored dynasties against the
             * current complete reference catalog and perform 298 AC
             * activation.
             */
            lastActivationReport =
                    DynastyScenarioBootstrapService.ensureDefault(
                            simulation,
                            dynasties
                    );

            /*
             * 3. Load generic organization memberships after all
             * dynasty organizations are guaranteed to exist.
             */
            membershipPersistence.loadInto(
                    memberships
            );

            /*
             * 4. Mirror old primary affiliations into the generic
             * organization membership system.
             */
            reconcile();

            /*
             * 5. Strict structural dynasty audit.
             *
             * Warnings are allowed.
             * Structural errors stop initialization.
             */
            lastIntegrityReport =
                    dynastyIntegrity.auditStrict();

            KnownWorld.LOGGER.info(
                    "Dynasty integrity audit passed: total={}, authored={}, generated={}, active={}, inactive={}, organizations={}, extinct={}, exiled={}, notYetFounded={}, noHead={}, noHeir={}, warnings={}.",
                    lastIntegrityReport.totalDynasties(),
                    lastIntegrityReport.authoredDynasties(),
                    lastIntegrityReport.generatedDynasties(),
                    lastIntegrityReport.activeAtScenarioStart(),
                    lastIntegrityReport.inactiveAtScenarioStart(),
                    lastIntegrityReport.activeRuntimeOrganizations(),
                    lastIntegrityReport.extinctDynasties(),
                    lastIntegrityReport.exiledDynasties(),
                    lastIntegrityReport.notYetFoundedDynasties(),
                    lastIntegrityReport.dynastiesWithoutHeads(),
                    lastIntegrityReport.dynastiesWithoutHeirs(),
                    lastIntegrityReport.warningCount()
            );

            /*
             * Dynasty bootstrap may create new organizations and authored
             * organization mappings. Persist those through the main NPC
             * simulation.
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

            reconcile();

            dynastyPersistence.save(
                    dynasties
            );

            membershipPersistence.save(
                    memberships
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
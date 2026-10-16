
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.time.CampaignDayObserverHub;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class CampaignAgricultureDayOperationTest {

    @TempDir
    Path directory;

    private static final HoldingId FIRST_HOLDING =
            new HoldingId(1L);

    private static final HoldingId SECOND_HOLDING =
            new HoldingId(2L);

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private static final class Fixture {

        final Path directory;

        final ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        final NpcRegistry npcs =
                new NpcRegistry();

        final SettlementManager settlements =
                new SettlementManager();

        final OrganizationManager organizations =
                new OrganizationManager(settlements);

        final NpcAffiliationManager affiliations =
                new NpcAffiliationManager(
                        npcs,
                        settlements,
                        organizations
                );

        final SettlementId firstSettlement;
        final SettlementId secondSettlement;

        final InventoryId firstFarm;
        final InventoryId secondFarm;

        final SettlementFoodScenarioAdapter adapter;
        final EconomyReferenceValidator validator;
        final AgriculturalIntervalLedger ledger;

        final List<AgriculturalDayEnrollment> enrollments;

        Fixture(
                Path directory,
                boolean includeSecondSettlement
        ) throws IOException {

            this.directory = directory;

            firstSettlement = makeSettlement(
                    "First Settlement"
            );

            Organization firstHousehold =
                    makeHousehold(
                            "First Household",
                            firstSettlement
                    );

            addResident(
                    firstSettlement,
                    firstHousehold
            );

            if (includeSecondSettlement) {

                secondSettlement = makeSettlement(
                        "Second Settlement"
                );

                Organization secondHousehold =
                        makeHousehold(
                                "Second Household",
                                secondSettlement
                        );

                addResident(
                        secondSettlement,
                        secondHousehold
                );

            } else {
                secondSettlement = null;
            }

            InventoryManager initial =
                    new InventoryManager(catalog);

            firstFarm = initial.create(
                    EconomicOwner.holding(FIRST_HOLDING),
                    firstSettlement
            ).id();

            initial.create(
                    EconomicOwner.organization(
                            firstHousehold.id()
                    ),
                    firstSettlement
            );

            List<AgriculturalDayEnrollment> prepared =
                    new ArrayList<>();

            prepared.add(
                    enrollment(
                            FIRST_HOLDING,
                            firstFarm,
                            firstSettlement
                    )
            );

            if (includeSecondSettlement) {

                Organization secondHousehold =
                        organizations.all()
                                .stream()
                                .filter(o ->
                                        o.type()
                                                == OrganizationType.HOUSEHOLD
                                                && secondSettlement.equals(
                                                o.seatSettlement()
                                        )
                                )
                                .findFirst()
                                .orElseThrow();

                secondFarm = initial.create(
                        EconomicOwner.holding(
                                SECOND_HOLDING
                        ),
                        secondSettlement
                ).id();

                initial.create(
                        EconomicOwner.organization(
                                secondHousehold.id()
                        ),
                        secondSettlement
                );

                prepared.add(
                        enrollment(
                                SECOND_HOLDING,
                                secondFarm,
                                secondSettlement
                        )
                );

            } else {
                secondFarm = null;
            }

            enrollments = List.copyOf(prepared);

            /*
             * Real snapshot-primary authority is
             * prepared through the existing matching
             * legacy/snapshot activation protocol.
             */
            new InventoryPersistence(directory)
                    .save(initial);

            new EconomySnapshotStore(directory)
                    .write(
                            EconomySnapshotStore.capture(
                                    initial
                            )
                    );

            EconomyAuthorityService.activateMatchingSnapshot(
                    directory,
                    catalog
            );

            adapter = new SettlementFoodScenarioAdapter(
                    npcs,
                    affiliations,
                    organizations,
                    settlements,
                    id -> FIRST_HOLDING.equals(id)
                            || SECOND_HOLDING.equals(id)
            );

            validator = new EconomyReferenceValidator(
                    npcs::contains,
                    id -> settlements.find(id).isPresent(),
                    id -> organizations.find(id).isPresent(),
                    id -> FIRST_HOLDING.equals(id)
                            || SECOND_HOLDING.equals(id)
            );

            ledger = new AgriculturalIntervalLedger(
                    directory
            );

            /*
             * Explicit test-only initialization.
             * Production runtime never does this
             * automatically.
             */
            ledger.initialize();
        }

        private SettlementId makeSettlement(String name) {
            return settlements.create(
                    name,
                    SettlementType.CASTLE,
                    new SimulationPosition(
                            "minecraft:overworld",
                            0.0,
                            64.0,
                            0.0
                    )
            ).id();
        }

        private Organization makeHousehold(
                String name,
                SettlementId settlement
        ) {
            return organizations.create(
                    name,
                    OrganizationType.HOUSEHOLD,
                    settlement
            );
        }

        void addResident(
                SettlementId settlement,
                Organization household
        ) {
            var npc = npcs.create(
                    "Test",
                    "Resident",
                    NpcSex.MALE,
                    280,
                    new SimulationPosition(
                            "minecraft:overworld",
                            0.0,
                            64.0,
                            0.0
                    )
            );

            affiliations.setResidence(
                    npc.id(),
                    settlement
            );

            if (household != null) {
                affiliations.setHousehold(
                        npc.id(),
                        household.id()
                );
            }
        }

        private AgriculturalDayEnrollment enrollment(
                HoldingId holding,
                InventoryId farm,
                SettlementId settlement
        ) {
            return new AgriculturalDayEnrollment(
                    holding,
                    farm,
                    settlement,
                    30L,
                    new AgriculturalYieldModel.Inputs(
                            1L,
                            1L,
                            100L,
                            100L,
                            1000,
                            1000,
                            1000
                    ),
                    0L,
                    1000L
            );
        }

        EconomyAuthorityService.Loaded load()
                throws IOException {
            return EconomyAuthorityService.load(
                    directory,
                    catalog
            );
        }

        CampaignAgricultureDayOperation operation()
                throws IOException {
            return operation(enrollments);
        }

        CampaignAgricultureDayOperation operation(
                List<AgriculturalDayEnrollment> farms
        ) throws IOException {
            return operation(load(), farms);
        }

        CampaignAgricultureDayOperation operation(
                EconomyAuthorityService.Loaded loaded,
                List<AgriculturalDayEnrollment> farms
        ) {
            SettlementAgricultureRunner runner =
                    new SettlementAgricultureRunner(
                            directory,
                            catalog,
                            loaded,
                            validator,
                            adapter,
                            ledger
                    );

            return new CampaignAgricultureDayOperation(
                    loaded,
                    adapter,
                    ledger,
                    runner,
                    farms
            );
        }

        long grainAt(InventoryId inventory)
                throws IOException {
            return load()
                    .manager()
                    .require(inventory)
                    .quantity(GRAIN);
        }

        EconomySnapshotStore.Snapshot snapshot()
                throws IOException {
            return new EconomySnapshotStore(directory)
                    .read(catalog);
        }
    }

    private SimulationTimeContract day(
            long absoluteDay
    ) {
        CampaignCalendar calendar =
                new CampaignCalendar(
                        0, 0, 365, 3
                );

        calendar.restore(
                absoluteDay,
                0,
                3
        );

        return SimulationTimeContract.capture(
                new SimulationClock(0L),
                calendar
        );
    }

    /**
     * Exercise normal tick-style advancement.
     *
     * The clock and calendar advance separately,
     * just as in NpcSimulation's established loop.
     */
    private void advanceLiveStyle(
            CampaignAgricultureDayCoordinator coordinator,
            int days
    ) {
        CampaignCalendar calendar =
                new CampaignCalendar(
                        0, 29, 365, 3
                );

        SimulationClock clock =
                new SimulationClock();

        for (int tick = 0; tick < days * 3; tick++) {

            clock.advance();

            calendar.advanceTick();

            coordinator.observe(
                    SimulationTimeContract.capture(
                            clock,
                            calendar
                    )
            );
        }
    }

    /**
     * Exercise accelerated campaign-day advancement
     * using the Batch 22.10 observer contract.
     *
     * This is a controlled accelerated-style driver,
     * not a full LifeCycleService/social fast-forward.
     */
    private void advanceAcceleratedStyle(
            CampaignAgricultureDayCoordinator coordinator,
            int days
    ) {
        CampaignCalendar calendar =
                new CampaignCalendar(
                        0, 29, 365, 3
                );

        SimulationClock clock =
                new SimulationClock();

        CampaignDayObserverHub observers =
                new CampaignDayObserverHub();

        observers.register(
                time -> coordinator.observe(time)
        );

        for (int i = 0; i < days; i++) {

            clock.advanceBy(
                    calendar.ticksPerCampaignDay()
            );

            calendar.advanceOneDay();

            observers.publish(
                    clock,
                    calendar
            );
        }
    }

    @Test
    void singleDayExecutesRealHarvestAndConsumption()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        var operation = f.operation();

        advanceLiveStyle(
                operation.strictCoordinator(29L),
                1
        );

        assertEquals(
                99L,
                f.grainAt(f.firstFarm)
        );

        assertEquals(
                30L,
                f.ledger.progress(FIRST_HOLDING)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertNotNull(operation.lastResult());

        assertEquals(
                100L,
                operation.lastResult()
                        .farms().get(0)
                        .food().harvestedKg()
        );
    }

    @Test
    void followingDayConsumesWithoutRepeatedHarvest()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        var operation = f.operation();

        advanceLiveStyle(
                operation.strictCoordinator(29L),
                2
        );

        assertEquals(
                98L,
                f.grainAt(f.firstFarm)
        );

        assertEquals(
                31L,
                f.ledger.progress(FIRST_HOLDING)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertEquals(
                0L,
                operation.lastResult()
                        .farms().get(0)
                        .food().harvestedKg()
        );
    }

    @Test
    void liveAndAcceleratedStyleHaveIdenticalSnapshots()
            throws IOException {

        Fixture live = new Fixture(
                directory.resolve("live"),
                false
        );

        Fixture accelerated = new Fixture(
                directory.resolve("accelerated"),
                false
        );

        advanceLiveStyle(
                live.operation().strictCoordinator(29L),
                2
        );

        advanceAcceleratedStyle(
                accelerated.operation()
                        .strictCoordinator(29L),
                2
        );

        assertEquals(
                live.snapshot(),
                accelerated.snapshot()
        );

        assertEquals(
                live.ledger.progress(FIRST_HOLDING),
                accelerated.ledger.progress(FIRST_HOLDING)
        );
    }

    @Test
    void readinessIsReadOnly()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        var operation = f.operation();

        var readiness = operation.inspect(
                29L, 30L
        );

        assertEquals(1, readiness.farms().size());

        assertEquals(
                1L,
                readiness.farms().get(0)
                        .representedResidents()
        );

        assertEquals(
                0L,
                f.grainAt(f.firstFarm)
        );

        assertTrue(
                f.ledger.progress(FIRST_HOLDING)
                        .isEmpty()
        );
    }

    @Test
    void repeatedDayObservationDoesNotDuplicateHarvest()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        var operation = f.operation();

        var coordinator =
                operation.strictCoordinator(29L);

        coordinator.observe(day(30L));

        assertEquals(
                CampaignAgricultureDayCoordinator.Observation
                        .SAME_DAY,
                coordinator.observe(day(30L))
        );

        assertEquals(1L, coordinator.executedDays());
        assertEquals(99L, f.grainAt(f.firstFarm));
    }

    @Test
    void staleRestartCannotRepeatCommittedInterval()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        f.operation()
                .strictCoordinator(29L)
                .observe(day(30L));

        var stale =
                f.operation().strictCoordinator(29L);

        assertThrows(
                UncheckedIOException.class,
                () -> stale.observe(day(30L))
        );

        assertTrue(stale.blocked());
        assertEquals(99L, f.grainAt(f.firstFarm));
    }

    @Test
    void missingLedgerRejectsExecution()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        Files.delete(f.ledger.file());

        assertThrows(
                IOException.class,
                () -> f.operation().inspect(
                        29L, 30L
                )
        );

        assertEquals(0L, f.grainAt(f.firstFarm));
    }

    @Test
    void pendingIntervalRejectsAutomaticReplay()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        f.ledger.begin(
                FIRST_HOLDING,
                new AgriculturalInterval(
                        29L, 30L, 30L
                )
        );

        assertThrows(
                IOException.class,
                () -> f.operation().inspect(
                        29L, 30L
                )
        );

        assertEquals(0L, f.grainAt(f.firstFarm));
    }

    @Test
    void unassignedResidentBlocksDayBeforeMutation()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        f.addResident(
                f.firstSettlement,
                null
        );

        var operation = f.operation();

        assertThrows(
                IllegalStateException.class,
                () -> operation.inspect(
                        29L, 30L
                )
        );

        assertEquals(0L, f.grainAt(f.firstFarm));

        assertTrue(
                f.ledger.progress(FIRST_HOLDING)
                        .isEmpty()
        );
    }

    @Test
    void twoFarmsCannotDoubleChargeOneSettlement()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        AgriculturalDayEnrollment fakeSecond =
                new AgriculturalDayEnrollment(
                        SECOND_HOLDING,
                        new InventoryId(999L),
                        f.firstSettlement,
                        30L,
                        f.enrollments.get(0)
                                .harvestInputs(),
                        0L,
                        1000L
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> f.operation(
                        List.of(
                                f.enrollments.get(0),
                                fakeSecond
                        )
                )
        );
    }

    @Test
    void differentSettlementsCanBothProduce()
            throws IOException {

        Fixture f = new Fixture(
                directory, true
        );

        var operation = f.operation();

        advanceAcceleratedStyle(
                operation.strictCoordinator(29L),
                1
        );

        assertEquals(99L, f.grainAt(f.firstFarm));
        assertEquals(99L, f.grainAt(f.secondFarm));

        assertEquals(
                2,
                operation.lastResult().farms().size()
        );

        assertEquals(
                30L,
                f.ledger.progress(SECOND_HOLDING)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );
    }

    @Test
    void secondSettlementFailurePreventsFirstMutation()
            throws IOException {

        Fixture f = new Fixture(
                directory, true
        );

        f.addResident(
                f.secondSettlement,
                null
        );

        var operation = f.operation();

        assertThrows(
                IllegalStateException.class,
                () -> operation.strictCoordinator(29L)
                        .observe(day(30L))
        );

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(0L, f.grainAt(f.secondFarm));

        assertTrue(
                f.ledger.progress(FIRST_HOLDING)
                        .isEmpty()
        );

        assertTrue(
                f.ledger.progress(SECOND_HOLDING)
                        .isEmpty()
        );
    }

    @Test
    void legacyModeCannotRunStrictAgriculture()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        var legacy =
                new EconomyAuthorityService.Loaded(
                        EconomyAuthorityService.Mode
                                .LEGACY_COMPATIBILITY,
                        f.load().manager(),
                        "TEST_LEGACY"
                );

        var operation = f.operation(
                legacy,
                f.enrollments
        );

        assertThrows(
                IOException.class,
                () -> operation.inspect(
                        29L, 30L
                )
        );
    }

    @Test
    void invalidDayWindowIsRejected()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        var operation = f.operation();

        assertThrows(
                IllegalArgumentException.class,
                () -> operation.inspect(
                        29L, 31L
                )
        );
    }

    @Test
    void missingDayBlocksStrictProgression()
            throws IOException {

        Fixture f = new Fixture(
                directory, false
        );

        var coordinator =
                f.operation().strictCoordinator(29L);

        assertThrows(
                IllegalStateException.class,
                () -> coordinator.observe(day(31L))
        );

        assertTrue(coordinator.blocked());

        assertEquals(0L, f.grainAt(f.firstFarm));
    }
}

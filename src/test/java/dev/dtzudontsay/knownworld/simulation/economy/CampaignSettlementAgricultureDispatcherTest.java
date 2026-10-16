
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class CampaignSettlementAgricultureDispatcherTest {

    @TempDir
    Path directory;

    private static final HoldingId HOLDING_A =
            new HoldingId(1L);

    private static final HoldingId HOLDING_B =
            new HoldingId(2L);

    private static final HoldingId HOLDING_C =
            new HoldingId(3L);

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private static final class Fixture {

        final Path directory;

        final ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        final NpcRegistry npcs = new NpcRegistry();

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
        final InventoryId thirdFarm;

        final InventoryId firstHousehold;
        final InventoryId secondHousehold;

        final SettlementFoodScenarioAdapter adapter;
        final EconomyReferenceValidator validator;
        final AgriculturalIntervalLedger ledger;

        final List<AgriculturalDayEnrollment> enrollments;

        Fixture(
                Path directory,
                boolean twoFarmsInFirstSettlement,
                boolean includeSecondSettlement
        ) throws IOException {

            this.directory = directory;

            firstSettlement = makeSettlement(
                    "First Settlement"
            );

            Organization firstHouseholdOrganization =
                    makeHousehold(
                            "First Household",
                            firstSettlement
                    );

            addResident(
                    firstSettlement,
                    firstHouseholdOrganization
            );

            Organization secondHouseholdOrganization;

            if (includeSecondSettlement) {
                secondSettlement = makeSettlement(
                        "Second Settlement"
                );

                secondHouseholdOrganization =
                        makeHousehold(
                                "Second Household",
                                secondSettlement
                        );

                addResident(
                        secondSettlement,
                        secondHouseholdOrganization
                );

            } else {
                secondSettlement = null;
                secondHouseholdOrganization = null;
            }

            InventoryManager original =
                    new InventoryManager(catalog);

            firstFarm = original.create(
                    EconomicOwner.holding(HOLDING_A),
                    firstSettlement
            ).id();

            firstHousehold = original.create(
                    EconomicOwner.organization(
                            firstHouseholdOrganization.id()
                    ),
                    firstSettlement
            ).id();

            List<AgriculturalDayEnrollment> prepared =
                    new ArrayList<>();

            prepared.add(
                    enrollment(
                            HOLDING_A,
                            firstFarm,
                            firstSettlement,
                            60L,
                            80_000L
                    )
            );

            if (twoFarmsInFirstSettlement) {

                secondFarm = original.create(
                        EconomicOwner.holding(HOLDING_B),
                        firstSettlement
                ).id();

                prepared.add(
                        enrollment(
                                HOLDING_B,
                                secondFarm,
                                firstSettlement,
                                40L,
                                80_000L
                        )
                );

            } else {
                secondFarm = null;
            }

            if (includeSecondSettlement) {

                thirdFarm = original.create(
                        EconomicOwner.holding(HOLDING_C),
                        secondSettlement
                ).id();

                secondHousehold = original.create(
                        EconomicOwner.organization(
                                secondHouseholdOrganization.id()
                        ),
                        secondSettlement
                ).id();

                prepared.add(
                        enrollment(
                                HOLDING_C,
                                thirdFarm,
                                secondSettlement,
                                30L,
                                20_000L
                        )
                );

            } else {
                thirdFarm = null;
                secondHousehold = null;
            }

            enrollments = List.copyOf(prepared);

            /*
             * Activate real snapshot-primary authority
             * through the existing matching-state path.
             */
            new InventoryPersistence(directory)
                    .save(original);

            new EconomySnapshotStore(directory)
                    .write(
                            EconomySnapshotStore.capture(
                                    original
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
                    id -> HOLDING_A.equals(id)
                            || HOLDING_B.equals(id)
                            || HOLDING_C.equals(id)
            );

            validator = new EconomyReferenceValidator(
                    npcs::contains,
                    id -> settlements.find(id).isPresent(),
                    id -> organizations.find(id).isPresent(),
                    id -> HOLDING_A.equals(id)
                            || HOLDING_B.equals(id)
                            || HOLDING_C.equals(id)
            );

            ledger = new AgriculturalIntervalLedger(
                    directory
            );

            ledger.initialize();
        }

        private SettlementId makeSettlement(String name) {
            return settlements.create(
                    name,
                    SettlementType.VILLAGE,
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

        private void addResident(
                SettlementId settlement,
                Organization household
        ) {
            var npc = npcs.create(
                    "Test",
                    "Resident",
                    NpcSex.FEMALE,
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

        void addUnassignedResidentToSecondSettlement() {
            if (secondSettlement == null) {
                throw new IllegalStateException(
                        "No second settlement"
                );
            }

            addResident(secondSettlement, null);
        }

        AgriculturalDayEnrollment enrollment(
                HoldingId holding,
                InventoryId inventory,
                SettlementId settlement,
                long harvestKg,
                long gramsPerPersonPerDay
        ) {
            return new AgriculturalDayEnrollment(
                    holding,
                    inventory,
                    settlement,
                    30L,
                    new AgriculturalYieldModel.Inputs(
                            1L,
                            1L,
                            harvestKg,
                            harvestKg,
                            1000,
                            1000,
                            1000
                    ),
                    0L,
                    gramsPerPersonPerDay
            );
        }

        EconomyAuthorityService.Loaded load()
                throws IOException {
            return EconomyAuthorityService.load(
                    directory,
                    catalog
            );
        }

        CampaignSettlementAgricultureDispatcher dispatcher()
                throws IOException {
            return dispatcher(enrollments);
        }

        CampaignSettlementAgricultureDispatcher dispatcher(
                List<AgriculturalDayEnrollment> farms
        ) throws IOException {
            return dispatcher(load(), farms);
        }

        CampaignSettlementAgricultureDispatcher dispatcher(
                EconomyAuthorityService.Loaded loaded,
                List<AgriculturalDayEnrollment> farms
        ) {
            return new CampaignSettlementAgricultureDispatcher(
                    directory,
                    catalog,
                    loaded,
                    validator,
                    adapter,
                    ledger,
                    farms
            );
        }

        long grainAt(InventoryId inventory)
                throws IOException {
            return load().manager()
                    .require(inventory)
                    .quantity(GRAIN);
        }

        EconomySnapshotStore.Snapshot snapshot()
                throws IOException {
            return new EconomySnapshotStore(directory)
                    .read(catalog);
        }
    }

    private SimulationTimeContract day(long absoluteDay) {
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

    private void advanceLiveStyle(
            CampaignAgricultureDayCoordinator coordinator
    ) {
        CampaignCalendar calendar =
                new CampaignCalendar(
                        0, 29, 365, 3
                );

        SimulationClock clock =
                new SimulationClock();

        for (int i = 0; i < 3; i++) {
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

    private void advanceAcceleratedStyle(
            CampaignAgricultureDayCoordinator coordinator
    ) {
        CampaignCalendar calendar =
                new CampaignCalendar(
                        0, 29, 365, 3
                );

        SimulationClock clock =
                new SimulationClock();

        CampaignDayObserverHub observers =
                new CampaignDayObserverHub();

        observers.register(coordinator::observe);

        clock.advanceBy(
                calendar.ticksPerCampaignDay()
        );

        calendar.advanceOneDay();

        observers.publish(clock, calendar);
    }

    @Test
    void groupsMultipleFarmsByExistingSettlementIdentity()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        var dispatcher = f.dispatcher();

        assertEquals(
                List.of(
                        f.firstSettlement,
                        f.secondSettlement
                ),
                dispatcher.settlements()
        );

        var readiness = dispatcher.inspect(29L, 30L);

        assertEquals(2, readiness.settlements().size());

        assertEquals(
                2,
                readiness.settlements()
                        .get(0).food().farms().size()
        );

        assertEquals(
                1,
                readiness.settlements()
                        .get(1).food().farms().size()
        );
    }

    @Test
    void executesTwoFarmAndSingleFarmSettlementsTogether()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        var result = f.dispatcher().executeDay(
                29L, 30L, 3L
        );

        assertEquals(2, result.settlements().size());

        var first = result.settlements().get(0).food();
        var second = result.settlements().get(1).food();

        assertEquals(100L, first.harvestedKg());
        assertEquals(80L, first.consumedKg());
        assertEquals(0L, first.shortageKg());

        assertEquals(30L, second.harvestedKg());
        assertEquals(20L, second.consumedKg());
        assertEquals(0L, second.shortageKg());

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(20L, f.grainAt(f.secondFarm));
        assertEquals(10L, f.grainAt(f.thirdFarm));

        assertEquals(0L, f.grainAt(f.firstHousehold));
        assertEquals(0L, f.grainAt(f.secondHousehold));

        /*
         * Two harvests + two transfers + one
         * consumption in the first settlement.
         *
         * One of each in the second settlement.
         */
        assertEquals(
                8,
                f.snapshot().transactions().size()
        );

        assertEquals(
                30L,
                f.ledger.progress(HOLDING_A)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertEquals(
                30L,
                f.ledger.progress(HOLDING_B)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertEquals(
                30L,
                f.ledger.progress(HOLDING_C)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );
    }

    @Test
    void singleFarmUsesSameSettlementFoodEngine()
            throws IOException {

        Fixture f = new Fixture(
                directory, false, false
        );

        var result = f.dispatcher().executeDay(
                29L, 30L, 3L
        );

        assertEquals(1, result.settlements().size());

        var food = result.settlements().get(0).food();

        assertEquals(1, food.farms().size());
        assertEquals(60L, food.harvestedKg());
        assertEquals(60L, food.consumedKg());
        assertEquals(20L, food.shortageKg());

        assertEquals(
                3,
                f.snapshot().transactions().size()
        );
    }

    @Test
    void followingDayConsumesReservesWithoutNewHarvests()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        var dispatcher = f.dispatcher();

        dispatcher.executeDay(29L, 30L, 3L);

        var next = dispatcher.executeDay(
                30L, 31L, 6L
        );

        var first = next.settlements().get(0).food();
        var second = next.settlements().get(1).food();

        assertEquals(0L, first.harvestedKg());
        assertEquals(20L, first.consumedKg());
        assertEquals(60L, first.shortageKg());

        assertEquals(0L, second.harvestedKg());
        assertEquals(10L, second.consumedKg());
        assertEquals(10L, second.shortageKg());

        assertEquals(0L, f.grainAt(f.secondFarm));
        assertEquals(0L, f.grainAt(f.thirdFarm));
    }

    @Test
    void globalReadinessInspectionNeverMutatesState()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        var before = f.snapshot();

        var readiness = f.dispatcher()
                .inspect(29L, 30L);

        assertEquals(2, readiness.settlements().size());

        assertEquals(before, f.snapshot());

        assertTrue(
                f.ledger.progress(HOLDING_A).isEmpty()
        );

        assertTrue(
                f.ledger.progress(HOLDING_B).isEmpty()
        );

        assertTrue(
                f.ledger.progress(HOLDING_C).isEmpty()
        );
    }

    @Test
    void laterSettlementPreflightFailureStopsAllExecution()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        f.addUnassignedResidentToSecondSettlement();

        var dispatcher = f.dispatcher();

        assertThrows(
                IllegalStateException.class,
                () -> dispatcher.executeDay(
                        29L, 30L, 3L
                )
        );

        assertTrue(dispatcher.blocked());

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(0L, f.grainAt(f.secondFarm));
        assertEquals(0L, f.grainAt(f.thirdFarm));

        assertTrue(
                f.ledger.progress(HOLDING_A).isEmpty()
        );

        assertTrue(
                f.ledger.progress(HOLDING_C).isEmpty()
        );
    }

    @Test
    void duplicateHoldingAndInventoryEnrollmentsAreRejected()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        AgriculturalDayEnrollment repeatedHolding =
                f.enrollment(
                        HOLDING_A,
                        f.thirdFarm,
                        f.secondSettlement,
                        30L,
                        20_000L
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> f.dispatcher(
                        List.of(
                                f.enrollments.get(0),
                                repeatedHolding
                        )
                )
        );

        AgriculturalDayEnrollment repeatedInventory =
                f.enrollment(
                        HOLDING_C,
                        f.firstFarm,
                        f.secondSettlement,
                        30L,
                        20_000L
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> f.dispatcher(
                        List.of(
                                f.enrollments.get(0),
                                repeatedInventory
                        )
                )
        );
    }

    @Test
    void enrollmentOrderDoesNotChangeEconomicResult()
            throws IOException {

        Fixture first = new Fixture(
                directory.resolve("first"),
                true,
                true
        );

        Fixture second = new Fixture(
                directory.resolve("second"),
                true,
                true
        );

        first.dispatcher().executeDay(
                29L, 30L, 3L
        );

        List<AgriculturalDayEnrollment> reversed =
                new ArrayList<>(second.enrollments);

        Collections.reverse(reversed);

        second.dispatcher(reversed).executeDay(
                29L, 30L, 3L
        );

        assertEquals(
                first.snapshot(),
                second.snapshot()
        );

        for (HoldingId holding : List.of(
                HOLDING_A, HOLDING_B, HOLDING_C
        )) {
            assertEquals(
                    first.ledger.progress(holding),
                    second.ledger.progress(holding)
            );
        }
    }

    @Test
    void restartWithStaleCampaignDayCannotRepeatHarvest()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        f.dispatcher().executeDay(
                29L, 30L, 3L
        );

        var restarted = f.dispatcher();

        assertThrows(
                IOException.class,
                () -> restarted.executeDay(
                        29L, 30L, 3L
                )
        );

        assertTrue(restarted.blocked());

        assertEquals(20L, f.grainAt(f.secondFarm));
        assertEquals(10L, f.grainAt(f.thirdFarm));
    }

    @Test
    void missingIntervalLedgerBlocksEverySettlement()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        Files.delete(f.ledger.file());

        assertThrows(
                IOException.class,
                () -> f.dispatcher().inspect(
                        29L, 30L
                )
        );

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(0L, f.grainAt(f.thirdFarm));
    }

    @Test
    void liveAndAcceleratedDayPatternsProduceSameSnapshot()
            throws IOException {

        Fixture live = new Fixture(
                directory.resolve("live"),
                true,
                true
        );

        Fixture accelerated = new Fixture(
                directory.resolve("accelerated"),
                true,
                true
        );

        advanceLiveStyle(
                live.dispatcher()
                        .strictCoordinator(29L)
        );

        advanceAcceleratedStyle(
                accelerated.dispatcher()
                        .strictCoordinator(29L)
        );

        assertEquals(
                live.snapshot(),
                accelerated.snapshot()
        );

        for (HoldingId holding : List.of(
                HOLDING_A, HOLDING_B, HOLDING_C
        )) {
            assertEquals(
                    live.ledger.progress(holding),
                    accelerated.ledger.progress(holding)
            );
        }
    }

    @Test
    void strictCoordinatorIgnoresDuplicateDayObservation()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        var coordinator = f.dispatcher()
                .strictCoordinator(29L);

        assertEquals(
                CampaignAgricultureDayCoordinator.Observation
                        .EXECUTED,
                coordinator.observe(day(30L))
        );

        assertEquals(
                CampaignAgricultureDayCoordinator.Observation
                        .SAME_DAY,
                coordinator.observe(day(30L))
        );

        assertEquals(1L, coordinator.executedDays());

        assertEquals(20L, f.grainAt(f.secondFarm));
    }

    @Test
    void inconsistentHouseholdRequirementsBlockSettlement()
            throws IOException {

        Fixture f = new Fixture(
                directory, true, true
        );

        AgriculturalDayEnrollment inconsistent =
                f.enrollment(
                        HOLDING_B,
                        f.secondFarm,
                        f.firstSettlement,
                        40L,
                        10_000L
                );

        var changed = new ArrayList<>(f.enrollments);
        changed.set(1, inconsistent);

        assertThrows(
                IllegalStateException.class,
                () -> f.dispatcher(changed)
                        .inspect(29L, 30L)
        );

        assertTrue(
                f.ledger.progress(HOLDING_A).isEmpty()
        );

        assertEquals(0L, f.grainAt(f.firstFarm));
    }
}

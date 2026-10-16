
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class MultiFarmSettlementAgricultureRunnerTest {

    @TempDir
    Path directory;

    private static final HoldingId FIRST =
            new HoldingId(1L);

    private static final HoldingId SECOND =
            new HoldingId(2L);

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

        final SettlementId settlement;

        final InventoryId firstFarm;
        final InventoryId secondFarm;
        final InventoryId householdInventory;

        final SettlementFoodScenarioAdapter adapter;
        final EconomyReferenceValidator validator;
        final AgriculturalIntervalLedger ledger;

        final List<AgriculturalDayEnrollment> enrollments;

        Fixture(Path directory) throws IOException {
            this.directory = directory;

            settlement = settlements.create(
                    "Multi-Farm Settlement",
                    SettlementType.VILLAGE,
                    new SimulationPosition(
                            "minecraft:overworld",
                            0.0,
                            64.0,
                            0.0
                    )
            ).id();

            Organization household = organizations.create(
                    "Settlement Household",
                    OrganizationType.HOUSEHOLD,
                    settlement
            );

            var resident = npcs.create(
                    "Agricultural",
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
                    resident.id(), settlement
            );

            affiliations.setHousehold(
                    resident.id(), household.id()
            );

            InventoryManager original =
                    new InventoryManager(catalog);

            firstFarm = original.create(
                    EconomicOwner.holding(FIRST),
                    settlement
            ).id();

            secondFarm = original.create(
                    EconomicOwner.holding(SECOND),
                    settlement
            ).id();

            householdInventory = original.create(
                    EconomicOwner.organization(
                            household.id()
                    ),
                    settlement
            ).id();

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
                    id -> FIRST.equals(id)
                            || SECOND.equals(id)
            );

            validator = new EconomyReferenceValidator(
                    npcs::contains,
                    id -> settlements.find(id).isPresent(),
                    id -> organizations.find(id).isPresent(),
                    id -> FIRST.equals(id)
                            || SECOND.equals(id)
            );

            ledger = new AgriculturalIntervalLedger(directory);
            ledger.initialize();

            enrollments = List.of(
                    enrollment(FIRST, firstFarm, 60L, 80_000L),
                    enrollment(SECOND, secondFarm, 40L, 80_000L)
            );
        }

        AgriculturalDayEnrollment enrollment(
                HoldingId holding,
                InventoryId inventory,
                long harvestKg,
                long gramsPerPerson
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
                    gramsPerPerson
            );
        }

        EconomyAuthorityService.Loaded load()
                throws IOException {
            return EconomyAuthorityService.load(
                    directory, catalog
            );
        }

        MultiFarmSettlementAgricultureRunner runner()
                throws IOException {
            return runner(enrollments);
        }

        MultiFarmSettlementAgricultureRunner runner(
                List<AgriculturalDayEnrollment> farms
        ) throws IOException {
            return new MultiFarmSettlementAgricultureRunner(
                    directory,
                    catalog,
                    load(),
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

        AgriculturalInterval interval(long start, long end) {
            return new AgriculturalInterval(start, end, 30L);
        }

        void addUnassignedResident() {
            var npc = npcs.create(
                    "Unassigned",
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

            affiliations.setResidence(npc.id(), settlement);
        }
    }

    private SimulationTimeContract day(long absoluteDay) {
        CampaignCalendar calendar =
                new CampaignCalendar(0, 0, 365, 3);

        calendar.restore(absoluteDay, 0, 3);

        return SimulationTimeContract.capture(
                new SimulationClock(0L),
                calendar
        );
    }

    @Test
    void twoHoldingsCommitOneSettlementFoodCycle()
            throws IOException {

        Fixture f = new Fixture(directory);

        var runner = f.runner();

        var result = runner.executeDay(
                29L, 30L, 3L
        );

        assertEquals(100L, result.harvestedKg());
        assertEquals(80L, result.distributedKg());
        assertEquals(80L, result.consumedKg());
        assertEquals(0L, result.shortageKg());

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(20L, f.grainAt(f.secondFarm));
        assertEquals(0L, f.grainAt(f.householdInventory));

        assertEquals(5, f.snapshot().transactions().size());

        assertEquals(
                30L,
                f.ledger.progress(FIRST)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertEquals(
                30L,
                f.ledger.progress(SECOND)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertFalse(
                f.ledger.progress(FIRST)
                        .orElseThrow()
                        .pending()
        );

        assertFalse(runner.blocked());
    }

    @Test
    void restartRejectsACommittedDay()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.runner().executeDay(29L, 30L, 3L);

        var restarted = f.runner();

        assertThrows(
                IOException.class,
                () -> restarted.executeDay(
                        29L, 30L, 3L
                )
        );

        assertTrue(restarted.blocked());

        assertEquals(20L, f.grainAt(f.secondFarm));
    }

    @Test
    void nextDayConsumesOnlyPreviouslyAvailableFood()
            throws IOException {

        Fixture f = new Fixture(directory);

        var runner = f.runner();

        runner.executeDay(29L, 30L, 3L);

        var next = runner.executeDay(
                30L, 31L, 6L
        );

        assertEquals(0L, next.harvestedKg());
        assertEquals(20L, next.distributedKg());
        assertEquals(20L, next.consumedKg());
        assertEquals(60L, next.shortageKg());

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(0L, f.grainAt(f.secondFarm));

        assertEquals(
                31L,
                f.ledger.progress(FIRST)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertEquals(
                31L,
                f.ledger.progress(SECOND)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );
    }

    @Test
    void readinessInspectionIsReadOnly()
            throws IOException {

        Fixture f = new Fixture(directory);

        var runner = f.runner();

        var preview = runner.inspect(29L, 30L);

        assertEquals(100L, preview.harvestedKg());
        assertEquals(80L, preview.distributedKg());

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(0L, f.grainAt(f.secondFarm));

        assertTrue(f.ledger.progress(FIRST).isEmpty());
        assertTrue(f.ledger.progress(SECOND).isEmpty());

        assertEquals(0, f.snapshot().transactions().size());
    }

    @Test
    void pendingHoldingBlocksEntireSettlement()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.ledger.begin(
                FIRST,
                f.interval(29L, 30L)
        );

        var runner = f.runner();

        assertThrows(
                IOException.class,
                () -> runner.executeDay(
                        29L, 30L, 3L
                )
        );

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(0L, f.grainAt(f.secondFarm));

        assertTrue(
                f.ledger.progress(SECOND).isEmpty()
        );
    }

    @Test
    void unresolvedResidentBlocksAllFarmMutations()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.addUnassignedResident();

        var runner = f.runner();

        assertThrows(
                IllegalStateException.class,
                () -> runner.executeDay(
                        29L, 30L, 3L
                )
        );

        assertEquals(0L, f.grainAt(f.firstFarm));
        assertEquals(0L, f.grainAt(f.secondFarm));

        assertTrue(f.ledger.progress(FIRST).isEmpty());
        assertTrue(f.ledger.progress(SECOND).isEmpty());
    }

    @Test
    void conflictingFoodRequirementsAreRejected()
            throws IOException {

        Fixture f = new Fixture(directory);

        var invalid = List.of(
                f.enrollments.get(0),
                f.enrollment(
                        SECOND,
                        f.secondFarm,
                        40L,
                        20_000L
                )
        );

        var runner = f.runner(invalid);

        assertThrows(
                IllegalStateException.class,
                () -> runner.inspect(29L, 30L)
        );

        assertTrue(f.ledger.progress(FIRST).isEmpty());
        assertTrue(f.ledger.progress(SECOND).isEmpty());
    }

    @Test
    void strictCoordinatorUsesRealDurableOperation()
            throws IOException {

        Fixture f = new Fixture(directory);

        var coordinator =
                f.runner().strictCoordinator(29L);

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
    void reverseEnrollmentOrderKeepsSnapshotDeterministic()
            throws IOException {

        Fixture first = new Fixture(
                directory.resolve("first")
        );

        Fixture second = new Fixture(
                directory.resolve("second")
        );

        first.runner().executeDay(29L, 30L, 3L);

        List<AgriculturalDayEnrollment> reversed =
                new ArrayList<>(second.enrollments);

        Collections.reverse(reversed);

        second.runner(reversed)
                .executeDay(29L, 30L, 3L);

        assertEquals(
                first.snapshot(),
                second.snapshot()
        );

        assertEquals(
                first.ledger.progress(FIRST),
                second.ledger.progress(FIRST)
        );

        assertEquals(
                first.ledger.progress(SECOND),
                second.ledger.progress(SECOND)
        );
    }
}

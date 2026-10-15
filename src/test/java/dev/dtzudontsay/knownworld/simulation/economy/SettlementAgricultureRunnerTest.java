
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public final class SettlementAgricultureRunnerTest {

    @TempDir
    Path directory;

    private static final HoldingId HOLDING =
            new HoldingId(1L);

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
        final Organization household;

        final InventoryId farmId;
        final InventoryId householdId;

        final SettlementFoodScenarioAdapter adapter;
        final EconomyReferenceValidator validator;

        final AgriculturalIntervalLedger ledger;

        Fixture(Path directory) throws IOException {
            this.directory = directory;

            settlement = settlements.create(
                    "Integrated Test Settlement",
                    SettlementType.CASTLE,
                    new SimulationPosition(
                            "minecraft:overworld",
                            0.0,
                            64.0,
                            0.0
                    )
            ).id();

            household = organizations.create(
                    "Integrated Test Household",
                    OrganizationType.HOUSEHOLD,
                    settlement
            );

            var resident = npcs.create(
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
                    resident.id(),
                    settlement
            );

            affiliations.setHousehold(
                    resident.id(),
                    household.id()
            );

            InventoryManager original =
                    new InventoryManager(catalog);

            farmId = original.create(
                    EconomicOwner.holding(HOLDING),
                    settlement
            ).id();

            householdId = original.create(
                    EconomicOwner.organization(
                            household.id()
                    ),
                    settlement
            ).id();

            /*
             * Prepare explicitly matching legacy
             * and snapshot state, then activate
             * snapshot authority through the
             * existing migration contract.
             */
            new InventoryPersistence(directory)
                    .save(original);

            new EconomySnapshotStore(directory)
                    .write(
                            EconomySnapshotStore.capture(
                                    original
                            )
                    );

            EconomyAuthorityService
                    .activateMatchingSnapshot(
                            directory,
                            catalog
                    );

            adapter = new SettlementFoodScenarioAdapter(
                    npcs,
                    affiliations,
                    organizations,
                    settlements,
                    HOLDING::equals
            );

            validator = new EconomyReferenceValidator(
                    npcs::contains,
                    id -> settlements.find(id).isPresent(),
                    id -> organizations.find(id).isPresent(),
                    HOLDING::equals
            );

            ledger = new AgriculturalIntervalLedger(
                    directory
            );
        }

        EconomyAuthorityService.Loaded load()
                throws IOException {
            return EconomyAuthorityService.load(
                    directory,
                    catalog
            );
        }

        SettlementAgricultureRunner runner()
                throws IOException {
            return new SettlementAgricultureRunner(
                    directory,
                    catalog,
                    load(),
                    validator,
                    adapter,
                    ledger
            );
        }

        AgriculturalInterval interval(
                long start,
                long end
        ) {
            return new AgriculturalInterval(
                    start, end, 30L
            );
        }

        AgriculturalYieldModel.Inputs harvest() {
            return new AgriculturalYieldModel.Inputs(
                    1L,
                    1L,
                    100L,
                    100L,
                    1000,
                    1000,
                    1000
            );
        }

        SettlementAgricultureRunner.Outcome execute(
                SettlementAgricultureRunner runner,
                AgriculturalInterval interval,
                long gramsPerPersonPerDay
        ) throws IOException {
            return runner.execute(
                    HOLDING,
                    farmId,
                    settlement,
                    interval,
                    harvest(),
                    0L,
                    gramsPerPersonPerDay
            );
        }
    }

    @Test
    void verifiedHarvestCommitsAndRecordsInterval()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.ledger.initialize();

        var outcome = f.execute(
                f.runner(),
                f.interval(29L, 30L),
                1000L
        );

        assertEquals(
                SettlementAgricultureRunner.Status.EXECUTED,
                outcome.status()
        );

        assertEquals(100L, outcome.food().harvestedKg());
        assertEquals(1L, outcome.food().consumedKg());

        assertEquals(
                99L,
                f.load().manager()
                        .require(f.farmId)
                        .quantity(GRAIN)
        );

        assertEquals(
                30L,
                f.ledger.progress(HOLDING)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertFalse(
                f.ledger.progress(HOLDING)
                        .orElseThrow()
                        .pending()
        );
    }

    @Test
    void restartDoesNotRepeatHarvest()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.ledger.initialize();

        f.execute(
                f.runner(),
                f.interval(29L, 30L),
                1000L
        );

        /*
         * New Loaded instance emulates a new
         * server session reading the durable
         * snapshot and agricultural ledger.
         */
        var restarted = f.runner();

        var duplicate = f.execute(
                restarted,
                f.interval(29L, 30L),
                1000L
        );

        assertEquals(
                SettlementAgricultureRunner.Status
                        .ALREADY_COMPLETED,
                duplicate.status()
        );

        assertNull(duplicate.food());

        assertEquals(
                99L,
                f.load().manager()
                        .require(f.farmId)
                        .quantity(GRAIN)
        );
    }

    @Test
    void nextCampaignIntervalConsumesWithoutNewHarvest()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.ledger.initialize();

        f.execute(
                f.runner(),
                f.interval(29L, 30L),
                1000L
        );

        var next = f.execute(
                f.runner(),
                f.interval(30L, 31L),
                1000L
        );

        assertEquals(
                SettlementAgricultureRunner.Status.EXECUTED,
                next.status()
        );

        assertEquals(0L, next.food().harvestedKg());
        assertEquals(1L, next.food().consumedKg());

        assertEquals(
                98L,
                f.load().manager()
                        .require(f.farmId)
                        .quantity(GRAIN)
        );
    }

    @Test
    void missingIntervalLedgerFailsClosed()
            throws IOException {

        Fixture f = new Fixture(directory);

        assertThrows(
                IOException.class,
                () -> f.execute(
                        f.runner(),
                        f.interval(29L, 30L),
                        1000L
                )
        );

        assertEquals(
                0L,
                f.load().manager()
                        .totalQuantity(GRAIN)
        );
    }

    @Test
    void failedIntervalIsNotAutomaticallyRetried()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.ledger.initialize();

        /*
         * Demand overflows the long-based
         * household model during execution.
         *
         * It occurs after durable PREPARED state,
         * but before economic resource mutations.
         */
        assertThrows(
                ArithmeticException.class,
                () -> f.execute(
                        f.runner(),
                        f.interval(0L, 29L),
                        Long.MAX_VALUE
                )
        );

        assertTrue(
                f.ledger.progress(HOLDING)
                        .orElseThrow()
                        .pending()
        );

        assertThrows(
                IOException.class,
                () -> f.execute(
                        f.runner(),
                        f.interval(0L, 29L),
                        1000L
                )
        );

        assertEquals(
                0L,
                f.load().manager()
                        .totalQuantity(GRAIN)
        );
    }

    @Test
    void unassignedResidentBlocksIntegratedSimulation()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.ledger.initialize();

        var unassigned = f.npcs.create(
                "Unassigned",
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

        f.affiliations.setResidence(
                unassigned.id(),
                f.settlement
        );

        assertThrows(
                IllegalStateException.class,
                () -> f.execute(
                        f.runner(),
                        f.interval(29L, 30L),
                        1000L
                )
        );

        assertTrue(
                f.ledger.progress(HOLDING).isEmpty()
        );
    }

    @Test
    void legacyAuthorityCannotExecuteGuardedInterval()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.ledger.initialize();

        var legacy = new EconomyAuthorityService.Loaded(
                EconomyAuthorityService.Mode
                        .LEGACY_COMPATIBILITY,
                new InventoryManager(f.catalog),
                "TEST_LEGACY"
        );

        var runner = new SettlementAgricultureRunner(
                directory,
                f.catalog,
                legacy,
                f.validator,
                f.adapter,
                f.ledger
        );

        assertThrows(
                IOException.class,
                () -> f.execute(
                        runner,
                        f.interval(29L, 30L),
                        1000L
                )
        );
    }
}

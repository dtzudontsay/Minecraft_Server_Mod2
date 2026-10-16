
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineManager;
import dev.dtzudontsay.knownworld.simulation.population.PopulationCohortManager;
import dev.dtzudontsay.knownworld.simulation.population.SettlementPopulationCensus;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingStatus;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public final class SettlementCohortFoodIntegrationTest {

    @TempDir
    Path directory;

    private static final HoldingId FARM =
            new HoldingId(1L);

    private static final ResourceId GRAIN =
            AgriculturalFoodCycle.GRAIN;

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

        final NpcRoutineManager routines =
                new NpcRoutineManager(npcs);

        final CampaignCalendar calendar =
                new CampaignCalendar();

        final PopulationCohortManager cohorts =
                new PopulationCohortManager(
                        settlements
                );

        final SettlementPopulationCensus census =
                new SettlementPopulationCensus(
                        settlements,
                        npcs,
                        affiliations,
                        routines,
                        cohorts,
                        calendar
                );

        final SettlementId settlement;

        final InventoryId farmInventory;
        final InventoryId householdInventory;
        final InventoryId cohortPantry;

        final SettlementFoodScenarioAdapter adapter;
        final EconomyReferenceValidator validator;
        final AgriculturalIntervalLedger ledger;

        final AgriculturalDayEnrollment enrollment;

        final PopulationCohortManager.Key anonymousFarmers;

        Fixture(Path directory) throws IOException {

            this.directory = directory;

            settlement = settlements.create(
                    "Cohort Food Test Settlement",
                    SettlementType.VILLAGE,
                    new SimulationPosition(
                            "minecraft:overworld",
                            0.0,
                            64.0,
                            0.0
                    )
            ).id();

            Organization household =
                    organizations.create(
                            "Named Resident Household",
                            OrganizationType.HOUSEHOLD,
                            settlement
                    );

            var npc = npcs.create(
                    "Named",
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

            affiliations.setHousehold(
                    npc.id(),
                    household.id()
            );

            anonymousFarmers =
                    new PopulationCohortManager.Key(
                            settlement,
                            PopulationCohortManager
                                    .AgeBand.ADULT,
                            NpcSex.MALE,
                            "northmen",
                            "old_gods",
                            "smallfolk",
                            "farmer"
                    );

            InventoryManager initial =
                    new InventoryManager(catalog);

            farmInventory = initial.create(
                    EconomicOwner.holding(FARM),
                    settlement
            ).id();

            householdInventory = initial.create(
                    EconomicOwner.organization(
                            household.id()
                    ),
                    settlement
            ).id();

            /*
             * Settlement-owned aggregate pantry.
             *
             * This is distinct from named household
             * inventories and holding-owned farms.
             */
            cohortPantry = initial.create(
                    EconomicOwner.settlement(
                            settlement
                    ),
                    settlement
            ).id();

            new InventoryPersistence(directory)
                    .save(initial);

            new EconomySnapshotStore(directory)
                    .write(
                            EconomySnapshotStore.capture(
                                    initial
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
                    FARM::equals
            );

            validator = new EconomyReferenceValidator(
                    npcs::contains,
                    id -> settlements.find(id).isPresent(),
                    id -> organizations.find(id).isPresent(),
                    FARM::equals
            );

            ledger = new AgriculturalIntervalLedger(
                    directory
            );

            ledger.initialize();

            /*
             * On day 30 this farm can harvest 100 kg.
             *
             * The named resident requires 80 kg per
             * day in this test to deliberately create
             * a constrained allocation scenario.
             *
             * This is a test fixture, not an ASOIAF
             * consumption assumption.
             */
            enrollment = new AgriculturalDayEnrollment(
                    FARM,
                    farmInventory,
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
                    80_000L
            );
        }

        void surveyAnonymousPopulation(long people) {

            cohorts.markSurveyed(
                    settlement,
                    PopulationCohortManager.Basis
                            .MOD_ESTIMATE
            );

            if (people > 0L) {
                cohorts.change(
                        anonymousFarmers,
                        people
                );
            }
        }

        SettlementCohortFoodIntegration cohortFood(
                InventoryId pantry
        ) {
            return new SettlementCohortFoodIntegration(
                    cohorts,
                    census,
                    pantry,
                    10_000L
            );
        }

        EconomyAuthorityService.Loaded load()
                throws IOException {

            return EconomyAuthorityService.load(
                    directory,
                    catalog
            );
        }

        MultiFarmSettlementAgricultureRunner runner(
                SettlementCohortFoodIntegration food
        ) throws IOException {

            return new MultiFarmSettlementAgricultureRunner(
                    directory,
                    catalog,
                    load(),
                    validator,
                    adapter,
                    ledger,
                    List.of(enrollment),
                    food
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

    @Test
    void realHarvestFeedsNamedAndAnonymousPopulationOnce()
            throws IOException {

        Fixture f = new Fixture(directory);

        /*
         * One named resident demands 80 kg.
         * Four anonymous residents demand 40 kg.
         * The real farm can supply 100 kg.
         */
        f.surveyAnonymousPopulation(4L);

        var runner = f.runner(
                f.cohortFood(f.cohortPantry)
        );

        var result = runner.executeDay(
                29L,
                30L,
                3L
        );

        assertEquals(
                100L,
                result.harvestedKg()
        );

        assertEquals(
                100L,
                result.distributedKg()
        );

        assertEquals(
                100L,
                result.consumedKg()
        );

        assertEquals(
                20L,
                result.shortageKg()
        );

        assertEquals(
                2,
                result.households().size()
        );

        var namedResult = result.households()
                .stream()
                .filter(
                        row -> row.inventory()
                                .equals(f.householdInventory)
                )
                .findFirst()
                .orElseThrow();

        var anonymousResult = result.households()
                .stream()
                .filter(
                        row -> row.inventory()
                                .equals(f.cohortPantry)
                )
                .findFirst()
                .orElseThrow();

        assertEquals(
                80L,
                namedResult.demandKg()
        );

        assertEquals(
                40L,
                anonymousResult.demandKg()
        );

        /*
         * Existing proportional food allocation:
         *
         * 100 kg shared against 120 kg demand.
         *
         * No group silently receives the complete
         * 100 kg independently.
         */
        assertEquals(
                67L,
                namedResult.consumedKg()
        );

        assertEquals(
                33L,
                anonymousResult.consumedKg()
        );

        assertEquals(
                13L,
                namedResult.shortageKg()
        );

        assertEquals(
                7L,
                anonymousResult.shortageKg()
        );

        assertEquals(
                0L,
                f.grainAt(f.farmInventory)
        );

        assertEquals(
                0L,
                f.grainAt(f.householdInventory)
        );

        assertEquals(
                0L,
                f.grainAt(f.cohortPantry)
        );

        /*
         * One production transaction, two transfers,
         * and two consumption transactions.
         */
        assertEquals(
                5,
                f.snapshot().transactions().size()
        );

        assertEquals(
                30L,
                f.ledger.progress(FARM)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );

        assertFalse(runner.blocked());

        /*
         * Population was used to calculate demand,
         * not changed by the economic transaction.
         */
        assertEquals(
                4L,
                f.cohorts.count(f.anonymousFarmers)
        );
    }

    @Test
    void inspectionIsReadOnlyAndRestartCannotReplayConsumption()
            throws IOException {

        Fixture f = new Fixture(directory);

        f.surveyAnonymousPopulation(4L);

        var runner = f.runner(
                f.cohortFood(f.cohortPantry)
        );

        var preview = runner.inspect(
                29L,
                30L
        );

        assertEquals(
                20L,
                preview.shortageKg()
        );

        assertEquals(
                0L,
                f.grainAt(f.farmInventory)
        );

        assertTrue(
                f.snapshot().transactions().isEmpty()
        );

        assertTrue(
                f.ledger.progress(FARM).isEmpty()
        );

        runner.executeDay(
                29L,
                30L,
                3L
        );

        var restarted = f.runner(
                f.cohortFood(f.cohortPantry)
        );

        assertThrows(
                IOException.class,
                () -> restarted.executeDay(
                        29L,
                        30L,
                        3L
                )
        );

        assertEquals(
                5,
                f.snapshot().transactions().size()
        );
    }

    @Test
    void unverifiedPopulationAndWrongPantryAreRejected()
            throws IOException {

        Fixture f = new Fixture(directory);

        /*
         * No survey has been established.
         */
        var unsurveyed = f.runner(
                f.cohortFood(f.cohortPantry)
        );

        assertThrows(
                IllegalStateException.class,
                () -> unsurveyed.inspect(
                        29L,
                        30L
                )
        );

        /*
         * The named household inventory cannot
         * also serve as the anonymous pantry.
         */
        f.surveyAnonymousPopulation(4L);

        var invalidPantry = f.runner(
                f.cohortFood(f.householdInventory)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> invalidPantry.inspect(
                        29L,
                        30L
                )
        );

        assertTrue(
                f.ledger.progress(FARM).isEmpty()
        );

        assertEquals(
                0,
                f.snapshot().transactions().size()
        );
    }

    @Test
    void surveyedEmptyCohortDoesNotInventConsumers()
            throws IOException {

        Fixture f = new Fixture(directory);

        /*
         * A valid zero-anonymous survey is different
         * from an unknown population.
         */
        f.surveyAnonymousPopulation(0L);

        var runner = f.runner(
                f.cohortFood(null)
        );

        var result = runner.executeDay(
                29L,
                30L,
                3L
        );

        assertEquals(
                1,
                result.households().size()
        );

        assertEquals(
                80L,
                result.consumedKg()
        );

        assertEquals(
                0L,
                result.shortageKg()
        );

        assertEquals(
                20L,
                f.grainAt(f.farmInventory)
        );
    }

    @Test
    void unplacedHoldingFoodDemandIsProjectedWithoutInventory()
            throws IOException {

        Fixture f = new Fixture(directory);

        HoldingId estateId =
                new HoldingId(20L);

        LandedHolding estate = new LandedHolding(
                estateId,
                "unplaced_food_estate",
                "Unplaced Food Estate",
                HoldingType.ESTATE,
                "winterfell",
                null,
                null,
                HoldingStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                0.0,
                0.0,
                0.0
        );

        PopulationCohortManager unplaced =
                new PopulationCohortManager(
                        f.settlements,
                        id -> estateId.equals(id)
                                ? Optional.of(estate)
                                : Optional.empty()
                );

        unplaced.markHoldingSurveyed(
                estateId,
                PopulationCohortManager.Basis.MOD_ESTIMATE
        );

        var farmers =
                new PopulationCohortManager.HoldingKey(
                        estateId,
                        PopulationCohortManager.AgeBand.ADULT,
                        NpcSex.FEMALE,
                        "northmen",
                        "old_gods",
                        "smallfolk",
                        "farmer"
                );

        var children =
                new PopulationCohortManager.HoldingKey(
                        estateId,
                        PopulationCohortManager.AgeBand.CHILD,
                        NpcSex.MALE,
                        "northmen",
                        "old_gods",
                        "smallfolk",
                        "unemployed"
                );

        unplaced.changeHolding(farmers, 12L);
        unplaced.changeHolding(children, 8L);

        var demand =
                SettlementCohortFoodIntegration
                        .projectHoldingDemand(
                                unplaced,
                                estateId,
                                3L,
                                800L
                        );

        assertEquals(
                20L,
                demand.anonymousResidents()
        );

        assertEquals(
                48L,
                demand.requiredKg()
        );

        /*
         * No new settlement or inventory was
         * created by calculating food requirements.
         */
        assertEquals(
                1,
                f.settlements.size()
        );

        assertEquals(
                0,
                f.snapshot().transactions().size()
        );
    }
}

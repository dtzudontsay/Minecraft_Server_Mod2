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
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class CampaignCohortAgricultureIntegrationTest {

    @TempDir
    Path directory;

    private static final HoldingId FIRST =
            new HoldingId(1L);

    private static final HoldingId SECOND =
            new HoldingId(2L);

    private static final ResourceId GRAIN =
            AgriculturalFoodCycle.GRAIN;

    private static final class Fixture {

        final Path directory;

        final ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        final SettlementManager settlements =
                new SettlementManager();

        final NpcRegistry npcs =
                new NpcRegistry();

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
                new PopulationCohortManager(settlements);

        final SettlementPopulationCensus census =
                new SettlementPopulationCensus(
                        settlements,
                        npcs,
                        affiliations,
                        routines,
                        cohorts,
                        calendar
                );

        final SettlementId namedSettlement;
        final SettlementId anonymousSettlement;

        final InventoryId firstFarm;
        final InventoryId secondFarm;
        final InventoryId namedHousehold;
        final InventoryId anonymousPantry;

        final PopulationCohortManager.Key anonymousFarmers;

        final SettlementFoodScenarioAdapter adapter;
        final EconomyReferenceValidator validator;
        final AgriculturalIntervalLedger ledger;

        final List<AgriculturalDayEnrollment> enrollments;

        Fixture(
                Path directory,
                boolean surveyAnonymous
        ) throws IOException {

            this.directory = directory;

            namedSettlement = settlements.create(
                    "Named Resident Settlement",
                    SettlementType.VILLAGE,
                    new SimulationPosition(
                            "minecraft:overworld",
                            0.0,
                            64.0,
                            0.0
                    )
            ).id();

            anonymousSettlement = settlements.create(
                    "Anonymous Population Settlement",
                    SettlementType.VILLAGE,
                    new SimulationPosition(
                            "minecraft:overworld",
                            500.0,
                            64.0,
                            500.0
                    )
            ).id();

            Organization household =
                    organizations.create(
                            "Represented Household",
                            OrganizationType.HOUSEHOLD,
                            namedSettlement
                    );

            var npc = npcs.create(
                    "Known",
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
                    namedSettlement
            );

            affiliations.setHousehold(
                    npc.id(),
                    household.id()
            );

            /*
             * The second settlement has no NPCs,
             * no named household organization,
             * and no individual residents in
             * NpcRegistry.
             */
            anonymousFarmers =
                    new PopulationCohortManager.Key(
                            anonymousSettlement,
                            PopulationCohortManager.AgeBand.ADULT,
                            NpcSex.MALE,
                            "northmen",
                            "old_gods",
                            "smallfolk",
                            "farmer"
                    );

            if (surveyAnonymous) {

                cohorts.markSurveyed(
                        anonymousSettlement,
                        PopulationCohortManager.Basis.MOD_ESTIMATE
                );

                cohorts.change(
                        anonymousFarmers,
                        4L
                );
            }

            InventoryManager original =
                    new InventoryManager(catalog);

            firstFarm = original.create(
                    EconomicOwner.holding(FIRST),
                    namedSettlement
            ).id();

            secondFarm = original.create(
                    EconomicOwner.holding(SECOND),
                    anonymousSettlement
            ).id();

            namedHousehold = original.create(
                    EconomicOwner.organization(
                            household.id()
                    ),
                    namedSettlement
            ).id();

            anonymousPantry = original.create(
                    EconomicOwner.settlement(
                            anonymousSettlement
                    ),
                    anonymousSettlement
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
                    enrollment(
                            FIRST,
                            firstFarm,
                            namedSettlement,
                            80_000L
                    ),
                    enrollment(
                            SECOND,
                            secondFarm,
                            anonymousSettlement,
                            80_000L
                    )
            );
        }

        private AgriculturalDayEnrollment enrollment(
                HoldingId holding,
                InventoryId inventory,
                SettlementId settlement,
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
                            100L,
                            100L,
                            1000,
                            1000,
                            1000
                    ),
                    0L,
                    gramsPerPerson
            );
        }

        EconomyAuthorityService.Loaded economy()
                throws IOException {

            return EconomyAuthorityService.load(
                    directory,
                    catalog
            );
        }

        SettlementCohortFoodIntegration anonymousFood() {

            return new SettlementCohortFoodIntegration(
                    cohorts,
                    census,
                    anonymousPantry,
                    10_000L
            );
        }

        CampaignSettlementAgricultureDispatcher dispatcher()
                throws IOException {

            return new CampaignSettlementAgricultureDispatcher(
                    directory,
                    catalog,
                    economy(),
                    validator,
                    adapter,
                    ledger,
                    enrollments,
                    Map.of(
                            anonymousSettlement,
                            anonymousFood()
                    )
            );
        }

        CampaignSettlementAgricultureDispatcher
        legacyDispatcher() throws IOException {

            return new CampaignSettlementAgricultureDispatcher(
                    directory,
                    catalog,
                    economy(),
                    validator,
                    adapter,
                    ledger,
                    enrollments
            );
        }

        long balance(InventoryId inventory)
                throws IOException {

            return economy()
                    .manager()
                    .require(inventory)
                    .quantity(GRAIN);
        }

        int transactionCount() throws IOException {

            return new EconomySnapshotStore(directory)
                    .read(catalog)
                    .transactions()
                    .size();
        }
    }

    @Test
    void mixedCampaignFeedsNamedAndAnonymousOnlySettlements()
            throws IOException {

        Fixture f = new Fixture(directory, true);

        var dispatcher = f.dispatcher();

        /*
         * All settlement groups are inspected before
         * any actual economic changes.
         */
        var preview = dispatcher.inspect(29L, 30L);

        assertEquals(
                2,
                preview.settlements().size()
        );

        assertEquals(
                f.namedSettlement,
                preview.settlements().get(0).settlement()
        );

        assertEquals(
                f.anonymousSettlement,
                preview.settlements().get(1).settlement()
        );

        assertEquals(
                0,
                f.transactionCount()
        );

        assertTrue(
                f.ledger.progress(FIRST).isEmpty()
        );

        assertTrue(
                f.ledger.progress(SECOND).isEmpty()
        );

        /*
         * Both farms harvest 100 kg on day 30.
         *
         * First settlement:
         * One represented resident consumes 80 kg.
         *
         * Second settlement:
         * Four anonymous residents consume
         * 4 * 10 = 40 kg.
         */
        var result = dispatcher.executeDay(
                29L,
                30L,
                3L
        );

        assertEquals(
                2,
                result.settlements().size()
        );

        var namedResult =
                result.settlements().get(0).food();

        var anonymousResult =
                result.settlements().get(1).food();

        assertEquals(
                100L,
                namedResult.harvestedKg()
        );

        assertEquals(
                100L,
                anonymousResult.harvestedKg()
        );

        assertEquals(
                80L,
                namedResult.consumedKg()
        );

        assertEquals(
                40L,
                anonymousResult.consumedKg()
        );

        assertEquals(
                0L,
                namedResult.shortageKg()
        );

        assertEquals(
                0L,
                anonymousResult.shortageKg()
        );

        assertEquals(
                1,
                namedResult.households().size()
        );

        assertEquals(
                1,
                anonymousResult.households().size()
        );

        assertEquals(
                f.namedHousehold,
                namedResult.households()
                        .get(0)
                        .inventory()
        );

        assertEquals(
                f.anonymousPantry,
                anonymousResult.households()
                        .get(0)
                        .inventory()
        );

        /*
         * The remaining grain is accounted for
         * in the original holding-owned farm
         * inventories.
         */
        assertEquals(
                20L,
                f.balance(f.firstFarm)
        );

        assertEquals(
                60L,
                f.balance(f.secondFarm)
        );

        assertEquals(
                0L,
                f.balance(f.namedHousehold)
        );

        assertEquals(
                0L,
                f.balance(f.anonymousPantry)
        );

        /*
         * Two productions, two transfers,
         * and two consumption transactions.
         */
        assertEquals(
                6,
                f.transactionCount()
        );

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

        /*
         * Food consumption doesn't directly
         * create or destroy demographic people.
         */
        assertEquals(
                4L,
                f.cohorts.count(
                        f.anonymousFarmers
                )
        );

        assertFalse(dispatcher.blocked());
    }

    @Test
    void unsurveyedAnonymousSettlementBlocksAllFarms()
            throws IOException {

        Fixture f = new Fixture(directory, false);

        var dispatcher = f.dispatcher();

        /*
         * The first settlement is valid, but the
         * second has no verified anonymous census.
         *
         * The global preflight must stop the entire
         * operation before the first farm is touched.
         */
        assertThrows(
                IllegalStateException.class,
                () -> dispatcher.executeDay(
                        29L,
                        30L,
                        3L
                )
        );

        assertTrue(dispatcher.blocked());

        assertEquals(
                0,
                f.transactionCount()
        );

        assertEquals(
                0L,
                f.balance(f.firstFarm)
        );

        assertEquals(
                0L,
                f.balance(f.secondFarm)
        );

        assertTrue(
                f.ledger.progress(FIRST).isEmpty()
        );

        assertTrue(
                f.ledger.progress(SECOND).isEmpty()
        );
    }

    @Test
    void oldDispatcherRemainsStrictAndUnknownConfigurationsFail()
            throws IOException {

        Fixture f = new Fixture(directory, true);

        /*
         * The legacy constructor does not enable
         * anonymous-only settlements.
         */
        var oldDispatcher = f.legacyDispatcher();

        assertThrows(
                IllegalStateException.class,
                () -> oldDispatcher.inspect(
                        29L,
                        30L
                )
        );

        /*
         * Cohort integration for an unenrolled
         * settlement is rejected at construction.
         */
        assertThrows(
                IllegalArgumentException.class,
                () -> new CampaignSettlementAgricultureDispatcher(
                        directory,
                        f.catalog,
                        f.economy(),
                        f.validator,
                        f.adapter,
                        f.ledger,
                        f.enrollments,
                        Map.of(
                                new SettlementId(999L),
                                f.anonymousFood()
                        )
                )
        );

        assertEquals(
                0,
                f.transactionCount()
        );

        assertTrue(
                f.ledger.progress(FIRST).isEmpty()
        );

        assertTrue(
                f.ledger.progress(SECOND).isEmpty()
        );
    }
}
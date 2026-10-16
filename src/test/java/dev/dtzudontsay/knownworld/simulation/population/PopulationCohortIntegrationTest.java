
package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.economy.*;
import dev.dtzudontsay.knownworld.simulation.npc.*;
import dev.dtzudontsay.knownworld.simulation.npc.routine.*;
import dev.dtzudontsay.knownworld.simulation.social.*;
import dev.dtzudontsay.knownworld.simulation.social.holding.*;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.world.settlement.*;
import dev.dtzudontsay.knownworld.world.reference.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public final class PopulationCohortIntegrationTest {
    @TempDir Path directory;

    private static final class Fixture {
        final SettlementManager settlements = new SettlementManager();
        final SettlementId place = settlements.create("Test Northern Village",
                SettlementType.VILLAGE, new SimulationPosition(
                        "minecraft:overworld", 0, 64, 0)).id();
        final SettlementId destination = settlements.create("Neighbor",
                SettlementType.VILLAGE, new SimulationPosition(
                        "minecraft:overworld", 100, 64, 100)).id();
        final NpcRegistry npcs = new NpcRegistry();
        final OrganizationManager organizations = new OrganizationManager(settlements);
        final NpcAffiliationManager affiliations =
                new NpcAffiliationManager(npcs, settlements, organizations);
        final NpcRoutineManager routines = new NpcRoutineManager(npcs);
        final CampaignCalendar calendar = new CampaignCalendar();
        final PopulationCohortManager cohorts = new PopulationCohortManager(settlements);
        final SettlementPopulationCensus census = new SettlementPopulationCensus(
                settlements, npcs, affiliations, routines, cohorts, calendar);
        final PopulationCohortManager.Key farmer = new PopulationCohortManager.Key(
                place, PopulationCohortManager.AgeBand.ADULT, NpcSex.FEMALE,
                "northmen", "old_gods", "smallfolk", "farmer");
        final PopulationCohortManager.Key child = new PopulationCohortManager.Key(
                place, PopulationCohortManager.AgeBand.CHILD, NpcSex.MALE,
                "northmen", "old_gods", "smallfolk", "unemployed");

        Fixture() {
            cohorts.markSurveyed(place, PopulationCohortManager.Basis.MOD_ESTIMATE);
            cohorts.markSurveyed(destination, PopulationCohortManager.Basis.MOD_ESTIMATE);
            var household = organizations.create("Test Household", OrganizationType.HOUSEHOLD, place);
            var npc = npcs.create("Named", "Farmer", NpcSex.MALE, 270,
                    new SimulationPosition("minecraft:overworld", 0, 64, 0));
            affiliations.setResidence(npc.id(), place);
            affiliations.setHousehold(npc.id(), household.id());
            routines.getOrCreate(npc.id()).setRole(NpcRoleType.FARMER);
            var deceased = npcs.create("Dead", "Farmer", NpcSex.MALE, 270,
                    new SimulationPosition("minecraft:overworld", 0, 64, 0));
            affiliations.setResidence(deceased.id(), place);
            routines.getOrCreate(deceased.id()).setRole(NpcRoleType.FARMER);
            deceased.markDead();
        }
    }

    @Test void censusDoesNotCountRepresentedOrDeadPeopleTwice() {
        Fixture f = new Fixture();
        f.cohorts.change(f.farmer, 7);
        f.cohorts.change(f.child, 3);
        var report = f.census.forSettlement(f.place);
        assertTrue(report.surveyed());
        assertEquals(1, report.represented());
        assertEquals(10, report.anonymous());
        assertEquals(11, report.total());
        assertEquals(8, report.availableFarmWorkers());
        assertEquals(0, report.residentsWithoutHousehold());
    }

    @Test void migrationAndDeathsConservePopulationWithoutCreatingNpcs() {
        Fixture f = new Fixture();
        f.cohorts.change(f.farmer, 7);
        f.cohorts.migrate(f.farmer, f.destination, 3);
        f.cohorts.change(f.farmer, -1);
        assertEquals(3, f.cohorts.count(f.farmer));
        assertEquals(3, f.cohorts.count(f.farmer.at(f.destination)));
        assertEquals(1, f.npcs.aliveCount());
        assertThrows(IllegalArgumentException.class, () -> f.cohorts.change(f.farmer, -4));
        assertEquals(3, f.cohorts.count(f.farmer));
    }

    @Test void workforceIsSplitBetweenFarmsInsteadOfRepeatedForEveryHolding()
            throws IOException {
        Fixture f = new Fixture();
        f.cohorts.change(f.farmer, 7);
        var census = f.census.forSettlement(f.place);
        InventoryManager inventories = new InventoryManager(ResourceCatalog.benchmarkDefaults());
        var castle = holding(1, "castle_cerwyn_domain", "castle_cerwyn", HoldingType.CASTLE);
        var estate = holding(2, "sample_estate", "castle_cerwyn", HoldingType.ESTATE);
        var castleInventory = inventories.create(EconomicOwner.holding(castle.id()), f.place).id();
        var estateInventory = inventories.create(EconomicOwner.holding(estate.id()), f.place).id();
        HoldingAgricultureCatalog catalog = HoldingAgricultureCatalog.loadDefault();
        var result = SettlementAgriculturalWorkforce.plan(f.place,
                List.of(new SettlementAgriculturalWorkforce.Farm(estate, estateInventory),
                        new SettlementAgriculturalWorkforce.Farm(castle, castleInventory)),
                census, inventories, catalog,
                id -> Optional.ofNullable(Map.of("castle_cerwyn", reference()).get(id)),
                1000, 1000);
        assertEquals(8, result.assignedWorkers());
        assertEquals(2, result.enrollments().size());
        assertEquals(castle.id(), result.enrollments().get(0).holding());
        long first = result.enrollments().get(0).harvestInputs().agriculturalWorkers();
        long second = result.enrollments().get(1).harvestInputs().agriculturalWorkers();
        assertEquals(8L, first + second);
        assertEquals(6L, first);
        assertEquals(2L, second);
        assertTrue(AgriculturalYieldModel.calculate(result.enrollments().get(0)
                .harvestInputs()).harvestKg() > 0L);
    }

    @Test void checkpointRoundTripAndMismatchedCalendarFailClosed() throws IOException {
        Fixture f = new Fixture();
        f.cohorts.change(f.farmer, 50);
        PopulationCohortPersistence store = new PopulationCohortPersistence(directory);
        store.save(f.calendar.absoluteDay(), f.cohorts);
        PopulationCohortManager loaded = new PopulationCohortManager(f.settlements);
        assertThrows(IOException.class, () -> store.loadInto(f.calendar.absoluteDay() + 1L, loaded));
        assertFalse(loaded.surveyed(f.place));
        assertTrue(store.loadInto(f.calendar.absoluteDay(), loaded));
        assertEquals(f.cohorts.snapshot(), loaded.snapshot());
        Files.writeString(store.file(), "CORRUPTED");
        assertThrows(IOException.class, () -> store.loadInto(f.calendar.absoluteDay(), loaded));
        assertEquals(50, loaded.count(f.farmer));
    }

    @Test void unsurveyedSettlementsNeverAcquireInventedPopulation() throws IOException {
        Fixture f = new Fixture();
        var newPlace = f.settlements.create("Unsurveyed", SettlementType.VILLAGE,
                new SimulationPosition("minecraft:overworld", 200, 64, 200)).id();
        assertFalse(f.census.forSettlement(newPlace).hasPopulationCoverage());
        assertThrows(IllegalStateException.class, () -> f.cohorts.change(
                f.farmer.at(newPlace), 1000));
        var castle = holding(1, "castle_cerwyn_domain", "castle_cerwyn", HoldingType.CASTLE);
        InventoryManager inventories = new InventoryManager(ResourceCatalog.benchmarkDefaults());
        var inventory = inventories.create(EconomicOwner.holding(castle.id()), newPlace).id();
        assertThrows(IllegalStateException.class, () -> SettlementAgriculturalWorkforce.plan(
                newPlace, List.of(new SettlementAgriculturalWorkforce.Farm(castle, inventory)),
                f.census.forSettlement(newPlace), inventories,
                HoldingAgricultureCatalog.loadDefault(),
                id -> Optional.ofNullable(Map.of("castle_cerwyn", reference()).get(id)),
                1000, 1000));
    }

    private static WorldLocationDefinition reference() {
        return new WorldLocationDefinition("castle_cerwyn", "Castle Cerwyn",
                WorldLocationKind.SETTLEMENT, "the_north", null, null,
                ReferenceProvenance.CANON, "Test named but unplaced location", false);
    }

    private static LandedHolding holding(long id, String authored,
                                         String location, HoldingType type) {
        return new LandedHolding(new HoldingId(id), authored, authored, type, location,
                null, null, HoldingStatus.ACTIVE, null, null, null, null,
                false, 0.0, 0.0, 0.0);
    }
}

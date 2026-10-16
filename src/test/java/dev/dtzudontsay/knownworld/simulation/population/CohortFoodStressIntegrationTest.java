package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.economy.CampaignSettlementAgricultureDispatcher;
import dev.dtzudontsay.knownworld.simulation.economy.InventoryId;
import dev.dtzudontsay.knownworld.simulation.economy.MultiFarmSettlementFoodCycle;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class CohortFoodStressIntegrationTest {
    @TempDir Path directory;

    private static final int DAYS_PER_YEAR = 10;
    private static final int YEAR = 298;
    private static final InventoryId PANTRY = new InventoryId(20L);
    private static final InventoryId NAMED_HOUSEHOLD = new InventoryId(21L);
    private static final AnnualCohortDemography.Rates SIMPLE_RATES =
            new AnnualCohortDemography.Rates(1000, 0, 0, 0, 0, 0, 0, 0);

    private static final class Fixture {
        final SettlementManager settlements = new SettlementManager();
        final SettlementId settlement = settlements.create("Winterfell test cohort",
                SettlementType.VILLAGE,
                new SimulationPosition("minecraft:overworld", 0, 64, 0)).id();
        final PopulationCohortManager people = new PopulationCohortManager(settlements);
        final PopulationCohortManager.Key women = new PopulationCohortManager.Key(
                settlement, PopulationCohortManager.AgeBand.ADULT, NpcSex.FEMALE,
                "northmen", "old_gods", "smallfolk", "farmer");
        Fixture() {
            people.markSurveyed(settlement, PopulationCohortManager.Basis.MOD_ESTIMATE);
            people.change(women, 100);
        }
    }

    private static SimulationTimeContract newYear() {
        CampaignCalendar calendar = new CampaignCalendar(298, 0, DAYS_PER_YEAR, 3);
        calendar.restore((YEAR + 1L) * DAYS_PER_YEAR, 0, 3);
        return SimulationTimeContract.capture(new SimulationClock(), calendar);
    }

    private static CampaignSettlementAgricultureDispatcher.DayResult completed(
            SettlementId settlement, int dayOfYear, long cohortDemand,
            long cohortShortage, long namedShortage) {
        var consumer = new MultiFarmSettlementFoodCycle.HouseholdResult(
                PANTRY, cohortDemand, cohortDemand - cohortShortage,
                cohortDemand - cohortShortage, cohortShortage, 0L);
        var named = new MultiFarmSettlementFoodCycle.HouseholdResult(
                NAMED_HOUSEHOLD, 100L, 100L - namedShortage,
                100L - namedShortage, namedShortage, 0L);
        var food = new MultiFarmSettlementFoodCycle.Result(
                List.of(), List.of(consumer, named), List.of(),
                0L, 0L, 0L, cohortShortage + namedShortage, 0);
        long start = (long) YEAR * DAYS_PER_YEAR + dayOfYear;
        return new CampaignSettlementAgricultureDispatcher.DayResult(
                start, start + 1L, start * 3L,
                List.of(new CampaignSettlementAgricultureDispatcher.SettlementResult(
                        settlement, food)));
    }

    @Test
    void fullYearOfMissingFoodReducesBirthsAndRaisesDeaths() throws IOException {
        Fixture f = new Fixture();
        CohortFoodStressLedger ledger = new CohortFoodStressLedger(directory);
        for (int day = 0; day < DAYS_PER_YEAR; day++) {
            ledger.recordCommittedDay(completed(f.settlement, day, 100L, 100L, 0L),
                    Map.of(f.settlement, PANTRY), DAYS_PER_YEAR);
        }
        assertEquals(1000, ledger.completedYearDeficitPermille(
                f.settlement, YEAR, DAYS_PER_YEAR));
        AnnualCohortDemography affected = new AnnualCohortDemography(
                f.people, SIMPLE_RATES, 42L, ledger);
        var preview = affected.previewAtNewYear(newYear());
        assertEquals(100L, preview.totalBefore());
        assertTrue(preview.deaths() > 0L);
        assertTrue(preview.births() < 100L);
        assertEquals(100L + preview.births() - preview.deaths(), preview.totalAfter());
        assertEquals(100L, f.people.count(f.women)); // preview did not mutate
        assertEquals(preview, affected.advanceAtNewYear(newYear()));
        assertEquals(AnnualCohortDemography.Status.ALREADY_APPLIED,
                affected.advanceAtNewYear(newYear()).status());
        assertEquals(YEAR, f.people.lastDemographicYear().orElseThrow());
    }

    @Test
    void missingDayBlocksMortalityAndLeavesPopulationUntouched() throws IOException {
        Fixture f = new Fixture();
        CohortFoodStressLedger ledger = new CohortFoodStressLedger(directory);
        for (int day = 0; day < DAYS_PER_YEAR - 1; day++) {
            ledger.recordCommittedDay(completed(f.settlement, day, 100L, 30L, 0L),
                    Map.of(f.settlement, PANTRY), DAYS_PER_YEAR);
        }
        var before = f.people.extendedSnapshot();
        var annual = new AnnualCohortDemography(f.people, SIMPLE_RATES, 42L, ledger);
        assertThrows(IllegalStateException.class,
                () -> annual.advanceAtNewYear(newYear()));
        assertEquals(before, f.people.extendedSnapshot());
        assertThrows(IllegalStateException.class, () -> ledger.recordCommittedDay(
                completed(f.settlement, 0, 100L, 30L, 0L),
                Map.of(f.settlement, PANTRY), DAYS_PER_YEAR));
    }

    @Test
    void savedShortageEvidenceSurvivesRestartAndDoesNotReplay() throws IOException {
        Fixture f = new Fixture();
        CohortFoodStressLedger ledger = new CohortFoodStressLedger(directory);
        for (int day = 0; day < DAYS_PER_YEAR; day++) {
            ledger.recordCommittedDay(completed(f.settlement, day, 100L, 40L, 0L),
                    Map.of(f.settlement, PANTRY), DAYS_PER_YEAR);
        }
        CohortFoodStressLedger restored = new CohortFoodStressLedger(directory);
        assertEquals(400, restored.completedYearDeficitPermille(
                f.settlement, YEAR, DAYS_PER_YEAR));
        assertThrows(IllegalStateException.class, () -> restored.recordCommittedDay(
                completed(f.settlement, DAYS_PER_YEAR - 1, 100L, 40L, 0L),
                Map.of(f.settlement, PANTRY), DAYS_PER_YEAR));
        var annual = new AnnualCohortDemography(f.people, SIMPLE_RATES, 42L, restored);
        var result = annual.advanceAtNewYear(newYear());
        PopulationCohortPersistence populationStore = new PopulationCohortPersistence(directory);
        populationStore.save(newYear().campaignAbsoluteDay(), f.people);
        PopulationCohortManager loaded = new PopulationCohortManager(f.settlements);
        assertTrue(populationStore.loadInto(newYear().campaignAbsoluteDay(), loaded));
        assertEquals(f.people.extendedSnapshot(), loaded.extendedSnapshot());
        assertEquals(AnnualCohortDemography.Status.ALREADY_APPLIED,
                new AnnualCohortDemography(loaded, SIMPLE_RATES, 42L, restored)
                        .advanceAtNewYear(newYear()).status());
        assertEquals(AnnualCohortDemography.Status.APPLIED, result.status());
    }

    @Test
    void namedNpcFoodShortageNeverBecomesAnonymousFoodStress() throws IOException {
        Fixture f = new Fixture();
        CohortFoodStressLedger ledger = new CohortFoodStressLedger(directory);
        for (int day = 0; day < DAYS_PER_YEAR; day++) {
            ledger.recordCommittedDay(completed(f.settlement, day, 100L, 0L, 100L),
                    Map.of(f.settlement, PANTRY), DAYS_PER_YEAR);
        }
        assertEquals(0, ledger.completedYearDeficitPermille(
                f.settlement, YEAR, DAYS_PER_YEAR));
        Fixture baseline = new Fixture();
        var expected = new AnnualCohortDemography(baseline.people,
                SIMPLE_RATES, 42L).advanceAtNewYear(newYear());
        var actual = new AnnualCohortDemography(f.people,
                SIMPLE_RATES, 42L, ledger).advanceAtNewYear(newYear());
        assertEquals(expected, actual);
    }

    @Test
    void damagedEvidenceFailsClosed() throws IOException {
        Fixture f = new Fixture();
        CohortFoodStressLedger ledger = new CohortFoodStressLedger(directory);
        ledger.recordCommittedDay(completed(f.settlement, 0, 100L, 50L, 0L),
                Map.of(f.settlement, PANTRY), DAYS_PER_YEAR);
        Files.writeString(ledger.file(), "broken checkpoint");
        assertThrows(IOException.class, () -> new CohortFoodStressLedger(directory));
    }
}
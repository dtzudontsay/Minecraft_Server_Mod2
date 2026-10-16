
package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.social.holding.*;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;
import dev.dtzudontsay.knownworld.simulation.world.settlement.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public final class AnnualCohortDemographyTest {
    @TempDir
    Path directory;

    private static final HoldingId HOLDING = new HoldingId(1L);

    private static final class Fixture {
        final SettlementManager settlements = new SettlementManager();

        final SettlementId town = settlements.create(
                "Test town",
                SettlementType.TOWN,
                new SimulationPosition("minecraft:overworld", 0, 64, 0)
        ).id();

        final LandedHolding estate = new LandedHolding(
                HOLDING, "test_estate", "Estate",
                HoldingType.ESTATE, "winterfell",
                null, null, HoldingStatus.ACTIVE,
                null, null, null, null,
                false, 0, 0, 0
        );

        final PopulationCohortManager people =
                new PopulationCohortManager(
                        settlements,
                        id -> HOLDING.equals(id)
                                ? Optional.of(estate)
                                : Optional.empty()
                );

        final PopulationCohortManager.Key townWomen =
                new PopulationCohortManager.Key(
                        town,
                        PopulationCohortManager.AgeBand.ADULT,
                        NpcSex.FEMALE,
                        "northmen", "old_gods", "smallfolk", "farmer"
                );

        final PopulationCohortManager.HoldingKey estateWomen =
                new PopulationCohortManager.HoldingKey(
                        HOLDING,
                        PopulationCohortManager.AgeBand.ADULT,
                        NpcSex.FEMALE,
                        "northmen", "old_gods", "smallfolk", "farmer"
                );

        Fixture() {
            people.markSurveyed(
                    town, PopulationCohortManager.Basis.MOD_ESTIMATE);
            people.markHoldingSurveyed(
                    HOLDING, PopulationCohortManager.Basis.MOD_ESTIMATE);
            people.change(townWomen, 400);
            people.changeHolding(estateWomen, 500);
        }

        AnnualCohortDemography dynamics() {
            return new AnnualCohortDemography(
                    people,
                    AnnualCohortDemography.Rates.provisional(),
                    42L
            );
        }
    }

    private static SimulationTimeContract beginningOf(int year) {
        CampaignCalendar calendar =
                new CampaignCalendar(298, 0, 365, 3);

        calendar.restore(((long) year) * 365L, 0, 3);

        return SimulationTimeContract.capture(
                new SimulationClock(), calendar);
    }

    @Test
    void anonymousResidentsAgeGiveBirthAndDieWithoutChangingNamedNpcState() {
        Fixture f = new Fixture();
        var dynamics = f.dynamics();

        var before = f.people.extendedSnapshot();
        var preview = dynamics.previewAtNewYear(beginningOf(299));

        assertEquals(before, f.people.extendedSnapshot());

        var result = dynamics.advanceAtNewYear(beginningOf(299));

        assertEquals(AnnualCohortDemography.Status.APPLIED, result.status());
        assertEquals(298, result.completedYear());
        assertEquals(900, result.totalBefore());
        assertTrue(result.births() > 0);
        assertTrue(result.agedToNextBand() > 0);
        assertTrue(result.deaths() > 0);

        assertEquals(
                result.totalBefore() + result.births() - result.deaths(),
                result.totalAfter()
        );

        assertEquals(result, preview);
        assertEquals(298, f.people.lastDemographicYear().orElseThrow());
        assertFalse(f.people.extendedSnapshot().holdingCohorts().isEmpty());
        assertFalse(f.people.extendedSnapshot().settlements().cohorts().isEmpty());
    }

    @Test
    void duplicateYearIsIdempotentAndSkippingYearsFailsClosed() {
        Fixture f = new Fixture();
        var dynamics = f.dynamics();

        dynamics.advanceAtNewYear(beginningOf(299));
        var before = f.people.extendedSnapshot();

        assertEquals(
                AnnualCohortDemography.Status.ALREADY_APPLIED,
                dynamics.advanceAtNewYear(beginningOf(299)).status()
        );

        assertEquals(before, f.people.extendedSnapshot());

        assertThrows(
                IllegalStateException.class,
                () -> dynamics.advanceAtNewYear(beginningOf(301))
        );

        assertEquals(before, f.people.extendedSnapshot());

        assertEquals(
                AnnualCohortDemography.Status.APPLIED,
                dynamics.advanceAtNewYear(beginningOf(300)).status()
        );

        assertEquals(299, f.people.lastDemographicYear().orElseThrow());
    }

    @Test
    void wrongCalendarBoundaryNeverChangesPopulation() {
        Fixture f = new Fixture();

        CampaignCalendar calendar =
                new CampaignCalendar(298, 10, 365, 3);

        var before = f.people.extendedSnapshot();

        assertThrows(
                IllegalArgumentException.class,
                () -> f.dynamics().advanceAtNewYear(
                        SimulationTimeContract.capture(
                                new SimulationClock(), calendar)
                )
        );

        assertEquals(before, f.people.extendedSnapshot());
    }

    @Test
    void savedYearMarkerPreventsReplayAfterRestart() throws Exception {
        Fixture f = new Fixture();

        f.dynamics().advanceAtNewYear(beginningOf(299));

        var store = new PopulationCohortPersistence(directory);
        long day = 299L * 365L;

        store.save(day, f.people);

        assertTrue(Files.readString(store.file()).startsWith(
                "KNOWNWORLD_POPULATION_COHORTS\t3\n"
        ));

        PopulationCohortManager loaded =
                new PopulationCohortManager(
                        f.settlements,
                        id -> HOLDING.equals(id)
                                ? Optional.of(f.estate)
                                : Optional.empty()
                );

        assertTrue(store.loadInto(day, loaded));
        assertEquals(f.people.extendedSnapshot(), loaded.extendedSnapshot());

        var again = new AnnualCohortDemography(
                loaded,
                AnnualCohortDemography.Rates.provisional(),
                42L
        );

        assertEquals(
                AnnualCohortDemography.Status.ALREADY_APPLIED,
                again.advanceAtNewYear(beginningOf(299)).status()
        );

        assertThrows(
                java.io.IOException.class,
                () -> store.loadInto(day + 1, loaded)
        );
    }

    @Test
    void oldV2CheckpointLoadsAndUpgradesWithoutInventingProgress()
            throws Exception {

        Fixture f = new Fixture();

        String body = "KNOWNWORLD_POPULATION_COHORTS\t2\nDAY\t"
                + (299L * 365L) + "\n"
                + "SURVEY\t" + f.town.value() + "\tMOD_ESTIMATE\n"
                + "COHORT\t" + f.town.value()
                + "\tADULT\tFEMALE\tnorthmen\told_gods\tsmallfolk\tfarmer\t400\n"
                + "HOLDING_SURVEY\t1\tMOD_ESTIMATE\n"
                + "HOLDING_COHORT\t1\tADULT\tFEMALE"
                + "\tnorthmen\told_gods\tsmallfolk\tfarmer\t500\n";

        String digest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(body.getBytes(StandardCharsets.UTF_8))
        );

        PopulationCohortPersistence store =
                new PopulationCohortPersistence(directory);

        Files.writeString(
                store.file(), body + "SHA256\t" + digest + "\n"
        );

        assertTrue(store.loadInto(299L * 365L, f.people));
        assertTrue(f.people.lastDemographicYear().isEmpty());
        assertEquals(500, f.people.holdingCount(f.estateWomen));

        store.save(299L * 365L, f.people);

        assertTrue(Files.readString(store.file()).startsWith(
                "KNOWNWORLD_POPULATION_COHORTS\t3\n"
        ));
    }
}

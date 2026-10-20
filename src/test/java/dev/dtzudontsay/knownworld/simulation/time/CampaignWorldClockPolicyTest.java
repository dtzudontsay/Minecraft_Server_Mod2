
package dev.dtzudontsay.knownworld.simulation.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CampaignWorldClockPolicyTest {

    @Test
    void standaloneDefaultRemainsCompatible() {

        CampaignCalendar calendar = new CampaignCalendar();

        assertEquals(
                1200,
                calendar.ticksPerCampaignDay()
        );

        assertEquals(365, calendar.daysPerYear());
        assertEquals(298, calendar.year());
    }

    @Test
    void legacyCalendarStillAdvancesAtConfiguredRate() {

        CampaignCalendar calendar = new CampaignCalendar();

        long start = calendar.absoluteDay();

        for (int i = 0; i < 1199; i++) {
            assertFalse(calendar.advanceTick());
        }

        assertEquals(start, calendar.absoluteDay());

        assertTrue(calendar.advanceTick());

        assertEquals(
                start + 1,
                calendar.absoluteDay()
        );
    }

    @Test
    void worldClockModeUsesOneMinecraftDay() {

        CampaignCalendar calendar = new CampaignCalendar();

        calendar.enableWorldClockAuthority(6000);

        assertEquals(
                24000,
                calendar.ticksPerCampaignDay()
        );

        assertTrue(calendar.worldClockAuthority());
        assertEquals(6000, calendar.tickRemainder());
    }

    @Test
    void worldAuthorityPreventsDoubleAdvancement() {

        CampaignCalendar calendar = new CampaignCalendar();

        long start = calendar.absoluteDay();

        calendar.enableWorldClockAuthority(6000);

        for (int i = 0; i < 48000; i++) {
            assertFalse(calendar.advanceTick());
        }

        assertEquals(start, calendar.absoluteDay());

        calendar.advanceOneDay();

        assertEquals(start + 1, calendar.absoluteDay());
    }

    @Test
    void actualOverworldPhaseCanBeSynchronized() {

        CampaignCalendar calendar = new CampaignCalendar();

        calendar.enableWorldClockAuthority(18000);

        assertEquals(18000, calendar.tickRemainder());

        calendar.syncWorldClockPhase(24010);

        assertEquals(10, calendar.tickRemainder());

        calendar.syncWorldClockPhase(48000);

        assertEquals(0, calendar.tickRemainder());
    }

    @Test
    void oldSaveMigrationDoesNotChangeHistoricalDate() {

        CampaignCalendar calendar = new CampaignCalendar();

        long savedDay =
                (long) 299 * 365 + 147;

        calendar.restore(savedDay, 750, 1200);

        assertEquals(299, calendar.year());
        assertEquals(1200, calendar.ticksPerCampaignDay());

        calendar.enableWorldClockAuthority(10500);

        assertEquals(savedDay, calendar.absoluteDay());
        assertEquals(24000, calendar.ticksPerCampaignDay());
        assertEquals(10500, calendar.tickRemainder());
    }

    @Test
    void historicalYearContains365Days() {

        CampaignCalendar calendar = new CampaignCalendar();

        for (int day = 0; day < 365; day++) {
            calendar.advanceOneDay();
        }

        assertEquals(299, calendar.year());
        assertEquals(0, calendar.dayOfYear());
    }

    @Test
    void worldClockDurationCannotBeOverridden() {

        CampaignCalendar calendar = new CampaignCalendar();

        calendar.enableWorldClockAuthority(0);

        assertThrows(
                IllegalStateException.class,
                () -> calendar.setTicksPerCampaignDay(1200)
        );

        assertDoesNotThrow(
                () -> calendar.setTicksPerCampaignDay(24000)
        );
    }

    @Test
    void invalidCalendarRestoreIsRejected() {

        CampaignCalendar calendar = new CampaignCalendar();

        assertThrows(
                IllegalArgumentException.class,
                () -> calendar.restore(100, 1200, 1200)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> calendar.restore(-1, 0, 24000)
        );
    }
}

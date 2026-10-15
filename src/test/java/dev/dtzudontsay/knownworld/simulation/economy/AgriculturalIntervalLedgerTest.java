
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public final class AgriculturalIntervalLedgerTest {

    @TempDir
    Path directory;

    private static final HoldingId FARM =
            new HoldingId(1L);

    private AgriculturalInterval interval(
            long start,
            long end
    ) {
        return new AgriculturalInterval(
                start, end, 30L
        );
    }

    private AgriculturalIntervalLedger ledger() {
        return new AgriculturalIntervalLedger(directory);
    }

    @Test
    void ledgerRequiresExplicitInitialization()
            throws IOException {

        assertThrows(
                IOException.class,
                () -> ledger().progress(FARM)
        );

        ledger().initialize();

        assertTrue(ledger().exists());
        assertTrue(ledger().progress(FARM).isEmpty());
    }

    @Test
    void initializationCannotSilentlyResetExistingLedger()
            throws IOException {

        ledger().initialize();

        assertThrows(
                IOException.class,
                () -> ledger().initialize()
        );
    }

    @Test
    void claimIsDurableAcrossNewLedgerInstance()
            throws IOException {

        ledger().initialize();

        assertEquals(
                AgriculturalIntervalLedger.BeginResult.STARTED,
                ledger().begin(FARM, interval(29L, 30L))
        );

        AgriculturalIntervalLedger restarted = ledger();

        var progress = restarted.progress(FARM)
                .orElseThrow();

        assertTrue(progress.pending());
        assertEquals(29L, progress.lastCompletedEndDay());
        assertEquals(30L, progress.pendingEndDayExclusive());
    }

    @Test
    void pendingIntervalCannotBeClaimedAgain()
            throws IOException {

        ledger().initialize();

        ledger().begin(FARM, interval(29L, 30L));

        assertThrows(
                IOException.class,
                () -> ledger().begin(
                        FARM, interval(29L, 30L)
                )
        );
    }

    @Test
    void completedIntervalCannotBeRepeated()
            throws IOException {

        ledger().initialize();

        ledger().begin(FARM, interval(29L, 30L));
        ledger().complete(FARM, interval(29L, 30L));

        assertEquals(
                AgriculturalIntervalLedger.BeginResult
                        .ALREADY_COMPLETED,
                ledger().begin(
                        FARM, interval(29L, 30L)
                )
        );

        assertFalse(
                ledger().progress(FARM)
                        .orElseThrow()
                        .pending()
        );
    }

    @Test
    void nextContiguousIntervalCanProceed()
            throws IOException {

        ledger().initialize();

        ledger().begin(FARM, interval(29L, 30L));
        ledger().complete(FARM, interval(29L, 30L));

        assertEquals(
                AgriculturalIntervalLedger.BeginResult.STARTED,
                ledger().begin(
                        FARM, interval(30L, 31L)
                )
        );

        ledger().complete(FARM, interval(30L, 31L));

        assertEquals(
                31L,
                ledger().progress(FARM)
                        .orElseThrow()
                        .lastCompletedEndDay()
        );
    }

    @Test
    void skippedIntervalIsRejected() throws IOException {
        ledger().initialize();

        ledger().begin(FARM, interval(29L, 30L));
        ledger().complete(FARM, interval(29L, 30L));

        assertThrows(
                IOException.class,
                () -> ledger().begin(
                        FARM, interval(31L, 32L)
                )
        );
    }

    @Test
    void corruptedLedgerFailsClosed() throws IOException {
        ledger().initialize();

        Files.writeString(
                ledger().file(),
                "CORRUPTED\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> ledger().progress(FARM)
        );
    }

    @Test
    void interruptedWriteMarkerFailsClosed()
            throws IOException {

        ledger().initialize();

        Files.writeString(
                directory.resolve(
                        "agriculture_intervals_v1.tsv.tmp"
                ),
                "INTERRUPTED\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> ledger().progress(FARM)
        );
    }

    @Test
    void progressIsTrackedIndependentlyPerHolding()
            throws IOException {

        ledger().initialize();

        HoldingId another = new HoldingId(2L);

        ledger().begin(FARM, interval(29L, 30L));
        ledger().complete(FARM, interval(29L, 30L));

        ledger().begin(
                another,
                interval(59L, 60L)
        );

        assertFalse(
                ledger().progress(FARM)
                        .orElseThrow()
                        .pending()
        );

        assertTrue(
                ledger().progress(another)
                        .orElseThrow()
                        .pending()
        );
    }
}

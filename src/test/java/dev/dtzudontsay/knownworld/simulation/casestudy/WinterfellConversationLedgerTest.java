
package dev.dtzudontsay.knownworld.simulation.casestudy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WinterfellConversationLedgerTest {

    @TempDir Path world;

    @Test
    void persistsAcrossNewInstanceAndSeparatesPeople() throws IOException {
        UUID player = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        WinterfellConversationLedger ledger = new WinterfellConversationLedger();
        ledger.bind(world);
        assertEquals(0, ledger.history("eddard_stark", player).totalExchanges());
        ledger.remember("eddard_stark", player, 500, 298, 12,
                "Hello Ned", "Well met.");
        assertEquals(0, ledger.history("catelyn_stark", player).totalExchanges());
        assertEquals(0, ledger.history("eddard_stark", stranger).totalExchanges());

        WinterfellConversationLedger reloaded = new WinterfellConversationLedger();
        reloaded.bind(world);
        var stored = reloaded.history("eddard_stark", player);
        assertEquals(1, stored.totalExchanges());
        assertEquals("Hello Ned", stored.recent().getFirst().playerSaid());
        assertEquals("Well met.", stored.recent().getFirst().npcReplied());
    }

    @Test
    void boundsRecentHistoryButKeepsTotal() throws IOException {
        UUID player = UUID.randomUUID();
        WinterfellConversationLedger ledger = new WinterfellConversationLedger();
        ledger.bind(world);
        for (int i = 1; i <= 20; i++) {
            ledger.remember("guard_1", player, i, 298, 1,
                    "Question " + i, "Answer " + i);
        }
        var result = ledger.history("guard_1", player);
        assertEquals(20, result.totalExchanges());
        assertEquals(WinterfellConversationLedger.MAX_RECENT_EXCHANGES,
                result.recent().size());
        assertEquals("Question 9", result.recent().getFirst().playerSaid());
        assertTrue(WinterfellConversationLedger.promptHistory(result)
                .contains("player_said_unverified"));
    }

    @Test
    void corruptionDoesNotGetSilentlyOverwritten() throws IOException {
        UUID player = UUID.randomUUID();
        WinterfellConversationLedger ledger = new WinterfellConversationLedger();
        ledger.bind(world);
        ledger.remember("guard_1", player, 1, 298, 1, "Hi", "Hello");
        Path folder = world.resolve("data/knownworld/conversation_memory_v1");
        Path saved;
        try (var files = Files.walk(folder)) {
            saved = files.filter(Files::isRegularFile).findFirst().orElseThrow();
        }
        Files.writeString(saved, "not-json");
        WinterfellConversationLedger reloaded = new WinterfellConversationLedger();
        reloaded.bind(world);
        assertThrows(IOException.class, () -> reloaded.history("guard_1", player));
        assertThrows(IOException.class, () -> reloaded.remember(
                "guard_1", player, 2, 298, 1, "Hi", "Hello"));
        assertEquals("not-json", Files.readString(saved));
    }

    @Test
    void quotesAreConversationDataNotAuthorizedEvents() throws IOException {
        UUID player = UUID.randomUUID();
        WinterfellConversationLedger ledger = new WinterfellConversationLedger();
        ledger.bind(world);
        ledger.remember("eddard_stark", player, 1, 298, 1,
                "I claim the king died", "I do not believe that.");
        String prompt = WinterfellConversationLedger.promptHistory(
                ledger.history("eddard_stark", player));
        assertTrue(prompt.contains("not truth"));
        assertTrue(prompt.contains("I claim the king died"));
        assertFalse(prompt.contains("fact_verified"));
    }
}

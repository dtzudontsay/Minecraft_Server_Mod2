
package dev.dtzudontsay.knownworld.simulation.casestudy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WinterfellPlayerRelationshipStoreTest {

    @TempDir
    Path world;

    @Test
    void keepsNpcAndPlayerRelationshipsSeparate() throws IOException {

        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        var store = new WinterfellPlayerRelationshipStore();
        store.bind(world);

        var result = store.act(
                "eddard_stark",
                player,
                0,
                WinterfellPlayerRelationshipStore.Action.PRAISE,
                200
        );

        assertTrue(result.accepted());
        assertTrue(result.relationship().respect() > 0);

        assertEquals(
                0,
                store.view("catelyn_stark", player, 0).respect()
        );

        assertEquals(
                0,
                store.view("eddard_stark", other, 0).respect()
        );
    }

    @Test
    void relationshipsPersistAcrossReload() throws IOException {

        UUID player = UUID.randomUUID();

        var first = new WinterfellPlayerRelationshipStore();
        first.bind(world);

        first.act(
                "winterfell_guard", player, 0,
                WinterfellPlayerRelationshipStore.Action.INSULT,
                200
        );

        var second = new WinterfellPlayerRelationshipStore();
        second.bind(world);

        var restored = second.view(
                "winterfell_guard", player, 0
        );

        assertTrue(restored.affection() < 0);
        assertTrue(restored.trust() < 0);
        assertEquals("INSULT", restored.lastAction());
    }

    @Test
    void completedConversationsAreIdempotent() throws IOException {

        UUID player = UUID.randomUUID();

        var store = new WinterfellPlayerRelationshipStore();
        store.bind(world);

        var starting = store.view("eddard_stark", player, 5);

        assertEquals(5, starting.syncedExchanges());
        assertEquals(0, starting.familiarity());

        var first = store.completedConversation(
                "eddard_stark", player, 5, 6
        );

        assertEquals(1, first.conversations());
        assertEquals(6, first.syncedExchanges());
        assertEquals(0.015, first.familiarity(), 0.000001);

        var repeated = store.completedConversation(
                "eddard_stark", player, 5, 6
        );

        assertEquals(1, repeated.conversations());
        assertEquals(
                first.familiarity(),
                repeated.familiarity(),
                0.000001
        );
    }

    @Test
    void deliberateSocialActionsAreRateLimited() throws IOException {

        UUID player = UUID.randomUUID();

        var store = new WinterfellPlayerRelationshipStore();
        store.bind(world);

        var first = store.act(
                "guard", player, 0,
                WinterfellPlayerRelationshipStore.Action.PRAISE,
                100
        );

        var early = store.act(
                "guard", player, 0,
                WinterfellPlayerRelationshipStore.Action.PRAISE,
                150
        );

        var later = store.act(
                "guard", player, 0,
                WinterfellPlayerRelationshipStore.Action.PRAISE,
                200
        );

        assertTrue(first.accepted());
        assertFalse(early.accepted());
        assertTrue(later.accepted());
    }

    @Test
    void threatsIncreaseFearAndDecreaseTrust() throws IOException {

        UUID player = UUID.randomUUID();

        var store = new WinterfellPlayerRelationshipStore();
        store.bind(world);

        var result = store.act(
                "guard", player, 0,
                WinterfellPlayerRelationshipStore.Action.THREATEN,
                100
        );

        assertTrue(result.accepted());
        assertTrue(result.relationship().fear() > 0);
        assertTrue(result.relationship().trust() < 0);
        assertTrue(result.relationship().affection() < 0);
    }

    @Test
    void relationshipValuesRemainBounded() throws IOException {

        UUID player = UUID.randomUUID();

        var store = new WinterfellPlayerRelationshipStore();
        store.bind(world);

        for (int i = 1; i <= 100; i++) {

            store.act(
                    "guard", player, 0,
                    WinterfellPlayerRelationshipStore.Action.THREATEN,
                    i * 100L
            );
        }

        var result = store.view("guard", player, 0);

        assertTrue(result.fear() <= 1);
        assertTrue(result.trust() >= -1);
        assertTrue(result.affection() >= -1);
        assertTrue(result.familiarity() <= 1);
    }

    @Test
    void refusesToOverwriteCorruptedData() throws IOException {

        UUID player = UUID.randomUUID();

        var first = new WinterfellPlayerRelationshipStore();
        first.bind(world);

        first.act(
                "guard", player, 0,
                WinterfellPlayerRelationshipStore.Action.INSULT,
                100
        );

        Path directory = world.resolve(
                "data/knownworld/player_relationships_v1"
        );

        Path file;

        try (var paths = Files.walk(directory)) {

            file = paths.filter(Files::isRegularFile)
                    .findFirst()
                    .orElseThrow();
        }

        Files.writeString(file, "invalid-json");

        var reloaded = new WinterfellPlayerRelationshipStore();
        reloaded.bind(world);

        assertThrows(
                IOException.class,
                () -> reloaded.view("guard", player, 0)
        );

        assertThrows(
                IOException.class,
                () -> reloaded.act(
                        "guard", player, 0,
                        WinterfellPlayerRelationshipStore.Action.PRAISE,
                        200
                )
        );

        assertEquals(
                "invalid-json",
                Files.readString(file)
        );
    }

    @Test
    void promptKeepsFeelingsSeparateFromWorldTruth() {

        var state = new WinterfellPlayerRelationshipStore.Snapshot(
                0.2, 0.5, 0.3, 0, 0.4,
                10, 15, 100, "PRAISE"
        );

        String prompt =
                WinterfellPlayerRelationshipStore.prompt(state);

        assertTrue(prompt.contains("Trust:"));
        assertTrue(prompt.contains("Familiarity:"));
        assertTrue(prompt.contains("NOT establish"));
        assertTrue(prompt.contains("objective truth"));
        assertTrue(prompt.contains("NOT authorize secret disclosures"));
    }
}

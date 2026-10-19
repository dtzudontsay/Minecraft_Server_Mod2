
package dev.dtzudontsay.knownworld.simulation.casestudy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WinterfellDialogueGuardTest {

    @Test
    void acceptsOrdinaryCharacterSpeech() {

        var result = WinterfellDialogueGuard.review(
                "eddard_stark",
                "Robert has been a friend since our youth, "
                        + "though a king's visit brings many duties.",
                "Robert Baratheon"
        );

        assertTrue(result.accepted());
    }

    @Test
    void rejectsInventedAemonSnowKing() {

        var result = WinterfellDialogueGuard.review(
                "wf298_test_servant",
                "King Aemon Snow and his sons are at Winterfell.",
                "Robert Baratheon"
        );

        assertFalse(result.accepted());
        assertTrue(
                result.reply().contains("Robert Baratheon")
        );
    }

    @Test
    void rejectsFakeHouseSnow() {

        var result = WinterfellDialogueGuard.review(
                "wf298_test_servant",
                "We serve House Snow, not House Baratheon.",
                "Robert Baratheon"
        );

        assertFalse(result.accepted());
    }

    @Test
    void rejectsInventedRamonTullyKinship() {

        var result = WinterfellDialogueGuard.review(
                "eddard_stark",
                "Ramon Tully is my sister's son.",
                "Robert Baratheon"
        );

        assertFalse(result.accepted());
    }

    @Test
    void rejectsLyannaChildbirthDisclosure() {

        var result = WinterfellDialogueGuard.review(
                "eddard_stark",
                "My sister Lyanna died giving birth to my son.",
                "Robert Baratheon"
        );

        assertFalse(result.accepted());
        assertEquals(
                "protected lineage disclosure",
                result.reason()
        );
    }

    @Test
    void rejectsHiddenParentageDisclosure() {

        var result = WinterfellDialogueGuard.review(
                "eddard_stark",
                "Jon Snow is the son of Rhaegar Targaryen.",
                "Robert Baratheon"
        );

        assertFalse(result.accepted());
    }

    @Test
    void rejectsRobertDeathWhileHeIsCurrentKing() {

        var result = WinterfellDialogueGuard.review(
                "wf298_test_servant",
                "King Robert is dead. Joffrey is king now.",
                "Robert Baratheon"
        );

        assertFalse(result.accepted());
    }

    @Test
    void doesNotFreezeRobertKingshipAfterSuccession() {

        var result = WinterfellDialogueGuard.review(
                "wf298_test_servant",
                "King Robert is dead.",
                "Joffrey Baratheon"
        );

        assertTrue(result.accepted());
    }

    @Test
    void refusesEmptyModelOutput() {

        var result = WinterfellDialogueGuard.review(
                "eddard_stark",
                " ",
                "Robert Baratheon"
        );

        assertFalse(result.accepted());
        assertFalse(result.reply().isBlank());
    }
}

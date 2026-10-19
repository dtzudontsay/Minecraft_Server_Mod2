
package dev.dtzudontsay.knownworld.simulation.integration;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventId;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SocialWitnessRuntimeTest {

    private static final NpcId ACTOR = new NpcId(101);
    private static final NpcId TARGET = new NpcId(202);

    private static WorldEvent social(
            String factKey,
            String factValue
    ) {

        return new WorldEvent(
                new WorldEventId(44),
                WorldEventType.SOCIAL,
                "A northern guard insulted a visiting lord.",
                SimulationPosition.overworld(10, 72, 20),
                1000L,
                0.45,
                ACTOR,
                TARGET,
                factKey,
                factValue
        );
    }

    @Test
    void observesRecognizedPublicSocialEvent() {

        assertTrue(
                SocialWitnessRuntime.isPublicSocialEvent(
                        social("social.event.insult", "0.4")
                )
        );
    }

    @Test
    void rejectsUnclassifiedSocialEvent() {

        assertFalse(
                SocialWitnessRuntime.isPublicSocialEvent(
                        social("secret.personal.disclosure", "yes")
                )
        );
    }

    @Test
    void excludesBetrayalsByDefault() {

        assertFalse(
                SocialWitnessRuntime.isPublicSocialEvent(
                        social("social.event.betray", "1.0")
                )
        );
    }

    @Test
    void rejectsMissingWitnessableFactKey() {

        assertFalse(
                SocialWitnessRuntime.isPublicSocialEvent(
                        social(null, null)
                )
        );
    }

    @Test
    void beliefKeyIsScopedToActorAndTarget() {

        String first =
                SocialWitnessRuntime.beliefKey(
                        ACTOR,
                        TARGET
                );

        String reverse =
                SocialWitnessRuntime.beliefKey(
                        TARGET,
                        ACTOR
                );

        assertNotEquals(first, reverse);

        assertEquals(
                first,
                SocialWitnessRuntime.beliefKey(
                        ACTOR,
                        TARGET
                )
        );
    }

    @Test
    void rejectsNullEvent() {

        assertFalse(
                SocialWitnessRuntime.isPublicSocialEvent(null)
        );
    }
}

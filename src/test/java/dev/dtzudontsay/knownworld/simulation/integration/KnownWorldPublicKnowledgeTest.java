
package dev.dtzudontsay.knownworld.simulation.integration;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventId;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcBelief;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KnownWorldPublicKnowledgeTest {

    private static final NpcId ACTOR = new NpcId(101);
    private static final NpcId SUBJECT = new NpcId(102);
    private static final NpcId WITNESS = new NpcId(103);
    private static final NpcId LISTENER = new NpcId(104);

    private static WorldEvent event(String key, WorldEventType type) {

        return new WorldEvent(
                new WorldEventId(10),
                type,
                "A guard publicly praised a servant.",
                SimulationPosition.overworld(4, 70, 4),
                100,
                0.4,
                ACTOR,
                SUBJECT,
                key,
                "0.3"
        );
    }

    private static WorldEvent publicEvent() {
        return event("social.event.praise", WorldEventType.SOCIAL);
    }

    private static String key() {

        return SocialWitnessRuntime.beliefKey(
                ACTOR,
                SUBJECT
        );
    }

    @Test
    void verifiedDirectWitnessCanUsePublicObservation() {

        NpcBelief belief = new NpcBelief(
                WITNESS,
                key(),
                publicEvent().summary(),
                0.9,
                null,
                100
        );

        assertTrue(
                KnownWorldPublicKnowledge.qualifies(
                        belief,
                        publicEvent()
                )
        );

        assertEquals(
                KnownWorldPublicKnowledge.Origin.WITNESSED,
                KnownWorldPublicKnowledge.origin(belief)
        );
    }

    @Test
    void verifiedHearsayRetainsSecondhandProvenance() {

        NpcBelief belief = new NpcBelief(
                LISTENER,
                key(),
                publicEvent().summary(),
                0.65,
                WITNESS,
                160
        );

        assertTrue(
                KnownWorldPublicKnowledge.qualifies(
                        belief,
                        publicEvent()
                )
        );

        assertEquals(
                KnownWorldPublicKnowledge.Origin.HEARD_FROM_OTHER,
                KnownWorldPublicKnowledge.origin(belief)
        );
    }

    @Test
    void randomPrivateBeliefIsNotPublicEvidence() {

        NpcBelief belief = new NpcBelief(
                WITNESS,
                "secret.lineage.jon_snow",
                publicEvent().summary(),
                0.9,
                null,
                100
        );

        assertFalse(
                KnownWorldPublicKnowledge.qualifies(
                        belief,
                        publicEvent()
                )
        );
    }

    @Test
    void fabricatedSummaryDoesNotPass() {

        NpcBelief belief = new NpcBelief(
                WITNESS,
                key(),
                "A completely invented meeting occurred.",
                0.9,
                null,
                100
        );

        assertFalse(
                KnownWorldPublicKnowledge.qualifies(
                        belief,
                        publicEvent()
                )
        );
    }

    @Test
    void secretAndNonSocialEventsAreExcluded() {

        NpcBelief belief = new NpcBelief(
                WITNESS,
                key(),
                publicEvent().summary(),
                0.9,
                null,
                100
        );

        assertFalse(
                KnownWorldPublicKnowledge.qualifies(
                        belief,
                        event(
                                "secret.personal.disclosure",
                                WorldEventType.SOCIAL
                        )
                )
        );

        assertFalse(
                KnownWorldPublicKnowledge.qualifies(
                        belief,
                        event(
                                "social.event.praise",
                                WorldEventType.POLITICAL
                        )
                )
        );
    }

    @Test
    void hearsayCannotPrecedeActualEvent() {

        NpcBelief belief = new NpcBelief(
                LISTENER,
                key(),
                publicEvent().summary(),
                0.8,
                WITNESS,
                90
        );

        assertFalse(
                KnownWorldPublicKnowledge.qualifies(
                        belief,
                        publicEvent()
                )
        );
    }

    @Test
    void hearsaySourceCannotBeListenerItself() {

        NpcBelief belief = new NpcBelief(
                LISTENER,
                key(),
                publicEvent().summary(),
                0.8,
                LISTENER,
                120
        );

        assertFalse(
                KnownWorldPublicKnowledge.qualifies(
                        belief,
                        publicEvent()
                )
        );
    }
}

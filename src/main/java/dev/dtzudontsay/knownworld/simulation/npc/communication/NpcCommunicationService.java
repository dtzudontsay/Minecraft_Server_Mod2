package dev.dtzudontsay.knownworld.simulation.npc.communication;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcBelief;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;

import java.util.Objects;

/**
 * Handles social transfer of information between NPCs.
 *
 * The listener does not simply copy the speaker's confidence.
 *
 * Trust and familiarity influence how strongly the listener accepts
 * the information.
 */
public final class NpcCommunicationService {

    private final NpcRegistry registry;

    private final NpcRelationshipManager relationships;

    private final NpcKnowledgeManager knowledge;

    private final NpcMemoryManager memories;

    public NpcCommunicationService(
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            NpcKnowledgeManager knowledge,
            NpcMemoryManager memories
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
                );

        this.knowledge =
                Objects.requireNonNull(
                        knowledge,
                        "knowledge"
                );

        this.memories =
                Objects.requireNonNull(
                        memories,
                        "memories"
                );
    }

    /**
     * Sincerely shares one belief from speaker to listener.
     *
     * This method represents:
     *
     * "The speaker tells the listener something the speaker actually
     * believes."
     *
     * Deliberate lying will later be a different operation.
     */
    public NpcBelief shareBelief(
            NpcId speaker,
            NpcId listener,
            String factKey,
            long tick
    ) {
        validateNpc(
                speaker
        );

        validateNpc(
                listener
        );

        if (speaker.equals(
                listener
        )) {
            throw new IllegalArgumentException(
                    "NPC cannot share information with itself"
            );
        }

        NpcBelief sourceBelief =
                knowledge.find(
                                speaker,
                                factKey
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "NPC #"
                                                        + speaker
                                                        + " does not believe anything about "
                                                        + factKey
                                        )
                        );

        /*
         * The listener's opinion of the speaker determines
         * credibility.
         */
        NpcRelationship listenerTowardSpeaker =
                relationships.getOrCreate(
                        listener,
                        speaker
                );

        NpcRelationship speakerTowardListener =
                relationships.getOrCreate(
                        speaker,
                        listener
                );

        double normalizedTrust =
                (
                        listenerTowardSpeaker.trust()
                                + 1.0
                ) / 2.0;

        /*
         * Even an unknown source has some credibility.
         *
         * trust:
         * -1.0 -> 0.50 multiplier
         *  0.0 -> 0.75 multiplier
         * +1.0 -> 1.00 multiplier
         */
        double trustFactor =
                0.50
                        + (
                        0.50
                                * normalizedTrust
                );

        /*
         * Familiar people are slightly easier to evaluate.
         *
         * unfamiliar -> 0.75
         * familiar   -> 1.00
         */
        double familiarityFactor =
                0.75
                        + (
                        0.25
                                * listenerTowardSpeaker
                                .familiarity()
                );

        double receivedConfidence =
                clampUnit(
                        sourceBelief.confidence()
                                * trustFactor
                                * familiarityFactor
                );

        knowledge.believe(
                listener,
                sourceBelief.factKey(),
                sourceBelief.value(),
                receivedConfidence,
                speaker,
                tick
        );

        /*
         * An interaction makes both people slightly more familiar
         * with one another.
         */
        listenerTowardSpeaker
                .increaseFamiliarity(
                        0.03
                );

        speakerTowardListener
                .increaseFamiliarity(
                        0.03
                );

        String speakerName =
                nameOf(
                        speaker
                );

        String listenerName =
                nameOf(
                        listener
                );

        memories.remember(
                speaker,
                NpcMemoryType.TOLD_INFORMATION,
                "Told "
                        + listenerName
                        + " that "
                        + sourceBelief.factKey()
                        + " = "
                        + sourceBelief.value(),
                0.35,
                listener,
                sourceBelief.factKey(),
                tick
        );

        memories.remember(
                listener,
                NpcMemoryType.HEARD_INFORMATION,
                "Heard from "
                        + speakerName
                        + " that "
                        + sourceBelief.factKey()
                        + " = "
                        + sourceBelief.value(),
                0.40,
                speaker,
                sourceBelief.factKey(),
                tick
        );

        return knowledge.find(
                        listener,
                        sourceBelief.factKey()
                )
                .orElseThrow();
    }

    private String nameOf(
            NpcId id
    ) {
        return registry.find(
                        id
                )
                .map(
                        NpcState::identity
                )
                .map(
                        identity ->
                                identity.fullName()
                )
                .orElse(
                        "NPC #" + id
                );
    }

    private void validateNpc(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        if (!registry.contains(
                id
        )) {
            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }
    }

    private static double clampUnit(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
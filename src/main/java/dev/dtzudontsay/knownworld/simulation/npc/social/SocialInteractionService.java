package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;

import java.util.Objects;

/**
 * Executes an objective social interaction and applies its immediate
 * consequences.
 *
 * This is the beginning of the dynamic social simulation.
 *
 * IMPORTANT:
 *
 * The service does not read scenario JSON and does not know whether
 * the characters are canonical ASOIAF characters.
 *
 * It works purely with runtime NPC state.
 *
 * This means the same system can eventually handle:
 *
 * - Eddard Stark and Robert Baratheon
 * - generated peasants
 * - generated nobles
 * - future children
 * - characters who do not exist at Scenario Day 1
 */
public final class SocialInteractionService {

    private final NpcRegistry registry;

    private final NpcRelationshipManager relationships;

    private final NpcMemoryManager memories;

    private final WorldEventManager events;

    public SocialInteractionService(
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            NpcMemoryManager memories,
            WorldEventManager events
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

        this.memories =
                Objects.requireNonNull(
                        memories,
                        "memories"
                );

        this.events =
                Objects.requireNonNull(
                        events,
                        "events"
                );
    }

    public Result perform(
            NpcId actor,
            NpcId target,
            SocialInteractionType type,
            double magnitude,
            long tick
    ) {

        Objects.requireNonNull(
                actor,
                "actor"
        );

        Objects.requireNonNull(
                target,
                "target"
        );

        Objects.requireNonNull(
                type,
                "type"
        );

        if (actor.equals(
                target
        )) {

            throw new IllegalArgumentException(
                    "NPC cannot socially interact with itself"
            );
        }

        if (!Double.isFinite(
                magnitude
        )
                || magnitude <= 0.0
                || magnitude > 1.0) {

            throw new IllegalArgumentException(
                    "Social interaction magnitude must be > 0.0 and <= 1.0"
            );
        }

        NpcState actorState =
                requireAlive(
                        actor
                );

        NpcState targetState =
                requireAlive(
                        target
                );

        /*
         * Relationships are directional.
         *
         * If Robert insults Eddard, the principal immediate effect is:
         *
         * Eddard -> Robert
         *
         * not automatically:
         *
         * Robert -> Eddard
         */
        NpcRelationship targetTowardActor =
                relationships.getOrCreate(
                        target,
                        actor
                );

        RelationshipSnapshot before =
                snapshot(
                        targetTowardActor
                );

        double affectionChange =
                type.affectionDelta()
                        * magnitude;

        double trustChange =
                type.trustDelta()
                        * magnitude;

        double respectChange =
                type.respectDelta()
                        * magnitude;

        double fearChange =
                type.fearDelta()
                        * magnitude;

        double familiarityGain =
                type.familiarityGain()
                        * magnitude;

        targetTowardActor.changeAffection(
                affectionChange
        );

        targetTowardActor.changeTrust(
                trustChange
        );

        targetTowardActor.changeRespect(
                respectChange
        );

        targetTowardActor.changeFear(
                fearChange
        );

        targetTowardActor.increaseFamiliarity(
                familiarityGain
        );

        /*
         * The actor also becomes slightly more familiar with the target
         * simply because the interaction happened.
         *
         * We deliberately do NOT mirror the emotional consequences.
         */
        relationships.getOrCreate(
                        actor,
                        target
                )
                .increaseFamiliarity(
                        familiarityGain
                                * 0.50
                );

        double importance =
                clampUnit(
                        type.baseImportance()
                                * (
                                0.50
                                        +
                                        magnitude
                                                * 0.50
                        )
                );

        String actorName =
                actorState.identity()
                        .fullName();

        String targetName =
                targetState.identity()
                        .fullName();

        String summary =
                actorName
                        + " "
                        + type.summaryVerb()
                        + " "
                        + targetName
                        + ".";

        WorldEvent event =
                events.create(
                        WorldEventType.SOCIAL,
                        summary,
                        targetState.position(),
                        tick,
                        importance,
                        actor,
                        target,
                        null,
                        null
                );

        /*
         * Target remembers experiencing the action.
         */
        memories.remember(
                target,
                NpcMemoryType.PERSONAL_EXPERIENCE,
                summary,
                importance,
                actor,
                null,
                tick
        );

        /*
         * The actor also remembers performing it, but generally with
         * slightly lower subjective importance.
         */
        memories.remember(
                actor,
                NpcMemoryType.PERSONAL_EXPERIENCE,
                "I "
                        + type.summaryVerb()
                        + " "
                        + targetName
                        + ".",
                clampUnit(
                        importance
                                * 0.75
                ),
                target,
                null,
                tick
        );

        RelationshipSnapshot after =
                snapshot(
                        targetTowardActor
                );

        return new Result(
                event,
                actor,
                target,
                type,
                magnitude,
                before,
                after
        );
    }

    private NpcState requireAlive(
            NpcId npc
    ) {

        NpcState state =
                registry.find(
                                npc
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown NPC "
                                                        + npc
                                        )
                        );

        if (!state.isAlive()) {

            throw new IllegalArgumentException(
                    "NPC is not alive: "
                            + npc
            );
        }

        return state;
    }

    private static RelationshipSnapshot snapshot(
            NpcRelationship relationship
    ) {

        return new RelationshipSnapshot(
                relationship.affection(),
                relationship.trust(),
                relationship.respect(),
                relationship.fear(),
                relationship.familiarity()
        );
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

    public record RelationshipSnapshot(
            double affection,
            double trust,
            double respect,
            double fear,
            double familiarity
    ) {
    }

    public record Result(
            WorldEvent event,
            NpcId actor,
            NpcId target,
            SocialInteractionType type,
            double magnitude,
            RelationshipSnapshot before,
            RelationshipSnapshot after
    ) {
    }
}
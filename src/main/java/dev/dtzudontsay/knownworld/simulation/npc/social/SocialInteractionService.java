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

public final class SocialInteractionService {

    private static final double SIGNIFICANT_HISTORY_THRESHOLD =
            0.60;

    private final NpcRegistry registry;

    private final NpcRelationshipManager relationships;

    private final NpcMemoryManager memories;

    private final WorldEventManager events;

    private final SocialReactionService reactions;

    public SocialInteractionService(
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            NpcMemoryManager memories,
            WorldEventManager events,
            SocialReactionService reactions
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

        this.reactions =
                Objects.requireNonNull(
                        reactions,
                        "reactions"
                );
    }

    public Result perform(
            NpcId actor,
            NpcId target,
            SocialInteractionType type,
            double magnitude,
            long tick
    ) {

        return perform(
                actor,
                target,
                type,
                magnitude,
                tick,
                SocialHistoryMode.FULL
        );
    }

    public Result perform(
            NpcId actor,
            NpcId target,
            SocialInteractionType type,
            double magnitude,
            long tick,
            SocialHistoryMode historyMode
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

        Objects.requireNonNull(
                historyMode,
                "historyMode"
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

        NpcRelationship targetTowardActor =
                relationships.getOrCreate(
                        target,
                        actor
                );

        RelationshipSnapshot before =
                snapshot(
                        targetTowardActor
                );

        /*
         * The same objective action can now produce different emotional
         * consequences depending on the target.
         */
        SocialReactionService.Reaction reaction =
                reactions.react(
                        actor,
                        target,
                        type,
                        magnitude
                );

        targetTowardActor.changeAffection(
                reaction.affectionDelta()
        );

        targetTowardActor.changeTrust(
                reaction.trustDelta()
        );

        targetTowardActor.changeRespect(
                reaction.respectDelta()
        );

        targetTowardActor.changeFear(
                reaction.fearDelta()
        );

        targetTowardActor.increaseFamiliarity(
                reaction.familiarityGain()
        );

        /*
         * Actor familiarity still increases simply because the interaction
         * occurred.
         *
         * Emotional effects remain directional.
         */
        relationships.getOrCreate(
                        actor,
                        target
                )
                .increaseFamiliarity(
                        reaction.familiarityGain()
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
                                * reaction.importanceMultiplier()
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

        boolean recordHistory =
                historyMode
                        == SocialHistoryMode.FULL
                        ||
                        (
                                historyMode
                                        == SocialHistoryMode.SIGNIFICANT_ONLY
                                        &&
                                        importance
                                                >= SIGNIFICANT_HISTORY_THRESHOLD
                        );

        WorldEvent event =
                null;

        if (recordHistory) {

            event =
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

            memories.remember(
                    target,
                    NpcMemoryType.PERSONAL_EXPERIENCE,
                    summary,
                    importance,
                    actor,
                    null,
                    tick
            );

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
        }

        return new Result(
                event,
                actor,
                target,
                type,
                magnitude,
                before,
                snapshot(
                        targetTowardActor
                ),
                reaction,
                recordHistory
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
            RelationshipSnapshot after,
            SocialReactionService.Reaction reaction,
            boolean historyRecorded
    ) {
    }
}
package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryMeaningService;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
import dev.dtzudontsay.knownworld.simulation.npc.profile.SkillPracticeService;
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

    private final SkillPracticeService skillPractice;

    public SocialInteractionService(
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            NpcMemoryManager memories,
            WorldEventManager events,
            SocialReactionService reactions,
            SkillPracticeService skillPractice
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

        this.skillPractice =
                Objects.requireNonNull(
                        skillPractice,
                        "skillPractice"
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
                ||
                magnitude <= 0.0
                ||
                magnitude > 1.0) {

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

        relationships.getOrCreate(
                        actor,
                        target
                )
                .increaseFamiliarity(
                        reaction.familiarityGain()
                                * 0.50
                );

        SkillPracticeService.Result practiceResult =
                skillPractice.practice(
                        actor,
                        practicedSkill(
                                type
                        ),
                        practiceIntensity(
                                type,
                                magnitude
                        )
                );

        double importance =
                clampUnit(
                        type.baseImportance()
                                *
                                (
                                        0.50
                                                +
                                                magnitude
                                                        * 0.50
                                )
                                *
                                reaction.importanceMultiplier()
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

        String memoryFactKey =
                memoryFactKey(
                        type
                );

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
                            memoryFactKey,
                            Double.toString(
                                    magnitude
                            )
                    );

            /*
             * Target remembers what the actor did TO them.
             *
             * This is the memory that should strongly influence future
             * behavior toward the actor.
             */
            memories.remember(
                    target,
                    NpcMemoryType.SOCIAL_INTERACTION,
                    summary,
                    importance,
                    actor,
                    memoryFactKey,
                    tick
            );

            /*
             * Actor also remembers having done it.
             *
             * The semantic key is the same factual event. Interpretation is
             * always from the memory owner's perspective when decision systems
             * consume memories.
             */
            memories.remember(
                    actor,
                    NpcMemoryType.SOCIAL_INTERACTION,
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
                    memoryFactKey,
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
                practiceResult,
                recordHistory
        );
    }

    private static String memoryFactKey(
            SocialInteractionType type
    ) {

        return switch (
                type
                ) {

            case HELP ->
                    NpcMemoryMeaningService.SOCIAL_HELP;

            case PRAISE ->
                    NpcMemoryMeaningService.SOCIAL_PRAISE;

            case INSULT ->
                    NpcMemoryMeaningService.SOCIAL_INSULT;

            case THREATEN ->
                    NpcMemoryMeaningService.SOCIAL_THREATEN;

            case BETRAY ->
                    NpcMemoryMeaningService.SOCIAL_BETRAY;
        };
    }

    private static CharacterSkill practicedSkill(
            SocialInteractionType type
    ) {

        return switch (
                type
                ) {

            case HELP ->
                    CharacterSkill.DIPLOMACY;

            case PRAISE ->
                    CharacterSkill.DIPLOMACY;

            case INSULT ->
                    CharacterSkill.DIPLOMACY;

            case THREATEN ->
                    CharacterSkill.LEADERSHIP;

            case BETRAY ->
                    CharacterSkill.INTRIGUE;
        };
    }

    private static double practiceIntensity(
            SocialInteractionType type,
            double magnitude
    ) {

        double multiplier =
                switch (
                        type
                        ) {

                    case HELP ->
                            0.55;

                    case PRAISE ->
                            0.45;

                    case INSULT ->
                            0.40;

                    case THREATEN ->
                            0.65;

                    case BETRAY ->
                            0.90;
                };

        return clampUnit(
                magnitude
                        * multiplier
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
            SkillPracticeService.Result practice,
            boolean historyRecorded
    ) {
    }
}
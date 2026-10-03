package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryMeaningService;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterDerivedTendency;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;

import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Chooses an NPC's social action toward another NPC.
 *
 * SIM 07:
 *
 * Decisions now distinguish positive from negative remembered history instead
 * of treating every important memory as emotionally equivalent.
 */
public final class SocialActionDecisionService {

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final CharacterPsychologyService psychology;

    private final NpcRelationshipManager relationships;

    private final NpcMemoryMeaningService memoryMeaning;

    public SocialActionDecisionService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
            CharacterPsychologyService psychology,
            NpcRelationshipManager relationships,
            NpcMemoryMeaningService memoryMeaning
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.profiles =
                Objects.requireNonNull(
                        profiles,
                        "profiles"
                );

        this.psychology =
                Objects.requireNonNull(
                        psychology,
                        "psychology"
                );

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
                );

        this.memoryMeaning =
                Objects.requireNonNull(
                        memoryMeaning,
                        "memoryMeaning"
                );
    }

    public Decision choose(
            NpcId actor,
            NpcId target,
            long currentTick,
            SplittableRandom random
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
                random,
                "random"
        );

        if (actor.equals(
                target
        )) {

            throw new IllegalArgumentException(
                    "NPC cannot choose a social action toward itself"
            );
        }

        NpcState actorState =
                requireAlive(
                        actor
                );

        requireAlive(
                target
        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        actor
                );

        Map<CharacterDerivedTendency, Double> tendencies =
                psychology.derivedTendencies(
                        actor
                );

        NpcRelationship relationship =
                relationships.find(
                                actor,
                                target
                        )
                        .orElse(
                                null
                        );

        double affection =
                relationship == null
                        ? 0.0
                        : relationship.affection();

        double trust =
                relationship == null
                        ? 0.0
                        : relationship.trust();

        double respect =
                relationship == null
                        ? 0.0
                        : relationship.respect();

        double fear =
                relationship == null
                        ? 0.0
                        : relationship.fear();

        double familiarity =
                relationship == null
                        ? 0.0
                        : relationship.familiarity();

        NpcMemoryMeaningService.Meaning remembered =
                memoryMeaning.toward(
                        actor,
                        target,
                        currentTick
                );

        double intervention =
                tendency(
                        tendencies,
                        CharacterDerivedTendency.INTERVENTION
                );

        double confrontation =
                tendency(
                        tendencies,
                        CharacterDerivedTendency.IMPULSIVE_CONFRONTATION
                );

        double mercy =
                tendency(
                        tendencies,
                        CharacterDerivedTendency.MERCY
                );

        double vengeance =
                tendency(
                        tendencies,
                        CharacterDerivedTendency.VENGEANCE
                );

        double deceit =
                tendency(
                        tendencies,
                        CharacterDerivedTendency.DECEITFUL_ADVANCEMENT
                );

        double risk =
                tendency(
                        tendencies,
                        CharacterDerivedTendency.RISK_TAKING
                );

        double loyalDuty =
                tendency(
                        tendencies,
                        CharacterDerivedTendency.LOYAL_DUTY
                );

        double aggression =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.AGGRESSION
                        )
                );

        double empathy =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.EMPATHY
                        )
                );

        double generosity =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.GENEROSITY
                        )
                );

        double vindictiveness =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.VINDICTIVENESS
                        )
                );

        double deceitfulness =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.DECEITFULNESS
                        )
                );

        double friendshipLoyalty =
                unitSigned(
                        profile.characterValue(
                                CharacterValue.FRIENDSHIP_LOYALTY
                        )
                );

        double honorCulture =
                unitSigned(
                        profile.characterValue(
                                CharacterValue.HONOR_CULTURE
                        )
                );

        double positiveBond =
                clampSigned(
                        affection
                                * 0.40
                                +
                                trust
                                        * 0.35
                                +
                                respect
                                        * 0.25
                );

        double negativeBond =
                clampUnit(
                        (
                                -affection
                                        - trust
                        )
                                * 0.50
                );

        double rememberedPositive =
                remembered.positive();

        double rememberedNegative =
                remembered.negative();

        double rememberedBetrayal =
                remembered.betrayal();

        double rememberedThreat =
                remembered.threat();

        double help =
                0.45
                        +
                        intervention
                                * 0.90
                        +
                        empathy
                                * 0.45
                        +
                        generosity
                                * 0.35
                        +
                        mercy
                                * 0.25
                        +
                        friendshipLoyalty
                                * 0.30
                        +
                        Math.max(
                                0.0,
                                positiveBond
                        )
                                * 0.80
                        +
                        rememberedPositive
                                * 0.55
                        -
                        negativeBond
                                * 0.45
                        -
                        rememberedNegative
                                * 0.40;

        double praise =
                0.40
                        +
                        unitSigned(
                                actorState.personality()
                                        .sociability()
                        )
                                * 0.50
                        +
                        empathy
                                * 0.20
                        +
                        Math.max(
                                0.0,
                                affection
                        )
                                * 0.65
                        +
                        Math.max(
                                0.0,
                                respect
                        )
                                * 0.55
                        +
                        rememberedPositive
                                * 0.40
                        -
                        negativeBond
                                * 0.30
                        -
                        rememberedNegative
                                * 0.25;

        double insult =
                0.12
                        +
                        confrontation
                                * 0.55
                        +
                        aggression
                                * 0.35
                        +
                        vindictiveness
                                * 0.30
                        +
                        vengeance
                                * 0.20
                        +
                        negativeBond
                                * 0.85
                        +
                        rememberedNegative
                                * 0.65
                        +
                        rememberedBetrayal
                                * 0.30
                        -
                        mercy
                                * 0.35
                        -
                        empathy
                                * 0.20;

        double threaten =
                0.06
                        +
                        confrontation
                                * 0.40
                        +
                        aggression
                                * 0.45
                        +
                        risk
                                * 0.25
                        +
                        vengeance
                                * 0.25
                        +
                        negativeBond
                                * 0.65
                        +
                        rememberedNegative
                                * 0.45
                        +
                        rememberedBetrayal
                                * 0.35
                        +
                        rememberedThreat
                                * 0.18
                        -
                        mercy
                                * 0.25;

        double betray =
                0.01
                        +
                        deceit
                                * 0.25
                        +
                        deceitfulness
                                * 0.15
                        +
                        risk
                                * 0.10
                        +
                        Math.max(
                                0.0,
                                actorState.personality()
                                        .ambition()
                        )
                                * 0.18
                        +
                        negativeBond
                                * 0.20
                        +
                        rememberedNegative
                                * 0.12
                        -
                        rememberedPositive
                                * 0.20
                        -
                        loyalDuty
                                * 0.28
                        -
                        honorCulture
                                * 0.18
                        -
                        friendshipLoyalty
                                * 0.15;

        /*
         * Betrayal is still not a normal interaction between strangers.
         */
        betray *=
                0.15
                        +
                        familiarity
                                * 0.85;

        /*
         * Fear suppresses direct confrontation.
         */
        insult *=
                1.0
                        -
                        fear
                                * 0.35;

        threaten *=
                1.0
                        -
                        fear
                                * 0.20;

        help =
                floorWeight(
                        help
                );

        praise =
                floorWeight(
                        praise
                );

        insult =
                floorWeight(
                        insult
                );

        threaten =
                floorWeight(
                        threaten
                );

        betray =
                Math.max(
                        0.002,
                        betray
                );

        SocialInteractionType chosen =
                weightedChoice(
                        random,
                        help,
                        praise,
                        insult,
                        threaten,
                        betray
                );

        double intensity =
                chooseIntensity(
                        chosen,
                        actorState,
                        profile,
                        tendencies,
                        relationship,
                        random
                );

        return new Decision(
                chosen,
                intensity,
                help,
                praise,
                insult,
                threaten,
                betray,
                remembered.totalSalience(),
                rememberedPositive,
                rememberedNegative,
                rememberedBetrayal,
                rememberedThreat
        );
    }

    private static double chooseIntensity(
            SocialInteractionType type,
            NpcState actor,
            CharacterProfile profile,
            Map<CharacterDerivedTendency, Double> tendencies,
            NpcRelationship relationship,
            SplittableRandom random
    ) {

        double base =
                switch (
                        type
                        ) {

                    case HELP ->
                            0.35
                                    +
                                    tendency(
                                            tendencies,
                                            CharacterDerivedTendency.INTERVENTION
                                    )
                                            * 0.35;

                    case PRAISE ->
                            0.30
                                    +
                                    unitSigned(
                                            actor.personality()
                                                    .sociability()
                                    )
                                            * 0.30;

                    case INSULT ->
                            0.30
                                    +
                                    tendency(
                                            tendencies,
                                            CharacterDerivedTendency.IMPULSIVE_CONFRONTATION
                                    )
                                            * 0.35;

                    case THREATEN ->
                            0.35
                                    +
                                    tendency(
                                            tendencies,
                                            CharacterDerivedTendency.RISK_TAKING
                                    )
                                            * 0.30;

                    case BETRAY ->
                            0.55
                                    +
                                    tendency(
                                            tendencies,
                                            CharacterDerivedTendency.DECEITFUL_ADVANCEMENT
                                    )
                                            * 0.30;
                };

        if (relationship != null) {

            base +=
                    Math.abs(
                            relationship.affection()
                    )
                            * 0.08;

            base +=
                    Math.abs(
                            relationship.trust()
                    )
                            * 0.08;
        }

        base +=
                random.nextDouble()
                        * 0.18;

        return clamp(
                base,
                0.20,
                1.0
        );
    }

    private static SocialInteractionType weightedChoice(
            SplittableRandom random,
            double help,
            double praise,
            double insult,
            double threaten,
            double betray
    ) {

        double total =
                help
                        +
                        praise
                        +
                        insult
                        +
                        threaten
                        +
                        betray;

        double roll =
                random.nextDouble(
                        total
                );

        if ((roll -= help)
                <= 0.0) {

            return SocialInteractionType.HELP;
        }

        if ((roll -= praise)
                <= 0.0) {

            return SocialInteractionType.PRAISE;
        }

        if ((roll -= insult)
                <= 0.0) {

            return SocialInteractionType.INSULT;
        }

        if ((roll -= threaten)
                <= 0.0) {

            return SocialInteractionType.THREATEN;
        }

        return SocialInteractionType.BETRAY;
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

    private static double tendency(
            Map<CharacterDerivedTendency, Double> tendencies,
            CharacterDerivedTendency tendency
    ) {

        return tendencies.getOrDefault(
                tendency,
                0.0
        );
    }

    private static double floorWeight(
            double value
    ) {

        return Math.max(
                0.01,
                value
        );
    }

    private static double unitSigned(
            double value
    ) {

        return (
                clampSigned(
                        value
                )
                        + 1.0
        )
                / 2.0;
    }

    private static double clampSigned(
            double value
    ) {

        return clamp(
                value,
                -1.0,
                1.0
        );
    }

    private static double clampUnit(
            double value
    ) {

        return clamp(
                value,
                0.0,
                1.0
        );
    }

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {

        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }

    public record Decision(
            SocialInteractionType type,
            double magnitude,
            double helpWeight,
            double praiseWeight,
            double insultWeight,
            double threatenWeight,
            double betrayWeight,
            double memorySalience,
            double positiveMemory,
            double negativeMemory,
            double betrayalMemory,
            double threatMemory
    ) {
    }
}
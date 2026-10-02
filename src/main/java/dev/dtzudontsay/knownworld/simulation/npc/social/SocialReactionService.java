package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
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

/**
 * Converts an objective social action into a subjective emotional reaction.
 *
 * Two characters can therefore experience the same action differently.
 */
public final class SocialReactionService {

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final CharacterPsychologyService psychology;

    private final NpcRelationshipManager relationships;

    public SocialReactionService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
            CharacterPsychologyService psychology,
            NpcRelationshipManager relationships
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
    }

    public Reaction react(
            NpcId actor,
            NpcId target,
            SocialInteractionType type,
            double magnitude
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

        if (!Double.isFinite(
                magnitude
        )
                || magnitude <= 0.0
                || magnitude > 1.0) {

            throw new IllegalArgumentException(
                    "magnitude must be > 0.0 and <= 1.0"
            );
        }

        requireAlive(
                actor
        );

        NpcState targetState =
                requireAlive(
                        target
                );

        CharacterProfile profile =
                profiles.getOrCreate(
                        target
                );

        Map<CharacterDerivedTendency, Double> tendencies =
                psychology.derivedTendencies(
                        target
                );

        NpcRelationship targetTowardActor =
                relationships.find(
                                target,
                                actor
                        )
                        .orElse(
                                null
                        );

        double previousTrust =
                targetTowardActor == null
                        ? 0.0
                        : targetTowardActor.trust();

        double previousAffection =
                targetTowardActor == null
                        ? 0.0
                        : targetTowardActor.affection();

        double empathy =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.EMPATHY
                        )
                );

        double forgiveness =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.FORGIVENESS
                        )
                );

        double vindictiveness =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.VINDICTIVENESS
                        )
                );

        double pride =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.PRIDE
                        )
                );

        double emotionalStability =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.EMOTIONAL_STABILITY
                        )
                );

        double suspiciousness =
                unitSigned(
                        profile.disposition(
                                CharacterDisposition.SUSPICIOUSNESS
                        )
                );

        double honorCulture =
                unitSigned(
                        profile.characterValue(
                                CharacterValue.HONOR_CULTURE
                        )
                );

        double friendshipLoyalty =
                unitSigned(
                        profile.characterValue(
                                CharacterValue.FRIENDSHIP_LOYALTY
                        )
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

        double courage =
                unitSigned(
                        targetState.personality()
                                .courage()
                );

        double positiveSensitivity =
                clamp(
                        0.75
                                +
                                empathy
                                        * 0.20
                                +
                                forgiveness
                                        * 0.10
                                +
                                Math.max(
                                        0.0,
                                        previousAffection
                                )
                                        * 0.10,
                        0.55,
                        1.35
                );

        double hostileSensitivity =
                clamp(
                        0.70
                                +
                                pride
                                        * 0.18
                                +
                                vindictiveness
                                        * 0.20
                                +
                                vengeance
                                        * 0.18
                                +
                                honorCulture
                                        * 0.10
                                -
                                forgiveness
                                        * 0.20
                                -
                                mercy
                                        * 0.08,
                        0.45,
                        1.50
                );

        double betrayalSensitivity =
                clamp(
                        0.75
                                +
                                Math.max(
                                        0.0,
                                        previousTrust
                                )
                                        * 0.40
                                +
                                friendshipLoyalty
                                        * 0.20
                                +
                                honorCulture
                                        * 0.15
                                +
                                vindictiveness
                                        * 0.15
                                -
                                forgiveness
                                        * 0.18,
                        0.55,
                        1.75
                );

        double fearSensitivity =
                clamp(
                        0.30
                                +
                                (
                                        1.0
                                                - courage
                                )
                                        * 0.85
                                +
                                (
                                        1.0
                                                - emotionalStability
                                )
                                        * 0.30,
                        0.20,
                        1.45
                );

        double trustAcceptance =
                clamp(
                        1.10
                                -
                                suspiciousness
                                        * 0.40,
                        0.55,
                        1.10
                );

        double affectionDelta =
                type.affectionDelta()
                        * magnitude;

        double trustDelta =
                type.trustDelta()
                        * magnitude;

        double respectDelta =
                type.respectDelta()
                        * magnitude;

        double fearDelta =
                type.fearDelta()
                        * magnitude;

        switch (
                type
        ) {

            case HELP -> {

                affectionDelta *=
                        positiveSensitivity;

                trustDelta *=
                        positiveSensitivity
                                * trustAcceptance;

                respectDelta *=
                        0.85
                                +
                                positiveSensitivity
                                        * 0.20;
            }

            case PRAISE -> {

                affectionDelta *=
                        positiveSensitivity;

                trustDelta *=
                        trustAcceptance;

                respectDelta *=
                        0.80
                                +
                                pride
                                        * 0.25;
            }

            case INSULT -> {

                affectionDelta *=
                        hostileSensitivity;

                trustDelta *=
                        hostileSensitivity;

                respectDelta *=
                        hostileSensitivity;

                fearDelta *=
                        fearSensitivity;
            }

            case THREATEN -> {

                affectionDelta *=
                        hostileSensitivity;

                trustDelta *=
                        hostileSensitivity;

                respectDelta *=
                        hostileSensitivity
                                * (
                                0.85
                                        +
                                        pride
                                                * 0.25
                        );

                fearDelta *=
                        fearSensitivity;
            }

            case BETRAY -> {

                affectionDelta *=
                        betrayalSensitivity;

                trustDelta *=
                        betrayalSensitivity;

                respectDelta *=
                        betrayalSensitivity;

                fearDelta *=
                        fearSensitivity;
            }
        }

        double emotionalImpact =
                Math.abs(
                        affectionDelta
                )
                        +
                        Math.abs(
                                trustDelta
                        )
                        +
                        Math.abs(
                                respectDelta
                        )
                        +
                        Math.abs(
                                fearDelta
                        );

        double importanceMultiplier =
                clamp(
                        0.80
                                +
                                emotionalImpact
                                        * 0.35,
                        0.75,
                        1.40
                );

        return new Reaction(
                affectionDelta,
                trustDelta,
                respectDelta,
                fearDelta,
                type.familiarityGain()
                        * magnitude,
                importanceMultiplier
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

    private static double tendency(
            Map<CharacterDerivedTendency, Double> tendencies,
            CharacterDerivedTendency tendency
    ) {

        return tendencies.getOrDefault(
                tendency,
                0.0
        );
    }

    private static double unitSigned(
            double value
    ) {

        return (
                clamp(
                        value,
                        -1.0,
                        1.0
                )
                        + 1.0
        )
                / 2.0;
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

    public record Reaction(
            double affectionDelta,
            double trustDelta,
            double respectDelta,
            double fearDelta,
            double familiarityGain,
            double importanceMultiplier
    ) {
    }
}
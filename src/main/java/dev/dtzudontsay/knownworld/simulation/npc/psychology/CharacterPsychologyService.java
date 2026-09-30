package dev.dtzudontsay.knownworld.simulation.npc.psychology;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class CharacterPsychologyService {

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final NpcRelationshipManager relationships;

    public CharacterPsychologyService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
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

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
                );
    }

    /*
     * =========================================================
     * DERIVED BEHAVIOUR
     * =========================================================
     */

    public Map<CharacterDerivedTendency, Double> derivedTendencies(
            NpcId npc
    ) {

        NpcState state =
                requireNpc(
                        npc
                );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        NpcPersonality personality =
                state.personality();

        EnumMap<CharacterDerivedTendency, Double> result =
                new EnumMap<>(
                        CharacterDerivedTendency.class
                );

        /*
         * High ambition combined with low honor/deceitfulness and
         * intrigue aptitude makes advancement through manipulation
         * more attractive.
         */
        result.put(
                CharacterDerivedTendency.DECEITFUL_ADVANCEMENT,
                weighted(
                        unitSigned(
                                personality.ambition()
                        ),
                        0.30,

                        unitSigned(
                                -personality.honor()
                        ),
                        0.25,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.DECEITFULNESS
                                )
                        ),
                        0.25,

                        profile.aptitude(
                                CharacterAptitude.INTRIGUE
                        ),
                        0.20
                )
        );

        /*
         * Courage without compassion is not the same thing as
         * intervening to help somebody.
         */
        result.put(
                CharacterDerivedTendency.INTERVENTION,
                weighted(
                        unitSigned(
                                personality.courage()
                        ),
                        0.30,

                        unitSigned(
                                personality.compassion()
                        ),
                        0.30,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.EMPATHY
                                )
                        ),
                        0.20,

                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.DUTY
                                )
                        ),
                        0.20
                )
        );

        /*
         * Low patience + courage + impulsivity/aggression.
         */
        result.put(
                CharacterDerivedTendency.IMPULSIVE_CONFRONTATION,
                weighted(
                        unitSigned(
                                personality.courage()
                        ),
                        0.25,

                        unitSigned(
                                -personality.patience()
                        ),
                        0.25,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.IMPULSIVITY
                                )
                        ),
                        0.30,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.AGGRESSION
                                )
                        ),
                        0.20
                )
        );

        result.put(
                CharacterDerivedTendency.PEER_CONFORMITY,
                clampUnit(
                        socialSusceptibility(
                                npc
                        )
                                * 0.60
                                +
                                unitSigned(
                                        profile.disposition(
                                                CharacterDisposition.CONFORMITY
                                        )
                                )
                                        * 0.25
                                +
                                unitSigned(
                                        personality.sociability()
                                )
                                        * 0.15
                )
        );

        result.put(
                CharacterDerivedTendency.RISK_TAKING,
                weighted(
                        unitSigned(
                                personality.courage()
                        ),
                        0.35,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.RISK_TOLERANCE
                                )
                        ),
                        0.40,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.IMPULSIVITY
                                )
                        ),
                        0.25
                )
        );

        result.put(
                CharacterDerivedTendency.MERCY,
                weighted(
                        unitSigned(
                                personality.compassion()
                        ),
                        0.30,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.EMPATHY
                                )
                        ),
                        0.20,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.FORGIVENESS
                                )
                        ),
                        0.20,

                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.MERCY
                                )
                        ),
                        0.30
                )
        );

        result.put(
                CharacterDerivedTendency.VENGEANCE,
                weighted(
                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.VINDICTIVENESS
                                )
                        ),
                        0.35,

                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.VENGEANCE
                                )
                        ),
                        0.35,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.AGGRESSION
                                )
                        ),
                        0.20,

                        unitSigned(
                                -personality.compassion()
                        ),
                        0.10
                )
        );

        result.put(
                CharacterDerivedTendency.LOYAL_DUTY,
                weighted(
                        unitSigned(
                                personality.honor()
                        ),
                        0.25,

                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.DUTY
                                )
                        ),
                        0.25,

                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.HOUSE_LOYALTY
                                )
                        ),
                        0.20,

                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.FAMILY_LOYALTY
                                )
                        ),
                        0.15,

                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.DYNASTIC_LOYALTY
                                )
                        ),
                        0.15
                )
        );

        result.put(
                CharacterDerivedTendency.POLITICAL_ASSERTIVENESS,
                weighted(
                        unitSigned(
                                personality.ambition()
                        ),
                        0.30,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.ASSERTIVENESS
                                )
                        ),
                        0.25,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.DOMINANCE
                                )
                        ),
                        0.20,

                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.POWER_SEEKING
                                )
                        ),
                        0.25
                )
        );

        result.put(
                CharacterDerivedTendency.RELIGIOUS_RECEPTIVENESS,
                clampUnit(
                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.RELIGIOSITY
                                )
                        )
                                * 0.35
                                +
                                unitSigned(
                                        profile.socialNorm(
                                                CharacterSocialNorm.RELIGIOUS
                                        )
                                )
                                        * 0.25
                                +
                                socialSusceptibility(
                                        npc
                                )
                                        * 0.25
                                +
                                profile.aptitude(
                                        CharacterAptitude.RELIGIOUS_STUDY
                                )
                                        * 0.15
                )
        );

        result.put(
                CharacterDerivedTendency.CULTURAL_OPENNESS,
                weighted(
                        unitSigned(
                                profile.characterValue(
                                        CharacterValue.CULTURAL_TOLERANCE
                                )
                        ),
                        0.30,

                        unitSigned(
                                profile.socialNorm(
                                        CharacterSocialNorm.COSMOPOLITAN
                                )
                        ),
                        0.25,

                        unitSigned(
                                -profile.socialNorm(
                                        CharacterSocialNorm.ISOLATIONIST
                                )
                        ),
                        0.20,

                        unitSigned(
                                profile.disposition(
                                        CharacterDisposition.CURIOSITY
                                )
                        ),
                        0.25
                )
        );

        result.put(
                CharacterDerivedTendency.MANIPULATION_SUSCEPTIBILITY,
                manipulationSusceptibility(
                        npc
                )
        );

        return Map.copyOf(
                result
        );
    }

    /*
     * =========================================================
     * RESISTANCE / SUSCEPTIBILITY
     * =========================================================
     */

    public double socialSusceptibility(
            NpcId npc
    ) {

        NpcState state =
                requireNpc(
                        npc
                );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        double value =
                0.50

                        - profile.disposition(
                        CharacterDisposition.INDEPENDENCE
                ) * 0.22

                        + profile.disposition(
                        CharacterDisposition.CONFORMITY
                ) * 0.18

                        + profile.disposition(
                        CharacterDisposition.GULLIBILITY
                ) * 0.15

                        + profile.disposition(
                        CharacterDisposition.ADAPTABILITY
                ) * 0.10

                        + state.personality()
                        .sociability()
                        * 0.05

                        - profile.disposition(
                        CharacterDisposition.SUSPICIOUSNESS
                ) * 0.08;

        return clampUnit(
                value
        );
    }

    public double persuasionResistance(
            NpcId npc
    ) {

        requireNpc(
                npc
        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        double value =
                0.50

                        + profile.disposition(
                        CharacterDisposition.INDEPENDENCE
                ) * 0.22

                        + profile.disposition(
                        CharacterDisposition.DISCIPLINE
                ) * 0.12

                        + profile.disposition(
                        CharacterDisposition.EMOTIONAL_STABILITY
                ) * 0.08

                        + profile.disposition(
                        CharacterDisposition.SUSPICIOUSNESS
                ) * 0.08

                        - profile.disposition(
                        CharacterDisposition.GULLIBILITY
                ) * 0.12

                        - profile.disposition(
                        CharacterDisposition.CONFORMITY
                ) * 0.10;

        return clampUnit(
                value
        );
    }

    public double manipulationSusceptibility(
            NpcId npc
    ) {

        requireNpc(
                npc
        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        double value =
                socialSusceptibility(
                        npc
                ) * 0.40

                        + unitSigned(
                        profile.disposition(
                                CharacterDisposition.GULLIBILITY
                        )
                ) * 0.25

                        + unitSigned(
                        -profile.disposition(
                                CharacterDisposition.SUSPICIOUSNESS
                        )
                ) * 0.20

                        + unitSigned(
                        -profile.disposition(
                                CharacterDisposition.INDEPENDENCE
                        )
                ) * 0.15;

        return clampUnit(
                value
        );
    }

    public double convictionResistance(
            NpcId npc,
            CharacterValue value
    ) {

        requireNpc(
                npc
        );

        Objects.requireNonNull(
                value,
                "value"
        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        double currentStrength =
                Math.abs(
                        profile.characterValue(
                                value
                        )
                );

        double result =
                0.15

                        + currentStrength
                        * 0.45

                        + positive(
                        profile.disposition(
                                CharacterDisposition.PERSISTENCE
                        )
                ) * 0.12

                        + positive(
                        profile.disposition(
                                CharacterDisposition.DISCIPLINE
                        )
                ) * 0.10

                        + positive(
                        profile.disposition(
                                CharacterDisposition.INDEPENDENCE
                        )
                ) * 0.08

                        + positive(
                        profile.characterValue(
                                CharacterValue.TRADITIONALISM
                        )
                ) * 0.05;

        return clamp(
                result,
                0.0,
                0.95
        );
    }

    /*
     * =========================================================
     * VALUE INFLUENCE
     * =========================================================
     */

    public CharacterInfluenceResult applyValuePressure(
            NpcId targetNpc,
            CharacterValue value,
            double targetValue,
            double pressure,
            CharacterInfluenceChannel channel
    ) {

        return applyValuePressure(
                targetNpc,
                null,
                value,
                targetValue,
                pressure,
                channel
        );
    }

    public CharacterInfluenceResult applyValuePressure(
            NpcId targetNpc,
            NpcId sourceNpc,
            CharacterValue value,
            double targetValue,
            double pressure,
            CharacterInfluenceChannel channel
    ) {

        requireNpc(
                targetNpc
        );

        validateOptionalSource(
                targetNpc,
                sourceNpc
        );

        Objects.requireNonNull(
                value,
                "value"
        );

        Objects.requireNonNull(
                channel,
                "channel"
        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        targetNpc
                );

        double before =
                profile.characterValue(
                        value
                );

        double susceptibility =
                socialSusceptibility(
                        targetNpc
                );

        double resistance =
                persuasionResistance(
                        targetNpc
                );

        double conviction =
                convictionResistance(
                        targetNpc,
                        value
                );

        double sourceFactor =
                sourceFactor(
                        targetNpc,
                        sourceNpc,
                        channel
                );

        double effectiveStrength =
                influenceStrength(
                        pressure,
                        channel,
                        susceptibility,
                        resistance,
                        conviction,
                        sourceFactor
                );

        double normalizedTarget =
                clampSigned(
                        targetValue
                );

        double after =
                moveToward(
                        before,
                        normalizedTarget,
                        effectiveStrength
                );

        profile.setCharacterValue(
                value,
                after
        );

        return result(
                before,
                normalizedTarget,
                after,
                susceptibility,
                resistance,
                conviction,
                sourceFactor,
                effectiveStrength
        );
    }

    /*
     * =========================================================
     * SOCIAL-NORM INFLUENCE
     * =========================================================
     */

    public CharacterInfluenceResult applySocialNormPressure(
            NpcId targetNpc,
            CharacterSocialNorm norm,
            double targetValue,
            double pressure,
            CharacterInfluenceChannel channel
    ) {

        return applySocialNormPressure(
                targetNpc,
                null,
                norm,
                targetValue,
                pressure,
                channel
        );
    }

    public CharacterInfluenceResult applySocialNormPressure(
            NpcId targetNpc,
            NpcId sourceNpc,
            CharacterSocialNorm norm,
            double targetValue,
            double pressure,
            CharacterInfluenceChannel channel
    ) {

        requireNpc(
                targetNpc
        );

        validateOptionalSource(
                targetNpc,
                sourceNpc
        );

        Objects.requireNonNull(
                norm,
                "norm"
        );

        Objects.requireNonNull(
                channel,
                "channel"
        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        targetNpc
                );

        double before =
                profile.socialNorm(
                        norm
                );

        double susceptibility =
                socialSusceptibility(
                        targetNpc
                );

        double resistance =
                persuasionResistance(
                        targetNpc
                );

        double conviction =
                generalConvictionResistance(
                        profile,
                        before
                );

        double sourceFactor =
                sourceFactor(
                        targetNpc,
                        sourceNpc,
                        channel
                );

        double effectiveStrength =
                influenceStrength(
                        pressure,
                        channel,
                        susceptibility,
                        resistance,
                        conviction,
                        sourceFactor
                );

        double normalizedTarget =
                clampSigned(
                        targetValue
                );

        double after =
                moveToward(
                        before,
                        normalizedTarget,
                        effectiveStrength
                );

        profile.setSocialNorm(
                norm,
                after
        );

        return result(
                before,
                normalizedTarget,
                after,
                susceptibility,
                resistance,
                conviction,
                sourceFactor,
                effectiveStrength
        );
    }

    /*
     * =========================================================
     * DISPOSITION INFLUENCE
     * =========================================================
     */

    public CharacterInfluenceResult applyDispositionPressure(
            NpcId targetNpc,
            CharacterDisposition disposition,
            double targetValue,
            double pressure,
            CharacterInfluenceChannel channel
    ) {

        return applyDispositionPressure(
                targetNpc,
                null,
                disposition,
                targetValue,
                pressure,
                channel
        );
    }

    public CharacterInfluenceResult applyDispositionPressure(
            NpcId targetNpc,
            NpcId sourceNpc,
            CharacterDisposition disposition,
            double targetValue,
            double pressure,
            CharacterInfluenceChannel channel
    ) {

        requireNpc(
                targetNpc
        );

        validateOptionalSource(
                targetNpc,
                sourceNpc
        );

        Objects.requireNonNull(
                disposition,
                "disposition"
        );

        Objects.requireNonNull(
                channel,
                "channel"
        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        targetNpc
                );

        double before =
                profile.disposition(
                        disposition
                );

        double susceptibility =
                socialSusceptibility(
                        targetNpc
                );

        double resistance =
                persuasionResistance(
                        targetNpc
                );

        /*
         * Dispositions are somewhat more psychologically stable
         * than learned social norms, but still capable of changing
         * over a lifetime.
         */
        double conviction =
                dispositionResistance(
                        profile,
                        before
                );

        double sourceFactor =
                sourceFactor(
                        targetNpc,
                        sourceNpc,
                        channel
                );

        double effectiveStrength =
                influenceStrength(
                        pressure,
                        channel,
                        susceptibility,
                        resistance,
                        conviction,
                        sourceFactor
                );

        double normalizedTarget =
                clampSigned(
                        targetValue
                );

        double after =
                moveToward(
                        before,
                        normalizedTarget,
                        effectiveStrength
                );

        profile.setDisposition(
                disposition,
                after
        );

        return result(
                before,
                normalizedTarget,
                after,
                susceptibility,
                resistance,
                conviction,
                sourceFactor,
                effectiveStrength
        );
    }

    /*
     * =========================================================
     * SOURCE / RELATIONSHIP EFFECT
     * =========================================================
     */

    private double sourceFactor(
            NpcId targetNpc,
            NpcId sourceNpc,
            CharacterInfluenceChannel channel
    ) {

        if (sourceNpc == null) {
            return 1.0;
        }

        NpcRelationship relationship =
                relationships.find(
                                targetNpc,
                                sourceNpc
                        )
                        .orElse(
                                null
                        );

        if (relationship == null) {

            /*
             * An unfamiliar person can still exert influence,
             * but substantially less than somebody known/trusted.
             */
            return 0.45;
        }

        double trust =
                unitSigned(
                        relationship.trust()
                );

        double respect =
                unitSigned(
                        relationship.respect()
                );

        double familiarity =
                relationship.familiarity();

        double fear =
                relationship.fear();

        if (
                channel
                        == CharacterInfluenceChannel.COERCION
        ) {

            return clamp(
                    0.20
                            + fear
                            * 0.45
                            + respect
                            * 0.10
                            + familiarity
                            * 0.10
                            + trust
                            * 0.05,
                    0.10,
                    1.10
            );
        }

        return clamp(
                0.25
                        + trust
                        * 0.30
                        + respect
                        * 0.20
                        + familiarity
                        * 0.15
                        + fear
                        * 0.05,
                0.10,
                1.10
        );
    }

    /*
     * =========================================================
     * INTERNAL RESISTANCE
     * =========================================================
     */

    private static double generalConvictionResistance(
            CharacterProfile profile,
            double currentValue
    ) {

        double result =
                0.12

                        + Math.abs(
                        currentValue
                ) * 0.38

                        + positive(
                        profile.disposition(
                                CharacterDisposition.PERSISTENCE
                        )
                ) * 0.10

                        + positive(
                        profile.disposition(
                                CharacterDisposition.DISCIPLINE
                        )
                ) * 0.08

                        + positive(
                        profile.disposition(
                                CharacterDisposition.INDEPENDENCE
                        )
                ) * 0.07;

        return clamp(
                result,
                0.0,
                0.90
        );
    }

    private static double dispositionResistance(
            CharacterProfile profile,
            double currentValue
    ) {

        double result =
                0.25

                        + Math.abs(
                        currentValue
                ) * 0.35

                        + positive(
                        profile.disposition(
                                CharacterDisposition.DISCIPLINE
                        )
                ) * 0.10

                        + positive(
                        profile.disposition(
                                CharacterDisposition.PERSISTENCE
                        )
                ) * 0.10;

        return clamp(
                result,
                0.0,
                0.92
        );
    }

    /*
     * =========================================================
     * INFLUENCE CALCULATION
     * =========================================================
     */

    private static double influenceStrength(
            double pressure,
            CharacterInfluenceChannel channel,
            double susceptibility,
            double resistance,
            double conviction,
            double sourceFactor
    ) {

        double normalizedPressure =
                clampUnit(
                        pressure
                );

        double result =
                normalizedPressure
                        * channel.strengthMultiplier()
                        * sourceFactor
                        * susceptibility

                        /*
                         * Resistance reduces influence but does not
                         * normally create absolute immunity.
                         */
                        * (
                        1.0
                                - resistance
                                * 0.65
                )

                        * (
                        1.0
                                - conviction
                                * 0.70
                );

        return clamp(
                result,
                0.0,
                1.0
        );
    }

    private static double moveToward(
            double current,
            double target,
            double strength
    ) {

        return clampSigned(
                current
                        +
                        (
                                target
                                        - current
                        )
                                * clampUnit(
                                strength
                        )
        );
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private NpcState requireNpc(
            NpcId npc
    ) {

        Objects.requireNonNull(
                npc,
                "npc"
        );

        return registry.find(
                        npc
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC "
                                                + npc
                                )
                );
    }

    private void validateOptionalSource(
            NpcId targetNpc,
            NpcId sourceNpc
    ) {

        if (sourceNpc == null) {
            return;
        }

        requireNpc(
                sourceNpc
        );

        if (targetNpc.equals(
                sourceNpc
        )) {

            throw new IllegalArgumentException(
                    "Influence source cannot be the same NPC as target"
            );
        }
    }

    private static CharacterInfluenceResult result(
            double before,
            double target,
            double after,
            double susceptibility,
            double resistance,
            double convictionResistance,
            double sourceFactor,
            double effectiveStrength
    ) {

        return new CharacterInfluenceResult(
                before,
                target,
                after,
                after - before,
                susceptibility,
                resistance,
                convictionResistance,
                sourceFactor,
                effectiveStrength
        );
    }

    private static double positive(
            double signed
    ) {

        return Math.max(
                0.0,
                clampSigned(
                        signed
                )
        );
    }

    private static double unitSigned(
            double signed
    ) {

        return (
                clampSigned(
                        signed
                )
                        + 1.0
        ) / 2.0;
    }

    private static double weighted(
            double value1,
            double weight1,
            double value2,
            double weight2,
            double value3,
            double weight3
    ) {

        return weightedInternal(
                new double[]{
                        value1,
                        value2,
                        value3
                },
                new double[]{
                        weight1,
                        weight2,
                        weight3
                }
        );
    }

    private static double weighted(
            double value1,
            double weight1,
            double value2,
            double weight2,
            double value3,
            double weight3,
            double value4,
            double weight4
    ) {

        return weightedInternal(
                new double[]{
                        value1,
                        value2,
                        value3,
                        value4
                },
                new double[]{
                        weight1,
                        weight2,
                        weight3,
                        weight4
                }
        );
    }

    private static double weighted(
            double value1,
            double weight1,
            double value2,
            double weight2,
            double value3,
            double weight3,
            double value4,
            double weight4,
            double value5,
            double weight5
    ) {

        return weightedInternal(
                new double[]{
                        value1,
                        value2,
                        value3,
                        value4,
                        value5
                },
                new double[]{
                        weight1,
                        weight2,
                        weight3,
                        weight4,
                        weight5
                }
        );
    }

    private static double weightedInternal(
            double[] values,
            double[] weights
    ) {

        double total =
                0.0;

        double totalWeight =
                0.0;

        for (
                int index = 0;
                index < values.length;
                index++
        ) {

            total +=
                    clampUnit(
                            values[
                                    index
                                    ]
                    )
                            * weights[
                            index
                            ];

            totalWeight +=
                    weights[
                            index
                            ];
        }

        if (totalWeight <= 0.0) {
            return 0.0;
        }

        return clampUnit(
                total
                        / totalWeight
        );
    }

    private static double clampSigned(
            double value
    ) {

        requireFinite(
                value
        );

        return clamp(
                value,
                -1.0,
                1.0
        );
    }

    private static double clampUnit(
            double value
    ) {

        requireFinite(
                value
        );

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

    private static void requireFinite(
            double value
    ) {

        if (!Double.isFinite(
                value
        )) {

            throw new IllegalArgumentException(
                    "Psychology value must be finite"
            );
        }
    }
}
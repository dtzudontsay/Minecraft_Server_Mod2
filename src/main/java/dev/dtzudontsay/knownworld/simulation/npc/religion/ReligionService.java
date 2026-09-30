package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalExposureWeight;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolution;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolver;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReligionService {

    private static final String DEVOTION =
            "religion.devotion";

    private static final String DOCTRINAL_KNOWLEDGE =
            "religion.doctrinal_knowledge";

    private static final String IDENTITY_IMPORTANCE =
            "religion.identity_importance";

    private static final String TOLERANCE =
            "religion.tolerance";

    private static final String CONVERSION_OPENNESS =
            "religion.conversion_openness";

    private static final String CONCEALMENT_WILLINGNESS =
            "religion.concealment_willingness";

    private static final String SYNCRETISM =
            "religion.syncretism";

    private static final String OBSERVANCE =
            "religion.observance";

    private static final String CONVERSION_MOMENTUM_PREFIX =
            "religion.conversion_momentum.";

    /*
     * Repeated exposure accumulates toward conversion rather than
     * making one conversation instantly rewrite religion.
     */
    private static final double CONVERSION_THRESHOLD =
            1.0;

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final NpcRelationshipManager relationships;

    private final CharacterPsychologyService psychology;

    public ReligionService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
            NpcRelationshipManager relationships,
            CharacterPsychologyService psychology
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

        this.psychology =
                Objects.requireNonNull(
                        psychology,
                        "psychology"
                );
    }

    /*
     * =========================================================
     * PERSONAL FAITH STATE
     * =========================================================
     */

    public PersonalFaithSnapshot snapshot(
            NpcId npc
    ) {

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        return new PersonalFaithSnapshot(
                profile.religion(),
                profile.value(
                        DEVOTION
                ),
                profile.value(
                        DOCTRINAL_KNOWLEDGE
                ),
                profile.value(
                        IDENTITY_IMPORTANCE
                ),
                profile.value(
                        TOLERANCE
                ),
                profile.value(
                        CONVERSION_OPENNESS
                ),
                profile.value(
                        CONCEALMENT_WILLINGNESS
                ),
                profile.value(
                        SYNCRETISM
                ),
                profile.value(
                        OBSERVANCE
                )
        );
    }

    public void initializeFaithState(
            NpcId npc,
            String religionId,
            double devotion,
            double doctrinalKnowledge,
            double identityImportance,
            double tolerance,
            double conversionOpenness,
            double concealmentWillingness,
            double syncretism,
            double observance
    ) {

        validateReligion(
                religionId
        );

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        profile.setReligion(
                religionId
        );

        profile.setValue(
                DEVOTION,
                devotion
        );

        profile.setValue(
                DOCTRINAL_KNOWLEDGE,
                doctrinalKnowledge
        );

        profile.setValue(
                IDENTITY_IMPORTANCE,
                identityImportance
        );

        profile.setValue(
                TOLERANCE,
                tolerance
        );

        profile.setValue(
                CONVERSION_OPENNESS,
                conversionOpenness
        );

        profile.setValue(
                CONCEALMENT_WILLINGNESS,
                concealmentWillingness
        );

        profile.setValue(
                SYNCRETISM,
                syncretism
        );

        profile.setValue(
                OBSERVANCE,
                observance
        );
    }

    public void setDevotion(
            NpcId npc,
            double value
    ) {

        requireProfile(
                npc
        ).setValue(
                DEVOTION,
                value
        );
    }

    public void setDoctrinalKnowledge(
            NpcId npc,
            double value
    ) {

        requireProfile(
                npc
        ).setValue(
                DOCTRINAL_KNOWLEDGE,
                value
        );
    }

    public void setIdentityImportance(
            NpcId npc,
            double value
    ) {

        requireProfile(
                npc
        ).setValue(
                IDENTITY_IMPORTANCE,
                value
        );
    }

    public void setTolerance(
            NpcId npc,
            double value
    ) {

        requireProfile(
                npc
        ).setValue(
                TOLERANCE,
                value
        );
    }

    public void setConversionOpenness(
            NpcId npc,
            double value
    ) {

        requireProfile(
                npc
        ).setValue(
                CONVERSION_OPENNESS,
                value
        );
    }

    public void setConcealmentWillingness(
            NpcId npc,
            double value
    ) {

        requireProfile(
                npc
        ).setValue(
                CONCEALMENT_WILLINGNESS,
                value
        );
    }

    public void setSyncretism(
            NpcId npc,
            double value
    ) {

        requireProfile(
                npc
        ).setValue(
                SYNCRETISM,
                value
        );
    }

    public void setObservance(
            NpcId npc,
            double value
    ) {

        requireProfile(
                npc
        ).setValue(
                OBSERVANCE,
                value
        );
    }

    /*
     * =========================================================
     * REGIONAL RELIGIOUS EXPOSURE
     * =========================================================
     */

    public Optional<String> strongestRegionalReligion(
            double minecraftX,
            double minecraftZ
    ) {

        RegionalInfluenceResolution resolution =
                RegionalInfluenceResolver.resolveMinecraft(
                                minecraftX,
                                minecraftZ
                        )
                        .orElse(
                                null
                        );

        if (resolution == null) {
            return Optional.empty();
        }

        return resolution.effectiveProfile()
                .religionExposure()
                .entrySet()
                .stream()
                .filter(
                        entry ->
                                entry.getValue()
                                        .known()
                )
                .max(
                        Comparator.comparingDouble(
                                entry ->
                                        effectiveExposureWeight(
                                                entry.getValue()
                                        )
                        )
                )
                .map(
                        Map.Entry::getKey
                );
    }

    public double regionalSupport(
            String religionId,
            double minecraftX,
            double minecraftZ
    ) {

        validateReligion(
                religionId
        );

        RegionalInfluenceResolution resolution =
                RegionalInfluenceResolver.resolveMinecraft(
                                minecraftX,
                                minecraftZ
                        )
                        .orElse(
                                null
                        );

        if (resolution == null) {
            return 0.0;
        }

        RegionalExposureWeight exposure =
                resolution.effectiveProfile()
                        .religionExposure()
                        .get(
                                religionId
                        );

        if (exposure == null
                || !exposure.known()) {

            return 0.0;
        }

        return clampUnit(
                exposure.weight()
                        * exposure.confidence()
        );
    }

    /**
     * Applies one event of ambient regional religious pressure.
     *
     * If the dominant regional religion is already the character's
     * religion, this reinforces devotion/identity/observance instead
     * of attempting a meaningless self-conversion.
     */
    public ReligionConversionResult applyRegionalPressure(
            NpcId npc,
            double minecraftX,
            double minecraftZ,
            double pressure
    ) {

        NpcState state =
                requireNpc(
                        npc
                );

        String regionalReligion =
                strongestRegionalReligion(
                        minecraftX,
                        minecraftZ
                )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No known regional religion exposure resolves here"
                                        )
                        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        double support =
                regionalSupport(
                        regionalReligion,
                        minecraftX,
                        minecraftZ
                );

        if (regionalReligion.equals(
                profile.religion()
        )) {

            reinforceCurrentFaith(
                    profile,
                    clampUnit(
                            pressure
                    )
                            * support
            );

            PersonalFaithSnapshot faith =
                    snapshot(
                            npc
                    );

            return new ReligionConversionResult(
                    faith.religionId(),
                    faith.religionId(),
                    clampUnit(
                            pressure
                    ),
                    psychology.socialSusceptibility(
                            npc
                    ),
                    1.0,
                    support,
                    faith.conversionOpenness(),
                    currentFaithResistance(
                            npc
                    ),
                    0.0,
                    0.0,
                    0.0,
                    false
            );
        }

        return applyConversionPressure(
                npc,
                null,
                regionalReligion,
                pressure,
                support
        );
    }

    /*
     * =========================================================
     * DIRECT CONVERSION / PERSUASION
     * =========================================================
     */

    public ReligionConversionResult applyConversionPressure(
            NpcId targetNpc,
            NpcId sourceNpc,
            String targetReligionId,
            double pressure
    ) {

        NpcState target =
                requireNpc(
                        targetNpc
                );

        double support =
                regionalSupport(
                        targetReligionId,
                        target.position()
                                .x(),
                        target.position()
                                .z()
                );

        return applyConversionPressure(
                targetNpc,
                sourceNpc,
                targetReligionId,
                pressure,
                support
        );
    }

    public ReligionConversionResult applyConversionPressure(
            NpcId targetNpc,
            NpcId sourceNpc,
            String targetReligionId,
            double pressure,
            double regionalSupport
    ) {

        requireNpc(
                targetNpc
        );

        validateReligion(
                targetReligionId
        );

        if (sourceNpc != null) {

            requireNpc(
                    sourceNpc
            );

            if (sourceNpc.equals(
                    targetNpc
            )) {

                throw new IllegalArgumentException(
                        "Religion influence source cannot equal target"
                );
            }
        }

        CharacterProfile profile =
                profiles.getOrCreate(
                        targetNpc
                );

        String previousReligion =
                profile.religion();

        if (targetReligionId.equals(
                previousReligion
        )) {

            reinforceCurrentFaith(
                    profile,
                    clampUnit(
                            pressure
                    )
            );

            PersonalFaithSnapshot faith =
                    snapshot(
                            targetNpc
                    );

            return new ReligionConversionResult(
                    previousReligion,
                    targetReligionId,
                    clampUnit(
                            pressure
                    ),
                    psychology.socialSusceptibility(
                            targetNpc
                    ),
                    relationshipSourceFactor(
                            targetNpc,
                            sourceNpc
                    ),
                    clampUnit(
                            regionalSupport
                    ),
                    faith.conversionOpenness(),
                    currentFaithResistance(
                            targetNpc
                    ),
                    0.0,
                    0.0,
                    0.0,
                    false
            );
        }

        PersonalFaithSnapshot faith =
                snapshot(
                        targetNpc
                );

        double susceptibility =
                psychology.socialSusceptibility(
                        targetNpc
                );

        double sourceFactor =
                relationshipSourceFactor(
                        targetNpc,
                        sourceNpc
                );

        double openness =
                effectiveConversionOpenness(
                        targetNpc,
                        faith
                );

        double resistance =
                currentFaithResistance(
                        targetNpc
                );

        double support =
                clampUnit(
                        regionalSupport
                );

        double effectivePressure =
                clampUnit(
                        clampUnit(
                                pressure
                        )

                                * (
                                0.35
                                        + susceptibility
                                        * 0.65
                        )

                                * (
                                0.35
                                        + sourceFactor
                                        * 0.65
                        )

                                * (
                                0.40
                                        + openness
                                        * 0.60
                        )

                                * (
                                0.75
                                        + support
                                        * 0.25
                        )

                                * (
                                1.0
                                        - resistance
                                        * 0.80
                        )
                );

        String momentumKey =
                momentumKey(
                        targetReligionId
                );

        double previousMomentum =
                profile.value(
                        momentumKey
                );

        double newMomentum =
                clampUnit(
                        previousMomentum
                                + effectivePressure
                );

        boolean converted =
                newMomentum
                        >= CONVERSION_THRESHOLD;

        if (converted) {

            convert(
                    profile,
                    targetReligionId,
                    effectivePressure,
                    support
            );

            newMomentum =
                    0.0;
        }

        profile.setValue(
                momentumKey,
                newMomentum
        );

        return new ReligionConversionResult(
                previousReligion,
                targetReligionId,
                clampUnit(
                        pressure
                ),
                susceptibility,
                sourceFactor,
                support,
                openness,
                resistance,
                effectivePressure,
                previousMomentum,
                newMomentum,
                converted
        );
    }

    /*
     * =========================================================
     * RESISTANCE / RECEPTIVENESS
     * =========================================================
     */

    public double currentFaithResistance(
            NpcId npc
    ) {

        PersonalFaithSnapshot faith =
                snapshot(
                        npc
                );

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        double religiosity =
                unitSigned(
                        profile.characterValue(
                                CharacterValue.RELIGIOSITY
                        )
                );

        double result =
                faith.devotion()
                        * 0.30

                        + faith.identityImportance()
                        * 0.25

                        + faith.doctrinalKnowledge()
                        * 0.15

                        + faith.observance()
                        * 0.10

                        + religiosity
                        * 0.15

                        + psychology.persuasionResistance(
                        npc
                ) * 0.05;

        /*
         * Syncretism makes coexistence with outside beliefs easier
         * without necessarily destroying current devotion.
         */
        result -=
                faith.syncretism()
                        * 0.15;

        return clamp(
                result,
                0.0,
                0.95
        );
    }

    public double effectiveConversionOpenness(
            NpcId npc
    ) {

        return effectiveConversionOpenness(
                npc,
                snapshot(
                        npc
                )
        );
    }

    private double effectiveConversionOpenness(
            NpcId npc,
            PersonalFaithSnapshot faith
    ) {

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        double toleranceValue =
                unitSigned(
                        profile.characterValue(
                                CharacterValue.RELIGIOUS_TOLERANCE
                        )
                );

        double openness =
                faith.conversionOpenness()
                        * 0.40

                        + faith.tolerance()
                        * 0.15

                        + faith.syncretism()
                        * 0.15

                        + toleranceValue
                        * 0.10

                        + psychology.socialSusceptibility(
                        npc
                ) * 0.20;

        return clampUnit(
                openness
        );
    }

    /*
     * =========================================================
     * INTERNAL STATE CHANGE
     * =========================================================
     */

    private static void reinforceCurrentFaith(
            CharacterProfile profile,
            double strength
    ) {

        double amount =
                clampUnit(
                        strength
                );

        profile.setValue(
                DEVOTION,
                moveToward(
                        profile.value(
                                DEVOTION
                        ),
                        1.0,
                        amount
                                * 0.20
                )
        );

        profile.setValue(
                IDENTITY_IMPORTANCE,
                moveToward(
                        profile.value(
                                IDENTITY_IMPORTANCE
                        ),
                        1.0,
                        amount
                                * 0.15
                )
        );

        profile.setValue(
                OBSERVANCE,
                moveToward(
                        profile.value(
                                OBSERVANCE
                        ),
                        1.0,
                        amount
                                * 0.15
                )
        );
    }

    private static void convert(
            CharacterProfile profile,
            String targetReligionId,
            double effectivePressure,
            double regionalSupport
    ) {

        profile.setReligion(
                targetReligionId
        );

        /*
         * A convert does not instantly become a theological expert
         * or perfect adherent.
         */
        profile.setValue(
                DEVOTION,
                clamp(
                        0.20
                                + effectivePressure
                                * 0.35,
                        0.20,
                        0.60
                )
        );

        profile.setValue(
                DOCTRINAL_KNOWLEDGE,
                clamp(
                        profile.value(
                                DOCTRINAL_KNOWLEDGE
                        )
                                * 0.30
                                + 0.10,
                        0.05,
                        0.40
                )
        );

        profile.setValue(
                IDENTITY_IMPORTANCE,
                clamp(
                        0.20
                                + effectivePressure
                                * 0.25,
                        0.20,
                        0.50
                )
        );

        profile.setValue(
                OBSERVANCE,
                clamp(
                        0.15
                                + regionalSupport
                                * 0.25,
                        0.10,
                        0.50
                )
        );
    }

    /*
     * =========================================================
     * RELATIONSHIP EFFECT
     * =========================================================
     */

    private double relationshipSourceFactor(
            NpcId targetNpc,
            NpcId sourceNpc
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

            return 0.40;
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

        double affection =
                unitSigned(
                        relationship.affection()
                );

        return clamp(
                0.15
                        + trust
                        * 0.30
                        + respect
                        * 0.25
                        + familiarity
                        * 0.15
                        + affection
                        * 0.15,
                0.10,
                1.10
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

    private CharacterProfile requireProfile(
            NpcId npc
    ) {

        requireNpc(
                npc
        );

        return profiles.getOrCreate(
                npc
        );
    }

    private static void validateReligion(
            String religionId
    ) {

        if (religionId == null
                || religionId.isBlank()) {

            throw new IllegalArgumentException(
                    "Religion ID cannot be empty"
            );
        }

        if (WorldReferenceCatalog.get()
                .religion(
                        religionId
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown religion: "
                            + religionId
            );
        }
    }

    private static double effectiveExposureWeight(
            RegionalExposureWeight exposure
    ) {

        return exposure.weight()
                * exposure.confidence();
    }

    private static String momentumKey(
            String religionId
    ) {

        return CONVERSION_MOMENTUM_PREFIX
                + religionId;
    }

    private static double moveToward(
            double current,
            double target,
            double strength
    ) {

        return clampUnit(
                current
                        + (
                        target - current
                )
                        * clampUnit(
                        strength
                )
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
        ) / 2.0;
    }

    private static double clampUnit(
            double value
    ) {

        if (!Double.isFinite(
                value
        )) {

            throw new IllegalArgumentException(
                    "Religion value must be finite"
            );
        }

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
}
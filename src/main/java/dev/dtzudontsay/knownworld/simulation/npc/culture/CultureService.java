package dev.dtzudontsay.knownworld.simulation.npc.culture;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.family.GenealogyManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.world.reference.ReferenceEntry;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalExposureWeight;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolution;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolver;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CultureService {

    private static final String IDENTITY_IMPORTANCE =
            "culture.identity_importance";

    private static final String HERITAGE_ATTACHMENT =
            "culture.heritage_attachment";

    private static final String ASSIMILATION_OPENNESS =
            "culture.assimilation_openness";

    private static final String MULTICULTURAL_IDENTITY =
            "culture.multicultural_identity";

    private static final String AFFINITY_PREFIX =
            "culture.affinity.";

    private static final String MOMENTUM_PREFIX =
            "culture.transition_momentum.";

    private static final double CULTURE_CHANGE_THRESHOLD =
            1.0;

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final GenealogyManager genealogy;

    private final NpcRelationshipManager relationships;

    private final CharacterPsychologyService psychology;

    private final CultureDistanceService cultureDistance;

    public CultureService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
            GenealogyManager genealogy,
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

        this.genealogy =
                Objects.requireNonNull(
                        genealogy,
                        "genealogy"
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

        this.cultureDistance =
                new CultureDistanceService();
    }

    /*
     * =========================================================
     * IDENTITY STATE
     * =========================================================
     */

    public CulturalIdentitySnapshot snapshot(
            NpcId npc
    ) {

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        Map<String, Double> affinities =
                new LinkedHashMap<>();

        for (
                ReferenceEntry culture :
                WorldReferenceCatalog.get()
                        .cultures()
        ) {

            double affinity =
                    profile.value(
                            affinityKey(
                                    culture.id()
                            )
                    );

            if (affinity > 0.0
                    || culture.id()
                    .equals(
                            profile.culture()
                    )) {

                affinities.put(
                        culture.id(),
                        affinity
                );
            }
        }

        return new CulturalIdentitySnapshot(
                profile.culture(),
                profile.value(
                        IDENTITY_IMPORTANCE
                ),
                profile.value(
                        HERITAGE_ATTACHMENT
                ),
                profile.value(
                        ASSIMILATION_OPENNESS
                ),
                profile.value(
                        MULTICULTURAL_IDENTITY
                ),
                Map.copyOf(
                        affinities
                ),
                profile.languages()
        );
    }

    public void initializeCultureState(
            NpcId npc,
            String cultureId,
            double identityImportance,
            double heritageAttachment,
            double assimilationOpenness,
            double multiculturalIdentity
    ) {

        String normalizedCulture =
                validateCulture(
                        cultureId
                );

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        profile.setCulture(
                normalizedCulture
        );

        profile.setValue(
                IDENTITY_IMPORTANCE,
                identityImportance
        );

        profile.setValue(
                HERITAGE_ATTACHMENT,
                heritageAttachment
        );

        profile.setValue(
                ASSIMILATION_OPENNESS,
                assimilationOpenness
        );

        profile.setValue(
                MULTICULTURAL_IDENTITY,
                multiculturalIdentity
        );

        profile.setValue(
                affinityKey(
                        normalizedCulture
                ),
                1.0
        );
    }

    public double affinity(
            NpcId npc,
            String cultureId
    ) {

        String normalizedCulture =
                validateCulture(
                        cultureId
                );

        return requireProfile(
                npc
        ).value(
                affinityKey(
                        normalizedCulture
                )
        );
    }

    /*
     * =========================================================
     * REGIONAL CULTURE
     * =========================================================
     */

    public Optional<String> strongestRegionalCulture(
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
                .cultureExposure()
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
                                        exposureStrength(
                                                entry.getValue()
                                        )
                        )
                )
                .map(
                        Map.Entry::getKey
                );
    }

    public double regionalCultureSupport(
            String cultureId,
            double minecraftX,
            double minecraftZ
    ) {

        String normalizedCulture =
                validateCulture(
                        cultureId
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
                        .cultureExposure()
                        .get(
                                normalizedCulture
                        );

        if (exposure == null
                || !exposure.known()) {

            return 0.0;
        }

        return exposureStrength(
                exposure
        );
    }

    public int applyRegionalExposure(
            NpcId npc,
            double minecraftX,
            double minecraftZ,
            double pressure
    ) {

        requireNpc(
                npc
        );

        RegionalInfluenceResolution resolution =
                RegionalInfluenceResolver.resolveMinecraft(
                                minecraftX,
                                minecraftZ
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No regional influence resolves at this position"
                                        )
                        );

        double normalizedPressure =
                clampUnit(
                        pressure
                );

        int changed =
                0;

        /*
         * Every known regional culture contributes affinity.
         */
        for (
                Map.Entry<String, RegionalExposureWeight> entry :
                resolution.effectiveProfile()
                        .cultureExposure()
                        .entrySet()
        ) {

            RegionalExposureWeight exposure =
                    entry.getValue();

            if (!exposure.known()) {
                continue;
            }

            String cultureId =
                    validateCulture(
                            entry.getKey()
                    );

            CharacterProfile profile =
                    profiles.getOrCreate(
                            npc
                    );

            double before =
                    profile.value(
                            affinityKey(
                                    cultureId
                            )
                    );

            double strength =
                    normalizedPressure
                            * exposureStrength(
                            exposure
                    )
                            * (
                            0.40
                                    + psychology.socialSusceptibility(
                                    npc
                            )
                                    * 0.60
                    );

            double after =
                    moveToward(
                            before,
                            1.0,
                            strength
                                    * 0.25
                    );

            profile.setValue(
                    affinityKey(
                            cultureId
                    ),
                    after
            );

            if (Math.abs(
                    after - before
            ) > 1.0e-9) {

                changed++;
            }
        }

        /*
         * Languages are learned from actual authored regional
         * language exposure, not guessed from culture names.
         */
        for (
                Map.Entry<String, RegionalExposureWeight> entry :
                resolution.effectiveProfile()
                        .languageExposure()
                        .entrySet()
        ) {

            RegionalExposureWeight exposure =
                    entry.getValue();

            if (!exposure.known()) {
                continue;
            }

            String languageId =
                    validateLanguage(
                            entry.getKey()
                    );

            CharacterProfile profile =
                    profiles.getOrCreate(
                            npc
                    );

            double before =
                    profile.languageProficiency(
                            languageId
                    );

            double after =
                    moveToward(
                            before,
                            1.0,
                            normalizedPressure
                                    * exposureStrength(
                                    exposure
                            )
                                    * 0.20
                    );

            profile.setLanguageProficiency(
                    languageId,
                    after
            );

            if (Math.abs(
                    after - before
            ) > 1.0e-9) {

                changed++;
            }
        }

        String strongest =
                strongestRegionalCulture(
                        minecraftX,
                        minecraftZ
                )
                        .orElse(
                                null
                        );

        if (strongest != null) {

            CultureInfluenceResult result =
                    applyCulturePressureInternal(
                            npc,
                            null,
                            strongest,
                            normalizedPressure,
                            regionalCultureSupport(
                                    strongest,
                                    minecraftX,
                                    minecraftZ
                            ),
                            null
                    );

            if (result.cultureChanged()) {
                changed++;
            }
        }

        return changed;
    }

    /*
     * =========================================================
     * PERSON-TO-PERSON CULTURAL INFLUENCE
     * =========================================================
     */

    public CultureInfluenceResult applyCulturePressure(
            NpcId targetNpc,
            NpcId sourceNpc,
            String targetCultureId,
            double pressure
    ) {

        NpcState target =
                requireNpc(
                        targetNpc
                );

        double regionalSupport =
                regionalCultureSupport(
                        targetCultureId,
                        target.position()
                                .x(),
                        target.position()
                                .z()
                );

        return applyCulturePressureInternal(
                targetNpc,
                sourceNpc,
                targetCultureId,
                pressure,
                regionalSupport,
                null
        );
    }

    public CultureInfluenceResult applyCulturePressure(
            NpcId targetNpc,
            String targetCultureId,
            double pressure
    ) {

        return applyCulturePressure(
                targetNpc,
                null,
                targetCultureId,
                pressure
        );
    }

    /*
     * =========================================================
     * PARENT / FAMILY FORMATION
     * =========================================================
     */

    public int applyParentInfluence(
            NpcId child,
            double pressure
    ) {

        requireNpc(
                child
        );

        Parentage parentage =
                genealogy.parentsOf(
                                child
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "NPC "
                                                        + child
                                                        + " has no biological parentage record"
                                        )
                        );

        int changed =
                0;

        if (parentage.mother() != null) {

            changed +=
                    applyParent(
                            child,
                            parentage.mother(),
                            pressure
                    );
        }

        if (parentage.father() != null) {

            changed +=
                    applyParent(
                            child,
                            parentage.father(),
                            pressure
                    );
        }

        updateMulticulturalIdentity(
                child
        );

        return changed;
    }

    private int applyParent(
            NpcId child,
            NpcId parent,
            double pressure
    ) {

        CharacterProfile parentProfile =
                requireProfile(
                        parent
                );

        if ("unknown".equals(
                parentProfile.culture()
        )) {

            return 0;
        }

        CultureInfluenceResult result =
                applyCulturePressureInternal(
                        child,
                        parent,
                        parentProfile.culture(),
                        pressure,
                        0.0,
                        1.10
                );

        CharacterProfile childProfile =
                requireProfile(
                        child
                );

        /*
         * Parent language transmission.
         *
         * This is not instant fluency; 18F will call formation
         * influence repeatedly/weighted across upbringing.
         */
        for (
                Map.Entry<String, Double> language :
                parentProfile.languages()
                        .entrySet()
        ) {

            double before =
                    childProfile.languageProficiency(
                            language.getKey()
                    );

            double target =
                    language.getValue();

            double after =
                    moveToward(
                            before,
                            target,
                            clampUnit(
                                    pressure
                            )
                                    * 0.25
                    );

            childProfile.setLanguageProficiency(
                    language.getKey(),
                    after
            );
        }

        return result.cultureChanged()
                ? 2
                : 1;
    }

    /*
     * =========================================================
     * CULTURAL PRESSURE CORE
     * =========================================================
     */

    private CultureInfluenceResult applyCulturePressureInternal(
            NpcId targetNpc,
            NpcId sourceNpc,
            String targetCultureId,
            double pressure,
            double regionalSupport,
            Double sourceFactorOverride
    ) {

        requireNpc(
                targetNpc
        );

        String normalizedCulture =
                validateCulture(
                        targetCultureId
                );

        if (sourceNpc != null) {

            requireNpc(
                    sourceNpc
            );

            if (targetNpc.equals(
                    sourceNpc
            )) {

                throw new IllegalArgumentException(
                        "Culture influence source cannot equal target"
                );
            }
        }

        CharacterProfile profile =
                profiles.getOrCreate(
                        targetNpc
                );

        String previousCulture =
                profile.culture();

        double susceptibility =
                psychology.socialSusceptibility(
                        targetNpc
                );

        double sourceFactor =
                sourceFactorOverride == null
                        ? relationshipSourceFactor(
                        targetNpc,
                        sourceNpc
                )
                        : clamp(
                        sourceFactorOverride,
                        0.0,
                        1.20
                );

        double openness =
                effectiveAssimilationOpenness(
                        targetNpc
                );

        double heritageResistance =
                heritageResistance(
                        targetNpc
                );

        double distance =
                cultureDistance(
                        previousCulture,
                        normalizedCulture
                );

        double support =
                clampUnit(
                        regionalSupport
                );

        double previousAffinity =
                profile.value(
                        affinityKey(
                                normalizedCulture
                        )
                );

        if (normalizedCulture.equals(
                previousCulture
        )) {

            double newAffinity =
                    moveToward(
                            previousAffinity,
                            1.0,
                            clampUnit(
                                    pressure
                            )
                                    * 0.25
                    );

            profile.setValue(
                    affinityKey(
                            normalizedCulture
                    ),
                    newAffinity
            );

            reinforcePrimaryCulture(
                    profile,
                    pressure
            );

            return new CultureInfluenceResult(
                    previousCulture,
                    normalizedCulture,
                    clampUnit(
                            pressure
                    ),
                    susceptibility,
                    sourceFactor,
                    support,
                    openness,
                    heritageResistance,
                    0.0,
                    0.0,
                    previousAffinity,
                    newAffinity,
                    0.0,
                    0.0,
                    false
            );
        }

        /*
         * Different culture.
         *
         * Cultural distance does not make contact impossible, but
         * related cultures generally assimilate more easily.
         */
        double distanceFactor =
                1.0
                        - distance
                        * 0.45;

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
                                0.35
                                        + openness
                                        * 0.65
                        )

                                * (
                                0.80
                                        + support
                                        * 0.20
                        )

                                * distanceFactor

                                * (
                                1.0
                                        - heritageResistance
                                        * 0.70
                        )
                );

        double newAffinity =
                moveToward(
                        previousAffinity,
                        1.0,
                        effectivePressure
                                * 0.45
                );

        profile.setValue(
                affinityKey(
                        normalizedCulture
                ),
                newAffinity
        );

        String momentumKey =
                momentumKey(
                        normalizedCulture
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

        boolean changed =
                newMomentum
                        >= CULTURE_CHANGE_THRESHOLD;

        if (changed) {

            changePrimaryCulture(
                    profile,
                    previousCulture,
                    normalizedCulture,
                    newAffinity
            );

            newMomentum =
                    0.0;
        }

        profile.setValue(
                momentumKey,
                newMomentum
        );

        updateMulticulturalIdentity(
                targetNpc
        );

        return new CultureInfluenceResult(
                previousCulture,
                normalizedCulture,
                clampUnit(
                        pressure
                ),
                susceptibility,
                sourceFactor,
                support,
                openness,
                heritageResistance,
                distance,
                effectivePressure,
                previousAffinity,
                newAffinity,
                previousMomentum,
                newMomentum,
                changed
        );
    }

    /*
     * =========================================================
     * IDENTITY / RESISTANCE
     * =========================================================
     */

    public double effectiveAssimilationOpenness(
            NpcId npc
    ) {

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        double explicit =
                profile.value(
                        ASSIMILATION_OPENNESS
                );

        double culturalTolerance =
                unitSigned(
                        profile.characterValue(
                                CharacterValue.CULTURAL_TOLERANCE
                        )
                );

        double cosmopolitanism =
                unitSigned(
                        profile.socialNorm(
                                CharacterSocialNorm.COSMOPOLITAN
                        )
                );

        double isolationism =
                unitSigned(
                        profile.socialNorm(
                                CharacterSocialNorm.ISOLATIONIST
                        )
                );

        return clampUnit(
                explicit
                        * 0.35

                        + culturalTolerance
                        * 0.20

                        + cosmopolitanism
                        * 0.15

                        + psychology.socialSusceptibility(
                        npc
                )
                        * 0.20

                        + (
                        1.0
                                - isolationism
                )
                        * 0.10
        );
    }

    public double heritageResistance(
            NpcId npc
    ) {

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        double identity =
                profile.value(
                        IDENTITY_IMPORTANCE
                );

        double heritage =
                profile.value(
                        HERITAGE_ATTACHMENT
                );

        double traditionalism =
                unitSigned(
                        profile.characterValue(
                                CharacterValue.TRADITIONALISM
                        )
                );

        double independence =
                psychology.persuasionResistance(
                        npc
                );

        return clamp(
                identity
                        * 0.30

                        + heritage
                        * 0.35

                        + traditionalism
                        * 0.20

                        + independence
                        * 0.15,
                0.0,
                0.95
        );
    }

    private void updateMulticulturalIdentity(
            NpcId npc
    ) {

        CharacterProfile profile =
                requireProfile(
                        npc
                );

        long meaningfulAffinities =
                WorldReferenceCatalog.get()
                        .cultures()
                        .stream()
                        .filter(
                                culture ->
                                        profile.value(
                                                affinityKey(
                                                        culture.id()
                                                )
                                        ) >= 0.45
                        )
                        .count();

        double target =
                meaningfulAffinities >= 2
                        ? Math.min(
                        1.0,
                        0.35
                                + (
                                meaningfulAffinities - 2
                        )
                                * 0.15
                )
                        : 0.0;

        profile.setValue(
                MULTICULTURAL_IDENTITY,
                moveToward(
                        profile.value(
                                MULTICULTURAL_IDENTITY
                        ),
                        target,
                        0.20
                )
        );
    }

    private static void reinforcePrimaryCulture(
            CharacterProfile profile,
            double pressure
    ) {

        double normalized =
                clampUnit(
                        pressure
                );

        profile.setValue(
                IDENTITY_IMPORTANCE,
                moveToward(
                        profile.value(
                                IDENTITY_IMPORTANCE
                        ),
                        1.0,
                        normalized
                                * 0.08
                )
        );

        profile.setValue(
                HERITAGE_ATTACHMENT,
                moveToward(
                        profile.value(
                                HERITAGE_ATTACHMENT
                        ),
                        1.0,
                        normalized
                                * 0.05
                )
        );
    }

    private static void changePrimaryCulture(
            CharacterProfile profile,
            String previousCulture,
            String targetCulture,
            double targetAffinity
    ) {

        if (!"unknown".equals(
                previousCulture
        )) {

            /*
             * Previous cultural identity does not disappear when
             * primary identification changes.
             */
            profile.setValue(
                    affinityKey(
                            previousCulture
                    ),
                    Math.max(
                            profile.value(
                                    affinityKey(
                                            previousCulture
                                    )
                            ),
                            0.50
                    )
            );
        }

        profile.setCulture(
                targetCulture
        );

        profile.setValue(
                affinityKey(
                        targetCulture
                ),
                Math.max(
                        targetAffinity,
                        0.70
                )
        );

        profile.setValue(
                IDENTITY_IMPORTANCE,
                moveToward(
                        profile.value(
                                IDENTITY_IMPORTANCE
                        ),
                        0.55,
                        0.25
                )
        );

        profile.setValue(
                HERITAGE_ATTACHMENT,
                moveToward(
                        profile.value(
                                HERITAGE_ATTACHMENT
                        ),
                        0.45,
                        0.20
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

        double affection =
                unitSigned(
                        relationship.affection()
                );

        double familiarity =
                relationship.familiarity();

        return clamp(
                0.15
                        + trust
                        * 0.25
                        + respect
                        * 0.20
                        + affection
                        * 0.20
                        + familiarity
                        * 0.20,
                0.10,
                1.10
        );
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private double cultureDistance(
            String first,
            String second
    ) {

        if ("unknown".equals(
                first
        )) {

            /*
             * No established identity means no cultural-distance
             * penalty, without pretending that UNKNOWN is a culture.
             */
            return 0.0;
        }

        return cultureDistance.distance(
                first,
                second
        );
    }

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

    private static String validateCulture(
            String cultureId
    ) {

        if (cultureId == null
                || cultureId.isBlank()) {

            throw new IllegalArgumentException(
                    "Culture ID cannot be empty"
            );
        }

        ReferenceEntry culture =
                WorldReferenceCatalog.get()
                        .culture(
                                cultureId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown culture "
                                                        + cultureId
                                        )
                        );

        return culture.id();
    }

    private static String validateLanguage(
            String languageId
    ) {

        if (languageId == null
                || languageId.isBlank()) {

            throw new IllegalArgumentException(
                    "Language ID cannot be empty"
            );
        }

        ReferenceEntry language =
                WorldReferenceCatalog.get()
                        .language(
                                languageId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown language "
                                                        + languageId
                                        )
                        );

        return language.id();
    }

    private static double exposureStrength(
            RegionalExposureWeight exposure
    ) {

        return clampUnit(
                exposure.weight()
                        * exposure.confidence()
        );
    }

    private static String affinityKey(
            String cultureId
    ) {

        return AFFINITY_PREFIX
                + cultureId.toLowerCase(
                Locale.ROOT
        );
    }

    private static String momentumKey(
            String cultureId
    ) {

        return MOMENTUM_PREFIX
                + cultureId.toLowerCase(
                Locale.ROOT
        );
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
                    "Culture value must be finite"
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
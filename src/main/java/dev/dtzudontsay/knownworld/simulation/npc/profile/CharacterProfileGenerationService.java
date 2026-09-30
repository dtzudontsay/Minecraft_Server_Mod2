package dev.dtzudontsay.knownworld.simulation.npc.profile;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class CharacterProfileGenerationService {

    private static final double DISPOSITION_VARIATION =
            0.15;

    private static final double APTITUDE_VARIATION =
            0.15;

    private static final double VALUE_VARIATION =
            0.10;

    private static final double SOCIAL_NORM_VARIATION =
            0.08;

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    public CharacterProfileGenerationService(
            NpcRegistry registry,
            CharacterProfileManager profiles
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
    }

    public CharacterProfile createChildProfile(
            NpcId child,
            NpcId mother,
            NpcId father,
            long seedValue
    ) {

        NpcState childState =
                requireNpc(
                        child
                );

        CharacterProfile motherProfile =
                profiles.getOrCreate(
                        mother
                );

        CharacterProfile fatherProfile =
                father == null
                        ? null
                        : profiles.getOrCreate(
                        father
                );

        Random random =
                randomFor(
                        child,
                        mother,
                        father,
                        seedValue
                );

        CharacterProfile childProfile =
                new CharacterProfile(
                        child,
                        inheritIdentity(
                                motherProfile.culture(),
                                fatherProfile == null
                                        ? null
                                        : fatherProfile.culture(),
                                random
                        ),
                        inheritIdentity(
                                motherProfile.religion(),
                                fatherProfile == null
                                        ? null
                                        : fatherProfile.religion(),
                                random
                        ),
                        "childhood",
                        dialogueFromPersonality(
                                childState.personality()
                        )
                );

        initializeDispositionFromPersonality(
                childProfile,
                childState.personality()
        );

        inheritDispositions(
                childProfile,
                motherProfile,
                fatherProfile,
                random
        );

        inheritAptitudes(
                childProfile,
                motherProfile,
                fatherProfile,
                random
        );

        inheritCharacterValues(
                childProfile,
                motherProfile,
                fatherProfile,
                random
        );

        inheritSocialNorms(
                childProfile,
                motherProfile,
                fatherProfile,
                random
        );

        inheritLegacyValues(
                childProfile,
                motherProfile,
                fatherProfile,
                random
        );

        inheritLanguages(
                childProfile,
                motherProfile,
                fatherProfile
        );

        initializeTraitsFromPersonality(
                childProfile,
                childState.personality()
        );

        /*
         * Learned competence is not genetically inherited.
         *
         * The child receives aptitude, while actual skill starts
         * at zero and develops through education/experience.
         */
        for (
                CharacterSkill skill :
                CharacterSkill.values()
        ) {

            childProfile.setSkill(
                    skill,
                    0.0
            );
        }

        childProfile.setOrientation(
                CharacterOrientation.UNSPECIFIED
        );

        childProfile.setHealthState(
                CharacterHealthState.HEALTHY
        );

        childProfile.setLegalStatus(
                CharacterLegalStatus.UNSPECIFIED
        );

        childProfile.setBirthplaceLocationId(
                "unknown"
        );

        childProfile.setUpbringingLocationId(
                "unknown"
        );

        childProfile.setWealth(
                0.0
        );

        childProfile.setSocialStatus(
                0.0
        );

        childProfile.setReputation(
                0.0
        );

        childProfile.addMotivation(
                "grow_and_learn"
        );

        childProfile.addGoal(
                "survive_childhood"
        );

        profiles.registerLoaded(
                childProfile
        );

        return childProfile;
    }

    private static void initializeDispositionFromPersonality(
            CharacterProfile profile,
            NpcPersonality personality
    ) {

        profile.setDisposition(
                CharacterDisposition.ASSERTIVENESS,
                personality.courage()
                        * 0.45
                        +
                        personality.ambition()
                                * 0.25
        );

        profile.setDisposition(
                CharacterDisposition.IMPULSIVITY,
                -personality.patience()
        );

        profile.setDisposition(
                CharacterDisposition.DISCIPLINE,
                personality.patience()
                        * 0.55
                        +
                        personality.honor()
                                * 0.20
        );

        profile.setDisposition(
                CharacterDisposition.EMPATHY,
                personality.compassion()
        );

        profile.setDisposition(
                CharacterDisposition.AGGRESSION,
                personality.courage()
                        * 0.25
                        -
                        personality.compassion()
                                * 0.35
        );

        profile.setDisposition(
                CharacterDisposition.DOMINANCE,
                personality.ambition()
                        * 0.50
                        +
                        personality.courage()
                                * 0.20
        );

        profile.setDisposition(
                CharacterDisposition.CONFORMITY,
                personality.sociability()
                        * 0.10
                        -
                        personality.ambition()
                                * 0.10
        );

        profile.setDisposition(
                CharacterDisposition.INDEPENDENCE,
                personality.ambition()
                        * 0.15
                        +
                        personality.courage()
                                * 0.15
        );

        profile.setDisposition(
                CharacterDisposition.RISK_TOLERANCE,
                personality.courage()
                        * 0.60
        );

        profile.setDisposition(
                CharacterDisposition.PERSISTENCE,
                personality.patience()
                        * 0.40
                        +
                        personality.ambition()
                                * 0.25
        );
    }

    private static void inheritDispositions(
            CharacterProfile child,
            CharacterProfile mother,
            CharacterProfile father,
            Random random
    ) {

        for (
                CharacterDisposition disposition :
                CharacterDisposition.values()
        ) {

            double parentValue =
                    parentAverageSigned(
                            mother.disposition(
                                    disposition
                            ),
                            father == null
                                    ? null
                                    : father.disposition(
                                    disposition
                            )
                    );

            double inherited =
                    parentValue
                            +
                            signedVariation(
                                    random,
                                    DISPOSITION_VARIATION
                            );

            double temperamentBase =
                    child.disposition(
                            disposition
                    );

            child.setDisposition(
                    disposition,
                    temperamentBase
                            * 0.55
                            +
                            inherited
                                    * 0.45
            );
        }
    }

    private static void inheritAptitudes(
            CharacterProfile child,
            CharacterProfile mother,
            CharacterProfile father,
            Random random
    ) {

        for (
                CharacterAptitude aptitude :
                CharacterAptitude.values()
        ) {

            double motherValue =
                    mother.aptitude(
                            aptitude
                    );

            double fatherValue =
                    father == null
                            ? motherValue
                            : father.aptitude(
                            aptitude
                    );

            double inherited;

            if (
                    motherValue == 0.0
                            &&
                            fatherValue == 0.0
            ) {

                inherited =
                        0.35
                                +
                                random.nextDouble()
                                        * 0.30;

            } else {

                inherited =
                        (
                                motherValue
                                        +
                                        fatherValue
                        ) / 2.0;

                inherited +=
                        signedVariation(
                                random,
                                APTITUDE_VARIATION
                        );
            }

            child.setAptitude(
                    aptitude,
                    inherited
            );
        }
    }

    private static void inheritCharacterValues(
            CharacterProfile child,
            CharacterProfile mother,
            CharacterProfile father,
            Random random
    ) {

        for (
                CharacterValue value :
                CharacterValue.values()
        ) {

            double parentValue =
                    parentAverageSigned(
                            mother.characterValue(
                                    value
                            ),
                            father == null
                                    ? null
                                    : father.characterValue(
                                    value
                            )
                    );

            /*
             * Values are learned much more strongly during
             * upbringing than biologically inherited.
             */
            child.setCharacterValue(
                    value,
                    (
                            parentValue
                                    +
                                    signedVariation(
                                            random,
                                            VALUE_VARIATION
                                    )
                    )
                            * 0.25
            );
        }
    }

    private static void inheritSocialNorms(
            CharacterProfile child,
            CharacterProfile mother,
            CharacterProfile father,
            Random random
    ) {

        for (
                CharacterSocialNorm norm :
                CharacterSocialNorm.values()
        ) {

            double parentValue =
                    parentAverageSigned(
                            mother.socialNorm(
                                    norm
                            ),
                            father == null
                                    ? null
                                    : father.socialNorm(
                                    norm
                            )
                    );

            child.setSocialNorm(
                    norm,
                    (
                            parentValue
                                    +
                                    signedVariation(
                                            random,
                                            SOCIAL_NORM_VARIATION
                                    )
                    )
                            * 0.35
            );
        }
    }

    private static void inheritLegacyValues(
            CharacterProfile child,
            CharacterProfile mother,
            CharacterProfile father,
            Random random
    ) {

        for (
                Map.Entry<String, Double> entry :
                mother.values()
                        .entrySet()
        ) {

            String key =
                    entry.getKey();

            double motherValue =
                    entry.getValue();

            double fatherValue =
                    father == null
                            ? motherValue
                            : father.values()
                            .getOrDefault(
                                    key,
                                    motherValue
                            );

            child.setValue(
                    key,
                    (
                            motherValue
                                    +
                                    fatherValue
                    ) / 2.0
                            +
                            signedVariation(
                                    random,
                                    VALUE_VARIATION
                            )
            );
        }

        if (father != null) {

            for (
                    Map.Entry<String, Double> entry :
                    father.values()
                            .entrySet()
            ) {

                if (
                        !child.values()
                                .containsKey(
                                        entry.getKey()
                                )
                ) {

                    child.setValue(
                            entry.getKey(),
                            entry.getValue()
                    );
                }
            }
        }
    }

    private static void inheritLanguages(
            CharacterProfile child,
            CharacterProfile mother,
            CharacterProfile father
    ) {

        for (
                Map.Entry<String, Double> entry :
                mother.languages()
                        .entrySet()
        ) {

            double fatherKnowledge =
                    father == null
                            ? 0.0
                            : father.languages()
                            .getOrDefault(
                                    entry.getKey(),
                                    0.0
                            );

            /*
             * This is exposure potential only.
             *
             * A newborn does not literally speak the language.
             * 18F upbringing will turn exposure into proficiency.
             */
            double exposure =
                    Math.max(
                            entry.getValue(),
                            fatherKnowledge
                    );

            child.setLanguageProficiency(
                    entry.getKey(),
                    exposure
                            * 0.05
            );
        }

        if (father != null) {

            for (
                    Map.Entry<String, Double> entry :
                    father.languages()
                            .entrySet()
            ) {

                if (
                        !child.languages()
                                .containsKey(
                                        entry.getKey()
                                )
                ) {

                    child.setLanguageProficiency(
                            entry.getKey(),
                            entry.getValue()
                                    * 0.05
                    );
                }
            }
        }
    }

    private static void initializeTraitsFromPersonality(
            CharacterProfile profile,
            NpcPersonality personality
    ) {

        if (personality.courage() >= 0.55) {
            profile.addTrait(
                    "brave"
            );
        }

        if (personality.courage() <= -0.55) {
            profile.addTrait(
                    "cautious"
            );
        }

        if (personality.ambition() >= 0.55) {
            profile.addTrait(
                    "ambitious"
            );
        }

        if (personality.compassion() >= 0.55) {
            profile.addTrait(
                    "compassionate"
            );
        }

        if (personality.compassion() <= -0.55) {
            profile.addTrait(
                    "callous"
            );
        }

        if (personality.honor() >= 0.65) {
            profile.addTrait(
                    "honorable"
            );
        }

        if (personality.patience() >= 0.55) {
            profile.addTrait(
                    "patient"
            );
        }

        if (personality.patience() <= -0.55) {
            profile.addTrait(
                    "impatient"
            );
        }

        if (personality.sociability() >= 0.55) {
            profile.addTrait(
                    "gregarious"
            );
        }

        if (personality.sociability() <= -0.55) {
            profile.addTrait(
                    "reserved"
            );
        }
    }

    private static DialoguePersona dialogueFromPersonality(
            NpcPersonality personality
    ) {

        return new DialoguePersona(
                personality.honor()
                        * 0.35,

                personality.sociability()
                        * 0.30,

                personality.compassion()
                        * 0.65,

                personality.courage()
                        * 0.35
                        -
                        personality.patience()
                                * 0.10,

                "",
                ""
        );
    }

    private static double parentAverageSigned(
            double mother,
            Double father
    ) {

        if (father == null) {
            return mother;
        }

        return (
                mother
                        +
                        father
        ) / 2.0;
    }

    private static double signedVariation(
            Random random,
            double magnitude
    ) {

        return (
                random.nextDouble()
                        * 2.0
                        -
                        1.0
        )
                * magnitude;
    }

    private NpcState requireNpc(
            NpcId npc
    ) {

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

    private static String inheritIdentity(
            String mother,
            String father,
            Random random
    ) {

        if (
                father == null
                        ||
                        "unknown".equals(
                                father
                        )
        ) {

            return mother;
        }

        if ("unknown".equals(
                mother
        )) {

            return father;
        }

        if (mother.equals(
                father
        )) {

            return mother;
        }

        /*
         * Temporary household-inheritance weighting.
         *
         * 18D/18E/18F will replace this with actual household,
         * religion, culture and upbringing influence.
         */
        return random.nextDouble()
                < 0.65
                ? mother
                : father;
    }

    private static Random randomFor(
            NpcId child,
            NpcId mother,
            NpcId father,
            long seedValue
    ) {

        long seed =
                seedValue;

        seed ^=
                child.value()
                        *
                        0x9E3779B97F4A7C15L;

        seed ^=
                mother.value()
                        *
                        0xBF58476D1CE4E5B9L;

        if (father != null) {

            seed ^=
                    father.value()
                            *
                            0x94D049BB133111EBL;
        }

        return new Random(
                seed
        );
    }
}
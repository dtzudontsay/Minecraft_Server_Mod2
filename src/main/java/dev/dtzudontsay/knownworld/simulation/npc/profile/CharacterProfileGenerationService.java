package dev.dtzudontsay.knownworld.simulation.npc.profile;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class CharacterProfileGenerationService {

    private static final double VALUE_VARIATION =
            0.10;

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

        String culture =
                inheritIdentity(
                        motherProfile.culture(),
                        fatherProfile == null
                                ? null
                                : fatherProfile.culture(),
                        random
                );

        String religion =
                inheritIdentity(
                        motherProfile.religion(),
                        fatherProfile == null
                                ? null
                                : fatherProfile.religion(),
                        random
                );

        DialoguePersona dialogue =
                dialogueFromPersonality(
                        childState.personality()
                );

        CharacterProfile childProfile =
                new CharacterProfile(
                        child,
                        culture,
                        religion,
                        "childhood",
                        dialogue
                );

        inheritValues(
                childProfile,
                motherProfile,
                fatherProfile,
                random
        );

        initializeTraitsFromPersonality(
                childProfile,
                childState.personality()
        );

        /*
         * A newborn does not inherit actual learned competence.
         *
         * Skills begin very low and will later be developed by
         * childhood, education, training and experience systems.
         */
        for (
                CharacterSkill skill :
                CharacterSkill.values()
        ) {

            double parentPotential =
                    parentalSkillAverage(
                            skill,
                            motherProfile,
                            fatherProfile
                    );

            double startingSkill =
                    parentPotential
                            * 0.10
                            * (
                            0.75
                                    + random.nextDouble()
                                    * 0.50
                    );

            childProfile.setSkill(
                    skill,
                    startingSkill
            );
        }

        childProfile.addMotivation(
                "grow_and_learn"
        );

        profiles.registerLoaded(
                childProfile
        );

        return childProfile;
    }

    private static void inheritValues(
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

            double inherited =
                    (
                            motherValue
                                    + fatherValue
                    )
                            / 2.0;

            inherited +=
                    (
                            random.nextDouble()
                                    * 2.0
                                    - 1.0
                    )
                            * VALUE_VARIATION;

            child.setValue(
                    key,
                    inherited
            );
        }

        if (father != null) {

            for (
                    Map.Entry<String, Double> entry :
                    father.values()
                            .entrySet()
            ) {

                if (child.values()
                        .containsKey(
                                entry.getKey()
                        )) {

                    continue;
                }

                child.setValue(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }
    }

    private static void initializeTraitsFromPersonality(
            CharacterProfile profile,
            NpcPersonality personality
    ) {
        if (personality.courage()
                >= 0.55) {

            profile.addTrait(
                    "brave"
            );
        }

        if (personality.courage()
                <= -0.55) {

            profile.addTrait(
                    "cautious"
            );
        }

        if (personality.ambition()
                >= 0.55) {

            profile.addTrait(
                    "ambitious"
            );
        }

        if (personality.compassion()
                >= 0.55) {

            profile.addTrait(
                    "compassionate"
            );
        }

        if (personality.compassion()
                <= -0.55) {

            profile.addTrait(
                    "callous"
            );
        }

        if (personality.honor()
                >= 0.65) {

            profile.addTrait(
                    "honorable"
            );
        }

        if (personality.patience()
                >= 0.55) {

            profile.addTrait(
                    "patient"
            );
        }

        if (personality.patience()
                <= -0.55) {

            profile.addTrait(
                    "impatient"
            );
        }

        if (personality.sociability()
                >= 0.55) {

            profile.addTrait(
                    "gregarious"
            );
        }

        if (personality.sociability()
                <= -0.55) {

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
                        - personality.patience()
                        * 0.10,
                "",
                ""
        );
    }

    private static double parentalSkillAverage(
            CharacterSkill skill,
            CharacterProfile mother,
            CharacterProfile father
    ) {
        if (father == null) {
            return mother.skill(
                    skill
            );
        }

        return (
                mother.skill(
                        skill
                )
                        + father.skill(
                        skill
                )
        )
                / 2.0;
    }

    private static String inheritIdentity(
            String mother,
            String father,
            Random random
    ) {
        if (father == null
                || "unknown".equals(
                father
        )) {

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

        return random.nextDouble()
                < 0.65
                ? mother
                : father;
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
                                        "Unknown NPC ID: "
                                                + npc
                                )
                );
    }

    private static Random randomFor(
            NpcId child,
            NpcId mother,
            NpcId father,
            long seedValue
    ) {
        long seed =
                seedValue;

        seed =
                seed * 31L
                        + child.value();

        seed =
                seed * 31L
                        + mother.value();

        seed =
                seed * 31L
                        + (
                        father == null
                                ? 0L
                                : father.value()
                );

        seed ^=
                0x6A09E667F3BCC909L;

        return new Random(
                seed
        );
    }
}
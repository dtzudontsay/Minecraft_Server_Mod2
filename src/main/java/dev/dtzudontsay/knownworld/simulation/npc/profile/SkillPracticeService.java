package dev.dtzudontsay.knownworld.simulation.npc.profile;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;

import java.util.Objects;

/**
 * Runtime learned-skill progression.
 *
 * This service is deliberately independent from any one gameplay system.
 *
 * Social simulation, combat, hunting, trade, education, medicine,
 * administration, sailing, crafting and other systems can all call:
 *
 *      practice(...)
 *
 * when an NPC genuinely exercises a skill.
 *
 * Skill represents learned competence.
 * Aptitude represents learning potential.
 */
public final class SkillPracticeService {

    /*
     * One ordinary practice opportunity should move a skill only slightly.
     *
     * This value is intentionally conservative because NPCs may accumulate
     * thousands of practice opportunities across a lifetime.
     */
    private static final double BASE_PRACTICE_GAIN =
            0.0020;

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    public SkillPracticeService(
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

    /**
     * Applies one learning opportunity.
     *
     * intensity:
     *
     * 0.0 = no meaningful practice
     * 1.0 = strong/full practice opportunity
     */
    public Result practice(
            NpcId npc,
            CharacterSkill skill,
            double intensity
    ) {

        Objects.requireNonNull(
                npc,
                "npc"
        );

        Objects.requireNonNull(
                skill,
                "skill"
        );

        if (!Double.isFinite(
                intensity
        )
                || intensity < 0.0
                || intensity > 1.0) {

            throw new IllegalArgumentException(
                    "Skill-practice intensity must be between 0.0 and 1.0"
            );
        }

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

            return new Result(
                    npc,
                    skill,
                    0.0,
                    0.0,
                    0.0,
                    false
            );
        }

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        double before =
                profile.skill(
                        skill
                );

        if (intensity <= 0.0
                || before >= 1.0) {

            return new Result(
                    npc,
                    skill,
                    before,
                    before,
                    0.0,
                    false
            );
        }

        CharacterAptitude aptitude =
                aptitudeFor(
                        skill
                );

        double naturalAptitude =
                profile.aptitude(
                        aptitude
                );

        /*
         * General learning aptitude contributes a smaller secondary bonus
         * to every learnable activity.
         */
        double generalLearning =
                profile.aptitude(
                        CharacterAptitude.LEARNING
                );

        /*
         * Effective learning potential remains non-zero even for somebody
         * with poor aptitude.
         */
        double learningFactor =
                0.30
                        +
                        naturalAptitude
                                * 0.55
                        +
                        generalLearning
                                * 0.15;

        /*
         * Diminishing returns.
         *
         * Going from 0.10 -> 0.20 should be much easier than
         * going from 0.90 -> 1.00.
         */
        double remainingCapacity =
                1.0
                        - before;

        double diminishingReturns =
                Math.pow(
                        remainingCapacity,
                        1.35
                );

        double gain =
                BASE_PRACTICE_GAIN
                        * intensity
                        * learningFactor
                        * diminishingReturns;

        double after =
                clampUnit(
                        before
                                + gain
                );

        profile.setSkill(
                skill,
                after
        );

        return new Result(
                npc,
                skill,
                before,
                after,
                after
                        - before,
                after
                        > before
        );
    }

    /**
     * Maps learned competence to its underlying aptitude.
     */
    public static CharacterAptitude aptitudeFor(
            CharacterSkill skill
    ) {

        return switch (
                Objects.requireNonNull(
                        skill,
                        "skill"
                )
                ) {

            case MARTIAL ->
                    CharacterAptitude.MARTIAL;

            case DIPLOMACY ->
                    CharacterAptitude.DIPLOMACY;

            case STEWARDSHIP ->
                    CharacterAptitude.STEWARDSHIP;

            case INTRIGUE ->
                    CharacterAptitude.INTRIGUE;

            case LEARNING ->
                    CharacterAptitude.LEARNING;

            case COMBAT ->
                    CharacterAptitude.COMBAT;

            case LEADERSHIP ->
                    CharacterAptitude.LEADERSHIP;

            case RIDING ->
                    CharacterAptitude.RIDING;

            case SAILING ->
                    CharacterAptitude.SAILING;

            case NAVIGATION ->
                    CharacterAptitude.NAVIGATION;

            case TRADE ->
                    CharacterAptitude.TRADE;

            case CRAFTING ->
                    CharacterAptitude.CRAFTING;

            case AGRICULTURE ->
                    CharacterAptitude.AGRICULTURE;

            case HUNTING ->
                    CharacterAptitude.HUNTING;

            case MEDICINE ->
                    CharacterAptitude.MEDICINE;

            case RELIGIOUS_STUDY ->
                    CharacterAptitude.RELIGIOUS_STUDY;

            case LANGUAGE ->
                    CharacterAptitude.LANGUAGES;

            case PERFORMANCE ->
                    CharacterAptitude.PERFORMANCE;

            case COMMAND ->
                    CharacterAptitude.COMMAND;

            case LOGISTICS ->
                    CharacterAptitude.LOGISTICS;
        };
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

    public record Result(
            NpcId npc,
            CharacterSkill skill,
            double before,
            double after,
            double gained,
            boolean changed
    ) {
    }
}
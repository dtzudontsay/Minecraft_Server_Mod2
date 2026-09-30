package dev.dtzudontsay.knownworld.simulation.social.relationship;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentity;

import java.util.Objects;

public final class EffectiveRelationshipResolver {

    private final NpcSimulation simulation;

    private final SocietyStructureRuntime society;

    private final DynastyRelationshipManager dynastyRelationships;

    public EffectiveRelationshipResolver(
            NpcSimulation simulation,
            SocietyStructureRuntime society,
            DynastyRelationshipManager dynastyRelationships
    ) {

        this.simulation =
                Objects.requireNonNull(
                        simulation,
                        "simulation"
                );

        this.society =
                Objects.requireNonNull(
                        society,
                        "society"
                );

        this.dynastyRelationships =
                Objects.requireNonNull(
                        dynastyRelationships,
                        "dynastyRelationships"
                );
    }

    /**
     * Returns the best currently available relationship view.
     *
     * IMPORTANT:
     *
     * Fallback values are NOT written into NpcRelationshipManager.
     *
     * They are contextual starting opinions only. Once two NPCs build
     * personal history, their explicit personal relationship takes
     * precedence automatically.
     */
    public EffectiveRelationship resolve(
            NpcId subject,
            NpcId target
    ) {

        Objects.requireNonNull(
                subject,
                "subject"
        );

        Objects.requireNonNull(
                target,
                "target"
        );

        if (subject.equals(
                target
        )) {

            throw new IllegalArgumentException(
                    "Cannot resolve relationship toward self"
            );
        }

        NpcRelationship personal =
                simulation.relationships()
                        .find(
                                subject,
                                target
                        )
                        .orElse(
                                null
                        );

        CharacterSocialIdentity subjectIdentity =
                society.characterSocialIdentityService()
                        .ensureIdentity(
                                subject
                        );

        CharacterSocialIdentity targetIdentity =
                society.characterSocialIdentityService()
                        .ensureIdentity(
                                target
                        );

        DynastyId subjectDynasty =
                preferredDynasty(
                        subjectIdentity
                );

        DynastyId targetDynasty =
                preferredDynasty(
                        targetIdentity
                );

        Baseline baseline =
                dynastyBaseline(
                        subjectDynasty,
                        targetDynasty
                );

        /*
         * Culture/religion are intentionally weak modifiers.
         *
         * Shared House/political history should matter far more than
         * simply sharing a religion or broad regional culture.
         */
        CharacterProfile subjectProfile =
                simulation.profiles()
                        .getOrCreate(
                                subject
                        );

        CharacterProfile targetProfile =
                simulation.profiles()
                        .getOrCreate(
                                target
                        );

        double affinity =
                baseline.affinity();

        double trust =
                baseline.trust();

        double respect =
                baseline.respect();

        double fear =
                baseline.fear();

        double familiarity =
                baseline.familiarity();

        boolean contextualModifier =
                false;

        if (knownAndEqual(
                subjectProfile.culture(),
                targetProfile.culture()
        )) {

            affinity += 0.05;
            trust += 0.03;
            familiarity += 0.06;
            contextualModifier =
                    true;
        }

        if (knownAndEqual(
                subjectProfile.religion(),
                targetProfile.religion()
        )) {

            affinity += 0.03;
            trust += 0.02;
            familiarity += 0.03;
            contextualModifier =
                    true;
        }

        Source source =
                baseline.source();

        if (source == Source.NEUTRAL_FALLBACK
                && contextualModifier) {

            source =
                    Source.CONTEXTUAL_FALLBACK;
        }

        /*
         * A newly-created personal relationship is allowed to inherit
         * the social/House baseline until the two people actually know
         * one another.
         *
         * familiarity = 0.0 -> pure fallback
         * familiarity = 0.5 -> 50/50 personal and fallback
         * familiarity = 1.0 -> pure personal history
         *
         * This prevents a blank NpcRelationship created by some other
         * service from accidentally erasing the House relationship.
         */
        if (personal != null) {

            double personalWeight =
                    clampUnit(
                            personal.familiarity()
                    );

            double fallbackWeight =
                    1.0 - personalWeight;

            affinity =
                    personal.affection()
                            * personalWeight
                            + affinity
                            * fallbackWeight;

            trust =
                    personal.trust()
                            * personalWeight
                            + trust
                            * fallbackWeight;

            respect =
                    personal.respect()
                            * personalWeight
                            + respect
                            * fallbackWeight;

            fear =
                    personal.fear()
                            * personalWeight
                            + fear
                            * fallbackWeight;

            familiarity =
                    Math.max(
                            personal.familiarity(),
                            familiarity
                    );

            source =
                    Source.PERSONAL_EXPLICIT;
        }

        return new EffectiveRelationship(
                clampSigned(
                        affinity
                ),
                clampSigned(
                        trust
                ),
                clampSigned(
                        respect
                ),
                clampUnit(
                        fear
                ),
                clampUnit(
                        familiarity
                ),
                source,
                subjectDynasty,
                targetDynasty
        );
    }

    private Baseline dynastyBaseline(
            DynastyId subject,
            DynastyId target
    ) {

        if (subject == null
                || target == null) {

            return Baseline.neutral();
        }

        if (subject.equals(
                target
        )) {

            return new Baseline(
                    0.48,
                    0.42,
                    0.42,
                    0.03,
                    0.72,
                    Source.SAME_DYNASTY
            );
        }

        DynastyRelationship authored =
                dynastyRelationships.find(
                                subject,
                                target
                        )
                        .orElse(
                                null
                        );

        if (authored != null) {

            return new Baseline(
                    authored.affinity(),
                    authored.trust(),
                    authored.respect(),
                    authored.fear(),
                    authored.familiarity(),
                    Source.DYNASTY_AUTHORED
            );
        }

        if (society.dynastyAllegiances()
                .isVassalOf(
                        subject,
                        target
                )) {

            return new Baseline(
                    0.18,
                    0.24,
                    0.55,
                    0.10,
                    0.62,
                    Source.VASSAL_TO_LIEGE
            );
        }

        if (society.dynastyAllegiances()
                .isVassalOf(
                        target,
                        subject
                )) {

            return new Baseline(
                    0.12,
                    0.18,
                    0.25,
                    0.03,
                    0.58,
                    Source.LIEGE_TO_VASSAL
            );
        }

        Dynasty subjectRoot =
                society.dynastyAllegiances()
                        .rootLiege(
                                subject
                        );

        Dynasty targetRoot =
                society.dynastyAllegiances()
                        .rootLiege(
                                target
                        );

        if (subjectRoot.id()
                .equals(
                        targetRoot.id()
                )) {

            return new Baseline(
                    0.06,
                    0.05,
                    0.10,
                    0.02,
                    0.32,
                    Source.SAME_REALM
            );
        }

        return Baseline.neutral();
    }

    private static DynastyId preferredDynasty(
            CharacterSocialIdentity identity
    ) {

        if (identity.currentDynasty() != null) {
            return identity.currentDynasty();
        }

        if (identity.legalFamilyDynasty() != null) {
            return identity.legalFamilyDynasty();
        }

        return identity.birthDynasty();
    }

    private static boolean knownAndEqual(
            String first,
            String second
    ) {

        if (first == null
                || second == null
                || "unknown".equals(
                first
        )
                || "unknown".equals(
                second
        )) {

            return false;
        }

        return first.equals(
                second
        );
    }

    private static double clampSigned(
            double value
    ) {

        return Math.max(
                -1.0,
                Math.min(
                        1.0,
                        value
                )
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

    public enum Source {

        PERSONAL_EXPLICIT,

        DYNASTY_AUTHORED,

        SAME_DYNASTY,

        VASSAL_TO_LIEGE,

        LIEGE_TO_VASSAL,

        SAME_REALM,

        CONTEXTUAL_FALLBACK,

        NEUTRAL_FALLBACK
    }

    public record EffectiveRelationship(
            double affection,
            double trust,
            double respect,
            double fear,
            double familiarity,
            Source source,
            DynastyId subjectDynasty,
            DynastyId targetDynasty
    ) {
    }

    private record Baseline(
            double affinity,
            double trust,
            double respect,
            double fear,
            double familiarity,
            Source source
    ) {

        private static Baseline neutral() {

            return new Baseline(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    Source.NEUTRAL_FALLBACK
            );
        }
    }
}

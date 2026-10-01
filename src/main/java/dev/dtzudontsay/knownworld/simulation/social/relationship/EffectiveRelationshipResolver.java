package dev.dtzudontsay.knownworld.simulation.social.relationship;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentity;

import java.util.Objects;

public final class EffectiveRelationshipResolver {

    private final NpcSimulation simulation;

    private final SocietyStructureRuntime society;

    private final EffectiveDynastyRelationshipResolver dynastyResolver;

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

        this.dynastyResolver =
                new EffectiveDynastyRelationshipResolver(
                        society,
                        Objects.requireNonNull(
                                dynastyRelationships,
                                "dynastyRelationships"
                        )
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

    public EffectiveDynastyRelationshipResolver.EffectiveDynastyRelationship resolveDynasty(
            DynastyId subject,
            DynastyId target
    ) {

        return dynastyResolver.resolve(
                subject,
                target
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

        EffectiveDynastyRelationshipResolver.EffectiveDynastyRelationship dynasty =
                dynastyResolver.resolve(
                        subject,
                        target
                );

        return new Baseline(
                dynasty.affinity(),
                dynasty.trust(),
                dynasty.respect(),
                dynasty.fear(),
                dynasty.familiarity(),
                switch (
                        dynasty.source()
                ) {

                    case DYNASTY_AUTHORED ->
                            Source.DYNASTY_AUTHORED;

                    case INHERITED_ANCESTOR_RELATION ->
                            Source.DYNASTY_INHERITED;

                    case SAME_DYNASTY ->
                            Source.SAME_DYNASTY;

                    case VASSAL_TO_LIEGE ->
                            Source.VASSAL_TO_LIEGE;

                    case LIEGE_TO_VASSAL ->
                            Source.LIEGE_TO_VASSAL;

                    case SAME_REALM ->
                            Source.SAME_REALM;

                    case NEUTRAL_FALLBACK ->
                            Source.NEUTRAL_FALLBACK;
                }
        );
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

        DYNASTY_INHERITED,

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

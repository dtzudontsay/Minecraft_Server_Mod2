package dev.dtzudontsay.knownworld.simulation.social.relationship;

import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves the effective STARTING political relationship between dynasties.
 *
 * Explicit authored relationships always win.
 *
 * Missing cross-realm relationships can inherit the nearest explicit
 * relationship between political ancestors. This is intentionally a
 * starting-context fallback, not a permanent political leash.
 *
 * Runtime DynastyRelationship objects remain mutable and can diverge
 * through future political events, allegiance changes, wars, marriages,
 * grievances, favors, betrayals, independence, and other simulation systems.
 */
public final class EffectiveDynastyRelationshipResolver {

    private static final double INHERITANCE_STEP_FACTOR =
            0.82;

    private static final double MINIMUM_INHERITANCE_FACTOR =
            0.30;

    private final SocietyStructureRuntime society;

    private final DynastyRelationshipManager relationships;

    public EffectiveDynastyRelationshipResolver(
            SocietyStructureRuntime society,
            DynastyRelationshipManager relationships
    ) {

        this.society =
                Objects.requireNonNull(
                        society,
                        "society"
                );

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
                );
    }

    public EffectiveDynastyRelationship resolve(
            DynastyId subject,
            DynastyId target
    ) {

        Objects.requireNonNull(
                subject,
                "subject"
        );

        Objects.requireNonNull(
                target,
                "target"
        );

        Dynasty subjectDynasty =
                society.dynasties()
                        .require(
                                subject
                        );

        Dynasty targetDynasty =
                society.dynasties()
                        .require(
                                target
                        );

        if (subject.equals(
                target
        )) {

            return new EffectiveDynastyRelationship(
                    0.48,
                    0.42,
                    0.42,
                    0.03,
                    0.72,
                    Source.SAME_DYNASTY,
                    subjectDynasty.authoredId(),
                    targetDynasty.authoredId(),
                    0,
                    0,
                    1.0
            );
        }

        DynastyRelationship exact =
                relationships.find(
                                subject,
                                target
                        )
                        .orElse(
                                null
                        );

        if (exact != null) {

            return explicit(
                    exact,
                    subjectDynasty.authoredId(),
                    targetDynasty.authoredId()
            );
        }

        List<Dynasty> subjectLineage =
                lineage(
                        subject
                );

        List<Dynasty> targetLineage =
                lineage(
                        target
                );

        CommonAncestor common =
                nearestCommonAncestor(
                        subjectLineage,
                        targetLineage
                );

        InheritedAnchor inherited =
                closestInheritedAnchor(
                        subjectLineage,
                        targetLineage,
                        common
                );

        if (inherited != null) {

            double attenuation =
                    inheritanceFactor(
                            inherited.subjectSteps()
                                    + inherited.targetSteps()
                    );

            DynastyRelationship authored =
                    inherited.relationship();

            return new EffectiveDynastyRelationship(
                    clampSigned(
                            authored.affinity()
                                    * attenuation
                    ),
                    clampSigned(
                            authored.trust()
                                    * attenuation
                    ),
                    clampSigned(
                            authored.respect()
                                    * attenuation
                    ),
                    clampUnit(
                            authored.fear()
                                    * attenuation
                    ),
                    clampUnit(
                            authored.familiarity()
                                    * Math.max(
                                    0.50,
                                    attenuation
                            )
                    ),
                    Source.INHERITED_ANCESTOR_RELATION,
                    inherited.subjectAnchor()
                            .authoredId(),
                    inherited.targetAnchor()
                            .authoredId(),
                    inherited.subjectSteps(),
                    inherited.targetSteps(),
                    attenuation
            );
        }

        if (society.dynastyAllegiances()
                .isVassalOf(
                        subject,
                        target
                )) {

            return new EffectiveDynastyRelationship(
                    0.18,
                    0.24,
                    0.55,
                    0.10,
                    0.62,
                    Source.VASSAL_TO_LIEGE,
                    subjectDynasty.authoredId(),
                    targetDynasty.authoredId(),
                    0,
                    0,
                    1.0
            );
        }

        if (society.dynastyAllegiances()
                .isVassalOf(
                        target,
                        subject
                )) {

            return new EffectiveDynastyRelationship(
                    0.12,
                    0.18,
                    0.25,
                    0.03,
                    0.58,
                    Source.LIEGE_TO_VASSAL,
                    subjectDynasty.authoredId(),
                    targetDynasty.authoredId(),
                    0,
                    0,
                    1.0
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

            return new EffectiveDynastyRelationship(
                    0.06,
                    0.05,
                    0.10,
                    0.02,
                    0.32,
                    Source.SAME_REALM,
                    subjectRoot.authoredId(),
                    targetRoot.authoredId(),
                    Math.max(
                            0,
                            subjectLineage.size() - 1
                    ),
                    Math.max(
                            0,
                            targetLineage.size() - 1
                    ),
                    1.0
            );
        }

        return new EffectiveDynastyRelationship(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                Source.NEUTRAL_FALLBACK,
                subjectRoot.authoredId(),
                targetRoot.authoredId(),
                Math.max(
                        0,
                        subjectLineage.size() - 1
                ),
                Math.max(
                        0,
                        targetLineage.size() - 1
                ),
                1.0
        );
    }

    private EffectiveDynastyRelationship explicit(
            DynastyRelationship relationship,
            String subjectAuthoredId,
            String targetAuthoredId
    ) {

        return new EffectiveDynastyRelationship(
                relationship.affinity(),
                relationship.trust(),
                relationship.respect(),
                relationship.fear(),
                relationship.familiarity(),
                Source.DYNASTY_AUTHORED,
                subjectAuthoredId,
                targetAuthoredId,
                0,
                0,
                1.0
        );
    }

    private List<Dynasty> lineage(
            DynastyId dynasty
    ) {

        List<Dynasty> result =
                new ArrayList<>();

        result.add(
                society.dynasties()
                        .require(
                                dynasty
                        )
        );

        result.addAll(
                society.dynastyAllegiances()
                        .liegeChain(
                                dynasty
                        )
        );

        return List.copyOf(
                result
        );
    }

    private CommonAncestor nearestCommonAncestor(
            List<Dynasty> subjectLineage,
            List<Dynasty> targetLineage
    ) {

        CommonAncestor best =
                null;

        for (
                int subjectIndex = 0;
                subjectIndex < subjectLineage.size();
                subjectIndex++
        ) {

            Dynasty subject =
                    subjectLineage.get(
                            subjectIndex
                    );

            for (
                    int targetIndex = 0;
                    targetIndex < targetLineage.size();
                    targetIndex++
            ) {

                Dynasty target =
                        targetLineage.get(
                                targetIndex
                        );

                if (!subject.id()
                        .equals(
                                target.id()
                        )) {

                    continue;
                }

                CommonAncestor candidate =
                        new CommonAncestor(
                                subject,
                                subjectIndex,
                                targetIndex
                        );

                if (best == null
                        || candidate.totalSteps()
                        < best.totalSteps()) {

                    best =
                            candidate;
                }
            }
        }

        return best;
    }

    private InheritedAnchor closestInheritedAnchor(
            List<Dynasty> subjectLineage,
            List<Dynasty> targetLineage,
            CommonAncestor common
    ) {

        InheritedAnchor best =
                null;

        for (
                int subjectIndex = 0;
                subjectIndex < subjectLineage.size();
                subjectIndex++
        ) {

            Dynasty subjectAnchor =
                    subjectLineage.get(
                            subjectIndex
                    );

            for (
                    int targetIndex = 0;
                    targetIndex < targetLineage.size();
                    targetIndex++
            ) {

                if (subjectIndex == 0
                        && targetIndex == 0) {

                    continue;
                }

                Dynasty targetAnchor =
                        targetLineage.get(
                                targetIndex
                        );

                if (subjectAnchor.id()
                        .equals(
                                targetAnchor.id()
                        )) {

                    continue;
                }

                /*
                 * When both dynasties share a political ancestor, do not
                 * inherit a relation which exists only ABOVE that shared
                 * branch point.
                 *
                 * Example:
                 *
                 * two minor Bolton vassals should not inherit
                 * Bolton -> Stark hostility merely because Stark sits
                 * above their common Bolton ancestor.
                 *
                 * A minor Bolton vassal versus a direct Stark vassal CAN
                 * inherit Bolton -> Stark because the Bolton anchor lies
                 * below the shared Stark root.
                 */
                if (common != null) {

                    boolean subjectBelowCommon =
                            subjectIndex
                                    < common.subjectSteps();

                    boolean targetBelowCommon =
                            targetIndex
                                    < common.targetSteps();

                    if (!subjectBelowCommon
                            && !targetBelowCommon) {

                        continue;
                    }
                }

                DynastyRelationship authored;

                try {

                    authored =
                            relationships.find(
                                            subjectAnchor.id(),
                                            targetAnchor.id()
                                    )
                                    .orElse(
                                            null
                                    );

                } catch (
                        IllegalArgumentException exception
                ) {

                    authored =
                            null;
                }

                if (authored == null) {
                    continue;
                }

                InheritedAnchor candidate =
                        new InheritedAnchor(
                                authored,
                                subjectAnchor,
                                targetAnchor,
                                subjectIndex,
                                targetIndex
                        );

                if (best == null
                        || better(
                                candidate,
                                best
                        )) {

                    best =
                            candidate;
                }
            }
        }

        return best;
    }

    private static boolean better(
            InheritedAnchor candidate,
            InheritedAnchor current
    ) {

        int candidateTotal =
                candidate.subjectSteps()
                        + candidate.targetSteps();

        int currentTotal =
                current.subjectSteps()
                        + current.targetSteps();

        if (candidateTotal
                != currentTotal) {

            return candidateTotal
                    < currentTotal;
        }

        int candidateMaximum =
                Math.max(
                        candidate.subjectSteps(),
                        candidate.targetSteps()
                );

        int currentMaximum =
                Math.max(
                        current.subjectSteps(),
                        current.targetSteps()
                );

        if (candidateMaximum
                != currentMaximum) {

            return candidateMaximum
                    < currentMaximum;
        }

        if (candidate.subjectSteps()
                != current.subjectSteps()) {

            return candidate.subjectSteps()
                    < current.subjectSteps();
        }

        return candidate.targetSteps()
                < current.targetSteps();
    }

    private static double inheritanceFactor(
            int totalSteps
    ) {

        if (totalSteps <= 0) {
            return 1.0;
        }

        return Math.max(
                MINIMUM_INHERITANCE_FACTOR,
                Math.pow(
                        INHERITANCE_STEP_FACTOR,
                        totalSteps
                )
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

        DYNASTY_AUTHORED,

        INHERITED_ANCESTOR_RELATION,

        SAME_DYNASTY,

        VASSAL_TO_LIEGE,

        LIEGE_TO_VASSAL,

        SAME_REALM,

        NEUTRAL_FALLBACK
    }

    public record EffectiveDynastyRelationship(
            double affinity,
            double trust,
            double respect,
            double fear,
            double familiarity,
            Source source,
            String subjectAnchor,
            String targetAnchor,
            int subjectSteps,
            int targetSteps,
            double attenuation
    ) {
    }

    private record CommonAncestor(
            Dynasty dynasty,
            int subjectSteps,
            int targetSteps
    ) {

        private int totalSteps() {
            return subjectSteps
                    + targetSteps;
        }
    }

    private record InheritedAnchor(
            DynastyRelationship relationship,
            Dynasty subjectAnchor,
            Dynasty targetAnchor,
            int subjectSteps,
            int targetSteps
    ) {
    }
}

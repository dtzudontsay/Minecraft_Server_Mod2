package dev.dtzudontsay.knownworld.simulation.social.relationship;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentity;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembership;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
     * Resolves the current effective personal relationship.
     *
     * Explicit authored/runtime personal history has priority.
     *
     * When personal history is absent or still unfamiliar, social
     * context supplies a STARTING fallback:
     *
     * - dynasty / political-house relation
     * - household
     * - social house organization
     * - primary political allegiance
     * - shared organizations
     * - legal parent/child relation
     * - culture and religion (weak only)
     *
     * These contextual values are NOT written into NpcRelationshipManager.
     * They therefore do not become permanent personal memories.
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

        MutableContext context =
                new MutableContext(
                        baseline.affinity(),
                        baseline.trust(),
                        baseline.respect(),
                        baseline.fear(),
                        baseline.familiarity(),
                        baseline.source()
                );

        applyFamilyContext(
                subject,
                target,
                subjectIdentity,
                targetIdentity,
                context
        );

        applyPrimarySocialContext(
                subjectIdentity,
                targetIdentity,
                context
        );

        applySharedMembershipContext(
                subject,
                target,
                subjectIdentity,
                targetIdentity,
                context
        );

        applyCultureReligionContext(
                subject,
                target,
                context
        );

        Source source =
                context.source;

        if (personal != null) {

            double personalWeight =
                    clampUnit(
                            personal.familiarity()
                    );

            double fallbackWeight =
                    1.0 - personalWeight;

            context.affinity =
                    personal.affection()
                            * personalWeight
                            + context.affinity
                            * fallbackWeight;

            context.trust =
                    personal.trust()
                            * personalWeight
                            + context.trust
                            * fallbackWeight;

            context.respect =
                    personal.respect()
                            * personalWeight
                            + context.respect
                            * fallbackWeight;

            context.fear =
                    personal.fear()
                            * personalWeight
                            + context.fear
                            * fallbackWeight;

            context.familiarity =
                    Math.max(
                            personal.familiarity(),
                            context.familiarity
                    );

            source =
                    Source.PERSONAL_EXPLICIT;
        }

        return new EffectiveRelationship(
                clampSigned(
                        context.affinity
                ),
                clampSigned(
                        context.trust
                ),
                clampSigned(
                        context.respect
                ),
                clampUnit(
                        context.fear
                ),
                clampUnit(
                        context.familiarity
                ),
                source,
                subjectDynasty,
                targetDynasty,
                List.copyOf(
                        context.modifiers
                )
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

    private void applyFamilyContext(
            NpcId subject,
            NpcId target,
            CharacterSocialIdentity subjectIdentity,
            CharacterSocialIdentity targetIdentity,
            MutableContext context
    ) {

        boolean directLegalFamily =
                target.equals(
                        subjectIdentity.legalMother()
                )
                        || target.equals(
                        subjectIdentity.legalFather()
                )
                        || subject.equals(
                        targetIdentity.legalMother()
                )
                        || subject.equals(
                        targetIdentity.legalFather()
                );

        if (!directLegalFamily) {
            return;
        }

        context.affinity +=
                0.20;

        context.trust +=
                0.18;

        context.respect +=
                0.12;

        context.familiarity +=
                0.30;

        context.modifiers.add(
                "LEGAL_PARENT_CHILD"
        );

        context.upgradeContextualSource();
    }

    private void applyPrimarySocialContext(
            CharacterSocialIdentity subject,
            CharacterSocialIdentity target,
            MutableContext context
    ) {

        if (sameKnown(
                subject.householdOrganization(),
                target.householdOrganization()
        )) {

            context.affinity +=
                    0.12;

            context.trust +=
                    0.12;

            context.respect +=
                    0.08;

            context.familiarity +=
                    0.28;

            context.modifiers.add(
                    "SAME_HOUSEHOLD"
            );

            context.upgradeContextualSource();
        }

        if (sameKnown(
                subject.houseOrganization(),
                target.houseOrganization()
        )) {

            context.affinity +=
                    0.08;

            context.trust +=
                    0.08;

            context.respect +=
                    0.06;

            context.familiarity +=
                    0.18;

            context.modifiers.add(
                    "SAME_HOUSE_ORGANIZATION"
            );

            context.upgradeContextualSource();
        }

        if (sameKnown(
                subject.primaryAllegianceOrganization(),
                target.primaryAllegianceOrganization()
        )) {

            double strength =
                    Math.min(
                            subject.allegianceStrength(),
                            target.allegianceStrength()
                    );

            context.affinity +=
                    0.04 * strength;

            context.trust +=
                    0.05 * strength;

            context.respect +=
                    0.04 * strength;

            context.familiarity +=
                    0.12 * strength;

            context.modifiers.add(
                    "SAME_PRIMARY_ALLEGIANCE"
            );

            context.upgradeContextualSource();
        }
    }

    private void applySharedMembershipContext(
            NpcId subject,
            NpcId target,
            CharacterSocialIdentity subjectIdentity,
            CharacterSocialIdentity targetIdentity,
            MutableContext context
    ) {

        List<OrganizationMembership> subjectMemberships =
                society.memberships()
                        .activeMembershipsFor(
                                subject
                        );

        Set<OrganizationId> targetOrganizations =
                new LinkedHashSet<>();

        for (
                OrganizationMembership membership :
                society.memberships()
                        .activeMembershipsFor(
                                target
                        )
        ) {

            targetOrganizations.add(
                    membership.organization()
            );
        }

        Set<OrganizationId> primaryAlreadyCounted =
                new LinkedHashSet<>();

        addIfPresent(
                primaryAlreadyCounted,
                subjectIdentity.householdOrganization()
        );

        addIfPresent(
                primaryAlreadyCounted,
                subjectIdentity.houseOrganization()
        );

        addIfPresent(
                primaryAlreadyCounted,
                subjectIdentity.primaryAllegianceOrganization()
        );

        addIfPresent(
                primaryAlreadyCounted,
                targetIdentity.householdOrganization()
        );

        addIfPresent(
                primaryAlreadyCounted,
                targetIdentity.houseOrganization()
        );

        addIfPresent(
                primaryAlreadyCounted,
                targetIdentity.primaryAllegianceOrganization()
        );

        int applied =
                0;

        for (
                OrganizationMembership membership :
                subjectMemberships
        ) {

            OrganizationId organizationId =
                    membership.organization();

            if (!targetOrganizations.contains(
                    organizationId
            )
                    || primaryAlreadyCounted.contains(
                    organizationId
            )) {

                continue;
            }

            Organization organization =
                    simulation.organizations()
                            .find(
                                    organizationId
                            )
                            .orElse(
                                    null
                            );

            if (organization == null) {
                continue;
            }

            SharedOrganizationModifier modifier =
                    modifierFor(
                            organization.type()
                    );

            if (modifier == null) {
                continue;
            }

            context.affinity +=
                    modifier.affinity();

            context.trust +=
                    modifier.trust();

            context.respect +=
                    modifier.respect();

            context.familiarity +=
                    modifier.familiarity();

            context.modifiers.add(
                    "SHARED_ORGANIZATION:"
                            + organizationId
            );

            context.upgradeContextualSource();

            applied++;

            /*
             * Prevent a character pair with many overlapping bookkeeping
             * organizations from accumulating an unrealistic stack.
             */
            if (applied >= 2) {
                break;
            }
        }
    }

    private void applyCultureReligionContext(
            NpcId subject,
            NpcId target,
            MutableContext context
    ) {

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

        if (knownAndEqual(
                subjectProfile.culture(),
                targetProfile.culture()
        )) {

            context.affinity +=
                    0.05;

            context.trust +=
                    0.03;

            context.familiarity +=
                    0.06;

            context.modifiers.add(
                    "SAME_CULTURE"
            );

            context.upgradeContextualSource();
        }

        if (knownAndEqual(
                subjectProfile.religion(),
                targetProfile.religion()
        )) {

            context.affinity +=
                    0.03;

            context.trust +=
                    0.02;

            context.familiarity +=
                    0.03;

            context.modifiers.add(
                    "SAME_RELIGION"
            );

            context.upgradeContextualSource();
        }
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

    private static SharedOrganizationModifier modifierFor(
            OrganizationType type
    ) {

        return switch (
                type
        ) {

            case MILITARY_ORDER ->
                    new SharedOrganizationModifier(
                            0.06,
                            0.08,
                            0.07,
                            0.16
                    );

            case ARMY,
                    CIVIC_GUARD,
                    MERCENARY_COMPANY ->
                    new SharedOrganizationModifier(
                            0.04,
                            0.05,
                            0.06,
                            0.12
                    );

            case CLAN,
                    NOMADIC_HOST ->
                    new SharedOrganizationModifier(
                            0.05,
                            0.06,
                            0.05,
                            0.14
                    );

            case RELIGIOUS_ORDER,
                    MYSTIC_ORDER,
                    SCHOLARLY_ORDER,
                    GUILD,
                    FINANCIAL_INSTITUTION ->
                    new SharedOrganizationModifier(
                            0.03,
                            0.04,
                            0.04,
                            0.10
                    );

            case GOVERNMENT,
                    COUNCIL,
                    FACTION,
                    POLITICAL_PARTY ->
                    new SharedOrganizationModifier(
                            0.02,
                            0.03,
                            0.04,
                            0.08
                    );

            case HOUSEHOLD,
                    NOBLE_HOUSE,
                    DYNASTIC_FAMILY,
                    SECRET_SOCIETY,
                    CRIMINAL_ORGANIZATION,
                    OTHER ->
                    null;
        };
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

    private static boolean sameKnown(
            OrganizationId first,
            OrganizationId second
    ) {

        return first != null
                && first.equals(
                second
        );
    }

    private static void addIfPresent(
            Set<OrganizationId> organizations,
            OrganizationId value
    ) {

        if (value != null) {

            organizations.add(
                    value
            );
        }
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

        SOCIAL_CONTEXT_FALLBACK,

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
            DynastyId targetDynasty,
            List<String> contextModifiers
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

    private record SharedOrganizationModifier(
            double affinity,
            double trust,
            double respect,
            double familiarity
    ) {
    }

    private static final class MutableContext {

        private double affinity;

        private double trust;

        private double respect;

        private double fear;

        private double familiarity;

        private Source source;

        private final List<String> modifiers =
                new ArrayList<>();

        private MutableContext(
                double affinity,
                double trust,
                double respect,
                double fear,
                double familiarity,
                Source source
        ) {

            this.affinity =
                    affinity;

            this.trust =
                    trust;

            this.respect =
                    respect;

            this.fear =
                    fear;

            this.familiarity =
                    familiarity;

            this.source =
                    source;
        }

        private void upgradeContextualSource() {

            if (source
                    == Source.NEUTRAL_FALLBACK) {

                source =
                        Source.SOCIAL_CONTEXT_FALLBACK;

            } else if (source
                    == Source.SAME_REALM) {

                source =
                        Source.CONTEXTUAL_FALLBACK;
            }
        }
    }
}

package dev.dtzudontsay.knownworld.simulation.social.identity;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.family.DynastyInheritanceRule;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembership;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipManager;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyManager;

import java.util.List;
import java.util.Objects;

public final class CharacterSocialIdentityService {

    private final NpcSimulation simulation;

    private final CharacterSocialIdentityManager identities;

    private final DynastyManager dynasties;

    private final OrganizationMembershipManager memberships;

    private final NonDynasticSocietyManager societies;

    public CharacterSocialIdentityService(
            NpcSimulation simulation,
            CharacterSocialIdentityManager identities,
            DynastyManager dynasties,
            OrganizationMembershipManager memberships,
            NonDynasticSocietyManager societies
    ) {

        this.simulation =
                Objects.requireNonNull(
                        simulation,
                        "simulation"
                );

        this.identities =
                Objects.requireNonNull(
                        identities,
                        "identities"
                );

        this.dynasties =
                Objects.requireNonNull(
                        dynasties,
                        "dynasties"
                );

        this.memberships =
                Objects.requireNonNull(
                        memberships,
                        "memberships"
                );

        this.societies =
                Objects.requireNonNull(
                        societies,
                        "societies"
                );
    }

    public CharacterSocialIdentity ensureIdentity(
            NpcId npc
    ) {

        CharacterSocialIdentity existing =
                identities.find(
                                npc
                        )
                        .orElse(
                                null
                        );

        if (existing != null) {

            synchronizePrimaryStructures(
                    existing
            );

            return existing;
        }

        CharacterSocialIdentity identity =
                identities.getOrCreate(
                        npc
                );

        NpcAffiliation affiliation =
                simulation.affiliations()
                        .getOrCreate(
                                npc
                        );

        identity.setHouseholdOrganization(
                affiliation.household()
        );

        identity.setHouseOrganization(
                affiliation.nobleHouse()
        );

        identity.setPrimaryAllegianceOrganization(
                affiliation.faction()
        );

        identity.setAllegianceStrength(
                affiliation.faction() == null
                        ? 0.0
                        : 0.65
        );

        DynastyId currentDynasty =
                dynastyForOrganization(
                        affiliation.nobleHouse()
                );

        if (currentDynasty == null) {

            currentDynasty =
                    inferDynastyFromParents(
                            npc
                    );
        }

        identity.setCurrentDynasty(
                currentDynasty
        );

        identity.setBirthDynasty(
                currentDynasty
        );

        identity.setLegalFamilyDynasty(
                currentDynasty
        );

        simulation.genealogy()
                .parentsOf(
                        npc
                )
                .ifPresent(
                        parentage -> {

                            identity.setLegalMother(
                                    parentage.mother()
                            );

                            identity.setLegalFather(
                                    parentage.father()
                            );
                        }
                );

        if (identity.houseOrganization() == null
                && currentDynasty != null) {

            dynasties.find(
                            currentDynasty
                    )
                    .map(
                            Dynasty::organizationId
                    )
                    .ifPresent(
                            identity::setHouseOrganization
                    );
        }

        synchronizePrimaryStructures(
                identity
        );

        return identity;
    }

    public void ensureAll() {

        for (
                NpcState npc :
                simulation.registry()
                        .all()
        ) {

            ensureIdentity(
                    npc.id()
            );
        }
    }

    /**
     * Called after a successful marriage.
     *
     * Birth dynasty never changes.
     *
     * The spouse on the non-inheriting side of the marriage becomes
     * socially attached to the anchor spouse's dynasty.
     */
    public void onMarriage(
            NpcId first,
            NpcId second,
            DynastyInheritanceRule rule,
            long tick
    ) {

        CharacterSocialIdentity firstIdentity =
                ensureIdentity(
                        first
                );

        CharacterSocialIdentity secondIdentity =
                ensureIdentity(
                        second
                );

        NpcId anchor =
                chooseMarriageAnchor(
                        first,
                        second,
                        rule,
                        firstIdentity,
                        secondIdentity
                );

        if (anchor == null) {
            return;
        }

        NpcId joining =
                anchor.equals(
                        first
                )
                        ? second
                        : first;

        CharacterSocialIdentity anchorIdentity =
                anchor.equals(
                        first
                )
                        ? firstIdentity
                        : secondIdentity;

        CharacterSocialIdentity joiningIdentity =
                joining.equals(
                        first
                )
                        ? firstIdentity
                        : secondIdentity;

        DynastyId targetDynasty =
                anchorIdentity.currentDynasty() != null
                        ? anchorIdentity.currentDynasty()
                        : anchorIdentity.birthDynasty();

        if (targetDynasty == null) {

            /*
             * There is no dynasty to marry into.
             *
             * Lowborn and many non-dynastic societies are valid.
             */
            return;
        }

        identities.setMarriedIntoDynasty(
                joining,
                targetDynasty
        );

        identities.setCurrentDynasty(
                joining,
                targetDynasty
        );

        identities.setLegalFamilyDynasty(
                joining,
                targetDynasty
        );

        Dynasty target =
                dynasties.require(
                        targetDynasty
                );

        if (target.organizationId() != null) {

            identities.setHouseOrganization(
                    joining,
                    target.organizationId()
            );
        }

        synchronizePrimaryStructures(
                joiningIdentity
        );
    }

    public void setPrimaryAllegiance(
            NpcId npc,
            OrganizationId organization,
            double strength
    ) {

        identities.setPrimaryAllegiance(
                npc,
                organization,
                strength
        );

        synchronizePrimaryStructures(
                identities.getOrCreate(
                        npc
                )
        );
    }

    public void joinOrganization(
            NpcId npc,
            OrganizationId organization,
            double loyalty,
            long tick,
            String... roles
    ) {

        OrganizationMembership membership =
                memberships.join(
                        npc,
                        organization,
                        tick,
                        loyalty
                );

        if (roles == null) {
            return;
        }

        for (
                String role :
                roles
        ) {

            if (role == null
                    || role.isBlank()) {

                continue;
            }

            memberships.addRole(
                    npc,
                    organization,
                    role
            );
        }
    }

    public CharacterSocialIdentitySnapshot snapshot(
            NpcId npc
    ) {

        CharacterSocialIdentity identity =
                ensureIdentity(
                        npc
                );

        Parentage biological =
                simulation.genealogy()
                        .parentsOf(
                                npc
                        )
                        .orElse(
                                null
                        );

        List<OrganizationMembership> allMemberships =
                memberships.activeMembershipsFor(
                        npc
                );

        List<OrganizationMembership> societyMemberships =
                allMemberships.stream()
                        .filter(
                                membership ->
                                        societies.find(
                                                membership.organization()
                                        ).isPresent()
                        )
                        .toList();

        List<OrganizationMembership> governmentMemberships =
                allMemberships.stream()
                        .filter(
                                membership -> {

                                    Organization organization =
                                            simulation.organizations()
                                                    .find(
                                                            membership.organization()
                                                    )
                                                    .orElse(
                                                            null
                                                    );

                                    if (organization == null) {
                                        return false;
                                    }

                                    return organization.type()
                                            == OrganizationType.GOVERNMENT
                                            || organization.type()
                                            == OrganizationType.FACTION
                                            || organization.type()
                                            == OrganizationType.COUNCIL;
                                }
                        )
                        .toList();

        var profile =
                simulation.profiles()
                        .getOrCreate(
                                npc
                        );

        return new CharacterSocialIdentitySnapshot(
                npc,
                identity.birthDynasty(),
                identity.currentDynasty(),
                identity.marriedIntoDynasty(),
                identity.legalFamilyDynasty(),
                biological == null
                        ? null
                        : biological.mother(),
                biological == null
                        ? null
                        : biological.father(),
                identity.legalMother(),
                identity.legalFather(),
                identity.householdOrganization(),
                identity.houseOrganization(),
                identity.primaryAllegianceOrganization(),
                identity.allegianceStrength(),
                profile.culture(),
                profile.religion(),
                allMemberships,
                societyMemberships,
                governmentMemberships,
                simulation.religiousMemberships()
                        .activeMembershipsFor(
                                npc
                        )
        );
    }

    private void synchronizePrimaryStructures(
            CharacterSocialIdentity identity
    ) {

        long tick =
                simulation.serverTickCounter();

        ensureMembershipIfPresent(
                identity.npc(),
                identity.householdOrganization(),
                tick,
                0.75
        );

        ensureMembershipIfPresent(
                identity.npc(),
                identity.houseOrganization(),
                tick,
                0.82
        );

        ensureMembershipIfPresent(
                identity.npc(),
                identity.primaryAllegianceOrganization(),
                tick,
                identity.allegianceStrength()
        );

        /*
         * Transitional compatibility bridge.
         *
         * NpcAffiliation remains supported while newer systems migrate
         * to CharacterSocialIdentity + generic memberships.
         */
        if (identity.householdOrganization() != null) {

            simulation.affiliations()
                    .setHousehold(
                            identity.npc(),
                            identity.householdOrganization()
                    );
        }

        if (identity.houseOrganization() != null) {

            Organization organization =
                    simulation.organizations()
                            .find(
                                    identity.houseOrganization()
                            )
                            .orElseThrow();

            if (organization.type()
                    == OrganizationType.NOBLE_HOUSE) {

                simulation.affiliations()
                        .setNobleHouse(
                                identity.npc(),
                                identity.houseOrganization()
                        );
            }
        }

        if (identity.primaryAllegianceOrganization() != null) {

            Organization organization =
                    simulation.organizations()
                            .find(
                                    identity.primaryAllegianceOrganization()
                            )
                            .orElseThrow();

            if (organization.type()
                    == OrganizationType.FACTION) {

                simulation.affiliations()
                        .setFaction(
                                identity.npc(),
                                identity.primaryAllegianceOrganization()
                        );
            }
        }
    }

    private void ensureMembershipIfPresent(
            NpcId npc,
            OrganizationId organization,
            long tick,
            double loyalty
    ) {

        if (organization == null) {
            return;
        }

        OrganizationMembership existing =
                memberships.find(
                                npc,
                                organization
                        )
                        .orElse(
                                null
                        );

        if (existing == null
                || !existing.isActive()) {

            memberships.join(
                    npc,
                    organization,
                    tick,
                    loyalty
            );
        }
    }

    private DynastyId dynastyForOrganization(
            OrganizationId organization
    ) {

        if (organization == null) {
            return null;
        }

        return dynasties.findByOrganization(
                        organization
                )
                .map(
                        Dynasty::id
                )
                .orElse(
                        null
                );
    }

    private DynastyId inferDynastyFromParents(
            NpcId child
    ) {

        Parentage parentage =
                simulation.genealogy()
                        .parentsOf(
                                child
                        )
                        .orElse(
                                null
                        );

        if (parentage == null) {
            return null;
        }

        if (parentage.father() != null) {

            CharacterSocialIdentity father =
                    ensureIdentity(
                            parentage.father()
                    );

            DynastyId fatherDynasty =
                    father.currentDynasty() != null
                            ? father.currentDynasty()
                            : father.birthDynasty();

            if (fatherDynasty != null) {
                return fatherDynasty;
            }
        }

        if (parentage.mother() != null) {

            CharacterSocialIdentity mother =
                    ensureIdentity(
                            parentage.mother()
                    );

            return mother.currentDynasty() != null
                    ? mother.currentDynasty()
                    : mother.birthDynasty();
        }

        return null;
    }

    private NpcId chooseMarriageAnchor(
            NpcId first,
            NpcId second,
            DynastyInheritanceRule rule,
            CharacterSocialIdentity firstIdentity,
            CharacterSocialIdentity secondIdentity
    ) {

        NpcState firstState =
                simulation.registry()
                        .find(
                                first
                        )
                        .orElseThrow();

        NpcState secondState =
                simulation.registry()
                        .find(
                                second
                        )
                        .orElseThrow();

        if (rule
                == DynastyInheritanceRule.MATRILINEAL) {

            if (firstState.identity()
                    .sex()
                    == NpcSex.FEMALE
                    && secondState.identity()
                    .sex()
                    == NpcSex.MALE) {

                return first;
            }

            if (secondState.identity()
                    .sex()
                    == NpcSex.FEMALE
                    && firstState.identity()
                    .sex()
                    == NpcSex.MALE) {

                return second;
            }

        } else {

            if (firstState.identity()
                    .sex()
                    == NpcSex.MALE
                    && secondState.identity()
                    .sex()
                    == NpcSex.FEMALE) {

                return first;
            }

            if (secondState.identity()
                    .sex()
                    == NpcSex.MALE
                    && firstState.identity()
                    .sex()
                    == NpcSex.FEMALE) {

                return second;
            }
        }

        boolean firstHasDynasty =
                firstIdentity.currentDynasty() != null
                        || firstIdentity.birthDynasty() != null;

        boolean secondHasDynasty =
                secondIdentity.currentDynasty() != null
                        || secondIdentity.birthDynasty() != null;

        if (firstHasDynasty
                && !secondHasDynasty) {

            return first;
        }

        if (secondHasDynasty
                && !firstHasDynasty) {

            return second;
        }

        /*
         * Ambiguous case.
         *
         * Do not invent a dynastic transition.
         */
        return null;
    }
}
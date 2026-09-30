package dev.dtzudontsay.knownworld.simulation.social.identity;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousMembership;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembership;

import java.util.List;

public record CharacterSocialIdentitySnapshot(
        NpcId npc,
        DynastyId birthDynasty,
        DynastyId currentDynasty,
        DynastyId marriedIntoDynasty,
        DynastyId legalFamilyDynasty,
        NpcId biologicalMother,
        NpcId biologicalFather,
        NpcId legalMother,
        NpcId legalFather,
        OrganizationId householdOrganization,
        OrganizationId houseOrganization,
        OrganizationId primaryAllegianceOrganization,
        double allegianceStrength,
        String cultureId,
        String personalReligionId,
        List<OrganizationMembership> organizationMemberships,
        List<OrganizationMembership> societyMemberships,
        List<OrganizationMembership> governmentAndFactionMemberships,
        List<ReligiousMembership> religiousMemberships
) {

    public CharacterSocialIdentitySnapshot {

        organizationMemberships =
                List.copyOf(
                        organizationMemberships
                );

        societyMemberships =
                List.copyOf(
                        societyMemberships
                );

        governmentAndFactionMemberships =
                List.copyOf(
                        governmentAndFactionMemberships
                );

        religiousMemberships =
                List.copyOf(
                        religiousMemberships
                );
    }
}
package dev.dtzudontsay.knownworld.simulation.social.identity;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CharacterSocialIdentityManager {

    private final NpcRegistry npcs;

    private final DynastyManager dynasties;

    private final OrganizationManager organizations;

    private final Map<NpcId, CharacterSocialIdentity> identities =
            new LinkedHashMap<>();

    public CharacterSocialIdentityManager(
            NpcRegistry npcs,
            DynastyManager dynasties,
            OrganizationManager organizations
    ) {

        this.npcs =
                Objects.requireNonNull(
                        npcs,
                        "npcs"
                );

        this.dynasties =
                Objects.requireNonNull(
                        dynasties,
                        "dynasties"
                );

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );
    }

    public synchronized CharacterSocialIdentity getOrCreate(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        return identities.computeIfAbsent(
                npc,
                CharacterSocialIdentity::new
        );
    }

    public synchronized Optional<CharacterSocialIdentity> find(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        return Optional.ofNullable(
                identities.get(
                        npc
                )
        );
    }

    public synchronized void registerLoaded(
            CharacterSocialIdentity loaded
    ) {

        Objects.requireNonNull(
                loaded,
                "loaded"
        );

        validateNpc(
                loaded.npc()
        );

        /*
         * Old saves may reference a dynasty or organization which was
         * deliberately removed from the authored catalog.
         *
         * Character identity persistence must therefore reconcile
         * gracefully instead of making such a save impossible to open.
         */
        CharacterSocialIdentity sanitized =
                new CharacterSocialIdentity(
                        loaded.npc(),
                        existingDynastyOrNull(
                                loaded.birthDynasty()
                        ),
                        existingDynastyOrNull(
                                loaded.currentDynasty()
                        ),
                        existingDynastyOrNull(
                                loaded.marriedIntoDynasty()
                        ),
                        existingDynastyOrNull(
                                loaded.legalFamilyDynasty()
                        ),
                        existingNpcOrNull(
                                loaded.legalMother()
                        ),
                        existingNpcOrNull(
                                loaded.legalFather()
                        ),
                        existingOrganizationOrNull(
                                loaded.householdOrganization()
                        ),
                        existingOrganizationOrNull(
                                loaded.houseOrganization()
                        ),
                        existingOrganizationOrNull(
                                loaded.primaryAllegianceOrganization()
                        ),
                        loaded.allegianceStrength()
                );

        validateHouseholdIfPresent(
                sanitized.householdOrganization()
        );

        validateHouseOrganizationIfPresent(
                sanitized.houseOrganization()
        );

        identities.put(
                sanitized.npc(),
                sanitized
        );
    }

    public synchronized void setBirthDynasty(
            NpcId npc,
            DynastyId dynasty
    ) {

        validateDynastyIfPresent(
                dynasty
        );

        getOrCreate(
                npc
        )
                .setBirthDynasty(
                        dynasty
                );
    }

    public synchronized void setCurrentDynasty(
            NpcId npc,
            DynastyId dynasty
    ) {

        validateDynastyIfPresent(
                dynasty
        );

        getOrCreate(
                npc
        )
                .setCurrentDynasty(
                        dynasty
                );
    }

    public synchronized void setMarriedIntoDynasty(
            NpcId npc,
            DynastyId dynasty
    ) {

        validateDynastyIfPresent(
                dynasty
        );

        getOrCreate(
                npc
        )
                .setMarriedIntoDynasty(
                        dynasty
                );
    }

    public synchronized void setLegalFamilyDynasty(
            NpcId npc,
            DynastyId dynasty
    ) {

        validateDynastyIfPresent(
                dynasty
        );

        getOrCreate(
                npc
        )
                .setLegalFamilyDynasty(
                        dynasty
                );
    }

    public synchronized void setLegalMother(
            NpcId npc,
            NpcId mother
    ) {

        validateNpcIfPresent(
                mother
        );

        if (npc.equals(
                mother
        )) {

            throw new IllegalArgumentException(
                    "NPC cannot be their own legal mother"
            );
        }

        getOrCreate(
                npc
        )
                .setLegalMother(
                        mother
                );
    }

    public synchronized void setLegalFather(
            NpcId npc,
            NpcId father
    ) {

        validateNpcIfPresent(
                father
        );

        if (npc.equals(
                father
        )) {

            throw new IllegalArgumentException(
                    "NPC cannot be their own legal father"
            );
        }

        getOrCreate(
                npc
        )
                .setLegalFather(
                        father
                );
    }

    public synchronized void setHouseholdOrganization(
            NpcId npc,
            OrganizationId organization
    ) {

        validateHouseholdIfPresent(
                organization
        );

        getOrCreate(
                npc
        )
                .setHouseholdOrganization(
                        organization
                );
    }

    public synchronized void setHouseOrganization(
            NpcId npc,
            OrganizationId organization
    ) {

        validateHouseOrganizationIfPresent(
                organization
        );

        getOrCreate(
                npc
        )
                .setHouseOrganization(
                        organization
                );
    }

    public synchronized void setPrimaryAllegiance(
            NpcId npc,
            OrganizationId organization,
            double strength
    ) {

        validateOrganizationIfPresent(
                organization
        );

        CharacterSocialIdentity identity =
                getOrCreate(
                        npc
                );

        identity.setPrimaryAllegianceOrganization(
                organization
        );

        identity.setAllegianceStrength(
                organization == null
                        ? 0.0
                        : strength
        );
    }

    public synchronized Collection<CharacterSocialIdentity> all() {

        return List.copyOf(
                identities.values()
        );
    }

    public synchronized int size() {

        return identities.size();
    }

    private DynastyId existingDynastyOrNull(
            DynastyId dynasty
    ) {

        if (dynasty == null) {
            return null;
        }

        return dynasties.find(
                        dynasty
                )
                .map(
                        value ->
                                value.id()
                )
                .orElse(
                        null
                );
    }

    private NpcId existingNpcOrNull(
            NpcId npc
    ) {

        return npc != null
                && npcs.contains(
                npc
        )
                ? npc
                : null;
    }

    private OrganizationId existingOrganizationOrNull(
            OrganizationId organization
    ) {

        if (organization == null) {
            return null;
        }

        return organizations.find(
                        organization
                )
                .map(
                        value ->
                                value.id()
                )
                .orElse(
                        null
                );
    }

    private void validateNpc(
            NpcId npc
    ) {

        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (!npcs.contains(
                npc
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC "
                            + npc
            );
        }
    }

    private void validateNpcIfPresent(
            NpcId npc
    ) {

        if (npc != null) {

            validateNpc(
                    npc
            );
        }
    }

    private void validateDynastyIfPresent(
            DynastyId dynasty
    ) {

        if (dynasty != null
                && dynasties.find(
                dynasty
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown dynasty "
                            + dynasty
            );
        }
    }

    private void validateOrganizationIfPresent(
            OrganizationId organization
    ) {

        if (organization != null
                && organizations.find(
                organization
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown organization "
                            + organization
            );
        }
    }

    private void validateHouseholdIfPresent(
            OrganizationId organizationId
    ) {

        if (organizationId == null) {
            return;
        }

        Organization organization =
                organizations.find(
                                organizationId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown household organization "
                                                        + organizationId
                                        )
                        );

        if (organization.type()
                != OrganizationType.HOUSEHOLD) {

            throw new IllegalArgumentException(
                    "Organization "
                            + organizationId
                            + " is "
                            + organization.type()
                            + ", expected HOUSEHOLD"
            );
        }
    }

    private void validateHouseOrganizationIfPresent(
            OrganizationId organizationId
    ) {

        if (organizationId == null) {
            return;
        }

        Organization organization =
                organizations.find(
                                organizationId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown house organization "
                                                        + organizationId
                                        )
                        );

        if (organization.type()
                != OrganizationType.NOBLE_HOUSE
                && organization.type()
                != OrganizationType.DYNASTIC_FAMILY) {

            throw new IllegalArgumentException(
                    "Organization "
                            + organizationId
                            + " is "
                            + organization.type()
                            + ", expected NOBLE_HOUSE or DYNASTIC_FAMILY"
            );
        }
    }
}
package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;

/**
 * Cultural / structural type of a lineage.
 *
 * Dynasty is deliberately broader than Westerosi "House".
 */
public enum DynastyType {

    ROYAL_HOUSE(
            OrganizationType.NOBLE_HOUSE
    ),

    GREAT_HOUSE(
            OrganizationType.NOBLE_HOUSE
    ),

    NOBLE_HOUSE(
            OrganizationType.NOBLE_HOUSE
    ),

    LANDED_KNIGHTLY_HOUSE(
            OrganizationType.NOBLE_HOUSE
    ),

    CADET_BRANCH(
            OrganizationType.NOBLE_HOUSE
    ),

    EXILED_DYNASTY(
            OrganizationType.NOBLE_HOUSE
    ),

    FREE_CITY_NOBLE_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    OLD_BLOOD_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    MAGISTERIAL_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    MERCHANT_DYNASTY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    MASTER_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    CLAN(
            OrganizationType.CLAN
    ),

    OTHER(
            OrganizationType.DYNASTIC_FAMILY
    );

    private final OrganizationType organizationType;

    DynastyType(
            OrganizationType organizationType
    ) {

        this.organizationType =
                organizationType;
    }

    public OrganizationType organizationType() {

        return organizationType;
    }
}
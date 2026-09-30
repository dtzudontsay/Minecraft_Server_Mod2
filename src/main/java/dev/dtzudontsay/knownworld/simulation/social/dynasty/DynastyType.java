package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;

/**
 * Broad structural form of a hereditary lineage.
 *
 * This is deliberately broader than the Westerosi concept of "House".
 */
public enum DynastyType {

    /*
     * =========================================================
     * WESTEROS
     * =========================================================
     */

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

    /*
     * =========================================================
     * VALYRIA / FREE CITIES
     * =========================================================
     */

    /**
     * One of the aristocratic dragonlord/freeholder lineages of
     * the Valyrian Freehold.
     */
    DRAGONLORD_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /**
     * Volantene family of the Old Blood.
     */
    OLD_BLOOD_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /**
     * Generic noble lineage of a Free City.
     */
    FREE_CITY_NOBLE_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /**
     * Family belonging to the political/magisterial elite of a
     * Free City such as Pentos, Myr, Lys or Tyrosh.
     */
    MAGISTERIAL_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /**
     * Braavosi family associated with the hereditary keyholders.
     */
    KEYHOLDER_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    MERCHANT_DYNASTY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /*
     * =========================================================
     * GHISCARI / QARTH
     * =========================================================
     */

    /**
     * Aristocratic master family of Slaver's Bay.
     *
     * The exact local style (Good/Wise/Great Master) belongs to
     * character and political roles rather than the dynasty type.
     */
    MASTER_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /**
     * Hereditary aristocratic lineage among Qarth's Pureborn.
     */
    PUREBORN_FAMILY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /*
     * =========================================================
     * FAR EAST / OTHER POLITICAL TRADITIONS
     * =========================================================
     */

    /**
     * Imperial ruling lineage, especially useful for Yi Ti and
     * other hereditary empires.
     */
    IMPERIAL_DYNASTY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /**
     * Princely hereditary ruling family outside the Westerosi
     * feudal-house framework.
     */
    PRINCELY_DYNASTY(
            OrganizationType.DYNASTIC_FAMILY
    ),

    /**
     * Hereditary lineage embedded in a tribal or nomadic social
     * structure.
     */
    TRIBAL_DYNASTY(
            OrganizationType.CLAN
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
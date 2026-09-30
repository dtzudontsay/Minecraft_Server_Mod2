package dev.dtzudontsay.knownworld.simulation.social.society;

import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;

/**
 * Social/political structures which must not be represented as
 * hereditary dynasties merely because they are politically important.
 */
public enum NonDynasticSocietyType {

    /**
     * Dothraki khalasar.
     *
     * Political and military following around a khal.
     */
    KHALASAR(
            OrganizationType.NOMADIC_HOST
    ),

    /**
     * Relatively stable Free Folk clan or tribe.
     */
    FREE_FOLK_CLAN(
            OrganizationType.CLAN
    ),

    /**
     * Smaller Free Folk warband / raiding band.
     */
    FREE_FOLK_BAND(
            OrganizationType.CLAN
    ),

    /**
     * Temporary coalition of otherwise independent Free Folk groups.
     *
     * Example:
     * the gathering under a King-Beyond-the-Wall.
     */
    FREE_FOLK_CONFEDERATION(
            OrganizationType.FACTION
    ),

    /**
     * Mobile blood-related Jogos Nhai band.
     */
    JOGOS_NHAI_BAND(
            OrganizationType.NOMADIC_HOST
    ),

    /**
     * Generic hereditary or cultural clan which is not equivalent
     * to a feudal noble dynasty.
     */
    TRIBAL_CLAN(
            OrganizationType.CLAN
    ),

    /**
     * Generic mobile military/social host.
     */
    MOBILE_WARBAND(
            OrganizationType.NOMADIC_HOST
    ),

    OTHER(
            OrganizationType.OTHER
    );

    private final OrganizationType organizationType;

    NonDynasticSocietyType(
            OrganizationType organizationType
    ) {

        this.organizationType =
                organizationType;
    }

    public OrganizationType organizationType() {

        return organizationType;
    }
}
package dev.dtzudontsay.knownworld.simulation.social;

/**
 * Broad structural categories for persistent organizations.
 *
 * Specific organizations must never become enum constants here.
 * For example:
 *
 * - House Stark        -> NOBLE_HOUSE
 * - Iron Bank          -> FINANCIAL_INSTITUTION
 * - Night's Watch      -> MILITARY_ORDER
 * - Golden Company     -> MERCENARY_COMPANY
 * - Dothraki khalasar  -> NOMADIC_HOST
 */
public enum OrganizationType {

    HOUSEHOLD,

    /**
     * Westerosi-style political/dynastic noble house.
     */
    NOBLE_HOUSE,

    /**
     * Dynasty/family which is socially important but is not naturally
     * represented as a Westerosi feudal noble house.
     *
     * Examples:
     * - Free City dynasties
     * - Old Blood families
     * - Ghiscari master families
     * - merchant dynasties
     */
    DYNASTIC_FAMILY,

    /**
     * Clan or clan-like kin organization.
     */
    CLAN,

    FACTION,

    GOVERNMENT,

    COUNCIL,

    ARMY,

    CIVIC_GUARD,

    MILITARY_ORDER,

    MERCENARY_COMPANY,

    RELIGIOUS_ORDER,

    MYSTIC_ORDER,

    SCHOLARLY_ORDER,

    GUILD,

    FINANCIAL_INSTITUTION,

    POLITICAL_PARTY,

    NOMADIC_HOST,

    SECRET_SOCIETY,

    CRIMINAL_ORGANIZATION,

    OTHER
}
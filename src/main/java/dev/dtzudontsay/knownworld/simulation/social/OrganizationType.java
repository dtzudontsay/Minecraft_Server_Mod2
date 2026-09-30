package dev.dtzudontsay.knownworld.simulation.social;

/**
 * Persistent social, political, military, religious and economic
 * organization categories.
 *
 * These are intentionally broad structural classes. Specific entities such
 * as the Night's Watch, Citadel, Iron Bank or a particular khalasar remain
 * Organization instances rather than enum constants.
 */
public enum OrganizationType {

    /**
     * Domestic household or residential court.
     */
    HOUSEHOLD,

    /**
     * Dynastic noble house.
     */
    NOBLE_HOUSE,

    /**
     * Broad political allegiance or realm-level faction.
     */
    FACTION,

    /**
     * Formal state or civic government.
     */
    GOVERNMENT,

    /**
     * Deliberative or advisory governing body.
     *
     * Examples: Small Council, conclave-like councils.
     */
    COUNCIL,

    /**
     * General military formation or army.
     */
    ARMY,

    /**
     * Permanent sworn military order.
     *
     * Examples: Kingsguard, Night's Watch.
     */
    MILITARY_ORDER,

    /**
     * Professional mercenary company.
     *
     * Examples: Golden Company, Second Sons.
     */
    MERCENARY_COMPANY,

    /**
     * Religious institution or religious order.
     */
    RELIGIOUS_ORDER,

    /**
     * Scholarly / educational institution.
     *
     * Example: Citadel.
     */
    SCHOLARLY_ORDER,

    /**
     * Trade or professional guild.
     */
    GUILD,

    /**
     * Bank or similarly persistent financial institution.
     *
     * Example: Iron Bank of Braavos.
     */
    FINANCIAL_INSTITUTION,

    /**
     * Organized political party.
     *
     * Example: Volantene Tigers or Elephants.
     */
    POLITICAL_PARTY,

    /**
     * Nomadic political/military host.
     *
     * Example: Dothraki khalasar.
     */
    NOMADIC_HOST,

    /**
     * Persistent organized criminal enterprise.
     */
    CRIMINAL_ORGANIZATION,

    OTHER
}
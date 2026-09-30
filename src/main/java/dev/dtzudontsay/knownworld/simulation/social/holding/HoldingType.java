package dev.dtzudontsay.knownworld.simulation.social.holding;

/**
 * A political/economic landed unit.
 *
 * This is deliberately NOT the same thing as WorldLocationKind.
 *
 * A world location describes geography.
 * A holding describes possession/control of land.
 */
public enum HoldingType {

    REGION,

    TERRITORY,

    CASTLE,

    FORTRESS,

    CITY,

    TOWN,

    VILLAGE,

    PORT,

    ESTATE,

    OTHER
}
package dev.dtzudontsay.knownworld.simulation.social.dynasty;

/**
 * Political / social condition of a dynasty at the scenario start.
 *
 * Temporal existence is separately represented through foundedYear,
 * extinctYear and activeAtScenarioStart.
 */
public enum DynastyStatus {

    /**
     * Living and socially/politically active.
     */
    ACTIVE,

    /**
     * Living lineage whose members are in exile.
     */
    EXILED,

    /**
     * Living lineage which has lost its traditional holdings or
     * political position.
     */
    DISPOSSESSED,

    /**
     * Known lineage which currently has no meaningful active
     * political organization but is not confidently declared extinct.
     */
    DORMANT,

    /**
     * Lore-known dynasty or branch which is founded only after
     * the scenario start.
     *
     * It exists in the reference catalog but must not create a
     * runtime organization in 298 AC.
     */
    NOT_YET_FOUNDED,

    /**
     * No surviving lineage at the scenario date.
     */
    EXTINCT,

    /**
     * Status cannot be confidently established.
     */
    UNKNOWN
}
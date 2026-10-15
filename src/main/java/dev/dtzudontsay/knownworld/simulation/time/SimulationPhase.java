
package dev.dtzudontsay.knownworld.simulation.time;

/**
 * Batch 20.1
 *
 * Canonical phases for future simulation scheduling.
 *
 * Enum declaration order is the intended default
 * execution order within a simulation step.
 *
 * These phases are contracts only until they are
 * integrated into the live simulation coordinator.
 */
public enum SimulationPhase {

    /**
     * Physical proximity and simulation fidelity.
     */
    ACTIVATION,

    /**
     * Individual short-term needs.
     */
    NEEDS,

    /**
     * Goal selection and individual decision-making.
     */
    DECISIONS,

    /**
     * Daily schedules and routine intentions.
     */
    ROUTINES,

    /**
     * Validated execution of selected actions.
     */
    ACTIONS,

    /**
     * Birth, pregnancy, ageing and mortality.
     */
    LIFE_CYCLE,

    /**
     * Population cohorts, household composition
     * and migration.
     */
    DEMOGRAPHY,

    /**
     * Production, consumption and ownership.
     */
    ECONOMY,

    /**
     * Social encounters and abstract relationships.
     */
    SOCIAL,

    /**
     * Governance, political interests and diplomacy.
     */
    POLITICS,

    /**
     * Mobilization, logistics and armed conflict.
     */
    MILITARY,

    /**
     * Cross-system invariants and reconciliation.
     */
    RECONCILIATION,

    /**
     * Event reporting and historical observation.
     *
     * This must not be treated as the only phase
     * allowed to record events.
     */
    HISTORY
}

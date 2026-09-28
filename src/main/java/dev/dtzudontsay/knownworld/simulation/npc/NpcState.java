package dev.dtzudontsay.knownworld.simulation.npc;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;

import java.util.Objects;

/**
 * Mutable runtime state belonging to one persistent NPC.
 */
public final class NpcState {

    private final NpcIdentity identity;

    private SimulationPosition position;
    private SimulationLevel simulationLevel;
    private NpcLifeState lifeState;

    public NpcState(
            NpcIdentity identity,
            SimulationPosition position
    ) {
        this.identity =
                Objects.requireNonNull(
                        identity,
                        "identity"
                );

        this.position =
                Objects.requireNonNull(
                        position,
                        "position"
                );

        this.simulationLevel =
                SimulationLevel.DORMANT;

        this.lifeState =
                NpcLifeState.ALIVE;
    }

    public NpcIdentity identity() {
        return identity;
    }

    public NpcId id() {
        return identity.id();
    }

    public SimulationPosition position() {
        return position;
    }

    public void setPosition(
            SimulationPosition position
    ) {
        this.position =
                Objects.requireNonNull(
                        position,
                        "position"
                );
    }

    public SimulationLevel simulationLevel() {
        return simulationLevel;
    }

    public void setSimulationLevel(
            SimulationLevel simulationLevel
    ) {
        this.simulationLevel =
                Objects.requireNonNull(
                        simulationLevel,
                        "simulationLevel"
                );
    }

    public NpcLifeState lifeState() {
        return lifeState;
    }

    public boolean isAlive() {
        return lifeState == NpcLifeState.ALIVE;
    }

    public void markDead() {
        lifeState = NpcLifeState.DEAD;

        /*
         * Dead people are retained by the registry.
         *
         * They remain important for:
         * - genealogy
         * - memories
         * - inheritance
         * - historical events
         * - relationships
         * - dialogue
         */
        simulationLevel =
                SimulationLevel.DORMANT;
    }
}
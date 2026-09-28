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
        this(
                identity,
                position,
                SimulationLevel.DORMANT,
                NpcLifeState.ALIVE
        );
    }

    public NpcState(
            NpcIdentity identity,
            SimulationPosition position,
            SimulationLevel simulationLevel,
            NpcLifeState lifeState
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
                Objects.requireNonNull(
                        simulationLevel,
                        "simulationLevel"
                );

        this.lifeState =
                Objects.requireNonNull(
                        lifeState,
                        "lifeState"
                );
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

    /**
     * Package-private on purpose.
     *
     * NPC movement must normally go through NpcRegistry so the
     * spatial index remains synchronized.
     */
    void setPosition(
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
        return lifeState ==
                NpcLifeState.ALIVE;
    }

    public void markDead() {
        lifeState =
                NpcLifeState.DEAD;

        /*
         * Dead NPC records remain permanently available for history,
         * genealogy, relationships, inheritance and memories.
         */
        simulationLevel =
                SimulationLevel.DORMANT;
    }
}
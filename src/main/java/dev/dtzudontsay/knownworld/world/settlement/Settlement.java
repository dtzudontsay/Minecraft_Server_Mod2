package dev.dtzudontsay.knownworld.simulation.world.settlement;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;

import java.util.Objects;

/**
 * Persistent named place used by the social/world simulation.
 *
 * A settlement is not merely a coordinate.
 *
 * Later it can own:
 *
 * - population
 * - buildings
 * - markets
 * - food stores
 * - defenses
 * - workplaces
 * - political control
 * - military presence
 */
public final class Settlement {

    private final SettlementId id;

    private final String name;

    private final SettlementType type;

    private final SimulationPosition center;

    public Settlement(
            SettlementId id,
            String name,
            SettlementType type,
            SimulationPosition center
    ) {
        this.id =
                Objects.requireNonNull(
                        id,
                        "id"
                );

        Objects.requireNonNull(
                name,
                "name"
        );

        this.name =
                name.trim();

        if (this.name.isEmpty()) {
            throw new IllegalArgumentException(
                    "Settlement name cannot be empty"
            );
        }

        this.type =
                Objects.requireNonNull(
                        type,
                        "type"
                );

        this.center =
                Objects.requireNonNull(
                        center,
                        "center"
                );
    }

    public SettlementId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public SettlementType type() {
        return type;
    }

    public SimulationPosition center() {
        return center;
    }
}
package dev.dtzudontsay.knownworld.simulation;

import java.util.Objects;

/**
 * Persistent world-space position used by the simulation.
 *
 * Coordinates are stored independently from Minecraft entities/chunks.
 */
public record SimulationPosition(
        String dimension,
        double x,
        double y,
        double z
) {

    public static final String OVERWORLD = "minecraft:overworld";

    public SimulationPosition {
        Objects.requireNonNull(dimension, "dimension");

        if (dimension.isBlank()) {
            throw new IllegalArgumentException(
                    "dimension cannot be blank"
            );
        }

        if (!Double.isFinite(x)
                || !Double.isFinite(y)
                || !Double.isFinite(z)) {

            throw new IllegalArgumentException(
                    "Simulation coordinates must be finite"
            );
        }
    }

    public static SimulationPosition overworld(
            double x,
            double y,
            double z
    ) {
        return new SimulationPosition(
                OVERWORLD,
                x,
                y,
                z
        );
    }

    public double horizontalDistanceSquared(
            SimulationPosition other
    ) {
        Objects.requireNonNull(other, "other");

        if (!dimension.equals(other.dimension)) {
            return Double.POSITIVE_INFINITY;
        }

        double dx = x - other.x;
        double dz = z - other.z;

        return dx * dx + dz * dz;
    }

    public double horizontalDistance(
            SimulationPosition other
    ) {
        return Math.sqrt(
                horizontalDistanceSquared(other)
        );
    }
}
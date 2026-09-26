package dev.dtzudontsay.knownworld.world.geography;

public record WorldCoordinate(double eastMetres, double northMetres) {

    public double distanceTo(WorldCoordinate other) {
        double eastDelta = other.eastMetres() - eastMetres;
        double northDelta = other.northMetres() - northMetres;
        return Math.hypot(eastDelta, northDelta);
    }
}
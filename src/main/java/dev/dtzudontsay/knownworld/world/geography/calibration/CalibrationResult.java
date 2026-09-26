package dev.dtzudontsay.knownworld.world.geography.calibration;

public record CalibrationResult(
        CalibrationStatus status,
        String primaryAnchorId,
        double metresPerPixel,
        double worldWidthMetres,
        double worldHeightMetres
) {
}
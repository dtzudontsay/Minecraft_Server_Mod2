package dev.dtzudontsay.knownworld.simulation.npc.religion;

public record ReligionConversionResult(
        String previousReligionId,
        String targetReligionId,
        double rawPressure,
        double socialSusceptibility,
        double sourceFactor,
        double regionalSupport,
        double openness,
        double currentFaithResistance,
        double effectivePressure,
        double previousMomentum,
        double newMomentum,
        boolean converted
) {
}

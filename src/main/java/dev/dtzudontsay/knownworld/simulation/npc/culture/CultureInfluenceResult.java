package dev.dtzudontsay.knownworld.simulation.npc.culture;

public record CultureInfluenceResult(
        String previousCultureId,
        String targetCultureId,
        double rawPressure,
        double socialSusceptibility,
        double sourceFactor,
        double regionalSupport,
        double assimilationOpenness,
        double heritageResistance,
        double culturalDistance,
        double effectivePressure,
        double previousAffinity,
        double newAffinity,
        double previousMomentum,
        double newMomentum,
        boolean cultureChanged
) {
}
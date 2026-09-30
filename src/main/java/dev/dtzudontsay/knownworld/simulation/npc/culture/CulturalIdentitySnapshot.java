package dev.dtzudontsay.knownworld.simulation.npc.culture;

import java.util.Map;

public record CulturalIdentitySnapshot(
        String primaryCultureId,
        double identityImportance,
        double heritageAttachment,
        double assimilationOpenness,
        double multiculturalIdentity,
        Map<String, Double> culturalAffinities,
        Map<String, Double> languages
) {
}
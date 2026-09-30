package dev.dtzudontsay.knownworld.world.reference.influence;

public record RegionalInfluenceResolution(
        String territoryId,
        String subregionId,
        RegionalInfluenceProfile territoryProfile,
        RegionalInfluenceProfile subregionProfile,
        RegionalInfluenceProfile effectiveProfile
) {

    public boolean hasSubregion() {
        return subregionId != null;
    }

    public boolean hasRefinement() {
        return subregionProfile != null;
    }

    public String mostSpecificLocationId() {

        if (subregionId != null) {
            return subregionId;
        }

        return territoryId;
    }
}
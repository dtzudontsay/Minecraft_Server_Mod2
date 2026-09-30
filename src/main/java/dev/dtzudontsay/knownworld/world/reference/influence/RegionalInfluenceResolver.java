package dev.dtzudontsay.knownworld.world.reference.influence;

import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.spatial.RegionalSubregionResolver;
import dev.dtzudontsay.knownworld.world.reference.spatial.TerritoryZoneResolver;

import java.util.Optional;

public final class RegionalInfluenceResolver {

    private RegionalInfluenceResolver() {
    }

    public static Optional<RegionalInfluenceResolution> resolveMinecraft(
            double minecraftX,
            double minecraftZ
    ) {

        WorldLocationDefinition territory =
                TerritoryZoneResolver.resolveMinecraft(
                                minecraftX,
                                minecraftZ
                        )
                        .orElse(
                                null
                        );

        if (territory == null) {
            return Optional.empty();
        }

        RegionalInfluenceCatalog catalog =
                RegionalInfluenceCatalog.get();

        RegionalInfluenceProfile territoryProfile =
                catalog.find(
                                territory.id()
                        )
                        .orElse(
                                RegionalInfluenceProfile.empty(
                                        territory.id()
                                )
                        );

        WorldLocationDefinition subregion =
                RegionalSubregionResolver.resolveMinecraft(
                                minecraftX,
                                minecraftZ
                        )
                        .orElse(
                                null
                        );

        RegionalInfluenceProfile refinement =
                subregion == null
                        ? null
                        : catalog.find(
                        subregion.id()
                ).orElse(
                        null
                );

        RegionalInfluenceProfile effective =
                refinement == null
                        ? territoryProfile
                        : RegionalInfluenceProfile.merge(
                        territoryProfile,
                        refinement
                );

        return Optional.of(
                new RegionalInfluenceResolution(
                        territory.id(),
                        subregion == null
                                ? null
                                : subregion.id(),
                        territoryProfile,
                        refinement,
                        effective
                )
        );
    }
}
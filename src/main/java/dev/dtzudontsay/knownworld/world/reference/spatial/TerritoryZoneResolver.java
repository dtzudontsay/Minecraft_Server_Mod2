package dev.dtzudontsay.knownworld.world.reference.spatial;

import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.util.Optional;

public final class TerritoryZoneResolver {

    private TerritoryZoneResolver() {
    }

    public static Optional<WorldLocationDefinition> resolveMinecraft(
            double minecraftX,
            double minecraftZ
    ) {
        KnownWorldGeoSample sample =
                KnownWorldGeoSampler.sampleMinecraft(
                        minecraftX,
                        minecraftZ
                );

        if (!sample.insideKnownWorldMap()) {

            return Optional.empty();
        }

        if (!sample.land()) {

            return Optional.empty();
        }

        WorldLocationDefinition landmass =
                LandmassZoneResolver.resolveMinecraft(
                                minecraftX,
                                minecraftZ
                        )
                        .orElse(
                                null
                        );

        if (landmass == null) {

            return Optional.empty();
        }

        return TerritoryZoneCatalog.get()
                .resolve(
                        sample.logicalMapCoordinate(),
                        landmass.id()
                )
                .flatMap(
                        zone ->
                                WorldReferenceCatalog.get()
                                        .location(
                                                zone.locationId()
                                        )
                );
    }

    public static Optional<String> resolveLocationIdMinecraft(
            double minecraftX,
            double minecraftZ
    ) {
        return resolveMinecraft(
                minecraftX,
                minecraftZ
        )
                .map(
                        WorldLocationDefinition::id
                );
    }
}
package dev.dtzudontsay.knownworld.world.reference.spatial;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.util.Optional;

public final class LandmassZoneResolver {

    private LandmassZoneResolver() {
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

        return resolveMap(
                sample.logicalMapCoordinate()
        );
    }

    public static Optional<WorldLocationDefinition> resolveMap(
            MapCoordinate mapCoordinate
    ) {
        return LandmassZoneCatalog.get()
                .resolve(
                        mapCoordinate
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

    public static boolean isLandIn(
            double minecraftX,
            double minecraftZ,
            String locationId
    ) {
        if (locationId == null
                || locationId.isBlank()) {

            return false;
        }

        return resolveLocationIdMinecraft(
                minecraftX,
                minecraftZ
        )
                .map(
                        resolved ->
                                resolved.equalsIgnoreCase(
                                        locationId
                                )
                )
                .orElse(
                        false
                );
    }
}
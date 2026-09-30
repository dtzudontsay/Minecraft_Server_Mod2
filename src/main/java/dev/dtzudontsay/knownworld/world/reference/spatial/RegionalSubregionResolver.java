package dev.dtzudontsay.knownworld.world.reference.spatial;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneDefinition;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneId;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneRegistry;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RegionalSubregionResolver {

    private static RegionalSubregionResolver instance;

    private final Map<String, BufferedImage> imagesByZoneId =
            new LinkedHashMap<>();

    private RegionalSubregionResolver() {
    }

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        RegionalSubregionResolver resolver =
                new RegionalSubregionResolver();

        try {

            resolver.loadImages();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load regional subregion images",
                    exception
            );
        }

        instance =
                resolver;
    }

    public static RegionalSubregionResolver get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Regional subregion resolver has not been bootstrapped"
            );
        }

        return instance;
    }

    public static Optional<WorldLocationDefinition> resolveMinecraft(
            double minecraftX,
            double minecraftZ
    ) {
        return get().resolveSample(
                KnownWorldGeoSampler.sampleMinecraft(
                        minecraftX,
                        minecraftZ
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

    private Optional<WorldLocationDefinition> resolveSample(
            KnownWorldGeoSample sample
    ) {
        if (!sample.insideKnownWorldMap()) {
            return Optional.empty();
        }

        if (!sample.land()) {
            return Optional.empty();
        }

        MapCoordinate master =
                sample.logicalMapCoordinate();

        return RegionalSubregionCatalog.get()
                .all()
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                RegionalSubregionMaskDefinition::priority
                        ).reversed()
                )
                .map(
                        mask ->
                                resolveInMask(
                                        master,
                                        mask
                                )
                )
                .filter(
                        Optional::isPresent
                )
                .map(
                        Optional::get
                )
                .findFirst();
    }

    private Optional<WorldLocationDefinition> resolveInMask(
            MapCoordinate master,
            RegionalSubregionMaskDefinition mask
    ) {
        BufferedImage image =
                imagesByZoneId.get(
                        mask.zoneId()
                );

        if (image == null) {
            return Optional.empty();
        }

        Point local =
                projectMasterToZonePixel(
                        master,
                        mask
                );

        if (local == null) {
            return Optional.empty();
        }

        int argb =
                image.getRGB(
                        local.x,
                        local.y
                );

        int alpha =
                (argb >>> 24)
                        & 0xFF;

        if (alpha == 0) {
            return Optional.empty();
        }

        int rgb =
                argb
                        & 0xFFFFFF;

        return mask.lookup(
                        rgb
                )
                .flatMap(
                        entry ->
                                WorldReferenceCatalog.get()
                                        .location(
                                                entry.locationId()
                                        )
                );
    }

    private Point projectMasterToZonePixel(
            MapCoordinate master,
            RegionalSubregionMaskDefinition mask
    ) {
        MapZoneDefinition zone =
                MapZoneRegistry.get(
                                MapZoneId.fromCode(
                                        mask.zoneId()
                                )
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Unknown map zone "
                                                        + mask.zoneId()
                                        )
                        );

        MapCoordinate masterTopLeft =
                zone.toMaster(
                        new MapCoordinate(
                                0.0,
                                0.0
                        )
                );

        MapCoordinate masterBottomRight =
                zone.toMaster(
                        new MapCoordinate(
                                mask.width() - 1.0,
                                mask.height() - 1.0
                        )
                );

        double x0 =
                masterTopLeft.pixelX();

        double y0 =
                masterTopLeft.pixelY();

        double x1 =
                masterBottomRight.pixelX();

        double y1 =
                masterBottomRight.pixelY();

        double minX =
                Math.min(
                        x0,
                        x1
                );

        double maxX =
                Math.max(
                        x0,
                        x1
                );

        double minY =
                Math.min(
                        y0,
                        y1
                );

        double maxY =
                Math.max(
                        y0,
                        y1
                );

        if (master.pixelX()
                < minX
                || master.pixelX()
                > maxX
                || master.pixelY()
                < minY
                || master.pixelY()
                > maxY) {

            return null;
        }

        double localX =
                (
                        master.pixelX()
                                - x0
                )
                        * (
                        mask.width() - 1.0
                )
                        / (
                        x1 - x0
                );

        double localY =
                (
                        master.pixelY()
                                - y0
                )
                        * (
                        mask.height() - 1.0
                )
                        / (
                        y1 - y0
                );

        int pixelX =
                (int) Math.round(
                        localX
                );

        int pixelY =
                (int) Math.round(
                        localY
                );

        if (pixelX < 0
                || pixelX >= mask.width()
                || pixelY < 0
                || pixelY >= mask.height()) {

            return null;
        }

        return new Point(
                pixelX,
                pixelY
        );
    }

    private void loadImages() throws IOException {

        for (
                RegionalSubregionMaskDefinition mask :
                RegionalSubregionCatalog.get()
                        .all()
        ) {

            try (
                    InputStream input =
                            RegionalSubregionResolver.class
                                    .getClassLoader()
                                    .getResourceAsStream(
                                            mask.imageResource()
                                    )
            ) {

                if (input == null) {

                    throw new IOException(
                            "Missing subregion image resource: "
                                    + mask.imageResource()
                    );
                }

                BufferedImage image =
                        ImageIO.read(
                                input
                        );

                if (image == null) {

                    throw new IOException(
                            "Could not decode image resource: "
                                    + mask.imageResource()
                    );
                }

                if (image.getWidth()
                        != mask.width()
                        || image.getHeight()
                        != mask.height()) {

                    throw new IOException(
                            "Subregion image "
                                    + mask.imageResource()
                                    + " has unexpected dimensions "
                                    + image.getWidth()
                                    + "x"
                                    + image.getHeight()
                                    + " (expected "
                                    + mask.width()
                                    + "x"
                                    + mask.height()
                                    + ")"
                    );
                }

                imagesByZoneId.put(
                        mask.zoneId(),
                        image
                );
            }
        }
    }
}
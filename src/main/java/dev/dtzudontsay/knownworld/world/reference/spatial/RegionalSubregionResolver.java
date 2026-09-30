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

        Point pixel =
                masterToRegionalPixel(
                        master,
                        mask
                );

        if (pixel == null) {
            return Optional.empty();
        }

        int argb =
                image.getRGB(
                        pixel.x,
                        pixel.y
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

    private Point masterToRegionalPixel(
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

        /*
         * IMPORTANT:
         *
         * Regional maps are calibrated through a full affine
         * transformation. Do not reconstruct the inverse from
         * a rectangular bounding box because the calibration can
         * contain small skew / rotation terms.
         *
         * RegionalMapTransform already provides the exact inverse.
         */
        MapCoordinate regional =
                zone.transform()
                        .toRegional(
                                master
                        );

        double regionalX =
                regional.pixelX();

        double regionalY =
                regional.pixelY();

        if (regionalX < 0.0
                || regionalY < 0.0
                || regionalX >= mask.width()
                || regionalY >= mask.height()) {

            return null;
        }

        int pixelX =
                (int) Math.floor(
                        regionalX
                );

        int pixelY =
                (int) Math.floor(
                        regionalY
                );

        if (pixelX < 0
                || pixelY < 0
                || pixelX >= imageWidth(
                mask
        )
                || pixelY >= imageHeight(
                mask
        )) {

            return null;
        }

        return new Point(
                pixelX,
                pixelY
        );
    }

    private static int imageWidth(
            RegionalSubregionMaskDefinition mask
    ) {
        return mask.width();
    }

    private static int imageHeight(
            RegionalSubregionMaskDefinition mask
    ) {
        return mask.height();
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
package dev.dtzudontsay.knownworld.world.reference.spatial;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneId;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class RegionalSubregionCatalog {

    private static final Gson GSON =
            new Gson();

    private static final String RESOURCE =
            "data/knownworld/reference/subregion_masks.json";

    private static RegionalSubregionCatalog instance;

    private final List<RegionalSubregionMaskDefinition> masks =
            new ArrayList<>();

    private RegionalSubregionCatalog() {
    }

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        RegionalSubregionCatalog catalog =
                new RegionalSubregionCatalog();

        try {

            catalog.load();

            catalog.validate();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load subregion mask catalog",
                    exception
            );
        }

        instance =
                catalog;

        KnownWorld.LOGGER.info(
                "Regional subregion catalog loaded with {} mask files and {} total color entries.",
                catalog.masks.size(),
                catalog.totalEntryCount()
        );
    }

    public static RegionalSubregionCatalog get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Regional subregion catalog has not been bootstrapped"
            );
        }

        return instance;
    }

    public List<RegionalSubregionMaskDefinition> all() {
        return List.copyOf(
                masks
        );
    }

    public int size() {
        return masks.size();
    }

    public int totalEntryCount() {

        int total =
                0;

        for (
                RegionalSubregionMaskDefinition mask :
                masks
        ) {

            total +=
                    mask.entryCount();
        }

        return total;
    }

    private void load() throws IOException {

        MaskData[] data =
                readJson(
                        RESOURCE,
                        MaskData[].class
                );

        for (
                MaskData entry :
                data
        ) {

            masks.add(
                    createMask(
                            entry
                    )
            );
        }
    }

    private RegionalSubregionMaskDefinition createMask(
            MaskData data
    ) {
        if (data.zoneId == null
                || data.zoneId.isBlank()) {

            throw new IllegalArgumentException(
                    "Subregion mask requires zoneId"
            );
        }

        if (data.imageResource == null
                || data.imageResource.isBlank()) {

            throw new IllegalArgumentException(
                    "Subregion mask "
                            + data.zoneId
                            + " requires imageResource"
            );
        }

        if (data.width <= 0
                || data.height <= 0) {

            throw new IllegalArgumentException(
                    "Subregion mask "
                            + data.zoneId
                            + " requires valid width and height"
            );
        }

        if (data.entries == null
                || data.entries.length == 0) {

            throw new IllegalArgumentException(
                    "Subregion mask "
                            + data.zoneId
                            + " requires at least one color entry"
            );
        }

        List<SubregionColorDefinition> entries =
                new ArrayList<>();

        for (
                MaskColorData color :
                data.entries
        ) {

            entries.add(
                    new SubregionColorDefinition(
                            color.hexColor,
                            color.locationId
                    )
            );
        }

        return new RegionalSubregionMaskDefinition(
                data.zoneId,
                data.imageResource,
                data.width,
                data.height,
                data.priority,
                entries
        );
    }

    private void validate() throws IOException {

        WorldReferenceCatalog references =
                WorldReferenceCatalog.get();

        for (
                RegionalSubregionMaskDefinition mask :
                masks
        ) {

            MapZoneId.fromCode(
                    mask.zoneId()
            );

            try (
                    InputStream input =
                            RegionalSubregionCatalog.class
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
            }

            for (
                    SubregionColorDefinition entry :
                    mask.allEntries()
            ) {

                if (references.location(
                        entry.locationId()
                ).isEmpty()) {

                    throw new IllegalStateException(
                            "Subregion color "
                                    + entry.hexColor()
                                    + " in zone "
                                    + mask.zoneId()
                                    + " references unknown location "
                                    + entry.locationId()
                    );
                }
            }
        }
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        RegionalSubregionCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Reference resource not found: "
                                + resource
                );
            }

            try (
                    Reader reader =
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
            ) {

                T value =
                        GSON.fromJson(
                                reader,
                                type
                        );

                if (value == null) {

                    throw new IOException(
                            "Reference JSON returned null: "
                                    + resource
                    );
                }

                return value;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid JSON in "
                            + resource,
                    exception
            );
        }
    }

    private static final class MaskData {

        String zoneId;

        String imageResource;

        int width;

        int height;

        int priority;

        MaskColorData[] entries;
    }

    private static final class MaskColorData {

        String hexColor;

        String locationId;
    }
}
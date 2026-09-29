package dev.dtzudontsay.knownworld.world.reference.spatial;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class LandmassZoneCatalog {

    private static final Gson GSON =
            new Gson();

    private static final String RESOURCE =
            "data/knownworld/reference/landmass_zones.json";

    private static LandmassZoneCatalog instance;

    private final Map<String, LandmassZoneDefinition> zones =
            new LinkedHashMap<>();

    private LandmassZoneCatalog() {
    }

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        LandmassZoneCatalog catalog =
                new LandmassZoneCatalog();

        try {

            catalog.load();

            catalog.validate();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load landmass zone catalog",
                    exception
            );
        }

        instance =
                catalog;

        KnownWorld.LOGGER.info(
                "Landmass zone catalog loaded with {} zones.",
                catalog.zones.size()
        );
    }

    public static LandmassZoneCatalog get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Landmass zone catalog has not been bootstrapped"
            );
        }

        return instance;
    }

    public Optional<LandmassZoneDefinition> find(
            String locationId
    ) {
        if (locationId == null) {

            return Optional.empty();
        }

        return Optional.ofNullable(
                zones.get(
                        locationId.trim()
                                .toLowerCase()
                )
        );
    }

    public Collection<LandmassZoneDefinition> all() {

        return List.copyOf(
                zones.values()
        );
    }

    public List<LandmassZoneDefinition> containing(
            MapCoordinate point
    ) {
        return zones.values()
                .stream()
                .filter(
                        zone ->
                                zone.contains(
                                        point
                                )
                )
                .sorted(
                        Comparator
                                .comparingInt(
                                        LandmassZoneDefinition::priority
                                )
                                .reversed()
                                .thenComparingDouble(
                                        LandmassZoneDefinition::totalAreaPixels
                                )
                )
                .toList();
    }

    public Optional<LandmassZoneDefinition> resolve(
            MapCoordinate point
    ) {
        List<LandmassZoneDefinition> matches =
                containing(
                        point
                );

        if (matches.isEmpty()) {

            return Optional.empty();
        }

        return Optional.of(
                matches.getFirst()
        );
    }

    public int size() {
        return zones.size();
    }

    private void load() throws IOException {

        ZoneData[] data =
                readJson(
                        RESOURCE,
                        ZoneData[].class
                );

        for (
                ZoneData entry :
                data
        ) {

            LandmassZoneDefinition definition =
                    createDefinition(
                            entry
                    );

            if (zones.putIfAbsent(
                    definition.locationId(),
                    definition
            ) != null) {

                throw new IllegalStateException(
                        "Duplicate landmass zone: "
                                + definition.locationId()
                );
            }
        }
    }

    private LandmassZoneDefinition createDefinition(
            ZoneData data
    ) {
        if (data.locationId == null
                || data.locationId.isBlank()) {

            throw new IllegalArgumentException(
                    "Landmass zone requires locationId"
            );
        }

        if (data.polygons == null
                || data.polygons.length == 0) {

            throw new IllegalArgumentException(
                    "Landmass zone "
                            + data.locationId
                            + " requires polygons"
            );
        }

        List<List<MapCoordinate>> polygons =
                new ArrayList<>();

        for (
                double[][] rawPolygon :
                data.polygons
        ) {

            if (rawPolygon == null
                    || rawPolygon.length < 3) {

                throw new IllegalArgumentException(
                        "Landmass zone "
                                + data.locationId
                                + " contains an invalid polygon"
                );
            }

            List<MapCoordinate> polygon =
                    new ArrayList<>();

            for (
                    double[] point :
                    rawPolygon
            ) {

                if (point == null
                        || point.length != 2) {

                    throw new IllegalArgumentException(
                            "Landmass polygon point must be [x, y]"
                    );
                }

                polygon.add(
                        new MapCoordinate(
                                point[0],
                                point[1]
                        )
                );
            }

            polygons.add(
                    polygon
            );
        }

        return new LandmassZoneDefinition(
                data.locationId,
                data.priority,
                polygons,
                enumValue(
                        ReferenceProvenance.class,
                        data.provenance,
                        ReferenceProvenance.UNKNOWN
                ),
                data.sourceNote
        );
    }

    private void validate() {

        WorldReferenceCatalog references =
                WorldReferenceCatalog.get();

        for (
                LandmassZoneDefinition zone :
                zones.values()
        ) {

            if (references.location(
                    zone.locationId()
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Landmass zone references unknown world location: "
                                + zone.locationId()
                );
            }
        }
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value,
            E fallback
    ) {
        if (value == null
                || value.isBlank()) {

            return fallback;
        }

        return Enum.valueOf(
                type,
                value.trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
        );
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        LandmassZoneCatalog.class
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

    private static final class ZoneData {

        String locationId;

        int priority;

        double[][][] polygons;

        String provenance;

        String sourceNote;
    }
}
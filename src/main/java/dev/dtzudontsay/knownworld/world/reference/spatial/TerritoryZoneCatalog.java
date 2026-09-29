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

public final class TerritoryZoneCatalog {

    private static final Gson GSON =
            new Gson();

    private static final String RESOURCE =
            "data/knownworld/reference/territory_zones.json";

    private static TerritoryZoneCatalog instance;

    private final Map<String, TerritoryZoneDefinition> zones =
            new LinkedHashMap<>();

    private TerritoryZoneCatalog() {
    }

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        TerritoryZoneCatalog catalog =
                new TerritoryZoneCatalog();

        try {

            catalog.load();

            catalog.validate();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load territory zone catalog",
                    exception
            );
        }

        instance =
                catalog;

        KnownWorld.LOGGER.info(
                "Territory zone catalog loaded with {} zones.",
                catalog.zones.size()
        );
    }

    public static TerritoryZoneCatalog get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Territory zone catalog has not been bootstrapped"
            );
        }

        return instance;
    }

    public Optional<TerritoryZoneDefinition> find(
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

    public Collection<TerritoryZoneDefinition> all() {

        return List.copyOf(
                zones.values()
        );
    }

    public int size() {
        return zones.size();
    }

    public List<TerritoryZoneDefinition> containing(
            MapCoordinate point,
            String landmassId
    ) {
        if (landmassId == null
                || landmassId.isBlank()) {

            return List.of();
        }

        String normalizedLandmass =
                landmassId.trim()
                        .toLowerCase();

        return zones.values()
                .stream()
                .filter(
                        zone ->
                                zone.requiredLandmassId()
                                        .equals(
                                                normalizedLandmass
                                        )
                )
                .filter(
                        zone ->
                                zone.contains(
                                        point
                                )
                )
                .sorted(
                        Comparator
                                .comparingInt(
                                        TerritoryZoneDefinition::priority
                                )
                                .reversed()
                                .thenComparingDouble(
                                        TerritoryZoneDefinition::totalAreaPixels
                                )
                )
                .toList();
    }

    public Optional<TerritoryZoneDefinition> resolve(
            MapCoordinate point,
            String landmassId
    ) {
        List<TerritoryZoneDefinition> matches =
                containing(
                        point,
                        landmassId
                );

        if (matches.isEmpty()) {

            return Optional.empty();
        }

        return Optional.of(
                matches.getFirst()
        );
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

            TerritoryZoneDefinition definition =
                    createDefinition(
                            entry
                    );

            if (zones.putIfAbsent(
                    definition.locationId(),
                    definition
            ) != null) {

                throw new IllegalStateException(
                        "Duplicate territory zone: "
                                + definition.locationId()
                );
            }
        }
    }

    private TerritoryZoneDefinition createDefinition(
            ZoneData data
    ) {
        if (data.locationId == null
                || data.locationId.isBlank()) {

            throw new IllegalArgumentException(
                    "Territory zone requires locationId"
            );
        }

        if (data.requiredLandmassId == null
                || data.requiredLandmassId.isBlank()) {

            throw new IllegalArgumentException(
                    "Territory zone "
                            + data.locationId
                            + " requires requiredLandmassId"
            );
        }

        if (data.polygons == null
                || data.polygons.length == 0) {

            throw new IllegalArgumentException(
                    "Territory zone "
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
                        "Territory zone "
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
                            "Territory polygon point must be [x, y]"
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

        return new TerritoryZoneDefinition(
                data.locationId,
                data.requiredLandmassId,
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

        LandmassZoneCatalog landmasses =
                LandmassZoneCatalog.get();

        for (
                TerritoryZoneDefinition zone :
                zones.values()
        ) {

            if (references.location(
                    zone.locationId()
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Territory zone references unknown world location: "
                                + zone.locationId()
                );
            }

            if (landmasses.find(
                    zone.requiredLandmassId()
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Territory zone "
                                + zone.locationId()
                                + " references unknown landmass zone "
                                + zone.requiredLandmassId()
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
                        TerritoryZoneCatalog.class
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

        String requiredLandmassId;

        int priority;

        double[][][] polygons;

        String provenance;

        String sourceNote;
    }
}
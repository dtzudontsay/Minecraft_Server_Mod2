package dev.dtzudontsay.knownworld.world.geography.zones;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class MapZoneRegistry {

    private static final Map<MapZoneId, MapZoneDefinition> ZONES =
            new EnumMap<>(MapZoneId.class);

    static {
        registerNorthWesteros();
        registerSouthWesteros();
        registerSummerIsles();
    }

    private MapZoneRegistry() {
    }

    private static void registerNorthWesteros() {
        register(
                new MapZoneDefinition(
                        MapZoneId.NW,
                        "Official The Known World regional map: North Westeros",
                        2048,
                        1942,
                        new RegionalMapTransform(
                                0.269235463,
                                0.000032805,
                                19.5523523,

                                -0.000015746,
                                0.269241799,
                                25.6487478
                        )
                )
        );
    }

    private static void registerSouthWesteros() {
        register(
                new MapZoneDefinition(
                        MapZoneId.SW,
                        "Official The Known World regional map: South Westeros",
                        2048,
                        1876,
                        new RegionalMapTransform(
                                0.251274800,
                                -0.000013323,
                                18.0074268,

                                0.000001467,
                                0.251214149,
                                482.4480120
                        )
                )
        );
    }

    private static void registerSummerIsles() {
        register(
                new MapZoneDefinition(
                        MapZoneId.SI,
                        "Official The Known World regional map: Summer Isles",
                        2048,
                        1189,
                        new RegionalMapTransform(
                                0.358829934,
                                -0.000020605,
                                17.8553612,

                                0.000020605,
                                0.358829934,
                                915.7434305
                        )
                )
        );
    }

    private static void register(
            MapZoneDefinition zone
    ) {
        if (ZONES.containsKey(zone.id())) {
            throw new IllegalStateException(
                    "Duplicate map zone: "
                            + zone.id()
            );
        }

        ZONES.put(
                zone.id(),
                zone
        );
    }

    public static Optional<MapZoneDefinition> get(
            MapZoneId id
    ) {
        return Optional.ofNullable(
                ZONES.get(id)
        );
    }

    public static int registeredZoneCount() {
        return ZONES.size();
    }

    public static Map<MapZoneId, MapZoneDefinition> getAll() {
        return Map.copyOf(
                ZONES
        );
    }
}
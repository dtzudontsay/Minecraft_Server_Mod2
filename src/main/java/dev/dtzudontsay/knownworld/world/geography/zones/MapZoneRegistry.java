package dev.dtzudontsay.knownworld.world.geography.zones;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class MapZoneRegistry {

    private static final Map<MapZoneId, MapZoneDefinition> ZONES =
            new EnumMap<>(MapZoneId.class);

    static {
        registerNorthWesteros();
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
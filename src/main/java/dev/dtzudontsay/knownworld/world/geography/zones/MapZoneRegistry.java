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

        registerNorthWestEssos();
        registerSouthWestEssos();
        registerNorthEssos();
        registerSouthEssos();

        registerNorthEastEssos();
        registerSouthEastEssos();

        registerSothoryos();
        registerUlthos();
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

    private static void registerNorthWestEssos() {
        register(
                new MapZoneDefinition(
                        MapZoneId.NWE,
                        "Official The Known World regional map: North-West Essos",
                        2048,
                        1769,
                        new RegionalMapTransform(
                                0.321658128,
                                -0.000005214,
                                515.2110972,

                                0.000044536,
                                0.321670459,
                                175.8865722
                        )
                )
        );
    }

    private static void registerSouthWestEssos() {
        register(
                new MapZoneDefinition(
                        MapZoneId.SWE,
                        "Official The Known World regional map: South-West Essos",
                        1683,
                        1228,
                        new RegionalMapTransform(
                                0.331419945,
                                -0.000010275,
                                496.911538,

                                -0.000029005,
                                0.331324618,
                                733.121605
                        )
                )
        );
    }

    private static void registerNorthEssos() {
        register(
                new MapZoneDefinition(
                        MapZoneId.NE,
                        "Official The Known World regional map: North Essos",
                        1428,
                        1515,
                        new RegionalMapTransform(
                                0.321896842,
                                -0.000026834,
                                1099.512758,

                                0.000005474,
                                0.321821671,
                                271.467371
                        )
                )
        );
    }

    private static void registerSouthEssos() {
        register(
                new MapZoneDefinition(
                        MapZoneId.SE,
                        "Official The Known World regional map: South Essos",
                        1645,
                        1564,
                        new RegionalMapTransform(
                                0.321698811,
                                0.000000142,
                                1033.633106,

                                -0.000139931,
                                0.321974235,
                                727.704597
                        )
                )
        );
    }

    private static void registerNorthEastEssos() {
        register(
                new MapZoneDefinition(
                        MapZoneId.NEE,
                        "Official The Known World regional map: North-East Essos",
                        1464,
                        1836,
                        new RegionalMapTransform(
                                0.321926267,
                                -0.000027815,
                                1558.229390,

                                0.000002935,
                                0.321844385,
                                307.164707
                        )
                )
        );
    }

    private static void registerSouthEastEssos() {
        register(
                new MapZoneDefinition(
                        MapZoneId.SEE,
                        "Official The Known World regional map: South-East Essos",
                        1609,
                        1161,
                        new RegionalMapTransform(
                                0.321935107,
                                -0.000015512,
                                1527.285771,

                                -0.000012575,
                                0.321831998,
                                900.000856
                        )
                )
        );
    }

    private static void registerSothoryos() {
        register(
                new MapZoneDefinition(
                        MapZoneId.SO,
                        "Official The Known World regional map: Sothoryos",
                        2048,
                        721,
                        new RegionalMapTransform(
                                0.353392311,
                                0.000158547,
                                831.249112,

                                -0.000044447,
                                0.353120367,
                                1093.184561
                        )
                )
        );
    }

    private static void registerUlthos() {
        register(
                new MapZoneDefinition(
                        MapZoneId.UL,
                        "Official The Known World regional map: Ulthos",
                        1083,
                        607,
                        new RegionalMapTransform(
                                0.292626437,
                                0.000017240,
                                1722.114368,

                                -0.000054948,
                                0.292531687,
                                1172.719604
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
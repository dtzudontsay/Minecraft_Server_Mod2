package dev.dtzudontsay.knownworld.world.geography.zones;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class MapZoneRegistry {

    private static final Map<MapZoneId, MapZoneDefinition> ZONES =
            new EnumMap<>(
                    MapZoneId.class
            );

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
                        2073,
                        1966,
                        new RegionalMapTransform(
                                0.26598696561824325,
                                0.00003240432824427481,
                                19.5523523,

                                -0.000015556014478764482,
                                0.2659533495465649,
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
                        2067,
                        1894,
                        new RegionalMapTransform(
                                0.24896394753146178,
                                -0.000013196315372424722,
                                18.0074268,

                                0.0000014535087124878994,
                                0.2488254249207607,
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
                        2952,
                        1714,
                        new RegionalMapTransform(
                                0.24890710772551677,
                                -0.000014289982486865149,
                                17.8553612,

                                0.0000142929295154185,
                                0.2488557861015762,
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
                        2251,
                        1945,
                        new RegionalMapTransform(
                                0.292637416896,
                                -0.00000474195061728395,
                                515.2110972,

                                0.00004051786311111111,
                                0.29254803061316875,
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
                        2248,
                        792,
                        new RegionalMapTransform(
                                0.3219377216809079,
                                0.00014431585335018964,
                                831.249112,

                                -0.00004049088072986204,
                                0.3214243542857143,
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

        if (
                ZONES.containsKey(
                        zone.id()
                )
        ) {

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
                ZONES.get(
                        id
                )
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
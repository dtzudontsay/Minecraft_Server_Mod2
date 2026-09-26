package dev.dtzudontsay.knownworld.world.geography;

public enum GeographicFeatureType {

    REGION(
            FeatureGeometryType.AREA,
            true
    ),

    FOREST(
            FeatureGeometryType.AREA,
            true
    ),

    GRASSLAND(
            FeatureGeometryType.AREA,
            true
    ),

    MOUNTAIN(
            FeatureGeometryType.AREA,
            true
    ),

    MOUNTAIN_RANGE(
            FeatureGeometryType.AREA,
            true
    ),

    HILLS(
            FeatureGeometryType.AREA,
            true
    ),

    VALLEY(
            FeatureGeometryType.AREA,
            true
    ),

    FIELD(
            FeatureGeometryType.AREA,
            true
    ),

    DESERT(
            FeatureGeometryType.AREA,
            true
    ),

    CANYON(
            FeatureGeometryType.AREA,
            true
    ),

    ISLAND(
            FeatureGeometryType.AREA,
            true
    ),

    ISLAND_GROUP(
            FeatureGeometryType.AREA,
            true
    ),

    PENINSULA(
            FeatureGeometryType.AREA,
            true
    ),

    CAPE(
            FeatureGeometryType.AREA,
            true
    ),

    COAST(
            FeatureGeometryType.AREA,
            true
    ),

    SHORE(
            FeatureGeometryType.AREA,
            true
    ),

    CLIFFS(
            FeatureGeometryType.AREA,
            true
    ),

    BAY(
            FeatureGeometryType.AREA,
            false
    ),

    SOUND(
            FeatureGeometryType.AREA,
            false
    ),

    SEA(
            FeatureGeometryType.AREA,
            false
    ),

    LAKE(
            FeatureGeometryType.AREA,
            false
    ),

    STRAIT(
            FeatureGeometryType.AREA,
            false
    ),

    RIVER(
            FeatureGeometryType.LINE,
            false
    ),

    ROAD(
            FeatureGeometryType.LINE,
            false
    ),

    PASS(
            FeatureGeometryType.LINE,
            false
    ),

    WALL(
            FeatureGeometryType.LINE,
            false
    ),

    FORTRESS_GROUP(
            FeatureGeometryType.AREA,
            true
    ),

    CITY_GROUP(
            FeatureGeometryType.AREA,
            true
    ),

    CASTLE(
            FeatureGeometryType.POINT,
            true
    ),

    CITY(
            FeatureGeometryType.POINT,
            true
    ),

    TOWN(
            FeatureGeometryType.POINT,
            true
    ),

    RUINED_CITY(
            FeatureGeometryType.POINT,
            true
    ),

    SETTLEMENT(
            FeatureGeometryType.POINT,
            true
    );

    private final FeatureGeometryType geometryType;
    private final boolean defaultShowEntryTitle;

    GeographicFeatureType(
            FeatureGeometryType geometryType,
            boolean defaultShowEntryTitle
    ) {
        this.geometryType = geometryType;
        this.defaultShowEntryTitle = defaultShowEntryTitle;
    }

    public FeatureGeometryType geometryType() {
        return geometryType;
    }

    public boolean defaultShowEntryTitle() {
        return defaultShowEntryTitle;
    }
}
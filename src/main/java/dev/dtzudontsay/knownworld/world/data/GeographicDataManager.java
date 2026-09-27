package dev.dtzudontsay.knownworld.world.data;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldDefinition;
import dev.dtzudontsay.knownworld.world.terrain.RasterTerrainProvider;
import dev.dtzudontsay.knownworld.world.terrain.TerrainProvider;
import dev.dtzudontsay.knownworld.world.terrain.TerrainSample;

public final class GeographicDataManager {

    private static final GeographicDataManager INSTANCE =
            new GeographicDataManager();

    private final RasterTerrainProvider terrainProvider;

    private GeographicDataManager() {

        terrainProvider =
                new RasterTerrainProvider();
    }

    public static GeographicDataManager getInstance() {

        return INSTANCE;
    }

    public TerrainSample sample(
            WorldCoordinate coordinate
    ) {

        return terrainProvider.sample(
                coordinate
        );
    }

    public boolean hasCanonicalElevationData() {

        return terrainProvider
                .hasCanonicalElevationData();
    }

    public String datasetState() {

        return WorldDefinition.DATASET_STATE;
    }

    public String primaryCanon() {

        return WorldDefinition.PRIMARY_CANON;
    }
}
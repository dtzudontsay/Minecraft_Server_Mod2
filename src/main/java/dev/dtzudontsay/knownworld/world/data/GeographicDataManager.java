package dev.dtzudontsay.knownworld.world.data;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldDefinition;
import dev.dtzudontsay.knownworld.world.terrain.PlaceholderTerrainProvider;
import dev.dtzudontsay.knownworld.world.terrain.TerrainProvider;
import dev.dtzudontsay.knownworld.world.terrain.TerrainSample;

public final class GeographicDataManager {
    private static final GeographicDataManager INSTANCE =
            new GeographicDataManager();

    private final TerrainProvider terrainProvider;

    private GeographicDataManager() {
        this.terrainProvider = new PlaceholderTerrainProvider();
    }

    public static GeographicDataManager getInstance() {
        return INSTANCE;
    }

    public TerrainSample sample(WorldCoordinate coordinate) {
        return terrainProvider.sample(coordinate);
    }

    public String datasetState() {
        return WorldDefinition.DATASET_STATE;
    }

    public String primaryCanon() {
        return WorldDefinition.PRIMARY_CANON;
    }
}
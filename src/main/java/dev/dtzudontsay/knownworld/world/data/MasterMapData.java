package dev.dtzudontsay.knownworld.world.data;

import dev.dtzudontsay.knownworld.world.geography.MapAnchorRegistry;
import dev.dtzudontsay.knownworld.world.geography.MapPixelBounds;
import dev.dtzudontsay.knownworld.world.geography.MasterMapDefinition;

public final class MasterMapData {

    private static final MasterMapData INSTANCE =
            new MasterMapData();

    private MasterMapData() {
    }

    public static MasterMapData getInstance() {
        return INSTANCE;
    }

    public String mapId() {
        return MasterMapDefinition.MAP_ID;
    }

    public String sourceName() {
        return MasterMapDefinition.SOURCE_NAME;
    }

    public int imageWidthPixels() {
        return MasterMapDefinition.IMAGE_WIDTH_PIXELS;
    }

    public int imageHeightPixels() {
        return MasterMapDefinition.IMAGE_HEIGHT_PIXELS;
    }

    public MapPixelBounds imageBounds() {
        return MasterMapDefinition.FULL_IMAGE_BOUNDS;
    }

    public boolean allowsGenerationOutsideMap() {
        return MasterMapDefinition.ALLOW_GENERATION_OUTSIDE_MAP;
    }

    public String outerBoundaryMode() {
        return MasterMapDefinition.OUTER_BOUNDARY_MODE;
    }

    public int anchorCount() {
        return MapAnchorRegistry.getAnchorCount();
    }
}
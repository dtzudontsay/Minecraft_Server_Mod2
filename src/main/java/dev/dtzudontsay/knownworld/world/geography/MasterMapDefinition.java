package dev.dtzudontsay.knownworld.world.geography;

public final class MasterMapDefinition {

    public static final String MAP_ID =
            "known_world_master";

    public static final String SOURCE_NAME =
            "The Known World";

    public static final int IMAGE_WIDTH_PIXELS =
            2048;

    public static final int IMAGE_HEIGHT_PIXELS =
            1357;

    public static final MapPixelBounds FULL_IMAGE_BOUNDS =
            new MapPixelBounds(
                    0,
                    0,
                    IMAGE_WIDTH_PIXELS,
                    IMAGE_HEIGHT_PIXELS
            );

    public static final boolean ALLOW_GENERATION_OUTSIDE_MAP =
            false;

    public static final String OUTER_BOUNDARY_MODE =
            "VISIBLE_WORLD_BORDER";

    public static final String CALIBRATION_ANCHOR =
            "THE_WALL";

    private MasterMapDefinition() {
    }
}
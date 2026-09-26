package dev.dtzudontsay.knownworld.world.geography;

public final class WorldDefinition {

    public static final String PROJECT_NAME =
            "Known World";

    public static final String PRIMARY_CANON =
            "BOOKS_AND_OFFICIAL_ASOIAF_MAPS";

    public static final String DATASET_STATE =
            "MASTER_MAP_CALIBRATION_PROVISIONAL";

    /*
     * Canonical geographic scale.
     *
     * Our source data always remains in real metres.
     */
    public static final double HORIZONTAL_METRES_PER_BLOCK =
            1.0;

    /*
     * Minecraft presentation scale.
     *
     * 1.00 = full 1:1 world
     * 0.50 = half-size world
     * 0.25 = quarter-size world
     * 0.10 = one tenth size
     *
     * Keep this at 1.0 for now.
     */
    public static final double MINECRAFT_WORLD_SCALE =
            1.0;

    private WorldDefinition() {
    }
}
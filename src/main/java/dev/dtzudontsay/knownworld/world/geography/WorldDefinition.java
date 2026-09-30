package dev.dtzudontsay.knownworld.world.geography;


public final class WorldDefinition {

    public static final String PROJECT_NAME =
            "Known World";


    public static final String PRIMARY_CANON =
            "BOOKS_AND_OFFICIAL_ASOIAF_MAPS";


    public static final String DATASET_STATE =
            "MASTER_MAP_CALIBRATION_PROVISIONAL";


    /*
     * ============================================================
     * WORLD SCALE
     * ============================================================
     *
     * Canonical geographic scale.
     *
     * Source data remains expressed in real metres.
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


    /*
     * ============================================================
     * LOWLAND TERRAIN TOGGLE
     * ============================================================
     *
     * true:
     *
     *     Adds deterministic vanilla-like rolling terrain to
     *     ordinary non-mountain land.
     *
     * false:
     *
     *     Disables the new lowland terrain layer and returns ordinary
     *     terrain to the previous near-flat behaviour.
     *
     * This does NOT disable:
     *
     *     - canonical mountains
     *     - Mother of Mountains
     *     - mountain height profiles
     *     - macro elevation
     *     - coastlines
     *     - ocean terrain
     *
     * This also does NOT enable Minecraft's vanilla terrain
     * generator.
     */

    public static final boolean ENABLE_LOWLAND_TERRAIN =
            true;


    private WorldDefinition() {
    }
}
package dev.dtzudontsay.knownworld.client.atlas;

/** Independent, client-only map switches. Changing ENABLED does not touch worldgen. */
public final class KnownWorldAtlasConfig {
    /** Set this to false to remove the M-key atlas entirely. */
    public static final boolean ENABLED = true;

    public static final int MAP_WIDTH = 2048;
    public static final int MAP_HEIGHT = 1357;
    // EXACTLY the same Wall-based provisional master calibration as zone_transforms.py.
    public static final double METRES_PER_MASTER_PIXEL =
            482_803.2 / Math.hypot(80.0, -7.5);
    public static final double MASTER_ORIGIN_X = 1024.0;
    public static final double MASTER_ORIGIN_Y = 678.5;

    public static final double INITIAL_ZOOM = 2.0;
    public static final double MAX_ZOOM = 15.0;

    private KnownWorldAtlasConfig() { }
}

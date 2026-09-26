package dev.dtzudontsay.knownworld.world.geography.calibration;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;

import java.util.List;

public final class CalibrationRegistry {

    public static final DistanceCalibrationAnchor THE_WALL =
            new DistanceCalibrationAnchor(
                    "the_wall",
                    "The Wall",
                    new MapCoordinate(
                            357.4,
                            228.9
                    ),
                    new MapCoordinate(
                            437.4,
                            221.4
                    ),
                    482_803.2,
                    "The Wall is canonically 300 miles long; "
                            + "pixel endpoints measured from the marked "
                            + "2048x1357 Known World master map."
            );

    private static final List<DistanceCalibrationAnchor> ANCHORS =
            List.of(
                    THE_WALL
            );

    private CalibrationRegistry() {
    }

    public static List<DistanceCalibrationAnchor> getAnchors() {
        return ANCHORS;
    }

    public static DistanceCalibrationAnchor primaryAnchor() {
        return THE_WALL;
    }

    public static int anchorCount() {
        return ANCHORS.size();
    }
}
package dev.dtzudontsay.knownworld.world.geography;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MapAnchorRegistry {

    private static final List<CanonicalAnchor> ANCHORS =
            new ArrayList<>();

    private MapAnchorRegistry() {
    }

    public static void register(CanonicalAnchor anchor) {
        ANCHORS.add(anchor);
    }

    public static List<CanonicalAnchor> getAnchors() {
        return Collections.unmodifiableList(ANCHORS);
    }

    public static int getAnchorCount() {
        return ANCHORS.size();
    }

    public static void clear() {
        ANCHORS.clear();
    }
}
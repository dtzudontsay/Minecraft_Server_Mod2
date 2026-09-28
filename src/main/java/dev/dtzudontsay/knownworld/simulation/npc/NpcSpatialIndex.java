package dev.dtzudontsay.knownworld.simulation.npc;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lightweight spatial index for simulated NPCs.
 *
 * NPCs are grouped into large world-space cells so nearby queries do
 * not need to scan the complete NPC population.
 *
 * This index contains simulation identities, not Minecraft entities.
 */
final class NpcSpatialIndex {

    /*
     * 256x256 block simulation cells.
     *
     * This is deliberately much larger than a Minecraft chunk.
     * The simulation does not care about chunk boundaries.
     */
    private static final double CELL_SIZE =
            256.0;

    private final Map<CellKey, Set<NpcId>> cells =
            new HashMap<>();

    public void add(
            NpcId id,
            SimulationPosition position
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(position, "position");

        CellKey key =
                keyFor(position);

        cells.computeIfAbsent(
                        key,
                        ignored ->
                                new LinkedHashSet<>()
                )
                .add(id);
    }

    public void remove(
            NpcId id,
            SimulationPosition position
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(position, "position");

        CellKey key =
                keyFor(position);

        Set<NpcId> ids =
                cells.get(key);

        if (ids == null) {
            return;
        }

        ids.remove(id);

        if (ids.isEmpty()) {
            cells.remove(key);
        }
    }

    public void move(
            NpcId id,
            SimulationPosition previous,
            SimulationPosition next
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(next, "next");

        CellKey previousKey =
                keyFor(previous);

        CellKey nextKey =
                keyFor(next);

        if (previousKey.equals(nextKey)) {
            return;
        }

        remove(
                id,
                previous
        );

        add(
                id,
                next
        );
    }

    /**
     * Returns candidate NPC IDs from spatial cells intersecting the
     * supplied radius.
     *
     * Exact distance filtering is performed by NpcRegistry.
     */
    public List<NpcId> queryCandidates(
            SimulationPosition center,
            double radius
    ) {
        Objects.requireNonNull(
                center,
                "center"
        );

        if (!Double.isFinite(radius)
                || radius < 0.0) {

            throw new IllegalArgumentException(
                    "radius must be finite and >= 0"
            );
        }

        long minimumCellX =
                cellCoordinate(
                        center.x() - radius
                );

        long maximumCellX =
                cellCoordinate(
                        center.x() + radius
                );

        long minimumCellZ =
                cellCoordinate(
                        center.z() - radius
                );

        long maximumCellZ =
                cellCoordinate(
                        center.z() + radius
                );

        List<NpcId> result =
                new ArrayList<>();

        for (
                long cellX = minimumCellX;
                cellX <= maximumCellX;
                cellX++
        ) {
            for (
                    long cellZ = minimumCellZ;
                    cellZ <= maximumCellZ;
                    cellZ++
            ) {
                CellKey key =
                        new CellKey(
                                center.dimension(),
                                cellX,
                                cellZ
                        );

                Set<NpcId> ids =
                        cells.get(key);

                if (ids != null) {
                    result.addAll(ids);
                }
            }
        }

        return result;
    }

    private static CellKey keyFor(
            SimulationPosition position
    ) {
        return new CellKey(
                position.dimension(),
                cellCoordinate(
                        position.x()
                ),
                cellCoordinate(
                        position.z()
                )
        );
    }

    private static long cellCoordinate(
            double coordinate
    ) {
        return (long) Math.floor(
                coordinate / CELL_SIZE
        );
    }

    private record CellKey(
            String dimension,
            long x,
            long z
    ) {
    }
}
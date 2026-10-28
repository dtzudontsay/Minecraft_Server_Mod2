package dev.dtzudontsay.knownworld.simulation.population;

import java.util.ArrayList;
import java.util.List;

/**
 * B4.3A: O(1) per queried person, deterministic *estimated* journey position.
 * This is not authoritative pathfinding, an observed trip, or an economy event.
 * The world clock is the same 24,000-tick campaign day as B4.2.
 */
public final class KnownWorldB43TravelModel {
    public record Point(int x, int z) { }
    private KnownWorldB43TravelModel() { }

    public static List<Point> waypoints(Point from, Point to, int[] fort) {
        List<Point> route = new ArrayList<>();
        route.add(from);
        if (fort != null && fort.length >= 4) {
            int cx = fort[0], cz = fort[1];
            for (int radius : new int[]{fort[2], fort[3]}) {
                if (radius <= 4) continue;
                boolean a = Math.max(Math.abs(from.x() - cx), Math.abs(from.z() - cz)) < radius - 3;
                boolean b = Math.max(Math.abs(to.x() - cx), Math.abs(to.z() - cz)) < radius - 3;
                if (a == b) continue;
                Point[] gates = {new Point(cx, cz - radius), new Point(cx, cz + radius),
                        new Point(cx - radius, cz), new Point(cx + radius, cz)};
                Point chosen = gates[0];
                double best = Double.POSITIVE_INFINITY;
                for (Point gate : gates) {
                    double length = distance(from, gate) + distance(gate, to);
                    if (length < best) { best = length; chosen = gate; }
                }
                // A gate is only a route estimate. Actual passage remains
                // subject to Minecraft navigation/terrain validation.
                route.add(chosen);
                break;
            }
        }
        route.add(to);
        return List.copyOf(route);
    }

    public static Point atFraction(List<Point> route, double ratio) {
        if (route == null || route.isEmpty()) throw new IllegalArgumentException("Missing route");
        if (route.size() == 1) return route.get(0);
        double length = 0;
        for (int i = 1; i < route.size(); i++) length += distance(route.get(i - 1), route.get(i));
        if (length < 0.001) return route.get(route.size() - 1);
        double remaining = Math.min(1.0, Math.max(0.0, ratio)) * length;
        for (int i = 1; i < route.size(); i++) {
            Point a = route.get(i - 1), b = route.get(i);
            double leg = distance(a, b);
            if (remaining <= leg || i == route.size() - 1) {
                double fraction = leg < 0.001 ? 1.0 : Math.min(1.0, remaining / leg);
                return new Point((int)Math.round(a.x() + (b.x() - a.x()) * fraction),
                        (int)Math.round(a.z() + (b.z() - a.z()) * fraction));
            }
            remaining -= leg;
        }
        return route.get(route.size() - 1);
    }

    /** Work start and return-end are the deterministic B4.2 schedule boundaries. */
    public static Point commute(String activity, int tick,
                                KnownWorldB42Schedule.Snapshot schedule,
                                Point home, Point workplace, int[] fort) {
        if (schedule == null || workplace == null) return home;
        if ("MODELED_WORK_SHIFT".equals(activity)) return workplace;
        if ("COMMUTING_TO_WORK".equals(activity)) {
            double f = fraction(tick, schedule.depart(), schedule.start());
            return atFraction(waypoints(home, workplace, fort), f);
        }
        if ("COMMUTING_HOME".equals(activity)) {
            double f = fraction(tick, schedule.end(), schedule.returnEnd());
            return atFraction(waypoints(workplace, home, fort), f);
        }
        return home;
    }

    private static double fraction(int tick, int beginning, int ending) {
        if (ending <= beginning) return tick >= ending ? 1.0 : 0.0;
        return Math.max(0.0, Math.min(1.0, (tick - beginning) / (double)(ending - beginning)));
    }

    public static Point reconcileRecent(Point observed, long then, Point estimated, long now) {
        if (observed == null || now < then || now - then >= KnownWorldB42Schedule.DAY)
            return estimated;
        if (now == then) return observed;
        // Physical continuity is bounded to a conservative ~2.8 blocks/sec.
        double permitted = Math.min(600.0, (now - then) * 0.14);
        double distance = distance(observed, estimated);
        if (distance <= permitted) return estimated;
        if (permitted < 1.0) return observed;
        return atFraction(List.of(observed, estimated), permitted / distance);
    }

    private static double distance(Point a, Point b) {
        return Math.hypot((double)b.x() - a.x(), (double)b.z() - a.z());
    }
}

package dev.dtzudontsay.knownworld.simulation.time;

/**
 * Persistent simulation clock.
 */
public final class SimulationClock {

    public static final long TICKS_PER_DAY =
            24000L;

    private long tick;

    public SimulationClock() {
        this(
                0L
        );
    }

    public SimulationClock(
            long tick
    ) {
        setTick(
                tick
        );
    }

    public long tick() {
        return tick;
    }

    public long day() {
        return tick
                / TICKS_PER_DAY;
    }

    public int tickOfDay() {
        return (int) Math.floorMod(
                tick,
                TICKS_PER_DAY
        );
    }

    public void advance() {

        if (tick == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Simulation clock exhausted"
            );
        }

        tick++;
    }

    public void setTick(
            long tick
    ) {
        if (tick < 0) {

            throw new IllegalArgumentException(
                    "Simulation tick cannot be negative"
            );
        }

        this.tick =
                tick;
    }
}
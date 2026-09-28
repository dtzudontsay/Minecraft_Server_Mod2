package dev.dtzudontsay.knownworld.simulation.npc.need;

/**
 * Slowly changing internal pressures belonging to one NPC.
 *
 * 0.0 = no need
 * 1.0 = extreme need
 */
public final class NpcNeeds {

    private double fatigue;

    private double hunger;

    private double social;

    public NpcNeeds() {
        this(
                0.0,
                0.0,
                0.0
        );
    }

    public NpcNeeds(
            double fatigue,
            double hunger,
            double social
    ) {
        this.fatigue =
                validate(
                        fatigue,
                        "fatigue"
                );

        this.hunger =
                validate(
                        hunger,
                        "hunger"
                );

        this.social =
                validate(
                        social,
                        "social"
                );
    }

    public double fatigue() {
        return fatigue;
    }

    public double hunger() {
        return hunger;
    }

    public double social() {
        return social;
    }

    public void increaseFatigue(
            double amount
    ) {
        fatigue =
                clamp(
                        fatigue + amount
                );
    }

    public void reduceFatigue(
            double amount
    ) {
        fatigue =
                clamp(
                        fatigue - Math.max(
                                0.0,
                                amount
                        )
                );
    }

    public void increaseHunger(
            double amount
    ) {
        hunger =
                clamp(
                        hunger + amount
                );
    }

    public void reduceHunger(
            double amount
    ) {
        hunger =
                clamp(
                        hunger - Math.max(
                                0.0,
                                amount
                        )
                );
    }

    public void increaseSocial(
            double amount
    ) {
        social =
                clamp(
                        social + amount
                );
    }

    public void reduceSocial(
            double amount
    ) {
        social =
                clamp(
                        social - Math.max(
                                0.0,
                                amount
                        )
                );
    }

    private static double validate(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)
                || value < 0.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    name
                            + " must be between 0.0 and 1.0"
            );
        }

        return value;
    }

    private static double clamp(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
package dev.dtzudontsay.knownworld.simulation.npc.family;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;

/**
 * Permanent biological parentage record.
 *
 * A parent can be unknown, but at least one biological parent must
 * be known when this record is created.
 *
 * Parentage does not change when somebody is adopted, fostered,
 * legitimized or changes dynasty. Those will be separate systems.
 */
public record Parentage(
        NpcId child,
        NpcId mother,
        NpcId father,
        long birthTick
) {

    public Parentage {
        Objects.requireNonNull(
                child,
                "child"
        );

        if (mother == null
                && father == null) {

            throw new IllegalArgumentException(
                    "At least one parent must be known"
            );
        }

        if (child.equals(
                mother
        )
                || child.equals(
                father
        )) {

            throw new IllegalArgumentException(
                    "NPC cannot be their own parent"
            );
        }

        if (mother != null
                && mother.equals(
                father
        )) {

            throw new IllegalArgumentException(
                    "Mother and father cannot be the same NPC"
            );
        }

        if (birthTick < 0) {
            throw new IllegalArgumentException(
                    "birthTick cannot be negative"
            );
        }
    }
}
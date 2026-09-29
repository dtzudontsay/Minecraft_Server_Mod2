package dev.dtzudontsay.knownworld.simulation.npc.lifecycle;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;

public record Pregnancy(
        NpcId mother,
        NpcId father,
        long conceivedDay,
        long dueDay
) {

    public Pregnancy {
        Objects.requireNonNull(
                mother,
                "mother"
        );

        Objects.requireNonNull(
                father,
                "father"
        );

        if (mother.equals(
                father
        )) {

            throw new IllegalArgumentException(
                    "Mother and father cannot be the same NPC"
            );
        }

        if (conceivedDay < 0) {

            throw new IllegalArgumentException(
                    "conceivedDay cannot be negative"
            );
        }

        if (dueDay <= conceivedDay) {

            throw new IllegalArgumentException(
                    "dueDay must follow conceivedDay"
            );
        }
    }
}
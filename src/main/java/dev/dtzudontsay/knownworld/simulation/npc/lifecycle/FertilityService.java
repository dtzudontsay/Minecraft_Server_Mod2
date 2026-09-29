package dev.dtzudontsay.knownworld.simulation.npc.lifecycle;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;

import java.util.Objects;

public final class FertilityService {

    /**
     * Daily probability before age multipliers.
     *
     * Approximately 52% yearly probability if both multipliers are
     * at maximum.
     */
    private static final double BASE_CONCEPTION_CHANCE_PER_DAY =
            0.0020;

    private final NpcRegistry registry;

    private final LifeHistoryManager lifeHistory;

    private final CampaignCalendar calendar;

    public FertilityService(
            NpcRegistry registry,
            LifeHistoryManager lifeHistory,
            CampaignCalendar calendar
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.lifeHistory =
                Objects.requireNonNull(
                        lifeHistory,
                        "lifeHistory"
                );

        this.calendar =
                Objects.requireNonNull(
                        calendar,
                        "calendar"
                );
    }

    public FertilityState stateOf(
            NpcId npc
    ) {
        NpcState state =
                requireNpc(
                        npc
                );

        if (!state.isAlive()) {
            return FertilityState.DEAD;
        }

        int age =
                age(
                        npc
                );

        if (age < 16) {
            return FertilityState.TOO_YOUNG;
        }

        if (state.identity()
                .sex()
                == NpcSex.FEMALE) {

            if (age <= 34) {
                return FertilityState.FERTILE;
            }

            if (age <= 44) {
                return FertilityState.REDUCED;
            }

            return FertilityState.INFERTILE_AGE;
        }

        if (age <= 49) {
            return FertilityState.FERTILE;
        }

        if (age <= 74) {
            return FertilityState.REDUCED;
        }

        return FertilityState.INFERTILE_AGE;
    }

    public double fertilityMultiplier(
            NpcId npc
    ) {
        NpcState state =
                requireNpc(
                        npc
                );

        int age =
                age(
                        npc
                );

        if (!state.isAlive()
                || age < 16) {

            return 0.0;
        }

        if (state.identity()
                .sex()
                == NpcSex.FEMALE) {

            if (age <= 29) {
                return 1.00;
            }

            if (age <= 34) {
                return 0.80;
            }

            if (age <= 39) {
                return 0.45;
            }

            if (age <= 44) {
                return 0.15;
            }

            return 0.0;
        }

        if (age <= 39) {
            return 1.00;
        }

        if (age <= 54) {
            return 0.75;
        }

        if (age <= 69) {
            return 0.40;
        }

        if (age <= 74) {
            return 0.15;
        }

        return 0.0;
    }

    public double conceptionChancePerDay(
            NpcId mother,
            NpcId father
    ) {
        NpcState motherState =
                requireNpc(
                        mother
                );

        NpcState fatherState =
                requireNpc(
                        father
                );

        if (motherState.identity()
                .sex()
                != NpcSex.FEMALE) {

            return 0.0;
        }

        if (fatherState.identity()
                .sex()
                != NpcSex.MALE) {

            return 0.0;
        }

        return BASE_CONCEPTION_CHANCE_PER_DAY
                * fertilityMultiplier(
                mother
        )
                * fertilityMultiplier(
                father
        );
    }

    private int age(
            NpcId npc
    ) {
        return lifeHistory.ageYears(
                npc,
                calendar.absoluteDay(),
                calendar.daysPerYear()
        );
    }

    private NpcState requireNpc(
            NpcId npc
    ) {
        return registry.find(
                        npc
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC ID: "
                                                + npc
                                )
                );
    }
}
package dev.dtzudontsay.knownworld.simulation.npc.family;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;

/**
 * Persistent historical union between two NPCs.
 *
 * The same record may progress:
 *
 * BETROTHED -> MARRIED -> ENDED
 *
 * This means we retain the full relationship history instead of
 * deleting marriages when they end.
 */
public final class MarriageRecord {

    private final MarriageId id;

    private final NpcId first;

    private final NpcId second;

    private final DynastyInheritanceRule inheritanceRule;

    private final long createdTick;

    private MarriageStatus status;

    private Long marriedTick;

    private Long endedTick;

    public MarriageRecord(
            MarriageId id,
            NpcId first,
            NpcId second,
            DynastyInheritanceRule inheritanceRule,
            long createdTick,
            MarriageStatus status,
            Long marriedTick,
            Long endedTick
    ) {
        this.id =
                Objects.requireNonNull(
                        id,
                        "id"
                );

        this.first =
                Objects.requireNonNull(
                        first,
                        "first"
                );

        this.second =
                Objects.requireNonNull(
                        second,
                        "second"
                );

        if (first.equals(
                second
        )) {
            throw new IllegalArgumentException(
                    "NPC cannot marry themselves"
            );
        }

        this.inheritanceRule =
                Objects.requireNonNull(
                        inheritanceRule,
                        "inheritanceRule"
                );

        if (createdTick < 0) {
            throw new IllegalArgumentException(
                    "createdTick cannot be negative"
            );
        }

        this.createdTick =
                createdTick;

        this.status =
                Objects.requireNonNull(
                        status,
                        "status"
                );

        if (marriedTick != null
                && marriedTick < createdTick) {

            throw new IllegalArgumentException(
                    "marriedTick cannot be earlier than createdTick"
            );
        }

        if (endedTick != null) {

            long earliest =
                    marriedTick == null
                            ? createdTick
                            : marriedTick;

            if (endedTick < earliest) {

                throw new IllegalArgumentException(
                        "endedTick cannot precede the active union"
                );
            }
        }

        if (status == MarriageStatus.MARRIED
                && marriedTick == null) {

            throw new IllegalArgumentException(
                    "MARRIED record requires marriedTick"
            );
        }

        if (status == MarriageStatus.ENDED
                && endedTick == null) {

            throw new IllegalArgumentException(
                    "ENDED record requires endedTick"
            );
        }

        this.marriedTick =
                marriedTick;

        this.endedTick =
                endedTick;
    }

    public MarriageId id() {
        return id;
    }

    public NpcId first() {
        return first;
    }

    public NpcId second() {
        return second;
    }

    public DynastyInheritanceRule inheritanceRule() {
        return inheritanceRule;
    }

    public long createdTick() {
        return createdTick;
    }

    public MarriageStatus status() {
        return status;
    }

    public Long marriedTick() {
        return marriedTick;
    }

    public Long endedTick() {
        return endedTick;
    }

    public boolean isActive() {
        return status != MarriageStatus.ENDED;
    }

    public boolean isMarried() {
        return status == MarriageStatus.MARRIED;
    }

    public boolean involves(
            NpcId npc
    ) {
        return first.equals(
                npc
        )
                || second.equals(
                npc
        );
    }

    public boolean involvesBoth(
            NpcId one,
            NpcId two
    ) {
        return (
                first.equals(
                        one
                )
                        && second.equals(
                        two
                )
        )
                || (
                first.equals(
                        two
                )
                        && second.equals(
                        one
                )
        );
    }

    public NpcId other(
            NpcId npc
    ) {
        if (first.equals(
                npc
        )) {
            return second;
        }

        if (second.equals(
                npc
        )) {
            return first;
        }

        throw new IllegalArgumentException(
                "NPC "
                        + npc
                        + " is not part of marriage "
                        + id
        );
    }

    public void marry(
            long tick
    ) {
        if (status == MarriageStatus.ENDED) {

            throw new IllegalStateException(
                    "Ended marriage cannot become active again"
            );
        }

        if (tick < createdTick) {

            throw new IllegalArgumentException(
                    "Marriage tick cannot precede creation"
            );
        }

        status =
                MarriageStatus.MARRIED;

        if (marriedTick == null) {
            marriedTick =
                    tick;
        }
    }

    public void end(
            long tick
    ) {
        if (status == MarriageStatus.ENDED) {
            return;
        }

        long earliest =
                marriedTick == null
                        ? createdTick
                        : marriedTick;

        if (tick < earliest) {

            throw new IllegalArgumentException(
                    "End tick cannot precede union"
            );
        }

        status =
                MarriageStatus.ENDED;

        endedTick =
                tick;
    }
}
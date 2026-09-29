package dev.dtzudontsay.knownworld.simulation.npc.lifecycle;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LifeHistoryManager {

    private final NpcRegistry registry;

    private final Map<NpcId, NpcLifeRecord> records =
            new LinkedHashMap<>();

    public LifeHistoryManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    /**
     * Creates approximate life records for authored/old-save NPCs.
     *
     * Since old NPC data only knows birthYear, their birthday is
     * treated as day 0 of that historical year.
     */
    public synchronized void ensureAll(
            int daysPerYear
    ) {
        for (
                NpcState npc :
                registry.all()
        ) {

            records.computeIfAbsent(
                    npc.id(),
                    ignored ->
                            new NpcLifeRecord(
                                    npc.id(),
                                    ((long) npc.identity()
                                            .birthYear())
                                            * daysPerYear,
                                    npc.isAlive()
                                            ? null
                                            : ((long) npc.identity()
                                            .birthYear())
                                            * daysPerYear
                            )
            );
        }
    }

    public synchronized NpcLifeRecord registerBirth(
            NpcId npc,
            long birthDay
    ) {
        validateNpc(
                npc
        );

        NpcLifeRecord existing =
                records.get(
                        npc
                );

        if (existing != null) {

            existing.setBirthDay(
                    birthDay
            );

            return existing;
        }

        NpcLifeRecord record =
                new NpcLifeRecord(
                        npc,
                        birthDay,
                        null
                );

        records.put(
                npc,
                record
        );

        return record;
    }

    public synchronized void registerLoaded(
            NpcLifeRecord record
    ) {
        Objects.requireNonNull(
                record,
                "record"
        );

        validateNpc(
                record.owner()
        );

        records.put(
                record.owner(),
                record
        );
    }

    public synchronized Optional<NpcLifeRecord> find(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return Optional.ofNullable(
                records.get(
                        npc
                )
        );
    }

    public synchronized NpcLifeRecord require(
            NpcId npc
    ) {
        return find(
                npc
        )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No life record for NPC "
                                                + npc
                                )
                );
    }

    public synchronized int ageYears(
            NpcId npc,
            long currentDay,
            int daysPerYear
    ) {
        NpcLifeRecord record =
                require(
                        npc
                );

        long ageDays =
                Math.max(
                        0L,
                        currentDay
                                - record.birthDay()
                );

        return (int) (
                ageDays
                        / daysPerYear
        );
    }

    public synchronized long ageDays(
            NpcId npc,
            long currentDay
    ) {
        return Math.max(
                0L,
                currentDay
                        - require(
                        npc
                )
                        .birthDay()
        );
    }

    public synchronized LifeStage lifeStage(
            NpcId npc,
            long currentDay,
            int daysPerYear
    ) {
        int age =
                ageYears(
                        npc,
                        currentDay,
                        daysPerYear
                );

        if (age < 2) {
            return LifeStage.INFANT;
        }

        if (age < 12) {
            return LifeStage.CHILD;
        }

        if (age < 16) {
            return LifeStage.ADOLESCENT;
        }

        if (age < 60) {
            return LifeStage.ADULT;
        }

        return LifeStage.ELDER;
    }

    public synchronized void markDead(
            NpcId npc,
            long day
    ) {
        require(
                npc
        )
                .markDead(
                        day
                );
    }

    public synchronized List<NpcLifeRecord> all() {
        return List.copyOf(
                records.values()
        );
    }

    public synchronized int size() {
        return records.size();
    }

    private void validateNpc(
            NpcId npc
    ) {
        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (!registry.contains(
                npc
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + npc
            );
        }
    }
}
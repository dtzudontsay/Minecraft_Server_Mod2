package dev.dtzudontsay.knownworld.simulation.npc.formation;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UpbringingManager {

    private final NpcRegistry registry;

    private final Map<NpcId, UpbringingRecord> records =
            new LinkedHashMap<>();

    public UpbringingManager(
            NpcRegistry registry
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    /**
     * Exact birth path for newly generated children.
     *
     * Here the current world position genuinely is the birth
     * position because creation happens at the mother's position.
     */
    public synchronized UpbringingRecord createBirthRecord(
            NpcId child,
            String birthLocationId,
            NpcId defaultGuardian
    ) {

        validateNpc(
                child
        );

        validateNpcIfPresent(
                defaultGuardian
        );

        UpbringingRecord existing =
                records.get(
                        child
                );

        if (existing != null) {
            return existing;
        }

        UpbringingRecord record =
                new UpbringingRecord(
                        child,
                        birthLocationId,
                        birthLocationId,
                        defaultGuardian,
                        null,
                        0
                );

        records.put(
                child,
                record
        );

        return record;
    }

    /**
     * Migration path for children that existed before Batch 18F.
     *
     * Birthplace and current upbringing are deliberately separate.
     * We never infer a historical birthplace from a child's current
     * position.
     */
    public synchronized UpbringingRecord ensureMigrated(
            NpcId child,
            String birthLocationId,
            String currentUpbringingLocationId,
            NpcId defaultGuardian,
            int currentAge
    ) {

        validateNpc(
                child
        );

        validateNpcIfPresent(
                defaultGuardian
        );

        UpbringingRecord existing =
                records.get(
                        child
                );

        if (existing != null) {

            if (existing.guardian() == null
                    && defaultGuardian != null) {

                existing.setGuardian(
                        defaultGuardian
                );
            }

            return existing;
        }

        UpbringingRecord record =
                new UpbringingRecord(
                        child,
                        birthLocationId,
                        currentUpbringingLocationId,
                        defaultGuardian,
                        null,
                        Math.max(
                                0,
                                currentAge
                        )
                );

        records.put(
                child,
                record
        );

        return record;
    }

    public synchronized Optional<UpbringingRecord> find(
            NpcId child
    ) {

        validateNpc(
                child
        );

        return Optional.ofNullable(
                records.get(
                        child
                )
        );
    }

    public synchronized boolean contains(
            NpcId child
    ) {

        validateNpc(
                child
        );

        return records.containsKey(
                child
        );
    }

    public synchronized UpbringingRecord require(
            NpcId child
    ) {

        return find(
                child
        )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No upbringing record for NPC "
                                                + child
                                )
                );
    }

    public synchronized void setGuardian(
            NpcId child,
            NpcId guardian
    ) {

        validateNpc(
                child
        );

        validateNpcIfPresent(
                guardian
        );

        require(
                child
        )
                .setGuardian(
                        guardian
                );
    }

    public synchronized void setFosterParent(
            NpcId child,
            NpcId fosterParent
    ) {

        validateNpc(
                child
        );

        validateNpcIfPresent(
                fosterParent
        );

        require(
                child
        )
                .setFosterParent(
                        fosterParent
                );
    }

    public synchronized void updateLocation(
            NpcId child,
            String locationId
    ) {

        require(
                child
        )
                .setUpbringingLocationId(
                        locationId
                );
    }

    public synchronized void registerLoaded(
            UpbringingRecord record
    ) {

        Objects.requireNonNull(
                record,
                "record"
        );

        validateNpc(
                record.child()
        );

        validateNpcIfPresent(
                record.guardian()
        );

        validateNpcIfPresent(
                record.fosterParent()
        );

        records.put(
                record.child(),
                record
        );
    }

    public synchronized List<UpbringingRecord> all() {

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
                    "Unknown NPC "
                            + npc
            );
        }
    }

    private void validateNpcIfPresent(
            NpcId npc
    ) {

        if (npc != null) {

            validateNpc(
                    npc
            );
        }
    }
}
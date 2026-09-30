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

    public synchronized UpbringingRecord ensureExisting(
            NpcId child,
            String currentLocationId,
            int currentAge
    ) {

        validateNpc(
                child
        );

        return records.computeIfAbsent(
                child,
                ignored ->
                        new UpbringingRecord(
                                child,
                                currentLocationId,
                                currentLocationId,
                                null,
                                null,
                                Math.max(
                                        0,
                                        currentAge
                                )
                        )
        );
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
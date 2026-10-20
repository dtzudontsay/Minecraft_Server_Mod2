
package dev.dtzudontsay.knownworld.simulation.world.settlement;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SettlementManager {

    private final Map<SettlementId, Settlement> settlements =
            new LinkedHashMap<>();

    private long nextId = 1L;

    public synchronized Settlement create(
            String name,
            SettlementType type,
            SimulationPosition center
    ) {

        Settlement settlement = new Settlement(
                allocateId(),
                name,
                type,
                center
        );

        settlements.put(
                settlement.id(),
                settlement
        );

        return settlement;
    }

    public synchronized void registerLoaded(
            Settlement settlement
    ) {

        Objects.requireNonNull(
                settlement,
                "settlement"
        );

        if (settlements.containsKey(settlement.id())) {

            throw new IllegalStateException(
                    "Duplicate settlement ID: " + settlement.id()
            );
        }

        settlements.put(
                settlement.id(),
                settlement
        );

        if (settlement.id().value() == Long.MAX_VALUE) {

            nextId = Long.MAX_VALUE;

        } else {

            nextId = Math.max(
                    nextId,
                    settlement.id().value() + 1
            );
        }
    }

    public synchronized Optional<Settlement> find(
            SettlementId id
    ) {

        return Optional.ofNullable(
                settlements.get(id)
        );
    }

    public synchronized Collection<Settlement> all() {

        return List.copyOf(
                settlements.values()
        );
    }

    public synchronized int size() {
        return settlements.size();
    }

    /**
     * B2.1: Guarded migration of one known historical
     * placeholder coordinate.
     *
     * IMPORTANT:
     * - Never creates a new settlement.
     * - Never changes its persistent SettlementId.
     * - Never changes its type or name.
     * - Never moves an NPC or physical Minecraft entity.
     * - Never changes household/residence affiliations.
     * - Refuses to change a settlement already moved
     *   elsewhere by another system or by authored data.
     *
     * Matching is on dimension and horizontal coordinates,
     * because old scenario data may have an inaccurate
     * surface Y value.
     *
     * This is NOT a generic unrestricted teleport API.
     */
    public synchronized boolean recenterIfLegacy(
            SettlementId id,
            SimulationPosition legacyCenter,
            SimulationPosition replacementCenter
    ) {

        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(
                legacyCenter,
                "legacyCenter"
        );
        Objects.requireNonNull(
                replacementCenter,
                "replacementCenter"
        );

        if (!legacyCenter.dimension().equals(
                replacementCenter.dimension()
        )) {

            throw new IllegalArgumentException(
                    "Cannot migrate settlement between dimensions"
            );
        }

        Settlement existing = settlements.get(id);

        if (existing == null) {

            throw new IllegalArgumentException(
                    "Unknown settlement ID: " + id
            );
        }

        SimulationPosition current = existing.center();

        if (!sameHorizontalLocation(
                current,
                legacyCenter
        )) {

            return false;
        }

        if (sameHorizontalLocation(
                current,
                replacementCenter
        )) {

            return false;
        }

        Settlement corrected = new Settlement(
                existing.id(),
                existing.name(),
                existing.type(),
                replacementCenter
        );

        settlements.put(id, corrected);

        return true;
    }

    private static boolean sameHorizontalLocation(
            SimulationPosition a,
            SimulationPosition b
    ) {

        return a.dimension().equals(b.dimension())
                && Math.abs(a.x() - b.x()) <= 0.01
                && Math.abs(a.z() - b.z()) <= 0.01;
    }

    private SettlementId allocateId() {

        if (nextId <= 0 || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Settlement ID space exhausted"
            );
        }

        SettlementId id = new SettlementId(nextId);

        nextId++;

        return id;
    }
}

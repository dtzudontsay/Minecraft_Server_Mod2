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

    private long nextId =
            1L;

    public synchronized Settlement create(
            String name,
            SettlementType type,
            SimulationPosition center
    ) {
        Settlement settlement =
                new Settlement(
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

        if (settlements.containsKey(
                settlement.id()
        )) {
            throw new IllegalStateException(
                    "Duplicate settlement ID: "
                            + settlement.id()
            );
        }

        settlements.put(
                settlement.id(),
                settlement
        );

        if (settlement.id().value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            settlement.id()
                                    .value()
                                    + 1
                    );
        }
    }

    public synchronized Optional<Settlement> find(
            SettlementId id
    ) {
        return Optional.ofNullable(
                settlements.get(
                        id
                )
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

    private SettlementId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Settlement ID space exhausted"
            );
        }

        SettlementId id =
                new SettlementId(
                        nextId
                );

        nextId++;

        return id;
    }
}
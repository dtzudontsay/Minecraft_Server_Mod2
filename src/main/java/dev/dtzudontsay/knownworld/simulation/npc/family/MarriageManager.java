package dev.dtzudontsay.knownworld.simulation.npc.family;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent betrothal and marriage registry.
 *
 * Current rule:
 *
 * one NPC may have at most one active betrothal/marriage.
 *
 * Historical ended marriages are retained permanently.
 */
public final class MarriageManager {

    private final NpcRegistry registry;

    private final Map<MarriageId, MarriageRecord> marriages =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public MarriageManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized MarriageRecord betroth(
            NpcId first,
            NpcId second,
            DynastyInheritanceRule rule,
            long tick
    ) {
        validatePair(
                first,
                second
        );

        Optional<MarriageRecord> existingPair =
                activeUnionBetween(
                        first,
                        second
                );

        if (existingPair.isPresent()) {
            return existingPair.get();
        }

        ensureAvailable(
                first
        );

        ensureAvailable(
                second
        );

        MarriageRecord record =
                new MarriageRecord(
                        allocateId(),
                        first,
                        second,
                        rule,
                        tick,
                        MarriageStatus.BETROTHED,
                        null,
                        null
                );

        marriages.put(
                record.id(),
                record
        );

        return record;
    }

    public synchronized MarriageRecord marry(
            NpcId first,
            NpcId second,
            DynastyInheritanceRule rule,
            long tick
    ) {
        validatePair(
                first,
                second
        );

        Optional<MarriageRecord> existing =
                activeUnionBetween(
                        first,
                        second
                );

        if (existing.isPresent()) {

            MarriageRecord record =
                    existing.get();

            if (record.inheritanceRule()
                    != rule) {

                throw new IllegalArgumentException(
                        "Existing union uses inheritance rule "
                                + record.inheritanceRule()
                );
            }

            record.marry(
                    tick
            );

            return record;
        }

        ensureAvailable(
                first
        );

        ensureAvailable(
                second
        );

        MarriageRecord record =
                new MarriageRecord(
                        allocateId(),
                        first,
                        second,
                        rule,
                        tick,
                        MarriageStatus.MARRIED,
                        tick,
                        null
                );

        marriages.put(
                record.id(),
                record
        );

        return record;
    }

    public synchronized boolean endCurrentUnion(
            NpcId npc,
            long tick
    ) {
        Optional<MarriageRecord> current =
                currentUnionOf(
                        npc
                );

        if (current.isEmpty()) {
            return false;
        }

        current.get()
                .end(
                        tick
                );

        return true;
    }

    public synchronized boolean endMarriage(
            MarriageId marriage,
            long tick
    ) {
        MarriageRecord record =
                marriages.get(
                        marriage
                );

        if (record == null) {

            throw new IllegalArgumentException(
                    "Unknown marriage ID: "
                            + marriage
            );
        }

        if (!record.isActive()) {
            return false;
        }

        record.end(
                tick
        );

        return true;
    }

    public synchronized Optional<MarriageRecord> find(
            MarriageId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        return Optional.ofNullable(
                marriages.get(
                        id
                )
        );
    }

    public synchronized Optional<MarriageRecord> currentUnionOf(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return marriages.values()
                .stream()
                .filter(
                        MarriageRecord::isActive
                )
                .filter(
                        marriage ->
                                marriage.involves(
                                        npc
                                )
                )
                .findFirst();
    }

    public synchronized Optional<MarriageRecord> currentMarriageOf(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return marriages.values()
                .stream()
                .filter(
                        MarriageRecord::isMarried
                )
                .filter(
                        marriage ->
                                marriage.involves(
                                        npc
                                )
                )
                .findFirst();
    }

    public synchronized Optional<NpcId> currentSpouseOf(
            NpcId npc
    ) {
        return currentMarriageOf(
                npc
        )
                .map(
                        marriage ->
                                marriage.other(
                                        npc
                                )
                );
    }

    public synchronized Optional<MarriageRecord> activeUnionBetween(
            NpcId first,
            NpcId second
    ) {
        validatePair(
                first,
                second
        );

        return marriages.values()
                .stream()
                .filter(
                        MarriageRecord::isActive
                )
                .filter(
                        marriage ->
                                marriage.involvesBoth(
                                        first,
                                        second
                                )
                )
                .findFirst();
    }

    public synchronized List<MarriageRecord> unionsOf(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        List<MarriageRecord> result =
                new ArrayList<>();

        for (
                MarriageRecord marriage :
                marriages.values()
        ) {

            if (marriage.involves(
                    npc
            )) {

                result.add(
                        marriage
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public synchronized List<MarriageRecord> all() {
        return List.copyOf(
                marriages.values()
        );
    }

    public synchronized int size() {
        return marriages.size();
    }

    public synchronized void registerLoaded(
            MarriageRecord marriage
    ) {
        Objects.requireNonNull(
                marriage,
                "marriage"
        );

        validatePair(
                marriage.first(),
                marriage.second()
        );

        if (marriages.containsKey(
                marriage.id()
        )) {

            throw new IllegalStateException(
                    "Duplicate marriage ID: "
                            + marriage.id()
            );
        }

        marriages.put(
                marriage.id(),
                marriage
        );

        if (marriage.id()
                .value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            marriage.id()
                                    .value()
                                    + 1
                    );
        }
    }

    private void ensureAvailable(
            NpcId npc
    ) {
        Optional<MarriageRecord> existing =
                currentUnionOf(
                        npc
                );

        if (existing.isPresent()) {

            throw new IllegalStateException(
                    "NPC #"
                            + npc
                            + " already has an active "
                            + existing.get()
                            .status()
                            + " union"
            );
        }
    }

    private void validatePair(
            NpcId first,
            NpcId second
    ) {
        validateNpc(
                first
        );

        validateNpc(
                second
        );

        if (first.equals(
                second
        )) {

            throw new IllegalArgumentException(
                    "NPC cannot marry themselves"
            );
        }
    }

    private void validateNpc(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        NpcState npc =
                registry.find(
                                id
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown NPC ID: "
                                                        + id
                                        )
                        );

        if (!npc.isAlive()) {

            throw new IllegalArgumentException(
                    "NPC #"
                            + id
                            + " is dead"
            );
        }
    }

    private MarriageId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Marriage ID space exhausted"
            );
        }

        MarriageId id =
                new MarriageId(
                        nextId
                );

        nextId++;

        return id;
    }
}
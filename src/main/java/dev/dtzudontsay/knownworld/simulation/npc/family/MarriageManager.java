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
        validateLivingPair(
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
        validateLivingPair(
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
        validateExistingNpc(
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
        validateExistingNpc(
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
        validateExistingPair(
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
        validateExistingNpc(
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

    /**
     * Registers a marriage record restored from persistence.
     *
     * Historical records are allowed to reference NPCs who are now dead.
     *
     * This is essential because a marriage may have existed during an NPC's
     * lifetime and then ended through death or another cause. Persistence is
     * restoring history, not creating a new marriage in the present.
     */
    public synchronized void registerLoaded(
            MarriageRecord marriage
    ) {
        Objects.requireNonNull(
                marriage,
                "marriage"
        );

        validateExistingPair(
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

        /*
         * Active unions involving dead NPCs indicate inconsistent persisted
         * state.
         *
         * Ended historical unions involving dead NPCs are valid and must be
         * retained.
         */
        if (marriage.isActive()) {

            requireLivingNpc(
                    marriage.first()
            );

            requireLivingNpc(
                    marriage.second()
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

    /**
     * Validation used when creating a new current union.
     */
    private void validateLivingPair(
            NpcId first,
            NpcId second
    ) {
        requireLivingNpc(
                first
        );

        requireLivingNpc(
                second
        );

        validateDifferentPeople(
                first,
                second
        );
    }

    /**
     * Validation used when reading historical/current relationships.
     *
     * The NPCs must exist, but they are not required to still be alive.
     */
    private void validateExistingPair(
            NpcId first,
            NpcId second
    ) {
        validateExistingNpc(
                first
        );

        validateExistingNpc(
                second
        );

        validateDifferentPeople(
                first,
                second
        );
    }

    private void validateDifferentPeople(
            NpcId first,
            NpcId second
    ) {
        if (first.equals(
                second
        )) {

            throw new IllegalArgumentException(
                    "NPC cannot marry themselves"
            );
        }
    }

    /**
     * Requires that an NPC exists in the registry.
     *
     * Dead NPCs are valid here because historical queries and persisted
     * records must remain accessible after death.
     */
    private NpcState validateExistingNpc(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        return registry.find(
                        id
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC ID: "
                                                + id
                                )
                );
    }

    /**
     * Requires that an NPC both exists and is currently alive.
     *
     * Used only for operations that create or require a living present-day
     * participant.
     */
    private NpcState requireLivingNpc(
            NpcId id
    ) {
        NpcState npc =
                validateExistingNpc(
                        id
                );

        if (!npc.isAlive()) {

            throw new IllegalArgumentException(
                    "NPC #"
                            + id
                            + " is dead"
            );
        }

        return npc;
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
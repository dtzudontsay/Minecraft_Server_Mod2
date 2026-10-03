package dev.dtzudontsay.knownworld.simulation.npc.memory;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Canonical storage for persistent NPC memories.
 *
 * SIM 08:
 *
 * Memory storage is now bounded.
 *
 * Without consolidation, even a small population can create enormous numbers
 * of individual memories over decades or centuries.
 *
 * The manager therefore:
 *
 * - preserves recent individual memories
 * - consolidates repeated perspective-aware social memories
 * - prefers important and recent memories
 * - guarantees a hard upper bound per NPC
 *
 * This is deliberately a detailed-memory store, not an infinite historical
 * event archive.
 *
 * Broader historical summaries, chronicles, dynastic history and world-event
 * archives are separate concepts and can persist information after an
 * individual NPC no longer stores every event as a detailed personal memory.
 */
public final class NpcMemoryManager {

    /**
     * We begin consolidation before reaching the hard limit so maintenance
     * normally has room to preserve meaningful detail.
     */
    public static final int SOFT_MEMORIES_PER_OWNER =
            48;

    /**
     * Absolute maximum number of detailed memories retained by one NPC.
     *
     * This is intentionally bounded for large populations.
     *
     * Later simulation-level importance tiers may allow major characters to
     * maintain richer active memory while keeping ordinary population cheaper.
     */
    public static final int MAX_MEMORIES_PER_OWNER =
            64;

    /**
     * For one repeated semantic relationship/action pattern, preserve the most
     * recent raw examples individually in addition to one older consolidated
     * memory.
     */
    private static final int RECENT_MEMORIES_PER_GROUP =
            2;

    private final NpcRegistry registry;

    private final Map<NpcMemoryId, NpcMemory> memories =
            new LinkedHashMap<>();

    private final Map<NpcId, List<NpcMemoryId>> memoriesByOwner =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public NpcMemoryManager(
            NpcRegistry registry
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized NpcMemory remember(
            NpcId owner,
            NpcMemoryType type,
            String summary,
            double importance,
            NpcId relatedNpc,
            String factKey,
            long createdTick
    ) {

        validateNpc(
                owner
        );

        if (relatedNpc != null) {

            validateNpc(
                    relatedNpc
            );
        }

        NpcMemoryId id =
                allocateId();

        NpcMemory memory =
                new NpcMemory(
                        id,
                        owner,
                        type,
                        summary,
                        importance,
                        relatedNpc,
                        factKey,
                        createdTick
                );

        put(
                memory
        );

        /*
         * Maintenance is owner-local.
         *
         * We never scan the entire world's memory store just because one NPC
         * formed one new memory.
         */
        maintainOwner(
                owner
        );

        return memory;
    }

    public synchronized void registerLoaded(
            NpcMemory memory
    ) {

        Objects.requireNonNull(
                memory,
                "memory"
        );

        validateNpc(
                memory.owner()
        );

        if (memory.relatedNpc() != null) {

            validateNpc(
                    memory.relatedNpc()
            );
        }

        if (memories.containsKey(
                memory.id()
        )) {

            throw new IllegalStateException(
                    "Duplicate memory ID: "
                            + memory.id()
            );
        }

        /*
         * Do not consolidate while a save is still being loaded.
         *
         * Loading should reconstruct persisted state exactly. The owner will
         * naturally be maintained the next time a new memory is formed.
         */
        put(
                memory
        );

        if (memory.id()
                .value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            memory.id()
                                    .value()
                                    + 1
                    );
        }
    }

    public synchronized List<NpcMemory> memoriesOf(
            NpcId owner
    ) {

        validateNpc(
                owner
        );

        List<NpcMemoryId> ids =
                memoriesByOwner.get(
                        owner
                );

        if (ids == null
                ||
                ids.isEmpty()) {

            return List.of();
        }

        List<NpcMemory> result =
                new ArrayList<>(
                        ids.size()
                );

        for (
                NpcMemoryId id :
                ids
        ) {

            NpcMemory memory =
                    memories.get(
                            id
                    );

            if (memory != null) {

                result.add(
                        memory
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public synchronized List<NpcMemory> importantMemoriesOf(
            NpcId owner,
            double minimumImportance
    ) {

        if (!Double.isFinite(
                minimumImportance
        )
                ||
                minimumImportance < 0.0
                ||
                minimumImportance > 1.0) {

            throw new IllegalArgumentException(
                    "minimumImportance must be between 0.0 and 1.0"
            );
        }

        return memoriesOf(
                owner
        )
                .stream()
                .filter(
                        memory ->
                                memory.importance()
                                        >= minimumImportance
                )
                .toList();
    }

    public synchronized Collection<NpcMemory> all() {

        return List.copyOf(
                memories.values()
        );
    }

    public synchronized int size() {

        return memories.size();
    }

    public synchronized int sizeOf(
            NpcId owner
    ) {

        validateNpc(
                owner
        );

        List<NpcMemoryId> ids =
                memoriesByOwner.get(
                        owner
                );

        return ids == null
                ? 0
                : ids.size();
    }

    /**
     * Maximum detailed-memory count currently held by any NPC.
     *
     * Mostly useful for diagnostics and simulation audits.
     */
    public synchronized int maximumOwnerMemoryCount() {

        int maximum =
                0;

        for (
                List<NpcMemoryId> ids :
                memoriesByOwner.values()
        ) {

            maximum =
                    Math.max(
                            maximum,
                            ids.size()
                    );
        }

        return maximum;
    }

    /**
     * Allows maintenance of old loaded saves without waiting for every NPC to
     * generate a new memory.
     *
     * Safe to call manually from future maintenance/debug systems.
     */
    public synchronized void maintainAll() {

        List<NpcId> owners =
                new ArrayList<>(
                        memoriesByOwner.keySet()
                );

        for (
                NpcId owner :
                owners
        ) {

            maintainOwner(
                    owner
            );
        }
    }

    private void maintainOwner(
            NpcId owner
    ) {

        List<NpcMemoryId> ids =
                memoriesByOwner.get(
                        owner
                );

        if (ids == null
                ||
                ids.size()
                        <= SOFT_MEMORIES_PER_OWNER) {

            return;
        }

        consolidateRepeatedSemanticMemories(
                owner
        );

        ids =
                memoriesByOwner.get(
                        owner
                );

        if (ids != null
                &&
                ids.size()
                        > MAX_MEMORIES_PER_OWNER) {

            pruneOwnerToHardLimit(
                    owner
            );
        }
    }

    /**
     * Repeated semantic social memories are the major source of memory growth.
     *
     * Example:
     *
     *   Robert insulted Eddard.
     *   Robert insulted Eddard.
     *   Robert insulted Eddard.
     *   Robert insulted Eddard.
     *
     * Instead of preserving every ancient instance forever, older repetitions
     * become one consolidated memory while the latest examples remain
     * individually available.
     *
     * Only perspective-aware SIM 08 keys are consolidated.
     *
     * Legacy "social.action.*" keys are deliberately NOT consolidated because
     * old saves did not encode whether the owner performed or received the
     * action.
     */
    private void consolidateRepeatedSemanticMemories(
            NpcId owner
    ) {

        List<NpcMemory> ownerMemories =
                new ArrayList<>(
                        memoriesOf(
                                owner
                        )
                );

        Map<ConsolidationKey, List<NpcMemory>> groups =
                new LinkedHashMap<>();

        for (
                NpcMemory memory :
                ownerMemories
        ) {

            if (memory.type()
                    != NpcMemoryType.SOCIAL_INTERACTION) {

                continue;
            }

            if (memory.relatedNpc()
                    == null) {

                continue;
            }

            if (!isPerspectiveAwareSocialKey(
                    memory.factKey()
            )) {

                continue;
            }

            ConsolidationKey key =
                    new ConsolidationKey(
                            memory.relatedNpc(),
                            memory.factKey()
                    );

            groups.computeIfAbsent(
                            key,
                            ignored ->
                                    new ArrayList<>()
                    )
                    .add(
                            memory
                    );
        }

        for (
                Map.Entry<ConsolidationKey, List<NpcMemory>> entry :
                groups.entrySet()
        ) {

            List<NpcMemory> group =
                    entry.getValue();

            group.sort(
                    Comparator
                            .comparingLong(
                                    NpcMemory::createdTick
                            )
                            .thenComparing(
                                    NpcMemory::id
                            )
            );

            /*
             * One consolidated record + two recent raw records = 3.
             *
             * Therefore consolidation actually reduces storage only once the
             * group has at least four memories.
             */
            if (group.size()
                    <= RECENT_MEMORIES_PER_GROUP
                    + 1) {

                continue;
            }

            int compressCount =
                    group.size()
                            - RECENT_MEMORIES_PER_GROUP;

            List<NpcMemory> compressed =
                    group.subList(
                            0,
                            compressCount
                    );

            double importance =
                    aggregateImportance(
                            compressed
                    );

            long createdTick =
                    compressed.get(
                                    compressed.size()
                                            - 1
                            )
                            .createdTick();

            ConsolidationKey key =
                    entry.getKey();

            for (
                    NpcMemory memory :
                    List.copyOf(
                            compressed
                    )
            ) {

                remove(
                        memory.id()
                );
            }

            NpcMemory consolidated =
                    new NpcMemory(
                            allocateId(),
                            owner,
                            NpcMemoryType.SOCIAL_INTERACTION,
                            consolidatedSummary(
                                    key,
                                    compressed.size()
                            ),
                            importance,
                            key.relatedNpc(),
                            key.factKey(),
                            createdTick
                    );

            put(
                    consolidated
            );
        }
    }

    /**
     * Repetition strengthens a consolidated memory but with diminishing
     * returns.
     *
     * Several weak incidents can become meaningful, but hundreds of trivial
     * repetitions cannot create an importance value above 1.
     */
    private static double aggregateImportance(
            List<NpcMemory> memories
    ) {

        double remaining =
                1.0;

        for (
                NpcMemory memory :
                memories
        ) {

            double contribution =
                    memory.importance()
                            * 0.65;

            remaining *=
                    1.0
                            - contribution;
        }

        return clampUnit(
                1.0
                        - remaining
        );
    }

    /**
     * If consolidation alone cannot keep the owner's detailed memories within
     * the hard limit, retain the most useful mixture of:
     *
     * - important memories
     * - recent memories
     * - major death/combat/political memories
     * - betrayal memories
     *
     * This is a last-resort detail budget, not ordinary forgetting.
     */
    private void pruneOwnerToHardLimit(
            NpcId owner
    ) {

        List<NpcMemory> ownerMemories =
                new ArrayList<>(
                        memoriesOf(
                                owner
                        )
                );

        if (ownerMemories.size()
                <= MAX_MEMORIES_PER_OWNER) {

            return;
        }

        long oldest =
                ownerMemories.stream()
                        .mapToLong(
                                NpcMemory::createdTick
                        )
                        .min()
                        .orElse(
                                0L
                        );

        long newest =
                ownerMemories.stream()
                        .mapToLong(
                                NpcMemory::createdTick
                        )
                        .max()
                        .orElse(
                                oldest
                        );

        /*
         * Explicit comparator instead of chained generic Comparator factories.
         *
         * This avoids Java inferring Comparator<Object> for the lambda and
         * keeps the ordering completely deterministic.
         *
         * Desired ordering:
         *
         * 1. highest retention score first
         * 2. newest memory first
         * 3. highest/newest memory ID first
         */
        ownerMemories.sort(
                (
                        left,
                        right
                ) -> {

                    int scoreComparison =
                            Double.compare(
                                    retentionScore(
                                            right,
                                            oldest,
                                            newest
                                    ),
                                    retentionScore(
                                            left,
                                            oldest,
                                            newest
                                    )
                            );

                    if (scoreComparison != 0) {

                        return scoreComparison;
                    }

                    int tickComparison =
                            Long.compare(
                                    right.createdTick(),
                                    left.createdTick()
                            );

                    if (tickComparison != 0) {

                        return tickComparison;
                    }

                    return right.id()
                            .compareTo(
                                    left.id()
                            );
                }
        );

        Set<NpcMemoryId> keep =
                new HashSet<>();

        for (
                int index = 0;
                index < MAX_MEMORIES_PER_OWNER;
                index++
        ) {

            keep.add(
                    ownerMemories.get(
                                    index
                            )
                            .id()
            );
        }

        for (
                NpcMemory memory :
                ownerMemories
        ) {

            if (!keep.contains(
                    memory.id()
            )) {

                remove(
                        memory.id()
                );
            }
        }
    }

    private static double retentionScore(
            NpcMemory memory,
            long oldest,
            long newest
    ) {

        double recency;

        if (newest <= oldest) {

            recency =
                    1.0;

        } else {

            recency =
                    (
                            double
                            )
                            (
                                    memory.createdTick()
                                            - oldest
                            )
                            /
                            (
                                    double
                                    )
                                    (
                                            newest
                                                    - oldest
                                    );
        }

        double typeBonus =
                switch (
                        memory.type()
                        ) {

                    case DEATH ->
                            0.25;

                    case COMBAT ->
                            0.15;

                    case POLITICAL ->
                            0.10;

                    case PERSONAL_EXPERIENCE ->
                            0.05;

                    default ->
                            0.0;
                };

        double semanticBonus =
                isBetrayalKey(
                        memory.factKey()
                )
                        ? 0.10
                        : 0.0;

        return memory.importance()
                * 0.70
                +
                recency
                        * 0.20
                +
                typeBonus
                +
                semanticBonus;
    }

    private static boolean isPerspectiveAwareSocialKey(
            String factKey
    ) {

        if (factKey == null) {

            return false;
        }

        return factKey.startsWith(
                "social.received."
        )
                ||
                factKey.startsWith(
                        "social.performed."
                );
    }

    private static boolean isBetrayalKey(
            String factKey
    ) {

        return "social.received.betray".equals(
                factKey
        )
                ||
                "social.performed.betray".equals(
                        factKey
                )
                ||
                "social.action.betray".equals(
                        factKey
                );
    }

    private static String consolidatedSummary(
            ConsolidationKey key,
            int count
    ) {

        String action =
                actionLabel(
                        key.factKey()
                );

        if (key.factKey()
                .startsWith(
                        "social.received."
                )) {

            return "Consolidated "
                    + count
                    + " memories of being "
                    + action
                    + " by NPC "
                    + key.relatedNpc()
                    + ".";
        }

        return "Consolidated "
                + count
                + " memories of having "
                + action
                + " NPC "
                + key.relatedNpc()
                + ".";
    }

    private static String actionLabel(
            String factKey
    ) {

        if (factKey.endsWith(
                ".help"
        )) {

            return "helped";
        }

        if (factKey.endsWith(
                ".praise"
        )) {

            return "praised";
        }

        if (factKey.endsWith(
                ".insult"
        )) {

            return "insulted";
        }

        if (factKey.endsWith(
                ".threaten"
        )) {

            return "threatened";
        }

        if (factKey.endsWith(
                ".betray"
        )) {

            return "betrayed";
        }

        return "affected";
    }

    private void put(
            NpcMemory memory
    ) {

        memories.put(
                memory.id(),
                memory
        );

        memoriesByOwner
                .computeIfAbsent(
                        memory.owner(),
                        ignored ->
                                new ArrayList<>()
                )
                .add(
                        memory.id()
                );
    }

    private void remove(
            NpcMemoryId id
    ) {

        NpcMemory removed =
                memories.remove(
                        id
                );

        if (removed == null) {

            return;
        }

        List<NpcMemoryId> ownerIds =
                memoriesByOwner.get(
                        removed.owner()
                );

        if (ownerIds == null) {

            return;
        }

        ownerIds.remove(
                id
        );

        if (ownerIds.isEmpty()) {

            memoriesByOwner.remove(
                    removed.owner()
            );
        }
    }

    private NpcMemoryId allocateId() {

        if (nextId <= 0
                ||
                nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Memory ID space exhausted"
            );
        }

        NpcMemoryId id =
                new NpcMemoryId(
                        nextId
                );

        nextId++;

        return id;
    }

    private void validateNpc(
            NpcId id
    ) {

        Objects.requireNonNull(
                id,
                "id"
        );

        if (!registry.contains(
                id
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }
    }

    private static double clampUnit(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private record ConsolidationKey(
            NpcId relatedNpc,
            String factKey
    ) {
    }
}
package dev.dtzudontsay.knownworld.simulation.npc.knowledge;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class NpcKnowledgeManager {

    private final NpcRegistry registry;

    private final Map<BeliefKey, NpcBelief> beliefs =
            new LinkedHashMap<>();

    public NpcKnowledgeManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized void believe(
            NpcId owner,
            String factKey,
            String value,
            double confidence,
            NpcId sourceNpc,
            long learnedTick
    ) {
        validateNpc(
                owner
        );

        if (sourceNpc != null) {
            validateNpc(
                    sourceNpc
            );
        }

        NpcBelief belief =
                new NpcBelief(
                        owner,
                        factKey,
                        value,
                        confidence,
                        sourceNpc,
                        learnedTick
                );

        beliefs.put(
                new BeliefKey(
                        owner,
                        belief.factKey()
                ),
                belief
        );
    }

    public synchronized void registerLoaded(
            NpcBelief belief
    ) {
        Objects.requireNonNull(
                belief,
                "belief"
        );

        validateNpc(
                belief.owner()
        );

        if (belief.sourceNpc() != null) {
            validateNpc(
                    belief.sourceNpc()
            );
        }

        beliefs.put(
                new BeliefKey(
                        belief.owner(),
                        belief.factKey()
                ),
                belief
        );
    }

    public synchronized Optional<NpcBelief> find(
            NpcId owner,
            String factKey
    ) {
        return Optional.ofNullable(
                beliefs.get(
                        new BeliefKey(
                                owner,
                                factKey
                        )
                )
        );
    }

    public synchronized List<NpcBelief> beliefsOf(
            NpcId owner
    ) {
        return beliefs.values()
                .stream()
                .filter(
                        belief ->
                                belief.owner()
                                        .equals(
                                                owner
                                        )
                )
                .toList();
    }

    public synchronized Collection<NpcBelief> all() {
        return List.copyOf(
                beliefs.values()
        );
    }

    public synchronized int size() {
        return beliefs.size();
    }

    private void validateNpc(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        if (!registry.contains(id)) {
            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }
    }

    private record BeliefKey(
            NpcId owner,
            String factKey
    ) {
    }
}
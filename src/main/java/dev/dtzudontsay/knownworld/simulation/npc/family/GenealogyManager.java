package dev.dtzudontsay.knownworld.simulation.npc.family;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent biological genealogy graph.
 *
 * Dead NPCs remain in the NPC registry, so ancestry can continue to
 * be resolved many generations later.
 */
public final class GenealogyManager {

    private final NpcRegistry registry;

    private final Map<NpcId, Parentage> parentageByChild =
            new LinkedHashMap<>();

    public GenealogyManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized Parentage registerBirth(
            NpcId child,
            NpcId mother,
            NpcId father,
            long birthTick
    ) {
        validateNpc(
                child
        );

        validateNpcIfPresent(
                mother
        );

        validateNpcIfPresent(
                father
        );

        if (parentageByChild.containsKey(
                child
        )) {

            throw new IllegalStateException(
                    "Parentage already exists for NPC "
                            + child
            );
        }

        Parentage parentage =
                new Parentage(
                        child,
                        mother,
                        father,
                        birthTick
                );

        parentageByChild.put(
                child,
                parentage
        );

        return parentage;
    }

    public synchronized void registerLoaded(
            Parentage parentage
    ) {
        Objects.requireNonNull(
                parentage,
                "parentage"
        );

        validateNpc(
                parentage.child()
        );

        validateNpcIfPresent(
                parentage.mother()
        );

        validateNpcIfPresent(
                parentage.father()
        );

        if (parentageByChild.containsKey(
                parentage.child()
        )) {

            throw new IllegalStateException(
                    "Duplicate parentage for NPC "
                            + parentage.child()
            );
        }

        parentageByChild.put(
                parentage.child(),
                parentage
        );
    }

    public synchronized Optional<Parentage> parentsOf(
            NpcId child
    ) {
        validateNpc(
                child
        );

        return Optional.ofNullable(
                parentageByChild.get(
                        child
                )
        );
    }

    public synchronized List<NpcId> childrenOf(
            NpcId parent
    ) {
        validateNpc(
                parent
        );

        List<NpcId> children =
                new ArrayList<>();

        for (
                Parentage parentage :
                parentageByChild.values()
        ) {

            if (parent.equals(
                    parentage.mother()
            )
                    || parent.equals(
                    parentage.father()
            )) {

                children.add(
                        parentage.child()
                );
            }
        }

        return List.copyOf(
                children
        );
    }

    public synchronized boolean areSiblings(
            NpcId first,
            NpcId second
    ) {
        if (first.equals(
                second
        )) {
            return false;
        }

        Optional<Parentage> a =
                parentsOf(
                        first
                );

        Optional<Parentage> b =
                parentsOf(
                        second
                );

        if (a.isEmpty()
                || b.isEmpty()) {

            return false;
        }

        Parentage pa =
                a.get();

        Parentage pb =
                b.get();

        return sameKnownNpc(
                pa.mother(),
                pb.mother()
        )
                || sameKnownNpc(
                pa.father(),
                pb.father()
        );
    }

    public synchronized List<Parentage> all() {
        return List.copyOf(
                parentageByChild.values()
        );
    }

    public synchronized int size() {
        return parentageByChild.size();
    }

    private static boolean sameKnownNpc(
            NpcId first,
            NpcId second
    ) {
        return first != null
                && first.equals(
                second
        );
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

    private void validateNpcIfPresent(
            NpcId id
    ) {
        if (id != null) {
            validateNpc(
                    id
            );
        }
    }
}
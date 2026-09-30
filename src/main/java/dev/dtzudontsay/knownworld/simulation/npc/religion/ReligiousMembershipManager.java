package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReligiousMembershipManager {

    private final NpcRegistry registry;

    private final Map<NpcId, ReligiousMembership> memberships =
            new LinkedHashMap<>();

    public ReligiousMembershipManager(
            NpcRegistry registry
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized Optional<ReligiousMembership> find(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        return Optional.ofNullable(
                memberships.get(
                        npc
                )
        );
    }

    public synchronized ReligiousMembership join(
            NpcId npc,
            String orderId,
            String roleId,
            double commitment
    ) {

        validateNpc(
                npc
        );

        ReligiousOrderDefinition definition =
                ReligiousInstitutionCatalog.get()
                        .find(
                                orderId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown religious order "
                                                        + orderId
                                        )
                        );

        ReligiousMembership existing =
                memberships.get(
                        npc
                );

        if (existing != null
                && existing.active()
                && definition.exclusiveMembership()) {

            throw new IllegalStateException(
                    "NPC already has an active exclusive religious membership"
            );
        }

        ReligiousMembership membership =
                new ReligiousMembership(
                        npc,
                        definition.id(),
                        roleId,
                        commitment,
                        true
                );

        memberships.put(
                npc,
                membership
        );

        return membership;
    }

    public synchronized void leave(
            NpcId npc
    ) {

        ReligiousMembership membership =
                memberships.get(
                        npc
                );

        if (membership != null) {

            membership.setActive(
                    false
            );
        }
    }

    public synchronized void registerLoaded(
            ReligiousMembership membership
    ) {

        Objects.requireNonNull(
                membership,
                "membership"
        );

        validateNpc(
                membership.npc()
        );

        if (ReligiousInstitutionCatalog.get()
                .find(
                        membership.orderId()
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown religious institution "
                            + membership.orderId()
            );
        }

        memberships.put(
                membership.npc(),
                membership
        );
    }

    public synchronized List<ReligiousMembership> all() {

        return List.copyOf(
                memberships.values()
        );
    }

    private void validateNpc(
            NpcId npc
    ) {

        if (!registry.contains(
                npc
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC "
                            + npc
            );
        }
    }
}
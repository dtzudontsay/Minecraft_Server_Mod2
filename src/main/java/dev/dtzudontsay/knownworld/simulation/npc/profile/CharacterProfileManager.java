package dev.dtzudontsay.knownworld.simulation.npc.profile;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CharacterProfileManager {

    private final NpcRegistry registry;

    private final Map<NpcId, CharacterProfile> profiles =
            new LinkedHashMap<>();

    public CharacterProfileManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized CharacterProfile getOrCreate(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return profiles.computeIfAbsent(
                npc,
                CharacterProfile::new
        );
    }

    public synchronized Optional<CharacterProfile> find(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return Optional.ofNullable(
                profiles.get(
                        npc
                )
        );
    }

    public synchronized void registerLoaded(
            CharacterProfile profile
    ) {
        Objects.requireNonNull(
                profile,
                "profile"
        );

        validateNpc(
                profile.owner()
        );

        profiles.put(
                profile.owner(),
                profile
        );
    }

    public synchronized List<CharacterProfile> all() {
        return List.copyOf(
                profiles.values()
        );
    }

    public synchronized int size() {
        return profiles.size();
    }

    public synchronized void ensureAll() {

        for (
                var npc :
                registry.all()
        ) {

            profiles.computeIfAbsent(
                    npc.id(),
                    CharacterProfile::new
            );
        }
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
                    "Unknown NPC ID: "
                            + npc
            );
        }
    }
}
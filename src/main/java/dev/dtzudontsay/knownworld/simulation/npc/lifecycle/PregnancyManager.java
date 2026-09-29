package dev.dtzudontsay.knownworld.simulation.npc.lifecycle;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PregnancyManager {

    public static final int DEFAULT_PREGNANCY_DAYS =
            280;

    private final NpcRegistry registry;

    private final Map<NpcId, Pregnancy> pregnancies =
            new LinkedHashMap<>();

    public PregnancyManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized Pregnancy conceive(
            NpcId mother,
            NpcId father,
            long currentDay
    ) {
        validateNpc(
                mother
        );

        validateNpc(
                father
        );

        if (pregnancies.containsKey(
                mother
        )) {

            throw new IllegalStateException(
                    "NPC #"
                            + mother
                            + " is already pregnant"
            );
        }

        Pregnancy pregnancy =
                new Pregnancy(
                        mother,
                        father,
                        currentDay,
                        currentDay
                                + DEFAULT_PREGNANCY_DAYS
                );

        pregnancies.put(
                mother,
                pregnancy
        );

        return pregnancy;
    }

    public synchronized void registerLoaded(
            Pregnancy pregnancy
    ) {
        Objects.requireNonNull(
                pregnancy,
                "pregnancy"
        );

        validateNpc(
                pregnancy.mother()
        );

        validateNpc(
                pregnancy.father()
        );

        pregnancies.put(
                pregnancy.mother(),
                pregnancy
        );
    }

    public synchronized Optional<Pregnancy> findByMother(
            NpcId mother
    ) {
        validateNpc(
                mother
        );

        return Optional.ofNullable(
                pregnancies.get(
                        mother
                )
        );
    }

    public synchronized boolean isPregnant(
            NpcId mother
    ) {
        return findByMother(
                mother
        )
                .isPresent();
    }

    public synchronized void complete(
            NpcId mother
    ) {
        pregnancies.remove(
                mother
        );
    }

    public synchronized List<Pregnancy> all() {
        return List.copyOf(
                pregnancies.values()
        );
    }

    public synchronized int size() {
        return pregnancies.size();
    }

    private void validateNpc(
            NpcId npc
    ) {
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
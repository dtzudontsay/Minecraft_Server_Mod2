package dev.dtzudontsay.knownworld.simulation.npc.goal;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class NpcGoalManager {

    private final NpcRegistry registry;

    private final Map<NpcGoalId, NpcGoal> goals =
            new LinkedHashMap<>();

    private final Map<NpcId, List<NpcGoalId>> goalsByOwner =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public NpcGoalManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized NpcGoal create(
            NpcId owner,
            NpcGoalType type,
            String description,
            double priority,
            long createdTick,
            NpcId targetNpc,
            SimulationPosition targetPosition,
            String factKey
    ) {
        validateNpc(
                owner
        );

        if (targetNpc != null) {
            validateNpc(
                    targetNpc
            );
        }

        NpcGoal goal =
                new NpcGoal(
                        allocateId(),
                        owner,
                        type,
                        description,
                        priority,
                        createdTick,
                        targetNpc,
                        targetPosition,
                        factKey,
                        NpcGoalStatus.PENDING
                );

        put(
                goal
        );

        return goal;
    }

    public synchronized void registerLoaded(
            NpcGoal goal
    ) {
        Objects.requireNonNull(
                goal,
                "goal"
        );

        validateNpc(
                goal.owner()
        );

        if (goal.targetNpc() != null) {
            validateNpc(
                    goal.targetNpc()
            );
        }

        if (goals.containsKey(
                goal.id()
        )) {

            throw new IllegalStateException(
                    "Duplicate goal ID: "
                            + goal.id()
            );
        }

        put(
                goal
        );

        if (goal.id().value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            goal.id()
                                    .value()
                                    + 1
                    );
        }
    }

    public synchronized Optional<NpcGoal> find(
            NpcGoalId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        return Optional.ofNullable(
                goals.get(
                        id
                )
        );
    }

    public synchronized List<NpcGoal> goalsOf(
            NpcId owner
    ) {
        validateNpc(
                owner
        );

        List<NpcGoalId> ids =
                goalsByOwner.get(
                        owner
                );

        if (ids == null) {
            return List.of();
        }

        List<NpcGoal> result =
                new ArrayList<>();

        for (NpcGoalId id : ids) {

            NpcGoal goal =
                    goals.get(
                            id
                    );

            if (goal != null) {
                result.add(
                        goal
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public synchronized Optional<NpcGoal> currentGoal(
            NpcId owner
    ) {
        return goalsOf(
                owner
        )
                .stream()
                .filter(
                        goal ->
                                !goal.isTerminal()
                )
                .sorted(
                        Comparator
                                .comparingDouble(
                                        NpcGoal::priority
                                )
                                .reversed()
                                .thenComparing(
                                        NpcGoal::createdTick
                                )
                )
                .findFirst();
    }

    public synchronized boolean hasOpenGoalOfType(
            NpcId owner,
            NpcGoalType type
    ) {
        Objects.requireNonNull(
                type,
                "type"
        );

        return goalsOf(
                owner
        )
                .stream()
                .anyMatch(
                        goal ->
                                goal.type()
                                        == type
                                        && !goal.isTerminal()
                );
    }

    public synchronized int cancelOpenGoalsOfType(
            NpcId owner,
            NpcGoalType type
    ) {
        Objects.requireNonNull(
                type,
                "type"
        );

        int cancelled =
                0;

        for (
                NpcGoal goal :
                goalsOf(
                        owner
                )
        ) {

            if (goal.type()
                    == type
                    && !goal.isTerminal()) {

                goal.cancel();

                cancelled++;
            }
        }

        return cancelled;
    }

    public synchronized Collection<NpcGoal> all() {
        return List.copyOf(
                goals.values()
        );
    }

    public synchronized int size() {
        return goals.size();
    }

    private void put(
            NpcGoal goal
    ) {
        goals.put(
                goal.id(),
                goal
        );

        goalsByOwner
                .computeIfAbsent(
                        goal.owner(),
                        ignored ->
                                new ArrayList<>()
                )
                .add(
                        goal.id()
                );
    }

    private NpcGoalId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Goal ID space exhausted"
            );
        }

        NpcGoalId id =
                new NpcGoalId(
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
}
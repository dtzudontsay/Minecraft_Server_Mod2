package dev.dtzudontsay.knownworld.simulation.social.title;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TitleManager {

    private final NpcRegistry npcs;

    private final OrganizationManager organizations;

    private final SettlementManager settlements;

    private final Map<TitleId, TitleDefinition> definitions =
            new LinkedHashMap<>();

    private final List<TitleAssignment> assignments =
            new ArrayList<>();

    private long nextId =
            1L;

    public TitleManager(
            NpcRegistry npcs,
            OrganizationManager organizations,
            SettlementManager settlements
    ) {
        this.npcs =
                Objects.requireNonNull(
                        npcs,
                        "npcs"
                );

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );

        this.settlements =
                Objects.requireNonNull(
                        settlements,
                        "settlements"
                );
    }

    public synchronized TitleDefinition create(
            String name,
            TitleType type,
            int authority,
            boolean exclusive,
            OrganizationId organization,
            SettlementId settlement
    ) {
        validateOrganizationIfPresent(
                organization
        );

        validateSettlementIfPresent(
                settlement
        );

        TitleDefinition definition =
                new TitleDefinition(
                        allocateId(),
                        name,
                        type,
                        authority,
                        exclusive,
                        organization,
                        settlement
                );

        definitions.put(
                definition.id(),
                definition
        );

        return definition;
    }

    public synchronized void registerLoadedDefinition(
            TitleDefinition definition
    ) {
        Objects.requireNonNull(
                definition,
                "definition"
        );

        validateOrganizationIfPresent(
                definition.organization()
        );

        validateSettlementIfPresent(
                definition.settlement()
        );

        if (definitions.containsKey(
                definition.id()
        )) {
            throw new IllegalStateException(
                    "Duplicate title ID: "
                            + definition.id()
            );
        }

        definitions.put(
                definition.id(),
                definition
        );

        if (definition.id().value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            definition.id()
                                    .value()
                                    + 1
                    );
        }
    }

    public synchronized TitleAssignment grant(
            TitleId titleId,
            NpcId npc,
            long tick
    ) {
        TitleDefinition title =
                requireTitle(
                        titleId
                );

        validateNpc(
                npc
        );

        for (TitleAssignment assignment : assignments) {

            if (assignment.title()
                    .equals(
                            titleId
                    )
                    && assignment.holder()
                    .equals(
                            npc
                    )
                    && assignment.isActive()) {

                return assignment;
            }
        }

        /*
         * An exclusive title automatically removes its previous holder.
         *
         * This gives us a primitive but useful succession mechanism.
         */
        if (title.exclusive()) {

            for (TitleAssignment assignment : assignments) {

                if (assignment.title()
                        .equals(
                                titleId
                        )
                        && assignment.isActive()) {

                    assignment.revoke(
                            tick
                    );
                }
            }
        }

        TitleAssignment assignment =
                new TitleAssignment(
                        titleId,
                        npc,
                        tick,
                        null
                );

        assignments.add(
                assignment
        );

        return assignment;
    }

    public synchronized void registerLoadedAssignment(
            TitleAssignment assignment
    ) {
        Objects.requireNonNull(
                assignment,
                "assignment"
        );

        requireTitle(
                assignment.title()
        );

        validateNpc(
                assignment.holder()
        );

        assignments.add(
                assignment
        );
    }

    public synchronized boolean revoke(
            TitleId title,
            NpcId holder,
            long tick
    ) {
        requireTitle(
                title
        );

        validateNpc(
                holder
        );

        boolean changed =
                false;

        for (TitleAssignment assignment : assignments) {

            if (assignment.title()
                    .equals(
                            title
                    )
                    && assignment.holder()
                    .equals(
                            holder
                    )
                    && assignment.isActive()) {

                assignment.revoke(
                        tick
                );

                changed =
                        true;
            }
        }

        return changed;
    }

    public synchronized int revokeAll(
            TitleId title,
            long tick
    ) {
        requireTitle(
                title
        );

        int revoked =
                0;

        for (TitleAssignment assignment : assignments) {

            if (assignment.title()
                    .equals(
                            title
                    )
                    && assignment.isActive()) {

                assignment.revoke(
                        tick
                );

                revoked++;
            }
        }

        return revoked;
    }

    public synchronized Optional<TitleDefinition> find(
            TitleId id
    ) {
        return Optional.ofNullable(
                definitions.get(
                        id
                )
        );
    }

    public synchronized List<TitleDefinition> allDefinitions() {
        return definitions.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                TitleDefinition::id
                        )
                )
                .toList();
    }

    public synchronized List<TitleAssignment> allAssignments() {
        return List.copyOf(
                assignments
        );
    }

    public synchronized List<TitleAssignment> activeAssignmentsOf(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return assignments.stream()
                .filter(
                        TitleAssignment::isActive
                )
                .filter(
                        assignment ->
                                assignment.holder()
                                        .equals(
                                                npc
                                        )
                )
                .toList();
    }

    public synchronized List<TitleDefinition> activeTitlesOf(
            NpcId npc
    ) {
        return activeAssignmentsOf(
                npc
        )
                .stream()
                .map(
                        assignment ->
                                definitions.get(
                                        assignment.title()
                                )
                )
                .filter(
                        Objects::nonNull
                )
                .sorted(
                        Comparator
                                .comparingInt(
                                        TitleDefinition::authority
                                )
                                .reversed()
                                .thenComparing(
                                        TitleDefinition::id
                                )
                )
                .toList();
    }

    public synchronized List<TitleAssignment> activeAssignmentsForTitle(
            TitleId title
    ) {
        requireTitle(
                title
        );

        return assignments.stream()
                .filter(
                        TitleAssignment::isActive
                )
                .filter(
                        assignment ->
                                assignment.title()
                                        .equals(
                                                title
                                        )
                )
                .toList();
    }

    public synchronized int definitionCount() {
        return definitions.size();
    }

    public synchronized long activeAssignmentCount() {
        return assignments.stream()
                .filter(
                        TitleAssignment::isActive
                )
                .count();
    }

    private TitleDefinition requireTitle(
            TitleId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        TitleDefinition title =
                definitions.get(
                        id
                );

        if (title == null) {
            throw new IllegalArgumentException(
                    "Unknown title ID: "
                            + id
            );
        }

        return title;
    }

    private void validateNpc(
            NpcId npc
    ) {
        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (!npcs.contains(
                npc
        )) {
            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + npc
            );
        }
    }

    private void validateOrganizationIfPresent(
            OrganizationId organization
    ) {
        if (organization != null
                && organizations.find(
                organization
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown organization ID: "
                            + organization
            );
        }
    }

    private void validateSettlementIfPresent(
            SettlementId settlement
    ) {
        if (settlement != null
                && settlements.find(
                settlement
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown settlement ID: "
                            + settlement
            );
        }
    }

    private TitleId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Title ID space exhausted"
            );
        }

        TitleId id =
                new TitleId(
                        nextId
                );

        nextId++;

        return id;
    }
}
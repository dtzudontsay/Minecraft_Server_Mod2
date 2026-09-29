package dev.dtzudontsay.knownworld.simulation.social.succession;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.family.GenealogyManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleAssignment;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleDefinition;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SuccessionService {

    private final NpcRegistry registry;

    private final GenealogyManager genealogy;

    private final TitleManager titles;

    private final SuccessionRuleManager rules;

    private final ClaimManager claims;

    private final WorldEventManager events;

    public SuccessionService(
            NpcRegistry registry,
            GenealogyManager genealogy,
            TitleManager titles,
            SuccessionRuleManager rules,
            ClaimManager claims,
            WorldEventManager events
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.genealogy =
                Objects.requireNonNull(
                        genealogy,
                        "genealogy"
                );

        this.titles =
                Objects.requireNonNull(
                        titles,
                        "titles"
                );

        this.rules =
                Objects.requireNonNull(
                        rules,
                        "rules"
                );

        this.claims =
                Objects.requireNonNull(
                        claims,
                        "claims"
                );

        this.events =
                Objects.requireNonNull(
                        events,
                        "events"
                );
    }

    public void handleDeath(
            NpcId deceased,
            long tick
    ) {
        List<TitleAssignment> held =
                new ArrayList<>(
                        titles.activeAssignmentsOf(
                                deceased
                        )
                );

        for (
                TitleAssignment assignment :
                held
        ) {

            processTitleDeath(
                    deceased,
                    assignment.title(),
                    tick
            );
        }
    }

    public Optional<NpcId> heirOf(
            TitleId title,
            NpcId currentHolder
    ) {
        List<NpcId> candidates =
                rankedCandidates(
                        title,
                        currentHolder
                );

        return candidates.stream()
                .filter(
                        this::isAlive
                )
                .findFirst();
    }

    public List<NpcId> rankedCandidates(
            TitleId title,
            NpcId currentHolder
    ) {
        Objects.requireNonNull(
                title,
                "title"
        );

        Objects.requireNonNull(
                currentHolder,
                "currentHolder"
        );

        SuccessionLaw law =
                rules.lawOf(
                        title
                );

        if (law == SuccessionLaw.NONE) {
            return List.of();
        }

        Set<NpcId> result =
                new LinkedHashSet<>();

        addDescendantBranches(
                currentHolder,
                law,
                result
        );

        addSiblingBranches(
                currentHolder,
                law,
                result
        );

        addParents(
                currentHolder,
                law,
                result
        );

        result.remove(
                currentHolder
        );

        return List.copyOf(
                result
        );
    }

    private void processTitleDeath(
            NpcId deceased,
            TitleId title,
            long tick
    ) {
        TitleDefinition definition =
                titles.find(
                                title
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown title ID: "
                                                        + title
                                        )
                        );

        SuccessionLaw law =
                rules.lawOf(
                        title
                );

        titles.revoke(
                title,
                deceased,
                tick
        );

        if (law == SuccessionLaw.NONE) {

            events.create(
                    WorldEventType.POLITICAL,
                    definition.name()
                            + " became vacant after the death of "
                            + name(
                            deceased
                    )
                            + ".",
                    positionOf(
                            deceased
                    ),
                    tick,
                    0.75,
                    deceased,
                    deceased,
                    "title.vacant",
                    title.toString()
            );

            return;
        }

        List<NpcId> candidates =
                rankedCandidates(
                        title,
                        deceased
                );

        NpcId heir =
                candidates.stream()
                        .filter(
                                this::isAlive
                        )
                        .findFirst()
                        .orElse(
                                null
                        );

        if (heir == null) {

            events.create(
                    WorldEventType.POLITICAL,
                    definition.name()
                            + " has no eligible heir.",
                    positionOf(
                            deceased
                    ),
                    tick,
                    0.90,
                    deceased,
                    deceased,
                    "title.heir",
                    "none"
            );

            return;
        }

        titles.grant(
                title,
                heir,
                tick
        );

        claims.resolveClaimsOfHolder(
                heir,
                title
        );

        int createdClaims =
                0;

        for (
                NpcId candidate :
                candidates
        ) {

            if (candidate.equals(
                    heir
            )) {
                continue;
            }

            if (!isAlive(
                    candidate
            )) {
                continue;
            }

            claims.addClaim(
                    candidate,
                    title,
                    createdClaims < 2
                            ? ClaimStrength.STRONG
                            : ClaimStrength.WEAK,
                    deceased,
                    tick
            );

            createdClaims++;

            if (createdClaims >= 5) {
                break;
            }
        }

        events.create(
                WorldEventType.POLITICAL,
                name(
                        heir
                )
                        + " inherited "
                        + definition.name()
                        + " after the death of "
                        + name(
                        deceased
                )
                        + ".",
                positionOf(
                        heir
                ),
                tick,
                0.95,
                heir,
                deceased,
                "title.inherited",
                title.toString()
        );
    }

    private void addDescendantBranches(
            NpcId parent,
            SuccessionLaw law,
            Set<NpcId> result
    ) {
        for (
                NpcId child :
                orderedChildren(
                        parent,
                        law
                )
        ) {

            result.add(
                    child
            );

            addDescendantBranches(
                    child,
                    law,
                    result
            );
        }
    }

    private void addSiblingBranches(
            NpcId npc,
            SuccessionLaw law,
            Set<NpcId> result
    ) {
        Optional<Parentage> parentage =
                genealogy.parentsOf(
                        npc
                );

        if (parentage.isEmpty()) {
            return;
        }

        Parentage parents =
                parentage.get();

        List<NpcId> parentIds =
                new ArrayList<>();

        if (parents.father() != null) {

            parentIds.add(
                    parents.father()
            );
        }

        if (parents.mother() != null) {

            parentIds.add(
                    parents.mother()
            );
        }

        for (
                NpcId parent :
                parentIds
        ) {

            for (
                    NpcId sibling :
                    orderedChildren(
                            parent,
                            law
                    )
            ) {

                if (sibling.equals(
                        npc
                )) {
                    continue;
                }

                result.add(
                        sibling
                );

                addDescendantBranches(
                        sibling,
                        law,
                        result
                );
            }
        }
    }

    private void addParents(
            NpcId npc,
            SuccessionLaw law,
            Set<NpcId> result
    ) {
        Optional<Parentage> parentage =
                genealogy.parentsOf(
                        npc
                );

        if (parentage.isEmpty()) {
            return;
        }

        Parentage parents =
                parentage.get();

        if (parents.father() != null
                && eligibleBySex(
                parents.father(),
                law
        )) {

            result.add(
                    parents.father()
            );
        }

        if (parents.mother() != null
                && eligibleBySex(
                parents.mother(),
                law
        )) {

            result.add(
                    parents.mother()
            );
        }
    }

    private List<NpcId> orderedChildren(
            NpcId parent,
            SuccessionLaw law
    ) {
        List<NpcId> children =
                new ArrayList<>(
                        genealogy.childrenOf(
                                parent
                        )
                );

        Comparator<NpcId> ageComparator =
                Comparator
                        .comparingInt(
                                (NpcId child) ->
                                        birthYear(
                                                child
                                        )
                        )
                        .thenComparingLong(
                                NpcId::value
                        );

        if (law
                == SuccessionLaw.ABSOLUTE_PRIMOGENITURE) {

            children.sort(
                    ageComparator
            );

            return children;
        }

        if (law
                == SuccessionLaw.MALE_ONLY_PRIMOGENITURE) {

            children.removeIf(
                    child ->
                            sexOf(
                                    child
                            )
                                    != NpcSex.MALE
            );

            children.sort(
                    ageComparator
            );

            return children;
        }

        Comparator<NpcId> malePreferenceComparator =
                Comparator
                        .comparingInt(
                                (NpcId child) ->
                                        sexOf(
                                                child
                                        )
                                                == NpcSex.MALE
                                                ? 0
                                                : 1
                        )
                        .thenComparing(
                                ageComparator
                        );

        children.sort(
                malePreferenceComparator
        );

        return children;
    }

    private boolean eligibleBySex(
            NpcId npc,
            SuccessionLaw law
    ) {
        if (law
                != SuccessionLaw.MALE_ONLY_PRIMOGENITURE) {

            return true;
        }

        return sexOf(
                npc
        )
                == NpcSex.MALE;
    }

    private int birthYear(
            NpcId npc
    ) {
        return registry.find(
                        npc
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC ID: "
                                                + npc
                                )
                )
                .identity()
                .birthYear();
    }

    private NpcSex sexOf(
            NpcId npc
    ) {
        return registry.find(
                        npc
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC ID: "
                                                + npc
                                )
                )
                .identity()
                .sex();
    }

    private boolean isAlive(
            NpcId npc
    ) {
        return registry.find(
                        npc
                )
                .map(
                        NpcState::isAlive
                )
                .orElse(
                        false
                );
    }

    private String name(
            NpcId npc
    ) {
        return registry.find(
                        npc
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC ID: "
                                                + npc
                                )
                )
                .identity()
                .fullName();
    }

    private SimulationPosition positionOf(
            NpcId npc
    ) {
        return registry.find(
                        npc
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC ID: "
                                                + npc
                                )
                )
                .position();
    }
}
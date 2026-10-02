package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SimulationReportService {

    private final NpcRegistry registry;

    private final NpcRelationshipManager relationships;

    public SimulationReportService(
            NpcRegistry registry,
            NpcRelationshipManager relationships
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
                );
    }

    public Report build(
            List<NpcId> population
    ) {

        Set<NpcId> cohort =
                Set.copyOf(
                        population
                );

        long alive =
                population.stream()
                        .map(
                                registry::find
                        )
                        .flatMap(
                                java.util.Optional::stream
                        )
                        .filter(
                                NpcState::isAlive
                        )
                        .count();

        List<NpcRelationship> relevant =
                relationships.all()
                        .stream()
                        .filter(
                                relation ->
                                        cohort.contains(
                                                relation.subject()
                                        )
                                                &&
                                                cohort.contains(
                                                        relation.target()
                                                )
                        )
                        .toList();

        int positive =
                0;

        int hostile =
                0;

        int saturated =
                0;

        double affectionTotal =
                0.0;

        double trustTotal =
                0.0;

        double respectTotal =
                0.0;

        double fearTotal =
                0.0;

        for (
                NpcRelationship relation :
                relevant
        ) {

            affectionTotal +=
                    relation.affection();

            trustTotal +=
                    relation.trust();

            respectTotal +=
                    relation.respect();

            fearTotal +=
                    relation.fear();

            if (relation.affection()
                    >= 0.50
                    &&
                    relation.trust()
                            >= 0.40) {

                positive++;
            }

            if (relation.affection()
                    <= -0.50
                    ||
                    relation.trust()
                            <= -0.50) {

                hostile++;
            }

            if (Math.abs(
                    relation.affection()
            ) >= 0.95
                    ||
                    Math.abs(
                            relation.trust()
                    ) >= 0.95
                    ||
                    Math.abs(
                            relation.respect()
                    ) >= 0.95
                    ||
                    relation.fear()
                            >= 0.95) {

                saturated++;
            }
        }

        int count =
                relevant.size();

        List<RelationshipSummary> strongestPositive =
                relevant.stream()
                        .sorted(
                                Comparator.comparingDouble(
                                                SimulationReportService::positiveScore
                                        )
                                        .reversed()
                        )
                        .limit(
                                5
                        )
                        .map(
                                this::summary
                        )
                        .toList();

        List<RelationshipSummary> strongestNegative =
                relevant.stream()
                        .sorted(
                                Comparator.comparingDouble(
                                        SimulationReportService::positiveScore
                                )
                        )
                        .limit(
                                5
                        )
                        .map(
                                this::summary
                        )
                        .toList();

        return new Report(
                population.size(),
                alive,
                count,
                positive,
                hostile,
                saturated,
                average(
                        affectionTotal,
                        count
                ),
                average(
                        trustTotal,
                        count
                ),
                average(
                        respectTotal,
                        count
                ),
                average(
                        fearTotal,
                        count
                ),
                strongestPositive,
                strongestNegative
        );
    }

    private RelationshipSummary summary(
            NpcRelationship relation
    ) {

        return new RelationshipSummary(
                nameOf(
                        relation.subject()
                ),
                nameOf(
                        relation.target()
                ),
                relation.affection(),
                relation.trust(),
                relation.respect(),
                relation.fear(),
                relation.familiarity()
        );
    }

    private String nameOf(
            NpcId npc
    ) {

        return registry.find(
                        npc
                )
                .map(
                        value ->
                                value.identity()
                                        .fullName()
                )
                .orElse(
                        "#"
                                + npc
                );
    }

    private static double positiveScore(
            NpcRelationship relation
    ) {

        return relation.affection()
                +
                relation.trust()
                +
                relation.respect()
                -
                relation.fear()
                        * 0.25;
    }

    private static double average(
            double total,
            int count
    ) {

        return count == 0
                ? 0.0
                : total
                / count;
    }

    public record RelationshipSummary(
            String subject,
            String target,
            double affection,
            double trust,
            double respect,
            double fear,
            double familiarity
    ) {
    }

    public record Report(
            int population,
            long alive,
            int directionalRelationships,
            int positiveRelationships,
            int hostileRelationships,
            int saturatedRelationships,
            double averageAffection,
            double averageTrust,
            double averageRespect,
            double averageFear,
            List<RelationshipSummary> strongestPositive,
            List<RelationshipSummary> strongestNegative
    ) {
    }
}
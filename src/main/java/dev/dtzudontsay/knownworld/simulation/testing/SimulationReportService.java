package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemory;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryMeaningService;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialContextService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SimulationReportService {

    private final NpcRegistry registry;

    private final NpcRelationshipManager relationships;

    private final CharacterProfileManager profiles;

    private final SocialContextService contexts;

    private final NpcMemoryManager memories;

    private final NpcMemoryMeaningService memoryMeaning;

    public SimulationReportService(
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            CharacterProfileManager profiles,
            SocialContextService contexts,
            NpcMemoryManager memories,
            NpcMemoryMeaningService memoryMeaning
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

        this.profiles =
                Objects.requireNonNull(
                        profiles,
                        "profiles"
                );

        this.contexts =
                Objects.requireNonNull(
                        contexts,
                        "contexts"
                );

        this.memories =
                Objects.requireNonNull(
                        memories,
                        "memories"
                );

        this.memoryMeaning =
                Objects.requireNonNull(
                        memoryMeaning,
                        "memoryMeaning"
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

        int sameHousehold =
                0;

        int sameNobleHouse =
                0;

        int crossNobleHouse =
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

            SocialContextService.Context context =
                    contexts.between(
                            relation.subject(),
                            relation.target()
                    );

            if (context.sameHousehold()) {

                sameHousehold++;
            }

            if (context.sameNobleHouse()) {

                sameNobleHouse++;

            } else {

                crossNobleHouse++;
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
                sameHousehold,
                sameNobleHouse,
                crossNobleHouse,
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
                strongestNegative,
                buildSkillReport(
                        population
                ),
                buildMemoryReport(
                        cohort
                )
        );
    }

    private SkillReport buildSkillReport(
            List<NpcId> population
    ) {

        double diplomacy =
                0.0;

        double intrigue =
                0.0;

        double leadership =
                0.0;

        double highest =
                0.0;

        String highestNpc =
                "none";

        CharacterSkill highestSkill =
                null;

        int counted =
                0;

        for (
                NpcId npc :
                population
        ) {

            CharacterProfile profile =
                    profiles.find(
                                    npc
                            )
                            .orElse(
                                    null
                            );

            if (profile == null) {

                continue;
            }

            counted++;

            diplomacy +=
                    profile.skill(
                            CharacterSkill.DIPLOMACY
                    );

            intrigue +=
                    profile.skill(
                            CharacterSkill.INTRIGUE
                    );

            leadership +=
                    profile.skill(
                            CharacterSkill.LEADERSHIP
                    );

            for (
                    CharacterSkill skill :
                    CharacterSkill.values()
            ) {

                double value =
                        profile.skill(
                                skill
                        );

                if (value > highest) {

                    highest =
                            value;

                    highestSkill =
                            skill;

                    highestNpc =
                            nameOf(
                                    npc
                            );
                }
            }
        }

        return new SkillReport(
                average(
                        diplomacy,
                        counted
                ),
                average(
                        intrigue,
                        counted
                ),
                average(
                        leadership,
                        counted
                ),
                highestNpc,
                highestSkill,
                highest
        );
    }

    private MemoryReport buildMemoryReport(
            Set<NpcId> cohort
    ) {

        int semantic =
                0;

        int positive =
                0;

        int negative =
                0;

        int betrayal =
                0;

        int threat =
                0;

        for (
                NpcMemory memory :
                memories.all()
        ) {

            if (!cohort.contains(
                    memory.owner()
            )) {

                continue;
            }

            if (memory.factKey() == null) {

                continue;
            }

            NpcMemoryMeaningService.MeaningContribution interpretation =
                    memoryMeaning.interpret(
                            memory
                    );

            if (interpretation
                    == NpcMemoryMeaningService.MeaningContribution.NEUTRAL) {

                continue;
            }

            semantic++;

            if (interpretation.valence()
                    > 0.0) {

                positive++;
            }

            if (interpretation.valence()
                    < 0.0) {

                negative++;
            }

            if (interpretation.betrayal()
                    > 0.0) {

                betrayal++;
            }

            if (interpretation.threat()
                    > 0.0) {

                threat++;
            }
        }

        return new MemoryReport(
                semantic,
                positive,
                negative,
                betrayal,
                threat
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

    public record SkillReport(
            double averageDiplomacy,
            double averageIntrigue,
            double averageLeadership,
            String highestNpc,
            CharacterSkill highestSkill,
            double highestSkillValue
    ) {
    }

    public record MemoryReport(
            int semanticMemories,
            int positiveMemories,
            int negativeMemories,
            int betrayalMemories,
            int threatMemories
    ) {
    }

    public record Report(
            int population,
            long alive,
            int directionalRelationships,
            int positiveRelationships,
            int hostileRelationships,
            int saturatedRelationships,
            int sameHouseholdRelationships,
            int sameNobleHouseRelationships,
            int crossNobleHouseRelationships,
            double averageAffection,
            double averageTrust,
            double averageRespect,
            double averageFear,
            List<RelationshipSummary> strongestPositive,
            List<RelationshipSummary> strongestNegative,
            SkillReport skills,
            MemoryReport memories
    ) {
    }
}
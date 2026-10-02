package dev.dtzudontsay.knownworld.simulation.social.relationship;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RelationshipReviewAudit {

    private final NpcSimulation simulation;

    private final SocietyStructureRuntime society;

    private final Map<String, Integer> directEdgeCounts;

    private final RelationshipReviewCatalog.Definition roster;

    public RelationshipReviewAudit(
            NpcSimulation simulation,
            SocietyStructureRuntime society,
            GlobalRelationshipCatalog.Definition relationships,
            RelationshipReviewCatalog.Definition roster
    ) {

        this.simulation =
                Objects.requireNonNull(
                        simulation,
                        "simulation"
                );

        this.society =
                Objects.requireNonNull(
                        society,
                        "society"
                );

        Objects.requireNonNull(
                relationships,
                "relationships"
        );

        this.roster =
                Objects.requireNonNull(
                        roster,
                        "roster"
                );

        this.directEdgeCounts =
                countDirectEdges(
                        relationships
                );
    }

    public Report audit() {

        List<Result> results =
                new ArrayList<>();

        Map<Status, Integer> counts =
                new LinkedHashMap<>();

        for (
                Status status :
                Status.values()
        ) {

            counts.put(
                    status,
                    0
            );
        }

        for (
                RelationshipReviewCatalog.Entry entry :
                roster.entries()
        ) {

            Result result =
                    classify(
                            entry
                    );

            results.add(
                    result
            );

            counts.put(
                    result.status(),
                    counts.get(
                            result.status()
                    ) + 1
            );
        }

        results.sort(
                Comparator.comparing(
                                (Result value) ->
                                        value.status()
                                                .ordinal()
                        )
                        .thenComparing(
                                Result::rosterName
                        )
        );

        return new Report(
                List.copyOf(
                        results
                ),
                Map.copyOf(
                        counts
                )
        );
    }

    private Result classify(
            RelationshipReviewCatalog.Entry entry
    ) {

        RelationshipReviewCatalog.DeclaredStatus declared =
                entry.declaredStatus();

        if (declared
                == RelationshipReviewCatalog.DeclaredStatus.NO_SUPPORTED_RELATION_FOUND) {

            return result(
                    entry,
                    Status.NO_SUPPORTED_RELATION_FOUND,
                    directEdgeCounts.getOrDefault(
                            entry.npcId(),
                            0
                    ),
                    false
            );
        }

        if (declared
                == RelationshipReviewCatalog.DeclaredStatus.SPECIAL_ENTITY_PENDING) {

            return result(
                    entry,
                    Status.SPECIAL_ENTITY_PENDING,
                    directEdgeCounts.getOrDefault(
                            entry.npcId(),
                            0
                    ),
                    false
            );
        }

        if (declared
                == RelationshipReviewCatalog.DeclaredStatus.CONTINUITY_UNCERTAIN) {

            return result(
                    entry,
                    Status.CONTINUITY_UNCERTAIN,
                    directEdgeCounts.getOrDefault(
                            entry.npcId(),
                            0
                    ),
                    false
            );
        }

        int direct =
                directEdgeCounts.getOrDefault(
                        entry.npcId(),
                        0
                );

        if (direct > 0) {

            return result(
                    entry,
                    Status.HAS_DIRECT_RELATION,
                    direct,
                    false
            );
        }

        NpcId npc =
                simulation.authoredIds()
                        .findNpc(
                                entry.npcId()
                        )
                        .orElse(
                                null
                        );

        if (npc == null) {

            return result(
                    entry,
                    Status.UNREVIEWED,
                    0,
                    false
            );
        }

        boolean fallback =
                hasMeaningfulFallback(
                        npc
                );

        return result(
                entry,
                fallback
                        ? Status.FALLBACK_ONLY
                        : Status.UNREVIEWED,
                0,
                fallback
        );
    }

    private boolean hasMeaningfulFallback(
            NpcId npc
    ) {

        CharacterSocialIdentity identity =
                society.characterSocialIdentityService()
                        .ensureIdentity(
                                npc
                        );

        if (identity.currentDynasty() != null
                || identity.legalFamilyDynasty() != null
                || identity.birthDynasty() != null
                || identity.householdOrganization() != null
                || identity.houseOrganization() != null
                || identity.primaryAllegianceOrganization() != null) {

            return true;
        }

        return !society.memberships()
                .activeMembershipsFor(
                        npc
                )
                .isEmpty();
    }

    private static Result result(
            RelationshipReviewCatalog.Entry entry,
            Status status,
            int directEdges,
            boolean fallbackAvailable
    ) {

        return new Result(
                entry.rosterName(),
                entry.npcId(),
                status,
                directEdges,
                fallbackAvailable,
                entry.note()
        );
    }

    private static Map<String, Integer> countDirectEdges(
            GlobalRelationshipCatalog.Definition definition
    ) {

        Map<String, Integer> result =
                new HashMap<>();

        for (
                GlobalRelationshipCatalog.CharacterRelationshipDefinition relationship :
                definition.characterRelationships()
        ) {

            result.merge(
                    relationship.subject(),
                    1,
                    Integer::sum
            );

            result.merge(
                    relationship.target(),
                    1,
                    Integer::sum
            );
        }

        return Map.copyOf(
                result
        );
    }

    public enum Status {

        HAS_DIRECT_RELATION,

        FALLBACK_ONLY,

        NO_SUPPORTED_RELATION_FOUND,

        CONTINUITY_UNCERTAIN,

        SPECIAL_ENTITY_PENDING,

        UNREVIEWED
    }

    public record Result(
            String rosterName,
            String npcId,
            Status status,
            int directEdgeCount,
            boolean fallbackAvailable,
            String note
    ) {
    }

    public record Report(
            List<Result> results,
            Map<Status, Integer> counts
    ) {

        public int count(
                Status status
        ) {

            return counts.getOrDefault(
                    status,
                    0
            );
        }

        public List<Result> withStatus(
                Status status
        ) {

            return results.stream()
                    .filter(
                            result ->
                                    result.status()
                                            == status
                    )
                    .toList();
        }

        public int total() {
            return results.size();
        }
    }
}

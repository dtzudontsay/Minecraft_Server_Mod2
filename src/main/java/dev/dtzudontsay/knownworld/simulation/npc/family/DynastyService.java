package dev.dtzudontsay.knownworld.simulation.npc.family;

import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;

import java.util.Objects;

/**
 * Resolves dynastic identity for newly-created children.
 *
 * Noble-house membership and surname are currently inherited from
 * the selected dynastic parent.
 *
 * Later this service can incorporate:
 *
 * - legitimacy
 * - bastardy
 * - legitimization
 * - cadet branches
 * - culture-specific naming
 * - house extinction
 * - founder-created dynasties
 */
public final class DynastyService {

    private final MarriageManager marriages;

    private final NpcAffiliationManager affiliations;

    public DynastyService(
            MarriageManager marriages,
            NpcAffiliationManager affiliations
    ) {
        this.marriages =
                Objects.requireNonNull(
                        marriages,
                        "marriages"
                );

        this.affiliations =
                Objects.requireNonNull(
                        affiliations,
                        "affiliations"
                );
    }

    public NpcState dynasticParent(
            NpcState mother,
            NpcState father
    ) {
        Objects.requireNonNull(
                mother,
                "mother"
        );

        if (father == null) {
            return mother;
        }

        return marriages.activeUnionBetween(
                        mother.id(),
                        father.id()
                )
                .filter(
                        MarriageRecord::isMarried
                )
                .map(
                        marriage -> {

                            if (marriage.inheritanceRule()
                                    == DynastyInheritanceRule.MATRILINEAL) {

                                return mother;
                            }

                            return father;
                        }
                )
                .orElseGet(
                        () ->
                                chooseFallbackParent(
                                        mother,
                                        father
                                )
                );
    }

    public String familyNameForChild(
            NpcState mother,
            NpcState father
    ) {
        NpcState parent =
                dynasticParent(
                        mother,
                        father
                );

        return parent.identity()
                .familyName();
    }

    public OrganizationId nobleHouseForChild(
            NpcState mother,
            NpcState father
    ) {
        NpcState primary =
                dynasticParent(
                        mother,
                        father
                );

        OrganizationId house =
                affiliations.getOrCreate(
                                primary.id()
                        )
                        .nobleHouse();

        if (house != null) {
            return house;
        }

        NpcState alternate =
                primary.id()
                        .equals(
                                mother.id()
                        )
                        ? father
                        : mother;

        if (alternate == null) {
            return null;
        }

        return affiliations.getOrCreate(
                        alternate.id()
                )
                .nobleHouse();
    }

    private NpcState chooseFallbackParent(
            NpcState mother,
            NpcState father
    ) {
        OrganizationId fatherHouse =
                affiliations.getOrCreate(
                                father.id()
                        )
                        .nobleHouse();

        if (fatherHouse != null) {
            return father;
        }

        return mother;
    }
}
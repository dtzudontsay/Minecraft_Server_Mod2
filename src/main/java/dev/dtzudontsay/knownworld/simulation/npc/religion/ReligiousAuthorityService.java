package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.util.Objects;

public final class ReligiousAuthorityService {

    private final CharacterProfileManager profiles;

    private final ReligionService religion;

    private final ReligiousMembershipManager memberships;

    public ReligiousAuthorityService(
            CharacterProfileManager profiles,
            ReligionService religion,
            ReligiousMembershipManager memberships
    ) {

        this.profiles =
                Objects.requireNonNull(
                        profiles,
                        "profiles"
                );

        this.religion =
                Objects.requireNonNull(
                        religion,
                        "religion"
                );

        this.memberships =
                Objects.requireNonNull(
                        memberships,
                        "memberships"
                );
    }

    /**
     * Institutional religious authority of an individual.
     *
     * This is intentionally a hook rather than a political decision.
     * Later title/realm systems can consume it.
     */
    public double religiousAuthority(
            NpcId npc
    ) {

        PersonalFaithSnapshot faith =
                religion.snapshot(
                        npc
                );

        ReligiousMembership membership =
                memberships.find(
                                npc
                        )
                        .filter(
                                ReligiousMembership::active
                        )
                        .orElse(
                                null
                        );

        if (membership == null) {

            return clampUnit(
                    faith.devotion()
                            * 0.20
                            + faith.doctrinalKnowledge()
                            * 0.15
                            + faith.observance()
                            * 0.10
            );
        }

        ReligiousOrderDefinition definition =
                ReligiousInstitutionCatalog.get()
                        .find(
                                membership.orderId()
                        )
                        .orElseThrow();

        double roleAuthority =
                0.0;

        if (definition.clergy()) {

            roleAuthority +=
                    0.15;
        }

        if (definition.militant()) {

            roleAuthority +=
                    0.05;
        }

        return clampUnit(
                membership.commitment()
                        * 0.30

                        + faith.devotion()
                        * 0.20

                        + faith.doctrinalKnowledge()
                        * 0.20

                        + faith.observance()
                        * 0.10

                        + roleAuthority
        );
    }

    /**
     * Religious legitimacy hook for future political/title systems.
     *
     * Returns a [0,1] support measure rather than deciding whether
     * a ruler is actually legitimate.
     */
    public ReligiousLegitimacyAssessment assessLegitimacy(
            NpcId npc,
            String expectedReligionId
    ) {

        validateReligion(
                expectedReligionId
        );

        PersonalFaithSnapshot faith =
                religion.snapshot(
                        npc
                );

        boolean sameReligion =
                expectedReligionId.equals(
                        faith.religionId()
                );

        double alignment =
                sameReligion
                        ? 1.0
                        : 0.0;

        double devotionSupport =
                sameReligion
                        ? faith.devotion()
                        : 0.0;

        double observanceSupport =
                sameReligion
                        ? faith.observance()
                        : 0.0;

        double institutionalStanding =
                0.0;

        ReligiousMembership membership =
                memberships.find(
                                npc
                        )
                        .filter(
                                ReligiousMembership::active
                        )
                        .orElse(
                                null
                        );

        if (membership != null) {

            ReligiousOrderDefinition definition =
                    ReligiousInstitutionCatalog.get()
                            .find(
                                    membership.orderId()
                            )
                            .orElseThrow();

            if (expectedReligionId.equals(
                    definition.religionId()
            )) {

                institutionalStanding =
                        clampUnit(
                                membership.commitment()
                                        * 0.60
                                        + religiousAuthority(
                                        npc
                                )
                                        * 0.40
                        );
            }
        }

        double total =
                alignment
                        * 0.45

                        + devotionSupport
                        * 0.20

                        + observanceSupport
                        * 0.15

                        + institutionalStanding
                        * 0.20;

        return new ReligiousLegitimacyAssessment(
                expectedReligionId,
                faith.religionId(),
                alignment,
                devotionSupport,
                observanceSupport,
                institutionalStanding,
                clampUnit(
                        total
                )
        );
    }

    private static void validateReligion(
            String religionId
    ) {

        if (religionId == null
                || religionId.isBlank()) {

            throw new IllegalArgumentException(
                    "Religion ID cannot be empty"
            );
        }

        if (WorldReferenceCatalog.get()
                .religion(
                        religionId
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown religion "
                            + religionId
            );
        }
    }

    private static double clampUnit(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
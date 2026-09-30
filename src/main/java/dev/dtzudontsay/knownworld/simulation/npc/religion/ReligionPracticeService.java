package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.social.Organization;

import java.util.Objects;

public final class ReligionPracticeService {

    private static final String RITUAL_PREFIX =
            "religion.ritual.";

    private static final String TABOO_PREFIX =
            "religion.taboo.";

    private final CharacterProfileManager profiles;

    private final ReligionService religion;

    private final ReligiousMembershipManager memberships;

    private final ReligiousInstitutionRuntimeManager institutions;

    public ReligionPracticeService(
            CharacterProfileManager profiles,
            ReligionService religion,
            ReligiousMembershipManager memberships,
            ReligiousInstitutionRuntimeManager institutions
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

        this.institutions =
                Objects.requireNonNull(
                        institutions,
                        "institutions"
                );
    }

    public boolean canJoinOrder(
            NpcId npc,
            String orderId
    ) {

        ReligiousOrderDefinition definition =
                ReligiousInstitutionCatalog.get()
                        .find(
                                orderId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown religious order "
                                                        + orderId
                                        )
                        );

        PersonalFaithSnapshot faith =
                religion.snapshot(
                        npc
                );

        if (!definition.religionId()
                .equals(
                        faith.religionId()
                )) {

            return false;
        }

        if (faith.devotion()
                < definition.minimumDevotion()) {

            return false;
        }

        return faith.doctrinalKnowledge()
                >= definition.minimumDoctrinalKnowledge();
    }

    public ReligiousMembership joinOrder(
            NpcId npc,
            String orderId,
            String roleId
    ) {

        if (!canJoinOrder(
                npc,
                orderId
        )) {

            throw new IllegalStateException(
                    "NPC does not meet the requirements for "
                            + orderId
            );
        }

        ReligiousOrderDefinition definition =
                ReligiousInstitutionCatalog.get()
                        .find(
                                orderId
                        )
                        .orElseThrow();

        if (!roleId.isBlank()
                && !definition.roles()
                .contains(
                        roleId
                )) {

            throw new IllegalArgumentException(
                    "Role "
                            + roleId
                            + " is not valid for "
                            + orderId
            );
        }

        Organization organization =
                institutions.ensureOrganization(
                        orderId
                );

        ReligiousMembership membership =
                memberships.join(
                        npc,
                        orderId,
                        organization.id(),
                        roleId,
                        0.50
                );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        if (!roleId.isBlank()) {

            profile.addOccupation(
                    roleId
            );
        }

        return membership;
    }

    public void leaveOrder(
            NpcId npc
    ) {

        memberships.leave(
                npc
        );
    }

    public void performRitual(
            NpcId npc,
            String ritualId
    ) {

        String normalized =
                normalize(
                        ritualId
                );

        validateKnownRitualIfMember(
                npc,
                normalized
        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        profile.setValue(
                RITUAL_PREFIX
                        + normalized,
                1.0
        );

        religion.setDevotion(
                npc,
                moveToward(
                        religion.snapshot(
                                npc
                        ).devotion(),
                        1.0,
                        0.05
                )
        );

        religion.setObservance(
                npc,
                moveToward(
                        religion.snapshot(
                                npc
                        ).observance(),
                        1.0,
                        0.08
                )
        );
    }

    public void violateTaboo(
            NpcId npc,
            String tabooId
    ) {

        String normalized =
                normalize(
                        tabooId
                );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npc
                );

        profile.setValue(
                TABOO_PREFIX
                        + normalized,
                1.0
        );

        religion.setObservance(
                npc,
                moveToward(
                        religion.snapshot(
                                npc
                        ).observance(),
                        0.0,
                        0.08
                )
        );
    }

    private void validateKnownRitualIfMember(
            NpcId npc,
            String ritualId
    ) {

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

            return;
        }

        ReligiousOrderDefinition definition =
                ReligiousInstitutionCatalog.get()
                        .find(
                                membership.orderId()
                        )
                        .orElseThrow();

        if (!definition.rituals()
                .isEmpty()
                && !definition.rituals()
                .contains(
                        ritualId
                )) {

            throw new IllegalArgumentException(
                    ritualId
                            + " is not a defined ritual for "
                            + definition.id()
            );
        }
    }

    private static double moveToward(
            double current,
            double target,
            double strength
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        current
                                + (
                                target - current
                        )
                                * strength
                )
        );
    }

    private static String normalize(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "identifier cannot be blank"
            );
        }

        return value.trim()
                .toLowerCase();
    }
}
package dev.dtzudontsay.knownworld.simulation.social.society;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembership;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipManager;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class NonDynasticSocietyService {

    private final NonDynasticSocietyManager societies;

    private final OrganizationMembershipManager memberships;

    public NonDynasticSocietyService(
            NonDynasticSocietyManager societies,
            OrganizationMembershipManager memberships
    ) {

        this.societies =
                Objects.requireNonNull(
                        societies,
                        "societies"
                );

        this.memberships =
                Objects.requireNonNull(
                        memberships,
                        "memberships"
                );
    }

    public OrganizationMembership join(
            NpcId npc,
            OrganizationId society,
            long tick,
            double loyalty
    ) {

        societies.require(
                society
        );

        return memberships.join(
                npc,
                society,
                tick,
                loyalty
        );
    }

    public void assignRole(
            NpcId npc,
            OrganizationId society,
            String role,
            long tick,
            double loyalty
    ) {

        join(
                npc,
                society,
                tick,
                loyalty
        );

        memberships.addRole(
                npc,
                society,
                role
        );
    }

    public KhalasarSuccessionSnapshot khalasarSuccessionSnapshot(
            OrganizationId organizationId
    ) {

        NonDynasticSociety society =
                societies.require(
                        organizationId
                );

        if (society.type()
                != NonDynasticSocietyType.KHALASAR) {

            throw new IllegalArgumentException(
                    "Organization "
                            + organizationId
                            + " is not a khalasar"
            );
        }

        List<NpcId> khalakkas =
                membersWithRole(
                        organizationId,
                        "khalakka"
                );

        List<NpcId> kos =
                membersWithRole(
                        organizationId,
                        "ko"
                );

        List<NpcId> bloodriders =
                membersWithRole(
                        organizationId,
                        "bloodrider"
                );

        List<NpcId> khaleesis =
                membersWithRole(
                        organizationId,
                        "khaleesi"
                );

        NpcId khal =
                society.primaryLeader();

        if (khal == null) {

            List<NpcId> khals =
                    membersWithRole(
                            organizationId,
                            "khal"
                    );

            if (!khals.isEmpty()) {

                khal =
                        khals.getFirst();
            }
        }

        return new KhalasarSuccessionSnapshot(
                organizationId,
                khal,
                khalakkas,
                kos,
                bloodriders,
                khaleesis
        );
    }

    /**
     * Runtime hook used later by death/succession simulation.
     *
     * This method does not itself choose who deserves the new khalasar.
     * The caller supplies the leader and followers after political/
     * personality simulation has made that decision.
     */
    public NonDynasticSociety splinterKhalasar(
            OrganizationId parentOrganization,
            String newName,
            NpcId newKhal,
            Collection<NpcId> followers,
            long tick
    ) {

        NonDynasticSociety parent =
                societies.require(
                        parentOrganization
                );

        if (parent.type()
                != NonDynasticSocietyType.KHALASAR) {

            throw new IllegalArgumentException(
                    "Can only splinter a KHALASAR"
            );
        }

        NonDynasticSociety splinter =
                societies.createGenerated(
                        newName,
                        NonDynasticSocietyType.KHALASAR,
                        parent.homeLocationId(),
                        parent.cultureId(),
                        parent.religionId(),
                        true,
                        -1,
                        Math.max(
                                0.10,
                                parent.militaryStrength()
                                        * 0.35
                        ),
                        Math.max(
                                0.25,
                                parent.cohesion()
                                        * 0.75
                        )
                );

        List<NpcId> transferred =
                new ArrayList<>();

        if (followers != null) {

            transferred.addAll(
                    followers
            );
        }

        if (!transferred.contains(
                newKhal
        )) {

            transferred.add(
                    newKhal
            );
        }

        for (
                NpcId npc :
                transferred
        ) {

            OrganizationMembership oldMembership =
                    memberships.find(
                                    npc,
                                    parentOrganization
                            )
                            .orElse(
                                    null
                            );

            List<String> oldRoles =
                    oldMembership == null
                            ? List.of()
                            : List.copyOf(
                            oldMembership.roles()
                    );

            double loyalty =
                    oldMembership == null
                            ? 0.65
                            : oldMembership.loyalty();

            if (oldMembership != null
                    && oldMembership.isActive()) {

                memberships.leave(
                        npc,
                        parentOrganization,
                        tick,
                        OrganizationMembershipStatus.RESIGNED
                );
            }

            OrganizationMembership newMembership =
                    memberships.join(
                            npc,
                            splinter.organizationId(),
                            tick,
                            loyalty
                    );

            for (
                    String role :
                    oldRoles
            ) {

                /*
                 * Khal/khaleesi/khalakka status is not blindly copied.
                 * Those roles depend on the new host's actual political
                 * and family situation.
                 */
                if ("khal".equals(
                        role
                )
                        || "khaleesi".equals(
                        role
                )
                        || "khalakka".equals(
                        role
                )) {

                    continue;
                }

                newMembership.addRole(
                        role
                );
            }
        }

        OrganizationMembership leaderMembership =
                memberships.find(
                                newKhal,
                                splinter.organizationId()
                        )
                        .orElseThrow();

        leaderMembership.removeRole(
                "ko"
        );

        leaderMembership.addRole(
                "khal"
        );

        societies.setPrimaryLeader(
                splinter.organizationId(),
                newKhal
        );

        return splinter;
    }

    public void markSplintered(
            OrganizationId society
    ) {

        societies.require(
                        society
                )
                .setStatus(
                        NonDynasticSocietyStatus.SPLINTERED
                );
    }

    public void dissolve(
            OrganizationId society,
            long tick
    ) {

        NonDynasticSociety target =
                societies.require(
                        society
                );

        for (
                OrganizationMembership membership :
                memberships.activeMembersOf(
                        society
                )
        ) {

            memberships.leave(
                    membership.npc(),
                    society,
                    tick,
                    OrganizationMembershipStatus.DISSOLVED
            );
        }

        target.setStatus(
                NonDynasticSocietyStatus.DISSOLVED
        );
    }

    private List<NpcId> membersWithRole(
            OrganizationId society,
            String role
    ) {

        return memberships.activeMembersOf(
                        society
                )
                .stream()
                .filter(
                        membership ->
                                membership.roles()
                                        .contains(
                                                role
                                        )
                )
                .map(
                        OrganizationMembership::npc
                )
                .toList();
    }
}
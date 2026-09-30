package dev.dtzudontsay.knownworld.simulation.social.society;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class NonDynasticSocietyManager {

    private final OrganizationManager organizations;

    private final NpcRegistry npcs;

    private final Map<OrganizationId, NonDynasticSociety> societies =
            new LinkedHashMap<>();

    private final Map<String, OrganizationId> authored =
            new LinkedHashMap<>();

    public NonDynasticSocietyManager(
            OrganizationManager organizations,
            NpcRegistry npcs
    ) {

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );

        this.npcs =
                Objects.requireNonNull(
                        npcs,
                        "npcs"
                );
    }

    public synchronized NonDynasticSociety reconcileAuthored(
            String authoredId,
            String name,
            NonDynasticSocietyType type,
            NonDynasticSocietyStatus status,
            OrganizationId organizationId,
            String homeLocationId,
            String cultureId,
            String religionId,
            boolean mobile,
            int populationEstimate,
            double militaryStrength,
            double cohesion,
            ReferenceProvenance provenance,
            String sourceNote,
            String primaryLeaderRole,
            String secondaryLeaderRole
    ) {

        String key =
                normalize(
                        authoredId
                );

        validateOrganization(
                organizationId,
                type
        );

        OrganizationId previousId =
                authored.get(
                        key
                );

        NonDynasticSociety previous =
                previousId == null
                        ? null
                        : societies.get(
                        previousId
                );

        NonDynasticSociety replacement =
                new NonDynasticSociety(
                        organizationId,
                        key,
                        name,
                        type,
                        previous == null
                                ? status
                                : compatibleStatus(
                                previous.status(),
                                status
                        ),
                        homeLocationId,
                        cultureId,
                        religionId,
                        mobile,
                        previous != null
                                && previous.hasPopulationEstimate()
                                ? previous.populationEstimate()
                                : populationEstimate,
                        previous == null
                                ? militaryStrength
                                : previous.militaryStrength(),
                        previous == null
                                ? cohesion
                                : previous.cohesion(),
                        provenance,
                        sourceNote,
                        primaryLeaderRole,
                        secondaryLeaderRole,
                        previous == null
                                ? null
                                : previous.primaryLeader(),
                        previous == null
                                ? null
                                : previous.secondaryLeader()
                );

        if (previousId != null
                && !previousId.equals(
                organizationId
        )) {

            societies.remove(
                    previousId
            );
        }

        societies.put(
                organizationId,
                replacement
        );

        authored.put(
                key,
                organizationId
        );

        return replacement;
    }

    public synchronized NonDynasticSociety createGenerated(
            String name,
            NonDynasticSocietyType type,
            String homeLocationId,
            String cultureId,
            String religionId,
            boolean mobile,
            int populationEstimate,
            double militaryStrength,
            double cohesion
    ) {

        Organization organization =
                organizations.create(
                        name,
                        type.organizationType(),
                        null
                );

        NonDynasticSociety society =
                new NonDynasticSociety(
                        organization.id(),
                        null,
                        name,
                        type,
                        NonDynasticSocietyStatus.ACTIVE,
                        homeLocationId,
                        cultureId,
                        religionId,
                        mobile,
                        populationEstimate,
                        militaryStrength,
                        cohesion,
                        ReferenceProvenance.PLAUSIBLE,
                        "Dynamically generated non-dynastic society.",
                        defaultPrimaryLeaderRole(
                                type
                        ),
                        defaultSecondaryLeaderRole(
                                type
                        ),
                        null,
                        null
                );

        societies.put(
                society.organizationId(),
                society
        );

        return society;
    }

    public synchronized void registerLoaded(
            NonDynasticSociety society
    ) {

        Objects.requireNonNull(
                society,
                "society"
        );

        validateOrganization(
                society.organizationId(),
                society.type()
        );

        validateNpcIfPresent(
                society.primaryLeader()
        );

        validateNpcIfPresent(
                society.secondaryLeader()
        );

        if (societies.putIfAbsent(
                society.organizationId(),
                society
        ) != null) {

            throw new IllegalStateException(
                    "Duplicate non-dynastic society organization "
                            + society.organizationId()
            );
        }

        if (society.hasAuthoredId()) {

            OrganizationId existing =
                    authored.putIfAbsent(
                            society.authoredId(),
                            society.organizationId()
                    );

            if (existing != null) {

                societies.remove(
                        society.organizationId()
                );

                throw new IllegalStateException(
                        "Duplicate authored society "
                                + society.authoredId()
                );
            }
        }
    }

    public synchronized int removeAuthoredNotIn(
            Set<String> retainedIds
    ) {

        List<String> stale =
                new ArrayList<>();

        for (
                String id :
                authored.keySet()
        ) {

            if (!retainedIds.contains(
                    id
            )) {

                stale.add(
                        id
                );
            }
        }

        for (
                String id :
                stale
        ) {

            OrganizationId organizationId =
                    authored.remove(
                            id
                    );

            if (organizationId != null) {

                societies.remove(
                        organizationId
                );
            }
        }

        return stale.size();
    }

    public synchronized Optional<NonDynasticSociety> find(
            OrganizationId organizationId
    ) {

        return Optional.ofNullable(
                societies.get(
                        organizationId
                )
        );
    }

    public synchronized Optional<NonDynasticSociety> findAuthored(
            String authoredId
    ) {

        if (authoredId == null
                || authoredId.isBlank()) {

            return Optional.empty();
        }

        OrganizationId organizationId =
                authored.get(
                        normalize(
                                authoredId
                        )
                );

        return organizationId == null
                ? Optional.empty()
                : find(
                organizationId
        );
    }

    public synchronized void setPrimaryLeader(
            OrganizationId organizationId,
            NpcId npc
    ) {

        validateNpcIfPresent(
                npc
        );

        require(
                organizationId
        )
                .setPrimaryLeader(
                        npc
                );
    }

    public synchronized void setSecondaryLeader(
            OrganizationId organizationId,
            NpcId npc
    ) {

        validateNpcIfPresent(
                npc
        );

        require(
                organizationId
        )
                .setSecondaryLeader(
                        npc
                );
    }

    public synchronized NonDynasticSociety require(
            OrganizationId organizationId
    ) {

        return find(
                organizationId
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown non-dynastic society "
                                                + organizationId
                                )
                );
    }

    public synchronized Collection<NonDynasticSociety> all() {

        return List.copyOf(
                societies.values()
        );
    }

    public synchronized int size() {

        return societies.size();
    }

    private void validateOrganization(
            OrganizationId organizationId,
            NonDynasticSocietyType type
    ) {

        Organization organization =
                organizations.find(
                                organizationId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown organization "
                                                        + organizationId
                                        )
                        );

        if (organization.type()
                != type.organizationType()) {

            throw new IllegalArgumentException(
                    "Organization "
                            + organizationId
                            + " is "
                            + organization.type()
                            + " but society type "
                            + type
                            + " requires "
                            + type.organizationType()
            );
        }
    }

    private void validateNpcIfPresent(
            NpcId npc
    ) {

        if (npc != null
                && !npcs.contains(
                npc
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC "
                            + npc
            );
        }
    }

    private static NonDynasticSocietyStatus compatibleStatus(
            NonDynasticSocietyStatus runtime,
            NonDynasticSocietyStatus catalog
    ) {

        if (runtime == NonDynasticSocietyStatus.DISSOLVED
                || runtime == NonDynasticSocietyStatus.SPLINTERED) {

            return runtime;
        }

        return catalog;
    }

    private static String defaultPrimaryLeaderRole(
            NonDynasticSocietyType type
    ) {

        return switch (type) {

            case KHALASAR ->
                    "khal";

            case JOGOS_NHAI_BAND ->
                    "jhat";

            case FREE_FOLK_CLAN ->
                    "clan_chieftain";

            case FREE_FOLK_CONFEDERATION ->
                    "king_beyond_the_wall";

            default ->
                    null;
        };
    }

    private static String defaultSecondaryLeaderRole(
            NonDynasticSocietyType type
    ) {

        return type == NonDynasticSocietyType.JOGOS_NHAI_BAND
                ? "moonsinger"
                : null;
    }

    private static String normalize(
            String value
    ) {

        Objects.requireNonNull(
                value,
                "value"
        );

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!normalized.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid authored society ID "
                            + value
            );
        }

        return normalized;
    }
}
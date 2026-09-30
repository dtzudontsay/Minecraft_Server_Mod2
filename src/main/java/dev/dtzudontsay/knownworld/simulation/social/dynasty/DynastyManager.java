package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DynastyManager {

    private final OrganizationManager organizations;

    private final NpcRegistry npcs;

    private final Map<DynastyId, Dynasty> dynasties =
            new LinkedHashMap<>();

    private final Map<String, DynastyId> authored =
            new LinkedHashMap<>();

    private final Map<OrganizationId, DynastyId> byOrganization =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public DynastyManager(
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

    public synchronized Dynasty ensureAuthored(
            String authoredId,
            String name,
            DynastyType type,
            DynastyStatus status,
            OrganizationId organizationId,
            String homeLocationId,
            String cultureId,
            String religionId,
            Integer foundedYear,
            Integer extinctYear,
            boolean activeAtScenarioStart,
            Set<DynastyContinuity> continuities,
            String words,
            String heraldry,
            double prestige,
            double wealth,
            double militaryStrength,
            ReferenceProvenance provenance,
            String sourceNote
    ) {

        String key =
                normalize(
                        authoredId
                );

        DynastyId existingId =
                authored.get(
                        key
                );

        if (existingId != null) {

            Dynasty existing =
                    require(
                            existingId
                    );

            if (!Objects.equals(
                    existing.organizationId(),
                    organizationId
            )) {

                throw new IllegalStateException(
                        "Authored dynasty "
                                + key
                                + " has conflicting organization binding"
                );
            }

            return existing;
        }

        validateOrganizationIfPresent(
                organizationId,
                type
        );

        Dynasty dynasty =
                new Dynasty(
                        allocateId(),
                        key,
                        name,
                        type,
                        status,
                        organizationId,
                        homeLocationId,
                        cultureId,
                        religionId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        foundedYear,
                        extinctYear,
                        activeAtScenarioStart,
                        continuities,
                        words,
                        heraldry,
                        prestige,
                        wealth,
                        militaryStrength,
                        provenance,
                        sourceNote
                );

        registerInternal(
                dynasty
        );

        return dynasty;
    }

    public synchronized Dynasty createGenerated(
            String name,
            DynastyType type,
            OrganizationId organizationId,
            String homeLocationId,
            String cultureId,
            String religionId
    ) {

        Objects.requireNonNull(
                organizationId,
                "Generated active dynasty requires organizationId"
        );

        validateOrganizationIfPresent(
                organizationId,
                type
        );

        Dynasty dynasty =
                new Dynasty(
                        allocateId(),
                        null,
                        name,
                        type,
                        DynastyStatus.ACTIVE,
                        organizationId,
                        homeLocationId,
                        cultureId,
                        religionId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        Set.of(
                                DynastyContinuity.BOOK
                        ),
                        "",
                        "",
                        0.20,
                        0.20,
                        0.20,
                        ReferenceProvenance.PLAUSIBLE,
                        "Dynamically generated lineage."
                );

        registerInternal(
                dynasty
        );

        return dynasty;
    }

    public synchronized void registerLoaded(
            Dynasty dynasty
    ) {

        Objects.requireNonNull(
                dynasty,
                "dynasty"
        );

        validateOrganizationIfPresent(
                dynasty.organizationId(),
                dynasty.type()
        );

        validateNpcIfPresent(
                dynasty.head()
        );

        validateNpcIfPresent(
                dynasty.heir()
        );

        registerInternal(
                dynasty
        );

        if (dynasty.id()
                .value()
                < Long.MAX_VALUE) {

            nextId =
                    Math.max(
                            nextId,
                            dynasty.id()
                                    .value()
                                    + 1
                    );
        }
    }

    public synchronized Optional<Dynasty> find(
            DynastyId id
    ) {

        return Optional.ofNullable(
                dynasties.get(
                        id
                )
        );
    }

    public synchronized Dynasty require(
            DynastyId id
    ) {

        return find(
                id
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown dynasty "
                                                + id
                                )
                );
    }

    public synchronized Optional<Dynasty> findAuthored(
            String authoredId
    ) {

        if (authoredId == null
                || authoredId.isBlank()) {

            return Optional.empty();
        }

        DynastyId id =
                authored.get(
                        normalize(
                                authoredId
                        )
                );

        return id == null
                ? Optional.empty()
                : find(
                id
        );
    }

    public synchronized Optional<Dynasty> findByOrganization(
            OrganizationId organizationId
    ) {

        if (organizationId == null) {
            return Optional.empty();
        }

        DynastyId id =
                byOrganization.get(
                        organizationId
                );

        return id == null
                ? Optional.empty()
                : find(
                id
        );
    }

    public synchronized void setParent(
            DynastyId dynasty,
            DynastyId parent
    ) {

        require(
                dynasty
        )
                .setParentDynasty(
                        parent == null
                                ? null
                                : require(
                                parent
                        ).id()
                );
    }

    public synchronized void setLiege(
            DynastyId dynasty,
            DynastyId liege
    ) {

        require(
                dynasty
        )
                .setLiegeDynasty(
                        liege == null
                                ? null
                                : require(
                                liege
                        ).id()
                );
    }

    public synchronized void setPredecessor(
            DynastyId dynasty,
            DynastyId predecessor
    ) {

        require(
                dynasty
        )
                .setPredecessorDynasty(
                        predecessor == null
                                ? null
                                : require(
                                predecessor
                        ).id()
                );
    }

    public synchronized void setSuccessor(
            DynastyId dynasty,
            DynastyId successor
    ) {

        require(
                dynasty
        )
                .setSuccessorDynasty(
                        successor == null
                                ? null
                                : require(
                                successor
                        ).id()
                );
    }

    public synchronized void setHead(
            DynastyId dynasty,
            NpcId head
    ) {

        validateNpcIfPresent(
                head
        );

        require(
                dynasty
        )
                .setHead(
                        head
                );
    }

    public synchronized void setHeir(
            DynastyId dynasty,
            NpcId heir
    ) {

        validateNpcIfPresent(
                heir
        );

        require(
                dynasty
        )
                .setHeir(
                        heir
                );
    }

    public synchronized Collection<Dynasty> all() {

        return List.copyOf(
                dynasties.values()
        );
    }

    public synchronized int size() {

        return dynasties.size();
    }

    private void registerInternal(
            Dynasty dynasty
    ) {

        if (dynasties.putIfAbsent(
                dynasty.id(),
                dynasty
        ) != null) {

            throw new IllegalStateException(
                    "Duplicate dynasty ID "
                            + dynasty.id()
            );
        }

        if (dynasty.hasOrganization()) {

            DynastyId existingOrganization =
                    byOrganization.putIfAbsent(
                            dynasty.organizationId(),
                            dynasty.id()
                    );

            if (existingOrganization != null) {

                dynasties.remove(
                        dynasty.id()
                );

                throw new IllegalStateException(
                        "Organization "
                                + dynasty.organizationId()
                                + " already belongs to dynasty "
                                + existingOrganization
                );
            }
        }

        if (dynasty.hasAuthoredId()) {

            DynastyId existingAuthored =
                    authored.putIfAbsent(
                            dynasty.authoredId(),
                            dynasty.id()
                    );

            if (existingAuthored != null) {

                dynasties.remove(
                        dynasty.id()
                );

                if (dynasty.hasOrganization()) {

                    byOrganization.remove(
                            dynasty.organizationId()
                    );
                }

                throw new IllegalStateException(
                        "Duplicate authored dynasty ID "
                                + dynasty.authoredId()
                );
            }
        }
    }

    private void validateOrganizationIfPresent(
            OrganizationId organizationId,
            DynastyType type
    ) {

        if (organizationId == null) {
            return;
        }

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
                            + ", but dynasty type "
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

    private DynastyId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Dynasty ID space exhausted"
            );
        }

        DynastyId id =
                new DynastyId(
                        nextId
                );

        nextId++;

        return id;
    }

    private static String normalize(
            String value
    ) {

        Objects.requireNonNull(
                value,
                "value"
        );

        String result =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!result.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid authored dynasty ID "
                            + value
            );
        }

        return result;
    }
}
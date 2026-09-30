package dev.dtzudontsay.knownworld.simulation.social.holding;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class LandedHoldingManager {

    private final NpcRegistry npcs;

    private final OrganizationManager organizations;

    private final TitleManager titles;

    private final DynastyManager dynasties;

    private final WorldReferenceCatalog references;

    private final Map<HoldingId, LandedHolding> holdings =
            new LinkedHashMap<>();

    private final Map<String, HoldingId> authored =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public LandedHoldingManager(
            NpcRegistry npcs,
            OrganizationManager organizations,
            TitleManager titles,
            DynastyManager dynasties,
            WorldReferenceCatalog references
    ) {

        this.npcs =
                Objects.requireNonNull(
                        npcs,
                        "npcs"
                );

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );

        this.titles =
                Objects.requireNonNull(
                        titles,
                        "titles"
                );

        this.dynasties =
                Objects.requireNonNull(
                        dynasties,
                        "dynasties"
                );

        this.references =
                Objects.requireNonNull(
                        references,
                        "references"
                );
    }

    public synchronized LandedHolding createAuthored(
            String authoredId,
            String name,
            HoldingType type,
            String worldLocationId,
            DynastyId deJureDynastyId,
            HoldingStatus status,
            DynastyId ownerDynastyId,
            NpcId holderNpcId,
            OrganizationId governmentOrganizationId,
            TitleId linkedTitleId,
            boolean capital,
            double taxBase,
            double militaryValue,
            double populationWeight
    ) {

        String key =
                normalize(
                        authoredId
                );

        if (authored.containsKey(
                key
        )) {

            throw new IllegalStateException(
                    "Authored holding already exists: "
                            + key
            );
        }

        validateReferences(
                worldLocationId,
                deJureDynastyId,
                null,
                ownerDynastyId,
                holderNpcId,
                governmentOrganizationId,
                linkedTitleId
        );

        LandedHolding holding =
                new LandedHolding(
                        allocateId(),
                        key,
                        name,
                        type,
                        worldLocationId,
                        deJureDynastyId,
                        null,
                        status,
                        ownerDynastyId,
                        holderNpcId,
                        governmentOrganizationId,
                        linkedTitleId,
                        capital,
                        taxBase,
                        militaryValue,
                        populationWeight
                );

        registerInternal(
                holding
        );

        return holding;
    }

    /**
     * Refreshes static authored metadata while retaining runtime ownership.
     *
     * This is important once conquests and succession exist:
     * updating a data pack must not magically give conquered land back
     * to its scenario-start owner.
     */
    public synchronized LandedHolding reconcileAuthored(
            String authoredId,
            String name,
            HoldingType type,
            String worldLocationId,
            DynastyId deJureDynastyId,
            HoldingStatus initialStatus,
            DynastyId initialOwnerDynastyId,
            NpcId initialHolderNpcId,
            OrganizationId initialGovernmentOrganizationId,
            TitleId initialLinkedTitleId,
            boolean capital,
            double taxBase,
            double militaryValue,
            double populationWeight
    ) {

        String key =
                normalize(
                        authoredId
                );

        HoldingId existingId =
                authored.get(
                        key
                );

        if (existingId == null) {

            return createAuthored(
                    key,
                    name,
                    type,
                    worldLocationId,
                    deJureDynastyId,
                    initialStatus,
                    initialOwnerDynastyId,
                    initialHolderNpcId,
                    initialGovernmentOrganizationId,
                    initialLinkedTitleId,
                    capital,
                    taxBase,
                    militaryValue,
                    populationWeight
            );
        }

        LandedHolding previous =
                require(
                        existingId
                );

        validateReferences(
                worldLocationId,
                deJureDynastyId,
                previous.parentHoldingId(),
                previous.ownerDynastyId(),
                previous.holderNpcId(),
                previous.governmentOrganizationId(),
                previous.linkedTitleId()
        );

        LandedHolding replacement =
                new LandedHolding(
                        previous.id(),
                        key,
                        name,
                        type,
                        worldLocationId,
                        deJureDynastyId,
                        previous.parentHoldingId(),
                        previous.status(),
                        previous.ownerDynastyId(),
                        previous.holderNpcId(),
                        previous.governmentOrganizationId(),
                        previous.linkedTitleId(),
                        capital,
                        taxBase,
                        militaryValue,
                        populationWeight
                );

        holdings.put(
                replacement.id(),
                replacement
        );

        return replacement;
    }

    public synchronized void registerLoaded(
            LandedHolding holding
    ) {

        Objects.requireNonNull(
                holding,
                "holding"
        );

        validateReferences(
                holding.worldLocationId(),
                holding.deJureDynastyId(),
                holding.parentHoldingId(),
                holding.ownerDynastyId(),
                holding.holderNpcId(),
                holding.governmentOrganizationId(),
                holding.linkedTitleId()
        );

        if (holdings.containsKey(
                holding.id()
        )) {

            throw new IllegalStateException(
                    "Duplicate holding ID "
                            + holding.id()
            );
        }

        if (holding.authoredId() != null) {

            HoldingId existing =
                    authored.putIfAbsent(
                            normalize(
                                    holding.authoredId()
                            ),
                            holding.id()
                    );

            if (existing != null) {

                throw new IllegalStateException(
                        "Duplicate authored holding ID "
                                + holding.authoredId()
                );
            }
        }

        holdings.put(
                holding.id(),
                holding
        );

        if (holding.id()
                .value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            holding.id()
                                    .value()
                                    + 1
                    );
        }
    }

    public synchronized Optional<LandedHolding> find(
            HoldingId id
    ) {

        return Optional.ofNullable(
                holdings.get(
                        id
                )
        );
    }

    public synchronized LandedHolding require(
            HoldingId id
    ) {

        return find(
                id
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown holding "
                                                + id
                                )
                );
    }

    public synchronized Optional<LandedHolding> findAuthored(
            String authoredId
    ) {

        if (authoredId == null
                || authoredId.isBlank()) {

            return Optional.empty();
        }

        HoldingId id =
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

    public synchronized LandedHolding requireAuthored(
            String authoredId
    ) {

        return findAuthored(
                authoredId
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown authored holding "
                                                + authoredId
                                )
                );
    }

    public synchronized void setParent(
            HoldingId holding,
            HoldingId parent
    ) {

        LandedHolding target =
                require(
                        holding
                );

        if (parent != null) {

            require(
                    parent
            );

            if (holding.equals(
                    parent
            )) {

                throw new IllegalArgumentException(
                        "Holding cannot be its own parent"
                );
            }

            ensureNoCycle(
                    holding,
                    parent
            );
        }

        target.setParentHoldingId(
                parent
        );
    }

    public synchronized void transferOwnership(
            HoldingId holding,
            DynastyId newOwner
    ) {

        if (newOwner != null) {

            dynasties.require(
                    newOwner
            );
        }

        require(
                holding
        )
                .setOwnerDynastyId(
                        newOwner
                );
    }

    public synchronized void setHolder(
            HoldingId holding,
            NpcId holder
    ) {

        validateNpcIfPresent(
                holder
        );

        require(
                holding
        )
                .setHolderNpcId(
                        holder
                );
    }

    public synchronized void setGovernment(
            HoldingId holding,
            OrganizationId organization
    ) {

        validateOrganizationIfPresent(
                organization
        );

        require(
                holding
        )
                .setGovernmentOrganizationId(
                        organization
                );
    }

    public synchronized void setLinkedTitle(
            HoldingId holding,
            TitleId title
    ) {

        validateTitleIfPresent(
                title
        );

        require(
                holding
        )
                .setLinkedTitleId(
                        title
                );
    }

    public synchronized List<LandedHolding> childrenOf(
            HoldingId parent
    ) {

        require(
                parent
        );

        return holdings.values()
                .stream()
                .filter(
                        holding ->
                                parent.equals(
                                        holding.parentHoldingId()
                                )
                )
                .sorted(
                        Comparator.comparing(
                                LandedHolding::id
                        )
                )
                .toList();
    }

    public synchronized List<LandedHolding> ownedBy(
            DynastyId dynasty
    ) {

        dynasties.require(
                dynasty
        );

        return holdings.values()
                .stream()
                .filter(
                        holding ->
                                dynasty.equals(
                                        holding.ownerDynastyId()
                                )
                )
                .sorted(
                        Comparator.comparing(
                                LandedHolding::id
                        )
                )
                .toList();
    }

    /**
     * Personal demesne.
     */
    public synchronized List<LandedHolding> heldBy(
            NpcId npc
    ) {

        validateNpcIfPresent(
                npc
        );

        return holdings.values()
                .stream()
                .filter(
                        holding ->
                                npc.equals(
                                        holding.holderNpcId()
                                )
                )
                .sorted(
                        Comparator.comparing(
                                LandedHolding::id
                        )
                )
                .toList();
    }

    public synchronized List<LandedHolding> atLocation(
            String worldLocationId
    ) {

        String normalized =
                normalize(
                        worldLocationId
                );

        return holdings.values()
                .stream()
                .filter(
                        holding ->
                                normalized.equals(
                                        holding.worldLocationId()
                                )
                )
                .sorted(
                        Comparator.comparing(
                                LandedHolding::id
                        )
                )
                .toList();
    }

    public synchronized Collection<LandedHolding> all() {

        return List.copyOf(
                holdings.values()
        );
    }

    public synchronized int size() {
        return holdings.size();
    }

    public synchronized int removeStaleAuthored(
            Set<String> validAuthoredIds
    ) {

        Set<String> normalizedValid =
                new LinkedHashSet<>();

        for (
                String value :
                validAuthoredIds
        ) {

            normalizedValid.add(
                    normalize(
                            value
                    )
            );
        }

        List<String> stale =
                authored.keySet()
                        .stream()
                        .filter(
                                id ->
                                        !normalizedValid.contains(
                                                id
                                        )
                        )
                        .toList();

        for (
                String authoredId :
                stale
        ) {

            HoldingId id =
                    authored.remove(
                            authoredId
                    );

            if (id == null) {
                continue;
            }

            holdings.remove(
                    id
            );

            for (
                    LandedHolding other :
                    holdings.values()
            ) {

                if (id.equals(
                        other.parentHoldingId()
                )) {

                    other.setParentHoldingId(
                            null
                    );
                }
            }
        }

        return stale.size();
    }

    private void registerInternal(
            LandedHolding holding
    ) {

        holdings.put(
                holding.id(),
                holding
        );

        if (holding.authoredId() != null) {

            authored.put(
                    normalize(
                            holding.authoredId()
                    ),
                    holding.id()
            );
        }
    }

    private void validateReferences(
            String worldLocationId,
            DynastyId deJureDynastyId,
            HoldingId parentHoldingId,
            DynastyId ownerDynastyId,
            NpcId holderNpcId,
            OrganizationId governmentOrganizationId,
            TitleId linkedTitleId
    ) {

        if (references.location(
                worldLocationId
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown world location "
                            + worldLocationId
            );
        }

        if (deJureDynastyId != null) {

            dynasties.require(
                    deJureDynastyId
            );
        }

        if (parentHoldingId != null
                && !holdings.containsKey(
                parentHoldingId
        )) {

            throw new IllegalArgumentException(
                    "Unknown parent holding "
                            + parentHoldingId
            );
        }

        if (ownerDynastyId != null) {

            dynasties.require(
                    ownerDynastyId
            );
        }

        validateNpcIfPresent(
                holderNpcId
        );

        validateOrganizationIfPresent(
                governmentOrganizationId
        );

        validateTitleIfPresent(
                linkedTitleId
        );
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

    private void validateOrganizationIfPresent(
            OrganizationId organization
    ) {

        if (organization != null
                && organizations.find(
                organization
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown organization "
                            + organization
            );
        }
    }

    private void validateTitleIfPresent(
            TitleId title
    ) {

        if (title != null
                && titles.find(
                title
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown title "
                            + title
            );
        }
    }

    private void ensureNoCycle(
            HoldingId child,
            HoldingId proposedParent
    ) {

        HoldingId current =
                proposedParent;

        int safety =
                0;

        while (current != null) {

            if (current.equals(
                    child
            )) {

                throw new IllegalArgumentException(
                        "Holding hierarchy cycle involving "
                                + child
                );
            }

            LandedHolding holding =
                    holdings.get(
                            current
                    );

            current =
                    holding == null
                            ? null
                            : holding.parentHoldingId();

            safety++;

            if (safety > holdings.size() + 1) {

                throw new IllegalStateException(
                        "Holding hierarchy appears cyclic"
                );
            }
        }
    }

    private HoldingId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Holding ID space exhausted"
            );
        }

        HoldingId result =
                new HoldingId(
                        nextId
                );

        nextId++;

        return result;
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

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "ID cannot be blank"
            );
        }

        return normalized;
    }
}
package dev.dtzudontsay.knownworld.simulation.social.succession;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ClaimManager {

    private final NpcRegistry registry;

    private final TitleManager titles;

    private final Map<ClaimId, ClaimRecord> claims =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public ClaimManager(
            NpcRegistry registry,
            TitleManager titles
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.titles =
                Objects.requireNonNull(
                        titles,
                        "titles"
                );
    }

    public synchronized ClaimRecord addClaim(
            NpcId claimant,
            TitleId title,
            ClaimStrength strength,
            NpcId inheritedFrom,
            long tick
    ) {
        validateNpc(
                claimant
        );

        validateNpcIfPresent(
                inheritedFrom
        );

        validateTitle(
                title
        );

        for (
                ClaimRecord existing :
                claims.values()
        ) {

            if (existing.active()
                    && existing.claimant()
                    .equals(
                            claimant
                    )
                    && existing.title()
                    .equals(
                            title
                    )) {

                return existing;
            }
        }

        ClaimRecord claim =
                new ClaimRecord(
                        allocateId(),
                        claimant,
                        title,
                        strength,
                        inheritedFrom,
                        tick,
                        true
                );

        claims.put(
                claim.id(),
                claim
        );

        return claim;
    }

    public synchronized void resolveClaimsOfHolder(
            NpcId holder,
            TitleId title
    ) {
        for (
                ClaimRecord claim :
                claims.values()
        ) {

            if (claim.active()
                    && claim.claimant()
                    .equals(
                            holder
                    )
                    && claim.title()
                    .equals(
                            title
                    )) {

                claim.deactivate();
            }
        }
    }

    public synchronized List<ClaimRecord> claimsOf(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return claims.values()
                .stream()
                .filter(
                        claim ->
                                claim.claimant()
                                        .equals(
                                                npc
                                        )
                )
                .toList();
    }

    public synchronized List<ClaimRecord> activeClaimsForTitle(
            TitleId title
    ) {
        validateTitle(
                title
        );

        return claims.values()
                .stream()
                .filter(
                        ClaimRecord::active
                )
                .filter(
                        claim ->
                                claim.title()
                                        .equals(
                                                title
                                        )
                )
                .toList();
    }

    public synchronized List<ClaimRecord> all() {
        return List.copyOf(
                claims.values()
        );
    }

    public synchronized int size() {
        return claims.size();
    }

    public synchronized void registerLoaded(
            ClaimRecord claim
    ) {
        Objects.requireNonNull(
                claim,
                "claim"
        );

        validateNpc(
                claim.claimant()
        );

        validateNpcIfPresent(
                claim.inheritedFrom()
        );

        validateTitle(
                claim.title()
        );

        if (claims.containsKey(
                claim.id()
        )) {

            throw new IllegalStateException(
                    "Duplicate claim ID: "
                            + claim.id()
            );
        }

        claims.put(
                claim.id(),
                claim
        );

        if (claim.id()
                .value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            claim.id()
                                    .value()
                                    + 1
                    );
        }
    }

    private void validateNpc(
            NpcId npc
    ) {
        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (!registry.contains(
                npc
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + npc
            );
        }
    }

    private void validateNpcIfPresent(
            NpcId npc
    ) {
        if (npc != null) {
            validateNpc(
                    npc
            );
        }
    }

    private void validateTitle(
            TitleId title
    ) {
        if (titles.find(
                title
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown title ID: "
                            + title
            );
        }
    }

    private ClaimId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Claim ID space exhausted"
            );
        }

        ClaimId id =
                new ClaimId(
                        nextId
                );

        nextId++;

        return id;
    }
}
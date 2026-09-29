package dev.dtzudontsay.knownworld.simulation.bootstrap;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps stable authored string IDs to runtime simulation IDs.
 *
 * Example:
 *
 * eddard_stark -> NPC #1
 * winterfell -> Settlement #1
 * house_stark -> Organization #2
 *
 * Authored IDs are what data files, dialogue systems and future LLM
 * context should use.
 *
 * Numeric IDs remain internal runtime/persistence identifiers.
 */
public final class AuthoredIdRegistry {

    private String scenarioId;

    private final Map<String, NpcId> npcs =
            new LinkedHashMap<>();

    private final Map<String, SettlementId> settlements =
            new LinkedHashMap<>();

    private final Map<String, OrganizationId> organizations =
            new LinkedHashMap<>();

    private final Map<String, TitleId> titles =
            new LinkedHashMap<>();

    public synchronized String scenarioId() {
        return scenarioId;
    }

    public synchronized void setScenarioId(
            String scenarioId
    ) {
        this.scenarioId =
                normalize(
                        scenarioId
                );
    }

    public synchronized void registerNpc(
            String authoredId,
            NpcId runtimeId
    ) {
        registerUnique(
                npcs,
                authoredId,
                runtimeId,
                "NPC"
        );
    }

    public synchronized void registerSettlement(
            String authoredId,
            SettlementId runtimeId
    ) {
        registerUnique(
                settlements,
                authoredId,
                runtimeId,
                "settlement"
        );
    }

    public synchronized void registerOrganization(
            String authoredId,
            OrganizationId runtimeId
    ) {
        registerUnique(
                organizations,
                authoredId,
                runtimeId,
                "organization"
        );
    }

    public synchronized void registerTitle(
            String authoredId,
            TitleId runtimeId
    ) {
        registerUnique(
                titles,
                authoredId,
                runtimeId,
                "title"
        );
    }

    public synchronized Optional<NpcId> findNpc(
            String authoredId
    ) {
        return Optional.ofNullable(
                npcs.get(
                        normalize(
                                authoredId
                        )
                )
        );
    }

    public synchronized Optional<SettlementId> findSettlement(
            String authoredId
    ) {
        return Optional.ofNullable(
                settlements.get(
                        normalize(
                                authoredId
                        )
                )
        );
    }

    public synchronized Optional<OrganizationId> findOrganization(
            String authoredId
    ) {
        return Optional.ofNullable(
                organizations.get(
                        normalize(
                                authoredId
                        )
                )
        );
    }

    public synchronized Optional<TitleId> findTitle(
            String authoredId
    ) {
        return Optional.ofNullable(
                titles.get(
                        normalize(
                                authoredId
                        )
                )
        );
    }

    public synchronized NpcId requireNpc(
            String authoredId
    ) {
        return findNpc(
                authoredId
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown authored NPC ID: "
                                                + authoredId
                                )
                );
    }

    public synchronized SettlementId requireSettlement(
            String authoredId
    ) {
        return findSettlement(
                authoredId
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown authored settlement ID: "
                                                + authoredId
                                )
                );
    }

    public synchronized OrganizationId requireOrganization(
            String authoredId
    ) {
        return findOrganization(
                authoredId
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown authored organization ID: "
                                                + authoredId
                                )
                );
    }

    public synchronized TitleId requireTitle(
            String authoredId
    ) {
        return findTitle(
                authoredId
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown authored title ID: "
                                                + authoredId
                                )
                );
    }

    public synchronized Map<String, NpcId> npcs() {
        return Map.copyOf(
                npcs
        );
    }

    public synchronized Map<String, SettlementId> settlements() {
        return Map.copyOf(
                settlements
        );
    }

    public synchronized Map<String, OrganizationId> organizations() {
        return Map.copyOf(
                organizations
        );
    }

    public synchronized Map<String, TitleId> titles() {
        return Map.copyOf(
                titles
        );
    }

    public synchronized boolean isEmpty() {
        return scenarioId == null
                && npcs.isEmpty()
                && settlements.isEmpty()
                && organizations.isEmpty()
                && titles.isEmpty();
    }

    private static <T> void registerUnique(
            Map<String, T> map,
            String authoredId,
            T runtimeId,
            String type
    ) {
        String key =
                normalize(
                        authoredId
                );

        Objects.requireNonNull(
                runtimeId,
                "runtimeId"
        );

        T existing =
                map.putIfAbsent(
                        key,
                        runtimeId
                );

        if (existing != null
                && !existing.equals(
                runtimeId
        )) {

            throw new IllegalStateException(
                    "Authored "
                            + type
                            + " ID already registered: "
                            + key
            );
        }
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
                        .toLowerCase();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Authored ID cannot be empty"
            );
        }

        if (!normalized.matches(
                "[a-z0-9_\\-.]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid authored ID: "
                            + value
                            + ". Use lowercase letters, digits, _, - or ."
            );
        }

        return normalized;
    }
}
package dev.dtzudontsay.knownworld.simulation.social.society;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipManager;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NonDynasticSocietyBootstrapService {

    private static final Gson GSON =
            new Gson();

    private static final String MANIFEST =
            "data/knownworld/scenarios/agot_298_ac/non_dynastic_societies.json";

    private NonDynasticSocietyBootstrapService() {
    }

    public static void ensureDefault(
            NpcSimulation simulation,
            NonDynasticSocietyManager societies,
            OrganizationMembershipManager memberships
    ) throws IOException {

        List<SocietyData> data =
                loadCatalog();

        Set<String> catalogIds =
                new LinkedHashSet<>();

        for (
                SocietyData entry :
                data
        ) {

            String id =
                    normalize(
                            entry.id
                    );

            if (!catalogIds.add(
                    id
            )) {

                throw new IllegalStateException(
                        "Duplicate non-dynastic society ID "
                                + id
                );
            }
        }

        int stale =
                societies.removeAuthoredNotIn(
                        catalogIds
                );

        int active =
                0;

        int resolvedLeaders =
                0;

        int unresolvedLeaders =
                0;

        /*
         * PASS 1:
         * organizations and society metadata.
         */
        for (
                SocietyData entry :
                data
        ) {

            NonDynasticSocietyType type =
                    enumValue(
                            NonDynasticSocietyType.class,
                            entry.type,
                            entry.id
                                    + ".type"
                    );

            NonDynasticSocietyStatus status =
                    enumValue(
                            NonDynasticSocietyStatus.class,
                            entry.status,
                            entry.id
                                    + ".status"
                    );

            validateReferences(
                    entry
            );

            OrganizationId organizationId =
                    ensureOrganization(
                            simulation,
                            entry.id,
                            entry.name,
                            type
                    );

            societies.reconcileAuthored(
                    entry.id,
                    entry.name,
                    type,
                    status,
                    organizationId,
                    entry.homeLocationId,
                    entry.cultureId,
                    entry.religionId,
                    entry.mobile,
                    entry.populationEstimate == null
                            ? -1
                            : entry.populationEstimate,
                    entry.militaryStrength == null
                            ? 0.40
                            : entry.militaryStrength,
                    entry.cohesion == null
                            ? 0.50
                            : entry.cohesion,
                    enumValue(
                            ReferenceProvenance.class,
                            entry.provenance,
                            entry.id
                                    + ".provenance"
                    ),
                    entry.sourceNote,
                    entry.primaryLeaderRole,
                    entry.secondaryLeaderRole
            );

            if (status == NonDynasticSocietyStatus.ACTIVE
                    || status == NonDynasticSocietyStatus.FORMING) {

                active++;
            }
        }

        /*
         * PASS 2:
         * resolve known leaders and explicit known memberships.
         *
         * Character references remain soft until those NPCs have been
         * authored.
         */
        for (
                SocietyData entry :
                data
        ) {

            NonDynasticSociety society =
                    societies.findAuthored(
                                    entry.id
                            )
                            .orElseThrow();

            societies.setPrimaryLeader(
                    society.organizationId(),
                    null
            );

            societies.setSecondaryLeader(
                    society.organizationId(),
                    null
            );

            if (hasText(
                    entry.primaryLeaderNpcId
            )) {

                var npc =
                        simulation.authoredIds()
                                .findNpc(
                                        entry.primaryLeaderNpcId
                                );

                if (npc.isPresent()) {

                    societies.setPrimaryLeader(
                            society.organizationId(),
                            npc.get()
                    );

                    joinWithRole(
                            memberships,
                            npc.get(),
                            society.organizationId(),
                            entry.primaryLeaderRole,
                            simulation.serverTickCounter(),
                            0.95
                    );

                    resolvedLeaders++;

                } else {

                    unresolvedLeaders++;
                }
            }

            if (hasText(
                    entry.secondaryLeaderNpcId
            )) {

                var npc =
                        simulation.authoredIds()
                                .findNpc(
                                        entry.secondaryLeaderNpcId
                                );

                if (npc.isPresent()) {

                    societies.setSecondaryLeader(
                            society.organizationId(),
                            npc.get()
                    );

                    joinWithRole(
                            memberships,
                            npc.get(),
                            society.organizationId(),
                            entry.secondaryLeaderRole,
                            simulation.serverTickCounter(),
                            0.90
                    );

                    resolvedLeaders++;

                } else {

                    unresolvedLeaders++;
                }
            }

            if (entry.members != null) {

                for (
                        MemberData member :
                        entry.members
                ) {

                    if (!hasText(
                            member.npcId
                    )) {

                        continue;
                    }

                    var npc =
                            simulation.authoredIds()
                                    .findNpc(
                                            member.npcId
                                    );

                    if (npc.isEmpty()) {
                        continue;
                    }

                    var membership =
                            memberships.join(
                                    npc.get(),
                                    society.organizationId(),
                                    simulation.serverTickCounter(),
                                    member.loyalty == null
                                            ? 0.75
                                            : member.loyalty
                            );

                    if (member.roles != null) {

                        for (
                                String role :
                                member.roles
                        ) {

                            membership.addRole(
                                    role
                            );
                        }
                    }
                }
            }
        }

        KnownWorld.LOGGER.info(
                "Non-dynastic society bootstrap ready: {} societies, {} operational, {} stale persisted records removed, leaders resolved={}, unresolved={}.",
                societies.size(),
                active,
                stale,
                resolvedLeaders,
                unresolvedLeaders
        );
    }

    private static void joinWithRole(
            OrganizationMembershipManager memberships,
            dev.dtzudontsay.knownworld.simulation.npc.NpcId npc,
            OrganizationId organization,
            String role,
            long tick,
            double loyalty
    ) {

        var membership =
                memberships.join(
                        npc,
                        organization,
                        tick,
                        loyalty
                );

        if (hasText(
                role
        )) {

            if (WorldReferenceCatalog.get()
                    .role(
                            role
                    )
                    .isEmpty()) {

                throw new IllegalStateException(
                        "Unknown society role "
                                + role
                );
            }

            membership.addRole(
                    role
            );
        }
    }

    private static OrganizationId ensureOrganization(
            NpcSimulation simulation,
            String authoredId,
            String name,
            NonDynasticSocietyType type
    ) {

        OrganizationId existingId =
                simulation.authoredIds()
                        .findOrganization(
                                authoredId
                        )
                        .orElse(
                                null
                        );

        if (existingId != null) {

            Organization organization =
                    simulation.organizations()
                            .find(
                                    existingId
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Missing authored organization "
                                                            + authoredId
                                            )
                            );

            if (organization.type()
                    != type.organizationType()) {

                throw new IllegalStateException(
                        authoredId
                                + " organization is "
                                + organization.type()
                                + " but society requires "
                                + type.organizationType()
                );
            }

            return existingId;
        }

        Organization organization =
                simulation.organizations()
                        .create(
                                name,
                                type.organizationType(),
                                null
                        );

        simulation.authoredIds()
                .registerOrganization(
                        authoredId,
                        organization.id()
                );

        return organization.id();
    }

    private static void validateReferences(
            SocietyData data
    ) {

        WorldReferenceCatalog references =
                WorldReferenceCatalog.get();

        if (hasText(
                data.homeLocationId
        )
                && references.location(
                data.homeLocationId
        ).isEmpty()) {

            throw new IllegalStateException(
                    data.id
                            + " references unknown location "
                            + data.homeLocationId
            );
        }

        if (hasText(
                data.cultureId
        )
                && references.culture(
                data.cultureId
        ).isEmpty()) {

            throw new IllegalStateException(
                    data.id
                            + " references unknown culture "
                            + data.cultureId
            );
        }

        if (hasText(
                data.religionId
        )
                && references.religion(
                data.religionId
        ).isEmpty()) {

            throw new IllegalStateException(
                    data.id
                            + " references unknown religion "
                            + data.religionId
            );
        }

        validateRoleIfPresent(
                data.primaryLeaderRole,
                data.id
                        + ".primaryLeaderRole"
        );

        validateRoleIfPresent(
                data.secondaryLeaderRole,
                data.id
                        + ".secondaryLeaderRole"
        );

        if (data.members != null) {

            for (
                    MemberData member :
                    data.members
            ) {

                if (member.roles == null) {
                    continue;
                }

                for (
                        String role :
                        member.roles
                ) {

                    validateRoleIfPresent(
                            role,
                            data.id
                                    + ".member.role"
                    );
                }
            }
        }
    }

    private static void validateRoleIfPresent(
            String role,
            String description
    ) {

        if (!hasText(
                role
        )) {

            return;
        }

        if (WorldReferenceCatalog.get()
                .role(
                        role
                )
                .isEmpty()) {

            throw new IllegalStateException(
                    description
                            + " references unknown role "
                            + role
            );
        }
    }

    private static List<SocietyData> loadCatalog()
            throws IOException {

        ManifestData manifest =
                readJson(
                        MANIFEST,
                        ManifestData.class
                );

        if (manifest.resources == null
                || manifest.resources.isEmpty()) {

            throw new IOException(
                    "Non-dynastic society manifest has no resources"
            );
        }

        List<SocietyData> result =
                new ArrayList<>();

        Map<String, String> sourceById =
                new LinkedHashMap<>();

        for (
                String resource :
                manifest.resources
        ) {

            JsonObject root =
                    readJson(
                            resource,
                            JsonObject.class
                    );

            JsonObject defaults =
                    root.has(
                            "defaults"
                    )
                            && root.get(
                            "defaults"
                    ).isJsonObject()
                            ? root.getAsJsonObject(
                            "defaults"
                    )
                            : new JsonObject();

            JsonArray societies =
                    root.getAsJsonArray(
                            "societies"
                    );

            if (societies == null) {

                throw new IOException(
                        resource
                                + " has no societies array"
                );
            }

            for (
                    JsonElement element :
                    societies
            ) {

                JsonObject merged =
                        merge(
                                defaults,
                                element.getAsJsonObject()
                        );

                SocietyData data =
                        GSON.fromJson(
                                merged,
                                SocietyData.class
                        );

                String normalized =
                        normalize(
                                data.id
                        );

                String previous =
                        sourceById.putIfAbsent(
                                normalized,
                                resource
                        );

                if (previous != null) {

                    throw new IOException(
                            "Duplicate society ID "
                                    + normalized
                                    + " in "
                                    + previous
                                    + " and "
                                    + resource
                    );
                }

                result.add(
                        data
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    private static JsonObject merge(
            JsonObject defaults,
            JsonObject entry
    ) {

        JsonObject result =
                defaults.deepCopy();

        for (
                Map.Entry<String, JsonElement> value :
                entry.entrySet()
        ) {

            result.add(
                    value.getKey(),
                    value.getValue()
                            .deepCopy()
            );
        }

        return result;
    }

    private static <T> T readJson(
            String path,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        NonDynasticSocietyBootstrapService.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        path
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Resource not found: "
                                + path
                );
            }

            try (
                    Reader reader =
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
            ) {

                T result =
                        GSON.fromJson(
                                reader,
                                type
                        );

                if (result == null) {

                    throw new IOException(
                            "Resource produced null: "
                                    + path
                    );
                }

                return result;
            }
        }
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value,
            String description
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be empty"
            );
        }

        try {

            return Enum.valueOf(
                    type,
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new IllegalArgumentException(
                    "Unknown "
                            + description
                            + ": "
                            + value,
                    exception
            );
        }
    }

    private static String normalize(
            String value
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalArgumentException(
                    "Society ID cannot be empty"
            );
        }

        return value.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static final class ManifestData {

        List<String> resources;
    }

    private static final class SocietyData {

        String id;

        String name;

        String type;

        String status;

        String homeLocationId;

        String cultureId;

        String religionId;

        boolean mobile;

        Integer populationEstimate;

        Double militaryStrength;

        Double cohesion;

        String provenance;

        String sourceNote;

        String primaryLeaderNpcId;

        String primaryLeaderRole;

        String secondaryLeaderNpcId;

        String secondaryLeaderRole;

        List<MemberData> members;
    }

    private static final class MemberData {

        String npcId;

        List<String> roles;

        Double loyalty;
    }
}
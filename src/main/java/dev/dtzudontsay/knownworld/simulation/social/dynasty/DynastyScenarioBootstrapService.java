package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
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

public final class DynastyScenarioBootstrapService {

    private static final Gson GSON =
            new Gson();

    private static final String MANIFEST_RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/dynasties.json";

    private DynastyScenarioBootstrapService() {
    }

    public static void ensureDefault(
            NpcSimulation simulation,
            DynastyManager manager
    ) throws IOException {

        List<DynastyData> data =
                loadCatalog();

        /*
         * Pass 1:
         * create reference dynasties and runtime organizations.
         */
        for (
                DynastyData entry :
                data
        ) {

            requireText(
                    entry.id,
                    "dynasty.id"
            );

            requireText(
                    entry.name,
                    entry.id
                            + ".name"
            );

            DynastyType type =
                    enumValue(
                            DynastyType.class,
                            entry.type,
                            entry.id
                                    + ".type"
                    );

            DynastyStatus status =
                    enumValue(
                            DynastyStatus.class,
                            entry.status,
                            entry.id
                                    + ".status"
                    );

            Set<DynastyContinuity> continuities =
                    parseContinuities(
                            entry
                    );

            validateReferences(
                    entry
            );

            OrganizationId organizationId =
                    null;

            if (entry.activeAtScenarioStart) {

                organizationId =
                        ensureOrganization(
                                simulation,
                                entry.id,
                                entry.name,
                                type
                        );
            }

            manager.ensureAuthored(
                    entry.id,
                    entry.name,
                    type,
                    status,
                    organizationId,
                    entry.homeLocationId,
                    entry.cultureId,
                    entry.religionId,
                    entry.foundedYear,
                    entry.extinctYear,
                    entry.activeAtScenarioStart,
                    continuities,
                    entry.words,
                    entry.heraldry,
                    metric(
                            entry.prestige,
                            type,
                            entry.activeAtScenarioStart,
                            Metric.PRESTIGE
                    ),
                    metric(
                            entry.wealth,
                            type,
                            entry.activeAtScenarioStart,
                            Metric.WEALTH
                    ),
                    metric(
                            entry.militaryStrength,
                            type,
                            entry.activeAtScenarioStart,
                            Metric.MILITARY
                    ),
                    enumValue(
                            ReferenceProvenance.class,
                            entry.provenance,
                            entry.id
                                    + ".provenance"
                    ),
                    entry.sourceNote
            );
        }

        /*
         * Pass 2:
         * resolve all structural dynasty relationships after the
         * complete catalog exists.
         */
        for (
                DynastyData entry :
                data
        ) {

            Dynasty dynasty =
                    requireAuthored(
                            manager,
                            entry.id
                    );

            if (hasText(
                    entry.parentDynastyId
            )) {

                manager.setParent(
                        dynasty.id(),
                        requireAuthored(
                                manager,
                                entry.parentDynastyId
                        ).id()
                );
            }

            if (hasText(
                    entry.liegeDynastyId
            )) {

                manager.setLiege(
                        dynasty.id(),
                        requireAuthored(
                                manager,
                                entry.liegeDynastyId
                        ).id()
                );
            }

            if (hasText(
                    entry.predecessorDynastyId
            )) {

                manager.setPredecessor(
                        dynasty.id(),
                        requireAuthored(
                                manager,
                                entry.predecessorDynastyId
                        ).id()
                );
            }

            if (hasText(
                    entry.successorDynastyId
            )) {

                manager.setSuccessor(
                        dynasty.id(),
                        requireAuthored(
                                manager,
                                entry.successorDynastyId
                        ).id()
                );
            }
        }

        /*
         * Pass 3:
         * NPC head/heir references remain deliberately soft while the
         * named population is still being authored.
         */
        for (
                DynastyData entry :
                data
        ) {

            Dynasty dynasty =
                    requireAuthored(
                            manager,
                            entry.id
                    );

            if (hasText(
                    entry.headNpcId
            )) {

                simulation.authoredIds()
                        .findNpc(
                                entry.headNpcId
                        )
                        .ifPresent(
                                npc ->
                                        manager.setHead(
                                                dynasty.id(),
                                                npc
                                        )
                        );
            }

            if (hasText(
                    entry.heirNpcId
            )) {

                simulation.authoredIds()
                        .findNpc(
                                entry.heirNpcId
                        )
                        .ifPresent(
                                npc ->
                                        manager.setHeir(
                                                dynasty.id(),
                                                npc
                                        )
                        );
            }
        }

        long active =
                manager.all()
                        .stream()
                        .filter(
                                Dynasty::activeAtScenarioStart
                        )
                        .count();

        KnownWorld.LOGGER.info(
                "Dynasty catalog ready with {} dynasties, {} active at scenario start.",
                manager.size(),
                active
        );
    }

    private static List<DynastyData> loadCatalog()
            throws IOException {

        CatalogManifest manifest =
                readJson(
                        MANIFEST_RESOURCE,
                        CatalogManifest.class
                );

        if (manifest.resources == null
                || manifest.resources.isEmpty()) {

            throw new IOException(
                    "Dynasty catalog manifest contains no resources"
            );
        }

        List<DynastyData> result =
                new ArrayList<>();

        Map<String, String> sourceById =
                new LinkedHashMap<>();

        for (
                String resource :
                manifest.resources
        ) {

            if (!hasText(
                    resource
            )) {

                throw new IOException(
                        "Dynasty catalog manifest contains blank resource"
                );
            }

            loadCatalogResource(
                    resource,
                    result,
                    sourceById
            );
        }

        return List.copyOf(
                result
        );
    }

    private static void loadCatalogResource(
            String resource,
            List<DynastyData> destination,
            Map<String, String> sourceById
    ) throws IOException {

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

        JsonArray dynasties =
                root.has(
                        "dynasties"
                )
                        && root.get(
                        "dynasties"
                ).isJsonArray()
                        ? root.getAsJsonArray(
                        "dynasties"
                )
                        : null;

        if (dynasties == null) {

            throw new IOException(
                    "Dynasty catalog resource has no dynasties array: "
                            + resource
            );
        }

        for (
                JsonElement element :
                dynasties
        ) {

            if (!element.isJsonObject()) {

                throw new IOException(
                        "Dynasty entry is not an object in "
                                + resource
                );
            }

            JsonObject merged =
                    merge(
                            defaults,
                            element.getAsJsonObject()
                    );

            DynastyData data =
                    GSON.fromJson(
                            merged,
                            DynastyData.class
                    );

            if (data == null
                    || !hasText(
                    data.id
            )) {

                throw new IOException(
                        "Dynasty entry without ID in "
                                + resource
                );
            }

            String normalized =
                    data.id.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            String previous =
                    sourceById.putIfAbsent(
                            normalized,
                            resource
                    );

            if (previous != null) {

                throw new IOException(
                        "Duplicate dynasty authored ID "
                                + normalized
                                + " in "
                                + previous
                                + " and "
                                + resource
                );
            }

            destination.add(
                    data
            );
        }
    }

    private static JsonObject merge(
            JsonObject defaults,
            JsonObject entry
    ) {

        JsonObject result =
                new JsonObject();

        for (
                Map.Entry<String, JsonElement> value :
                defaults.entrySet()
        ) {

            result.add(
                    value.getKey(),
                    value.getValue()
                            .deepCopy()
            );
        }

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

    private static Dynasty requireAuthored(
            DynastyManager manager,
            String id
    ) {

        return manager.findAuthored(
                        id
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Unknown referenced dynasty "
                                                + id
                                )
                );
    }

    private static OrganizationId ensureOrganization(
            NpcSimulation simulation,
            String authoredId,
            String name,
            DynastyType type
    ) {

        var ids =
                simulation.authoredIds();

        OrganizationId existingId =
                ids.findOrganization(
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
                                                    "Authored organization mapping "
                                                            + authoredId
                                                            + " points to missing organization "
                                                            + existingId
                                            )
                            );

            if (organization.type()
                    != type.organizationType()) {

                throw new IllegalStateException(
                        "Dynasty "
                                + authoredId
                                + " expects organization type "
                                + type.organizationType()
                                + ", but existing organization is "
                                + organization.type()
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

        ids.registerOrganization(
                authoredId,
                organization.id()
        );

        return organization.id();
    }

    private static Set<DynastyContinuity> parseContinuities(
            DynastyData data
    ) {

        if (data.continuities == null
                || data.continuities.isEmpty()) {

            throw new IllegalArgumentException(
                    "Dynasty "
                            + data.id
                            + " requires at least one continuity"
            );
        }

        Set<DynastyContinuity> result =
                new LinkedHashSet<>();

        for (
                String value :
                data.continuities
        ) {

            result.add(
                    enumValue(
                            DynastyContinuity.class,
                            value,
                            data.id
                                    + ".continuity"
                    )
            );
        }

        return Set.copyOf(
                result
        );
    }

    private static void validateReferences(
            DynastyData data
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
                    "Dynasty "
                            + data.id
                            + " references unknown home location "
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
                    "Dynasty "
                            + data.id
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
                    "Dynasty "
                            + data.id
                            + " references unknown religion "
                            + data.religionId
            );
        }

        DynastyStatus status =
                enumValue(
                        DynastyStatus.class,
                        data.status,
                        data.id
                                + ".status"
                );

        if ((status == DynastyStatus.EXTINCT
                || status == DynastyStatus.NOT_YET_FOUNDED)
                && data.activeAtScenarioStart) {

            throw new IllegalStateException(
                    "Dynasty "
                            + data.id
                            + " has status "
                            + status
                            + " but is active at scenario start"
            );
        }

        if (data.foundedYear != null
                && data.extinctYear != null
                && data.extinctYear < data.foundedYear) {

            throw new IllegalStateException(
                    "Dynasty "
                            + data.id
                            + " has extinctYear before foundedYear"
            );
        }
    }

    private static double metric(
            Double explicit,
            DynastyType type,
            boolean active,
            Metric metric
    ) {

        if (explicit != null) {

            return clamp01(
                    explicit
            );
        }

        if (!active) {

            return metric == Metric.PRESTIGE
                    ? historicalPrestige(
                    type
            )
                    : 0.0;
        }

        return switch (type) {

            case ROYAL_HOUSE ->
                    switch (metric) {
                        case PRESTIGE -> 0.95;
                        case WEALTH -> 0.82;
                        case MILITARY -> 0.86;
                    };

            case GREAT_HOUSE ->
                    switch (metric) {
                        case PRESTIGE -> 0.88;
                        case WEALTH -> 0.72;
                        case MILITARY -> 0.76;
                    };

            case NOBLE_HOUSE ->
                    switch (metric) {
                        case PRESTIGE -> 0.50;
                        case WEALTH -> 0.44;
                        case MILITARY -> 0.44;
                    };

            case LANDED_KNIGHTLY_HOUSE ->
                    switch (metric) {
                        case PRESTIGE -> 0.34;
                        case WEALTH -> 0.30;
                        case MILITARY -> 0.34;
                    };

            case CADET_BRANCH ->
                    switch (metric) {
                        case PRESTIGE -> 0.42;
                        case WEALTH -> 0.36;
                        case MILITARY -> 0.38;
                    };

            case EXILED_DYNASTY ->
                    switch (metric) {
                        case PRESTIGE -> 0.72;
                        case WEALTH -> 0.16;
                        case MILITARY -> 0.10;
                    };

            default ->
                    switch (metric) {
                        case PRESTIGE -> 0.40;
                        case WEALTH -> 0.38;
                        case MILITARY -> 0.32;
                    };
        };
    }

    private static double historicalPrestige(
            DynastyType type
    ) {

        return switch (type) {

            case ROYAL_HOUSE ->
                    0.90;

            case GREAT_HOUSE ->
                    0.80;

            case NOBLE_HOUSE ->
                    0.45;

            case LANDED_KNIGHTLY_HOUSE ->
                    0.30;

            case CADET_BRANCH ->
                    0.34;

            case EXILED_DYNASTY ->
                    0.65;

            default ->
                    0.35;
        };
    }

    private static double clamp01(
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

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String raw,
            String description
    ) {

        requireText(
                raw,
                description
        );

        try {

            return Enum.valueOf(
                    type,
                    raw.trim()
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
                            + raw,
                    exception
            );
        }
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static void requireText(
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
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        DynastyScenarioBootstrapService.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Dynasty resource not found: "
                                + resource
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
                            "Dynasty resource produced null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid dynasty JSON: "
                            + resource,
                    exception
            );
        }
    }

    private enum Metric {
        PRESTIGE,
        WEALTH,
        MILITARY
    }

    private static final class CatalogManifest {

        List<String> resources;
    }

    private static final class DynastyData {

        String id;

        String name;

        String type;

        String status;

        List<String> continuities;

        boolean activeAtScenarioStart;

        Integer foundedYear;

        Integer extinctYear;

        String homeLocationId;

        String cultureId;

        String religionId;

        String parentDynastyId;

        String liegeDynastyId;

        String predecessorDynastyId;

        String successorDynastyId;

        String headNpcId;

        String heirNpcId;

        String words;

        String heraldry;

        Double prestige;

        Double wealth;

        Double militaryStrength;

        String provenance;

        String sourceNote;
    }
}
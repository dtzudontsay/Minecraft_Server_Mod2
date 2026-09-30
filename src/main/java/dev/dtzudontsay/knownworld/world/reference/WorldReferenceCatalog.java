package dev.dtzudontsay.knownworld.world.reference;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;
import dev.dtzudontsay.knownworld.world.geography.location.FeatureLocation;
import dev.dtzudontsay.knownworld.world.geography.location.FeatureLocationResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class WorldReferenceCatalog {

    private static final Gson GSON =
            new Gson();

    private static final String BASE =
            "data/knownworld/reference/";

    private static WorldReferenceCatalog instance;

    private final Map<String, WorldLocationDefinition> locations =
            new LinkedHashMap<>();

    private final Map<String, ReferenceEntry> cultures =
            new LinkedHashMap<>();

    private final Map<String, ReferenceEntry> religions =
            new LinkedHashMap<>();

    private final Map<String, ReferenceEntry> religiousOrders =
            new LinkedHashMap<>();

    private final Map<String, ReferenceEntry> languages =
            new LinkedHashMap<>();

    private final Map<String, ReferenceEntry> occupations =
            new LinkedHashMap<>();

    private final Map<String, ReferenceEntry> roles =
            new LinkedHashMap<>();

    private WorldReferenceCatalog() {
    }

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        WorldReferenceCatalog catalog =
                new WorldReferenceCatalog();

        try {

            catalog.loadLocations(
                    BASE
                            + "locations.json"
            );

            catalog.loadEntries(
                    BASE
                            + "cultures.json",
                    catalog.cultures
            );

            catalog.loadEntries(
                    BASE
                            + "religions.json",
                    catalog.religions
            );

            catalog.loadEntries(
                    BASE
                            + "religious_orders.json",
                    catalog.religiousOrders
            );

            catalog.loadEntries(
                    BASE
                            + "languages.json",
                    catalog.languages
            );

            catalog.loadEntries(
                    BASE
                            + "occupations.json",
                    catalog.occupations
            );

            catalog.loadEntries(
                    BASE
                            + "roles.json",
                    catalog.roles
            );

            /*
             * Batch 19.0A.
             *
             * Small future vocabulary expansions can now live in
             * dedicated extension files rather than forcing complete
             * rewrites of the large baseline catalogs.
             */
            catalog.loadOptionalEntries(
                    BASE
                            + "occupations_extensions.json",
                    catalog.occupations
            );

            catalog.loadOptionalEntries(
                    BASE
                            + "roles_extensions.json",
                    catalog.roles
            );

            catalog.importGeographicFeatures();

            catalog.validate();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load Known World reference catalog",
                    exception
            );
        }

        instance =
                catalog;

        KnownWorld.LOGGER.info(
                "World reference catalog loaded: {} locations, {} cultures, {} religions, {} religious orders, {} languages, {} occupations and {} roles.",
                catalog.locations.size(),
                catalog.cultures.size(),
                catalog.religions.size(),
                catalog.religiousOrders.size(),
                catalog.languages.size(),
                catalog.occupations.size(),
                catalog.roles.size()
        );
    }

    public static WorldReferenceCatalog get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "World reference catalog has not been bootstrapped"
            );
        }

        return instance;
    }

    public Optional<WorldLocationDefinition> location(
            String id
    ) {

        return Optional.ofNullable(
                locations.get(
                        normalizeId(
                                id
                        )
                )
        );
    }

    public Optional<ReferenceEntry> culture(
            String id
    ) {

        return findEntry(
                cultures,
                id
        );
    }

    public Optional<ReferenceEntry> religion(
            String id
    ) {

        return findEntry(
                religions,
                id
        );
    }

    public Optional<ReferenceEntry> religiousOrder(
            String id
    ) {

        return findEntry(
                religiousOrders,
                id
        );
    }

    public Optional<ReferenceEntry> language(
            String id
    ) {

        return findEntry(
                languages,
                id
        );
    }

    public Optional<ReferenceEntry> occupation(
            String id
    ) {

        return findEntry(
                occupations,
                id
        );
    }

    public Optional<ReferenceEntry> role(
            String id
    ) {

        return findEntry(
                roles,
                id
        );
    }

    public Collection<WorldLocationDefinition> locations() {

        return List.copyOf(
                locations.values()
        );
    }

    public Collection<ReferenceEntry> cultures() {

        return List.copyOf(
                cultures.values()
        );
    }

    public Collection<ReferenceEntry> religions() {

        return List.copyOf(
                religions.values()
        );
    }

    public Collection<ReferenceEntry> religiousOrders() {

        return List.copyOf(
                religiousOrders.values()
        );
    }

    public Collection<ReferenceEntry> languages() {

        return List.copyOf(
                languages.values()
        );
    }

    public Collection<ReferenceEntry> occupations() {

        return List.copyOf(
                occupations.values()
        );
    }

    public Collection<ReferenceEntry> roles() {

        return List.copyOf(
                roles.values()
        );
    }

    public List<WorldLocationDefinition> childrenOf(
            String parentId
    ) {

        String normalized =
                normalizeId(
                        parentId
                );

        return locations.values()
                .stream()
                .filter(
                        location ->
                                normalized.equals(
                                        location.parentId()
                                )
                )
                .toList();
    }

    public List<WorldLocationDefinition> pathToRoot(
            String locationId
    ) {

        WorldLocationDefinition current =
                location(
                        locationId
                )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown world location: "
                                                        + locationId
                                        )
                        );

        List<WorldLocationDefinition> reversed =
                new ArrayList<>();

        int safety =
                0;

        while (current != null) {

            reversed.add(
                    current
            );

            if (!current.hasParent()) {
                break;
            }

            current =
                    locations.get(
                            current.parentId()
                    );

            safety++;

            if (safety > 100) {

                throw new IllegalStateException(
                        "Location hierarchy cycle detected"
                );
            }
        }

        List<WorldLocationDefinition> result =
                new ArrayList<>();

        for (
                int index =
                reversed.size() - 1;
                index >= 0;
                index--
        ) {

            result.add(
                    reversed.get(
                            index
                    )
            );
        }

        return List.copyOf(
                result
        );
    }

    public Optional<FeatureLocation> resolvedMapLocation(
            String locationId
    ) {

        WorldLocationDefinition location =
                location(
                        locationId
                )
                        .orElse(
                                null
                        );

        if (location == null
                || !location.hasGeographicFeature()) {

            return Optional.empty();
        }

        return FeatureLocationResolver.resolve(
                location.geographicFeatureId()
        );
    }

    public String effectiveCultureId(
            String cultureId
    ) {

        String normalized =
                normalizeId(
                        cultureId
                );

        ReferenceEntry current =
                cultures.get(
                        normalized
                );

        if (current == null) {

            throw new IllegalArgumentException(
                    "Unknown culture: "
                            + cultureId
            );
        }

        if (WorldReferenceSettings.get()
                .subculturesEnabled()) {

            return current.id();
        }

        int safety =
                0;

        while (
                "subculture".equals(
                        current.category()
                )
                        && current.hasParent()
        ) {

            ReferenceEntry parent =
                    cultures.get(
                            current.parentId()
                    );

            if (parent == null) {
                break;
            }

            current =
                    parent;

            safety++;

            if (safety > 50) {

                throw new IllegalStateException(
                        "Culture hierarchy cycle detected"
                );
            }
        }

        return current.id();
    }

    public boolean languageBarriersEnabled() {

        return WorldReferenceSettings.get()
                .effectiveLanguageBarriersEnabled();
    }

    private void loadLocations(
            String resource
    ) throws IOException {

        LocationData[] data =
                readJson(
                        resource,
                        LocationData[].class
                );

        for (
                LocationData entry :
                data
        ) {

            WorldLocationDefinition definition =
                    new WorldLocationDefinition(
                            entry.id,
                            entry.displayName,
                            enumValue(
                                    WorldLocationKind.class,
                                    entry.kind
                            ),
                            entry.parentId,
                            entry.geographicFeatureId,
                            entry.boundaryMaskId,
                            enumValue(
                                    ReferenceProvenance.class,
                                    entry.provenance
                            ),
                            entry.sourceNote,
                            false
                    );

            if (locations.putIfAbsent(
                    definition.id(),
                    definition
            ) != null) {

                throw new IllegalStateException(
                        "Duplicate world location: "
                                + definition.id()
                );
            }
        }
    }

    private void loadEntries(
            String resource,
            Map<String, ReferenceEntry> destination
    ) throws IOException {

        EntryData[] data =
                readJson(
                        resource,
                        EntryData[].class
                );

        addEntries(
                data,
                destination,
                resource
        );
    }

    private void loadOptionalEntries(
            String resource,
            Map<String, ReferenceEntry> destination
    ) throws IOException {

        EntryData[] data =
                readOptionalJson(
                        resource,
                        EntryData[].class
                );

        if (data == null) {
            return;
        }

        addEntries(
                data,
                destination,
                resource
        );
    }

    private void addEntries(
            EntryData[] data,
            Map<String, ReferenceEntry> destination,
            String resource
    ) {

        for (
                EntryData entry :
                data
        ) {

            ReferenceEntry definition =
                    new ReferenceEntry(
                            entry.id,
                            entry.displayName,
                            entry.parentId,
                            entry.category,
                            enumValue(
                                    ReferenceProvenance.class,
                                    entry.provenance
                            ),
                            entry.sourceNote
                    );

            if (destination.putIfAbsent(
                    definition.id(),
                    definition
            ) != null) {

                throw new IllegalStateException(
                        "Duplicate reference entry "
                                + definition.id()
                                + " while loading "
                                + resource
                );
            }
        }
    }

    private void importGeographicFeatures() {

        for (
                GeographicFeature feature :
                GeographicFeatureRegistry.getAll()
        ) {

            if (locations.containsKey(
                    feature.id()
            )) {

                continue;
            }

            locations.put(
                    feature.id(),
                    new WorldLocationDefinition(
                            feature.id(),
                            feature.displayName(),
                            locationKindFor(
                                    feature.type()
                            ),
                            null,
                            feature.id(),
                            null,
                            provenanceFor(
                                    feature.confidence()
                            ),
                            feature.sourceNote(),
                            true
                    )
            );
        }
    }

    private void validate() {

        for (
                WorldLocationDefinition location :
                locations.values()
        ) {

            if (location.hasParent()
                    && !locations.containsKey(
                    location.parentId()
            )) {

                throw new IllegalStateException(
                        "Location "
                                + location.id()
                                + " references missing parent "
                                + location.parentId()
                );
            }
        }

        validateParentsWithin(
                cultures,
                "culture"
        );

        validateParentsWithin(
                religions,
                "religion"
        );

        validateParentsWithin(
                languages,
                "language"
        );

        for (
                ReferenceEntry order :
                religiousOrders.values()
        ) {

            if (order.hasParent()
                    && !religions.containsKey(
                    order.parentId()
            )) {

                throw new IllegalStateException(
                        "Religious order "
                                + order.id()
                                + " references missing religion "
                                + order.parentId()
                );
            }
        }
    }

    private static void validateParentsWithin(
            Map<String, ReferenceEntry> entries,
            String description
    ) {

        for (
                ReferenceEntry entry :
                entries.values()
        ) {

            if (entry.hasParent()
                    && !entries.containsKey(
                    entry.parentId()
            )) {

                throw new IllegalStateException(
                        description
                                + " "
                                + entry.id()
                                + " references missing parent "
                                + entry.parentId()
                );
            }
        }
    }

    private static WorldLocationKind locationKindFor(
            GeographicFeatureType type
    ) {

        return switch (type) {

            case CONTINENT ->
                    WorldLocationKind.CONTINENT;

            case REGION ->
                    WorldLocationKind.REGION;

            case CASTLE,
                 CITY,
                 TOWN,
                 RUINED_CITY,
                 SETTLEMENT ->
                    WorldLocationKind.SETTLEMENT;

            case FORTRESS_GROUP,
                 CITY_GROUP ->
                    WorldLocationKind.SUBREGION;

            default ->
                    WorldLocationKind.NATURAL_FEATURE;
        };
    }

    private static ReferenceProvenance provenanceFor(
            SourceConfidence confidence
    ) {

        return switch (confidence) {

            case CANON ->
                    ReferenceProvenance.CANON;

            case STRONGLY_INFERRED ->
                    ReferenceProvenance.HYBRID_CANON;

            case INTERPOLATED ->
                    ReferenceProvenance.PLAUSIBLE;
        };
    }

    private static Optional<ReferenceEntry> findEntry(
            Map<String, ReferenceEntry> entries,
            String id
    ) {

        return Optional.ofNullable(
                entries.get(
                        normalizeId(
                                id
                        )
                )
        );
    }

    private static String normalizeId(
            String id
    ) {

        Objects.requireNonNull(
                id,
                "id"
        );

        return id.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Missing enum value for "
                            + type.getSimpleName()
            );
        }

        return Enum.valueOf(
                type,
                value.trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
        );
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        WorldReferenceCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Reference resource not found: "
                                + resource
                );
            }

            return parseJson(
                    input,
                    resource,
                    type
            );
        }
    }

    private static <T> T readOptionalJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        WorldReferenceCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {
                return null;
            }

            return parseJson(
                    input,
                    resource,
                    type
            );
        }
    }

    private static <T> T parseJson(
            InputStream input,
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                Reader reader =
                        new InputStreamReader(
                                input,
                                StandardCharsets.UTF_8
                        )
        ) {

            T value =
                    GSON.fromJson(
                            reader,
                            type
                    );

            if (value == null) {

                throw new IOException(
                        "Reference JSON returned null: "
                                + resource
                );
            }

            return value;

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid reference JSON: "
                            + resource,
                    exception
            );
        }
    }

    private static final class LocationData {

        String id;

        String displayName;

        String kind;

        String parentId;

        String geographicFeatureId;

        String boundaryMaskId;

        String provenance;

        String sourceNote;
    }

    private static final class EntryData {

        String id;

        String displayName;

        String parentId;

        String category;

        String provenance;

        String sourceNote;
    }
}
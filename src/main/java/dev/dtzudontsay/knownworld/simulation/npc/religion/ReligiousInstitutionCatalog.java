package dev.dtzudontsay.knownworld.simulation.npc.religion;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

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
import java.util.Optional;
import java.util.Set;

public final class ReligiousInstitutionCatalog {

    private static final Gson GSON =
            new Gson();

    private static final String RESOURCE =
            "data/knownworld/reference/religious_institutions.json";

    private static ReligiousInstitutionCatalog instance;

    private final Map<String, ReligiousOrderDefinition> definitions =
            new LinkedHashMap<>();

    private ReligiousInstitutionCatalog() {
    }

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        ReligiousInstitutionCatalog catalog =
                new ReligiousInstitutionCatalog();

        try {

            catalog.load();

            catalog.validate();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load religious institutions",
                    exception
            );
        }

        instance =
                catalog;

        KnownWorld.LOGGER.info(
                "Religious institution catalog loaded with {} definitions.",
                catalog.definitions.size()
        );
    }

    public static ReligiousInstitutionCatalog get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "ReligiousInstitutionCatalog has not been bootstrapped"
            );
        }

        return instance;
    }

    public Optional<ReligiousOrderDefinition> find(
            String id
    ) {

        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                definitions.get(
                        id.trim()
                                .toLowerCase(
                                        Locale.ROOT
                                )
                )
        );
    }

    public Collection<ReligiousOrderDefinition> all() {

        return List.copyOf(
                definitions.values()
        );
    }

    public List<ReligiousOrderDefinition> forReligion(
            String religionId
    ) {

        if (religionId == null) {
            return List.of();
        }

        String normalized =
                religionId.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return definitions.values()
                .stream()
                .filter(
                        definition ->
                                definition.religionId()
                                        .equals(
                                                normalized
                                        )
                )
                .toList();
    }

    private void load() throws IOException {

        DefinitionData[] data =
                readJson(
                        RESOURCE,
                        DefinitionData[].class
                );

        for (
                DefinitionData entry :
                data
        ) {

            ReligiousOrderDefinition definition =
                    new ReligiousOrderDefinition(
                            entry.id,
                            entry.displayName,
                            entry.religionId,
                            enumValue(
                                    ReligiousOrderCategory.class,
                                    entry.category
                            ),
                            entry.clergy,
                            entry.militant,
                            entry.exclusiveMembership,
                            entry.minimumDevotion,
                            entry.minimumDoctrinalKnowledge,
                            setOf(
                                    entry.rituals
                            ),
                            setOf(
                                    entry.taboos
                            ),
                            setOf(
                                    entry.roles
                            ),
                            enumValue(
                                    ReferenceProvenance.class,
                                    entry.provenance
                            ),
                            entry.sourceNote
                    );

            if (definitions.putIfAbsent(
                    definition.id(),
                    definition
            ) != null) {

                throw new IllegalStateException(
                        "Duplicate religious institution ID "
                                + definition.id()
                );
            }
        }
    }

    private void validate() {

        WorldReferenceCatalog references =
                WorldReferenceCatalog.get();

        for (
                ReligiousOrderDefinition definition :
                definitions.values()
        ) {

            if (references.religion(
                    definition.religionId()
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Religious institution "
                                + definition.id()
                                + " references unknown religion "
                                + definition.religionId()
                );
            }

            /*
             * Where the old reference religious_orders.json already
             * contains the same ID, require consistency.
             */
            references.religiousOrder(
                    definition.id()
            ).ifPresent(
                    reference -> {

                        if (!definition.religionId()
                                .equals(
                                        reference.parentId()
                                )) {

                            throw new IllegalStateException(
                                    "Religious institution "
                                            + definition.id()
                                            + " conflicts with reference parent "
                                            + reference.parentId()
                            );
                        }
                    }
            );
        }
    }

    private static Set<String> setOf(
            String[] values
    ) {

        if (values == null
                || values.length == 0) {

            return Set.of();
        }

        return Set.copyOf(
                List.of(
                        values
                )
        );
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String raw
    ) {

        if (raw == null
                || raw.isBlank()) {

            throw new IllegalArgumentException(
                    "Missing enum value for "
                            + type.getSimpleName()
            );
        }

        return Enum.valueOf(
                type,
                raw.trim()
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
                        ReligiousInstitutionCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Resource not found: "
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

                T value =
                        GSON.fromJson(
                                reader,
                                type
                        );

                if (value == null) {

                    throw new IOException(
                            "Resource returned null: "
                                    + resource
                    );
                }

                return value;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid JSON in "
                            + resource,
                    exception
            );
        }
    }

    private static final class DefinitionData {

        String id;

        String displayName;

        String religionId;

        String category;

        boolean clergy;

        boolean militant;

        boolean exclusiveMembership;

        double minimumDevotion;

        double minimumDoctrinalKnowledge;

        String[] rituals;

        String[] taboos;

        String[] roles;

        String provenance;

        String sourceNote;
    }
}
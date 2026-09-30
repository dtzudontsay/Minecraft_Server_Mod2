package dev.dtzudontsay.knownworld.simulation.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves authored population packs for a scenario.
 *
 * A population pack groups all authored population data belonging to
 * one coherent part of the world or roster.
 *
 * Examples:
 *
 * north_core
 * royal_court_core
 * nights_watch
 * riverlands_major
 * dothraki_named
 *
 * The important architectural rule is:
 *
 * scenario.json no longer needs to contain hundreds of character paths.
 */
public final class PopulationPackCatalog {

    private static final Gson GSON =
            new Gson();

    private PopulationPackCatalog() {
    }

    public static Plan load(
            String scenarioResource,
            String manifestReference
    ) throws IOException {

        Objects.requireNonNull(
                scenarioResource,
                "scenarioResource"
        );

        if (!hasText(
                manifestReference
        )) {

            return Plan.empty();
        }

        String scenarioBase =
                parentPath(
                        scenarioResource
                );

        String manifestResource =
                resolve(
                        scenarioBase,
                        manifestReference
                );

        ManifestData manifest =
                readJson(
                        manifestResource,
                        ManifestData.class
                );

        if (manifest.packs == null) {

            throw new IOException(
                    "Population pack manifest "
                            + manifestResource
                            + " has no packs array"
            );
        }

        List<Pack> packs =
                new ArrayList<>();

        List<String> characterResources =
                new ArrayList<>();

        List<String> parentageResources =
                new ArrayList<>();

        List<String> marriageResources =
                new ArrayList<>();

        List<String> socialIdentityResources =
                new ArrayList<>();

        Set<String> packIds =
                new LinkedHashSet<>();

        Set<String> characterPaths =
                new LinkedHashSet<>();

        Set<String> parentagePaths =
                new LinkedHashSet<>();

        Set<String> marriagePaths =
                new LinkedHashSet<>();

        Set<String> socialIdentityPaths =
                new LinkedHashSet<>();

        for (
                String packReference :
                manifest.packs
        ) {

            if (!hasText(
                    packReference
            )) {

                continue;
            }

            String packResource =
                    resolve(
                            scenarioBase,
                            packReference
                    );

            PackData data =
                    readJson(
                            packResource,
                            PackData.class
                    );

            String id =
                    normalizeId(
                            data.id,
                            "population pack id"
                    );

            if (!packIds.add(
                    id
            )) {

                throw new IOException(
                        "Duplicate population pack ID "
                                + id
                );
            }

            boolean enabled =
                    data.enabled == null
                            || data.enabled;

            PopulationPackKind kind =
                    hasText(
                            data.kind
                    )
                            ? enumValue(
                            PopulationPackKind.class,
                            data.kind,
                            id + ".kind"
                    )
                            : PopulationPackKind.AUTHORED_NAMED;

            List<String> packCharacters =
                    resolveEntries(
                            scenarioBase,
                            data.characters
                    );

            List<String> packParentages =
                    resolveEntries(
                            scenarioBase,
                            data.parentages
                    );

            List<String> packMarriages =
                    resolveEntries(
                            scenarioBase,
                            data.marriages
                    );

            List<String> packSocialIdentities =
                    resolveEntries(
                            scenarioBase,
                            data.socialIdentities
                    );

            Pack pack =
                    new Pack(
                            id,
                            hasText(
                                    data.displayName
                            )
                                    ? data.displayName.trim()
                                    : id,
                            kind,
                            normalizeOptionalId(
                                    data.regionId
                            ),
                            enabled,
                            packResource,
                            packCharacters,
                            packParentages,
                            packMarriages,
                            packSocialIdentities
                    );

            packs.add(
                    pack
            );

            if (!enabled) {
                continue;
            }

            addUniqueResources(
                    characterResources,
                    characterPaths,
                    packCharacters,
                    "character",
                    id
            );

            addUniqueResources(
                    parentageResources,
                    parentagePaths,
                    packParentages,
                    "parentage",
                    id
            );

            addUniqueResources(
                    marriageResources,
                    marriagePaths,
                    packMarriages,
                    "marriage",
                    id
            );

            addUniqueResources(
                    socialIdentityResources,
                    socialIdentityPaths,
                    packSocialIdentities,
                    "social identity",
                    id
            );
        }

        return new Plan(
                List.copyOf(
                        packs
                ),
                List.copyOf(
                        characterResources
                ),
                List.copyOf(
                        parentageResources
                ),
                List.copyOf(
                        marriageResources
                ),
                List.copyOf(
                        socialIdentityResources
                )
        );
    }

    private static void addUniqueResources(
            List<String> destination,
            Set<String> seen,
            List<String> resources,
            String description,
            String packId
    ) throws IOException {

        for (
                String resource :
                resources
        ) {

            if (!seen.add(
                    resource
            )) {

                throw new IOException(
                        "Population pack "
                                + packId
                                + " references duplicate "
                                + description
                                + " resource "
                                + resource
                );
            }

            destination.add(
                    resource
            );
        }
    }

    private static List<String> resolveEntries(
            String scenarioBase,
            List<String> entries
    ) {

        if (entries == null
                || entries.isEmpty()) {

            return List.of();
        }

        List<String> result =
                new ArrayList<>();

        for (
                String entry :
                entries
        ) {

            if (!hasText(
                    entry
            )) {

                continue;
            }

            result.add(
                    resolve(
                            scenarioBase,
                            entry
                    )
            );
        }

        return List.copyOf(
                result
        );
    }

    private static String resolve(
            String base,
            String child
    ) {

        if (!hasText(
                child
        )) {

            throw new IllegalArgumentException(
                    "Population-pack resource path cannot be blank"
            );
        }

        String trimmed =
                child.trim();

        if (trimmed.startsWith(
                "data/"
        )) {

            return trimmed;
        }

        return base
                + "/"
                + trimmed;
    }

    private static String parentPath(
            String resource
    ) {

        int separator =
                resource.lastIndexOf(
                        '/'
                );

        if (separator < 0) {

            return "";
        }

        return resource.substring(
                0,
                separator
        );
    }

    private static String normalizeId(
            String value,
            String description
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be blank"
            );
        }

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!normalized.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid "
                            + description
                            + ": "
                            + value
            );
        }

        return normalized;
    }

    private static String normalizeOptionalId(
            String value
    ) {

        if (!hasText(
                value
        )) {

            return null;
        }

        return normalizeId(
                value,
                "optional population-pack ID"
        );
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value,
            String description
    ) {

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

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        PopulationPackCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Population-pack resource not found: "
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
                            "Population-pack resource produced null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid JSON in population-pack resource "
                            + resource,
                    exception
            );
        }
    }

    public enum PopulationPackKind {

        /**
         * Hand-authored named characters.
         */
        AUTHORED_NAMED,

        /**
         * Reserved for the later regional demographic generator.
         */
        GENERATED_DEMOGRAPHIC,

        /**
         * Pack may eventually contain both authored anchors and
         * demographic generation instructions.
         */
        MIXED
    }

    public record Pack(
            String id,
            String displayName,
            PopulationPackKind kind,
            String regionId,
            boolean enabled,
            String resource,
            List<String> characterResources,
            List<String> parentageResources,
            List<String> marriageResources,
            List<String> socialIdentityResources
    ) {

        public Pack {

            Objects.requireNonNull(
                    id,
                    "id"
            );

            Objects.requireNonNull(
                    displayName,
                    "displayName"
            );

            Objects.requireNonNull(
                    kind,
                    "kind"
            );

            Objects.requireNonNull(
                    resource,
                    "resource"
            );

            characterResources =
                    List.copyOf(
                            characterResources
                    );

            parentageResources =
                    List.copyOf(
                            parentageResources
                    );

            marriageResources =
                    List.copyOf(
                            marriageResources
                    );

            socialIdentityResources =
                    List.copyOf(
                            socialIdentityResources
                    );
        }
    }

    public record Plan(
            List<Pack> packs,
            List<String> characterResources,
            List<String> parentageResources,
            List<String> marriageResources,
            List<String> socialIdentityResources
    ) {

        public Plan {

            packs =
                    List.copyOf(
                            packs
                    );

            characterResources =
                    List.copyOf(
                            characterResources
                    );

            parentageResources =
                    List.copyOf(
                            parentageResources
                    );

            marriageResources =
                    List.copyOf(
                            marriageResources
                    );

            socialIdentityResources =
                    List.copyOf(
                            socialIdentityResources
                    );
        }

        public static Plan empty() {

            return new Plan(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        public int enabledPackCount() {

            return (int) packs.stream()
                    .filter(
                            Pack::enabled
                    )
                    .count();
        }
    }

    private static final class ManifestData {

        List<String> packs;
    }

    private static final class PackData {

        String id;

        String displayName;

        String kind;

        String regionId;

        Boolean enabled;

        List<String> characters;

        List<String> parentages;

        List<String> marriages;

        List<String> socialIdentities;
    }
}
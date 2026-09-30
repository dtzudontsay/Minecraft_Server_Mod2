package dev.dtzudontsay.knownworld.world.reference.influence;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import dev.dtzudontsay.knownworld.world.reference.spatial.TerritoryZoneCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RegionalInfluenceCatalog {

    private static final Gson GSON =
            new Gson();

    private static final String BASE_RESOURCE =
            "data/knownworld/reference/regional_influences.json";

    private static final String EXTENSION_RESOURCE =
            "data/knownworld/reference/regional_influence_extensions.json";

    private static RegionalInfluenceCatalog instance;

    private final Map<String, RegionalInfluenceProfile> profiles =
            new LinkedHashMap<>();

    private RegionalInfluenceCatalog() {
    }

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        RegionalInfluenceCatalog catalog =
                new RegionalInfluenceCatalog();

        try {

            catalog.loadBase();

            catalog.loadExtensions();

            catalog.validate();

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load regional influence catalog",
                    exception
            );
        }

        instance =
                catalog;

        KnownWorld.LOGGER.info(
                "Regional influence catalog loaded with {} effective profiles.",
                catalog.profiles.size()
        );
    }

    public static RegionalInfluenceCatalog get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Regional influence catalog has not been bootstrapped"
            );
        }

        return instance;
    }

    public Optional<RegionalInfluenceProfile> find(
            String locationId
    ) {

        if (locationId == null
                || locationId.isBlank()) {

            return Optional.empty();
        }

        return Optional.ofNullable(
                profiles.get(
                        normalizeId(
                                locationId
                        )
                )
        );
    }

    public List<RegionalInfluenceProfile> all() {

        return List.copyOf(
                profiles.values()
        );
    }

    public int size() {
        return profiles.size();
    }

    /*
     * =========================================================
     * LOAD
     * =========================================================
     */

    private void loadBase() throws IOException {

        ProfileData[] data =
                readJson(
                        BASE_RESOURCE,
                        ProfileData[].class,
                        false
                );

        for (
                ProfileData entry :
                data
        ) {

            RegionalInfluenceProfile profile =
                    createProfile(
                            entry
                    );

            if (profiles.putIfAbsent(
                    profile.locationId(),
                    profile
            ) != null) {

                throw new IllegalStateException(
                        "Duplicate regional influence profile: "
                                + profile.locationId()
                );
            }
        }
    }

    /**
     * Batch 18H.1.
     *
     * Extension profiles refine the existing regional model without
     * requiring the large historical baseline JSON to be rewritten.
     *
     * When an extension has the same locationId as an existing profile,
     * only dimensions authored by the extension replace/refine the base.
     */
    private void loadExtensions() throws IOException {

        ProfileData[] data =
                readJson(
                        EXTENSION_RESOURCE,
                        ProfileData[].class,
                        true
                );

        if (data == null) {
            return;
        }

        for (
                ProfileData entry :
                data
        ) {

            RegionalInfluenceProfile refinement =
                    createProfile(
                            entry
                    );

            RegionalInfluenceProfile existing =
                    profiles.get(
                            refinement.locationId()
                    );

            if (existing == null) {

                profiles.put(
                        refinement.locationId(),
                        refinement
                );

            } else {

                profiles.put(
                        refinement.locationId(),
                        RegionalInfluenceProfile.merge(
                                existing,
                                refinement
                        )
                );
            }
        }
    }

    private RegionalInfluenceProfile createProfile(
            ProfileData data
    ) {

        if (data == null
                || data.locationId == null
                || data.locationId.isBlank()) {

            throw new IllegalArgumentException(
                    "Regional influence profile requires locationId"
            );
        }

        EnumMap<CharacterValue, RegionalInfluenceValue> values =
                new EnumMap<>(
                        CharacterValue.class
                );

        if (data.values != null) {

            for (
                    Map.Entry<String, InfluenceData> entry :
                    data.values.entrySet()
            ) {

                values.put(
                        enumValue(
                                CharacterValue.class,
                                entry.getKey()
                        ),
                        influenceValue(
                                entry.getValue()
                        )
                );
            }
        }

        EnumMap<CharacterSocialNorm, RegionalInfluenceValue> norms =
                new EnumMap<>(
                        CharacterSocialNorm.class
                );

        if (data.socialNorms != null) {

            for (
                    Map.Entry<String, InfluenceData> entry :
                    data.socialNorms.entrySet()
            ) {

                norms.put(
                        enumValue(
                                CharacterSocialNorm.class,
                                entry.getKey()
                        ),
                        influenceValue(
                                entry.getValue()
                        )
                );
            }
        }

        return new RegionalInfluenceProfile(
                data.locationId,
                data.sourceNote,
                values,
                norms,
                exposureMap(
                        data.cultures
                ),
                exposureMap(
                        data.religions
                ),
                exposureMap(
                        data.languages
                )
        );
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private void validate() {

        WorldReferenceCatalog references =
                WorldReferenceCatalog.get();

        for (
                RegionalInfluenceProfile profile :
                profiles.values()
        ) {

            if (references.location(
                    profile.locationId()
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Regional influence profile references unknown location "
                                + profile.locationId()
                );
            }

            validateCultureExposure(
                    references,
                    profile
            );

            validateReligionExposure(
                    references,
                    profile
            );

            validateLanguageExposure(
                    references,
                    profile
            );
        }

        /*
         * Every major runtime territory must have a baseline.
         *
         * Sparse/unknown profiles are allowed, but completely missing
         * territories are not.
         */
        for (
                var territory :
                TerritoryZoneCatalog.get()
                        .all()
        ) {

            if (!profiles.containsKey(
                    territory.locationId()
            )) {

                throw new IllegalStateException(
                        "Missing regional influence profile for territory "
                                + territory.locationId()
                );
            }
        }
    }

    private static void validateCultureExposure(
            WorldReferenceCatalog references,
            RegionalInfluenceProfile profile
    ) {

        for (
                String culture :
                profile.cultureExposure()
                        .keySet()
        ) {

            if (references.culture(
                    culture
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Regional influence profile "
                                + profile.locationId()
                                + " references unknown culture "
                                + culture
                );
            }
        }
    }

    private static void validateReligionExposure(
            WorldReferenceCatalog references,
            RegionalInfluenceProfile profile
    ) {

        for (
                String religion :
                profile.religionExposure()
                        .keySet()
        ) {

            if (references.religion(
                    religion
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Regional influence profile "
                                + profile.locationId()
                                + " references unknown religion "
                                + religion
                );
            }
        }
    }

    private static void validateLanguageExposure(
            WorldReferenceCatalog references,
            RegionalInfluenceProfile profile
    ) {

        for (
                String language :
                profile.languageExposure()
                        .keySet()
        ) {

            if (references.language(
                    language
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Regional influence profile "
                                + profile.locationId()
                                + " references unknown language "
                                + language
                );
            }
        }
    }

    /*
     * =========================================================
     * DATA CONVERSION
     * =========================================================
     */

    private static RegionalInfluenceValue influenceValue(
            InfluenceData data
    ) {

        if (data == null) {

            return RegionalInfluenceValue.unknown();
        }

        return new RegionalInfluenceValue(
                data.value,
                provenance(
                        data.provenance
                ),
                data.confidence,
                data.sourceNote
        );
    }

    private static Map<String, RegionalExposureWeight>
    exposureMap(
            Map<String, ExposureData> data
    ) {

        Map<String, RegionalExposureWeight> result =
                new LinkedHashMap<>();

        if (data == null) {
            return result;
        }

        for (
                Map.Entry<String, ExposureData> entry :
                data.entrySet()
        ) {

            ExposureData value =
                    entry.getValue();

            result.put(
                    normalizeId(
                            entry.getKey()
                    ),
                    value == null
                            ? RegionalExposureWeight.unknown()
                            : new RegionalExposureWeight(
                            value.weight,
                            provenance(
                                    value.provenance
                            ),
                            value.confidence,
                            value.sourceNote
                    )
            );
        }

        return result;
    }

    private static ReferenceProvenance provenance(
            String raw
    ) {

        if (raw == null
                || raw.isBlank()) {

            return ReferenceProvenance.UNKNOWN;
        }

        return ReferenceProvenance.valueOf(
                raw.trim()
                        .toUpperCase(
                                Locale.ROOT
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

    private static String normalizeId(
            String id
    ) {

        if (id == null
                || id.isBlank()) {

            throw new IllegalArgumentException(
                    "Reference ID cannot be blank"
            );
        }

        return id.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    /*
     * =========================================================
     * JSON
     * =========================================================
     */

    private static <T> T readJson(
            String resource,
            Class<T> type,
            boolean optional
    ) throws IOException {

        try (
                InputStream input =
                        RegionalInfluenceCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                if (optional) {
                    return null;
                }

                throw new IOException(
                        "Reference resource not found: "
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

                if (value == null
                        && !optional) {

                    throw new IOException(
                            "Reference JSON returned null: "
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

    private static final class ProfileData {

        String locationId;

        String sourceNote;

        Map<String, InfluenceData> values;

        Map<String, InfluenceData> socialNorms;

        Map<String, ExposureData> cultures;

        Map<String, ExposureData> religions;

        Map<String, ExposureData> languages;
    }

    private static final class InfluenceData {

        double value;

        String provenance;

        double confidence;

        String sourceNote;
    }

    private static final class ExposureData {

        double weight;

        String provenance;

        double confidence;

        String sourceNote;
    }
}
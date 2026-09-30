package dev.dtzudontsay.knownworld.world.reference.influence;

import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class RegionalInfluenceProfile {

    private final String locationId;

    private final String sourceNote;

    private final EnumMap<CharacterValue, RegionalInfluenceValue>
            characterValues;

    private final EnumMap<CharacterSocialNorm, RegionalInfluenceValue>
            socialNorms;

    private final Map<String, RegionalExposureWeight>
            cultureExposure;

    private final Map<String, RegionalExposureWeight>
            religionExposure;

    private final Map<String, RegionalExposureWeight>
            languageExposure;

    public RegionalInfluenceProfile(
            String locationId,
            String sourceNote,
            Map<CharacterValue, RegionalInfluenceValue> characterValues,
            Map<CharacterSocialNorm, RegionalInfluenceValue> socialNorms,
            Map<String, RegionalExposureWeight> cultureExposure,
            Map<String, RegionalExposureWeight> religionExposure,
            Map<String, RegionalExposureWeight> languageExposure
    ) {

        this.locationId =
                normalizeId(
                        locationId
                );

        this.sourceNote =
                sourceNote == null
                        ? ""
                        : sourceNote.trim();

        this.characterValues =
                new EnumMap<>(
                        CharacterValue.class
                );

        if (characterValues != null) {

            this.characterValues.putAll(
                    characterValues
            );
        }

        this.socialNorms =
                new EnumMap<>(
                        CharacterSocialNorm.class
                );

        if (socialNorms != null) {

            this.socialNorms.putAll(
                    socialNorms
            );
        }

        this.cultureExposure =
                normalizeExposureMap(
                        cultureExposure
                );

        this.religionExposure =
                normalizeExposureMap(
                        religionExposure
                );

        this.languageExposure =
                normalizeExposureMap(
                        languageExposure
                );
    }

    public static RegionalInfluenceProfile empty(
            String locationId
    ) {

        return new RegionalInfluenceProfile(
                locationId,
                "",
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public String locationId() {
        return locationId;
    }

    public String sourceNote() {
        return sourceNote;
    }

    public RegionalInfluenceValue characterValue(
            CharacterValue value
    ) {

        Objects.requireNonNull(
                value,
                "value"
        );

        return characterValues.getOrDefault(
                value,
                RegionalInfluenceValue.unknown()
        );
    }

    public RegionalInfluenceValue socialNorm(
            CharacterSocialNorm norm
    ) {

        Objects.requireNonNull(
                norm,
                "norm"
        );

        return socialNorms.getOrDefault(
                norm,
                RegionalInfluenceValue.unknown()
        );
    }

    /*
     * Complete maps.
     *
     * Every supported dimension is present. Missing authored data
     * becomes explicit UNKNOWN rather than silently becoming neutral.
     */
    public Map<CharacterValue, RegionalInfluenceValue>
    characterValues() {

        EnumMap<CharacterValue, RegionalInfluenceValue> result =
                new EnumMap<>(
                        CharacterValue.class
                );

        for (
                CharacterValue value :
                CharacterValue.values()
        ) {

            result.put(
                    value,
                    characterValue(
                            value
                    )
            );
        }

        return Map.copyOf(
                result
        );
    }

    public Map<CharacterSocialNorm, RegionalInfluenceValue>
    socialNorms() {

        EnumMap<CharacterSocialNorm, RegionalInfluenceValue> result =
                new EnumMap<>(
                        CharacterSocialNorm.class
                );

        for (
                CharacterSocialNorm norm :
                CharacterSocialNorm.values()
        ) {

            result.put(
                    norm,
                    socialNorm(
                            norm
                    )
            );
        }

        return Map.copyOf(
                result
        );
    }

    public Map<CharacterValue, RegionalInfluenceValue>
    authoredCharacterValues() {

        return Map.copyOf(
                characterValues
        );
    }

    public Map<CharacterSocialNorm, RegionalInfluenceValue>
    authoredSocialNorms() {

        return Map.copyOf(
                socialNorms
        );
    }

    public Map<String, RegionalExposureWeight>
    cultureExposure() {

        return Map.copyOf(
                cultureExposure
        );
    }

    public Map<String, RegionalExposureWeight>
    religionExposure() {

        return Map.copyOf(
                religionExposure
        );
    }

    public Map<String, RegionalExposureWeight>
    languageExposure() {

        return Map.copyOf(
                languageExposure
        );
    }

    public static RegionalInfluenceProfile merge(
            RegionalInfluenceProfile base,
            RegionalInfluenceProfile refinement
    ) {

        Objects.requireNonNull(
                base,
                "base"
        );

        if (refinement == null) {
            return base;
        }

        EnumMap<CharacterValue, RegionalInfluenceValue> values =
                new EnumMap<>(
                        CharacterValue.class
                );

        values.putAll(
                base.characterValues
        );

        values.putAll(
                refinement.characterValues
        );

        EnumMap<CharacterSocialNorm, RegionalInfluenceValue> norms =
                new EnumMap<>(
                        CharacterSocialNorm.class
                );

        norms.putAll(
                base.socialNorms
        );

        norms.putAll(
                refinement.socialNorms
        );

        Map<String, RegionalExposureWeight> cultures =
                new LinkedHashMap<>(
                        base.cultureExposure
                );

        cultures.putAll(
                refinement.cultureExposure
        );

        Map<String, RegionalExposureWeight> religions =
                new LinkedHashMap<>(
                        base.religionExposure
                );

        religions.putAll(
                refinement.religionExposure
        );

        Map<String, RegionalExposureWeight> languages =
                new LinkedHashMap<>(
                        base.languageExposure
                );

        languages.putAll(
                refinement.languageExposure
        );

        String combinedNote =
                refinement.sourceNote.isBlank()
                        ? base.sourceNote
                        : base.sourceNote.isBlank()
                        ? refinement.sourceNote
                        : base.sourceNote
                        + " | Refinement: "
                        + refinement.sourceNote;

        return new RegionalInfluenceProfile(
                refinement.locationId,
                combinedNote,
                values,
                norms,
                cultures,
                religions,
                languages
        );
    }

    private static Map<String, RegionalExposureWeight>
    normalizeExposureMap(
            Map<String, RegionalExposureWeight> input
    ) {

        Map<String, RegionalExposureWeight> result =
                new LinkedHashMap<>();

        if (input == null) {
            return result;
        }

        for (
                Map.Entry<String, RegionalExposureWeight> entry :
                input.entrySet()
        ) {

            result.put(
                    normalizeId(
                            entry.getKey()
                    ),
                    Objects.requireNonNull(
                            entry.getValue(),
                            "exposure value"
                    )
            );
        }

        return result;
    }

    private static String normalizeId(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Regional influence ID cannot be empty"
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
                    "Invalid regional influence ID: "
                            + value
            );
        }

        return normalized;
    }
}
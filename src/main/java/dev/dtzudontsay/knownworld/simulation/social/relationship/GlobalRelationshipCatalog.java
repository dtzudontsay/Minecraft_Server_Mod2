package dev.dtzudontsay.knownworld.simulation.social.relationship;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class GlobalRelationshipCatalog {

    public static final String DEFAULT_RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/global_relationships.json";

    private static final Gson GSON =
            new Gson();

    private GlobalRelationshipCatalog() {
    }

    public static Definition loadDefault()
            throws IOException {

        return load(
                DEFAULT_RESOURCE
        );
    }

    public static Definition load(
            String resource
    ) throws IOException {

        Objects.requireNonNull(
                resource,
                "resource"
        );

        try (
                InputStream input =
                        GlobalRelationshipCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Global relationship resource not found: "
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

                DefinitionData data =
                        GSON.fromJson(
                                reader,
                                DefinitionData.class
                        );

                if (data == null) {

                    throw new IOException(
                            "Global relationship resource produced null: "
                                    + resource
                    );
                }

                List<CharacterRelationshipDefinition> characters =
                        data.characterRelationships == null
                                ? List.of()
                                : data.characterRelationships
                                .stream()
                                .map(
                                        CharacterRelationshipData::toDefinition
                                )
                                .toList();

                List<DynastyRelationshipDefinition> dynasties =
                        data.dynastyRelationships == null
                                ? List.of()
                                : data.dynastyRelationships
                                .stream()
                                .map(
                                        DynastyRelationshipData::toDefinition
                                )
                                .toList();

                return new Definition(
                        characters,
                        dynasties
                );
            }

        } catch (
                JsonParseException
                        | IllegalArgumentException exception
        ) {

            throw new IOException(
                    "Invalid global relationship resource "
                            + resource,
                    exception
            );
        }
    }

    public record Definition(
            List<CharacterRelationshipDefinition> characterRelationships,
            List<DynastyRelationshipDefinition> dynastyRelationships
    ) {

        public Definition {

            characterRelationships =
                    List.copyOf(
                            characterRelationships
                    );

            dynastyRelationships =
                    List.copyOf(
                            dynastyRelationships
                    );
        }
    }

    public record CharacterRelationshipDefinition(
            int authoringVersion,
            String subject,
            String target,
            RelationshipClass relationshipClass,
            Confidence confidence,
            double affection,
            double trust,
            double respect,
            double fear,
            double familiarity,
            String note
    ) {
    }

    public record DynastyRelationshipDefinition(
            int authoringVersion,
            String subjectDynasty,
            String targetDynasty,
            Confidence confidence,
            double affinity,
            double trust,
            double respect,
            double fear,
            double familiarity,
            String note
    ) {
    }

    public enum RelationshipClass {

        DIRECT_CANON,

        STRONGLY_INFERRED_PERSONAL,

        PROFESSIONAL_OR_POLITICAL,

        REPUTATION_ONLY
    }

    public enum Confidence {

        CANON,

        STRONGLY_INFERRED,

        INFERRED,

        PLAUSIBLE_RECONSTRUCTION
    }

    private static final class DefinitionData {

        List<CharacterRelationshipData> characterRelationships;

        List<DynastyRelationshipData> dynastyRelationships;
    }

    private static final class CharacterRelationshipData {

        int authoringVersion;

        String subject;

        String target;

        String relationshipClass;

        String confidence;

        double affection;

        double trust;

        double respect;

        double fear;

        double familiarity;

        String note;

        CharacterRelationshipDefinition toDefinition() {

            requireId(
                    subject,
                    "character relationship subject"
            );

            requireId(
                    target,
                    "character relationship target"
            );

            if (subject.equals(
                    target
            )) {

                throw new IllegalArgumentException(
                        "Character relationship cannot target itself: "
                                + subject
                );
            }

            return new CharacterRelationshipDefinition(
                    positiveVersion(
                            authoringVersion
                    ),
                    subject,
                    target,
                    enumValue(
                            RelationshipClass.class,
                            relationshipClass,
                            "relationshipClass"
                    ),
                    enumValue(
                            Confidence.class,
                            confidence,
                            "confidence"
                    ),
                    signed(
                            affection,
                            "affection"
                    ),
                    signed(
                            trust,
                            "trust"
                    ),
                    signed(
                            respect,
                            "respect"
                    ),
                    unit(
                            fear,
                            "fear"
                    ),
                    unit(
                            familiarity,
                            "familiarity"
                    ),
                    note == null
                            ? ""
                            : note.trim()
            );
        }
    }

    private static final class DynastyRelationshipData {

        int authoringVersion;

        String subjectDynasty;

        String targetDynasty;

        String confidence;

        double affinity;

        double trust;

        double respect;

        double fear;

        double familiarity;

        String note;

        DynastyRelationshipDefinition toDefinition() {

            requireId(
                    subjectDynasty,
                    "dynasty relationship subject"
            );

            requireId(
                    targetDynasty,
                    "dynasty relationship target"
            );

            if (subjectDynasty.equals(
                    targetDynasty
            )) {

                throw new IllegalArgumentException(
                        "Dynasty relationship cannot target itself: "
                                + subjectDynasty
                );
            }

            return new DynastyRelationshipDefinition(
                    positiveVersion(
                            authoringVersion
                    ),
                    subjectDynasty,
                    targetDynasty,
                    enumValue(
                            Confidence.class,
                            confidence,
                            "confidence"
                    ),
                    signed(
                            affinity,
                            "affinity"
                    ),
                    signed(
                            trust,
                            "trust"
                    ),
                    signed(
                            respect,
                            "respect"
                    ),
                    unit(
                            fear,
                            "fear"
                    ),
                    unit(
                            familiarity,
                            "familiarity"
                    ),
                    note == null
                            ? ""
                            : note.trim()
            );
        }
    }

    private static int positiveVersion(
            int value
    ) {

        if (value <= 0) {
            return 1;
        }

        return value;
    }

    private static void requireId(
            String value,
            String description
    ) {

        if (value == null
                || value.isBlank()
                || !value.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid "
                            + description
                            + ": "
                            + value
            );
        }
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value,
            String description
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be blank"
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

    private static double signed(
            double value,
            String description
    ) {

        if (!Double.isFinite(
                value
        )
                || value < -1.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    description
                            + " must be between -1 and 1"
            );
        }

        return value;
    }

    private static double unit(
            double value,
            String description
    ) {

        if (!Double.isFinite(
                value
        )
                || value < 0.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    description
                            + " must be between 0 and 1"
            );
        }

        return value;
    }
}

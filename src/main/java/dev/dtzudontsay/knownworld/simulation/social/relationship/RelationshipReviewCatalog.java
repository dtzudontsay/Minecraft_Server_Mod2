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

public final class RelationshipReviewCatalog {

    public static final String DEFAULT_RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/relationship_review_roster.json";

    private static final Gson GSON =
            new Gson();

    private RelationshipReviewCatalog() {
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
                        RelationshipReviewCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Relationship review resource not found: "
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
                            "Relationship review resource produced null: "
                                    + resource
                    );
                }

                List<Entry> entries =
                        data.entries == null
                                ? List.of()
                                : data.entries
                                .stream()
                                .map(
                                        EntryData::toEntry
                                )
                                .toList();

                return new Definition(
                        entries
                );
            }

        } catch (
                JsonParseException
                        | IllegalArgumentException exception
        ) {

            throw new IOException(
                    "Invalid relationship review resource "
                            + resource,
                    exception
            );
        }
    }

    public record Definition(
            List<Entry> entries
    ) {

        public Definition {

            entries =
                    List.copyOf(
                            entries
                    );
        }
    }

    public record Entry(
            String rosterName,
            String npcId,
            DeclaredStatus declaredStatus,
            String note
    ) {
    }

    public enum DeclaredStatus {

        AUTO,

        NO_SUPPORTED_RELATION_FOUND,

        SPECIAL_ENTITY_PENDING,

        CONTINUITY_UNCERTAIN
    }

    private static final class DefinitionData {

        List<EntryData> entries;
    }

    private static final class EntryData {

        String rosterName;

        String npcId;

        String declaredStatus;

        String note;

        Entry toEntry() {

            if (rosterName == null
                    || rosterName.isBlank()) {

                throw new IllegalArgumentException(
                        "relationship review rosterName cannot be blank"
                );
            }

            requireId(
                    npcId,
                    "relationship review npcId"
            );

            return new Entry(
                    rosterName.trim(),
                    npcId.trim(),
                    enumValue(
                            declaredStatus
                    ),
                    note == null
                            ? ""
                            : note.trim()
            );
        }
    }

    private static DeclaredStatus enumValue(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return DeclaredStatus.AUTO;
        }

        try {

            return DeclaredStatus.valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new IllegalArgumentException(
                    "Unknown relationship review status "
                            + value,
                    exception
            );
        }
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
}

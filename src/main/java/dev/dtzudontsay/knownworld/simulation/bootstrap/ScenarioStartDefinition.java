package dev.dtzudontsay.knownworld.simulation.bootstrap;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ScenarioStartDefinition {

    private final String scenarioId;

    private final String anchorId;

    private final String displayName;

    private final int startYear;

    private final int startDayOfYear;

    private final int daysPerYear;

    private final String chronologyCertainty;

    private final String seasonId;

    private final String description;

    private final String essosSynchronizationNote;

    private final List<String> establishedFacts;

    private final List<String> notYetOccurred;

    private final List<String> sourceNotes;

    public ScenarioStartDefinition(
            String scenarioId,
            String anchorId,
            String displayName,
            int startYear,
            int startDayOfYear,
            int daysPerYear,
            String chronologyCertainty,
            String seasonId,
            String description,
            String essosSynchronizationNote,
            List<String> establishedFacts,
            List<String> notYetOccurred,
            List<String> sourceNotes
    ) {

        this.scenarioId =
                requireId(
                        scenarioId,
                        "scenarioId"
                );

        this.anchorId =
                requireId(
                        anchorId,
                        "anchorId"
                );

        this.displayName =
                requireText(
                        displayName,
                        "displayName"
                );

        if (daysPerYear <= 0) {

            throw new IllegalArgumentException(
                    "daysPerYear must be positive"
            );
        }

        if (startDayOfYear < 0
                || startDayOfYear >= daysPerYear) {

            throw new IllegalArgumentException(
                    "startDayOfYear must be within the configured year"
            );
        }

        this.startYear =
                startYear;

        this.startDayOfYear =
                startDayOfYear;

        this.daysPerYear =
                daysPerYear;

        this.chronologyCertainty =
                requireId(
                        chronologyCertainty,
                        "chronologyCertainty"
                );

        this.seasonId =
                requireId(
                        seasonId,
                        "seasonId"
                );

        this.description =
                requireText(
                        description,
                        "description"
                );

        this.essosSynchronizationNote =
                optionalText(
                        essosSynchronizationNote
                );

        this.establishedFacts =
                immutableTextList(
                        establishedFacts,
                        "establishedFacts"
                );

        this.notYetOccurred =
                immutableTextList(
                        notYetOccurred,
                        "notYetOccurred"
                );

        this.sourceNotes =
                immutableTextList(
                        sourceNotes,
                        "sourceNotes"
                );
    }

    public String scenarioId() {
        return scenarioId;
    }

    public String anchorId() {
        return anchorId;
    }

    public String displayName() {
        return displayName;
    }

    public int startYear() {
        return startYear;
    }

    public int startDayOfYear() {
        return startDayOfYear;
    }

    public int humanStartDay() {
        return startDayOfYear + 1;
    }

    public int daysPerYear() {
        return daysPerYear;
    }

    public String chronologyCertainty() {
        return chronologyCertainty;
    }

    public String seasonId() {
        return seasonId;
    }

    public String description() {
        return description;
    }

    public String essosSynchronizationNote() {
        return essosSynchronizationNote;
    }

    public List<String> establishedFacts() {
        return establishedFacts;
    }

    public List<String> notYetOccurred() {
        return notYetOccurred;
    }

    public List<String> sourceNotes() {
        return sourceNotes;
    }

    public long absoluteStartDay() {

        return ((long) startYear)
                * daysPerYear
                + startDayOfYear;
    }

    private static List<String> immutableTextList(
            List<String> values,
            String description
    ) {

        if (values == null) {
            return List.of();
        }

        return values.stream()
                .map(
                        value ->
                                requireText(
                                        value,
                                        description
                                )
                )
                .toList();
    }

    private static String requireId(
            String value,
            String description
    ) {

        String normalized =
                requireText(
                        value,
                        description
                )
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

    private static String requireText(
            String value,
            String description
    ) {

        Objects.requireNonNull(
                value,
                description
        );

        String trimmed =
                value.trim();

        if (trimmed.isEmpty()) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be blank"
            );
        }

        return trimmed;
    }

    private static String optionalText(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
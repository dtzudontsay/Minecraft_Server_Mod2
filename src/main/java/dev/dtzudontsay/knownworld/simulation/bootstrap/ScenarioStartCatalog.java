package dev.dtzudontsay.knownworld.simulation.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public final class ScenarioStartCatalog {

    private static final Gson GSON =
            new Gson();

    private static final String DEFAULT_SCENARIO =
            "data/knownworld/scenarios/agot_298_ac/scenario.json";

    private static ScenarioStartCatalog instance;

    private final ScenarioStartDefinition definition;

    private ScenarioStartCatalog(
            ScenarioStartDefinition definition
    ) {

        this.definition =
                definition;
    }

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        try {

            ScenarioFile scenario =
                    readJson(
                            DEFAULT_SCENARIO,
                            ScenarioFile.class
                    );

            if (scenario.id == null
                    || scenario.id.isBlank()) {

                throw new IllegalStateException(
                        "Scenario start cannot load because scenario.id is missing"
                );
            }

            if (scenario.startDefinition == null
                    || scenario.startDefinition.isBlank()) {

                throw new IllegalStateException(
                        "Scenario "
                                + scenario.id
                                + " does not define startDefinition"
                );
            }

            String resource =
                    resolveSibling(
                            DEFAULT_SCENARIO,
                            scenario.startDefinition
                    );

            StartData data =
                    readJson(
                            resource,
                            StartData.class
                    );

            ScenarioStartDefinition definition =
                    new ScenarioStartDefinition(
                            data.scenarioId,
                            data.anchorId,
                            data.displayName,
                            data.startYear,
                            data.startDayOfYear,
                            data.daysPerYear,
                            data.chronologyCertainty,
                            data.seasonId,
                            data.description,
                            data.essosSynchronizationNote,
                            data.establishedFacts,
                            data.notYetOccurred,
                            data.sourceNotes
                    );

            if (!scenario.id.equals(
                    definition.scenarioId()
            )) {

                throw new IllegalStateException(
                        "Scenario start definition belongs to "
                                + definition.scenarioId()
                                + " but scenario is "
                                + scenario.id
                );
            }

            instance =
                    new ScenarioStartCatalog(
                            definition
                    );

            KnownWorld.LOGGER.info(
                    "Scenario start locked: scenario={} anchor={} year={} day={} season={} certainty={}.",
                    definition.scenarioId(),
                    definition.anchorId(),
                    definition.startYear(),
                    definition.humanStartDay(),
                    definition.seasonId(),
                    definition.chronologyCertainty()
            );

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load scenario start definition",
                    exception
            );
        }
    }

    public static ScenarioStartCatalog get() {

        if (instance == null) {

            throw new IllegalStateException(
                    "Scenario start catalog has not been bootstrapped"
            );
        }

        return instance;
    }

    public ScenarioStartDefinition definition() {
        return definition;
    }

    public long daysSinceStart(
            CampaignCalendar calendar
    ) {

        if (calendar.daysPerYear()
                != definition.daysPerYear()) {

            throw new IllegalStateException(
                    "Campaign calendar uses "
                            + calendar.daysPerYear()
                            + " days/year but scenario start uses "
                            + definition.daysPerYear()
            );
        }

        return calendar.absoluteDay()
                - definition.absoluteStartDay();
    }

    public void validateCalendar(
            CampaignCalendar calendar
    ) {

        if (calendar.daysPerYear()
                != definition.daysPerYear()) {

            throw new IllegalStateException(
                    "Campaign calendar days-per-year does not match scenario start definition"
            );
        }

        if (calendar.absoluteDay()
                < definition.absoluteStartDay()) {

            throw new IllegalStateException(
                    "Campaign calendar is before the locked scenario start"
            );
        }
    }

    private static String resolveSibling(
            String parentResource,
            String child
    ) {

        if (child.startsWith(
                "data/"
        )) {

            return child;
        }

        int separator =
                parentResource.lastIndexOf(
                        '/'
                );

        String base =
                separator < 0
                        ? ""
                        : parentResource.substring(
                        0,
                        separator + 1
                );

        return base
                + child;
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        ScenarioStartCatalog.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Scenario start resource not found: "
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
                            "Scenario start JSON produced null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid scenario start JSON: "
                            + resource,
                    exception
            );
        }
    }

    private static final class ScenarioFile {

        String id;

        String startDefinition;
    }

    private static final class StartData {

        String scenarioId;

        String anchorId;

        String displayName;

        int startYear;

        int startDayOfYear;

        int daysPerYear;

        String chronologyCertainty;

        String seasonId;

        String description;

        String essosSynchronizationNote;

        java.util.List<String> establishedFacts;

        java.util.List<String> notYetOccurred;

        java.util.List<String> sourceNotes;
    }
}
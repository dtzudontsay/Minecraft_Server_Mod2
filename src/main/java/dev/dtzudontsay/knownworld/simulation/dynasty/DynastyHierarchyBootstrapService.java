package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DynastyHierarchyBootstrapService {

    private static final Gson GSON =
            new Gson();

    private static final String BASE =
            "data/knownworld/scenarios/agot_298_ac/";

    private static final String RESOURCE =
            BASE
                    + "dynasty_hierarchy.json";

    private DynastyHierarchyBootstrapService() {
    }

    public static Report apply(
            DynastyManager manager
    ) throws IOException {

        RootData root =
                read(
                        RESOURCE,
                        RootData.class
                );

        List<RegionRuleData> regions =
                root.regions == null
                        ? List.of()
                        : root.regions;

        int defaultAssignments =
                0;

        int overrideAssignments =
                0;

        int excluded =
                0;

        int inactive =
                0;

        for (
                RegionRuleData region :
                regions
        ) {

            String dynastyResource =
                    resolve(
                            BASE,
                            requireText(
                                    region.dynastyResource,
                                    "dynastyResource"
                            )
                    );

            DynastyFileData dynastyFile =
                    read(
                            dynastyResource,
                            DynastyFileData.class
                    );

            Dynasty defaultLiege =
                    requireAuthored(
                            manager,
                            requireText(
                                    region.defaultLiegeDynastyId,
                                    "defaultLiegeDynastyId"
                            )
                    );

            Set<String> excludedIds =
                    normalizeSet(
                            region.excludedDynastyIds
                    );

            List<DynastyEntryData> dynastyEntries =
                    dynastyFile.dynasties == null
                            ? List.of()
                            : dynastyFile.dynasties;

            for (
                    DynastyEntryData entry :
                    dynastyEntries
            ) {

                Dynasty dynasty =
                        requireAuthored(
                                manager,
                                requireText(
                                        entry.id,
                                        dynastyResource + ".dynasty.id"
                                )
                        );

                if (!dynasty.activeAtScenarioStart()) {

                    manager.setLiege(
                            dynasty.id(),
                            null
                    );

                    inactive++;

                    continue;
                }

                if (dynasty.id()
                        .equals(
                                defaultLiege.id()
                        )
                        || excludedIds.contains(
                        normalize(
                                dynasty.authoredId()
                        )
                )) {

                    manager.setLiege(
                            dynasty.id(),
                            null
                    );

                    excluded++;

                    continue;
                }

                manager.setLiege(
                        dynasty.id(),
                        defaultLiege.id()
                );

                defaultAssignments++;
            }

            List<OverrideData> overrides =
                    region.overrides == null
                            ? List.of()
                            : region.overrides;

            for (
                    OverrideData override :
                    overrides
            ) {

                Dynasty vassal =
                        requireAuthored(
                                manager,
                                requireText(
                                        override.vassalDynastyId,
                                        "override.vassalDynastyId"
                                )
                        );

                if (!vassal.activeAtScenarioStart()) {
                    continue;
                }

                Dynasty liege =
                        requireAuthored(
                                manager,
                                requireText(
                                        override.liegeDynastyId,
                                        "override.liegeDynastyId"
                                )
                        );

                manager.setLiege(
                        vassal.id(),
                        liege.id()
                );

                overrideAssignments++;
            }
        }

        validateNoCycles(
                manager
        );

        KnownWorld.LOGGER.info(
                "Dynasty hierarchy bootstrap complete: regions={}, defaults={}, overrides={}, excludedRoots={}, inactiveCleared={}.",
                regions.size(),
                defaultAssignments,
                overrideAssignments,
                excluded,
                inactive
        );

        return new Report(
                regions.size(),
                defaultAssignments,
                overrideAssignments,
                excluded,
                inactive
        );
    }

    private static void validateNoCycles(
            DynastyManager manager
    ) {

        for (
                Dynasty dynasty :
                manager.all()
        ) {

            Set<DynastyId> visited =
                    new LinkedHashSet<>();

            Dynasty current =
                    dynasty;

            int safety =
                    0;

            while (
                    current != null
                            && current.liegeDynasty() != null
            ) {

                if (!visited.add(
                        current.id()
                )) {

                    throw new IllegalStateException(
                            "Dynasty liege cycle detected from "
                                    + dynasty.authoredId()
                    );
                }

                DynastyId liegeId =
                        current.liegeDynasty();

                current =
                        manager.find(
                                        liegeId
                                )
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Dynasty "
                                                                + dynasty.authoredId()
                                                                + " references missing liege "
                                                                + liegeId
                                                )
                                );

                safety++;

                if (safety > manager.size() + 1) {

                    throw new IllegalStateException(
                            "Dynasty liege hierarchy appears cyclic"
                    );
                }
            }
        }
    }

    private static Dynasty requireAuthored(
            DynastyManager manager,
            String authoredId
    ) {

        return manager.findAuthored(
                        authoredId
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown authored dynasty "
                                                + authoredId
                                )
                );
    }

    private static Set<String> normalizeSet(
            List<String> values
    ) {

        if (values == null
                || values.isEmpty()) {

            return Set.of();
        }

        Set<String> result =
                new LinkedHashSet<>();

        for (
                String value :
                values
        ) {

            result.add(
                    normalize(
                            value
                    )
            );
        }

        return Set.copyOf(
                result
        );
    }

    private static String resolve(
            String base,
            String child
    ) {

        if (child.startsWith(
                "data/"
        )) {

            return child;
        }

        return base
                + child;
    }

    private static String normalize(
            String value
    ) {

        return requireText(
                value,
                "id"
        )
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private static String requireText(
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

        return value.trim();
    }

    private static <T> T read(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        DynastyHierarchyBootstrapService.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Dynasty hierarchy resource not found: "
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
                            "Dynasty hierarchy JSON returned null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid dynasty hierarchy JSON "
                            + resource,
                    exception
            );
        }
    }

    public record Report(
            int regionCount,
            int defaultAssignmentCount,
            int overrideAssignmentCount,
            int excludedRootCount,
            int inactiveClearedCount
    ) {
    }

    private static final class RootData {
        List<RegionRuleData> regions;
    }

    private static final class RegionRuleData {
        String dynastyResource;
        String defaultLiegeDynastyId;
        List<String> excludedDynastyIds;
        List<OverrideData> overrides;
    }

    private static final class OverrideData {
        String vassalDynastyId;
        String liegeDynastyId;
        String sourceNote;
    }

    private static final class DynastyFileData {
        List<DynastyEntryData> dynasties;
    }

    private static final class DynastyEntryData {
        String id;
    }
}
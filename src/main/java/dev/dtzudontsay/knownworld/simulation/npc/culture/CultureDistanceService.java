package dev.dtzudontsay.knownworld.simulation.npc.culture;

import dev.dtzudontsay.knownworld.world.reference.ReferenceEntry;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CultureDistanceService {

    public double distance(
            String firstCultureId,
            String secondCultureId
    ) {

        ReferenceEntry first =
                requireCulture(
                        firstCultureId
                );

        ReferenceEntry second =
                requireCulture(
                        secondCultureId
                );

        if (first.id()
                .equals(
                        second.id()
                )) {

            return 0.0;
        }

        List<String> firstPath =
                pathToRoot(
                        first.id()
                );

        List<String> secondPath =
                pathToRoot(
                        second.id()
                );

        int shared =
                sharedPrefixLength(
                        firstPath,
                        secondPath
                );

        /*
         * No common culture ancestor in the reference graph.
         *
         * Example:
         * Westerosi branch vs Dothraki branch.
         */
        if (shared == 0) {
            return 1.0;
        }

        int firstRemaining =
                firstPath.size()
                        - shared;

        int secondRemaining =
                secondPath.size()
                        - shared;

        int totalSteps =
                firstRemaining
                        + secondRemaining;

        /*
         * Direct parent/subculture relationship.
         *
         * Example:
         * Westerosi -> Northman.
         */
        if (totalSteps == 1) {
            return 0.20;
        }

        /*
         * Sibling subcultures under the same parent.
         *
         * Example:
         * Northman <-> Valeman.
         */
        if (totalSteps == 2) {
            return 0.35;
        }

        return clampUnit(
                0.35
                        + totalSteps
                        * 0.12
        );
    }

    public boolean sameCultureFamily(
            String firstCultureId,
            String secondCultureId
    ) {

        ReferenceEntry first =
                requireCulture(
                        firstCultureId
                );

        ReferenceEntry second =
                requireCulture(
                        secondCultureId
                );

        List<String> firstPath =
                pathToRoot(
                        first.id()
                );

        List<String> secondPath =
                pathToRoot(
                        second.id()
                );

        return sharedPrefixLength(
                firstPath,
                secondPath
        ) > 0;
    }

    private static List<String> pathToRoot(
            String cultureId
    ) {

        WorldReferenceCatalog catalog =
                WorldReferenceCatalog.get();

        List<String> reversed =
                new ArrayList<>();

        ReferenceEntry current =
                catalog.culture(
                                cultureId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown culture "
                                                        + cultureId
                                        )
                        );

        int safety =
                0;

        while (current != null) {

            reversed.add(
                    current.id()
            );

            if (!current.hasParent()) {
                break;
            }

            /*
             * Store the values before current is reassigned.
             *
             * This avoids capturing the mutable loop variable
             * inside the exception lambda.
             */
            String currentId =
                    current.id();

            String parentId =
                    current.parentId();

            ReferenceEntry parent =
                    catalog.culture(
                                    parentId
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Culture "
                                                            + currentId
                                                            + " references missing parent "
                                                            + parentId
                                            )
                            );

            current =
                    parent;

            safety++;

            if (safety > 50) {

                throw new IllegalStateException(
                        "Culture hierarchy cycle detected"
                );
            }
        }

        List<String> result =
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

    private static int sharedPrefixLength(
            List<String> first,
            List<String> second
    ) {

        int maximum =
                Math.min(
                        first.size(),
                        second.size()
                );

        int result =
                0;

        while (
                result < maximum
                        && Objects.equals(
                        first.get(
                                result
                        ),
                        second.get(
                                result
                        )
                )
        ) {

            result++;
        }

        return result;
    }

    private static ReferenceEntry requireCulture(
            String cultureId
    ) {

        if (cultureId == null
                || cultureId.isBlank()) {

            throw new IllegalArgumentException(
                    "Culture ID cannot be empty"
            );
        }

        return WorldReferenceCatalog.get()
                .culture(
                        cultureId
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown culture "
                                                + cultureId
                                )
                );
    }

    private static double clampUnit(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
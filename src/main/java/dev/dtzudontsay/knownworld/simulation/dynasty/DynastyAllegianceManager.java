package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DynastyAllegianceManager {

    private final DynastyManager dynasties;

    /*
     * Runtime deviations from the authored/de-jure hierarchy.
     *
     * A present key with null value means:
     * "explicitly independent right now."
     */
    private final Map<DynastyId, DynastyId> overrides =
            new LinkedHashMap<>();

    public DynastyAllegianceManager(
            DynastyManager dynasties
    ) {

        this.dynasties =
                Objects.requireNonNull(
                        dynasties,
                        "dynasties"
                );
    }

    public synchronized Optional<Dynasty> currentLiege(
            DynastyId vassal
    ) {

        Dynasty dynasty =
                dynasties.require(
                        vassal
                );

        DynastyId liegeId;

        if (overrides.containsKey(
                vassal
        )) {

            liegeId =
                    overrides.get(
                            vassal
                    );

        } else {

            liegeId =
                    dynasty.liegeDynasty();
        }

        return liegeId == null
                ? Optional.empty()
                : dynasties.find(
                liegeId
        );
    }

    public synchronized Optional<Dynasty> deJureLiege(
            DynastyId vassal
    ) {

        Dynasty dynasty =
                dynasties.require(
                        vassal
                );

        return dynasty.liegeDynasty() == null
                ? Optional.empty()
                : dynasties.find(
                dynasty.liegeDynasty()
        );
    }

    public synchronized boolean hasOverride(
            DynastyId vassal
    ) {

        dynasties.require(
                vassal
        );

        return overrides.containsKey(
                vassal
        );
    }

    public synchronized boolean isExplicitlyIndependent(
            DynastyId vassal
    ) {

        dynasties.require(
                vassal
        );

        return overrides.containsKey(
                vassal
        )
                && overrides.get(
                vassal
        ) == null;
    }

    public synchronized void setCurrentLiege(
            DynastyId vassal,
            DynastyId liege
    ) {

        Dynasty vassalDynasty =
                dynasties.require(
                        vassal
                );

        if (liege != null) {

            Dynasty liegeDynasty =
                    dynasties.require(
                            liege
                    );

            if (vassalDynasty.id()
                    .equals(
                            liegeDynasty.id()
                    )) {

                throw new IllegalArgumentException(
                        "Dynasty cannot be its own current liege"
                );
            }
        }

        ensureNoCycle(
                vassal,
                liege
        );

        overrides.put(
                vassal,
                liege
        );
    }

    public synchronized void clearOverride(
            DynastyId vassal
    ) {

        dynasties.require(
                vassal
        );

        overrides.remove(
                vassal
        );
    }

    public synchronized void registerLoadedOverride(
            DynastyId vassal,
            DynastyId liege
    ) {

        dynasties.require(
                vassal
        );

        if (liege != null) {

            dynasties.require(
                    liege
            );
        }

        if (overrides.containsKey(
                vassal
        )) {

            throw new IllegalStateException(
                    "Duplicate dynasty allegiance override for "
                            + vassal
            );
        }

        ensureNoCycle(
                vassal,
                liege
        );

        overrides.put(
                vassal,
                liege
        );
    }

    public synchronized List<Dynasty> directVassals(
            DynastyId liege
    ) {

        dynasties.require(
                liege
        );

        List<Dynasty> result =
                new ArrayList<>();

        for (
                Dynasty dynasty :
                dynasties.all()
        ) {

            Optional<Dynasty> current =
                    currentLiege(
                            dynasty.id()
                    );

            if (current.isPresent()
                    && current.get()
                    .id()
                    .equals(
                            liege
                    )) {

                result.add(
                        dynasty
                );
            }
        }

        result.sort(
                Comparator.comparing(
                        Dynasty::authoredId,
                        Comparator.nullsLast(
                                String::compareTo
                        )
                )
        );

        return List.copyOf(
                result
        );
    }

    public synchronized List<Dynasty> liegeChain(
            DynastyId dynastyId
    ) {

        dynasties.require(
                dynastyId
        );

        List<Dynasty> result =
                new ArrayList<>();

        Set<DynastyId> visited =
                new LinkedHashSet<>();

        DynastyId current =
                dynastyId;

        int safety =
                0;

        while (true) {

            if (!visited.add(
                    current
            )) {

                throw new IllegalStateException(
                        "Current dynasty allegiance cycle detected"
                );
            }

            Optional<Dynasty> liege =
                    currentLiege(
                            current
                    );

            if (liege.isEmpty()) {
                break;
            }

            Dynasty next =
                    liege.get();

            result.add(
                    next
            );

            current =
                    next.id();

            safety++;

            if (safety > dynasties.size() + 1) {

                throw new IllegalStateException(
                        "Current dynasty allegiance hierarchy appears cyclic"
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public synchronized Dynasty rootLiege(
            DynastyId dynasty
    ) {

        Dynasty root =
                dynasties.require(
                        dynasty
                );

        for (
                Dynasty liege :
                liegeChain(
                        dynasty
                )
        ) {

            root =
                    liege;
        }

        return root;
    }

    public synchronized boolean isVassalOf(
            DynastyId possibleVassal,
            DynastyId possibleLiege
    ) {

        dynasties.require(
                possibleVassal
        );

        dynasties.require(
                possibleLiege
        );

        for (
                Dynasty liege :
                liegeChain(
                        possibleVassal
                )
        ) {

            if (liege.id()
                    .equals(
                            possibleLiege
                    )) {

                return true;
            }
        }

        return false;
    }

    public synchronized List<OverrideEntry> overrides() {

        return overrides.entrySet()
                .stream()
                .map(
                        entry ->
                                new OverrideEntry(
                                        entry.getKey(),
                                        entry.getValue()
                                )
                )
                .sorted(
                        Comparator.comparing(
                                OverrideEntry::vassal
                        )
                )
                .toList();
    }

    public synchronized int overrideCount() {

        return overrides.size();
    }

    private void ensureNoCycle(
            DynastyId changedVassal,
            DynastyId proposedLiege
    ) {

        if (proposedLiege == null) {
            return;
        }

        DynastyId current =
                proposedLiege;

        Set<DynastyId> visited =
                new LinkedHashSet<>();

        int safety =
                0;

        while (current != null) {

            if (current.equals(
                    changedVassal
            )) {

                throw new IllegalArgumentException(
                        "Current dynasty allegiance would create a cycle"
                );
            }

            if (!visited.add(
                    current
            )) {

                throw new IllegalStateException(
                        "Existing current dynasty allegiance is cyclic"
                );
            }

            current =
                    effectiveLiegeId(
                            current,
                            changedVassal,
                            proposedLiege
                    );

            safety++;

            if (safety > dynasties.size() + 1) {

                throw new IllegalStateException(
                        "Current dynasty allegiance hierarchy appears cyclic"
                );
            }
        }
    }

    private DynastyId effectiveLiegeId(
            DynastyId dynastyId,
            DynastyId changedVassal,
            DynastyId proposedLiege
    ) {

        if (dynastyId.equals(
                changedVassal
        )) {

            return proposedLiege;
        }

        if (overrides.containsKey(
                dynastyId
        )) {

            return overrides.get(
                    dynastyId
            );
        }

        return dynasties.require(
                        dynastyId
                )
                .liegeDynasty();
    }

    public record OverrideEntry(
            DynastyId vassal,
            DynastyId liege
    ) {
    }
}
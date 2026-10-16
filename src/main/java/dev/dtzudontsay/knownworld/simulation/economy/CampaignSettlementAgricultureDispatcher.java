package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Campaign-wide agricultural dispatcher.
 *
 * Groups existing farm enrollments by SettlementId.
 *
 * Batch 22.21:
 * Each settlement can independently opt into
 * anonymous-population food consumption.
 *
 * A single settlement runner serves one or more farms
 * and one complete set of consumers.
 *
 * All settlements are inspected before execution.
 *
 * No automatic Fabric hooks or implicit enrollment.
 *
 * Cross-settlement economic execution remains
 * non-atomic; partial failures block further use.
 */
public final class CampaignSettlementAgricultureDispatcher
        implements CampaignAgricultureDayCoordinator.DayOperation {

    public record SettlementPreview(
            SettlementId settlement,
            MultiFarmSettlementFoodCycle.Result food
    ) {
        public SettlementPreview {
            Objects.requireNonNull(settlement);
            Objects.requireNonNull(food);
        }
    }

    public record Readiness(
            long startDayInclusive,
            long endDayExclusive,
            List<SettlementPreview> settlements
    ) {
        public Readiness {
            settlements = List.copyOf(settlements);
        }
    }

    public record SettlementResult(
            SettlementId settlement,
            MultiFarmSettlementFoodCycle.Result food
    ) {
        public SettlementResult {
            Objects.requireNonNull(settlement);
            Objects.requireNonNull(food);
        }
    }

    public record DayResult(
            long startDayInclusive,
            long endDayExclusive,
            long simulationTick,
            List<SettlementResult> settlements
    ) {
        public DayResult {
            settlements = List.copyOf(settlements);
        }
    }

    private final EconomyAuthorityService.Loaded economy;

    private final List<AgriculturalDayEnrollment> enrollments;

    private final List<MultiFarmSettlementAgricultureRunner>
            settlementRunners;

    private boolean blocked;
    private DayResult lastResult;

    /**
     * Original dispatcher constructor.
     *
     * Preserves existing behavior for campaigns
     * using only represented NPC households.
     */
    public CampaignSettlementAgricultureDispatcher(
            Path directory,
            ResourceCatalog catalog,
            EconomyAuthorityService.Loaded economy,
            EconomyReferenceValidator validator,
            SettlementFoodScenarioAdapter adapter,
            AgriculturalIntervalLedger ledger,
            List<AgriculturalDayEnrollment> enrollments
    ) {
        this(
                directory,
                catalog,
                economy,
                validator,
                adapter,
                ledger,
                enrollments,
                Map.of()
        );
    }

    /**
     * Batch 22.21 constructor.
     *
     * Each map key must be an enrolled settlement.
     *
     * A map entry tells the corresponding settlement
     * runner to include anonymous residents through
     * the existing SettlementCohortFoodIntegration.
     *
     * Settlements without entries retain their
     * original named-household-only behavior.
     */
    public CampaignSettlementAgricultureDispatcher(
            Path directory,
            ResourceCatalog catalog,
            EconomyAuthorityService.Loaded economy,
            EconomyReferenceValidator validator,
            SettlementFoodScenarioAdapter adapter,
            AgriculturalIntervalLedger ledger,
            List<AgriculturalDayEnrollment> enrollments,
            Map<SettlementId, SettlementCohortFoodIntegration>
                    cohortIntegrations
    ) {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(catalog, "catalog");

        this.economy = Objects.requireNonNull(
                economy, "economy"
        );

        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(adapter, "adapter");
        Objects.requireNonNull(ledger, "ledger");
        Objects.requireNonNull(enrollments, "enrollments");
        Objects.requireNonNull(
                cohortIntegrations, "cohortIntegrations"
        );

        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one agricultural holding "
                            + "must be enrolled"
            );
        }

        Map<SettlementId, SettlementCohortFoodIntegration>
                configuredCohorts =
                Map.copyOf(cohortIntegrations);

        List<AgriculturalDayEnrollment> sorted =
                new ArrayList<>(List.copyOf(enrollments));

        sorted.sort(
                Comparator.comparingLong(
                        (AgriculturalDayEnrollment farm) ->
                                farm.settlement().value()
                ).thenComparingLong(
                        farm -> farm.holding().value()
                )
        );

        Set<HoldingId> holdingIds = new HashSet<>();
        Set<InventoryId> farmInventoryIds = new HashSet<>();

        Map<Long, List<AgriculturalDayEnrollment>> grouped =
                new TreeMap<>();

        for (AgriculturalDayEnrollment farm : sorted) {

            if (!holdingIds.add(farm.holding())) {
                throw new IllegalArgumentException(
                        "Holding enrolled more than once: "
                                + farm.holding()
                );
            }

            if (!farmInventoryIds.add(
                    farm.farmInventory())) {
                throw new IllegalArgumentException(
                        "Farm inventory enrolled more than once: "
                                + farm.farmInventory()
                );
            }

            grouped.computeIfAbsent(
                    farm.settlement().value(),
                    unused -> new ArrayList<>()
            ).add(farm);
        }

        /*
         * Reject cohort configurations targeting
         * settlements without any enrolled farm.
         *
         * A misspelled or unrelated SettlementId must
         * not silently disable its residents' food demand.
         */
        Set<InventoryId> cohortPantryIds = new HashSet<>();

        for (var entry : configuredCohorts.entrySet()) {

            SettlementId settlementId = entry.getKey();

            if (!grouped.containsKey(
                    settlementId.value())) {
                throw new IllegalArgumentException(
                        "Cohort food integration targets "
                                + "an unenrolled settlement: "
                                + settlementId
                );
            }

            InventoryId pantry =
                    entry.getValue().cohortPantry();

            if (pantry != null) {

                if (!cohortPantryIds.add(pantry)) {
                    throw new IllegalArgumentException(
                            "Aggregate pantry configured for "
                                    + "multiple settlements: "
                                    + pantry
                    );
                }

                if (farmInventoryIds.contains(pantry)) {
                    throw new IllegalArgumentException(
                            "Aggregate pantry also enrolled "
                                    + "as a farm: "
                                    + pantry
                    );
                }
            }
        }

        List<MultiFarmSettlementAgricultureRunner> runners =
                new ArrayList<>();

        /*
         * Settlement order is deterministic because
         * grouped is a TreeMap keyed by SettlementId.
         */
        for (List<AgriculturalDayEnrollment> group :
                grouped.values()) {

            SettlementId settlementId =
                    group.get(0).settlement();

            runners.add(
                    new MultiFarmSettlementAgricultureRunner(
                            directory,
                            catalog,
                            economy,
                            validator,
                            adapter,
                            ledger,
                            group,
                            configuredCohorts.get(settlementId)
                    )
            );
        }

        this.enrollments = List.copyOf(sorted);
        this.settlementRunners = List.copyOf(runners);
    }

    public List<AgriculturalDayEnrollment> enrollments() {
        return enrollments;
    }

    public List<SettlementId> settlements() {
        return settlementRunners.stream()
                .map(
                        MultiFarmSettlementAgricultureRunner
                                ::settlement
                )
                .toList();
    }

    public synchronized boolean blocked() {
        return blocked;
    }

    public synchronized DayResult lastResult() {
        return lastResult;
    }

    /**
     * Read-only campaign-wide preflight.
     *
     * Named households, anonymous residents,
     * inventories, agriculture intervals and journal
     * capacity are checked before any execution.
     */
    public synchronized Readiness inspect(
            long startDayInclusive,
            long endDayExclusive
    ) throws IOException {

        requireNotBlocked();

        InventoryManager manager = economy.manager();

        synchronized (manager) {
            return inspectLocked(
                    startDayInclusive,
                    endDayExclusive
            );
        }
    }

    private Readiness inspectLocked(
            long startDayInclusive,
            long endDayExclusive
    ) throws IOException {

        List<SettlementPreview> previews =
                new ArrayList<>();

        for (MultiFarmSettlementAgricultureRunner runner :
                settlementRunners) {

            MultiFarmSettlementFoodCycle.Result forecast =
                    runner.inspect(
                            startDayInclusive,
                            endDayExclusive
                    );

            previews.add(
                    new SettlementPreview(
                            runner.settlement(),
                            forecast
                    )
            );
        }

        return new Readiness(
                startDayInclusive,
                endDayExclusive,
                previews
        );
    }

    /**
     * Execute one complete campaign day.
     *
     * Phase 1:
     * Preflight every settlement, including all
     * anonymous-population integrations.
     *
     * Phase 2:
     * Execute each settlement in ID order through
     * its existing guarded runner.
     *
     * This is not a cross-settlement transaction.
     */
    public synchronized DayResult executeDay(
            long startDayInclusive,
            long endDayExclusive,
            long simulationTick
    ) throws IOException {

        requireNotBlocked();

        try {

            if (simulationTick < 0L) {
                throw new IllegalArgumentException(
                        "Simulation tick cannot be negative"
                );
            }

            InventoryManager manager = economy.manager();

            synchronized (manager) {

                /*
                 * Critical:
                 * No farm ledger is claimed and no grain
                 * is changed until ALL settlements pass.
                 */
                inspectLocked(
                        startDayInclusive,
                        endDayExclusive
                );

                List<SettlementResult> results =
                        new ArrayList<>();

                for (MultiFarmSettlementAgricultureRunner runner :
                        settlementRunners) {

                    MultiFarmSettlementFoodCycle.Result food =
                            runner.executeDay(
                                    startDayInclusive,
                                    endDayExclusive,
                                    simulationTick
                            );

                    results.add(
                            new SettlementResult(
                                    runner.settlement(),
                                    food
                            )
                    );
                }

                DayResult completed = new DayResult(
                        startDayInclusive,
                        endDayExclusive,
                        simulationTick,
                        results
                );

                lastResult = completed;

                return completed;
            }

        } catch (IOException | RuntimeException | Error exception) {
            blocked = true;
            throw exception;
        }
    }

    private void requireNotBlocked() {
        if (blocked) {
            throw new IllegalStateException(
                    "Campaign agricultural dispatcher is "
                            + "blocked after a failed operation"
            );
        }
    }

    public CampaignAgricultureDayCoordinator
    strictCoordinator(long initialCampaignDay) {

        return new CampaignAgricultureDayCoordinator(
                initialCampaignDay,
                this
        );
    }

    @Override
    public void execute(
            long startDayInclusive,
            long endDayExclusive,
            long simulationTick
    ) {
        try {
            executeDay(
                    startDayInclusive,
                    endDayExclusive,
                    simulationTick
            );

        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Campaign agricultural dispatch failed",
                    exception
            );
        }
    }
}
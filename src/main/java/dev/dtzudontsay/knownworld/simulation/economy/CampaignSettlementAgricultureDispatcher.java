
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
 * Batch 22.13.
 *
 * Unified campaign-day agricultural dispatcher.
 *
 * Groups holding enrollments by existing SettlementId.
 *
 * One MultiFarmSettlementAgricultureRunner is used
 * per settlement, even when the settlement contains
 * only one enrolled farm.
 *
 * Household demand is calculated once per settlement.
 *
 * All settlement groups are preflighted before
 * the first group is executed.
 *
 * No automatic Fabric registration or enrollment.
 *
 * Cross-settlement execution is NOT an atomic
 * transaction. Unexpected partial failures require
 * manual recovery.
 */
public final class CampaignSettlementAgricultureDispatcher
        implements CampaignAgricultureDayCoordinator.DayOperation {

    public record SettlementPreview(
            SettlementId settlement,
            MultiFarmSettlementFoodCycle.Result food
    ) {
        public SettlementPreview {
            Objects.requireNonNull(settlement, "settlement");
            Objects.requireNonNull(food, "food");
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
            Objects.requireNonNull(settlement, "settlement");
            Objects.requireNonNull(food, "food");
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

    public CampaignSettlementAgricultureDispatcher(
            Path directory,
            ResourceCatalog catalog,
            EconomyAuthorityService.Loaded economy,
            EconomyReferenceValidator validator,
            SettlementFoodScenarioAdapter adapter,
            AgriculturalIntervalLedger ledger,
            List<AgriculturalDayEnrollment> enrollments
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

        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one agricultural holding "
                            + "must be enrolled"
            );
        }

        List<AgriculturalDayEnrollment> sorted =
                new ArrayList<>(List.copyOf(enrollments));

        /*
         * Global deterministic ordering:
         *
         * 1. SettlementId
         * 2. HoldingId
         */
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
                    farm.farmInventory()
            )) {
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

        List<MultiFarmSettlementAgricultureRunner> runners =
                new ArrayList<>();

        for (List<AgriculturalDayEnrollment> group :
                grouped.values()) {

            /*
             * The same runner handles ONE farm and
             * MANY farms.
             *
             * No separate household consumption path
             * is selected for single-farm settlements.
             */
            runners.add(
                    new MultiFarmSettlementAgricultureRunner(
                            directory,
                            catalog,
                            economy,
                            validator,
                            adapter,
                            ledger,
                            group
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
     * Global read-only preflight.
     *
     * No settlement may be executed unless every
     * enrolled settlement first passes inspection.
     *
     * This includes farm/household identities,
     * inventory balances, journal capacity, and
     * persistent interval progression.
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
     * Execute one complete campaign-day transition.
     *
     * Phase 1: inspect ALL settlements.
     * Phase 2: execute each settlement in ID order.
     *
     * This ordering is deterministic but not
     * cross-settlement atomic.
     *
     * Any failure blocks this dispatcher instance.
     * Durable interval state prevents unsafe retry
     * after a process restart.
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
                 * No mutation or ledger claim occurs
                 * until ALL groups are ready.
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
                    "Campaign agricultural dispatcher "
                            + "is blocked after a failed operation"
            );
        }
    }

    /**
     * Existing strict campaign-day coordinator.
     *
     * Both normal and accelerated simulation can
     * dispatch through the same callback contract.
     *
     * Construction is explicit; no live-world
     * registration occurs here.
     */
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

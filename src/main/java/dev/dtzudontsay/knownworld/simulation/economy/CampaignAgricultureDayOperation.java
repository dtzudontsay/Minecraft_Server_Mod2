
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Batch 22.11.
 *
 * Executes one campaign day of agricultural
 * activity for an explicitly enrolled collection
 * of holdings.
 *
 * Reuses:
 * - CampaignAgricultureDayCoordinator.DayOperation
 * - SettlementFoodScenarioAdapter
 * - SettlementAgricultureRunner
 * - AgriculturalIntervalLedger
 * - EconomyAuthorityService
 *
 * Does not register a Fabric lifecycle callback.
 * Does not automatically enroll any holdings.
 */
public final class CampaignAgricultureDayOperation
        implements CampaignAgricultureDayCoordinator.DayOperation {

    public record FarmReadiness(
            HoldingId holding,
            long representedResidents,
            int householdCount
    ) {
    }

    public record Readiness(
            long startDayInclusive,
            long endDayExclusive,
            List<FarmReadiness> farms
    ) {
        public Readiness {
            farms = List.copyOf(farms);
        }
    }

    public record FarmResult(
            HoldingId holding,
            SettlementFoodCycle.Result food
    ) {
        public FarmResult {
            Objects.requireNonNull(holding);
            Objects.requireNonNull(food);
        }
    }

    public record DayResult(
            long startDayInclusive,
            long endDayExclusive,
            long simulationTick,
            List<FarmResult> farms
    ) {
        public DayResult {
            farms = List.copyOf(farms);
        }
    }

    private final EconomyAuthorityService.Loaded economy;
    private final SettlementFoodScenarioAdapter adapter;
    private final AgriculturalIntervalLedger ledger;
    private final SettlementAgricultureRunner runner;
    private final List<AgriculturalDayEnrollment> enrollments;

    private DayResult lastResult;

    public CampaignAgricultureDayOperation(
            EconomyAuthorityService.Loaded economy,
            SettlementFoodScenarioAdapter adapter,
            AgriculturalIntervalLedger ledger,
            SettlementAgricultureRunner runner,
            List<AgriculturalDayEnrollment> enrollments
    ) {
        this.economy = Objects.requireNonNull(
                economy, "economy"
        );
        this.adapter = Objects.requireNonNull(
                adapter, "adapter"
        );
        this.ledger = Objects.requireNonNull(
                ledger, "ledger"
        );
        this.runner = Objects.requireNonNull(
                runner, "runner"
        );

        Objects.requireNonNull(
                enrollments, "enrollments"
        );

        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one farm must be enrolled"
            );
        }

        List<AgriculturalDayEnrollment> sorted =
                new ArrayList<>(
                        List.copyOf(enrollments)
                );

        sorted.sort(
                Comparator.comparingLong(
                        enrollment ->
                                enrollment.holding().value()
                )
        );

        Set<HoldingId> holdings = new HashSet<>();
        Set<InventoryId> inventories = new HashSet<>();
        Set<SettlementId> settlements = new HashSet<>();

        for (AgriculturalDayEnrollment enrollment :
                sorted) {

            if (!holdings.add(enrollment.holding())) {
                throw new IllegalArgumentException(
                        "Duplicate agricultural holding: "
                                + enrollment.holding()
                );
            }

            if (!inventories.add(
                    enrollment.farmInventory()
            )) {
                throw new IllegalArgumentException(
                        "Duplicate agricultural farm inventory"
                );
            }

            /*
             * The current SettlementFoodCycle models
             * one farm supplying all represented
             * households of its settlement.
             *
             * Two farms in one settlement would
             * charge household consumption twice.
             *
             * A real multi-farm allocation model
             * is required before lifting this guard.
             */
            if (!settlements.add(
                    enrollment.settlement()
            )) {
                throw new IllegalArgumentException(
                        "Multiple agricultural farms "
                                + "in one settlement are not yet "
                                + "supported safely"
                );
            }
        }

        this.enrollments = List.copyOf(sorted);
    }

    public List<AgriculturalDayEnrollment> enrollments() {
        return enrollments;
    }

    public synchronized DayResult lastResult() {
        return lastResult;
    }

    /**
     * Readiness inspection does not create any
     * economic transactions or ledger claims.
     *
     * Requires explicit snapshot-primary authority,
     * a readable agricultural interval ledger,
     * correctly referenced farms and households,
     * and sufficient maximum checkpoint capacity.
     */
    public Readiness inspect(
            long startDayInclusive,
            long endDayExclusive
    ) throws IOException {

        InventoryManager manager = economy.manager();

        synchronized (manager) {
            return inspectLocked(
                    manager,
                    startDayInclusive,
                    endDayExclusive
            );
        }
    }

    private Readiness inspectLocked(
            InventoryManager manager,
            long startDayInclusive,
            long endDayExclusive
    ) throws IOException {

        if (startDayInclusive < 0L
                || endDayExclusive <= startDayInclusive
                || startDayInclusive == Long.MAX_VALUE
                || endDayExclusive
                != startDayInclusive + 1L) {
            throw new IllegalArgumentException(
                    "Agricultural execution requires "
                            + "exactly one campaign day"
            );
        }

        if (economy.mode()
                != EconomyAuthorityService.Mode
                .SNAPSHOT_PRIMARY) {
            throw new IOException(
                    "Agricultural day execution requires "
                            + "snapshot-primary economy authority"
            );
        }

        if (manager.transactionCount()
                > EconomyJournalContinuity
                .DEFAULT_RETAINED_TRANSACTIONS) {
            throw new IOException(
                    "Pending economic journal exceeds "
                            + "snapshot checkpoint retention limit"
            );
        }

        List<FarmReadiness> readiness =
                new ArrayList<>();

        for (AgriculturalDayEnrollment enrollment :
                enrollments) {

            AgriculturalInterval interval =
                    enrollment.interval(
                            startDayInclusive,
                            endDayExclusive
                    );

            /*
             * Verify durable progress before any
             * economic mutation is attempted.
             *
             * A pending interval is ambiguous
             * and cannot be automatically replayed.
             */
            var progress =
                    ledger.progress(
                            enrollment.holding()
                    );

            if (progress.isPresent()) {

                if (progress.get().pending()) {
                    throw new IOException(
                            "Prepared agricultural interval "
                                    + "requires manual recovery for "
                                    + enrollment.holding()
                    );
                }

                /*
                 * Even a previously completed day
                 * is considered unsafe for a fresh
                 * strict execution operation.
                 *
                 * The caller's campaign-day cursor
                 * must agree with durable progress.
                 */
                if (progress.get()
                        .lastCompletedEndDay()
                        != startDayInclusive) {
                    throw new IOException(
                            "Agricultural ledger and "
                                    + "campaign day disagree for "
                                    + enrollment.holding()
                    );
                }
            }

            SettlementFoodScenarioAdapter.Resolved
                    resolved = adapter.resolve(
                    manager,
                    enrollment.holding(),
                    enrollment.farmInventory(),
                    enrollment.settlement(),
                    interval,
                    enrollment.harvestInputs(),
                    enrollment.protectedReserveKg(),
                    enrollment.gramsPerPersonPerDay()
            );

            if (resolved.unassignedResidents() != 0L) {
                throw new IllegalStateException(
                        "Unassigned living residents at "
                                + enrollment.settlement()
                                + ": "
                                + resolved.unassignedResidents()
                );
            }

            long householdCount =
                    resolved.cycleInputs()
                            .households()
                            .size();

            /*
             * Upper bound:
             * one production record plus one
             * transfer and one consumption record
             * per represented household.
             *
             * The existing runner saves a baseline
             * before executing each interval.
             */
            long maximumTransactions =
                    Math.addExact(
                            1L,
                            Math.multiplyExact(
                                    householdCount,
                                    2L
                            )
                    );

            if (maximumTransactions
                    > EconomyJournalContinuity
                    .DEFAULT_RETAINED_TRANSACTIONS) {
                throw new IllegalStateException(
                        "Too many possible transactions "
                                + "in one agricultural interval"
                );
            }

            readiness.add(
                    new FarmReadiness(
                            enrollment.holding(),
                            resolved.representedResidents(),
                            resolved.cycleInputs()
                                    .households()
                                    .size()
                    )
            );
        }

        return new Readiness(
                startDayInclusive,
                endDayExclusive,
                readiness
        );
    }

    /**
     * Execute a complete campaign-day operation.
     *
     * All enrollments are inspected first.
     *
     * Each farm uses the existing verified
     * SettlementAgricultureRunner.
     *
     * Cross-holding rollback is not yet supported.
     * A later farm failure must stop the calling
     * strict coordinator rather than trigger retry.
     */
    public synchronized DayResult executeDay(
            long startDayInclusive,
            long endDayExclusive,
            long simulationTick
    ) throws IOException {

        if (simulationTick < 0L) {
            throw new IllegalArgumentException(
                    "Simulation tick cannot be negative"
            );
        }

        InventoryManager manager = economy.manager();

        synchronized (manager) {

            inspectLocked(
                    manager,
                    startDayInclusive,
                    endDayExclusive
            );

            List<FarmResult> results =
                    new ArrayList<>();

            for (AgriculturalDayEnrollment enrollment :
                    enrollments) {

                AgriculturalInterval interval =
                        enrollment.interval(
                                startDayInclusive,
                                endDayExclusive
                        );

                SettlementAgricultureRunner.Outcome outcome =
                        runner.execute(
                                enrollment.holding(),
                                enrollment.farmInventory(),
                                enrollment.settlement(),
                                interval,
                                enrollment.harvestInputs(),
                                enrollment.protectedReserveKg(),
                                enrollment.gramsPerPersonPerDay()
                        );

                /*
                 * A preflighted new day must execute.
                 * Unexpected ALREADY_COMPLETED means
                 * some other actor advanced ledger
                 * state after readiness validation.
                 */
                if (outcome.status()
                        != SettlementAgricultureRunner
                        .Status.EXECUTED) {
                    throw new IllegalStateException(
                            "Unexpected completed interval "
                                    + "during daily execution"
                    );
                }

                results.add(
                        new FarmResult(
                                enrollment.holding(),
                                outcome.food()
                        )
                );
            }

            lastResult = new DayResult(
                    startDayInclusive,
                    endDayExclusive,
                    simulationTick,
                    results
            );

            return lastResult;
        }
    }

    /**
     * Adapt checked persistence failures to the
     * established DayOperation callback contract.
     *
     * The strict coordinator catches the resulting
     * RuntimeException and blocks further execution.
     */
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
                    "Agricultural day checkpoint failed",
                    exception
            );
        }
    }

    /**
     * Construct an explicitly activated coordinator.
     *
     * This does not register callbacks or enable
     * agriculture in EconomyRuntime.
     */
    public CampaignAgricultureDayCoordinator
    strictCoordinator(long initialCampaignDay) {

        return new CampaignAgricultureDayCoordinator(
                initialCampaignDay,
                this
        );
    }
}

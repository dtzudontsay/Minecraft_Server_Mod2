
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
import java.util.Objects;
import java.util.Set;

/**
 * Batch 22.13.
 *
 * Guarded agricultural runner for one settlement
 * containing ONE OR MORE enrolled holdings.
 *
 * All holdings contribute to one settlement food
 * operation. Household demand is calculated once.
 *
 * Reuses the authoritative:
 *
 * - NPC and household identity registries
 * - Holding and settlement identity registries
 * - InventoryManager resource accounting
 * - EconomyAuthorityService snapshot checkpoints
 * - AgriculturalIntervalLedger replay protection
 *
 * No automatic runtime activation.
 *
 * Economy and ledger files are not a single
 * cross-domain atomic transaction.
 */
public final class MultiFarmSettlementAgricultureRunner
        implements CampaignAgricultureDayCoordinator.DayOperation {

    private final Path directory;
    private final ResourceCatalog catalog;
    private final EconomyAuthorityService.Loaded economy;
    private final EconomyReferenceValidator validator;
    private final SettlementFoodScenarioAdapter adapter;
    private final AgriculturalIntervalLedger ledger;

    private final SettlementId settlement;
    private final List<AgriculturalDayEnrollment> farms;

    private boolean blocked;

    private MultiFarmSettlementFoodCycle.Result lastResult;

    public MultiFarmSettlementAgricultureRunner(
            Path directory,
            ResourceCatalog catalog,
            EconomyAuthorityService.Loaded economy,
            EconomyReferenceValidator validator,
            SettlementFoodScenarioAdapter adapter,
            AgriculturalIntervalLedger ledger,
            List<AgriculturalDayEnrollment> farms
    ) {
        this.directory = Objects.requireNonNull(
                directory, "directory"
        );

        this.catalog = Objects.requireNonNull(
                catalog, "catalog"
        );

        this.economy = Objects.requireNonNull(
                economy, "economy"
        );

        this.validator = Objects.requireNonNull(
                validator, "validator"
        );

        this.adapter = Objects.requireNonNull(
                adapter, "adapter"
        );

        this.ledger = Objects.requireNonNull(
                ledger, "ledger"
        );

        Objects.requireNonNull(farms, "farms");

        /*
         * Batch 22.13:
         * Accept one or more farms so the same runner
         * handles both single-farm and multi-farm
         * settlements.
         */
        if (farms.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one agricultural holding "
                            + "must be enrolled"
            );
        }

        if (!directory.toAbsolutePath()
                .normalize()
                .equals(
                        ledger.file()
                                .getParent()
                                .toAbsolutePath()
                                .normalize()
                )) {
            throw new IllegalArgumentException(
                    "Agricultural ledger must use "
                            + "the economy save directory"
            );
        }

        List<AgriculturalDayEnrollment> sorted =
                new ArrayList<>(List.copyOf(farms));

        sorted.sort(
                Comparator.comparingLong(
                        farm -> farm.holding().value()
                )
        );

        this.settlement = sorted.get(0).settlement();

        Set<HoldingId> holdingIds = new HashSet<>();
        Set<InventoryId> inventoryIds = new HashSet<>();

        for (AgriculturalDayEnrollment farm : sorted) {

            if (!settlement.equals(farm.settlement())) {
                throw new IllegalArgumentException(
                        "A settlement runner cannot contain "
                                + "farms from different settlements"
                );
            }

            if (!holdingIds.add(farm.holding())
                    || !inventoryIds.add(
                    farm.farmInventory()
            )) {
                throw new IllegalArgumentException(
                        "Duplicate holding or farm inventory"
                );
            }
        }

        this.farms = List.copyOf(sorted);
    }

    public SettlementId settlement() {
        return settlement;
    }

    public List<AgriculturalDayEnrollment> farms() {
        return farms;
    }

    public synchronized boolean blocked() {
        return blocked;
    }

    public synchronized MultiFarmSettlementFoodCycle.Result
    lastResult() {
        return lastResult;
    }

    /**
     * Read-only preflight.
     *
     * Does not claim intervals, mutate inventories,
     * or create economic transactions.
     */
    public synchronized MultiFarmSettlementFoodCycle.Result
    inspect(
            long startDayInclusive,
            long endDayExclusive
    ) throws IOException {

        if (blocked) {
            throw new IllegalStateException(
                    "Agricultural settlement runner is blocked"
            );
        }

        InventoryManager manager = economy.manager();

        synchronized (manager) {
            return prepareInputs(
                    manager,
                    startDayInclusive,
                    endDayExclusive
            ).preview();
        }
    }

    private record Prepared(
            MultiFarmSettlementFoodCycle.Inputs inputs,
            MultiFarmSettlementFoodCycle.Result preview
    ) {
    }

    private Prepared prepareInputs(
            InventoryManager manager,
            long startDayInclusive,
            long endDayExclusive
    ) throws IOException {

        if (startDayInclusive < 0L
                || startDayInclusive == Long.MAX_VALUE
                || endDayExclusive
                != startDayInclusive + 1L) {
            throw new IllegalArgumentException(
                    "Exactly one campaign day is required"
            );
        }

        if (economy.mode()
                != EconomyAuthorityService.Mode
                .SNAPSHOT_PRIMARY) {
            throw new IOException(
                    "Snapshot-primary economy authority "
                            + "is required"
            );
        }

        if (manager.transactionCount()
                > EconomyJournalContinuity
                .DEFAULT_RETAINED_TRANSACTIONS) {
            throw new IOException(
                    "Existing pending journal cannot fit "
                            + "in a verified snapshot"
            );
        }

        validator.validate(manager);

        List<SettlementFoodCycle.Household> households =
                null;

        List<MultiFarmSettlementFoodCycle.Farm>
                farmInputs = new ArrayList<>();

        for (AgriculturalDayEnrollment farm : farms) {

            AgriculturalInterval interval =
                    farm.interval(
                            startDayInclusive,
                            endDayExclusive
                    );

            var progress = ledger.progress(
                    farm.holding()
            );

            if (progress.isPresent()) {

                if (progress.get().pending()) {
                    throw new IOException(
                            "Unresolved PREPARED interval "
                                    + "for holding "
                                    + farm.holding()
                    );
                }

                if (progress.get()
                        .lastCompletedEndDay()
                        != startDayInclusive) {
                    throw new IOException(
                            "Agricultural ledger and "
                                    + "campaign time disagree for "
                                    + farm.holding()
                    );
                }
            }

            SettlementFoodScenarioAdapter.Resolved
                    resolved = adapter.resolve(
                    manager,
                    farm.holding(),
                    farm.farmInventory(),
                    farm.settlement(),
                    interval,
                    farm.harvestInputs(),
                    farm.protectedReserveKg(),
                    farm.gramsPerPersonPerDay()
            );

            if (resolved.unassignedResidents() != 0L) {
                throw new IllegalStateException(
                        "Unassigned living residents in "
                                + "settlement " + settlement
                );
            }

            List<SettlementFoodCycle.Household> current =
                    resolved.cycleInputs().households();

            /*
             * Every farm in a settlement must resolve
             * the same population and food requirements.
             *
             * Consumption will happen only once.
             */
            if (households == null) {
                households = current;

            } else if (!households.equals(current)) {
                throw new IllegalStateException(
                        "Farm enrollments disagree about "
                                + "household population or demand"
                );
            }

            farmInputs.add(
                    new MultiFarmSettlementFoodCycle.Farm(
                            farm.holding(),
                            farm.farmInventory(),
                            farm.harvestPeriodDays(),
                            farm.harvestInputs(),
                            farm.protectedReserveKg()
                    )
            );
        }

        MultiFarmSettlementFoodCycle.Inputs inputs =
                new MultiFarmSettlementFoodCycle.Inputs(
                        settlement,
                        startDayInclusive,
                        endDayExclusive,
                        farmInputs,
                        Objects.requireNonNull(households)
                );

        MultiFarmSettlementFoodCycle.Result preview =
                MultiFarmSettlementFoodCycle.plan(
                        manager,
                        inputs
                );

        if (preview.journalEntries()
                > EconomyJournalContinuity
                .DEFAULT_RETAINED_TRANSACTIONS) {
            throw new IllegalStateException(
                    "Complete settlement cycle exceeds "
                            + "verified snapshot journal retention"
            );
        }

        return new Prepared(inputs, preview);
    }

    /**
     * Execute and checkpoint exactly one agricultural
     * day for this settlement.
     *
     * All farm ledgers are marked PREPARED before any
     * agricultural balance is changed.
     *
     * Any failure blocks this runner instance.
     * The persistent ledger also blocks unsafe replay.
     */
    public synchronized MultiFarmSettlementFoodCycle.Result
    executeDay(
            long startDayInclusive,
            long endDayExclusive,
            long simulationTick
    ) throws IOException {

        if (blocked) {
            throw new IllegalStateException(
                    "Agricultural settlement runner is blocked "
                            + "after a previous failure"
            );
        }

        try {
            if (simulationTick < 0L) {
                throw new IllegalArgumentException(
                        "Negative simulation tick"
                );
            }

            InventoryManager manager = economy.manager();

            synchronized (manager) {

                Prepared prepared = prepareInputs(
                        manager,
                        startDayInclusive,
                        endDayExclusive
                );

                /*
                 * Verified checkpoint of any earlier
                 * pending economic activity.
                 */
                EconomyAuthorityService.save(
                        directory,
                        catalog,
                        economy
                );

                /*
                 * Claim every enrolled holding BEFORE
                 * the settlement's resource changes.
                 */
                for (AgriculturalDayEnrollment farm : farms) {

                    AgriculturalIntervalLedger.BeginResult
                            claim = ledger.begin(
                            farm.holding(),
                            farm.interval(
                                    startDayInclusive,
                                    endDayExclusive
                            )
                    );

                    if (claim !=
                            AgriculturalIntervalLedger.BeginResult
                                    .STARTED) {
                        throw new IOException(
                                "Unexpected completed "
                                        + "agricultural interval"
                        );
                    }
                }

                /*
                 * One settlement-level operation.
                 *
                 * Harvest multiple farms.
                 * Distribute grain from actual owners.
                 * Consume each household's food once.
                 */
                MultiFarmSettlementFoodCycle.Result result =
                        MultiFarmSettlementFoodCycle.execute(
                                manager,
                                prepared.inputs()
                        );

                validator.validate(manager);

                /*
                 * Save all farm and household balances
                 * together in one verified economic
                 * snapshot.
                 */
                EconomyAuthorityService.save(
                        directory,
                        catalog,
                        economy
                );

                /*
                 * These ledger writes are separate
                 * and not cross-file atomic.
                 *
                 * Any interruption must remain
                 * fail-closed.
                 */
                for (AgriculturalDayEnrollment farm : farms) {
                    ledger.complete(
                            farm.holding(),
                            farm.interval(
                                    startDayInclusive,
                                    endDayExclusive
                            )
                    );
                }

                lastResult = result;

                return result;
            }

        } catch (IOException | RuntimeException | Error exception) {
            blocked = true;
            throw exception;
        }
    }

    /**
     * Explicit coordinator construction only.
     *
     * Does not register server tick callbacks.
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
                    "Agricultural settlement checkpoint failed",
                    exception
            );
        }
    }
}

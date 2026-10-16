
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
 * Guarded agricultural runner for one settlement
 * containing one or more enrolled holdings.
 *
 * Batch 22.20:
 *
 * Optionally includes anonymous settlement residents
 * in the SAME MultiFarmSettlementFoodCycle as existing
 * persistent-NPC households.
 *
 * Reuses existing InventoryManager, economy snapshots
 * and AgriculturalIntervalLedger.
 *
 * No automatic runtime activation.
 *
 * Economy and agricultural ledger files are not
 * one cross-domain atomic transaction.
 */
public final class MultiFarmSettlementAgricultureRunner
        implements CampaignAgricultureDayCoordinator.DayOperation {

    private final Path directory;
    private final ResourceCatalog catalog;
    private final EconomyAuthorityService.Loaded economy;
    private final EconomyReferenceValidator validator;
    private final SettlementFoodScenarioAdapter adapter;
    private final AgriculturalIntervalLedger ledger;

    private final SettlementCohortFoodIntegration cohortFood;

    private final SettlementId settlement;
    private final List<AgriculturalDayEnrollment> farms;

    private boolean blocked;
    private MultiFarmSettlementFoodCycle.Result lastResult;

    /**
     * Original constructor.
     *
     * Existing controlled agricultural scenarios
     * continue functioning without demographic
     * cohort integration.
     */
    public MultiFarmSettlementAgricultureRunner(
            Path directory,
            ResourceCatalog catalog,
            EconomyAuthorityService.Loaded economy,
            EconomyReferenceValidator validator,
            SettlementFoodScenarioAdapter adapter,
            AgriculturalIntervalLedger ledger,
            List<AgriculturalDayEnrollment> farms
    ) {
        this(
                directory,
                catalog,
                economy,
                validator,
                adapter,
                ledger,
                farms,
                null
        );
    }

    /**
     * Batch 22.20 constructor.
     *
     * cohortFood supplies a read-only demographic
     * connection and a pre-existing aggregate pantry.
     *
     * Both named and anonymous residents then consume
     * resources in one guarded settlement operation.
     */
    public MultiFarmSettlementAgricultureRunner(
            Path directory,
            ResourceCatalog catalog,
            EconomyAuthorityService.Loaded economy,
            EconomyReferenceValidator validator,
            SettlementFoodScenarioAdapter adapter,
            AgriculturalIntervalLedger ledger,
            List<AgriculturalDayEnrollment> farms,
            SettlementCohortFoodIntegration cohortFood
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

        this.cohortFood = cohortFood;

        Objects.requireNonNull(farms, "farms");

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
     * Read-only inspection of the complete
     * economic operation.
     *
     * Includes anonymous consumers when configured.
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
                || endDayExclusive != startDayInclusive + 1L) {

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
                            "Agricultural ledger and campaign "
                                    + "time disagree for "
                                    + farm.holding()
                    );
                }
            }

            SettlementFoodScenarioAdapter.Resolved resolved =
                    adapter.resolve(
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
                                + "settlement "
                                + settlement
                );
            }

            List<SettlementFoodCycle.Household> current =
                    resolved.cycleInputs().households();

            /*
             * Each farm must see identical existing
             * household demand. It is counted once.
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

        List<SettlementFoodCycle.Household> consumers =
                Objects.requireNonNull(households);

        /*
         * Batch 22.20:
         *
         * Resolve anonymous population ONCE after all
         * individual farm enrollments have been checked.
         *
         * Do not append the anonymous population once
         * per farm, as that would duplicate consumption.
         *
         * The returned list is passed to the SAME
         * multi-farm cycle as named NPC households.
         */
        if (cohortFood != null) {

            consumers = cohortFood.appendHouseholds(
                    manager,
                    settlement,
                    consumers
            );
        }

        MultiFarmSettlementFoodCycle.Inputs inputs =
                new MultiFarmSettlementFoodCycle.Inputs(
                        settlement,
                        startDayInclusive,
                        endDayExclusive,
                        farmInputs,
                        consumers
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
     * Execute one agricultural day.
     *
     * Existing interval claims and snapshot saves
     * protect the COMPLETE food operation, including
     * both named and anonymous consumers.
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
                 * Claim all farm intervals before
                 * performing resource changes.
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
                 * One unified operation:
                 *
                 * - Harvest all participating farms
                 * - Distribute actual available grain
                 * - Feed named NPC households
                 * - Feed surveyed anonymous residents
                 * - Record shortages for both groups
                 *
                 * There is one economic journal.
                 */
                MultiFarmSettlementFoodCycle.Result result =
                        MultiFarmSettlementFoodCycle.execute(
                                manager,
                                prepared.inputs()
                        );

                validator.validate(manager);

                /*
                 * Commit all updated resource balances,
                 * including the aggregate cohort pantry.
                 */
                EconomyAuthorityService.save(
                        directory,
                        catalog,
                        economy
                );

                /*
                 * Existing replay protection.
                 *
                 * These ledger writes are separate from
                 * the economic checkpoint; interrupted
                 * commits remain fail-closed.
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
     * Existing strict campaign-day coordinator.
     *
     * No automatic Fabric/server registration occurs.
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

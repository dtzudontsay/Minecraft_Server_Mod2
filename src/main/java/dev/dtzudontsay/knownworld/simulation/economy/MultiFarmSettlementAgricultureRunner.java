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
 * Guarded settlement agricultural operation.
 *
 * One or more farms produce food for one settlement.
 * Every consumer participates in one shared food cycle.
 *
 * Batch 22.21:
 * Supports settlements containing no persistent NPC
 * households, provided a verified anonymous population
 * and a valid settlement pantry are configured.
 *
 * The existing economy, snapshot persistence, farm
 * interval ledger, and replay protection are reused.
 *
 * Not automatically registered with Minecraft.
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
     * Existing named-household agricultural scenarios
     * remain supported without modification.
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
     * Cohort-enabled constructor from Batch 22.20.
     *
     * The optional cohort integration supplies
     * validated anonymous inhabitants and their
     * pre-existing settlement-owned pantry.
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
     * Preflight without modifying inventory balances,
     * farm ledgers, or economic journals.
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
                    "Snapshot-primary economy authority required"
            );
        }

        if (manager.transactionCount()
                > EconomyJournalContinuity
                .DEFAULT_RETAINED_TRANSACTIONS) {
            throw new IOException(
                    "Pending journal exceeds verified "
                            + "snapshot retention"
            );
        }

        validator.validate(manager);

        List<SettlementFoodCycle.Household> households =
                null;

        List<MultiFarmSettlementFoodCycle.Farm> farmInputs =
                new ArrayList<>();

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
                                    + "time disagree for holding "
                                    + farm.holding()
                    );
                }
            }

            /*
             * With cohort integration, zero persistent
             * NPC households is permitted.
             *
             * Without cohort integration, the adapter
             * retains its previous strict behavior.
             */
            SettlementFoodScenarioAdapter.Resolved resolved =
                    adapter.resolve(
                            manager,
                            farm.holding(),
                            farm.farmInventory(),
                            farm.settlement(),
                            interval,
                            farm.harvestInputs(),
                            farm.protectedReserveKg(),
                            farm.gramsPerPersonPerDay(),
                            cohortFood != null
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
             * Every farm must resolve exactly the same
             * named household consumers.
             *
             * Their food requirement is calculated
             * only once for the settlement.
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
         * This is the only place where anonymous
         * settlement consumers are appended.
         *
         * They are NOT appended once per farm.
         *
         * SettlementCohortFoodIntegration checks the
         * population survey, named-person count,
         * pantry owner, pantry location and collisions.
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
     * Execute and checkpoint one campaign day.
     *
     * The existing interval ledger protects against
     * replay. The economy snapshot includes the
     * aggregate pantry and every named household.
     *
     * Cross-file atomic recovery is not implemented.
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
                 * Commit any earlier pending economic
                 * activity before beginning this day.
                 */
                EconomyAuthorityService.save(
                        directory,
                        catalog,
                        economy
                );

                /*
                 * Claim every participating holding
                 * before changing any food balance.
                 */
                for (AgriculturalDayEnrollment farm : farms) {

                    AgriculturalIntervalLedger.BeginResult claim =
                            ledger.begin(
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
                 * The same existing food engine:
                 *
                 * - Produces farm grain
                 * - Allocates scarce food proportionally
                 * - Feeds represented NPC households
                 * - Feeds anonymous settlement residents
                 * - Reports actual unmet demand
                 *
                 * All resource mutations go through
                 * InventoryManager.
                 */
                MultiFarmSettlementFoodCycle.Result result =
                        MultiFarmSettlementFoodCycle.execute(
                                manager,
                                prepared.inputs()
                        );

                validator.validate(manager);

                EconomyAuthorityService.save(
                        directory,
                        catalog,
                        economy
                );

                /*
                 * Mark completed intervals after the
                 * economic balances are saved.
                 *
                 * An interrupted interval remains
                 * fail-closed, as in previous batches.
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
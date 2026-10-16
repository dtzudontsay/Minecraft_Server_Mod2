
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
 * Batch 22.12.
 *
 * Explicit, guarded multi-farm settlement runner.
 *
 * All enrolled holdings belong to one settlement.
 * Household population is resolved once from
 * existing NPC affiliations and organizations.
 *
 * All farm intervals are marked PREPARED before
 * the first resource mutation.
 *
 * The complete settlement resource flow is then
 * committed through EconomyAuthorityService.
 *
 * This runner does not register Minecraft hooks
 * and does not activate agriculture automatically.
 *
 * Separate ledger and economy files still do not
 * provide cross-domain atomicity.
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

        if (farms.size() < 2) {
            throw new IllegalArgumentException(
                    "Multi-farm runner requires at least "
                            + "two enrolled holdings"
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
                        "Multi-farm runner only supports "
                                + "one settlement at a time"
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
     * Read-only settlement readiness check.
     *
     * No ledger claim and no economic mutation.
     */
    public synchronized MultiFarmSettlementFoodCycle.Result
    inspect(
            long startDayInclusive,
            long endDayExclusive
    ) throws IOException {

        if (blocked) {
            throw new IllegalStateException(
                    "Multi-farm runner is blocked"
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

    /**
     * Validate every farm, household, interval and
     * projected economic transaction before claiming
     * a single interval.
     */
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
                    "Existing pending journal cannot "
                            + "fit in a verified snapshot"
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

            var progress = ledger.progress(farm.holding());

            if (progress.isPresent()) {
                if (progress.get().pending()) {
                    throw new IOException(
                            "Unresolved PREPARED interval "
                                    + "for holding " + farm.holding()
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

            /*
             * The existing adapter confirms:
             *
             * - Holding exists
             * - Farm inventory belongs to holding
             * - Farm and household locations match
             * - Household identities are organizations
             * - Household population uses living NPCs
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
                            farm.gramsPerPersonPerDay()
                    );

            if (resolved.unassignedResidents() != 0L) {
                throw new IllegalStateException(
                        "Unassigned residents in settlement "
                                + settlement
                );
            }

            List<SettlementFoodCycle.Household> current =
                    resolved.cycleInputs().households();

            if (households == null) {
                households = current;
            } else if (!households.equals(current)) {
                throw new IllegalStateException(
                        "Farm enrollments disagree about "
                                + "household population or food demand"
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
     * Execute and durably checkpoint one complete
     * settlement agricultural day.
     *
     * Any failure blocks this runner instance.
     * Prepared ledger markers block unsafe retries
     * across process restarts.
     */
    public synchronized MultiFarmSettlementFoodCycle.Result
    executeDay(
            long startDayInclusive,
            long endDayExclusive,
            long simulationTick
    ) throws IOException {

        if (blocked) {
            throw new IllegalStateException(
                    "Multi-farm runner is blocked "
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
                 * Commit and rotate earlier pending
                 * transactions before claiming the day.
                 */
                EconomyAuthorityService.save(
                        directory,
                        catalog,
                        economy
                );

                /*
                 * Claim ALL holdings before production
                 * or consumption begins.
                 *
                 * If any claim fails, no resources
                 * have been changed by this day.
                 * Earlier claims remain PREPARED and
                 * must be reconciled explicitly.
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
                                "Unexpected already completed "
                                        + "farm interval"
                        );
                    }
                }

                MultiFarmSettlementFoodCycle.Result result =
                        MultiFarmSettlementFoodCycle.execute(
                                manager,
                                prepared.inputs()
                        );

                validator.validate(manager);

                /*
                 * Commit the FULL settlement operation
                 * in one verified economy snapshot.
                 */
                EconomyAuthorityService.save(
                        directory,
                        catalog,
                        economy
                );

                /*
                 * Separate holding-ledger writes are
                 * not cross-file atomic.
                 *
                 * Partial completion on disk blocks
                 * further execution until reconciled.
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
     * Attach the exact same durable day operation
     * to the coordinator used by normal and
     * accelerated campaign-day progression.
     *
     * This method only constructs a coordinator.
     * It does not register a live server callback.
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
                    "Multi-farm agricultural checkpoint failed",
                    exception
            );
        }
    }
}

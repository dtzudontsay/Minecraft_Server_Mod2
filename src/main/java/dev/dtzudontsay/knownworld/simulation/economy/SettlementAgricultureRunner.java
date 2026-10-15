
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Batch 22.9.
 *
 * Explicitly invoked, guarded agriculture coordinator.
 *
 * Safety sequence:
 *
 * 1. Require snapshot-primary authority.
 * 2. Resolve authoritative household identities.
 * 3. Validate economic references.
 * 4. Save the pre-interval economy baseline.
 * 5. Durably claim the agricultural interval.
 * 6. Execute the existing settlement food cycle.
 * 7. Commit and verify economic balances.
 * 8. Durably mark the interval completed.
 *
 * If steps 6-8 fail, the ledger remains PREPARED.
 * Automatic retry is then prohibited.
 *
 * This class is not a Fabric initializer and does
 * not automatically simulate any world.
 */
public final class SettlementAgricultureRunner {

    public enum Status {
        EXECUTED,
        ALREADY_COMPLETED
    }

    public record Outcome(
            Status status,
            SettlementFoodCycle.Result food
    ) {
        public Outcome {
            Objects.requireNonNull(status);

            if (status == Status.EXECUTED && food == null) {
                throw new IllegalArgumentException(
                        "Executed interval requires a result"
                );
            }

            if (status == Status.ALREADY_COMPLETED
                    && food != null) {
                throw new IllegalArgumentException(
                        "Previously completed interval "
                                + "must not return new food activity"
                );
            }
        }
    }

    private final Path directory;
    private final ResourceCatalog catalog;
    private final EconomyAuthorityService.Loaded economy;
    private final EconomyReferenceValidator validator;
    private final SettlementFoodScenarioAdapter adapter;
    private final AgriculturalIntervalLedger ledger;

    public SettlementAgricultureRunner(
            Path directory,
            ResourceCatalog catalog,
            EconomyAuthorityService.Loaded economy,
            EconomyReferenceValidator validator,
            SettlementFoodScenarioAdapter adapter,
            AgriculturalIntervalLedger ledger
    ) {
        this.directory = Objects.requireNonNull(directory);
        this.catalog = Objects.requireNonNull(catalog);
        this.economy = Objects.requireNonNull(economy);
        this.validator = Objects.requireNonNull(validator);
        this.adapter = Objects.requireNonNull(adapter);
        this.ledger = Objects.requireNonNull(ledger);

        if (!directory.toAbsolutePath().normalize().equals(
                ledger.file()
                        .getParent()
                        .toAbsolutePath()
                        .normalize()
        )) {
            throw new IllegalArgumentException(
                    "Interval ledger and economy must "
                            + "use the same save directory"
            );
        }
    }

    public Outcome execute(
            HoldingId farmHolding,
            InventoryId farmInventory,
            SettlementId settlement,
            AgriculturalInterval interval,
            AgriculturalYieldModel.Inputs harvestInputs,
            long protectedReserveKg,
            long gramsPerPersonPerDay
    ) throws IOException {

        Objects.requireNonNull(farmHolding);
        Objects.requireNonNull(farmInventory);
        Objects.requireNonNull(settlement);
        Objects.requireNonNull(interval);
        Objects.requireNonNull(harvestInputs);

        if (economy.mode()
                != EconomyAuthorityService.Mode
                .SNAPSHOT_PRIMARY) {
            throw new IOException(
                    "Integrated agriculture requires "
                            + "explicit snapshot-primary authority"
            );
        }

        InventoryManager manager = economy.manager();

        synchronized (manager) {

            /*
             * Check durable progress before performing
             * unnecessary work.
             *
             * A missing or damaged ledger fails here.
             */
            var progress = ledger.progress(farmHolding);

            if (progress.isPresent()) {

                if (progress.get().pending()) {
                    throw new IOException(
                            "Agricultural interval is PREPARED "
                                    + "and cannot be repeated safely"
                    );
                }

                if (interval.endDayExclusive()
                        <= progress.get()
                        .lastCompletedEndDay()) {
                    return new Outcome(
                            Status.ALREADY_COMPLETED,
                            null
                    );
                }
            }

            SettlementFoodScenarioAdapter.Resolved
                    resolved = adapter.resolve(
                    manager,
                    farmHolding,
                    farmInventory,
                    settlement,
                    interval,
                    harvestInputs,
                    protectedReserveKg,
                    gramsPerPersonPerDay
            );

            /*
             * Never silently ignore known individual
             * residents lacking household assignment.
             *
             * Abstract population cohorts will need
             * an additional authoritative input later.
             */
            if (resolved.unassignedResidents() != 0L) {
                throw new IllegalStateException(
                        "Settlement has "
                                + resolved.unassignedResidents()
                                + " living residents without "
                                + "household affiliation"
                );
            }

            /*
             * Conservative journal-budget guard.
             *
             * One harvest and up to one transfer
             * and one consumption per household.
             */
            long worstCaseOperations =
                    Math.addExact(
                            1L,
                            Math.multiplyExact(
                                    resolved.cycleInputs()
                                            .households()
                                            .size(),
                                    2L
                            )
                    );

            if (worstCaseOperations
                    > EconomyJournalContinuity
                    .DEFAULT_RETAINED_TRANSACTIONS) {
                throw new IllegalStateException(
                        "Agricultural interval requires too "
                                + "many possible transactions for "
                                + "one verified checkpoint"
                );
            }

            /*
             * Validate current owner and settlement
             * references against existing registries.
             */
            validator.validate(manager);

            /*
             * Establish a verified baseline BEFORE
             * we claim the interval.
             *
             * This also rotates any earlier pending
             * journal activity.
             */
            EconomyAuthorityService.save(
                    directory,
                    catalog,
                    economy
            );

            AgriculturalIntervalLedger.BeginResult claim =
                    ledger.begin(
                            farmHolding,
                            interval
                    );

            if (claim ==
                    AgriculturalIntervalLedger.BeginResult
                            .ALREADY_COMPLETED) {
                return new Outcome(
                        Status.ALREADY_COMPLETED,
                        null
                );
            }

            /*
             * From this point onward, any error must
             * leave the interval in PREPARED state.
             *
             * We do NOT silently retry or roll back
             * resource balances.
             */
            SettlementFoodCycle.Result food =
                    SettlementFoodCycle.execute(
                            manager,
                            resolved.cycleInputs()
                    );

            validator.validate(manager);

            /*
             * Verified snapshot commit and journal
             * rotation from Batch 22.4.
             */
            EconomyAuthorityService.save(
                    directory,
                    catalog,
                    economy
            );

            /*
             * Only after the snapshot is verified
             * may the interval become COMPLETED.
             */
            ledger.complete(
                    farmHolding,
                    interval
            );

            return new Outcome(
                    Status.EXECUTED,
                    food
            );
        }
    }
}

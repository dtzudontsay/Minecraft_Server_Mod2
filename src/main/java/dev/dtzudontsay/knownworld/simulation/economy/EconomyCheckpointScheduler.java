
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.util.Objects;

/**
 * Batch 22.5.
 *
 * A deterministic, server-tick driven checkpoint
 * scheduler for the economy.
 *
 * Only snapshot-primary economies can be
 * automatically checkpointed.
 *
 * This class does not create resources or
 * initialize inventories.
 */
public final class EconomyCheckpointScheduler {

    public static final int DEFAULT_CHECK_INTERVAL_TICKS = 200;
    public static final int DEFAULT_CHECKPOINT_THRESHOLD = 8_000;

    public enum Decision {
        NOT_DUE,
        LEGACY_MODE,
        BELOW_THRESHOLD,
        CHECKPOINT_REQUIRED
    }

    @FunctionalInterface
    public interface CheckpointAction {
        void save() throws IOException;
    }

    private final EconomyAuthorityService.Mode mode;
    private final InventoryManager inventories;
    private final int checkIntervalTicks;
    private final int checkpointThreshold;
    private final CheckpointAction checkpointAction;

    private int ticksSinceCheck;
    private long successfulCheckpoints;

    public EconomyCheckpointScheduler(
            EconomyAuthorityService.Mode mode,
            InventoryManager inventories,
            CheckpointAction checkpointAction
    ) {
        this(
                mode,
                inventories,
                DEFAULT_CHECK_INTERVAL_TICKS,
                DEFAULT_CHECKPOINT_THRESHOLD,
                checkpointAction
        );
    }

    public EconomyCheckpointScheduler(
            EconomyAuthorityService.Mode mode,
            InventoryManager inventories,
            int checkIntervalTicks,
            int checkpointThreshold,
            CheckpointAction checkpointAction
    ) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.inventories = Objects.requireNonNull(
                inventories, "inventories"
        );
        this.checkpointAction = Objects.requireNonNull(
                checkpointAction, "checkpointAction"
        );

        if (checkIntervalTicks <= 0) {
            throw new IllegalArgumentException(
                    "Check interval must be positive"
            );
        }

        if (checkpointThreshold <= 0
                || checkpointThreshold >
                EconomyJournalContinuity.DEFAULT_RETAINED_TRANSACTIONS) {
            throw new IllegalArgumentException(
                    "Checkpoint threshold must be between 1 and "
                            + EconomyJournalContinuity
                            .DEFAULT_RETAINED_TRANSACTIONS
            );
        }

        this.checkIntervalTicks = checkIntervalTicks;
        this.checkpointThreshold = checkpointThreshold;
    }

    public synchronized int checkIntervalTicks() {
        return checkIntervalTicks;
    }

    public synchronized int checkpointThreshold() {
        return checkpointThreshold;
    }

    public synchronized long successfulCheckpoints() {
        return successfulCheckpoints;
    }

    public synchronized int ticksUntilNextCheck() {
        return checkIntervalTicks - ticksSinceCheck;
    }

    /**
     * Evaluates the current journal without
     * changing scheduler state.
     */
    public Decision inspect() {
        if (mode != EconomyAuthorityService.Mode.SNAPSHOT_PRIMARY) {
            return Decision.LEGACY_MODE;
        }

        if (inventories.transactionCount() >= checkpointThreshold) {
            return Decision.CHECKPOINT_REQUIRED;
        }

        return Decision.BELOW_THRESHOLD;
    }

    /**
     * Called once per server tick.
     *
     * The checkpoint action should validate live
     * economic references and save using the existing
     * EconomyAuthorityService.
     *
     * IOException propagates to the caller so that
     * failures cannot be mistaken for success.
     */
    public synchronized Decision tick() throws IOException {
        ticksSinceCheck++;

        if (ticksSinceCheck < checkIntervalTicks) {
            return Decision.NOT_DUE;
        }

        ticksSinceCheck = 0;

        Decision decision = inspect();

        if (decision != Decision.CHECKPOINT_REQUIRED) {
            return decision;
        }

        /*
         * The checkpoint action must complete
         * successfully before we report success.
         *
         * Batch 22.4 owns the actual verified
         * commit and journal rotation protocol.
         */
        checkpointAction.save();

        /*
         * A successful action must have reduced the
         * pending journal below the threshold.
         *
         * This prevents a no-op save action from
         * masquerading as a successful checkpoint.
         */
        if (inventories.transactionCount() >= checkpointThreshold) {
            throw new IOException(
                    "Economic checkpoint action completed "
                            + "without rotating the pending journal"
            );
        }

        successfulCheckpoints++;
        return Decision.CHECKPOINT_REQUIRED;
    }
}

package dev.dtzudontsay.knownworld.simulation.social.holding;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleAssignment;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reconciles active landed holdings whose persisted current holder
 * is no longer alive.
 *
 * -------------------------------------------------------------
 * RESPONSIBILITY
 * -------------------------------------------------------------
 *
 * This service only reconciles holding/title projections.
 *
 * It deliberately does NOT change:
 *
 * - dynasty head;
 * - dynasty heir;
 * - dynasty succession law;
 * - de-jure ownership;
 * - current owner dynasty.
 *
 * Dynasty leadership is handled by
 * DynastyLeadershipReconciliationService.
 *
 * Resolution priority:
 *
 * 1. Living active holder of the holding's linked title.
 * 2. Owning dynasty's explicitly stored living heir.
 * 3. Owning dynasty's living current head.
 * 4. No supported successor -> holding becomes vacant.
 */
public final class DeadHoldingHolderReconciliationService {

    private final NpcSimulation simulation;

    private final DynastyManager dynasties;

    private final LandedHoldingManager holdings;

    private final TitleManager titles;

    public DeadHoldingHolderReconciliationService(
            NpcSimulation simulation,
            DynastyManager dynasties,
            LandedHoldingManager holdings
    ) {

        this.simulation =
                Objects.requireNonNull(
                        simulation,
                        "simulation"
                );

        this.dynasties =
                Objects.requireNonNull(
                        dynasties,
                        "dynasties"
                );

        this.holdings =
                Objects.requireNonNull(
                        holdings,
                        "holdings"
                );

        this.titles =
                Objects.requireNonNull(
                        simulation.titles(),
                        "titles"
                );
    }

    public Report reconcileAll() {

        int examined =
                0;

        int deadHolders =
                0;

        int resolvedFromTitle =
                0;

        int resolvedFromDynastyHeir =
                0;

        int resolvedFromDynastyHead =
                0;

        int madeVacant =
                0;

        int titleTransfers =
                0;

        for (
                LandedHolding holding :
                holdings.all()
        ) {

            if (holding.status()
                    != HoldingStatus.ACTIVE) {

                continue;
            }

            examined++;

            NpcId previousHolder =
                    holding.holderNpcId();

            if (previousHolder == null) {
                continue;
            }

            NpcState previousState =
                    simulation.registry()
                            .find(
                                    previousHolder
                            )
                            .orElse(
                                    null
                            );

            /*
             * Missing references remain an integrity-audit problem.
             * This service only repairs known NPCs that subsequently died.
             */
            if (previousState == null) {
                continue;
            }

            if (previousState.isAlive()) {
                continue;
            }

            deadHolders++;

            Resolution resolution =
                    resolveSuccessor(
                            holding,
                            previousHolder
                    );

            NpcId successor =
                    resolution.successor();

            if (successor == null) {

                holdings.setHolder(
                        holding.id(),
                        null
                );

                madeVacant++;

                KnownWorld.LOGGER.warn(
                        "Cleared dead holder {} from holding {} [{}]; no supported runtime successor was available.",
                        previousState.identity()
                                .fullName(),
                        holding.name(),
                        holding.authoredId()
                );

                continue;
            }

            holdings.setHolder(
                    holding.id(),
                    successor
            );

            switch (
                    resolution.source()
            ) {

                case LINKED_TITLE ->
                        resolvedFromTitle++;

                case DYNASTY_HEIR ->
                        resolvedFromDynastyHeir++;

                case DYNASTY_HEAD ->
                        resolvedFromDynastyHead++;

                case NONE -> {
                }
            }

            if (synchronizeLinkedTitle(
                    holding,
                    successor
            )) {

                titleTransfers++;
            }

            NpcState successorState =
                    simulation.registry()
                            .find(
                                    successor
                            )
                            .orElse(
                                    null
                            );

            String successorName =
                    successorState == null
                            ? successor.toString()
                            : successorState.identity()
                            .fullName();

            KnownWorld.LOGGER.info(
                    "Reconciled dead holding holder: {} [{}] {} -> {} via {}.",
                    holding.name(),
                    holding.authoredId(),
                    previousState.identity()
                            .fullName(),
                    successorName,
                    resolution.source()
            );
        }

        /*
         * The final two zero values are retained for compatibility with
         * existing reporting/debug output from the previous batch.
         *
         * They are intentionally always zero now because this service no
         * longer owns dynasty leadership.
         */
        return new Report(
                examined,
                deadHolders,
                resolvedFromTitle,
                resolvedFromDynastyHeir,
                resolvedFromDynastyHead,
                madeVacant,
                titleTransfers,
                0,
                0
        );
    }

    private Resolution resolveSuccessor(
            LandedHolding holding,
            NpcId deadHolder
    ) {

        Optional<NpcId> titleSuccessor =
                livingLinkedTitleHolder(
                        holding,
                        deadHolder
                );

        if (titleSuccessor.isPresent()) {

            return new Resolution(
                    titleSuccessor.get(),
                    ResolutionSource.LINKED_TITLE
            );
        }

        DynastyId ownerDynastyId =
                holding.ownerDynastyId();

        if (ownerDynastyId == null) {

            return Resolution.none();
        }

        Dynasty dynasty =
                dynasties.find(
                                ownerDynastyId
                        )
                        .orElse(
                                null
                        );

        if (dynasty == null) {

            return Resolution.none();
        }

        NpcId heir =
                dynasty.heir();

        if (isLivingCandidate(
                heir,
                deadHolder
        )) {

            return new Resolution(
                    heir,
                    ResolutionSource.DYNASTY_HEIR
            );
        }

        NpcId head =
                dynasty.head();

        if (isLivingCandidate(
                head,
                deadHolder
        )) {

            return new Resolution(
                    head,
                    ResolutionSource.DYNASTY_HEAD
            );
        }

        return Resolution.none();
    }

    private Optional<NpcId> livingLinkedTitleHolder(
            LandedHolding holding,
            NpcId deadHolder
    ) {

        TitleId titleId =
                holding.linkedTitleId();

        if (titleId == null) {

            return Optional.empty();
        }

        if (titles.find(
                titleId
        ).isEmpty()) {

            return Optional.empty();
        }

        List<TitleAssignment> active =
                titles.activeAssignmentsForTitle(
                                titleId
                        )
                        .stream()
                        .filter(
                                TitleAssignment::isActive
                        )
                        .filter(
                                assignment ->
                                        isLivingCandidate(
                                                assignment.holder(),
                                                deadHolder
                                        )
                        )
                        .sorted(
                                Comparator
                                        .comparingLong(
                                                TitleAssignment::grantedTick
                                        )
                                        .reversed()
                                        .thenComparingLong(
                                                assignment ->
                                                        assignment.holder()
                                                                .value()
                                        )
                        )
                        .toList();

        if (active.isEmpty()) {

            return Optional.empty();
        }

        return Optional.of(
                active.get(
                                0
                        )
                        .holder()
        );
    }

    /**
     * Synchronizes an explicitly linked title with the resolved holder.
     *
     * TitleManager.grant() already knows how to revoke the previous
     * assignment for exclusive titles.
     */
    private boolean synchronizeLinkedTitle(
            LandedHolding holding,
            NpcId successor
    ) {

        TitleId titleId =
                holding.linkedTitleId();

        if (titleId == null) {
            return false;
        }

        if (titles.find(
                titleId
        ).isEmpty()) {

            return false;
        }

        boolean alreadyActive =
                titles.activeAssignmentsForTitle(
                                titleId
                        )
                        .stream()
                        .anyMatch(
                                assignment ->
                                        assignment.isActive()
                                                && assignment.holder()
                                                .equals(
                                                        successor
                                                )
                        );

        if (alreadyActive) {
            return false;
        }

        titles.grant(
                titleId,
                successor,
                simulation.serverTickCounter()
        );

        return true;
    }

    private boolean isLivingCandidate(
            NpcId candidate,
            NpcId deadHolder
    ) {

        if (candidate == null) {
            return false;
        }

        if (candidate.equals(
                deadHolder
        )) {

            return false;
        }

        return simulation.registry()
                .find(
                        candidate
                )
                .map(
                        NpcState::isAlive
                )
                .orElse(
                        false
                );
    }

    public enum ResolutionSource {

        LINKED_TITLE,

        DYNASTY_HEIR,

        DYNASTY_HEAD,

        NONE
    }

    private record Resolution(
            NpcId successor,
            ResolutionSource source
    ) {

        private static Resolution none() {

            return new Resolution(
                    null,
                    ResolutionSource.NONE
            );
        }
    }

    public record Report(
            int activeHoldingsExamined,
            int deadHoldersFound,
            int resolvedFromLinkedTitle,
            int resolvedFromDynastyHeir,
            int resolvedFromDynastyHead,
            int madeVacant,
            int linkedTitleTransfers,
            int dynastyHeadsAdvanced,
            int dynastyHeirsCleared
    ) {

        public int resolved() {

            return resolvedFromLinkedTitle
                    + resolvedFromDynastyHeir
                    + resolvedFromDynastyHead;
        }

        public boolean changedAnything() {

            return deadHoldersFound > 0;
        }
    }
}
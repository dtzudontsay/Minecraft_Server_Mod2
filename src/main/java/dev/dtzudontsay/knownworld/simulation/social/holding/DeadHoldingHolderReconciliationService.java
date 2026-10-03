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
 * WHY THIS EXISTS
 * -------------------------------------------------------------
 *
 * Scenario data describes the political world at the scenario
 * starting point.
 *
 * Persisted runtime state describes what the simulation has become.
 *
 * Once a holder dies, authored scenario-start ownership MUST NOT
 * simply be reapplied. Otherwise a long-running world would keep
 * restoring dead historical rulers whenever it starts.
 *
 * This service therefore repairs runtime political state using only
 * runtime information that already has explicit succession meaning.
 *
 * Resolution priority:
 *
 * 1. A living active holder of the holding's linked title.
 * 2. The owning dynasty's explicitly stored heir.
 * 3. The owning dynasty's living current head.
 * 4. No supported successor -> leave the holding vacant.
 *
 * We intentionally DO NOT guess primogeniture, gender preference,
 * legitimacy, elective law, seniority, partition, etc. Those rules
 * belong in the future succession/government-law system.
 *
 * A vacant holding is preferable to silently inventing a ruler.
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

    /**
     * Repairs all active holdings that currently reference a dead NPC.
     *
     * The operation is deterministic and idempotent:
     *
     * - a valid living holder is never touched;
     * - once a dead holder has been replaced, another pass does nothing;
     * - an unresolved dead holder becomes a vacancy rather than remaining
     *   structurally invalid.
     */
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

        int dynastyHeadsAdvanced =
                0;

        int dynastyHeirsCleared =
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
             * Missing NPC references are handled by the normal structural
             * integrity audit. This service is specifically responsible
             * for the "known NPC but now dead" case.
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
                        previousState.identity().fullName(),
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
                    /*
                     * Impossible here because successor != null.
                     */
                }
            }

            Dynasty ownerDynasty =
                    holding.ownerDynastyId() == null
                            ? null
                            : dynasties.find(
                                    holding.ownerDynastyId()
                            )
                            .orElse(
                                    null
                            );

            if (ownerDynasty != null) {

                HeadAdvanceResult headResult =
                        advanceDynastyLeadershipIfAppropriate(
                                ownerDynasty,
                                previousHolder,
                                successor
                        );

                if (headResult.headAdvanced()) {
                    dynastyHeadsAdvanced++;
                }

                if (headResult.heirCleared()) {
                    dynastyHeirsCleared++;
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
                    previousState.identity().fullName(),
                    successorName,
                    resolution.source()
            );
        }

        return new Report(
                examined,
                deadHolders,
                resolvedFromTitle,
                resolvedFromDynastyHeir,
                resolvedFromDynastyHead,
                madeVacant,
                titleTransfers,
                dynastyHeadsAdvanced,
                dynastyHeirsCleared
        );
    }

    private Resolution resolveSuccessor(
            LandedHolding holding,
            NpcId deadHolder
    ) {

        /*
         * ---------------------------------------------------------
         * 1. LINKED TITLE
         * ---------------------------------------------------------
         *
         * A title may already have been transferred by some other
         * simulation system. If so, that is stronger evidence than
         * deriving anything from a dynasty.
         */
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

        /*
         * ---------------------------------------------------------
         * 2/3. DYNASTIC SUCCESSION
         * ---------------------------------------------------------
         *
         * Only use explicit runtime dynasty leadership information.
         * Do not infer primogeniture from genealogy here.
         */
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
                                        .thenComparing(
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

    /**
     * If the dead holding holder was also the owning dynasty's current
     * head, move the head to the resolved successor.
     *
     * If that successor was the stored heir, clear the heir slot.
     *
     * We deliberately do not invent the NEXT heir here. That requires
     * actual succession-law evaluation and belongs to the later
     * succession system.
     */
    private HeadAdvanceResult advanceDynastyLeadershipIfAppropriate(
            Dynasty dynasty,
            NpcId deadHolder,
            NpcId successor
    ) {

        boolean headAdvanced =
                false;

        boolean heirCleared =
                false;

        if (deadHolder.equals(
                dynasty.head()
        )) {

            dynasties.setHead(
                    dynasty.id(),
                    successor
            );

            headAdvanced =
                    true;
        }

        if (successor.equals(
                dynasty.heir()
        )) {

            dynasties.setHeir(
                    dynasty.id(),
                    null
            );

            heirCleared =
                    true;
        }

        return new HeadAdvanceResult(
                headAdvanced,
                heirCleared
        );
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

    private record HeadAdvanceResult(
            boolean headAdvanced,
            boolean heirCleared
    ) {
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
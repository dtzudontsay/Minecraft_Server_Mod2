package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingStatus;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingManager;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentity;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentityManager;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionLaw;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Keeps runtime dynasty leadership coherent with NPC mortality and the
 * succession rules that already exist elsewhere in the simulation.
 *
 * -------------------------------------------------------------
 * RESPONSIBILITY
 * -------------------------------------------------------------
 *
 * This service owns:
 *
 * - dynasty head death reconciliation;
 * - invalid/dead dynasty heir reconciliation;
 * - promotion of an explicitly stored living dynasty heir;
 * - fallback to an already configured hereditary title law;
 * - calculation of a next heir when an existing title law supports it.
 *
 * It deliberately does NOT own:
 *
 * - landed-holding succession;
 * - de-jure ownership;
 * - allegiance;
 * - elective government;
 * - legitimisation;
 * - bastardy law;
 * - designation;
 * - partition;
 * - seniority;
 * - invented generic primogeniture rules.
 *
 * If no supported successor exists, leaving a dynasty without a current
 * head is preferable to silently inventing a political/legal rule.
 */
public final class DynastyLeadershipReconciliationService {

    private final NpcSimulation simulation;

    private final DynastyManager dynasties;

    private final CharacterSocialIdentityManager identities;

    private final LandedHoldingManager holdings;

    public DynastyLeadershipReconciliationService(
            NpcSimulation simulation,
            DynastyManager dynasties,
            CharacterSocialIdentityManager identities,
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

        this.identities =
                Objects.requireNonNull(
                        identities,
                        "identities"
                );

        this.holdings =
                Objects.requireNonNull(
                        holdings,
                        "holdings"
                );
    }

    /**
     * Full reconciliation pass.
     *
     * Intended for:
     *
     * - world loading;
     * - pre-save safety reconciliation;
     * - migration from older saves.
     */
    public Report reconcileAll(
            long tick
    ) {

        MutableReport report =
                new MutableReport();

        for (
                Dynasty dynasty :
                dynasties.all()
        ) {

            report.dynastiesExamined++;

            reconcileDynasty(
                    dynasty,
                    tick,
                    report
            );
        }

        return report.freeze();
    }

    /**
     * Targeted death-time reconciliation.
     *
     * Only dynasties directly referencing the deceased as head or heir
     * need to be inspected.
     */
    public Report handleDeath(
            NpcId deceased,
            long tick
    ) {

        Objects.requireNonNull(
                deceased,
                "deceased"
        );

        MutableReport report =
                new MutableReport();

        for (
                Dynasty dynasty :
                dynasties.all()
        ) {

            if (!deceased.equals(
                    dynasty.head()
            )
                    && !deceased.equals(
                    dynasty.heir()
            )) {

                continue;
            }

            report.dynastiesExamined++;

            reconcileDynasty(
                    dynasty,
                    tick,
                    report
            );
        }

        return report.freeze();
    }

    private void reconcileDynasty(
            Dynasty dynasty,
            long tick,
            MutableReport report
    ) {

        NpcId head =
                dynasty.head();

        /*
         * No authored/runtime head exists.
         *
         * Do not create one merely because members of the dynasty exist.
         * A missing head may be intentional or may await a later legal /
         * government system.
         */
        if (head == null) {

            reconcileHeirWithoutHead(
                    dynasty,
                    report
            );

            return;
        }

        NpcState headState =
                simulation.registry()
                        .find(
                                head
                        )
                        .orElse(
                                null
                        );

        /*
         * Missing NPC references are structural corruption and belong to
         * the integrity audit. Do not hide them by silently rewriting state.
         */
        if (headState == null) {
            return;
        }

        if (headState.isAlive()) {

            reconcileHeirForLivingHead(
                    dynasty,
                    head,
                    report
            );

            return;
        }

        report.deadHeadsFound++;

        NpcId previousHeir =
                dynasty.heir();

        NpcId successor =
                livingCandidate(
                        previousHeir,
                        head
                )
                        ? previousHeir
                        : null;

        SuccessionSource source =
                successor == null
                        ? SuccessionSource.NONE
                        : SuccessionSource.STORED_DYNASTY_HEIR;

        /*
         * If the stored heir is absent or dead, ask the actual hereditary
         * title system.
         *
         * This is materially different from inventing generic
         * primogeniture here: the title must already exist and must
         * already have a configured non-NONE SuccessionLaw.
         */
        if (successor == null) {

            Optional<NpcId> legalSuccessor =
                    lawDrivenHeir(
                            dynasty,
                            head
                    );

            if (legalSuccessor.isPresent()) {

                successor =
                        legalSuccessor.get();

                source =
                        SuccessionSource.LINKED_TITLE_LAW;
            }
        }

        if (successor == null) {

            dynasties.setHead(
                    dynasty.id(),
                    null
            );

            if (dynasty.heir() != null) {

                dynasties.setHeir(
                        dynasty.id(),
                        null
                );

                report.deadHeirsCleared++;
            }

            report.headsVacated++;

            createVacancyEvent(
                    dynasty,
                    headState,
                    tick
            );

            report.eventsCreated++;

            KnownWorld.LOGGER.warn(
                    "Dynasty leadership became vacant: {} [{}] after death of {}; no supported runtime successor was available.",
                    dynasty.name(),
                    dynasty.authoredId(),
                    headState.identity().fullName()
            );

            return;
        }

        dynasties.setHead(
                dynasty.id(),
                successor
        );

        report.headsAdvanced++;

        if (source
                == SuccessionSource.STORED_DYNASTY_HEIR) {

            report.advancedFromStoredHeir++;

        } else if (
                source
                        == SuccessionSource.LINKED_TITLE_LAW
        ) {

            report.advancedFromLinkedTitleLaw++;
        }

        /*
         * The promoted character cannot simultaneously remain the heir.
         */
        dynasties.setHeir(
                dynasty.id(),
                null
        );

        /*
         * Now ask the existing legal title machinery whether it can
         * determine the next heir.
         */
        Optional<NpcId> nextHeir =
                lawDrivenHeir(
                        dynasty,
                        successor
                );

        if (nextHeir.isPresent()) {

            dynasties.setHeir(
                    dynasty.id(),
                    nextHeir.get()
            );

            report.heirsRecomputed++;
        }

        createSuccessionEvent(
                dynasty,
                headState,
                successor,
                source,
                tick
        );

        report.eventsCreated++;

        NpcState successorState =
                simulation.registry()
                        .find(
                                successor
                        )
                        .orElse(
                                null
                        );

        KnownWorld.LOGGER.info(
                "Dynasty leadership reconciled: {} [{}] {} -> {} via {}; nextHeir={}.",
                dynasty.name(),
                dynasty.authoredId(),
                headState.identity().fullName(),
                successorState == null
                        ? successor
                        : successorState.identity().fullName(),
                source,
                dynasty.heir()
        );
    }

    /**
     * A dynasty without a head is not assigned one here.
     *
     * We only remove an heir that has become objectively invalid through
     * death. Missing-NPC references remain untouched for the integrity
     * audit to expose.
     */
    private void reconcileHeirWithoutHead(
            Dynasty dynasty,
            MutableReport report
    ) {

        NpcId heir =
                dynasty.heir();

        if (heir == null) {
            return;
        }

        NpcState heirState =
                simulation.registry()
                        .find(
                                heir
                        )
                        .orElse(
                                null
                        );

        if (heirState == null) {
            return;
        }

        if (!heirState.isAlive()) {

            dynasties.setHeir(
                    dynasty.id(),
                    null
            );

            report.deadHeirsCleared++;
        }
    }

    private void reconcileHeirForLivingHead(
            Dynasty dynasty,
            NpcId livingHead,
            MutableReport report
    ) {

        NpcId heir =
                dynasty.heir();

        if (heir == null) {

            /*
             * Do not populate every historically authored null heir merely
             * because genealogy exists.
             *
             * Null remains meaningful until a concrete legal system gives
             * us enough information.
             */
            return;
        }

        NpcState heirState =
                simulation.registry()
                        .find(
                                heir
                        )
                        .orElse(
                                null
                        );

        /*
         * Missing references are intentionally preserved so the strict
         * integrity layer can report corruption.
         */
        if (heirState == null) {
            return;
        }

        boolean invalid =
                heir.equals(
                        livingHead
                )
                        || !heirState.isAlive();

        if (!invalid) {
            return;
        }

        dynasties.setHeir(
                dynasty.id(),
                null
        );

        report.deadHeirsCleared++;

        Optional<NpcId> replacement =
                lawDrivenHeir(
                        dynasty,
                        livingHead
                );

        if (replacement.isPresent()) {

            dynasties.setHeir(
                    dynasty.id(),
                    replacement.get()
            );

            report.heirsRecomputed++;
        }
    }

    /**
     * Finds a successor using only hereditary title laws that are already
     * configured in the simulation.
     */
    private Optional<NpcId> lawDrivenHeir(
            Dynasty dynasty,
            NpcId currentHolder
    ) {

        List<LandedHolding> candidates =
                new ArrayList<>();

        for (
                LandedHolding holding :
                holdings.all()
        ) {

            if (holding.status()
                    != HoldingStatus.ACTIVE) {

                continue;
            }

            boolean belongsToDynasty =
                    dynasty.id()
                            .equals(
                                    holding.ownerDynastyId()
                            )
                            || (
                            holding.ownerDynastyId()
                                    == null
                                    && dynasty.id()
                                    .equals(
                                            holding.deJureDynastyId()
                                    )
                    );

            if (!belongsToDynasty) {
                continue;
            }

            if (holding.linkedTitleId()
                    == null) {

                continue;
            }

            candidates.add(
                    holding
            );
        }

        candidates.sort(
                Comparator
                        .comparingInt(
                                (LandedHolding holding) ->
                                        holding.capital()
                                                ? 0
                                                : 1
                        )
                        .thenComparingLong(
                                holding ->
                                        holding.id()
                                                .value()
                        )
        );

        for (
                LandedHolding holding :
                candidates
        ) {

            TitleId title =
                    holding.linkedTitleId();

            if (simulation.titles()
                    .find(
                            title
                    )
                    .isEmpty()) {

                continue;
            }

            SuccessionLaw law =
                    simulation.successionRules()
                            .lawOf(
                                    title
                            );

            if (law
                    == SuccessionLaw.NONE) {

                continue;
            }

            Optional<NpcId> legalHeir =
                    simulation.succession()
                            .heirOf(
                                    title,
                                    currentHolder
                            );

            if (legalHeir.isEmpty()) {
                continue;
            }

            NpcId candidate =
                    legalHeir.get();

            if (!livingCandidate(
                    candidate,
                    currentHolder
            )) {

                continue;
            }

            /*
             * A title succession candidate should still have a meaningful
             * relationship to this dynasty.
             *
             * Birth dynasty is intentionally accepted so, for example,
             * a married daughter does not cease to be genealogically part
             * of her birth house merely because her current social dynasty
             * changed through marriage.
             */
            if (!belongsToDynasty(
                    candidate,
                    dynasty.id()
            )) {

                continue;
            }

            return Optional.of(
                    candidate
            );
        }

        return Optional.empty();
    }

    private boolean belongsToDynasty(
            NpcId npc,
            DynastyId dynasty
    ) {

        CharacterSocialIdentity identity =
                identities.find(
                                npc
                        )
                        .orElse(
                                null
                        );

        if (identity == null) {
            return false;
        }

        return dynasty.equals(
                identity.currentDynasty()
        )
                || dynasty.equals(
                identity.legalFamilyDynasty()
        )
                || dynasty.equals(
                identity.birthDynasty()
        );
    }

    private boolean livingCandidate(
            NpcId candidate,
            NpcId excluded
    ) {

        if (candidate == null) {
            return false;
        }

        if (excluded != null
                && excluded.equals(
                candidate
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

    private void createSuccessionEvent(
            Dynasty dynasty,
            NpcState deceasedHead,
            NpcId successor,
            SuccessionSource source,
            long tick
    ) {

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

        simulation.events()
                .create(
                        WorldEventType.POLITICAL,
                        successorName
                                + " became head of "
                                + dynasty.name()
                                + " after the death of "
                                + deceasedHead.identity()
                                .fullName()
                                + ".",
                        successorState == null
                                ? deceasedHead.position()
                                : successorState.position(),
                        tick,
                        0.85,
                        successor,
                        deceasedHead.id(),
                        "dynasty.head.succeeded",
                        dynasty.hasAuthoredId()
                                ? dynasty.authoredId()
                                : dynasty.id()
                                .toString()
                );
    }

    private void createVacancyEvent(
            Dynasty dynasty,
            NpcState deceasedHead,
            long tick
    ) {

        simulation.events()
                .create(
                        WorldEventType.POLITICAL,
                        dynasty.name()
                                + " was left without a confirmed head after the death of "
                                + deceasedHead.identity()
                                .fullName()
                                + ".",
                        deceasedHead.position(),
                        tick,
                        0.80,
                        deceasedHead.id(),
                        deceasedHead.id(),
                        "dynasty.head.vacant",
                        dynasty.hasAuthoredId()
                                ? dynasty.authoredId()
                                : dynasty.id()
                                .toString()
                );
    }

    public enum SuccessionSource {

        STORED_DYNASTY_HEIR,

        LINKED_TITLE_LAW,

        NONE
    }

    private static final class MutableReport {

        private int dynastiesExamined;

        private int deadHeadsFound;

        private int headsAdvanced;

        private int advancedFromStoredHeir;

        private int advancedFromLinkedTitleLaw;

        private int headsVacated;

        private int deadHeirsCleared;

        private int heirsRecomputed;

        private int eventsCreated;

        private Report freeze() {

            return new Report(
                    dynastiesExamined,
                    deadHeadsFound,
                    headsAdvanced,
                    advancedFromStoredHeir,
                    advancedFromLinkedTitleLaw,
                    headsVacated,
                    deadHeirsCleared,
                    heirsRecomputed,
                    eventsCreated
            );
        }
    }

    public record Report(
            int dynastiesExamined,
            int deadHeadsFound,
            int headsAdvanced,
            int advancedFromStoredHeir,
            int advancedFromLinkedTitleLaw,
            int headsVacated,
            int deadHeirsCleared,
            int heirsRecomputed,
            int eventsCreated
    ) {

        public boolean changedAnything() {

            return headsAdvanced > 0
                    || headsVacated > 0
                    || deadHeirsCleared > 0
                    || heirsRecomputed > 0;
        }
    }
}
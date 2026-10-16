
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingStatus;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.time.CampaignDayBoundaryCheck;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Batch 22.14.
 *
 * Read-only readiness assessment connecting:
 *
 * - Existing campaign time
 * - Existing landed holdings
 * - Existing agricultural enrollments
 * - Existing settlement economic preflight
 * - Explicit scenario coverage evidence
 *
 * No new world state is authoritative here.
 *
 * This class never enables autonomous agriculture.
 */
public final class AgriculturalActivationReadiness {

    public enum Severity {
        CONTROLLED_BLOCKER,
        AUTONOMOUS_BLOCKER
    }

    public enum Code {
        CAMPAIGN_TIME_MISMATCH,
        DISPATCHER_NOT_READY,
        HOLDING_MISSING,
        HOLDING_NOT_ACTIVE,
        NON_PRODUCING_HOLDING,
        GEOGRAPHY_NOT_VERIFIED,
        POPULATION_NOT_VERIFIED,
        PRODUCTION_NOT_CALIBRATED,
        CROSS_DOMAIN_SAVE_NOT_ATOMIC
    }

    public record Issue(
            Code code,
            Severity severity,
            String explanation
    ) {
        public Issue {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(severity, "severity");
            Objects.requireNonNull(
                    explanation, "explanation"
            );
        }
    }

    /**
     * Scenario evidence must eventually be obtained
     * from authoritative scenario, geography and
     * demographic data.
     *
     * This is an interface for evidence, not a new
     * holding, geographic or population registry.
     *
     * Passing true means the caller asserts that the
     * corresponding evidence has been independently
     * established. It does not automatically prove
     * that the underlying data is correct.
     */
    public interface ScenarioEvidence {

        boolean confirmsHoldingSettlementLink(
                HoldingId holding,
                String holdingWorldLocationId,
                SettlementId settlement
        );

        boolean confirmsCompletePopulation(
                SettlementId settlement
        );

        boolean confirmsCalibratedProduction(
                HoldingId holding
        );

        /**
         * Fail-closed default for scenarios that
         * have not established these relationships.
         */
        static ScenarioEvidence unverified() {
            return new ScenarioEvidence() {

                @Override
                public boolean confirmsHoldingSettlementLink(
                        HoldingId holding,
                        String location,
                        SettlementId settlement
                ) {
                    return false;
                }

                @Override
                public boolean confirmsCompletePopulation(
                        SettlementId settlement
                ) {
                    return false;
                }

                @Override
                public boolean confirmsCalibratedProduction(
                        HoldingId holding
                ) {
                    return false;
                }
            };
        }
    }

    public record Assessment(
            long startDayInclusive,
            long endDayExclusive,
            int enrolledHoldings,
            int enrolledSettlements,
            CampaignSettlementAgricultureDispatcher.Readiness
            economicPreview,
            List<Issue> issues
    ) {
        public Assessment {
            issues = List.copyOf(issues);
        }

        /**
         * The read-only preconditions for explicitly
         * controlled test execution.
         *
         * This is NOT permission to register a live
         * Fabric callback or start autonomous farming.
         */
        public boolean controlledReady() {
            return issues.stream().noneMatch(
                    issue -> issue.severity()
                            == Severity.CONTROLLED_BLOCKER
            );
        }

        /**
         * Autonomous activation additionally requires
         * cross-domain save safety.
         *
         * At present this remains false because
         * the economy, calendar, NPC and holding saves
         * are not one atomic operation.
         */
        public boolean autonomousReady() {
            return issues.isEmpty();
        }
    }

    private AgriculturalActivationReadiness() {
    }

    /**
     * Inspect a configured dispatcher against actual
     * campaign time and existing landed holdings.
     *
     * Use holdings::find from LandedHoldingManager
     * for holdingLookup in the real simulation.
     *
     * All reported issues are deterministic for
     * unchanged source state.
     *
     * No inventory balances or interval markers
     * are changed.
     */
    public static Assessment inspect(
            CampaignSettlementAgricultureDispatcher dispatcher,
            SimulationTimeContract time,
            long startDayInclusive,
            long endDayExclusive,
            Function<HoldingId, Optional<LandedHolding>>
                    holdingLookup,
            ScenarioEvidence evidence
    ) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(
                holdingLookup, "holdingLookup"
        );
        Objects.requireNonNull(evidence, "evidence");

        List<Issue> issues = new ArrayList<>();

        CampaignDayBoundaryCheck.Result boundary =
                CampaignDayBoundaryCheck.check(
                        time,
                        startDayInclusive,
                        endDayExclusive
                );

        if (!boundary.aligned()) {
            add(
                    issues,
                    Code.CAMPAIGN_TIME_MISMATCH,
                    Severity.CONTROLLED_BLOCKER,
                    "Campaign-day boundary: "
                            + boundary.status()
            );
        }

        List<AgriculturalDayEnrollment> enrollments =
                dispatcher.enrollments();

        Set<Long> settlementIds = new TreeSet<>();

        for (AgriculturalDayEnrollment enrollment :
                enrollments) {

            settlementIds.add(
                    enrollment.settlement().value()
            );

            Optional<LandedHolding> existing =
                    Objects.requireNonNull(
                            holdingLookup.apply(
                                    enrollment.holding()
                            ),
                            "holding lookup result"
                    );

            if (existing.isEmpty()) {
                add(
                        issues,
                        Code.HOLDING_MISSING,
                        Severity.CONTROLLED_BLOCKER,
                        "Holding does not exist: "
                                + enrollment.holding()
                );
                continue;
            }

            LandedHolding holding = existing.get();

            if (!holding.id().equals(
                    enrollment.holding()
            )) {
                add(
                        issues,
                        Code.HOLDING_MISSING,
                        Severity.CONTROLLED_BLOCKER,
                        "Holding lookup returned wrong identity: "
                                + enrollment.holding()
                );
                continue;
            }

            if (holding.status() != HoldingStatus.ACTIVE) {
                add(
                        issues,
                        Code.HOLDING_NOT_ACTIVE,
                        Severity.CONTROLLED_BLOCKER,
                        "Holding is not active: "
                                + enrollment.holding()
                                + " (" + holding.status() + ")"
                );
            }

            /*
             * Region and territory holdings are
             * political/geographic aggregates.
             *
             * Treating them as individual productive
             * farms risks counting their child estates
             * a second time.
             */
            if (holding.type() == HoldingType.REGION
                    || holding.type()
                    == HoldingType.TERRITORY) {
                add(
                        issues,
                        Code.NON_PRODUCING_HOLDING,
                        Severity.CONTROLLED_BLOCKER,
                        "Aggregate holding cannot be enrolled "
                                + "as a farm: "
                                + enrollment.holding()
                );
            }

            if (!evidence.confirmsHoldingSettlementLink(
                    holding.id(),
                    holding.worldLocationId(),
                    enrollment.settlement()
            )) {
                add(
                        issues,
                        Code.GEOGRAPHY_NOT_VERIFIED,
                        Severity.CONTROLLED_BLOCKER,
                        "No verified geography link for "
                                + holding.id()
                                + " -> "
                                + enrollment.settlement()
                );
            }

            if (!evidence.confirmsCalibratedProduction(
                    holding.id()
            )) {
                add(
                        issues,
                        Code.PRODUCTION_NOT_CALIBRATED,
                        Severity.CONTROLLED_BLOCKER,
                        "No verified agricultural production "
                                + "inputs for "
                                + holding.id()
                );
            }
        }

        /*
         * The existing individual NPC registry is
         * not sufficient proof of TOTAL population.
         *
         * Require explicit evidence of demographic
         * coverage or cohort reconciliation.
         */
        for (long id : settlementIds) {
            SettlementId settlement =
                    new SettlementId(id);

            if (!evidence.confirmsCompletePopulation(
                    settlement
            )) {
                add(
                        issues,
                        Code.POPULATION_NOT_VERIFIED,
                        Severity.CONTROLLED_BLOCKER,
                        "Complete population is not established "
                                + "for " + settlement
                );
            }
        }

        CampaignSettlementAgricultureDispatcher.Readiness
                preview = null;

        /*
         * Reuse the entire economic preflight:
         * ownership, household membership, balances,
         * journal capacity, snapshot authority, and
         * durable interval progress.
         *
         * It is read-only.
         */
        try {
            preview = dispatcher.inspect(
                    startDayInclusive,
                    endDayExclusive
            );
        } catch (IOException | RuntimeException exception) {
            add(
                    issues,
                    Code.DISPATCHER_NOT_READY,
                    Severity.CONTROLLED_BLOCKER,
                    exception.getClass().getSimpleName()
                            + ": " + exception.getMessage()
            );
        }

        /*
         * This is an honest architectural blocker.
         *
         * Economic snapshots and agricultural ledger
         * files are durable but are not atomically
         * committed together with all campaign,
         * NPC and political state.
         */
        add(
                issues,
                Code.CROSS_DOMAIN_SAVE_NOT_ATOMIC,
                Severity.AUTONOMOUS_BLOCKER,
                "Full campaign, population, holding, economy "
                        + "and interval checkpoint coordination "
                        + "is not implemented"
        );

        return new Assessment(
                startDayInclusive,
                endDayExclusive,
                enrollments.size(),
                settlementIds.size(),
                preview,
                issues
        );
    }

    private static void add(
            List<Issue> issues,
            Code code,
            Severity severity,
            String explanation
    ) {
        issues.add(
                new Issue(
                        code,
                        severity,
                        explanation
                )
        );
    }
}

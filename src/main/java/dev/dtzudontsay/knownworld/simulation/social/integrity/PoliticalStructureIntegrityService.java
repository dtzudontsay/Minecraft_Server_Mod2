package dev.dtzudontsay.knownworld.simulation.social.integrity;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyAllegianceManager;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingStatus;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingManager;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PoliticalStructureIntegrityService {

    private final NpcSimulation simulation;

    private final DynastyManager dynasties;

    private final DynastyAllegianceManager allegiances;

    private final LandedHoldingManager holdings;

    private final WorldReferenceCatalog references;

    public PoliticalStructureIntegrityService(
            NpcSimulation simulation,
            DynastyManager dynasties,
            DynastyAllegianceManager allegiances,
            LandedHoldingManager holdings,
            WorldReferenceCatalog references
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

        this.allegiances =
                Objects.requireNonNull(
                        allegiances,
                        "allegiances"
                );

        this.holdings =
                Objects.requireNonNull(
                        holdings,
                        "holdings"
                );

        this.references =
                Objects.requireNonNull(
                        references,
                        "references"
                );
    }

    public PoliticalStructureIntegrityReport audit() {

        List<PoliticalStructureIntegrityReport.Issue> issues =
                new ArrayList<>();

        auditHoldings(
                issues
        );

        auditDynastyHierarchy(
                issues
        );

        auditCurrentAllegiance(
                issues
        );

        issues.sort(
                Comparator
                        .comparing(
                                PoliticalStructureIntegrityReport.Issue::severity
                        )
                        .thenComparing(
                                PoliticalStructureIntegrityReport.Issue::code
                        )
                        .thenComparing(
                                PoliticalStructureIntegrityReport.Issue::subject
                        )
        );

        return new PoliticalStructureIntegrityReport(
                holdings.size(),
                dynasties.size(),
                allegiances.overrideCount(),
                issues
        );
    }

    public PoliticalStructureIntegrityReport auditStrict() {

        PoliticalStructureIntegrityReport report =
                audit();

        if (!report.clean()) {

            StringBuilder message =
                    new StringBuilder(
                            "Political structure integrity audit failed with "
                    )
                            .append(
                                    report.errorCount()
                            )
                            .append(
                                    " error(s)."
                            );

            int shown =
                    0;

            for (
                    PoliticalStructureIntegrityReport.Issue issue :
                    report.errors()
            ) {

                message.append(
                                System.lineSeparator()
                        )
                        .append(
                                " - ["
                        )
                        .append(
                                issue.code()
                        )
                        .append(
                                "] "
                        )
                        .append(
                                issue.subject()
                        )
                        .append(
                                ": "
                        )
                        .append(
                                issue.message()
                        );

                shown++;

                if (shown >= 10) {

                    if (report.errorCount() > shown) {

                        message.append(
                                        System.lineSeparator()
                                )
                                .append(
                                        " - ... and "
                                )
                                .append(
                                        report.errorCount() - shown
                                )
                                .append(
                                        " more error(s)."
                                );
                    }

                    break;
                }
            }

            throw new IllegalStateException(
                    message.toString()
            );
        }

        return report;
    }

    private void auditHoldings(
            List<PoliticalStructureIntegrityReport.Issue> issues
    ) {

        Map<DynastyId, List<LandedHolding>> capitalsByDynasty =
                new LinkedHashMap<>();

        for (
                LandedHolding holding :
                holdings.all()
        ) {

            String subject =
                    holdingSubject(
                            holding
                    );

            if (references.location(
                    holding.worldLocationId()
            ).isEmpty()) {

                error(
                        issues,
                        "HOLDING_MISSING_LOCATION",
                        subject,
                        "References unknown world location "
                                + holding.worldLocationId()
                );
            }

            if (holding.parentHoldingId() != null
                    && holdings.find(
                    holding.parentHoldingId()
            ).isEmpty()) {

                error(
                        issues,
                        "HOLDING_MISSING_PARENT",
                        subject,
                        "References missing parent holding "
                                + holding.parentHoldingId()
                );
            }

            auditHoldingDynasty(
                    issues,
                    holding,
                    holding.deJureDynastyId(),
                    "de-jure",
                    true
            );

            auditHoldingDynasty(
                    issues,
                    holding,
                    holding.ownerDynastyId(),
                    "current owner",
                    false
            );

            auditHoldingHolder(
                    issues,
                    holding
            );

            if (holding.governmentOrganizationId() != null
                    && simulation.organizations()
                    .find(
                            holding.governmentOrganizationId()
                    )
                    .isEmpty()) {

                error(
                        issues,
                        "HOLDING_MISSING_GOVERNMENT",
                        subject,
                        "References missing government organization "
                                + holding.governmentOrganizationId()
                );
            }

            if (holding.linkedTitleId() != null
                    && simulation.titles()
                    .find(
                            holding.linkedTitleId()
                    )
                    .isEmpty()) {

                error(
                        issues,
                        "HOLDING_MISSING_TITLE",
                        subject,
                        "References missing linked title "
                                + holding.linkedTitleId()
                );
            }

            if (holding.capital()
                    && holding.ownerDynastyId() != null) {

                capitalsByDynasty
                        .computeIfAbsent(
                                holding.ownerDynastyId(),
                                ignored ->
                                        new ArrayList<>()
                        )
                        .add(
                                holding
                        );
            }

            if (holding.deJureDynastyId() != null
                    && holding.ownerDynastyId() != null
                    && !holding.deJureDynastyId()
                    .equals(
                            holding.ownerDynastyId()
                    )) {

                info(
                        issues,
                        "HOLDING_OWNER_DIVERGES_FROM_DE_JURE",
                        subject,
                        "Current owner differs from de-jure dynasty. This is legal runtime state and should be preserved."
                );
            }

            if (holding.status() == HoldingStatus.ACTIVE
                    && holding.ownerDynastyId() == null) {

                info(
                        issues,
                        "ACTIVE_HOLDING_WITHOUT_DYNASTIC_OWNER",
                        subject,
                        "Active holding has no dynasty owner. This can be valid for civic, institutional or unassigned territory."
                );
            }
        }

        auditHoldingCycles(
                issues
        );

        for (
                Map.Entry<DynastyId, List<LandedHolding>> entry :
                capitalsByDynasty.entrySet()
        ) {

            List<LandedHolding> capitals =
                    entry.getValue();

            if (capitals.size() <= 1) {
                continue;
            }

            Dynasty dynasty =
                    dynasties.require(
                            entry.getKey()
                    );

            warning(
                    issues,
                    "MULTIPLE_CAPITAL_HOLDINGS",
                    dynastySubject(
                            dynasty
                    ),
                    "Owns multiple holdings marked capital: "
                            + capitals.stream()
                            .map(
                                    LandedHolding::authoredId
                            )
                            .toList()
            );
        }
    }

    private void auditHoldingDynasty(
            List<PoliticalStructureIntegrityReport.Issue> issues,
            LandedHolding holding,
            DynastyId dynastyId,
            String relationship,
            boolean deJure
    ) {

        if (dynastyId == null) {
            return;
        }

        Dynasty dynasty =
                dynasties.find(
                                dynastyId
                        )
                        .orElse(
                                null
                        );

        if (dynasty == null) {

            error(
                    issues,
                    "HOLDING_MISSING_DYNASTY",
                    holdingSubject(
                            holding
                    ),
                    "References unknown "
                            + relationship
                            + " dynasty "
                            + dynastyId
            );

            return;
        }

        if (holding.status() == HoldingStatus.ACTIVE
                && !dynasty.activeAtScenarioStart()) {

            warning(
                    issues,
                    deJure
                            ? "ACTIVE_HOLDING_INACTIVE_AT_START_DE_JURE_DYNASTY"
                            : "ACTIVE_HOLDING_INACTIVE_AT_START_OWNER_DYNASTY",
                    holdingSubject(
                            holding
                    ),
                    "Active holding references inactive scenario-start "
                            + relationship
                            + " dynasty "
                            + dynasty.authoredId()
            );
        }
    }

    private void auditHoldingHolder(
            List<PoliticalStructureIntegrityReport.Issue> issues,
            LandedHolding holding
    ) {

        NpcId holderId =
                holding.holderNpcId();

        if (holderId == null) {
            return;
        }

        NpcState holder =
                simulation.registry()
                        .find(
                                holderId
                        )
                        .orElse(
                                null
                        );

        if (holder == null) {

            error(
                    issues,
                    "HOLDING_MISSING_HOLDER",
                    holdingSubject(
                            holding
                    ),
                    "References unknown NPC holder "
                            + holderId
            );

            return;
        }

        if (holding.status() == HoldingStatus.ACTIVE
                && !holder.isAlive()) {

            error(
                    issues,
                    "ACTIVE_HOLDING_DEAD_HOLDER",
                    holdingSubject(
                            holding
                    ),
                    "Current holder "
                            + holder.identity()
                            .fullName()
                            + " is not alive"
            );
        }
    }

    private void auditHoldingCycles(
            List<PoliticalStructureIntegrityReport.Issue> issues
    ) {

        for (
                LandedHolding start :
                holdings.all()
        ) {

            Set<HoldingId> visited =
                    new LinkedHashSet<>();

            LandedHolding current =
                    start;

            int safety =
                    0;

            while (current != null) {

                if (!visited.add(
                        current.id()
                )) {

                    error(
                            issues,
                            "HOLDING_PARENT_CYCLE",
                            holdingSubject(
                                    start
                            ),
                            "Holding parent hierarchy contains a cycle involving numeric holding "
                                    + current.id()
                    );

                    break;
                }

                HoldingId parentId =
                        current.parentHoldingId();

                if (parentId == null) {
                    break;
                }

                current =
                        holdings.find(
                                        parentId
                                )
                                .orElse(
                                        null
                                );

                if (current == null) {
                    break;
                }

                safety++;

                if (safety > holdings.size() + 1) {

                    error(
                            issues,
                            "HOLDING_PARENT_CYCLE",
                            holdingSubject(
                                    start
                            ),
                            "Holding parent hierarchy exceeded safe traversal depth"
                    );

                    break;
                }
            }
        }
    }

    private void auditDynastyHierarchy(
            List<PoliticalStructureIntegrityReport.Issue> issues
    ) {

        for (
                Dynasty dynasty :
                dynasties.all()
        ) {

            DynastyId liegeId =
                    dynasty.liegeDynasty();

            if (liegeId == null) {
                continue;
            }

            Dynasty liege =
                    dynasties.find(
                                    liegeId
                            )
                            .orElse(
                                    null
                            );

            if (liege == null) {

                error(
                        issues,
                        "DE_JURE_LIEGE_MISSING",
                        dynastySubject(
                                dynasty
                        ),
                        "References missing de-jure liege "
                                + liegeId
                );

                continue;
            }

            if (dynasty.activeAtScenarioStart()
                    && !liege.activeAtScenarioStart()) {

                warning(
                        issues,
                        "ACTIVE_DYNASTY_INACTIVE_AT_START_DE_JURE_LIEGE",
                        dynastySubject(
                                dynasty
                        ),
                        "Active dynasty has inactive scenario-start de-jure liege "
                                + liege.authoredId()
                );
            }
        }

        auditDeJureLiegeCycles(
                issues
        );
    }

    private void auditDeJureLiegeCycles(
            List<PoliticalStructureIntegrityReport.Issue> issues
    ) {

        for (
                Dynasty start :
                dynasties.all()
        ) {

            Set<DynastyId> visited =
                    new LinkedHashSet<>();

            Dynasty current =
                    start;

            int safety =
                    0;

            while (current != null) {

                if (!visited.add(
                        current.id()
                )) {

                    error(
                            issues,
                            "DE_JURE_LIEGE_CYCLE",
                            dynastySubject(
                                    start
                            ),
                            "De-jure liege hierarchy contains a cycle involving "
                                    + dynastySubject(
                                    current
                            )
                    );

                    break;
                }

                DynastyId liegeId =
                        current.liegeDynasty();

                if (liegeId == null) {
                    break;
                }

                current =
                        dynasties.find(
                                        liegeId
                                )
                                .orElse(
                                        null
                                );

                if (current == null) {
                    break;
                }

                safety++;

                if (safety > dynasties.size() + 1) {

                    error(
                            issues,
                            "DE_JURE_LIEGE_CYCLE",
                            dynastySubject(
                                    start
                            ),
                            "De-jure liege hierarchy exceeded safe traversal depth"
                    );

                    break;
                }
            }
        }
    }

    private void auditCurrentAllegiance(
            List<PoliticalStructureIntegrityReport.Issue> issues
    ) {

        for (
                Dynasty dynasty :
                dynasties.all()
        ) {

            try {

                List<Dynasty> chain =
                        allegiances.liegeChain(
                                dynasty.id()
                        );

                if (!chain.isEmpty()) {

                    Dynasty currentLiege =
                            chain.get(
                                    0
                            );

                    if (dynasty.activeAtScenarioStart()
                            && !currentLiege.activeAtScenarioStart()) {

                        warning(
                                issues,
                                "ACTIVE_DYNASTY_INACTIVE_AT_START_CURRENT_LIEGE",
                                dynastySubject(
                                        dynasty
                                ),
                                "Active dynasty is currently sworn to inactive dynasty "
                                        + currentLiege.authoredId()
                        );
                    }
                }

            } catch (
                    RuntimeException exception
            ) {

                error(
                        issues,
                        "CURRENT_ALLEGIANCE_CYCLE_OR_INVALID_CHAIN",
                        dynastySubject(
                                dynasty
                        ),
                        exception.getMessage() == null
                                ? exception.getClass()
                                .getSimpleName()
                                : exception.getMessage()
                );
            }

            if (!allegiances.hasOverride(
                    dynasty.id()
            )) {
                continue;
            }

            if (allegiances.isExplicitlyIndependent(
                    dynasty.id()
            )) {

                info(
                        issues,
                        "CURRENT_ALLEGIANCE_INDEPENDENCE_OVERRIDE",
                        dynastySubject(
                                dynasty
                        ),
                        "Runtime allegiance explicitly overrides de-jure hierarchy with independence."
                );

                continue;
            }

            Dynasty current =
                    allegiances.currentLiege(
                                    dynasty.id()
                            )
                            .orElse(
                                    null
                            );

            Dynasty deJure =
                    allegiances.deJureLiege(
                                    dynasty.id()
                            )
                            .orElse(
                                    null
                            );

            if (!Objects.equals(
                    current == null
                            ? null
                            : current.id(),
                    deJure == null
                            ? null
                            : deJure.id()
            )) {

                info(
                        issues,
                        "CURRENT_ALLEGIANCE_DIVERGES_FROM_DE_JURE",
                        dynastySubject(
                                dynasty
                        ),
                        "Current liege is "
                                + dynastyName(
                                current
                        )
                                + " while de-jure liege is "
                                + dynastyName(
                                deJure
                        )
                );
            }
        }
    }

    private static String holdingSubject(
            LandedHolding holding
    ) {

        return holding.authoredId() == null
                ? holding.name()
                + " [#"
                + holding.id()
                + "]"
                : holding.name()
                + " ["
                + holding.authoredId()
                + "]";
    }

    private static String dynastySubject(
            Dynasty dynasty
    ) {

        return dynasty.authoredId() == null
                ? dynasty.name()
                + " [#"
                + dynasty.id()
                + "]"
                : dynasty.name()
                + " ["
                + dynasty.authoredId()
                + "]";
    }

    private static String dynastyName(
            Dynasty dynasty
    ) {

        return dynasty == null
                ? "none"
                : dynastySubject(
                dynasty
        );
    }

    private static void error(
            List<PoliticalStructureIntegrityReport.Issue> issues,
            String code,
            String subject,
            String message
    ) {

        add(
                issues,
                PoliticalStructureIntegrityReport.Severity.ERROR,
                code,
                subject,
                message
        );
    }

    private static void warning(
            List<PoliticalStructureIntegrityReport.Issue> issues,
            String code,
            String subject,
            String message
    ) {

        add(
                issues,
                PoliticalStructureIntegrityReport.Severity.WARNING,
                code,
                subject,
                message
        );
    }

    private static void info(
            List<PoliticalStructureIntegrityReport.Issue> issues,
            String code,
            String subject,
            String message
    ) {

        add(
                issues,
                PoliticalStructureIntegrityReport.Severity.INFO,
                code,
                subject,
                message
        );
    }

    private static void add(
            List<PoliticalStructureIntegrityReport.Issue> issues,
            PoliticalStructureIntegrityReport.Severity severity,
            String code,
            String subject,
            String message
    ) {

        issues.add(
                new PoliticalStructureIntegrityReport.Issue(
                        severity,
                        code,
                        subject,
                        message
                )
        );
    }
}
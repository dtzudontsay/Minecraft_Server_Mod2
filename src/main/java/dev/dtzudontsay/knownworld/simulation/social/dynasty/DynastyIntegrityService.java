package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DynastyIntegrityService {

    private final NpcSimulation simulation;

    private final DynastyManager dynasties;

    public DynastyIntegrityService(
            NpcSimulation simulation,
            DynastyManager dynasties
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
    }

    public DynastyIntegrityReport audit() {

        List<String> errors =
                new ArrayList<>();

        List<String> warnings =
                new ArrayList<>();

        Set<String> authoredIds =
                new HashSet<>();

        Set<OrganizationId> organizationIds =
                new HashSet<>();

        int authoredCount =
                0;

        int generatedCount =
                0;

        int activeCount =
                0;

        int inactiveCount =
                0;

        int activeOrganizations =
                0;

        int extinct =
                0;

        int exiled =
                0;

        int dispossessed =
                0;

        int dormant =
                0;

        int notYetFounded =
                0;

        int unknownStatus =
                0;

        int withoutHeads =
                0;

        int withoutHeirs =
                0;

        int withoutHome =
                0;

        int withoutCulture =
                0;

        int withoutReligion =
                0;

        for (
                Dynasty dynasty :
                dynasties.all()
        ) {

            String label =
                    label(
                            dynasty
                    );

            if (dynasty.hasAuthoredId()) {

                authoredCount++;

                if (!authoredIds.add(
                        dynasty.authoredId()
                )) {

                    errors.add(
                            "Duplicate authored dynasty ID: "
                                    + dynasty.authoredId()
                    );
                }

            } else {

                generatedCount++;
            }

            if (dynasty.activeAtScenarioStart()) {

                activeCount++;

                if (!dynasty.hasOrganization()) {

                    errors.add(
                            label
                                    + " is active at scenario start but has no runtime organization"
                    );

                } else {

                    activeOrganizations++;
                }

                if (dynasty.status()
                        == DynastyStatus.EXTINCT) {

                    errors.add(
                            label
                                    + " is active at scenario start but status is EXTINCT"
                    );
                }

                if (dynasty.status()
                        == DynastyStatus.NOT_YET_FOUNDED) {

                    errors.add(
                            label
                                    + " is active at scenario start but status is NOT_YET_FOUNDED"
                    );
                }

            } else {

                inactiveCount++;

                if (dynasty.hasOrganization()) {

                    errors.add(
                            label
                                    + " is inactive at scenario start but still owns a runtime organization"
                    );
                }
            }

            switch (dynasty.status()) {

                case EXTINCT ->
                        extinct++;

                case EXILED ->
                        exiled++;

                case DISPOSSESSED ->
                        dispossessed++;

                case DORMANT ->
                        dormant++;

                case NOT_YET_FOUNDED ->
                        notYetFounded++;

                case UNKNOWN ->
                        unknownStatus++;

                default -> {
                }
            }

            if (dynasty.hasOrganization()) {

                Organization organization =
                        simulation.organizations()
                                .find(
                                        dynasty.organizationId()
                                )
                                .orElse(
                                        null
                                );

                if (organization == null) {

                    errors.add(
                            label
                                    + " references missing organization "
                                    + dynasty.organizationId()
                    );

                } else {

                    if (organization.type()
                            != dynasty.type()
                            .organizationType()) {

                        errors.add(
                                label
                                        + " organization type mismatch: dynasty requires "
                                        + dynasty.type()
                                        .organizationType()
                                        + " but organization is "
                                        + organization.type()
                        );
                    }

                    if (!organizationIds.add(
                            dynasty.organizationId()
                    )) {

                        errors.add(
                                "Runtime organization "
                                        + dynasty.organizationId()
                                        + " is attached to more than one dynasty"
                        );
                    }
                }
            }

            validateRelationship(
                    dynasty,
                    dynasty.parentDynasty(),
                    "parent",
                    errors
            );

            validateRelationship(
                    dynasty,
                    dynasty.liegeDynasty(),
                    "liege",
                    errors
            );

            validateRelationship(
                    dynasty,
                    dynasty.predecessorDynasty(),
                    "predecessor",
                    errors
            );

            validateRelationship(
                    dynasty,
                    dynasty.successorDynasty(),
                    "successor",
                    errors
            );

            if (dynasty.parentDynasty() != null
                    && dynasty.type()
                    != DynastyType.CADET_BRANCH) {

                warnings.add(
                        label
                                + " has parent dynasty "
                                + dynasty.parentDynasty()
                                + " but type is "
                                + dynasty.type()
                );
            }

            if (dynasty.type()
                    == DynastyType.CADET_BRANCH
                    && dynasty.parentDynasty() == null) {

                warnings.add(
                        label
                                + " is a CADET_BRANCH but has no known parent dynasty"
                );
            }

            if (dynasty.head() != null
                    && !simulation.registry()
                    .contains(
                            dynasty.head()
                    )) {

                errors.add(
                        label
                                + " references missing head NPC "
                                + dynasty.head()
                );
            }

            if (dynasty.heir() != null
                    && !simulation.registry()
                    .contains(
                            dynasty.heir()
                    )) {

                errors.add(
                        label
                                + " references missing heir NPC "
                                + dynasty.heir()
                );
            }

            if (dynasty.head() != null
                    && dynasty.head()
                    .equals(
                            dynasty.heir()
                    )) {

                warnings.add(
                        label
                                + " has the same NPC as head and heir: "
                                + dynasty.head()
                );
            }

            if (dynasty.activeAtScenarioStart()
                    && dynasty.head() == null) {

                withoutHeads++;
            }

            if (dynasty.activeAtScenarioStart()
                    && dynasty.heir() == null) {

                withoutHeirs++;
            }

            if (dynasty.homeLocationId() == null) {

                withoutHome++;

                if (requiresLandedBaseline(
                        dynasty
                )) {

                    warnings.add(
                            label
                                    + " has no home/seat region baseline"
                    );
                }
            }

            if (dynasty.cultureId() == null) {

                withoutCulture++;

                if (dynasty.activeAtScenarioStart()) {

                    warnings.add(
                            label
                                    + " is active but has no culture baseline"
                    );
                }
            }

            if (dynasty.religionId() == null) {

                withoutReligion++;

                if (isWesterosiHouse(
                        dynasty.type()
                )
                        && dynasty.activeAtScenarioStart()) {

                    warnings.add(
                            label
                                    + " is an active Westerosi-style house without a religion baseline"
                    );
                }
            }

            if (dynasty.foundedYear() != null
                    && dynasty.extinctYear() != null
                    && dynasty.extinctYear()
                    < dynasty.foundedYear()) {

                errors.add(
                        label
                                + " becomes extinct before its founding year"
                );
            }
        }

        return new DynastyIntegrityReport(
                dynasties.size(),
                authoredCount,
                generatedCount,
                activeCount,
                inactiveCount,
                activeOrganizations,
                extinct,
                exiled,
                dispossessed,
                dormant,
                notYetFounded,
                unknownStatus,
                withoutHeads,
                withoutHeirs,
                withoutHome,
                withoutCulture,
                withoutReligion,
                errors,
                warnings
        );
    }

    public DynastyIntegrityReport auditStrict() {

        DynastyIntegrityReport report =
                audit();

        if (!report.passed()) {

            String firstError =
                    report.errors()
                            .isEmpty()
                            ? "unknown dynasty integrity error"
                            : report.errors()
                            .getFirst();

            throw new IllegalStateException(
                    "Dynasty integrity audit failed with "
                            + report.errorCount()
                            + " error(s). First error: "
                            + firstError
            );
        }

        return report;
    }

    private void validateRelationship(
            Dynasty dynasty,
            DynastyId target,
            String relationship,
            List<String> errors
    ) {

        if (target == null) {
            return;
        }

        if (target.equals(
                dynasty.id()
        )) {

            errors.add(
                    label(
                            dynasty
                    )
                            + " is its own "
                            + relationship
            );

            return;
        }

        if (dynasties.find(
                target
        ).isEmpty()) {

            errors.add(
                    label(
                            dynasty
                    )
                            + " references missing "
                            + relationship
                            + " dynasty "
                            + target
            );
        }
    }

    private static boolean requiresLandedBaseline(
            Dynasty dynasty
    ) {

        if (!dynasty.activeAtScenarioStart()) {

            return false;
        }

        return switch (dynasty.type()) {

            case ROYAL_HOUSE,
                 GREAT_HOUSE,
                 NOBLE_HOUSE,
                 LANDED_KNIGHTLY_HOUSE,
                 CADET_BRANCH ->
                    true;

            case EXILED_DYNASTY ->
                    false;

            default ->
                    false;
        };
    }

    private static boolean isWesterosiHouse(
            DynastyType type
    ) {

        return switch (type) {

            case ROYAL_HOUSE,
                 GREAT_HOUSE,
                 NOBLE_HOUSE,
                 LANDED_KNIGHTLY_HOUSE,
                 CADET_BRANCH,
                 EXILED_DYNASTY ->
                    true;

            default ->
                    false;
        };
    }

    private static String label(
            Dynasty dynasty
    ) {

        if (dynasty.hasAuthoredId()) {

            return dynasty.authoredId()
                    + " ("
                    + dynasty.name()
                    + ")";
        }

        return "#"
                + dynasty.id()
                + " ("
                + dynasty.name()
                + ")";
    }
}
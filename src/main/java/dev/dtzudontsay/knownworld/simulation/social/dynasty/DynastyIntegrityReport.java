package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import java.util.List;

public record DynastyIntegrityReport(
        int totalDynasties,
        int authoredDynasties,
        int generatedDynasties,
        int activeAtScenarioStart,
        int inactiveAtScenarioStart,
        int activeRuntimeOrganizations,
        int extinctDynasties,
        int exiledDynasties,
        int dispossessedDynasties,
        int dormantDynasties,
        int notYetFoundedDynasties,
        int unknownStatusDynasties,
        int dynastiesWithoutHeads,
        int dynastiesWithoutHeirs,
        int dynastiesWithoutHome,
        int dynastiesWithoutCulture,
        int dynastiesWithoutReligion,
        List<String> errors,
        List<String> warnings
) {

    public DynastyIntegrityReport {

        errors =
                List.copyOf(
                        errors
                );

        warnings =
                List.copyOf(
                        warnings
                );
    }

    public boolean passed() {

        return errors.isEmpty();
    }

    public int errorCount() {

        return errors.size();
    }

    public int warningCount() {

        return warnings.size();
    }
}
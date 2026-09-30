package dev.dtzudontsay.knownworld.simulation.social.dynasty;

/**
 * Result of reconciling the authored dynasty catalog with the
 * persistent runtime state at scenario startup.
 */
public record DynastyActivationReport(
        int cataloguedDynasties,
        int newlyCreatedDynasties,
        int reconciledDynasties,
        int stalePersistedDynastiesRemoved,
        int activeDynasties,
        int inactiveDynasties,
        int activeOrganizations,
        int resolvedHeads,
        int unresolvedHeads,
        int resolvedHeirs,
        int unresolvedHeirs
) {
}

package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoleType;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutine;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutineManager;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Reconciles *distinct* existing persistent NPCs and anonymous population.
 * Nothing is spawned, assigned to a household, or added to inventory here.
 */
public final class SettlementPopulationCensus {
    public record Result(SettlementId settlement, boolean surveyed,
                         long represented, long anonymous, long total,
                         long representedFarmers, long anonymousFarmers,
                         long availableFarmWorkers, long residentsWithoutHousehold,
                         long livingNpcsWithoutResidence) {
        /** A surveyed count is a scenario assertion, not proof of historical accuracy. */
        public boolean hasPopulationCoverage() { return surveyed; }
    }

    private final SettlementManager settlements;
    private final NpcRegistry npcs;
    private final NpcAffiliationManager affiliations;
    private final NpcRoutineManager routines;
    private final PopulationCohortManager cohorts;
    private final CampaignCalendar calendar;

    public SettlementPopulationCensus(SettlementManager settlements,
                                      NpcRegistry npcs,
                                      NpcAffiliationManager affiliations,
                                      NpcRoutineManager routines,
                                      PopulationCohortManager cohorts,
                                      CampaignCalendar calendar) {
        this.settlements = Objects.requireNonNull(settlements);
        this.npcs = Objects.requireNonNull(npcs);
        this.affiliations = Objects.requireNonNull(affiliations);
        this.routines = Objects.requireNonNull(routines);
        this.cohorts = Objects.requireNonNull(cohorts);
        this.calendar = Objects.requireNonNull(calendar);
    }

    public Result forSettlement(SettlementId settlement) {
        Objects.requireNonNull(settlement, "settlement");
        if (settlements.find(settlement).isEmpty()) {
            throw new IllegalArgumentException("Unknown settlement: " + settlement);
        }
        Map<NpcId, NpcAffiliation> placement = new HashMap<>();
        for (NpcAffiliation affiliation : affiliations.all()) {
            placement.put(affiliation.owner(), affiliation);
        }
        Map<NpcId, NpcRoleType> roles = new HashMap<>();
        for (NpcRoutine routine : routines.all()) {
            roles.put(routine.owner(), routine.role());
        }

        long named = 0L;
        long namedFarmers = 0L;
        long unassignedHouseholds = 0L;
        long withoutResidence = 0L;
        for (NpcState npc : npcs.all()) {
            if (!npc.isAlive()) continue;
            NpcAffiliation association = placement.get(npc.id());
            if (association == null || association.residenceSettlement() == null) {
                withoutResidence = Math.addExact(withoutResidence, 1L);
                continue;
            }
            if (!settlement.equals(association.residenceSettlement())) continue;
            named = Math.addExact(named, 1L);
            if (association.household() == null) {
                unassignedHouseholds = Math.addExact(unassignedHouseholds, 1L);
            }
            // Occupation means *available* labor, not proof that today's shift occurred.
            int age = calendar.year() - npc.identity().birthYear();
            if (age >= 16 && age < 60 && roles.get(npc.id()) == NpcRoleType.FARMER) {
                namedFarmers = Math.addExact(namedFarmers, 1L);
            }
        }
        long anonymous = 0L;
        long anonymousFarmers = 0L;
        PopulationCohortManager.Snapshot snapshot = cohorts.snapshot();
        for (PopulationCohortManager.Cohort cohort : snapshot.cohorts()) {
            var key = cohort.key();
            if (!settlement.equals(key.settlement())) continue;
            anonymous = Math.addExact(anonymous, cohort.people());
            if (key.age() == PopulationCohortManager.AgeBand.ADULT
                    && key.occupationId().equals("farmer")) {
                anonymousFarmers = Math.addExact(anonymousFarmers, cohort.people());
            }
        }
        return new Result(settlement, snapshot.surveyed().containsKey(settlement),
                named, anonymous, Math.addExact(named, anonymous),
                namedFarmers, anonymousFarmers,
                Math.addExact(namedFarmers, anonymousFarmers),
                unassignedHouseholds, withoutResidence);
    }
}

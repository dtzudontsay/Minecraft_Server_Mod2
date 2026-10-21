package dev.dtzudontsay.knownworld.simulation.integration;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class KnownWorldSettlementPresetsTest {
    @Test
    void preservesAllMapMarkersAndPoliticalHoldings() {
        var index = KnownWorldSettlementPresets.index();
        assertEquals(253, index.markerToSite().size());
        assertEquals(259, index.holdings().size());
        assertTrue(index.sites().size() <= 253);
        assertTrue(index.sites().size() > 0);
    }

    @Test
    void specialPlacesAreNotNormalFarmingVillages() {
        var fort = KnownWorldSettlementPresets.findMarker("NEE-005").orElseThrow();
        assertTrue(fort.mode().contains("MONUMENTAL"));
        assertEquals("SPECIAL", fort.tier());
        var ruined = KnownWorldSettlementPresets.findMarker("SEE-009").orElseThrow();
        assertTrue(ruined.mode().contains("RUIN"));
        assertEquals(0, ruined.planningPopulation());
    }

    @Test
    void allSitesRemainConstructionDisabled() {
        for (var site : KnownWorldSettlementPresets.index().sites().values()) {
            assertFalse(site.physicalConstructionEnabled(), site.siteId());
            assertFalse(site.markerIds().isEmpty(), site.siteId());
        }
    }

    @Test
    void workplaceCapacitiesAndHouseholdsRemainPlanningOnly() {
        var index = KnownWorldSettlementPresets.index();
        var farm = index.workplaces().get("FARM");
        assertNotNull(farm);
        assertEquals(12, farm.maxStaff());
        var winterfell = KnownWorldSettlementPresets.findMarker("NW-029").orElseThrow();
        assertTrue(winterfell.housingPlots() >= 0);
        assertTrue(winterfell.housingBacklog() >= 0);
        assertNotNull(index.holdings().get("winterfell_domain"));
    }
}

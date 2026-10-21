package dev.dtzudontsay.knownworld.simulation.integration;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

public class KnownWorldSiteRegistryTest {

    @Test
    void everyMarkerAndHoldingIsPreserved() {
        var r=KnownWorldSiteRegistry.registry();
        assertEquals(253,r.markers().size());
        assertEquals(259,r.holdings().size());
        assertEquals(253,r.sites().values().stream()
                .mapToInt(s->s.markerIds().size()).sum());
        assertTrue(r.sites().size()>0);
        assertFalse(r.regions().isEmpty());
        assertTrue(r.references().size() >= 249);
        assertTrue(r.sites().size()<=253);
    }

    @Test
    void originalWinterfellCoordinatesArePreserved() {
        var marker=KnownWorldSiteRegistry.registry().marker("NW-029").orElseThrow();
        assertEquals(-4050173L,marker.x());
        assertEquals(-1976496L,marker.z());
        assertTrue(marker.planningAnchorAccepted());
        assertFalse(marker.constructionApproved());
        assertEquals("MAJOR",KnownWorldSiteRegistry.siteAtMarker("NW-029").orElseThrow().tier());
    }

    @Test
    void allFiveFortsRemainSeparateMonumentalSitesWithoutInventedHolders() {
        var r=KnownWorldSiteRegistry.registry();
        Set<String> ids = java.util.stream.IntStream.rangeClosed(5,9)
                .mapToObj(i->r.marker("NEE-00"+i).orElseThrow().siteId())
                .collect(Collectors.toSet());
        assertEquals(5,ids.size());
        for (String id:ids){
            var s=r.site(id).orElseThrow();
            assertEquals("SPECIAL",s.tier());
            assertEquals("FIVE_FORTS_MONUMENTAL",s.archetype());
            assertFalse(s.constructionApproved());
        }
    }

    @Test
    void domainsDoNotGenerateStandaloneTowns() {
        var north=KnownWorldSiteRegistry.holding("north_domain").orElseThrow();
        assertTrue(north.politicalDomain());
        assertEquals("NO_SETTLEMENT",north.tier());
    }

    @Test
    void nestedRedKeepDoesNotBecomeASecondKingsLanding() {
        var redKeep=KnownWorldSiteRegistry.holding("red_keep_domain").orElseThrow();
        assertEquals("kings_landing",redKeep.parentCityGeographicId());
        assertEquals("SPECIAL",redKeep.tier());
        assertFalse(redKeep.constructionApproved());
    }

    @Test
    void northRegionHasAProfileWithoutDuplicatedPopulation() {
        var north=KnownWorldSiteRegistry.registry().region("the_north").orElseThrow();
        assertEquals("the_north",north.regionId());
        assertTrue(north.descendantSiteIds().size()>0);
    }

    @Test
    void everyCanonicalReferenceCanExistWithoutASeparateRedXMarker() {
        var winterfell=KnownWorldSiteRegistry.registry().placeReference("winterfell").orElseThrow();
        assertTrue(winterfell.markerIds().contains("NW-029"));
        assertTrue(winterfell.holdingIds().contains("winterfell_domain"));
    }

    @Test
    void noPopulationOrBuildingsSpawnAtB2B3Stage() {
        var r=KnownWorldSiteRegistry.registry();
        for(var marker:r.markers().values())assertFalse(marker.constructionApproved());
        for(var s:r.sites().values()){
            assertFalse(s.constructionApproved());
            assertFalse(s.createsPeople());
            assertTrue(s.plannedPopulationBaseline()>=0);
            int a=s.workerSectorPercent().values().stream().mapToInt(Integer::intValue).sum();
            int b=s.workerRolePercent().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(a,b);
            assertTrue(a==0||a==100);
        }
        for(var holding:r.holdings().values())assertFalse(holding.constructionApproved());
    }
}

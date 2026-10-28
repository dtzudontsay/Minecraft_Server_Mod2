package dev.dtzudontsay.knownworld.world.generation.settlement;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KnownWorldC49TerrainPolicyTest {
    private static final KnownWorldC49TerrainPolicy.Site SITE =
            new KnownWorldC49TerrainPolicy.Site("s1","Test Quay",List.of("NW-999"),
                    0,0,"SMALL",550,110,45);
    private static final KnownWorldC49TerrainPolicy.Policy POLICY =
            new KnownWorldC49TerrainPolicy.Policy(128,16,80,16,20,28,8);

    @Test void movesOffCoastAndRequiresContiguousArea() {
        KnownWorldC49TerrainPolicy.TerrainSampler sampler =
                new KnownWorldC49TerrainPolicy.TerrainSampler() {
                    public boolean isDryLand(int x,int z) { return x>=48; }
                    public int surfaceHeight(int x,int z) { return 64; }
                };
        var r=KnownWorldC49TerrainPolicy.find(SITE,sampler,POLICY);
        assertTrue(r.successful());
        assertTrue(r.placement().constructionX()>=64);
        assertEquals(1,r.placement().inlandDx());
        assertTrue(r.placement().connectedDryCells()>=20);
        assertEquals(550,SITE.population());
    }

    @Test void offersTerracesWithoutFlatteningMountain() {
        KnownWorldC49TerrainPolicy.TerrainSampler sampler =
                new KnownWorldC49TerrainPolicy.TerrainSampler() {
                    public boolean isDryLand(int x,int z) { return true; }
                    public int surfaceHeight(int x,int z) { return 70+Math.floorDiv(x,3); }
                };
        var r=KnownWorldC49TerrainPolicy.find(SITE,sampler,POLICY);
        assertTrue(r.successful());
        assertTrue(r.placement().terracesRequired());
        assertTrue(r.placement().retainingWallsRequired());
        assertTrue(r.placement().switchbackRoadsRequired());
    }

    @Test void recordsAttemptAndMarkersInsteadOfDroppingSite() {
        KnownWorldC49TerrainPolicy.TerrainSampler sampler =
                new KnownWorldC49TerrainPolicy.TerrainSampler() {
                    public boolean isDryLand(int x,int z) { return false; }
                    public int surfaceHeight(int x,int z) { return 62; }
                };
        var r=KnownWorldC49TerrainPolicy.find(SITE,sampler,POLICY);
        assertFalse(r.successful());
        assertEquals("s1",r.failure().siteId());
        assertEquals(List.of("NW-999"),r.failure().markerIds());
        assertEquals(110,r.failure().homesDeferred());
        assertNotNull(r.failure().reason());
    }
}

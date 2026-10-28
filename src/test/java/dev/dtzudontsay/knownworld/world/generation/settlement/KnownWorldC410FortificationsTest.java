package dev.dtzudontsay.knownworld.world.generation.settlement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KnownWorldC410FortificationsTest {
    @Test void tierGeometryAndCoreSize() {
        var a=KnownWorldC410Fortifications.forTier("SMALL");
        var b=KnownWorldC410Fortifications.forTier("MEDIUM");
        var c=KnownWorldC410Fortifications.forTier("MAJOR");
        assertEquals(56,a.outerHalfSpan());
        assertEquals(106,b.outerHalfSpan());
        assertEquals(171,c.outerHalfSpan());
        assertEquals(149,c.innerHalfSpan());
        assertEquals(89,c.keepEast()-c.keepWest()+1);
        // C4.15 expands the major complex to include its forward wings.
        // The main hall remains 89 x 64, while its reserved core is 89 x 87.
        assertEquals(35,c.keepNorth());
        assertEquals(121,c.keepSouth());
        assertEquals(87,c.keepSouth()-c.keepNorth()+1);
        assertTrue(c.keepEast()-c.keepWest()+1 > b.keepEast()-b.keepWest()+1);
        assertTrue(c.keepSouth()-c.keepNorth()+1 > b.keepSouth()-b.keepNorth()+1);
        assertThrows(IllegalArgumentException.class,
                     () -> KnownWorldC410Fortifications.forTier("SPECIAL"));
    }
    @Test void curtainsAndGateOpenings() {
        assertTrue(KnownWorldC410Fortifications.inCurtain(171,0,171,3));
        assertFalse(KnownWorldC410Fortifications.inCurtain(165,0,171,3));
        assertTrue(KnownWorldC410Fortifications.isGate(0,171,171));
        assertTrue(KnownWorldC410Fortifications.isGate(-171,0,171));
        assertFalse(KnownWorldC410Fortifications.isGate(120,171,171));
    }
}

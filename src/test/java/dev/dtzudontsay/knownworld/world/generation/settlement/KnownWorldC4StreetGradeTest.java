package dev.dtzudontsay.knownworld.world.generation.settlement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnownWorldC4StreetGradeTest {
    @Test void flatStreetPreservesOriginalLevel() {
        assertEquals(70, KnownWorldC4StreetGrade.height(70, new int[]{70,70,70,70,70}, 70, -1));
    }

    @Test void approachesApprovedDoorWithoutUnboundedPillars() {
        int[] samples = {70,70,71,70,69};
        for (int distance = 0; distance <= 5; distance++) {
            int y = KnownWorldC4StreetGrade.height(70, samples, 78, distance);
            assertTrue(Math.abs(y - 70) <= 4);
            assertTrue(y >= 70);
        }
        assertEquals(74, KnownWorldC4StreetGrade.height(70, samples, 78, 0));
        assertEquals(70, KnownWorldC4StreetGrade.height(70, samples, 78, 5));
    }

    @Test void worksAtNegativeWorldCoordinatesAndForDownhillDoor() {
        assertEquals(66, KnownWorldC4StreetGrade.height(70, new int[]{70,70,70,70,70}, 40, 0));
    }

    @Test void medianRejectsSingleElevationSpike() {
        assertEquals(100, KnownWorldC4StreetGrade.height(100,
                new int[]{100,100,100,100,350}, 100, -1));
    }

    @Test void rejectsIncorrectSampling() {
        assertThrows(IllegalArgumentException.class,
                () -> KnownWorldC4StreetGrade.height(70, new int[]{70}, 70, 0));
    }
}

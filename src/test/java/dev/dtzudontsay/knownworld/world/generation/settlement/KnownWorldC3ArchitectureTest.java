package dev.dtzudontsay.knownworld.world.generation.settlement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Metadata/contract tests. Real terrain and Minecraft blocks need in-world review. */
class KnownWorldC3ArchitectureTest {
    @Test
    void allFortyThreeC1TemplatesLoad() {
        assertEquals(43, KnownWorldC3Architecture.allTemplates().size());
        assertEquals("HOME", KnownWorldC3Architecture.template("HOUSE").family());
        assertEquals("GARDEN", KnownWorldC3Architecture.template("FIELD").family());
    }

    @Test
    void farmAndBakeryHaveSeparateWorkstationsAndCapacity() {
        var farm = KnownWorldC3Architecture.template("FARM");
        var bakery = KnownWorldC3Architecture.template("BAKERY");
        assertTrue(farm.maxStaff() > 0);
        assertTrue(bakery.maxStaff() > 0);
        assertNotEquals(farm.stations(), bakery.stations());
    }

    @Test
    void specialSitesRequireFurtherAuthorization() {
        assertTrue(KnownWorldC3Architecture.template("DOCK").requiresShore());
        assertTrue(KnownWorldC3Architecture.template("BROTHEL").requiresPolicy());
        assertTrue(KnownWorldC3Architecture.template("RUIN").openAir());
        assertTrue(KnownWorldC3Architecture.template("KEEP").floors() > 1);
    }
}

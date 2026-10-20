
package dev.dtzudontsay.knownworld.simulation.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Requires the generated JSON from build_b2_holding_links.py. */
public class KnownWorldHoldingGeoCandidatesTest {

    @Test
    void allHoldingsHaveUniqueAuthoredIdentities() {
        var holdings = KnownWorldHoldingGeoCandidates.all();
        assertEquals(259, holdings.size(), "Scenario should retain all 259 holding records");
        assertTrue(holdings.containsKey("winterfell_domain"));
    }

    @Test
    void markerCandidatesNeverClaimConstructionApproval() {
        var all = KnownWorldHoldingGeoCandidates.all();
        var records = all.values().stream().flatMap(h -> h.candidates().stream()).toList();
        assertTrue(records.size() >= 50, "Expected imported map name evidence");
        for (var marker : records) {
            assertNotNull(marker.identityEvidence());
            assertFalse(marker.id().isBlank());
        }
    }

    @Test
    void domainRecordsRemainDifferentFromPhysicalSeats() {
        var north = KnownWorldHoldingGeoCandidates.find("north_domain").orElseThrow();
        assertTrue(north.domain());
        assertEquals("DOMAIN_NOT_SETTLEMENT", north.status());
    }

    @Test
    void winterfellHasRecognizedMarkerEvidence() {
        var winterfell = KnownWorldHoldingGeoCandidates.find("winterfell_domain")
                .orElseThrow();
        assertFalse(winterfell.domain());
        assertTrue(winterfell.candidates().stream()
                .anyMatch(m -> m.id().equals("NW-029")));
    }
}

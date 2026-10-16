
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingStatus;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationKind;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public final class HoldingAgricultureCatalogTest {
    private static final SettlementId SETTLEMENT = new SettlementId(1L);

    private static LandedHolding holding(long id, String authoredId,
                                         String worldLocation, HoldingType type) {
        return new LandedHolding(
                new HoldingId(id), authoredId, authoredId, type, worldLocation,
                null, null, HoldingStatus.ACTIVE, null, null, null, null,
                false, 0.0, 0.0, 0.0
        );
    }

    private static WorldLocationDefinition location(String id, String parent) {
        return new WorldLocationDefinition(
                id, id, WorldLocationKind.SETTLEMENT, parent,
                null, null, ReferenceProvenance.CANON,
                "Test reference without physical map coordinates", false
        );
    }

    private static Function<String, Optional<WorldLocationDefinition>> lookup() {
        Map<String, WorldLocationDefinition> locations = Map.of(
                "winterfell", location("winterfell", "the_north"),
                "castle_cerwyn", location("castle_cerwyn", "the_north"),
                "highgarden", location("highgarden", "the_reach"),
                "braavos", location("braavos", "essos")
        );
        return id -> Optional.ofNullable(locations.get(id));
    }

    @Test
    void resolvesWinterfellOverrideWithoutCoordinatesOrPopulationWeights()
            throws IOException {
        var catalog = HoldingAgricultureCatalog.loadDefault();
        var winterfell = holding(1L, "winterfell_domain", "winterfell",
                HoldingType.CASTLE);
        var capacity = catalog.resolve(winterfell, lookup()).orElseThrow();
        assertEquals("the_north", capacity.sourceRegionId());
        assertEquals("MOD_ESTIMATE", capacity.provenance());
        assertEquals(1500L, capacity.farmableHectares());
        assertEquals(750L, capacity.cultivatedHectares());
        assertEquals(0L, capacity.yieldInputs(0L, 1000, 1000).agriculturalWorkers());
        assertEquals(0L, AgriculturalYieldModel.calculate(
                capacity.yieldInputs(0L, 1000, 1000)).harvestKg());
    }

    @Test
    void regionalAncestryWorksWithoutNamedMapFeatures() throws IOException {
        var catalog = HoldingAgricultureCatalog.loadDefault();
        var cerwyn = catalog.resolve(
                holding(2L, "castle_cerwyn_domain", "castle_cerwyn", HoldingType.CASTLE),
                lookup()).orElseThrow();
        var highgarden = catalog.resolve(
                holding(3L, "highgarden_domain", "highgarden", HoldingType.CASTLE),
                lookup()).orElseThrow();
        assertEquals(600L, cerwyn.farmableHectares());
        assertEquals(900L, highgarden.farmableHectares());
        assertTrue(highgarden.grainKgPerHectare() > cerwyn.grainKgPerHectare());
    }

    @Test
    void enrollmentFeedsExistingSettlementFoodEngine() throws IOException {
        var catalog = HoldingAgricultureCatalog.loadDefault();
        InventoryManager inventories = new InventoryManager(
                ResourceCatalog.benchmarkDefaults());
        var winterfell = holding(1L, "winterfell_domain", "winterfell", HoldingType.CASTLE);
        InventoryId farm = inventories.create(
                EconomicOwner.holding(winterfell.id()), SETTLEMENT).id();
        InventoryId home = inventories.create(
                EconomicOwner.npc(new NpcId(1L)), SETTLEMENT).id();

        AgriculturalDayEnrollment enrollment = AgriculturalDayEnrollment.fromHolding(
                winterfell, farm, SETTLEMENT, catalog, lookup(),
                10L, 1000, 1000
        );
        assertEquals(750L, enrollment.harvestInputs().cultivatedHectares());

        var result = MultiFarmSettlementFoodCycle.execute(
                inventories,
                new MultiFarmSettlementFoodCycle.Inputs(
                        SETTLEMENT, 119L, 120L,
                        java.util.List.of(new MultiFarmSettlementFoodCycle.Farm(
                                enrollment.holding(), enrollment.farmInventory(),
                                enrollment.harvestPeriodDays(), enrollment.harvestInputs(),
                                enrollment.protectedReserveKg())),
                        java.util.List.of(new SettlementFoodCycle.Household(
                                home, 1L, enrollment.gramsPerPersonPerDay()))
                )
        );
        assertTrue(result.harvestedKg() > 0L);
        assertTrue(result.consumedKg() > 0L);
        assertEquals(0L, result.shortageKg());
        assertEquals(result.harvestedKg(),
                result.distributedKg() + inventories.require(farm)
                        .quantity(AgriculturalFoodCycle.GRAIN));
    }

    @Test
    void unprofiledAndAggregateHoldingsAreNotGivenInventedFarms()
            throws IOException {
        var catalog = HoldingAgricultureCatalog.loadDefault();
        assertTrue(catalog.resolve(
                holding(1L, "north_domain", "the_north", HoldingType.REGION),
                lookup()).isEmpty());
        assertTrue(catalog.resolve(
                holding(2L, "braavos_domain", "braavos", HoldingType.CITY),
                lookup()).isEmpty());
    }

    @Test
    void invalidCatalogCannotSilentlyProduceGrain() {
        assertThrows(IllegalArgumentException.class, () ->
                HoldingAgricultureCatalog.read(new StringReader(
                        "{\"schemaVersion\":1,\"regionalProfiles\":[{\"locationId\":\"the_north\"," +
                                "\"provenance\":\"CANON\"}],\"holdingOverrides\":[]}")));
    }
}


package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class SettlementFoodScenarioAdapterTest {

    private static final HoldingId HOLDING =
            new HoldingId(1L);

    private static final class Fixture {

        final NpcRegistry npcs = new NpcRegistry();

        final SettlementManager settlements =
                new SettlementManager();

        final OrganizationManager organizations =
                new OrganizationManager(settlements);

        final NpcAffiliationManager affiliations =
                new NpcAffiliationManager(
                        npcs,
                        settlements,
                        organizations
                );

        final InventoryManager inventories =
                new InventoryManager(
                        ResourceCatalog.benchmarkDefaults()
                );

        final SettlementId settlement;

        final Organization household;

        final ResourceInventory farm;
        final ResourceInventory foodStore;

        final SettlementFoodScenarioAdapter adapter;

        Fixture() {
            settlement = settlements.create(
                    "Test Settlement",
                    SettlementType.CASTLE,
                    new SimulationPosition(
                            "minecraft:overworld",
                            0.0,
                            64.0,
                            0.0
                    )
            ).id();

            household = organizations.create(
                    "Test Household",
                    OrganizationType.HOUSEHOLD,
                    settlement
            );

            farm = inventories.create(
                    EconomicOwner.holding(HOLDING),
                    settlement
            );

            foodStore = inventories.create(
                    EconomicOwner.organization(
                            household.id()
                    ),
                    settlement
            );

            adapter = new SettlementFoodScenarioAdapter(
                    npcs,
                    affiliations,
                    organizations,
                    settlements,
                    HOLDING::equals
            );
        }

        NpcState addResident(boolean assigned) {
            NpcState npc = npcs.create(
                    "Resident",
                    "Test",
                    NpcSex.FEMALE,
                    280,
                    new SimulationPosition(
                            "minecraft:overworld",
                            0.0,
                            64.0,
                            0.0
                    )
            );

            affiliations.setResidence(
                    npc.id(),
                    settlement
            );

            if (assigned) {
                affiliations.setHousehold(
                        npc.id(),
                        household.id()
                );
            }

            return npc;
        }

        SettlementFoodScenarioAdapter.Resolved resolve() {
            return adapter.resolve(
                    inventories,
                    HOLDING,
                    farm.id(),
                    settlement,
                    new AgriculturalInterval(
                            29L, 30L, 30L
                    ),
                    new AgriculturalYieldModel.Inputs(
                            1L, 1L, 100L, 100L,
                            1000, 1000, 1000
                    ),
                    0L,
                    1000L
            );
        }
    }

    @Test
    void existingHouseholdMembersDeterminePopulation() {
        Fixture f = new Fixture();

        f.addResident(true);
        f.addResident(true);

        var result = f.resolve();

        assertEquals(2L, result.representedResidents());
        assertEquals(0L, result.unassignedResidents());

        assertEquals(
                1,
                result.cycleInputs().households().size()
        );

        assertEquals(
                2L,
                result.cycleInputs().households()
                        .get(0).population()
        );

        assertEquals(
                f.foodStore.id(),
                result.cycleInputs().households()
                        .get(0).inventory()
        );
    }

    @Test
    void missingHouseholdInventoryIsRejected() {
        Fixture f = new Fixture();

        f.addResident(true);

        Organization second = f.organizations.create(
                "Unfunded Household",
                OrganizationType.HOUSEHOLD,
                f.settlement
        );

        NpcState resident = f.addResident(false);

        f.affiliations.setHousehold(
                resident.id(),
                second.id()
        );

        assertThrows(
                IllegalStateException.class,
                f::resolve
        );

        assertEquals(
                0L,
                f.inventories.transactionCount()
        );
    }

    @Test
    void unassignedResidentsAreReported() {
        Fixture f = new Fixture();

        f.addResident(true);
        f.addResident(false);

        var result = f.resolve();

        assertEquals(1L, result.representedResidents());
        assertEquals(1L, result.unassignedResidents());
    }

    @Test
    void deadResidentsDoNotConsumeFood() {
        Fixture f = new Fixture();

        f.addResident(true);

        NpcState deceased = f.addResident(true);
        deceased.markDead();

        var result = f.resolve();

        assertEquals(1L, result.representedResidents());

        assertEquals(
                1L,
                result.cycleInputs().households()
                        .get(0).population()
        );
    }

    @Test
    void emptyHouseholdPopulationIsNotInvented() {
        Fixture f = new Fixture();

        assertThrows(
                IllegalStateException.class,
                f::resolve
        );
    }

    @Test
    void wrongFarmOwnerIsRejected() {
        Fixture f = new Fixture();

        f.addResident(true);

        ResourceInventory wrong =
                f.inventories.create(
                        EconomicOwner.organization(
                                f.household.id()
                        ),
                        f.settlement
                );

        assertThrows(
                IllegalStateException.class,
                () -> f.adapter.resolve(
                        f.inventories,
                        HOLDING,
                        wrong.id(),
                        f.settlement,
                        new AgriculturalInterval(
                                29L, 30L, 30L
                        ),
                        new AgriculturalYieldModel.Inputs(
                                1L, 1L, 100L, 100L,
                                1000, 1000, 1000
                        ),
                        0L,
                        1000L
                )
        );
    }

    @Test
    void planningDoesNotChangeInventoryBalances() {
        Fixture f = new Fixture();

        f.addResident(true);

        f.resolve();

        assertEquals(
                0L,
                f.inventories.totalQuantity(
                        new ResourceId("grain")
                )
        );

        assertEquals(
                0L,
                f.inventories.transactionCount()
        );
    }
}

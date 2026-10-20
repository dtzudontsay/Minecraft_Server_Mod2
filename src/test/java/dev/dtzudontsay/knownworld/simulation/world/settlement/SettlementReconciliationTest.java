
package dev.dtzudontsay.knownworld.simulation.world.settlement;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SettlementReconciliationTest {

    private static final SimulationPosition LEGACY =
            SimulationPosition.overworld(3.5, 106, 3.5);

    private static final SimulationPosition CORRECTED =
            SimulationPosition.overworld(
                    -4050155.5,
                    106,
                    -1976441.5
            );

    @Test
    void migratesLegacyCenterWithoutChangingIdentity() {

        SettlementManager manager = new SettlementManager();

        Settlement winterfell = manager.create(
                "Winterfell",
                SettlementType.CASTLE,
                LEGACY
        );

        SettlementId originalId = winterfell.id();

        assertTrue(manager.recenterIfLegacy(
                originalId,
                LEGACY,
                CORRECTED
        ));

        Settlement result =
                manager.find(originalId).orElseThrow();

        assertEquals(originalId, result.id());
        assertEquals("Winterfell", result.name());
        assertEquals(SettlementType.CASTLE, result.type());
        assertEquals(CORRECTED, result.center());
        assertEquals(1, manager.size());
    }

    @Test
    void repeatedMigrationDoesNothing() {

        SettlementManager manager = new SettlementManager();

        Settlement winterfell = manager.create(
                "Winterfell",
                SettlementType.CASTLE,
                LEGACY
        );

        assertTrue(manager.recenterIfLegacy(
                winterfell.id(),
                LEGACY,
                CORRECTED
        ));

        assertFalse(manager.recenterIfLegacy(
                winterfell.id(),
                LEGACY,
                CORRECTED
        ));

        assertEquals(
                CORRECTED,
                manager.find(
                        winterfell.id()
                ).orElseThrow().center()
        );
    }

    @Test
    void refusesToMoveAlreadyLocatedSettlement() {

        SettlementManager manager = new SettlementManager();

        SimulationPosition established =
                SimulationPosition.overworld(
                        -4050100,
                        112,
                        -1976380
                );

        Settlement winterfell = manager.create(
                "Winterfell",
                SettlementType.CASTLE,
                established
        );

        assertFalse(manager.recenterIfLegacy(
                winterfell.id(),
                LEGACY,
                CORRECTED
        ));

        assertEquals(
                established,
                manager.find(
                        winterfell.id()
                ).orElseThrow().center()
        );
    }

    @Test
    void doesNotMoveOtherSettlements() {

        SettlementManager manager = new SettlementManager();

        Settlement winterfell = manager.create(
                "Winterfell",
                SettlementType.CASTLE,
                LEGACY
        );

        SimulationPosition anotherPosition =
                SimulationPosition.overworld(100, 85, 200);

        Settlement village = manager.create(
                "Other Village",
                SettlementType.VILLAGE,
                anotherPosition
        );

        assertTrue(manager.recenterIfLegacy(
                winterfell.id(),
                LEGACY,
                CORRECTED
        ));

        assertEquals(
                anotherPosition,
                manager.find(
                        village.id()
                ).orElseThrow().center()
        );

        assertEquals(2, manager.size());
    }

    @Test
    void legacyVerticalDifferenceIsTolerated() {

        SettlementManager manager = new SettlementManager();

        Settlement winterfell = manager.create(
                "Winterfell",
                SettlementType.CASTLE,
                SimulationPosition.overworld(
                        3.5,
                        120,
                        3.5
                )
        );

        assertTrue(manager.recenterIfLegacy(
                winterfell.id(),
                LEGACY,
                CORRECTED
        ));
    }

    @Test
    void refusesCrossDimensionMigration() {

        SettlementManager manager = new SettlementManager();

        Settlement winterfell = manager.create(
                "Winterfell",
                SettlementType.CASTLE,
                LEGACY
        );

        SimulationPosition nether =
                new SimulationPosition(
                        "minecraft:the_nether",
                        0,
                        70,
                        0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.recenterIfLegacy(
                        winterfell.id(),
                        LEGACY,
                        nether
                )
        );
    }

    @Test
    void unknownSettlementIsNotSilentlyCreated() {

        SettlementManager manager = new SettlementManager();

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.recenterIfLegacy(
                        new SettlementId(9999),
                        LEGACY,
                        CORRECTED
                )
        );

        assertEquals(0, manager.size());
    }
}

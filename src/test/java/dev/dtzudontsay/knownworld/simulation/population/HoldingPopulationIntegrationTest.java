
package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.economy.HoldingAgricultureCatalog;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.social.holding.*;
import dev.dtzudontsay.knownworld.simulation.world.settlement.*;
import dev.dtzudontsay.knownworld.world.reference.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public final class HoldingPopulationIntegrationTest {

    @TempDir
    Path directory;

    private static final HoldingId CASTLE =
            new HoldingId(1L);

    private static final HoldingId ESTATE =
            new HoldingId(2L);

    private static final HoldingId REGION =
            new HoldingId(3L);

    private static LandedHolding holding(
            HoldingId id,
            String name,
            HoldingType type
    ) {
        return new LandedHolding(
                id,
                name,
                name,
                type,
                "castle_cerwyn",
                null,
                null,
                HoldingStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                0,
                0,
                0
        );
    }

    private static final class Fixture {

        final SettlementManager settlements =
                new SettlementManager();

        final Map<HoldingId, LandedHolding> holdings =
                Map.of(
                        CASTLE,
                        holding(
                                CASTLE,
                                "castle_cerwyn_domain",
                                HoldingType.CASTLE
                        ),
                        ESTATE,
                        holding(
                                ESTATE,
                                "cerwyn_estate",
                                HoldingType.ESTATE
                        ),
                        REGION,
                        holding(
                                REGION,
                                "the_north_domain",
                                HoldingType.REGION
                        )
                );

        final PopulationCohortManager cohorts =
                new PopulationCohortManager(
                        settlements,
                        id -> Optional.ofNullable(
                                holdings.get(id)
                        )
                );

        PopulationCohortManager.HoldingKey farmers(
                HoldingId id
        ) {
            return new PopulationCohortManager.HoldingKey(
                    id,
                    PopulationCohortManager.AgeBand.ADULT,
                    NpcSex.FEMALE,
                    "northmen",
                    "old_gods",
                    "smallfolk",
                    "farmer"
            );
        }

        PopulationCohortManager.HoldingKey children(
                HoldingId id
        ) {
            return new PopulationCohortManager.HoldingKey(
                    id,
                    PopulationCohortManager.AgeBand.CHILD,
                    NpcSex.MALE,
                    "northmen",
                    "old_gods",
                    "smallfolk",
                    "unemployed"
            );
        }

        void survey() {
            cohorts.markHoldingSurveyed(
                    CASTLE,
                    PopulationCohortManager.Basis.MOD_ESTIMATE
            );

            cohorts.markHoldingSurveyed(
                    ESTATE,
                    PopulationCohortManager.Basis.MOD_ESTIMATE
            );
        }

        WorldLocationDefinition reference() {
            return new WorldLocationDefinition(
                    "castle_cerwyn",
                    "Castle Cerwyn",
                    WorldLocationKind.SETTLEMENT,
                    "the_north",
                    null,
                    null,
                    ReferenceProvenance.CANON,
                    "Unplaced reference",
                    false
            );
        }
    }

    @Test
    void unplacedHoldingPopulationCanSupportExistingAgriculturalYield()
            throws IOException {

        Fixture f = new Fixture();

        assertEquals(
                0,
                f.settlements.size()
        );

        f.survey();

        f.cohorts.changeHolding(
                f.farmers(CASTLE),
                120
        );

        f.cohorts.changeHolding(
                f.children(CASTLE),
                80
        );

        var projection = HoldingPopulationProjection.project(
                f.holdings.get(CASTLE),
                f.cohorts,
                HoldingAgricultureCatalog.loadDefault(),
                id -> "castle_cerwyn".equals(id)
                        ? Optional.of(f.reference())
                        : Optional.empty(),
                1000,
                1000
        );

        assertEquals(
                200,
                projection.anonymousResidents()
        );

        assertEquals(
                120,
                projection.anonymousAdultFarmers()
        );

        assertTrue(
                projection.potentialHarvestKg() > 0
        );

        assertEquals(
                "MOD_ESTIMATE",
                projection.agriculturalProvenance()
        );
    }

    @Test
    void holdingMigrationAndRelocationToARealSettlementConservePeople() {

        Fixture f = new Fixture();

        f.survey();

        f.cohorts.changeHolding(
                f.farmers(CASTLE),
                100
        );

        f.cohorts.migrateHolding(
                f.farmers(CASTLE),
                ESTATE,
                30
        );

        assertEquals(
                70,
                f.cohorts.holdingCount(
                        f.farmers(CASTLE)
                )
        );

        assertEquals(
                30,
                f.cohorts.holdingCount(
                        f.farmers(ESTATE)
                )
        );

        SettlementId settlement = f.settlements.create(
                "Castle Cerwyn",
                SettlementType.CASTLE,
                new SimulationPosition(
                        "minecraft:overworld",
                        3.5,
                        100,
                        3.5
                )
        ).id();

        f.cohorts.markSurveyed(
                settlement,
                PopulationCohortManager.Basis.MOD_ESTIMATE
        );

        f.cohorts.moveToSettlement(
                f.farmers(CASTLE),
                settlement,
                50
        );

        assertEquals(
                20,
                f.cohorts.holdingCount(
                        f.farmers(CASTLE)
                )
        );

        assertEquals(
                50,
                f.cohorts.count(
                        f.farmers(CASTLE).at(settlement)
                )
        );

        assertEquals(
                100,
                f.cohorts.holdingCount(
                        f.farmers(CASTLE)
                )
                        + f.cohorts.holdingCount(
                        f.farmers(ESTATE)
                )
                        + f.cohorts.count(
                        f.farmers(CASTLE).at(settlement)
                )
        );
    }

    @Test
    void rejectsAggregateUnknownAndUnsurveyedHoldings() {

        Fixture f = new Fixture();

        assertThrows(
                IllegalArgumentException.class,
                () -> f.cohorts.markHoldingSurveyed(
                        REGION,
                        PopulationCohortManager.Basis.MOD_ESTIMATE
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> f.cohorts.markHoldingSurveyed(
                        new HoldingId(999),
                        PopulationCohortManager.Basis.MOD_ESTIMATE
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> f.cohorts.changeHolding(
                        f.farmers(CASTLE),
                        10
                )
        );

        f.survey();

        assertThrows(
                IllegalArgumentException.class,
                () -> f.cohorts.migrateHolding(
                        f.farmers(CASTLE),
                        ESTATE,
                        -1
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> f.cohorts.changeHolding(
                        f.farmers(CASTLE),
                        -1
                )
        );
    }

    @Test
    void v3CheckpointRoundTripIncludesBothLocationKinds()
            throws IOException {

        Fixture f = new Fixture();

        f.survey();

        f.cohorts.changeHolding(
                f.farmers(CASTLE),
                90
        );

        SettlementId town = f.settlements.create(
                "Town",
                SettlementType.TOWN,
                new SimulationPosition(
                        "minecraft:overworld",
                        0,
                        64,
                        0
                )
        ).id();

        f.cohorts.markSurveyed(
                town,
                PopulationCohortManager.Basis.USER_AUTHORED
        );

        f.cohorts.change(
                f.farmers(CASTLE).at(town),
                12
        );

        PopulationCohortPersistence store =
                new PopulationCohortPersistence(
                        directory
                );

        /*
         * This fixture includes TWO holding surveys.
         * The v3 reader must accept both of them.
         */
        store.save(
                108770L,
                f.cohorts
        );

        assertTrue(
                Files.readString(
                        store.file()
                ).startsWith(
                        "KNOWNWORLD_POPULATION_COHORTS\t3\n"
                )
        );

        PopulationCohortManager loaded =
                new PopulationCohortManager(
                        f.settlements,
                        id -> Optional.ofNullable(
                                f.holdings.get(id)
                        )
                );

        assertTrue(
                store.loadInto(
                        108770L,
                        loaded
                )
        );

        assertEquals(
                f.cohorts.extendedSnapshot(),
                loaded.extendedSnapshot()
        );

        assertThrows(
                IOException.class,
                () -> store.loadInto(
                        108771L,
                        loaded
                )
        );

        Files.writeString(
                store.file(),
                "damaged checkpoint"
        );

        assertThrows(
                IOException.class,
                () -> store.loadInto(
                        108770L,
                        loaded
                )
        );

        assertEquals(
                90L,
                loaded.holdingCount(
                        f.farmers(CASTLE)
                )
        );
    }

    @Test
    void readsOriginalV1SettlementCheckpointAndUpgradesToV3()
            throws Exception {

        Fixture f = new Fixture();

        SettlementId town = f.settlements.create(
                "Town",
                SettlementType.TOWN,
                new SimulationPosition(
                        "minecraft:overworld",
                        0,
                        64,
                        0
                )
        ).id();

        /*
         * Original Batch 22.17 checkpoint.
         *
         * V1 has no holding cohorts and no annual
         * demographic progress marker.
         */
        String body =
                "KNOWNWORLD_POPULATION_COHORTS\t1\n"
                        + "DAY\t77\n"
                        + "SURVEY\t"
                        + town.value()
                        + "\tMOD_ESTIMATE\n"
                        + "COHORT\t"
                        + town.value()
                        + "\tADULT\tMALE"
                        + "\tnorthmen\told_gods"
                        + "\tsmallfolk\tfarmer\t42\n";

        String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(
                                body.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
        );

        PopulationCohortPersistence store =
                new PopulationCohortPersistence(
                        directory
                );

        Files.writeString(
                store.file(),
                body + "SHA256\t" + hash + "\n"
        );

        assertTrue(
                store.loadInto(
                        77,
                        f.cohorts
                )
        );

        assertEquals(
                42L,
                f.cohorts.count(
                        new PopulationCohortManager.Key(
                                town,
                                PopulationCohortManager.AgeBand.ADULT,
                                NpcSex.MALE,
                                "northmen",
                                "old_gods",
                                "smallfolk",
                                "farmer"
                        )
                )
        );

        assertTrue(
                f.cohorts.extendedSnapshot()
                        .holdingCohorts()
                        .isEmpty()
        );

        /*
         * Old checkpoints have not yet performed
         * any annual demographic progression.
         */
        assertTrue(
                f.cohorts.lastDemographicYear().isEmpty()
        );

        store.save(
                77,
                f.cohorts
        );

        /*
         * FIX:
         *
         * Older test expected version 2.
         * Batch 22.19 now writes version 3,
         * while retaining v1/v2 read support.
         */
        assertTrue(
                Files.readString(
                        store.file()
                ).startsWith(
                        "KNOWNWORLD_POPULATION_COHORTS\t3\n"
                )
        );

        /*
         * Ensure migration preserved the inhabitants.
         */
        assertEquals(
                42L,
                f.cohorts.count(
                        new PopulationCohortManager.Key(
                                town,
                                PopulationCohortManager.AgeBand.ADULT,
                                NpcSex.MALE,
                                "northmen",
                                "old_gods",
                                "smallfolk",
                                "farmer"
                        )
                )
        );
    }
}

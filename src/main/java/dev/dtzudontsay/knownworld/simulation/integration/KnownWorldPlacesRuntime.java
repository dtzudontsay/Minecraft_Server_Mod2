
package dev.dtzudontsay.knownworld.simulation.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.Settlement;
import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * B2.1: Canonical holding/location/settlement reconciliation.
 *
 * Authorities:
 * - WorldReferenceCatalog: named geographic identities
 *   and map placements.
 * - LandedHoldingManager: political landed holdings.
 * - SettlementManager: persistent simulated settlements.
 * - Scenario settlement JSON: authored starting centers.
 *
 * This runtime does not introduce a parallel mutable
 * location catalog or invent placements for unmapped sites.
 *
 * The only automatic save migration is the explicitly
 * recognized old Winterfell (3.5, 3.5) scenario placeholder.
 */
public final class KnownWorldPlacesRuntime
        implements ModInitializer {

    private static final String SETTLEMENT_RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/settlements.json";

    private static final String WINTERFELL_ID =
            "winterfell";

    private static final String WINTERFELL_HOLDING =
            "winterfell_domain";

    private static final SimulationPosition LEGACY_WINTERFELL =
            SimulationPosition.overworld(
                    3.5,
                    106.0,
                    3.5
            );

    /*
     * Sanity range around the independently verified
     * Winterfell V3 generation origin.
     *
     * This is a safety check, not a second placement
     * database. The actual target comes from settlements.json.
     */
    private static final double V3_ORIGIN_X = -4_050_173;
    private static final double V3_ORIGIN_Z = -1_976_496;
    private static final double MAX_EXPECTED_OFFSET = 256.0;

    private static MinecraftServer activeServer;
    private static boolean winterfellMigrated;

    private enum Placement {
        MAPPED,
        NAMED_ONLY,
        UNKNOWN_REFERENCE
    }

    private record Totals(
            int total,
            int nonSiteDomains,
            int possibleSites,
            int mappedSites,
            int unplacedSites,
            int unknownReferences,
            int currentSettlements
    ) { }

    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                KnownWorldPlacesRuntime::start
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> {

                    if (activeServer == server) {

                        activeServer = null;
                        winterfellMigrated = false;
                    }
                }
        );

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("kwplaces")

                                        .then(
                                                Commands.literal("status")
                                                        .executes(context ->
                                                                status(
                                                                        context.getSource()
                                                                )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("winterfell")
                                                        .executes(context ->
                                                                winterfell(
                                                                        context.getSource()
                                                                )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("audit")
                                                        .executes(context ->
                                                                audit(
                                                                        context.getSource()
                                                                )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("holding")
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(context ->
                                                                                holding(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "id"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );

        KnownWorld.LOGGER.info(
                "Known World B2 holding and location audit registered."
        );
    }

    private static void start(MinecraftServer server) {

        activeServer = null;
        winterfellMigrated = false;

        ServerLevel overworld =
                server.getLevel(Level.OVERWORLD);

        if (overworld == null
                || !(overworld.getChunkSource()
                .getGenerator()
                instanceof KnownWorldChunkGenerator)) {

            return;
        }

        NpcSimulation simulation =
                NpcSimulation.getNullable();

        SocietyStructureRuntime society =
                SocietyStructureRuntime.getNullable();

        if (simulation == null
                || simulation.server() != server
                || society == null) {

            KnownWorld.LOGGER.warn(
                    "B2 geography integration cannot start: "
                            + "NPC or society runtime unavailable."
            );

            return;
        }

        activeServer = server;

        /*
         * Read the same scenario authoring resource used
         * to create Winterfell on a fresh save.
         *
         * Existing saves retain SettlementId and other
         * simulation relationships.
         */
        SimulationPosition corrected =
                authoredWinterfellPosition();

        var settlementId =
                simulation.authoredIds()
                        .findSettlement(WINTERFELL_ID);

        if (settlementId.isPresent()) {

            winterfellMigrated =
                    simulation.settlements()
                            .recenterIfLegacy(
                                    settlementId.get(),
                                    LEGACY_WINTERFELL,
                                    corrected
                            );

            if (winterfellMigrated) {

                simulation.save();

                KnownWorld.LOGGER.info(
                        "B2 migrated Winterfell legacy settlement "
                                + "center to X={} Y={} Z={}; "
                                + "existing NPC positions were not moved.",
                        corrected.x(),
                        corrected.y(),
                        corrected.z()
                );
            }
        }

        Totals totals = count(
                society.holdings(),
                WorldReferenceCatalog.get(),
                simulation
        );

        KnownWorld.LOGGER.info(
                "B2 geography: {} holdings, {} possible physical "
                        + "sites, {} mapped sites, {} named-only sites, "
                        + "{} domain records, {} simulated settlements.",
                totals.total(),
                totals.possibleSites(),
                totals.mappedSites(),
                totals.unplacedSites(),
                totals.nonSiteDomains(),
                totals.currentSettlements()
        );
    }

    /**
     * Read-only geographic classification.
     * A political holding is NOT automatically a town.
     */
    public static boolean mayHavePhysicalSeat(
            HoldingType type
    ) {

        return switch (type) {

            case REGION, TERRITORY -> false;

            case CASTLE, FORTRESS, CITY, TOWN, VILLAGE,
                 PORT, ESTATE, OTHER -> true;
        };
    }

    private static Placement placement(
            LandedHolding holding,
            WorldReferenceCatalog catalog
    ) {

        if (catalog.location(
                holding.worldLocationId()
        ).isEmpty()) {

            return Placement.UNKNOWN_REFERENCE;
        }

        if (catalog.resolvedMapLocation(
                holding.worldLocationId()
        ).isPresent()) {

            return Placement.MAPPED;
        }

        return Placement.NAMED_ONLY;
    }

    private static Totals count(
            LandedHoldingManager holdings,
            WorldReferenceCatalog catalog,
            NpcSimulation simulation
    ) {

        int total = 0;
        int domains = 0;
        int sites = 0;
        int mapped = 0;
        int unplaced = 0;
        int unknown = 0;

        for (LandedHolding holding : holdings.all()) {

            total++;

            Placement resolved = placement(
                    holding,
                    catalog
            );

            if (resolved == Placement.UNKNOWN_REFERENCE) {
                unknown++;
            }

            if (!mayHavePhysicalSeat(
                    holding.type()
            )) {

                domains++;
                continue;
            }

            sites++;

            if (resolved == Placement.MAPPED) {
                mapped++;
            } else {
                unplaced++;
            }
        }

        return new Totals(
                total,
                domains,
                sites,
                mapped,
                unplaced,
                unknown,
                simulation.settlements().size()
        );
    }

    private static int status(
            CommandSourceStack source
    ) {

        var runtimes = requireRuntimes(source);

        if (runtimes == null) {
            return 0;
        }

        Totals totals = count(
                runtimes.society().holdings(),
                WorldReferenceCatalog.get(),
                runtimes.simulation()
        );

        say(
                source,
                String.format(
                        Locale.ROOT,
                        "[Known World / Places] "
                                + "holdings=%d | possible seats=%d "
                                + "| mapped seats=%d | unplaced seats=%d "
                                + "| domain records=%d | missing references=%d "
                                + "| simulated settlements=%d",
                        totals.total(),
                        totals.possibleSites(),
                        totals.mappedSites(),
                        totals.unplacedSites(),
                        totals.nonSiteDomains(),
                        totals.unknownReferences(),
                        totals.currentSettlements()
                )
        );

        return 1;
    }

    private static int winterfell(
            CommandSourceStack source
    ) {

        Runtimes runtimes = requireRuntimes(source);

        if (runtimes == null) {
            return 0;
        }

        Optional<dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId>
                id = runtimes.simulation()
                .authoredIds()
                .findSettlement(WINTERFELL_ID);

        if (id.isEmpty()) {

            fail(
                    source,
                    "No authored Winterfell settlement ID is registered."
            );

            return 0;
        }

        Optional<Settlement> settlement =
                runtimes.simulation()
                        .settlements()
                        .find(id.get());

        if (settlement.isEmpty()) {

            fail(
                    source,
                    "Winterfell settlement ID has no associated record."
            );

            return 0;
        }

        SimulationPosition current =
                settlement.get().center();

        SimulationPosition target =
                authoredWinterfellPosition();

        double horizontalGap = current.horizontalDistance(target);

        say(
                source,
                String.format(
                        Locale.ROOT,
                        "[Known World / Winterfell] "
                                + "settlement ID=%s | "
                                + "center=(%.1f, %.1f, %.1f) | "
                                + "scenario target=(%.1f, %.1f, %.1f) | "
                                + "horizontal difference=%.1fm | "
                                + "legacy migrated this start=%s",
                        id.get(),
                        current.x(),
                        current.y(),
                        current.z(),
                        target.x(),
                        target.y(),
                        target.z(),
                        horizontalGap,
                        winterfellMigrated
                )
        );

        var holding = runtimes.society()
                .holdings()
                .findAuthored(WINTERFELL_HOLDING);

        if (holding.isPresent()) {

            say(
                    source,
                    "Holding link: "
                            + WINTERFELL_HOLDING
                            + " -> "
                            + holding.get().worldLocationId()
                            + " | type="
                            + holding.get().type()
            );
        }

        return 1;
    }

    private static int holding(
            CommandSourceStack source,
            String holdingId
    ) {

        Runtimes runtimes = requireRuntimes(source);

        if (runtimes == null) {
            return 0;
        }

        LandedHolding holding = runtimes.society()
                .holdings()
                .findAuthored(
                        holdingId.toLowerCase(Locale.ROOT)
                )
                .orElse(null);

        if (holding == null) {

            fail(
                    source,
                    "Unknown authored holding: " + holdingId
            );

            return 0;
        }

        WorldReferenceCatalog catalog =
                WorldReferenceCatalog.get();

        WorldLocationDefinition location =
                catalog.location(
                        holding.worldLocationId()
                ).orElse(null);

        Placement status = placement(
                holding,
                catalog
        );

        say(
                source,
                "[Known World / Holding] "
                        + holding.name()
                        + " | authored="
                        + holding.authoredId()
                        + " | runtime ID="
                        + holding.id()
                        + " | type="
                        + holding.type()
                        + " | status="
                        + holding.status()
        );

        say(
                source,
                "Geography: "
                        + holding.worldLocationId()
                        + " | location kind="
                        + (location == null
                        ? "UNRESOLVED"
                        : location.kind())
                        + " | source provenance="
                        + (location == null
                        ? "UNKNOWN"
                        : location.provenance())
                        + " | placement="
                        + status
                        + " | potential physical seat="
                        + mayHavePhysicalSeat(holding.type())
        );

        catalog.resolvedMapLocation(
                holding.worldLocationId()
        ).ifPresent(feature ->

                say(
                        source,
                        String.format(
                                Locale.ROOT,
                                "Resolved map anchor: X=%.1f Z=%.1f"
                                        + " | feature=%s",
                                feature.minecraftX(),
                                feature.minecraftZ(),
                                feature.featureId()
                        )
                )
        );

        say(
                source,
                "Political links: parent="
                        + holding.parentHoldingId()
                        + " | holder NPC="
                        + holding.holderNpcId()
                        + " | owner dynasty="
                        + holding.ownerDynastyId()
                        + " | government="
                        + holding.governmentOrganizationId()
                        + " | title="
                        + holding.linkedTitleId()
        );

        runtimes.simulation()
                .authoredIds()
                .findSettlement(holding.worldLocationId())
                .flatMap(
                        runtimes.simulation().settlements()::find
                )
                .ifPresent(settlement ->
                        say(
                                source,
                                String.format(
                                        Locale.ROOT,
                                        "Simulated settlement: %s ID=%s "
                                                + "at X=%.1f Z=%.1f",
                                        settlement.name(),
                                        settlement.id(),
                                        settlement.center().x(),
                                        settlement.center().z()
                                )
                        )
                );

        return 1;
    }

    private static int audit(
            CommandSourceStack source
    ) {

        Runtimes runtimes = requireRuntimes(source);

        if (runtimes == null) {
            return 0;
        }

        Totals totals = count(
                runtimes.society().holdings(),
                WorldReferenceCatalog.get(),
                runtimes.simulation()
        );

        say(
                source,
                "[Known World / Places Audit] "
                        + totals.possibleSites()
                        + " potential physical seats; "
                        + totals.unplacedSites()
                        + " do not yet have a resolved map anchor."
        );

        List<LandedHolding> unplaced =
                new ArrayList<>();

        for (LandedHolding holding :
                runtimes.society().holdings().all()) {

            if (mayHavePhysicalSeat(holding.type())
                    && placement(
                    holding,
                    WorldReferenceCatalog.get()
            ) != Placement.MAPPED) {

                unplaced.add(holding);
            }
        }

        unplaced.sort(
                Comparator.comparing(
                        holding -> holding.authoredId() == null
                                ? holding.name()
                                : holding.authoredId()
                )
        );

        int shown = Math.min(10, unplaced.size());

        for (int i = 0; i < shown; i++) {

            LandedHolding holding = unplaced.get(i);

            say(
                    source,
                    "- "
                            + holding.authoredId()
                            + " -> "
                            + holding.worldLocationId()
                            + " ["
                            + placement(
                            holding,
                            WorldReferenceCatalog.get()
                    )
                            + "]"
            );
        }

        if (unplaced.size() > shown) {

            say(
                    source,
                    "... and "
                            + (unplaced.size() - shown)
                            + " more. No missing locations were invented."
            );
        }

        return 1;
    }

    private record Runtimes(
            NpcSimulation simulation,
            SocietyStructureRuntime society
    ) { }

    private static Runtimes requireRuntimes(
            CommandSourceStack source
    ) {

        if (activeServer == null
                || source.getServer() != activeServer) {

            fail(
                    source,
                    "Place integration is not active in this world."
            );

            return null;
        }

        NpcSimulation simulation =
                NpcSimulation.getNullable();

        SocietyStructureRuntime society =
                SocietyStructureRuntime.getNullable();

        if (simulation == null || society == null) {

            fail(
                    source,
                    "Simulation or society records are unavailable."
            );

            return null;
        }

        return new Runtimes(
                simulation,
                society
        );
    }

    /**
     * Load the scenario's authoritative settlement
     * position rather than maintaining a second copy.
     */
    private static SimulationPosition authoredWinterfellPosition() {

        try (InputStream stream =
                     KnownWorldPlacesRuntime.class
                             .getClassLoader()
                             .getResourceAsStream(
                                     SETTLEMENT_RESOURCE
                             )) {

            if (stream == null) {

                throw new IllegalStateException(
                        "Missing scenario settlement resource: "
                                + SETTLEMENT_RESOURCE
                );
            }

            JsonArray entries = JsonParser.parseReader(
                    new InputStreamReader(
                            stream,
                            StandardCharsets.UTF_8
                    )
            ).getAsJsonArray();

            for (JsonElement element : entries) {

                JsonObject entry =
                        element.getAsJsonObject();

                if (!WINTERFELL_ID.equals(
                        entry.get("id").getAsString()
                )) {

                    continue;
                }

                JsonObject position =
                        entry.getAsJsonObject("position");

                SimulationPosition result =
                        new SimulationPosition(
                                position.get("dimension").getAsString(),
                                position.get("x").getAsDouble(),
                                position.get("y").getAsDouble(),
                                position.get("z").getAsDouble()
                        );

                /*
                 * Fail early if somebody accidentally restores
                 * the old placeholder in the resource or
                 * supplies coordinates outside the V3 site.
                 */
                if (!SimulationPosition.OVERWORLD.equals(
                        result.dimension()
                )
                        || Math.abs(
                        result.x() - V3_ORIGIN_X
                ) > MAX_EXPECTED_OFFSET
                        || Math.abs(
                        result.z() - V3_ORIGIN_Z
                ) > MAX_EXPECTED_OFFSET) {

                    throw new IllegalStateException(
                            "Winterfell scenario coordinates do "
                                    + "not agree with the existing V3 location."
                    );
                }

                return result;
            }

            throw new IllegalStateException(
                    "No winterfell settlement in "
                            + SETTLEMENT_RESOURCE
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Cannot read scenario settlement coordinates",
                    exception
            );
        }
    }

    private static void say(
            CommandSourceStack source,
            String text
    ) {

        source.sendSuccess(
                () -> Component.literal(text),
                false
        );
    }

    private static void fail(
            CommandSourceStack source,
            String text
    ) {

        source.sendFailure(
                Component.literal(text)
        );
    }
}

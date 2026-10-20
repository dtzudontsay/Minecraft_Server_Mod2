
package dev.dtzudontsay.knownworld.simulation.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.KnownWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * B2.2 read-only candidate geographic evidence. NOT a building-site catalog.
 *
 * Holding identity/ownership remain with LandedHoldingManager;
 * mapped named locations remain with WorldReferenceCatalog.
 * This index only preserves map-observation provenance for later B3 review.
 */
public final class KnownWorldHoldingGeoCandidates implements ModInitializer {

    private static final String RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/holding_geo_candidates.json";

    public record Marker(String id, long candidateX, long candidateZ,
                         String identityEvidence, String reviewStatus,
                         String landMaskStatus) { }

    public record Holding(String authoredHoldingId, String worldLocationId,
                          String holdingType, boolean domain,
                          String status, List<Marker> candidates) {
        public Holding {
            candidates = List.copyOf(candidates);
        }
    }

    private static Map<String, Holding> indexed;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("kwgeolinks")
                        .then(Commands.literal("status").executes(ctx -> {
                            Map<String, Holding> all = all();
                            long withCandidate = all.values().stream()
                                    .filter(h -> !h.candidates().isEmpty()).count();
                            long domains = all.values().stream()
                                    .filter(Holding::domain).count();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[Known World / Geo evidence] holdings=" + all.size()
                                            + " | with candidate map markers=" + withCandidate
                                            + " | domains=" + domains
                                            + " | approved building sites=0"), false);
                            return 1;
                        }))
                        .then(Commands.literal("holding")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            Holding h = all().get(id);
                                            if (h == null) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "Unknown authored holding ID: " + id));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "[Geo evidence] " + h.authoredHoldingId()
                                                            + " -> " + h.worldLocationId()
                                                            + " | " + h.holdingType()
                                                            + " | " + h.status()
                                                            + " | markers=" + h.candidates().size()), false);
                                            for (Marker marker : h.candidates()) {
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        "  " + marker.id() + " X=" + marker.candidateX()
                                                                + " Z=" + marker.candidateZ()
                                                                + " | " + marker.identityEvidence()
                                                                + " | land=" + marker.landMaskStatus()
                                                                + " | NOT BUILD-APPROVED"), false);
                                            }
                                            return 1;
                                        }))));
        KnownWorld.LOGGER.info("B2.2 geographic-evidence commands registered");
    }

    public static synchronized Map<String, Holding> all() {
        if (indexed == null) indexed = load();
        return indexed;
    }

    public static Optional<Holding> find(String authoredHoldingId) {
        return Optional.ofNullable(all().get(authoredHoldingId));
    }

    private static Map<String, Holding> load() {
        try (InputStream stream = KnownWorldHoldingGeoCandidates.class
                .getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing B2.2 generated evidence: " + RESOURCE
                        + ". Run: py tools/geodata/build_b2_holding_links.py");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != 1
                    || !"NONE".equals(root.get("physicalPlacementApproval").getAsString())) {
                throw new IllegalStateException("Unexpected B2.2 geographic evidence version/approval");
            }
            Map<String, Holding> found = new LinkedHashMap<>();
            JsonArray entries = root.getAsJsonArray("holdings");
            for (JsonElement raw : entries) {
                JsonObject entry = raw.getAsJsonObject();
                List<Marker> markers = new ArrayList<>();
                for (JsonElement rawMarker : entry.getAsJsonArray("candidates")) {
                    JsonObject m = rawMarker.getAsJsonObject();
                    if (m.get("physicalPlacementApproved").getAsBoolean()) {
                        throw new IllegalStateException("Unreviewed marker marked build-approved");
                    }
                    markers.add(new Marker(
                            m.get("markerId").getAsString(),
                            m.get("candidateX").getAsLong(),
                            m.get("candidateZ").getAsLong(),
                            m.get("identityEvidence").getAsString(),
                            m.get("reviewStatus").getAsString(),
                            m.get("landMaskStatus").getAsString()));
                }
                Holding holding = new Holding(
                        entry.get("holdingId").getAsString(),
                        entry.get("worldLocationId").getAsString(),
                        entry.get("holdingType").getAsString(),
                        entry.get("isDomain").getAsBoolean(),
                        entry.get("status").getAsString(), markers);
                if (found.putIfAbsent(holding.authoredHoldingId(), holding) != null) {
                    throw new IllegalStateException("Duplicate B2.2 holding " + holding.authoredHoldingId());
                }
            }
            return Map.copyOf(found);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read B2.2 holding geographic evidence", error);
        }
    }
}

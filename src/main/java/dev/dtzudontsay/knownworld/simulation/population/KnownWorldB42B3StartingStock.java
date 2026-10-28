package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.economy.EconomicOwner;
import dev.dtzudontsay.knownworld.simulation.economy.EconomyAuthorityService;
import dev.dtzudontsay.knownworld.simulation.economy.EconomyRuntime;
import dev.dtzudontsay.knownworld.simulation.economy.InventoryManager;
import dev.dtzudontsay.knownworld.simulation.economy.ResourceId;
import dev.dtzudontsay.knownworld.simulation.economy.ResourceInventory;
import dev.dtzudontsay.knownworld.simulation.integration.KnownWorldSiteRegistry;
import dev.dtzudontsay.knownworld.simulation.world.settlement.Settlement;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

/**
 * B4.2B.3 canonical public-granary bridge. NOT a B4.0 household wallet.
 *
 * Built into the native EconomyRuntime; no alternate economic balances.
 * Default: read-only. A one-site grant requires OP permission, verified
 * site->canonical SettlementId match and SNAPSHOT_PRIMARY economy authority.
 * Pending markers make interrupted grants fail-closed rather than replay.
 */
public final class KnownWorldB42B3StartingStock {
    private static final String SOURCE = "/data/knownworld/scenarios/agot_298_ac/world_opening_reserves_b4_2b3_v1/manifest.json";
    private static final String SCHEMA = "knownworld.b4_2b3.opening_public_reserves.v1";
    private static final ResourceId GRAIN = new ResourceId("grain");
    private static final ResourceId COIN = new ResourceId("coin");
    private static JsonObject plan;
    private KnownWorldB42B3StartingStock() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwstocks")
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("audit")
                    .then(Commands.argument("site", StringArgumentType.word())
                        .executes(ctx -> audit(ctx.getSource(), StringArgumentType.getString(ctx, "site")))))
                .then(Commands.literal("grant")
                    .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                    .then(Commands.argument("site", StringArgumentType.word())
                        .executes(ctx -> grant(ctx.getSource(), StringArgumentType.getString(ctx, "site")))))));
    }

    private static synchronized JsonObject plan() {
        if (plan != null) return plan;
        try (InputStream in = KnownWorldB42B3StartingStock.class.getResourceAsStream(SOURCE)) {
            if (in == null) throw new IllegalStateException("Opening reserve proposal missing; regenerate resource index");
            JsonObject data = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!SCHEMA.equals(data.get("schemaVersion").getAsString())
                || data.get("historicalStartAC").getAsInt() != 298
                || data.get("siteCount").getAsInt() != 248
                || data.get("peopleTotal").getAsLong() != 3760684L
                || data.get("householdsTotal").getAsLong() != 799343L
                || data.get("workersTotal").getAsLong() != 1880341L
                || data.get("actualGrainCreditedKg").getAsLong() != 0
                || data.get("actualCoinCreditedUnits").getAsLong() != 0
                || data.get("autonomousProduction").getAsBoolean()
                || data.get("autonomousConsumption").getAsBoolean()
                || data.get("autorun").getAsBoolean()
                || !"READ_ONLY_PROPOSAL_UNTIL_EXPLICIT_SNAPSHOT_TRANSACTION".equals(data.get("authority").getAsString())
                || data.getAsJsonArray("sitePlans").size() != 248)
                throw new IllegalStateException("Unverified initial economy data; stock grants disabled");
            // Verify all 248 records, not merely the top-level manifest claims.
            long pop = 0, households = 0, workers = 0, grain = 0, coin = 0;
            Set<String> ids = new HashSet<>();
            for (JsonElement element : data.getAsJsonArray("sitePlans")) {
                JsonObject row = element.getAsJsonObject();
                String id = row.get("siteId").getAsString();
                if (!id.matches("site_[a-z][a-z0-9_]{1,90}") || !ids.add(id))
                    throw new IllegalStateException("Unknown or duplicate economic site ID");
                long n = row.get("population").getAsLong();
                long h = row.get("households").getAsLong();
                long w = row.get("modeledWorkers").getAsLong();
                long g = row.get("grainKg").getAsLong();
                long c = row.get("treasuryCoinUnits").getAsLong();
                if (n < 0 || h < 0 || w < 0 || w > n || h > n
                    || g != Math.multiplyExact(n, 75L * 90L) / 100L
                    || c != Math.addExact(Math.multiplyExact(n, 12L), Math.multiplyExact(w, 45L))
                    || row.get("automaticCreditAuthorized").getAsBoolean())
                    throw new IllegalStateException("Opening economy source arithmetic/authority mismatch");
                pop = Math.addExact(pop, n);
                households = Math.addExact(households, h);
                workers = Math.addExact(workers, w);
                grain = Math.addExact(grain, g);
                coin = Math.addExact(coin, c);
            }
            if (ids.size() != 248 || pop != 3760684L || households != 799343L
                || workers != 1880341L
                || grain != data.get("proposedPublicGrainKg").getAsLong()
                || coin != data.get("proposedPublicCoinUnits").getAsLong())
                throw new IllegalStateException("Opening economy site sums are inconsistent");
            plan = data;
            return data;
        } catch (Exception e) {
            throw new IllegalStateException("Opening-stock validation failure", e);
        }
    }

    private static JsonObject sitePlan(String id) {
        if (!id.matches("site_[a-z][a-z0-9_]{1,90}")) throw new IllegalArgumentException("Unsafe site ID");
        JsonArray sites = plan().getAsJsonArray("sitePlans");
        for (JsonElement el : sites) {
            JsonObject row = el.getAsJsonObject();
            if (id.equals(row.get("siteId").getAsString())) return row;
        }
        throw new IllegalArgumentException("Unknown starting-stock site " + id);
    }

    private static String normalized(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static SettlementId canonicalSettlement(String siteId) {
        var site = KnownWorldSiteRegistry.registry().site(siteId)
            .orElseThrow(() -> new IllegalStateException("Site is absent from authoritative registry"));
        NpcSimulation sim = NpcSimulation.getNullable();
        if (sim == null) throw new IllegalStateException("NPC simulation not running");
        SettlementId match = null;
        for (Settlement candidate : sim.settlements().all()) {
            if (!normalized(candidate.name()).equals(normalized(site.name()))) continue;
            var at = candidate.center();
            if (Math.abs(at.x()-site.x()) > 24 || Math.abs(at.z()-site.z()) > 24) continue;
            if (match != null) throw new IllegalStateException("Ambiguous native settlement mapping; no funds granted");
            match = candidate.id();
        }
        if (match == null) throw new IllegalStateException("No validated native SettlementId at this site; no funds granted");
        return match;
    }

    private static Path receiptDir(CommandSourceStack src) {
        return src.getServer().getWorldPath(LevelResource.ROOT)
            .resolve("knownworld").resolve("npc").resolve("b42b3_starting_stock_receipts");
    }
    private static Path receipt(Path directory, String id, String suffix) {
        if (!id.matches("site_[a-z][a-z0-9_]{1,90}")) throw new IllegalArgumentException("Bad receipt ID");
        return directory.resolve(id + suffix);
    }

    private static int status(CommandSourceStack src) {
        try {
            JsonObject data = plan();
            EconomyRuntime economy = EconomyRuntime.getNullable();
            String mode = economy == null ? "NOT_RUNNING" : economy.authorityMode().name();
            message(src, "[B4.2B.3] 248 starting public-reserve proposals, 3,760,684 residents; "
                + "planned grain=" + data.get("proposedPublicGrainKg").getAsLong() + " kg; proposed public treasury="
                + data.get("proposedPublicCoinUnits").getAsLong() + " minor units.");
            message(src, "[B4.2B.3] No automatic grant, consumption, production, wages or personal wallets. "
                + "Native economic authority=" + mode + "; explicit grants require SNAPSHOT_PRIMARY and a valid canonical site link.");
            return 1;
        } catch (Exception e) { src.sendFailure(Component.literal("[B4.2B.3] BLOCKED: " + e.getMessage())); return 0; }
    }

    private static int audit(CommandSourceStack src, String id) {
        try {
            JsonObject row = sitePlan(id);
            String mapping;
            try { mapping = canonicalSettlement(id).toString(); }
            catch (Exception e) { mapping = "BLOCKED: " + e.getMessage(); }
            Path dir = receiptDir(src);
            boolean pending = Files.exists(receipt(dir, id, ".pending.json"), LinkOption.NOFOLLOW_LINKS);
            boolean committed = Files.exists(receipt(dir, id, ".committed.json"), LinkOption.NOFOLLOW_LINKS);
            message(src, "[B4.2B.3] " + id + " pop=" + row.get("population").getAsInt()
                + ", reserve grain=" + row.get("grainKg").getAsLong() + "kg, public coin="
                + row.get("treasuryCoinUnits").getAsLong() + " | native SettlementId=" + mapping);
            message(src, "[B4.2B.3] Receipt pending=" + pending + ", committed=" + committed
                + "; Winterfell V3 and unverified native links may not receive grants. Owner=SETTLEMENT, not individual households.");
            return 1;
        } catch (Exception e) { src.sendFailure(Component.literal("[B4.2B.3] AUDIT FAILED: " + e.getMessage())); return 0; }
    }

    private static int grant(CommandSourceStack src, String id) {
        try {
            JsonObject row = sitePlan(id);
            if (id.equals("site_nw_029")) throw new IllegalStateException("Winterfell V3 requires separate authored approval");
            long grain = row.get("grainKg").getAsLong();
            long coin = row.get("treasuryCoinUnits").getAsLong();
            if (grain <= 0 || coin <= 0) throw new IllegalStateException("No positive public opening stock");
            // Scenario-day grants are an explicit operator decision; no automatic
            // bootstrap, personal entitlements, or historical time reset.
            EconomyRuntime economy = EconomyRuntime.getNullable();
            if (economy == null || economy.authorityMode() != EconomyAuthorityService.Mode.SNAPSHOT_PRIMARY)
                throw new IllegalStateException("Requires explicitly activated SNAPSHOT_PRIMARY; no legacy write permitted");
            SettlementId canonical = canonicalSettlement(id);
            Path dir = receiptDir(src);
            if (Files.isSymbolicLink(dir)) throw new IllegalStateException("Linked receipt directory is unsafe");
            Path pending = receipt(dir, id, ".pending.json");
            Path committed = receipt(dir, id, ".committed.json");
            if (Files.exists(pending, LinkOption.NOFOLLOW_LINKS) || Files.exists(committed, LinkOption.NOFOLLOW_LINKS))
                throw new IllegalStateException("Prior grant receipt exists; no duplicate, manual audit required");
            InventoryManager ledger = economy.inventories();
            EconomicOwner owner = EconomicOwner.settlement(canonical);
            // Existing balances must not be silently topped up. Zero-only initial balances.
            for (ResourceInventory inventory : ledger.all()) {
                if (inventory.owner().equals(owner) && inventory.location().equals(canonical)
                    && (inventory.quantity(GRAIN) != 0 || inventory.quantity(COIN) != 0))
                    throw new IllegalStateException("Native settlement already owns grain/coin; no opening grant allowed");
            }
            if (ledger.remainingJournalCapacity() < 2)
                throw new IllegalStateException("Journal has insufficient capacity for both opening entries");
            Files.createDirectories(dir);
            // Pending receipt is durable BEFORE any authoritative economic mutation.
            // Crash after verified checkpoint but before rename means STOP and manual reconcile.
            String content="{\"batch\":\"B4_2B3\",\"siteId\":\""+id+"\",\"canonicalSettlementId\":"
                +canonical.value()+",\"grainKg\":"+grain+",\"publicCoinUnits\":"+coin
                +",\"status\":\"PENDING_DISALLOW_REPLAY\"}\n";
            try (FileChannel channel = FileChannel.open(pending, StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            ResourceInventory inv = economy.provisioner().ensureEmptyInventory(owner, canonical);
            ledger.produce(inv.id(), GRAIN, grain, "B4.2B.3 298AC initial public granary "+id);
            ledger.produce(inv.id(), COIN, coin, "B4.2B.3 298AC initial settlement treasury "+id);
            // Explicit native verified economy snapshot. Never mark committed before this succeeds.
            economy.commitB42B3SnapshotNow();
            try { Files.move(pending, committed, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { throw new IOException("Snapshot committed, but atomic receipt rename unavailable; leave pending and stop",e); }
            message(src,"[B4.2B.3] VERIFIED SNAPSHOT COMMITTED for "+id+" (native settlement "+canonical+ "). "
                +"Public grain="+grain+" kg; treasury coin="+coin+" minor units. Household balances untouched.");
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("[B4.2B.3] GRANT REFUSED/INTERRUPTED: " + e.getMessage()
                + ". Check /kwstocks audit and any pending receipt; NEVER retry blindly."));
            return 0;
        }
    }
    private static void message(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg), false);
    }
}

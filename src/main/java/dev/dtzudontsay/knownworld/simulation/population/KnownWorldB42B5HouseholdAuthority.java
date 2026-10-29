package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.economy.EconomicOwner;
import dev.dtzudontsay.knownworld.simulation.economy.EconomyRuntime;
import dev.dtzudontsay.knownworld.simulation.economy.ResourceId;
import dev.dtzudontsay.knownworld.simulation.economy.ResourceInventory;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * B4.2B.5 — NON-MUTATING canonical-identity/economic readiness bridge.
 *
 * The existing B4.0 string householdId is authoritative. The existing
 * SettlementManager supplies native SettlementId, and EconomyRuntime is the
 * ONLY inventory authority. This class never allocates IDs, creates accounts,
 * credits stock, pays wages, consumes food or changes world saves.
 *
 * Any failing reconciliation deliberately blocks subsequent economic
 * activation. In particular, a matching map marker is NOT a legal property
 * title, and a workplace coordinator is NOT a verified employer.
 */
public final class KnownWorldB42B5HouseholdAuthority {
    private static final String BASE = "/data/knownworld/scenarios/agot_298_ac/world_household_daily_b4_2b4_v1/";
    private static final String SCHEMA = "knownworld.b4_2b4.household_daily_plan.v1";
    private static final ResourceId GRAIN = new ResourceId("grain");
    private static final ResourceId COIN = new ResourceId("coin");
    private static volatile JsonObject manifest;

    public record Audit(String siteId, int households, int people, int workers,
                        long proposedGrainKg, long proposedCoinUnits,
                        long dailyGrainGrams, long proposedWages,
                        SettlementId nativeSettlement, String blocker) {
        public boolean siteIdentityVerified() { return nativeSettlement != null; }
    }

    private KnownWorldB42B5HouseholdAuthority() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwbridge")
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("site")
                    .then(Commands.argument("siteId", StringArgumentType.word())
                        .executes(ctx -> site(ctx.getSource(),
                            StringArgumentType.getString(ctx, "siteId")))))
                .then(Commands.literal("household")
                    .then(Commands.argument("siteId", StringArgumentType.word())
                        .then(Commands.argument("householdId", StringArgumentType.string())
                            .executes(ctx -> household(ctx.getSource(),
                                StringArgumentType.getString(ctx, "siteId"),
                                StringArgumentType.getString(ctx, "householdId"))))))));
    }

    private static synchronized JsonObject manifest() {
        if (manifest != null) return manifest;
        try (InputStream in = KnownWorldB42B5HouseholdAuthority.class.getResourceAsStream(BASE + "manifest.json")) {
            if (in == null) throw new IllegalStateException("Missing B4.2B.4 authoritative source manifest");
            JsonObject m = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!SCHEMA.equals(m.get("schemaVersion").getAsString())
                || m.get("siteCount").getAsInt() != 248
                || m.get("peopleTotal").getAsLong() != 3760684L
                || m.get("householdsTotal").getAsLong() != 799343L
                || m.get("workersTotal").getAsLong() != 1880341L
                || m.get("dailyTransactionsActivated").getAsBoolean()
                || !m.get("publicStockNotDuplicated").getAsBoolean()
                || !"HOUSEHOLD_TRANSFER_PLANNING_NATIVE_INVENTORY_ONLY".equals(m.get("authority").getAsString()))
                throw new IllegalStateException("Unsafe or changed household source; bridge disabled");
            JsonArray sites = m.getAsJsonArray("sites");
            if (sites == null || sites.size() != 248) throw new IllegalStateException("Expected exactly 248 site records");
            Set<String> seen = new HashSet<>();
            long households = 0, people = 0, workers = 0;
            for (JsonElement e : sites) {
                JsonObject s = e.getAsJsonObject();
                String id = s.get("siteId").getAsString();
                if (!safeSite(id) || !seen.add(id)) throw new IllegalStateException("Duplicate/unsafe site ID");
                long h = s.get("households").getAsLong(), p = s.get("people").getAsLong();
                long w = s.get("workers").getAsLong();
                if (h < 0 || p < 0 || w < 0 || h > p || w > p) throw new IllegalStateException("Invalid site census " + id);
                households = Math.addExact(households, h);
                people = Math.addExact(people, p);
                workers = Math.addExact(workers, w);
            }
            if (households != 799343L || people != 3760684L || workers != 1880341L)
                throw new IllegalStateException("B4.2B.4 census no longer reconciles");
            manifest = m;
            return m;
        } catch (Exception e) {
            throw new IllegalStateException("B4.2B.5 source validation failed", e);
        }
    }

    private static boolean safeSite(String id) {
        return id != null && id.matches("site_[a-z][a-z0-9_]{1,90}");
    }

    private static JsonObject siteRow(String id) {
        if (!safeSite(id)) throw new IllegalArgumentException("Invalid site ID");
        for (JsonElement e : manifest().getAsJsonArray("sites")) {
            JsonObject row = e.getAsJsonObject();
            if (id.equals(row.get("siteId").getAsString())) return row;
        }
        throw new IllegalArgumentException("No B4.2B.4 site " + id);
    }

    private static int partition(String householdId) {
        try {
            byte[] sha = MessageDigest.getInstance("SHA-256")
                .digest(householdId.getBytes(StandardCharsets.UTF_8));
            return ((((sha[0] & 255) << 8) | (sha[1] & 255))) % 16;
        } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }

    private static JsonObject shard(String site, int part) {
        String name = BASE + site + "/p" + String.format(Locale.ROOT, "%02d", part) + ".json.gz";
        try (InputStream in = KnownWorldB42B5HouseholdAuthority.class.getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("Missing household partition " + name);
            try (GZIPInputStream gz = new GZIPInputStream(in)) {
                JsonObject obj = JsonParser.parseReader(new InputStreamReader(gz, StandardCharsets.UTF_8)).getAsJsonObject();
                if (obj.getAsJsonObject("households") == null)
                    throw new IllegalStateException("Partition contains no households object " + name);
                return obj.getAsJsonObject("households");
            }
        } catch (Exception e) { throw new IllegalStateException("Unreadable B4 household shard " + name, e); }
    }

    /**
     * Read-only per-site census validation, independently summing the actual
     * compressed records. Does not treat the proposed figures as real balances.
     * Intentionally invoked only on demand; large cities can take more work.
     */
    public static Audit audit(String siteId) {
        JsonObject row = siteRow(siteId);
        int households = 0, people = 0, workers = 0;
        long grain = 0, coin = 0, demand = 0, wages = 0;
        Set<String> seen = new HashSet<>();
        JsonArray parts = row.getAsJsonArray("parts");
        if (parts == null) throw new IllegalStateException("Missing parts list");
        Set<Integer> found = new HashSet<>();
        for (JsonElement element : parts) {
            int part = element.getAsInt();
            if (part < 0 || part >= 16 || !found.add(part))
                throw new IllegalStateException("Invalid partition index");
            JsonObject members = shard(siteId, part);
            for (var entry : members.entrySet()) {
                String id = entry.getKey();
                if (id.isBlank() || id.length() > 180 || partition(id) != part || !seen.add(id))
                    throw new IllegalStateException("Duplicate/mispartitioned household " + id);
                JsonObject h = entry.getValue().getAsJsonObject();
                // No fabricated native owner, household inventory or transactions.
                if (!h.get("nativeHouseholdOwnerId").isJsonNull()
                    || !h.get("nativeInventoryId").isJsonNull()
                    || h.get("authoritativeTransactions").getAsBoolean())
                    throw new IllegalStateException("Unexpected authoritative ownership in read-only source");
                int m = h.get("members").getAsInt(), w = h.get("workers").getAsInt();
                if (m <= 0 || w < 0 || w > m || h.get("headPersonId").getAsString().isBlank())
                    throw new IllegalStateException("Invalid household demographics " + id);
                households = Math.addExact(households, 1);
                people = Math.addExact(people, m);
                workers = Math.addExact(workers, w);
                grain = Math.addExact(grain, h.get("grainAllocationFromPublicKg").getAsLong());
                coin = Math.addExact(coin, h.get("openingCoinTransferUnits").getAsLong());
                demand = Math.addExact(demand, h.get("dailyGrainNeedGrams").getAsLong());
                wages = Math.addExact(wages, h.get("proposedDailyWagesCoinUnits").getAsLong());
            }
        }
        if (households != row.get("indexedHouseholds").getAsInt()
            || people != row.get("indexedPeople").getAsInt()
            || people != row.get("people").getAsInt() && households > 0
            || workers != row.get("workers").getAsInt() && households > 0
            || grain != row.get("householdGrainFromPublicKg").getAsLong()
            || coin != row.get("householdCoinFromPublicUnits").getAsLong()
            || demand != row.get("dailyHouseholdDemandGrams").getAsLong()
            || wages != row.get("estimatedDailyGrossWagesCoinUnits").getAsLong())
            throw new IllegalStateException("Per-household sums disagree with frozen site ledger: " + siteId);

        SettlementId nativeSite = null;
        String blocker = null;
        try { nativeSite = KnownWorldB42B3StartingStock.canonicalSettlement(siteId); }
        catch (Exception e) { blocker = "NATIVE_SETTLEMENT_UNRESOLVED: " + e.getMessage(); }
        EconomyRuntime economy = EconomyRuntime.getNullable();
        if (blocker == null && economy == null) blocker = "NATIVE_ECONOMY_NOT_RUNNING";
        if (blocker == null && nativeSite != null) {
            boolean publicBalance = false;
            for (ResourceInventory inv : economy.inventories().all()) {
                if (inv.owner().equals(EconomicOwner.settlement(nativeSite))
                    && inv.location().equals(nativeSite)
                    && (inv.quantity(GRAIN) > 0 || inv.quantity(COIN) > 0)) {
                    publicBalance = true;
                    break;
                }
            }
            if (!publicBalance) blocker = "NO_VERIFIED_PUBLIC_NATIVE_STOCK_AT_SETTLEMENT";
        }
        if (households == 0 && row.get("households").getAsInt() > 0)
            blocker = "SOURCE_HOUSEHOLDS_UNRESOLVED_SEPARATE_AUTHORITY";
        if (blocker == null) blocker = "HOUSEHOLD_ACCOUNT_OWNER_NOT_YET_IMPLEMENTED";
        return new Audit(siteId, households, people, workers, grain, coin, demand, wages, nativeSite, blocker);
    }

    /** Exact B4 source household record; no new account is minted. */
    public static JsonObject findHousehold(String siteId, String householdId) {
        JsonObject site = siteRow(siteId);
        Objects.requireNonNull(householdId, "householdId");
        if (householdId.isBlank() || householdId.length() > 180)
            throw new IllegalArgumentException("Invalid household key");
        int part = partition(householdId);
        boolean allowed = false;
        for (JsonElement e : site.getAsJsonArray("parts")) if (e.getAsInt() == part) { allowed = true; break; }
        if (!allowed) throw new IllegalArgumentException("No household partition for this key at " + siteId);
        JsonElement match = shard(siteId, part).get(householdId);
        if (match == null || !match.isJsonObject()) throw new IllegalArgumentException("Unknown B4 household at site " + siteId);
        return match.getAsJsonObject().deepCopy();
    }

    private static int status(CommandSourceStack src) {
        try {
            JsonObject m = manifest();
            NpcSimulation sim = NpcSimulation.getNullable();
            EconomyRuntime economy = EconomyRuntime.getNullable();
            send(src, "[B4.2B.5] Frozen B4.0 population=" + m.get("peopleTotal").getAsLong()
                + ", households=" + m.get("householdsTotal").getAsLong()
                + ", indexed=" + m.get("indexedHouseholds").getAsLong()
                + ", unresolved=" + m.get("unresolvedHouseholds").getAsLong());
            send(src, "[B4.2B.5] Native settlements currently loaded="
                + (sim == null ? "NOT_RUNNING" : sim.settlements().size())
                + ", economy=" + (economy == null ? "NOT_RUNNING" : economy.authorityMode())
                + ". Read-only checks; no wallets, transfers, wages or automatic food consumption.");
            return 1;
        } catch (Exception e) { return error(src, e); }
    }

    private static int site(CommandSourceStack src, String siteId) {
        try {
            Audit a = audit(siteId);
            send(src, "[B4.2B.5] " + a.siteId() + ": " + a.households() + " households, "
                + a.people() + " indexed people, " + a.workers() + " indexed workers. SOURCE INTEGRITY PASSED; NOT AN ECONOMIC ACTIVATION.");
            send(src, "[B4.2B.5] Proposed only: grain=" + a.proposedGrainKg()
                + " kg, coin=" + a.proposedCoinUnits() + ", daily grain=" + a.dailyGrainGrams()
                + " g, wages=" + a.proposedWages());
            send(src, "[B4.2B.5] Native settlement="
                + (a.nativeSettlement() == null ? "UNRESOLVED" : a.nativeSettlement())
                + "; activation BLOCKED: " + a.blocker());
            return 1;
        } catch (Exception e) { return error(src, e); }
    }

    private static int household(CommandSourceStack src, String siteId, String householdId) {
        try {
            JsonObject h = findHousehold(siteId, householdId);
            send(src, "[B4.2B.5] " + householdId + " at " + siteId + ": "
                + h.get("members").getAsInt() + " residents, " + h.get("workers").getAsInt()
                + " modeled workers; head candidate=" + h.get("headPersonId").getAsString());
            send(src, "[B4.2B.5] Native owner=UNBOUND; inventory=UNBOUND; planned grain="
                + h.get("grainAllocationFromPublicKg").getAsLong() + "kg, coin="
                + h.get("openingCoinTransferUnits").getAsLong()
                + ". Neither income nor ownership is legally verified.");
            return 1;
        } catch (Exception e) { return error(src, e); }
    }

    private static int error(CommandSourceStack src, Exception e) {
        src.sendFailure(Component.literal("[B4.2B.5] BLOCKED: " + e.getMessage()));
        return 0;
    }
    private static void send(CommandSourceStack src, String message) {
        src.sendSuccess(() -> Component.literal(message), false);
    }
}

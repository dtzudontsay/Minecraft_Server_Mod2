package dev.dtzudontsay.knownworld.world.generation.settlement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * C2 controlled real-worldgen pilot. Each block is written ONLY to the new
 * ChunkAccess that Minecraft is already constructing. The immutable plot index
 * is precomputed from reviewed B2 map anchors and the C1 blueprint demand.
 *
 * No runtime /fill, no ServerLevel mutation, no NPCs, no edits to existing
 * chunks. Never overwrites Winterfell and never enables special sites.
 *
 * IMPORTANT: This is C2 PHASE ONE ONLY, not the worldwide C3/C4 executor.
 */
public final class KnownWorldUniversalWorldgenC2 {
    private static final String RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/settlement_construction_c4_v1.json";
    private static volatile Map<String, Pilot> pilots;
    private static final AtomicInteger DECORATED_CHUNKS = new AtomicInteger();
    private static final AtomicInteger SKIPPED_UNSAFE_PLOT_FRAGMENTS = new AtomicInteger();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();

    public record Lot(String id, String kind, int x, int z, int width, int depth,
                      String face, int capacity, int maxStaff) { }
    public record PilotInfo(String siteId, String name, String tier, List<String> markerIds,
                            int anchorX, int anchorZ, int lots, int chunks, int safeLots,
                            int skippedLots, int housingBacklog) { }
    private record ChunkPlan(BitSet roads, int[] lots) { }
    private record Pilot(String id, String name, String tier, String palette,
                         List<String> markers, int x, int z, int housingBacklog,
                         int[] bounds, List<Lot> lots, Map<Long, ChunkPlan> chunks) { }
    private record Survey(boolean safe, int floor, int minY, int maxY) { }

    private KnownWorldUniversalWorldgenC2() { }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffff_ffffL);
    }

    private static Map<String, Pilot> data() {
        Map<String, Pilot> result = pilots;
        if (result != null) return result;
        synchronized (KnownWorldUniversalWorldgenC2.class) {
            if (pilots == null) pilots = load();
            return pilots;
        }
    }

    private static Map<String, Pilot> load() {
        try (InputStream stream = KnownWorldUniversalWorldgenC2.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing C2 generated resource "
                    + RESOURCE + "; run py tools/geodata/build_c2_physical_pilots.py");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream,
                    StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != 1
                    || !root.getAsJsonObject("policy").get("pilotsOnly").getAsBoolean()
                    || root.getAsJsonObject("summary").get("allMapMarkers").getAsInt() != 253
                    || root.getAsJsonObject("summary").get("allPoliticalHoldings").getAsInt() != 259
                    || root.getAsJsonObject("summary").get("pilotCount").getAsInt() != 2)
                throw new IllegalStateException("C2 pilot manifest invalid or broad authorization attempted");
            Map<String, Pilot> all = new LinkedHashMap<>();
            for (JsonElement entry : root.getAsJsonArray("pilots")) {
                JsonObject p = entry.getAsJsonObject();
                String id = p.get("siteId").getAsString();
                String tier = p.get("tier").getAsString();
                if (!("SMALL".equals(tier) || "MEDIUM".equals(tier))
                        || !"GENERAL_COMPACT_SETTLEMENT".equals(p.get("mode").getAsString()))
                    throw new IllegalStateException("Unsafe C2 pilot: " + id);
                JsonObject anchor = p.getAsJsonObject("anchor");
                List<String> markers = new ArrayList<>();
                for (JsonElement marker : p.getAsJsonArray("markerIds")) {
                    String name = marker.getAsString();
                    if ("NW-029".equals(name)) throw new IllegalStateException("Cannot override Winterfell V3");
                    markers.add(name);
                }
                int[] bounds = new int[4];
                JsonArray bb = p.getAsJsonArray("bounds");
                for (int i = 0; i < 4; i++) bounds[i] = bb.get(i).getAsInt();
                List<Lot> lots = new ArrayList<>();
                for (JsonElement e : p.getAsJsonArray("lots")) {
                    JsonObject b = e.getAsJsonObject();
                    int w = b.get("width").getAsInt(), d = b.get("depth").getAsInt();
                    if (w < 5 || d < 5 || w > 33 || d > 33)
                        throw new IllegalStateException("Unexpected C2 building size in " + id);
                    lots.add(new Lot(b.get("id").getAsString(), b.get("kind").getAsString(),
                            b.get("x").getAsInt(), b.get("z").getAsInt(), w, d,
                            b.get("face").getAsString(), b.get("capacity").getAsInt(),
                            b.get("staff").getAsInt()));
                }
                Map<Long, ChunkPlan> chunks = new HashMap<>();
                for (JsonElement e : p.getAsJsonArray("chunks")) {
                    JsonObject c = e.getAsJsonObject();
                    BitSet road = new BitSet(256);
                    for (JsonElement i : c.getAsJsonArray("r")) {
                        int pixel = i.getAsInt();
                        if (pixel < 0 || pixel >= 256) throw new IllegalStateException("Bad C2 road index");
                        road.set(pixel);
                    }
                    JsonArray rawLots = c.getAsJsonArray("b");
                    int[] ids = new int[rawLots.size()];
                    for (int i = 0; i < ids.length; i++) {
                        ids[i] = rawLots.get(i).getAsInt();
                        if (ids[i] < 0 || ids[i] >= lots.size())
                            throw new IllegalStateException("Bad C2 lot index");
                    }
                    long chunkKey = key(c.get("cx").getAsInt(), c.get("cz").getAsInt());
                    if (chunks.putIfAbsent(chunkKey, new ChunkPlan(road, ids)) != null)
                        throw new IllegalStateException("Duplicate C2 chunk index");
                }
                Pilot pilot = new Pilot(id, p.get("name").getAsString(), tier,
                        p.get("palette").getAsString(), List.copyOf(markers),
                        anchor.get("x").getAsInt(), anchor.get("z").getAsInt(),
                        p.get("housingBacklogAtLeast").getAsInt(), bounds,
                        List.copyOf(lots), Map.copyOf(chunks));
                if (all.putIfAbsent(id, pilot) != null)
                    throw new IllegalStateException("Duplicate C2 pilot " + id);
            }
            if (all.size() != 2) throw new IllegalStateException("C2 must have exactly two pilots");
            KnownWorld.LOGGER.info("C2 pilot worldgen indexed {} sites (no NPC spawning)", all.size());
            return Map.copyOf(all);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot load C2 pilot worldgen resource", failure);
        }
    }

    public static List<PilotInfo> inspect() {
        List<PilotInfo> infos = new ArrayList<>();
        for (Pilot p : data().values()) {
            int safe = 0;
            for (Lot l : p.lots) if (approvedSurvey(l).safe) safe++;
            infos.add(new PilotInfo(p.id, p.name, p.tier, p.markers, p.x, p.z,
                    p.lots.size(), p.chunks.size(), safe, p.lots.size() - safe, p.housingBacklog));
        }
        infos.sort(java.util.Comparator.comparing(PilotInfo::siteId));
        return List.copyOf(infos);
    }

    public static int chunksDecorated() { return DECORATED_CHUNKS.get(); }
    public static int skippedUnsafePlotFragments() { return SKIPPED_UNSAFE_PLOT_FRAGMENTS.get(); }

    /** Reserve a 2-chunk feature-spawn source margin around the actual C2 lots. */
    public static boolean suppressNaturalDecoration(ChunkAccess chunk) {
        int cx = chunk.getPos().x(), cz = chunk.getPos().z();
        int wx = cx << 4, wz = cz << 4;
        for (Pilot p : data().values()) {
            if (wx > p.bounds[1] || wx + 15 < p.bounds[0]
                    || wz > p.bounds[3] || wz + 15 < p.bounds[2]) continue;
            // Margin also protects from biome features starting in adjacent chunks.
            for (int dz = -2; dz <= 2; dz++) {
                for (int dx = -2; dx <= 2; dx++) {
                    if (p.chunks.containsKey(key(cx + dx, cz + dz))) return true;
                }
            }
        }
        return false;
    }

    /** Same pilot footprint is excluded from randomly generated vanilla structures. */
    public static boolean suppressVanillaStructures(ChunkAccess chunk) {
        return suppressNaturalDecoration(chunk);
    }

    public static void decorate(WorldGenLevel level, ChunkAccess chunk) {
        int cx = chunk.getPos().x(), cz = chunk.getPos().z();
        int wx = cx << 4, wz = cz << 4;
        for (Pilot p : data().values()) {
            if (wx > p.bounds[1] || wx + 15 < p.bounds[0]
                    || wz > p.bounds[3] || wz + 15 < p.bounds[2]) continue;
            ChunkPlan cp = p.chunks.get(key(cx, cz));
            if (cp == null) continue;
            // Only new chunk generation. We do not load or modify neighbours.
            // Suppression above prevents natural trees/lava from being generated
            // inside the reservations of this and its nearby source chunks.
            List<KnownWorldC4RoadPainter.Entrance> entrances = entrancesNear(p, wx, wz);
            for (int index = cp.roads.nextSetBit(0); index >= 0; index = cp.roads.nextSetBit(index + 1)) {
                int x = wx + (index & 15), z = wz + (index >>> 4);
                road(chunk, x, z, p, entrances);
            }
            for (int id : cp.lots) {
                Lot lot = p.lots.get(id);
                Survey terrain = approvedSurvey(lot);
                if (!terrain.safe) {
                    SKIPPED_UNSAFE_PLOT_FRAGMENTS.incrementAndGet();
                    continue;
                }
                if ("FIELD".equals(lot.kind())) {
                    // FIELD uses the C2 irrigated-field painter; C3 intentionally
                    // rejects it as a building template.
                    buildLot(chunk, lot, terrain.floor, p.palette, wx, wz);
                } else {
                    KnownWorldC3Architecture.buildLot(chunk, lot, terrain.floor, p.palette, p.tier);
                }
            }
            DECORATED_CHUNKS.incrementAndGet();
        }
    }

    private static boolean isLand(int x, int z) {
        return KnownWorldGeoSampler.sampleMinecraft(x + .5, z + .5).land();
    }

    /** Reuses the same authoritative geographic elevation used by KnownWorldChunkGenerator. */
    private static int top(int x, int z) {
        double metres = GeographicDataManager.getInstance()
                .sample(WorldProjection.fromMinecraft(x + .5, z + .5)).elevationMetres();
        return (int) Math.max(-63, Math.min(2030, Math.round(metres)));
    }

    private static Survey survey(Lot l) {
        int[] samples = new int[9];
        int i = 0, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        // Check the actual footprint, not only nine samples: narrow rivers
        // and sharp cliffs sometimes hide between the corners/center.
        for (int dx = 0; dx < l.width; dx += 2) {
            for (int dz = 0; dz < l.depth; dz += 2) {
                int x = l.x + dx, z = l.z + dz;
                if (!isLand(x, z)) return new Survey(false, 0, 0, 0);
                int height = top(x, z);
                if (i < samples.length) samples[i++] = height;
                lo = Math.min(lo, height); hi = Math.max(hi, height);
            }
        }
        // Consistent floor comes from the nine canonical survey points,
        // while the denser coverage above rejects hidden hazards.
        i = 0;
        for (int dx : new int[] {1, l.width / 2, l.width - 2})
            for (int dz : new int[] {1, l.depth / 2, l.depth - 2})
                samples[i++] = top(l.x + dx, l.z + dz);
        Arrays.sort(samples);
        // Tiered terraces handle moderate mountain slopes without impossible
        // fill pillars. Truly steep/cliff or water plots require C3 placement.
        int maxGrade = "FIELD".equals(l.kind) ? 3 : 6;
        int floor = samples[5];
        boolean accessible = true;
        if (!"FIELD".equals(l.kind)) {
            int doorX = l.x + l.width / 2;
            int doorZ = "NORTH".equals(l.face) ? l.z - 2 : l.z + l.depth + 1;
            accessible = isLand(doorX, doorZ)
                    && Math.abs(top(doorX, doorZ) - floor) <= 5;
        }
        return new Survey(hi - lo <= maxGrade && floor > -48 && floor < 1980
                && accessible, floor, lo, hi);
    }

    private static BlockState wall(String palette) {
        String id = palette.toUpperCase(java.util.Locale.ROOT);
        if (id.contains("DORN")) return Blocks.SANDSTONE.defaultBlockState();
        if (id.contains("SUMMER") || id.contains("SOTHORYOS"))
            return Blocks.JUNGLE_PLANKS.defaultBlockState();
        if (id.contains("GHISCAR") || id.contains("FREE_CITY"))
            return Blocks.BRICKS.defaultBlockState();
        if (id.contains("YITI") || id.contains("SHADOW"))
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        if (id.contains("NORTHERN") || id.contains("WALL") || id.contains("IBBEN"))
            return Blocks.SPRUCE_PLANKS.defaultBlockState();
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    private static BlockState roof(String palette) {
        String id = palette.toUpperCase(java.util.Locale.ROOT);
        if (id.contains("DORN") || id.contains("GHISCAR"))
            return Blocks.TERRACOTTA.defaultBlockState();
        if (id.contains("YITI") || id.contains("SHADOW"))
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        if (id.contains("SUMMER") || id.contains("SOTHORYOS"))
            return Blocks.JUNGLE_PLANKS.defaultBlockState();
        return Blocks.DARK_OAK_PLANKS.defaultBlockState();
    }

    // C4.2 APPROVED STREET ENTRANCE CONNECTIONS
    private static final java.util.concurrent.ConcurrentMap<String, Survey> SURVEY_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static Survey approvedSurvey(Lot lot) {
        return SURVEY_CACHE.computeIfAbsent(lot.id(), ignored -> survey(lot));
    }

    /** Only entrances near the current chunk; no O(roads * all buildings) surveys. */
    private static List<KnownWorldC4RoadPainter.Entrance> entrancesNear(
            Pilot pilot, int worldX, int worldZ) {
        List<KnownWorldC4RoadPainter.Entrance> nearby = new ArrayList<>();
        for (Lot lot : pilot.lots) {
            if ("FIELD".equals(lot.kind())) continue;
            int doorX = lot.x() + lot.width() / 2;
            int doorZ = "NORTH".equals(lot.face())
                    ? lot.z() - 1 : lot.z() + lot.depth();
            if (doorX < worldX - 5 || doorX > worldX + 20
                    || doorZ < worldZ - 5 || doorZ > worldZ + 20) continue;
            Survey survey = approvedSurvey(lot);
            if (survey.safe())
                nearby.add(new KnownWorldC4RoadPainter.Entrance(doorX, doorZ, survey.floor()));
        }
        return nearby;
    }

    private static void road(ChunkAccess chunk, int x, int z, Pilot pilot,
                             List<KnownWorldC4RoadPainter.Entrance> entrances) {
        KnownWorldC4RoadPainter.paint(chunk, x, z, pilot.palette,
                KnownWorldUniversalWorldgenC2::top,
                KnownWorldUniversalWorldgenC2::isLand, entrances);
    }

    private static void clearHazards(ChunkAccess c, int x, int z, int terrainY) {
        for (int y = Math.max(c.getMinY(), terrainY - 18);
                y <= Math.min(c.getMaxY() - 1, terrainY + 22); y++) {
            BlockState state = c.getBlockState(new BlockPos(x, y, z));
            if (state.getFluidState().is(FluidTags.LAVA)) put(c, x, y, z,
                    y <= terrainY ? COBBLE : AIR);
            else if (y >= terrainY - 2 && (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)))
                put(c, x, y, z, AIR);
        }
    }

    private static void buildLot(ChunkAccess c, Lot b, int floor, String palette, int wx, int wz) {
        int x1 = b.x + b.width - 1, z1 = b.z + b.depth - 1;
        int x0 = Math.max(b.x, wx), z0 = Math.max(b.z, wz);
        int endX = Math.min(x1, wx + 15), endZ = Math.min(z1, wz + 15);
        BlockState wall = wall(palette), roof = roof(palette);
        boolean field = "FIELD".equals(b.kind);
        for (int z = z0; z <= endZ; z++) for (int x = x0; x <= endX; x++) {
            int xx = x - b.x, zz = z - b.z;
            int ground = top(x, z);
            clearHazards(c, x, z, ground);
            if (field) {
                // 17x26 irrigated farmland; workers use a single farm building
                // with a staffing cap. Crops are real blocks, not census NPCs.
                boolean water = ((xx - 4) % 8 == 0) && ((zz - 4) % 8 == 0);
                for (int y = Math.min(ground, floor) - 3; y < floor; y++) put(c, x, y, z, COBBLE);
                if (water) {
                    put(c, x, floor, z, Blocks.WATER.defaultBlockState());
                } else {
                    put(c, x, floor, z, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
                    put(c, x, floor + 1, z, Blocks.WHEAT.defaultBlockState());
                }
                for (int y = floor + 2; y <= Math.max(floor + 8, ground + 8); y++) put(c, x, y, z, AIR);
                continue;
            }
            // Fix and terrace the whole building footprint, with strict grade limit.
            for (int y = Math.min(ground, floor) - 5; y <= floor; y++) put(c, x, y, z,
                    y == floor ? Blocks.SPRUCE_PLANKS.defaultBlockState() : COBBLE);
            int slope = Math.min(3, Math.min(xx, b.width - 1 - xx));
            int wallHeight = ("BARRACKS".equals(b.kind) || "TEMPLE".equals(b.kind)) ? 8 : 5;
            int roofY = floor + wallHeight + 1 + slope;
            for (int y = floor + 1; y <= Math.max(roofY + 3, ground + 12); y++) put(c, x, y, z, AIR);
            boolean edge = xx == 0 || xx == b.width - 1 || zz == 0 || zz == b.depth - 1;
            if (edge) for (int y = floor + 1; y <= floor + wallHeight; y++) put(c, x, y, z, wall);
            if ((zz == 0 || zz == b.depth - 1) && slope > 0)
                for (int y = floor + wallHeight + 1; y < roofY; y++) put(c, x, y, z, wall);
            put(c, x, roofY, z, roof);
            boolean north = "NORTH".equals(b.face);
            boolean door = edge && xx == b.width / 2 && zz == (north ? 0 : b.depth - 1);
            if (door) {
                BlockState state = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING,
                        north ? Direction.NORTH : Direction.SOUTH);
                put(c, x, floor + 1, z, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                put(c, x, floor + 2, z, state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            } else if (edge && zz == (north ? b.depth - 1 : 0)
                    && (xx == 2 || xx == b.width - 3))
                put(c, x, floor + 3, z, Blocks.GLASS_PANE.defaultBlockState());
            if (xx == 2 && zz == 2) {
                put(c, x, floor + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                put(c, x, floor + 2, z, Blocks.LANTERN.defaultBlockState());
            }
            if (xx == 3 && zz == b.depth - 3) {
                put(c, x, floor + 1, z, interiorWorkstation(b.kind));
            }
            if ("HOUSE".equals(b.kind) && (xx == 3 || xx == 5 || xx == 7)
                    && (zz == 3 || zz == 4)) {
                BlockState bed = Blocks.BED.red().defaultBlockState()
                        .setValue(BedBlock.FACING, Direction.SOUTH);
                put(c, x, floor + 1, z,
                        bed.setValue(BedBlock.PART, zz == 3 ? BedPart.FOOT : BedPart.HEAD));
            }
            if ("HOUSE".equals(b.kind) && xx == b.width - 3 && zz == b.depth - 3)
                put(c, x, floor + 1, z, Blocks.BARREL.defaultBlockState());
            // Household surname signs are NOT fabricated: B4 must bind actual
            // persistent household IDs to these stable lot IDs first.
            // Adjacent-road approaches remain walkable: no two-block cliff
            // jumps across the door apron in moderate terrain.
        }
    }

    private static BlockState interiorWorkstation(String kind) {
        return switch (kind) {
            case "BAKERY", "INN" -> Blocks.FURNACE.defaultBlockState();
            case "SMITHY", "ARMORY" -> Blocks.BLAST_FURNACE.defaultBlockState();
            case "FARM" -> Blocks.COMPOSTER.defaultBlockState();
            case "GRANARY", "MARKET" -> Blocks.BARREL.defaultBlockState();
            case "BARRACKS" -> Blocks.SMITHING_TABLE.defaultBlockState();
            case "ADMINISTRATION" -> Blocks.LECTERN.defaultBlockState();
            case "TEMPLE" -> Blocks.BOOKSHELF.defaultBlockState();
            default -> Blocks.CRAFTING_TABLE.defaultBlockState();
        };
    }

    private static void put(ChunkAccess c, int x, int y, int z, BlockState value) {
        if (y < c.getMinY() || y >= c.getMaxY()) return;
        if (x >> 4 != c.getPos().x() || z >> 4 != c.getPos().z()) return;
        c.setBlockState(new BlockPos(x, y, z), value, 0);
    }
}

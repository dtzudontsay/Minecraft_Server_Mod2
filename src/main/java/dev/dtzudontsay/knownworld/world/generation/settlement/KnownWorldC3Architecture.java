package dev.dtzudontsay.knownworld.world.generation.settlement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2.Lot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * C3 block-by-block architecture for C1's 43 named modules.
 *
 * Called ONLY by C2 after the plot has passed the existing geographic survey.
 * Runs inside Minecraft's new-chunk decoration pass. All writes are clipped to
 * that exact ChunkAccess. Does not load surrounding chunks or touch ServerLevel.
 *
 * This implements *physical* interiors, doors, multi-floor access, workstations,
 * farms, pens and public-site forms. It does NOT simulate a worker or invent
 * household names. Even a fully furnished bakery has 0 staffed NPCs until B4/D.
 */
public final class KnownWorldC3Architecture {
    private static final String RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/architecture_templates_c3_v1.json";
    /**
     * Delay block-registry access until world generation actually paints a lot.
     * C3 metadata queries (and ordinary JUnit tests) must not initialize
     * Minecraft's registries before the game calls Bootstrap.bootStrap().
     */
    private static final class BuildingBlocks {
        private static final BlockState AIR = Blocks.AIR.defaultBlockState();
        private static final BlockState STONE = Blocks.COBBLESTONE.defaultBlockState();
        private BuildingBlocks() { }
    }
    private static volatile Map<String, Template> catalog;
    // C4_6_LIVE_GALLERY_SINK: render the SAME C3 template into a loaded
    // superflat inspection world via ServerLevel.setBlock (client-sync safe).
    // Thread-local only, always cleared after the one gallery structure.
    private static final ThreadLocal<ServerLevel> GALLERY_LEVEL = new ThreadLocal<>();

    public static void buildLiveGalleryLot(ServerLevel level, Lot lot, int floor,
                                           String palette, String tier) {
        if (GALLERY_LEVEL.get() != null) throw new IllegalStateException("Nested gallery painter");
        GALLERY_LEVEL.set(level);
        try {
            int minCX=lot.x()>>4, maxCX=(lot.x()+lot.width()+2)>>4;
            int minCZ=lot.z()>>4, maxCZ=(lot.z()+lot.depth()+3)>>4;
            for (int cx=minCX;cx<=maxCX;cx++)
                for (int cz=minCZ;cz<=maxCZ;cz++)
                    buildLot(level.getChunk(cx,cz),lot,floor,palette,tier);
        } finally { GALLERY_LEVEL.remove(); }
    }

    public record Template(String id, String family, int floors, int maxStaff,
                           boolean openAir, boolean requiresShore,
                           boolean requiresMountainResource, boolean requiresPolicy, List<String> stations) { }

    private KnownWorldC3Architecture() { }

    public static Map<String, Template> allTemplates() {
        Map<String, Template> ready = catalog;
        if (ready != null) return ready;
        synchronized (KnownWorldC3Architecture.class) {
            if (catalog == null) catalog = readTemplates();
            return catalog;
        }
    }

    public static Template template(String id) {
        String key = switch (id) {
            case "HOUSE" -> "HOUSE_SMALL";
            case "FIELD" -> "GARDEN";
            default -> id;
        };
        Template t = allTemplates().get(key);
        if (t == null) throw new IllegalArgumentException("Unknown C3 architecture module: " + id);
        return t;
    }

    private static Map<String, Template> readTemplates() {
        try (InputStream in = KnownWorldC3Architecture.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Missing C3 architecture resource "
                    + RESOURCE + "; run py tools/geodata/build_c3_architectural_templates.py");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in,
                    StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != 1
                    || root.get("templateCount").getAsInt() < 43
                    || !root.get("doesNotAuthoriseNewSites").getAsBoolean()
                    || !"C2_SELECTED_PILOTS_ONLY".equals(root.get("generationPolicy").getAsString())) {
                throw new IllegalStateException("C3 architecture schema or safety policy mismatch");
            }
            Map<String, Template> templates = new LinkedHashMap<>();
            for (JsonElement raw : root.getAsJsonArray("templates")) {
                JsonObject j = raw.getAsJsonObject();
                List<String> stations = new ArrayList<>();
                for (JsonElement e : j.getAsJsonArray("stations")) stations.add(e.getAsString());
                int floors = j.get("floors").getAsInt();
                if (floors < 1 || floors > 3) throw new IllegalStateException("Unsafe floor count");
                Template t = new Template(j.get("id").getAsString(), j.get("family").getAsString(),
                        floors, j.get("maxStaff").getAsInt(), j.get("openAir").getAsBoolean(),
                        j.get("requiresShore").getAsBoolean(),
                        j.get("requiresMountainResource").getAsBoolean(), j.get("requiresPolicy").getAsBoolean(),
                        List.copyOf(stations));
                if (templates.putIfAbsent(t.id(), t) != null)
                    throw new IllegalStateException("Duplicate C3 template: " + t.id());
            }
            if (templates.size() != root.get("templateCount").getAsInt()) throw new IllegalStateException("Missing C3 architecture programs");
            return Map.copyOf(templates);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Cannot read C3 architectural templates", ex);
        }
    }

    /** C2 has already established lot elevation and safe grade. */
    public static void buildLot(ChunkAccess c, Lot lot, int floor, String palette, String tier) {
        String requested = "HOUSE".equals(lot.kind())
                ? ("MEDIUM".equals(tier) ? "HOUSE_MEDIUM" : "HOUSE_SMALL") : lot.kind();
        Template template = template(requested);
        // A special module may be defined and inspectable without being
        // permitted at an unreviewed C2 pilot site.
        if (GALLERY_LEVEL.get() == null && (template.requiresShore() || template.requiresMountainResource() || template.requiresPolicy())) {
            throw new IllegalStateException("Module " + requested
                    + " requires separate C4 site/policy approval, not a C2 pilot");
        }
        if ("FIELD".equals(lot.kind()))
            throw new IllegalArgumentException("C2 farmland remains its own safe field renderer");
        if (template.openAir()) {
            buildOutdoor(c, lot, floor, template);
        } else if ("SHELTER".equals(template.family())) {
            buildShelter(c, lot, floor, template);
        } else {
            buildHouse(c, lot, floor, palette, template);
        }
        // C4.5 - instance-local public building signs for new chunks only.
        if (GALLERY_LEVEL.get() == null)
            KnownWorldStructureSigns.attach(c, lot.x(), lot.z(), lot.width(),
                    lot.depth(), floor, lot.face(), requested);
    }

    private static BlockState regionalWall(String palette, String family) {
        if ("KEEP".equals(family) || "TOWER".equals(family)) return Blocks.STONE_BRICKS.defaultBlockState();
        String p = palette.toUpperCase(Locale.ROOT);
        if (p.contains("DORN")) return Blocks.SANDSTONE.defaultBlockState();
        if (p.contains("GHISCAR") || p.contains("ESSOS_FREE")) return Blocks.BRICKS.defaultBlockState();
        if (p.contains("YITI") || p.contains("SHADOW")) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        if (p.contains("NORTHERN") || p.contains("IBBEN") || p.contains("WALL"))
            return Blocks.SPRUCE_PLANKS.defaultBlockState();
        if (p.contains("SUMMER") || p.contains("SOTHORYOS"))
            return Blocks.JUNGLE_PLANKS.defaultBlockState();
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    private static BlockState regionalRoof(String palette) {
        String p = palette.toUpperCase(Locale.ROOT);
        if (p.contains("DORN") || p.contains("GHISCAR")) return Blocks.TERRACOTTA.defaultBlockState();
        if (p.contains("YITI") || p.contains("SHADOW")) return Blocks.DEEPSLATE_TILES.defaultBlockState();
        if (p.contains("SUMMER") || p.contains("SOTHORYOS")) return Blocks.JUNGLE_PLANKS.defaultBlockState();
        return Blocks.DARK_OAK_PLANKS.defaultBlockState();
    }

    private static void buildHouse(ChunkAccess c, Lot b, int floor, String palette, Template t) {
        int floors = t.floors();
        int wallY = floor + floors * 6;
        boolean flat = palette.toUpperCase(Locale.ROOT).contains("DORN")
                || "STORE".equals(t.family());
        BlockState wall = regionalWall(palette, t.family());
        BlockState roof = regionalRoof(palette);
        BlockState deck = Blocks.SPRUCE_PLANKS.defaultBlockState();
        int x0 = b.x(), z0 = b.z();
        int x1 = x0 + b.width() - 1, z1 = z0 + b.depth() - 1;
        int minX = Math.max(x0, c.getPos().x() << 4);
        int maxX = Math.min(x1, (c.getPos().x() << 4) + 15);
        int minZ = Math.max(z0, c.getPos().z() << 4);
        int maxZ = Math.min(z1, (c.getPos().z() << 4) + 15);
        boolean north = "NORTH".equals(b.face());
        for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
            int xx = x - x0, zz = z - z0;
            clearHazards(c, x, z, floor);
            int slope = flat ? 0 : Math.min(3, Math.min(xx, b.width() - 1 - xx));
            int roofY = wallY + 1 + slope;
            // C2 accepts only plots whose sampled terrain differs by at most six blocks.
            // Bounded foundations/clearance avoid querying a mutable chunk heightmap.
            for (int y = floor - 8; y <= floor; y++)
                put(c, x, y, z, y == floor ? deck : BuildingBlocks.STONE);
            for (int y = floor + 1; y <= Math.max(floor + 16, roofY + 4); y++)
                put(c, x, y, z, BuildingBlocks.AIR);
            boolean perimeter = xx == 0 || xx == b.width() - 1 || zz == 0 || zz == b.depth() - 1;
            if (perimeter) for (int y = floor + 1; y <= wallY; y++) put(c, x, y, z, wall);
            if ((zz == 0 || zz == b.depth() - 1) && slope > 0) {
                for (int y = wallY + 1; y < roofY; y++) put(c, x, y, z, wall);
            }
            put(c, x, roofY, z, roof);
            // Fully walkable upper floors, with an intentional 1x1 ladder shaft.
            if (!perimeter && !(xx == 1 && zz == 3)) {
                for (int level = 1; level < floors; level++)
                    put(c, x, floor + level * 6, z, deck);
            }
            int frontZ = north ? 0 : b.depth() - 1;
            if (perimeter && xx == b.width() / 2 && zz == frontZ) {
                BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(
                        DoorBlock.FACING, north ? Direction.NORTH : Direction.SOUTH);
                put(c, x, floor + 1, z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                put(c, x, floor + 2, z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            } else if (perimeter && (xx == 2 || xx == b.width() - 3)
                    && (zz == 0 || zz == b.depth() - 1) && zz != frontZ) {
                for (int level = 0; level < floors; level++)
                    put(c, x, floor + level * 6 + 3, z, Blocks.GLASS_PANE.defaultBlockState());
            }
            // True supported lantern: lantern sits on fence rather than floating.
            if (xx == b.width() - 2 && zz == b.depth() / 2) {
                put(c, x, floor + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
                put(c, x, floor + 2, z, Blocks.LANTERN.defaultBlockState());
            }
            if (floors > 1 && xx == 1 && zz == 3) {
                // WEST supporting wall at x0; facing EAST is attached to it.
                for (int yy = floor + 1; yy < floor + floors * 6; yy++) {
                    put(c, x - 1, yy, z, wall);
                    put(c, x, yy, z, Blocks.LADDER.defaultBlockState()
                            .setValue(LadderBlock.FACING, Direction.EAST));
                }
            }
        }
        placeStations(c, b, floor, t, north);
        if ("HOME".equals(t.family()) || "URBAN_HOME".equals(t.family()))
            furnishBeds(c, b, floor, t);
        if ("BARRACKS".equals(t.family())) furnishBeds(c, b, floor, t);
        // A fixed sign may only be authored once a real household is bound in
        // B4. Never substitute fabricated family names during worldgen.
    }

    private static void placeStations(ChunkAccess c, Lot lot, int floor, Template t, boolean north) {
        int size = Math.min(3, t.stations().size());
        for (int i = 0; i < size; i++) {
            int xx = 3 + 2 * i;
            int zz = north ? lot.depth() - 3 : 2;
            if (xx >= lot.width() - 1 || zz >= lot.depth() - 1) continue;
            BlockState state = station(t.stations().get(i));
            put(c, lot.x() + xx, floor + 1, lot.z() + zz, state);
        }
        if ("STORE".equals(t.family())) {
            for (int x = 3; x < lot.width() - 3; x += 3)
                put(c, lot.x() + x, floor + 1, lot.z() + lot.depth() / 2,
                        Blocks.BARREL.defaultBlockState());
        }
    }

    private static void furnishBeds(ChunkAccess c, Lot lot, int floor, Template t) {
        int beds = "BARRACKS".equals(t.family()) ? 3 : 2;
        for (int i = 0; i < beds; i++) {
            int xx = 3 + 3 * i;
            if (xx >= lot.width() - 2 || lot.depth() <= 7) continue;
            BlockState bed = Blocks.BED.red().defaultBlockState()
                    .setValue(BedBlock.FACING, Direction.SOUTH);
            put(c, lot.x() + xx, floor + 1, lot.z() + 4,
                    bed.setValue(BedBlock.PART, BedPart.FOOT));
            put(c, lot.x() + xx, floor + 1, lot.z() + 5,
                    bed.setValue(BedBlock.PART, BedPart.HEAD));
        }
    }

    private static void buildOutdoor(ChunkAccess c, Lot lot, int floor, Template t) {
        int xx0 = Math.max(lot.x(), c.getPos().x() << 4);
        int xx1 = Math.min(lot.x() + lot.width() - 1, (c.getPos().x() << 4) + 15);
        int zz0 = Math.max(lot.z(), c.getPos().z() << 4);
        int zz1 = Math.min(lot.z() + lot.depth() - 1, (c.getPos().z() << 4) + 15);
        String family = t.family();
        for (int z = zz0; z <= zz1; z++) for (int x = xx0; x <= xx1; x++) {
            int dx = x - lot.x(), dz = z - lot.z();
            clearHazards(c, x, z, floor);
            for (int y = floor - 3; y <= floor; y++) put(c, x, y, z,
                    y == floor ? Blocks.GRASS_BLOCK.defaultBlockState() : BuildingBlocks.STONE);
            for (int y = floor + 1; y <= floor + 9; y++) put(c, x, y, z, BuildingBlocks.AIR);
            boolean edge = dx == 0 || dz == 0 || dx == lot.width() - 1 || dz == lot.depth() - 1;
            if ("PASTURE".equals(family) && edge) {
                boolean gate = dx == lot.width() / 2 && dz == lot.depth() - 1;
                put(c, x, floor + 1, z, gate ? Blocks.OAK_FENCE_GATE.defaultBlockState()
                        : Blocks.OAK_FENCE.defaultBlockState());
            }
            if ("GARDEN".equals(family) && !edge) {
                if ((dx % 5) == 0) put(c, x, floor, z, Blocks.WATER.defaultBlockState());
                else {
                    put(c, x, floor, z, Blocks.FARMLAND.defaultBlockState()
                            .setValue(BlockStateProperties.MOISTURE, 7));
                    put(c, x, floor + 1, z, Blocks.WHEAT.defaultBlockState());
                }
            }
            if ("COURTYARD".equals(family)) put(c, x, floor, z,
                    Blocks.STONE_BRICKS.defaultBlockState());
            int cx = lot.width() / 2, cz = lot.depth() / 2;
            if ("WELL".equals(family) && Math.abs(dx - cx) <= 1 && Math.abs(dz - cz) <= 1) {
                put(c, x, floor, z, Blocks.STONE_BRICKS.defaultBlockState());
                if (dx == cx && dz == cz) put(c, x, floor + 1, z, Blocks.WATER.defaultBlockState());
                else put(c, x, floor + 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
            }
            if ("MONUMENT".equals(family) && dx == cx && dz == cz) {
                for (int y = 1; y <= 6; y++) put(c, x, floor + y, z,
                        Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            }
            if ("RUIN".equals(family) && edge && ((dx * 17 + dz * 31) % 9) < 6) {
                for (int y = 1; y <= 2 + ((dx * 7 + dz * 13) & 3); y++)
                    put(c, x, floor + y, z, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
            }
            if ("COURTYARD".equals(family) && dx == cx && dz == cz)
                put(c, x, floor + 1, z, Blocks.BELL.defaultBlockState());
            if ("RITUAL_GROUND".equals(family) && dx == cx && dz == cz)
                put(c, x, floor + 1, z, Blocks.CAMPFIRE.defaultBlockState());
        }
    }

    private static void buildShelter(ChunkAccess c, Lot lot, int floor, Template t) {
        int x0 = Math.max(lot.x(), c.getPos().x() << 4);
        int x1 = Math.min(lot.x() + lot.width() - 1, (c.getPos().x() << 4) + 15);
        int z0 = Math.max(lot.z(), c.getPos().z() << 4);
        int z1 = Math.min(lot.z() + lot.depth() - 1, (c.getPos().z() << 4) + 15);
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) {
            int dx = x - lot.x(), dz = z - lot.z();
            clearHazards(c, x, z, floor);
            for (int y = floor - 3; y <= floor; y++) put(c, x, y, z,
                    y == floor ? Blocks.COARSE_DIRT.defaultBlockState() : BuildingBlocks.STONE);
            for (int y = floor + 1; y <= floor + 10; y++) put(c, x, y, z, BuildingBlocks.AIR);
            boolean corner = (dx == 0 || dx == lot.width() - 1)
                    && (dz == 0 || dz == lot.depth() - 1);
            if (corner) for (int y = floor + 1; y <= floor + 4; y++)
                put(c, x, y, z, Blocks.OAK_FENCE.defaultBlockState());
            int slope = Math.min(3, Math.min(dx, lot.width() - 1 - dx));
            put(c, x, floor + 5 + slope, z, Blocks.WOOL.brown().defaultBlockState());
        }
        int middleX = lot.x() + lot.width() / 2;
        int middleZ = lot.z() + lot.depth() / 2;
        put(c, middleX, floor + 1, middleZ, Blocks.CAMPFIRE.defaultBlockState());
        put(c, middleX + 2, floor + 1, middleZ, Blocks.BARREL.defaultBlockState());
    }

    private static BlockState station(String type) {
        return switch (type) {
            case "FURNACE" -> Blocks.FURNACE.defaultBlockState();
            case "BLAST_FURNACE" -> Blocks.BLAST_FURNACE.defaultBlockState();
            case "SMOKER" -> Blocks.SMOKER.defaultBlockState();
            case "ANVIL" -> Blocks.ANVIL.defaultBlockState();
            case "GRINDSTONE" -> Blocks.GRINDSTONE.defaultBlockState();
            case "STONECUTTER", "SAW_STONE" -> Blocks.STONECUTTER.defaultBlockState();
            case "LOOM" -> Blocks.LOOM.defaultBlockState();
            case "COMPOSTER" -> Blocks.COMPOSTER.defaultBlockState();
            case "LECTERN" -> Blocks.LECTERN.defaultBlockState();
            case "BOOKSHELF" -> Blocks.BOOKSHELF.defaultBlockState();
            case "BREWING" -> Blocks.BREWING_STAND.defaultBlockState();
            case "SMITHING" -> Blocks.SMITHING_TABLE.defaultBlockState();
            case "CAULDRON" -> Blocks.CAULDRON.defaultBlockState();
            case "CHEST" -> Blocks.CHEST.defaultBlockState();
            case "HAY" -> Blocks.HAY_BLOCK.defaultBlockState();
            case "WATER" -> Blocks.WATER.defaultBlockState();
            case "CAMPFIRE" -> Blocks.CAMPFIRE.defaultBlockState();
            case "STONE" -> Blocks.STONE_BRICKS.defaultBlockState();
            case "CRAFTING" -> Blocks.CRAFTING_TABLE.defaultBlockState();
            default -> Blocks.BARREL.defaultBlockState();
        };
    }

    private static void clearHazards(ChunkAccess c, int x, int z, int floor) {
        for (int y = Math.max(c.getMinY(), floor - 18);
             y <= Math.min(c.getMaxY() - 1, floor + 25); y++) {
            BlockState state = c.getBlockState(new BlockPos(x, y, z));
            if (state.getFluidState().is(FluidTags.LAVA))
                put(c, x, y, z, y <= floor ? BuildingBlocks.STONE : BuildingBlocks.AIR);
            else if (y >= floor - 3 && (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)))
                put(c, x, y, z, BuildingBlocks.AIR);
        }
    }

    private static void put(ChunkAccess c, int x, int y, int z, BlockState value) {
        if (y < c.getMinY() || y >= c.getMaxY()) return;
        if ((x >> 4) != c.getPos().x() || (z >> 4) != c.getPos().z()) return;
        ServerLevel live = GALLERY_LEVEL.get();
        if (live == null) c.setBlockState(new BlockPos(x, y, z), value, 0);
        else live.setBlock(new BlockPos(x, y, z), value, 2);
    }
}

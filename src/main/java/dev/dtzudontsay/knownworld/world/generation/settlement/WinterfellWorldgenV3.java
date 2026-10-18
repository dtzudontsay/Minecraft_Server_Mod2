package dev.dtzudontsay.knownworld.world.generation.settlement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.util.concurrent.atomic.AtomicInteger;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Winterfell 298 AC settlement WORLDGEN overlay, schema 3.
 *
 * Runs during the KnownWorldChunkGenerator's biome-decoration pass, BEFORE
 * the newly generated chunk is exposed to clients. Never writes to a live
 * ServerLevel or alters already-generated chunks. Stable, independent of
 * player position, world tick, resident/NPC activation or traversal order.
 *
 * This implementation deliberately does not use a server tick or /fill.
 * Different buildings and their road links are indexed by their chunk.
 *
 * NOTE: Direct ChunkAccess blockstate placement is deliberate: vanilla doors,
 * beds, windows and block roofs receive exact states within the target chunk;
 * we do not try to build across chunks with off-thread ServerLevel.setBlock.
 */
public final class WinterfellWorldgenV3 {
    public static final boolean ENABLED = true;

    private static final String SOURCE =
            "/data/knownworld/case_study/winterfell_v3_worldgen.json";
    private static final int EXPECTED_SCHEMA = 3;
    private static final int X = -4_050_173;
    private static final int Z = -1_976_496;

    private static volatile Plan shared;
    private static final AtomicInteger chunksDecoratedThisSession = new AtomicInteger();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();

    private record Building(String id, String template, int x, int z,
                            int width, int depth, int capacity) { }
    private record ChunkPlan(BitSet roads, int[] buildings) { }
    private record Plan(Map<Long, ChunkPlan> chunks, List<Building> buildings,
                        String version) { }

    private WinterfellWorldgenV3() { }

    public static void registerDebugCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("kwinterfell3")
                        .then(Commands.literal("status").executes(context -> {
                            Plan p = plan();
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "[Winterfell V3] Auto-worldgen " + (ENABLED ? "ON" : "OFF")
                                    + " at NW-029 X=" + X + " Z=" + Z
                                    + " | planned buildings=" + p.buildings.size()
                                    + " | indexed chunks=" + p.chunks.size()
                                    + " | chunks generated this session="
                                    + chunksDecoratedThisSession.get()
                                    + " | no construction command needed."), false);
                            return 1;
                        }))));
    }

    public static void decorate(WorldGenLevel level, ChunkAccess chunk) {
        if (!ENABLED) return;
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        // Avoid even loading the large atlas-town index in distant Essos chunks.
        int relX = (chunkX << 4) - X;
        int relZ = (chunkZ << 4) - Z;
        if (Math.abs(relX) > 4_500 || Math.abs(relZ) > 4_500) return;
        var p = plan();
        ChunkPlan cp = p.chunks.get(key(chunkX, chunkZ));
        if (cp == null) return;
        int wx = chunkX << 4;
        int wz = chunkZ << 4;

        // 1. Prepare streets on natural heightmap, clearing incidental foliage.
        // 3-block corridors are indexed ahead of time (including chunk edges).
        for (int index = cp.roads.nextSetBit(0); index >= 0;
             index = cp.roads.nextSetBit(index + 1)) {
            int x = wx + (index & 15);
            int z = wz + (index >>> 4);
            roadAt(chunk, x, z);
        }

        // 2. Homes/workplaces. Only fragments within the current chunk are
        // written, so no forced neighbour-chunk loads or partial live builds.
        for (int idx : cp.buildings) {
            Building b = p.buildings.get(idx);
            // Stark household belongs inside the major keep, rather than a
            // separate 11x12 generic cottage in the north-west castle ward.
            if ("castle_home_0001".equals(b.id)) continue;
            buildingInChunk(chunk, b, wx, wz);
        }

        // 3. Castle wall/tower/keep components, over roads and structures.
        // Real arches remain where a planned road crosses a wall.
        castleInChunk(chunk, wx, wz);
        chunksDecoratedThisSession.incrementAndGet();
    }

    private static Plan plan() {
        Plan ready = shared;
        if (ready != null) return ready;
        synchronized (WinterfellWorldgenV3.class) {
            if (shared != null) return shared;
            try (InputStream in = WinterfellWorldgenV3.class.getResourceAsStream(SOURCE)) {
                if (in == null) throw new IllegalStateException("Missing " + SOURCE
                        + "; run tools/geodata/build_winterfell_v3_worldgen.py");
                JsonObject root = JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.get("schema").getAsInt() != EXPECTED_SCHEMA
                        || root.get("anchorX").getAsInt() != X
                        || root.get("anchorZ").getAsInt() != Z
                        || root.get("population").getAsInt() != 5000
                        || root.get("households").getAsInt() != 1136) {
                    throw new IllegalStateException("Winterfell V3 index does not match NW-029/5000 census");
                }
                List<Building> buildings = new ArrayList<>();
                for (JsonElement e : root.getAsJsonArray("buildings")) {
                    JsonObject j = e.getAsJsonObject();
                    buildings.add(new Building(j.get("id").getAsString(),
                            j.get("t").getAsString(), j.get("x").getAsInt(),
                            j.get("z").getAsInt(), j.get("w").getAsInt(),
                            j.get("d").getAsInt(), j.get("cap").getAsInt()));
                }
                Map<Long, ChunkPlan> chunks = new HashMap<>();
                for (JsonElement e : root.getAsJsonArray("chunks")) {
                    JsonObject j = e.getAsJsonObject();
                    BitSet roads = new BitSet(256);
                    for (JsonElement n : j.getAsJsonArray("r")) roads.set(n.getAsInt());
                    JsonArray ids = j.getAsJsonArray("b");
                    int[] indices = new int[ids.size()];
                    for (int i = 0; i < indices.length; i++) {
                        indices[i] = ids.get(i).getAsInt();
                    }
                    chunks.put(key(j.get("x").getAsInt(),j.get("z").getAsInt()),
                            new ChunkPlan(roads, indices));
                }
                shared = new Plan(Map.copyOf(chunks), List.copyOf(buildings),
                        root.get("versionHash").getAsString());
                KnownWorld.LOGGER.info("Winterfell V3 chunk worldgen ready: {} chunks, {} plots; {}",
                        chunks.size(), buildings.size(), shared.version.substring(0, 12));
                return shared;
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot load Winterfell V3 worldgen index", failure);
            }
        }
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffff_ffffL);
    }

    /** Reproduce the same pre-decoration land height as KnownWorldChunkGenerator. */
    private static int naturalTopY(int x, int z) {
        double metres = GeographicDataManager.getInstance()
                .sample(WorldProjection.fromMinecraft(x + 0.5, z + 0.5))
                .elevationMetres();
        return Math.max(-63, Math.min(2030, (int) Math.round(metres)));
    }

    private static boolean naturalLand(int x, int z) {
        return KnownWorldGeoSampler.sampleMinecraft(x + 0.5, z + 0.5).land();
    }

    // Median filtering suppresses single-block bumps without a 16x16-chunk
    // boundary seam. The function depends only on immutable world geography.
    private static int smoothedRoadY(int x, int z) {
        int[] values = {
            naturalTopY(x, z), naturalTopY(x - 3, z),
            naturalTopY(x + 3, z), naturalTopY(x, z - 3),
            naturalTopY(x, z + 3), naturalTopY(x - 6, z),
            naturalTopY(x + 6, z), naturalTopY(x, z - 6),
            naturalTopY(x, z + 6)
        };
        java.util.Arrays.sort(values);
        return values[values.length / 2];
    }

    private static void roadAt(ChunkAccess chunk, int x, int z) {
        if (!naturalLand(x, z)) return; // Do not fill real mapped seas/rivers.
        int naturalY = naturalTopY(x,z);
        int y = smoothedRoadY(x,z);
        // Avoid creating massive aerial roads at cliff edges. Steep sections
        // need authored bridges and road-grade routing in a later pass.
        if (Math.abs(naturalY - y) > 4) y = naturalY;
        for (int yy = Math.min(naturalY, y) - 3; yy <= y; yy++) {
            block(chunk, x, yy, z, yy == y ? GRAVEL : COBBLE);
        }
        for (int yy = y + 1; yy <= Math.max(y + 9, naturalY + 9); yy++) {
            block(chunk, x, yy, z, AIR);
        }
    }

    private static int buildingFloor(Building b) {
        int x = X + b.x, z = Z + b.z;
        int[] vals = new int[9];
        int at = 0;
        for (int dx : new int[]{0,b.width / 2,b.width - 1}) {
            for (int dz : new int[]{0,b.depth / 2,b.depth - 1}) {
                vals[at++] = naturalTopY(x + dx,z + dz);
            }
        }
        java.util.Arrays.sort(vals);
        // High-middle percentile gives a useful terrace while limiting walls
        // of unnaturally tall fill on gently rolling terrain.
        return vals[6];
    }

    private static void buildingInChunk(ChunkAccess chunk, Building b, int chunkX0, int chunkZ0) {
        int x0=X+b.x, z0=Z+b.z;
        int x1=x0+b.width-1, z1=z0+b.depth-1;
        int floor = buildingFloor(b);
        boolean stone = b.template.equals("MERCHANT") || b.template.equals("GUARD_QUARTER")
                || b.template.equals("CASTLE_QUARTER");
        boolean workshop = b.template.equals("WORKSHOP");
        boolean farmhouse = b.template.equals("FARMHOUSE");
        int wallHeight = stone ? 9 : 6;
        BlockState wall = stone ? BRICKS : workshop ? Blocks.OAK_PLANKS.defaultBlockState()
                : Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState roof = stone ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                : Blocks.DARK_OAK_PLANKS.defaultBlockState();
        int minX = Math.max(x0,chunkX0), maxX = Math.min(x1,chunkX0+15);
        int minZ = Math.max(z0,chunkZ0), maxZ = Math.min(z1+6,chunkZ0+15);
        for (int z=minZ;z<=maxZ;z++) {
            for (int x=minX;x<=maxX;x++) {
                int xx=x-x0, zz=z-z0;
                if (z>z1) {
                    // Door approaches and stair-free ramp, at most one block per
                    // step. These access tiles are indexed into adjacent chunks.
                    if (Math.abs(x-(x0+b.width/2))>1) continue;
                    int step=z-z1;
                    int target=Math.max(smoothedRoadY(x,z),floor-Math.max(0,step-1));
                    int top=Math.min(floor, target);
                    for(int y=Math.max(-62,naturalTopY(x,z)-3);y<=top;y++)
                        block(chunk,x,y,z,y==top?COBBLE:STONE);
                    for(int y=top+1;y<=Math.max(top+2,naturalTopY(x,z)+2);y++)block(chunk,x,y,z,AIR);
                    continue;
                }
                int nativeY=naturalTopY(x,z);
                // Foundation with limited earth cut/fill to the proposed floor.
                for (int y=Math.min(nativeY,floor)-6;y<=floor;y++) {
                    block(chunk,x,y,z,y==floor?Blocks.SPRUCE_PLANKS.defaultBlockState():COBBLE);
                }
                int slope=Math.min(3,Math.min(xx,b.width-1-xx));
                int roofY=floor+wallHeight+1+slope;
                for(int y=floor+1;y<=Math.max(roofY+2,nativeY+10);y++)block(chunk,x,y,z,AIR);
                boolean perimeter=xx==0||xx==b.width-1||zz==0||zz==b.depth-1;
                if(perimeter)for(int y=floor+1;y<=floor+wallHeight;y++)block(chunk,x,y,z,wall);
                if((zz==0||zz==b.depth-1) && slope>0){
                    for(int y=floor+wallHeight+1;y<roofY;y++)block(chunk,x,y,z,wall);
                }
                block(chunk,x,roofY,z,roof);
                // Second floor in higher-status houses, without sealing stairs
                // off entirely. Ladder/vertical circulation is future template work.
                if(stone && floor+5<roofY && xx>0 && xx<b.width-1
                        && zz>0 && zz<b.depth-1 && !(xx<=2 && zz<=3)) {
                    block(chunk,x,floor+5,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
                }
                if(perimeter && xx==b.width/2 && zz==b.depth-1) {
                    BlockState door=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(
                            DoorBlock.FACING,Direction.SOUTH);
                    block(chunk,x,floor+1,z,door.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER));
                    block(chunk,x,floor+2,z,door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER));
                } else if(perimeter && ((zz==0||zz==b.depth-1) && (xx==2||xx==b.width-3))
                        && xx!=b.width/2) {
                    block(chunk,x,floor+3,z,Blocks.GLASS_PANE.defaultBlockState());
                    if(stone)block(chunk,x,floor+7,z,Blocks.GLASS_PANE.defaultBlockState());
                }
                if(xx>0 && xx<b.width-1 && zz>0 && zz<b.depth-1 && b.capacity>0) {
                    // Foot and head are independently determined from this
                    // block coordinate. They work even if a bed spans chunks.
                    int ix=xx-2;
                    if(ix>=0 && ix%3==0) {
                        int bedsPerRow=(b.width-4+2)/3;
                        for(int footZ=2;footZ+1<b.depth-1;footZ+=3) {
                            int slot=((footZ-2)/3)*bedsPerRow+ix/3;
                            if(slot>=b.capacity)continue;
                            if(zz==footZ || zz==footZ+1) {
                                BlockState bed=Blocks.BED.red().defaultBlockState()
                                        .setValue(BedBlock.FACING,Direction.SOUTH);
                                block(chunk,x,floor+1,z,bed.setValue(BedBlock.PART,
                                        zz==footZ ? BedPart.FOOT : BedPart.HEAD));
                            }
                        }
                    }
                }
                if(xx==3 && zz==b.depth-2) {
                    block(chunk,x,floor+1,z, workshop?Blocks.FURNACE.defaultBlockState()
                            :farmhouse?Blocks.COMPOSTER.defaultBlockState()
                            :stone?Blocks.BARREL.defaultBlockState()
                            :Blocks.CRAFTING_TABLE.defaultBlockState());
                }
            }
        }
    }

    private static void castleInChunk(ChunkAccess chunk,int chunkX0,int chunkZ0) {
        // Small custom component grammar rather than a single dark rectangular
        // prism. Two thick curtain rings, arched gatehouses, towers, multistorey
        // central keep, Great Hall, roof slopes and open courtyards.
        if(chunkX0>X+182 || chunkX0+15<X-182 || chunkZ0>Z+182 || chunkZ0+15<Z-182)return;
        int courtyard=naturalTopY(X,Z);
        for(int z=chunkZ0;z<chunkZ0+16;z++)for(int x=chunkX0;x<chunkX0+16;x++) {
            int rx=x-X,rz=z-Z;
            int arx=Math.abs(rx),arz=Math.abs(rz);
            if(arx>181||arz>181)continue;
            // Walls lie on inside edge of the ring. Outermost ring six blocks
            // thick. Inner ring four blocks. No unexplained open wall holes.
            boolean outer=Math.max(arx,arz)>=166 && Math.max(arx,arz)<=171;
            boolean inner=Math.max(arx,arz)>=146 && Math.max(arx,arz)<=149;
            if(outer||inner){
                int height=outer?23:29;
                int wallFoot=naturalTopY(x,z);
                int gateWidth=outer?10:7;
                boolean eastGate=rx>0&&arx>=146&&(
                        (arz>=15-gateWidth&&arz<=15+gateWidth)
                        || (inner&&((rz>=88&&rz<=100)||(rz>=113&&rz<=124)||(rz>=136&&rz<=148))));
                for(int y=wallFoot-5;y<=wallFoot+height;y++) {
                    if(eastGate && y>=wallFoot+1 && y<wallFoot+8) {
                        block(chunk,x,y,z,AIR);
                    } else {
                        block(chunk,x,y,z,BRICKS);
                    }
                }
                if((rz%5==0||rx%5==0))block(chunk,x,wallFoot+height+1,z,BRICKS);
            }
            // Defensive circular towers at corners and along curtain walls.
            tower(chunk,x,z,rx,rz,171,171,9,courtyard,37);
            tower(chunk,x,z,rx,rz,-171,171,9,courtyard,37);
            tower(chunk,x,z,rx,rz,171,-171,9,courtyard,37);
            tower(chunk,x,z,rx,rz,-171,-171,9,courtyard,37);
            for(int d=-108;d<=108;d+=108){
                tower(chunk,x,z,rx,rz,171,d,7,courtyard,33);
                tower(chunk,x,z,rx,rz,-171,d,7,courtyard,33);
                tower(chunk,x,z,rx,rz,d,171,7,courtyard,33);
                tower(chunk,x,z,rx,rz,d,-171,7,courtyard,33);
            }
            // The Stark residence / main keep. Larger, pitched and multi-level,
            // with round corner towers, windows and a south-facing grand arch.
            keep(chunk,x,z,rx,rz,courtyard);
            // Entrance courtyard walkway from interior west-south passage to keep.
            if(rx>=16&&rx<=18 && rz>=46&&rz<=58){
                int pathY=courtyard+Math.min(2,Math.max(0,rz-55));
                for(int y=courtyard-2;y<=pathY;y++)block(chunk,x,y,z,GRAVEL);
                for(int y=pathY+1;y<pathY+5;y++)block(chunk,x,y,z,AIR);
            }
        }
    }

    private static void tower(ChunkAccess c,int x,int z,int rx,int rz,
                              int tx,int tz,int rad,int baseline,int height) {
        int dx=rx-tx,dz=rz-tz,d2=dx*dx+dz*dz;
        if(d2>rad*rad)return;
        int foundation=naturalTopY(x,z);
        int foot=Math.max(baseline-2,Math.min(baseline+5,foundation));
        for(int y=foot-5;y<=foot+height;y++) {
            if(y>foot+2 && y<foot+height-1 && d2<(rad-2)*(rad-2))
                block(c,x,y,z,AIR);
            else block(c,x,y,z,BRICKS);
        }
        if(dx==0 || dz==0)block(c,x,foot+height+1,z,BRICKS);
    }

    private static void keep(ChunkAccess c,int x,int z,int rx,int rz,int centerGround) {
        if(rx < -48 || rx > 48 || rz < 54 || rz > 125)return;
        int floor=centerGround+2;
        int borderX=Math.min(rx+44,44-rx);
        int borderZ=Math.min(rz-58,121-rz);
        boolean body=rx>=-44&&rx<=44&&rz>=58&&rz<=121;
        if(body){
            for(int y=Math.min(naturalTopY(x,z),floor)-8;y<=floor;y++)block(c,x,y,z,BRICKS);
            int inset=Math.min(9,Math.min(rx+44,44-rx)/4);
            int roofY=floor+23+inset;
            for(int y=floor+1;y<=roofY+3;y++)block(c,x,y,z,AIR);
            boolean outer=borderX<3||borderZ<3;
            if(outer)for(int y=floor+1;y<=floor+22;y++)block(c,x,y,z,BRICKS);
            for(int y:new int[]{8,16})if(y<roofY && !outer && !(rx>=14&&rx<=19&&rz>=58&&rz<=69))
                block(c,x,floor+y,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
            if(borderZ<3&&inset>0)for(int y=floor+23;y<roofY;y++)block(c,x,y,z,BRICKS);
            block(c,x,roofY,z,Blocks.DEEPSLATE_TILES.defaultBlockState());
            // Two-storey narrow windows spaced on all four walls.
            if(outer && ((rz==58||rz==121)&&rx%11==0 ||
                         (rx==-44||rx==44)&&rz%11==0)){
                for(int y:new int[]{floor+5,floor+15})block(c,x,y,z,Blocks.GLASS_PANE.defaultBlockState());
            }
            if(rz==58&&rx>=15&&rx<=19){
                for(int y=floor+1;y<=floor+7;y++)block(c,x,y,z,AIR);
            }
        }
        for(int tx:new int[]{-44,44})for(int tz:new int[]{58,121})
            tower(c,x,z,rx,rz,tx,tz,6,centerGround,37);
    }

    private static void block(ChunkAccess chunk,int x,int y,int z,BlockState block) {
        if(y<chunk.getMinY() || y>=chunk.getMaxY())return;
        int cx=chunk.getPos().x,cz=chunk.getPos().z;
        if((x>>4)!=cx||(z>>4)!=cz)return;
        chunk.setBlockState(new BlockPos(x,y,z),block,0);
    }
}

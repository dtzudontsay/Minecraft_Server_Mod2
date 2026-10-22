package dev.dtzudontsay.knownworld.world.generation.settlement;

import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2.Lot;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * C4.7 deterministic, chunk-local architectural detail layer.
 * No Level access, block entities, mobs, delayed work, or neighbouring chunks.
 * All coordinates stay in the lot footprint, including basement features.
 * The C3 put sink supplies exactly the same blocks to worldgen and gallery.
 */
public final class KnownWorldC47ArchitecturalDetails {
    private KnownWorldC47ArchitecturalDetails() { }

    public static BlockState wall(String id, BlockState regional) {
        if (has(id, "ADMINISTRATION","BANK","JUSTICE_HALL","TEMPLE","GREAT_HALL","KEEP",
                "GUARD_TOWER","SCHOOL","SCHOLASTIC_HALL","PUBLIC_BATH","ARCANE_STUDY",
                "CITY_SERVICES","RITUAL_GROUND","FIGHTING_PIT")) return Blocks.STONE_BRICKS.defaultBlockState();
        if (has(id, "BRICKWORKS","SMITHY","ARMORY","BREWERY","MILL","MASONRY",
                "POTTERY","CANAL_WORKS","MINE")) return Blocks.BRICKS.defaultBlockState();
        if (has(id,"SHIPYARD","DOCK","FERRY_LANDING","CART_DEPOT","STABLE","WAREHOUSE",
                "FARM","GRANARY")) return Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        if (has(id,"INN","TAVERN","ENTERTAINMENT_HALL","BROTHEL"))
            return Blocks.SPRUCE_PLANKS.defaultBlockState();
        if (has(id,"CHANDLERY","BOOKBINDERY","JEWELER","MARKET","WEAVERY"))
            return Blocks.TERRACOTTA.defaultBlockState();
        return regional;
    }

    public static BlockState roof(String id, BlockState regional) {
        if (has(id,"ADMINISTRATION","BANK","JUSTICE_HALL","TEMPLE","GREAT_HALL","KEEP","ARCANE_STUDY"))
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        if (has(id,"SMITHY","ARMORY","BRICKWORKS","BREWERY","MILL","MINE","CANAL_WORKS"))
            return Blocks.STONE_BRICKS.defaultBlockState();
        if (has(id,"SHIPYARD","CART_DEPOT","STABLE","WAREHOUSE","FARM"))
            return Blocks.SPRUCE_PLANKS.defaultBlockState();
        if (has(id,"TAVERN","INN","BROTHEL","ENTERTAINMENT_HALL"))
            return Blocks.DARK_OAK_PLANKS.defaultBlockState();
        return regional;
    }

    public static boolean flatRoof(String id) {
        return has(id,"WAREHOUSE","BANK","MARKET","BREWERY","BRICKWORKS",
                "PUBLIC_BATH","CANAL_WORKS","CART_DEPOT");
    }

    public static int roofRise(String id,int x,int z,int width,int depth,boolean flat) {
        if (flat) return 0;
        int ridge = has(id,"TEMPLE","JUSTICE_HALL","GREAT_HALL","ADMINISTRATION","INN",
                "TAVERN","SHIPYARD","STABLE","FOREST_CAMP")
                ? Math.min(z,depth-1-z) : Math.min(x,width-1-x);
        int cap = has(id,"TEMPLE","ADMINISTRATION","JUSTICE_HALL","BANK","GREAT_HALL")?5:3;
        return Math.max(0,Math.min(cap,ridge));
    }

    public static boolean gable(String id,int x,int z,int width,int depth) {
        return has(id,"TEMPLE","JUSTICE_HALL","GREAT_HALL","ADMINISTRATION","INN",
                "TAVERN","SHIPYARD","STABLE","FOREST_CAMP")
                ? (x==0 || x==width-1) : (z==0 || z==depth-1);
    }

    private static boolean has(String id,String... values) {
        for (String value:values) if(value.equals(id))return true;
        return false;
    }

    private record Canvas(ChunkAccess chunk, Lot lot, int floor) {
        int w(){return lot.width();}
        int d(){return lot.depth();}
        void b(int x,int y,int z,BlockState block){
            if(x<0 || x>=w() || z<0 || z>=d() || y< -8 || y>27)return;
            KnownWorldC3Architecture.c47Put(chunk,lot.x()+x,floor+y,lot.z()+z,block);
        }
        void fill(int x0,int y0,int z0,int x1,int y1,int z1,BlockState block) {
            for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)for(int y=y0;y<=y1;y++)b(x,y,z,block);
        }
        void lamp(int x,int z,int storey){
            int y=1+storey*6;
            b(x,y,z,Blocks.SPRUCE_FENCE.defaultBlockState());
            b(x,y+1,z,Blocks.LANTERN.defaultBlockState());
        }
        void table(int x,int z,int y){
            b(x,y,z,Blocks.SPRUCE_FENCE.defaultBlockState());
            b(x,y+1,z,Blocks.SPRUCE_SLAB.defaultBlockState());
        }
        void bench(int x,int z,int y){
            b(x,y,z,Blocks.SPRUCE_STAIRS.defaultBlockState());
        }
        void wallBand(BlockState band,int y){
            for(int x=0;x<w();x++) {b(x,y,0,band);b(x,y,d()-1,band);}
            for(int z=0;z<d();z++) {b(0,y,z,band);b(w()-1,y,z,band);}
        }
        void crate(int x,int z){
            b(x,1,z,Blocks.BARREL.defaultBlockState());
            b(x,2,z,Blocks.CHEST.defaultBlockState());
        }
        void plinth(int x,int z,int height){
            b(x,1,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
            for(int y=2;y<=height;y++)b(x,y,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
            b(x,height+1,z,Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
    }

    public static void decorate(ChunkAccess chunk,Lot lot,int floor,
                                KnownWorldC3Architecture.Template t,String palette) {
        Canvas c=new Canvas(chunk,lot,floor);
        String id=t.id();
        // Baseline requirements for every enclosed template: floor-by-floor
        // light, usable circulation, role-colored facades, and work furniture.
        if(!t.openAir() && !"SHELTER".equals(t.family())) {
            facade(c,t);
            int n=Math.max(1,t.floors());
            for(int storey=0;storey<n;storey++){
                int y=1+storey*6;
                if(c.w()>=11 && c.d()>=10){
                    c.lamp(c.w()-3,3,storey);
                    c.lamp(c.w()-3,c.d()-4,storey);
                    if(c.w()>=19 || c.d()>=18) c.lamp(3,c.d()-4,storey);
                    // Furnished upper rooms instead of empty lofts.
                    if(storey>0){
                        c.table(Math.min(5,c.w()-4),Math.min(4,c.d()-4),y);
                        c.bench(Math.min(6,c.w()-4),Math.min(4,c.d()-4),y);
                        c.b(3,y,c.d()-3,Blocks.BOOKSHELF.defaultBlockState());
                    }
                }
            }
            if(has(id,"HOUSE_SMALL","HOUSE_MEDIUM","HOUSE_URBAN")) {
                c.table(4,3,1);
                c.b(3,1,c.d()-3,Blocks.CHEST.defaultBlockState());
            } else if (t.stations().size()>0 && c.w()>10) {
                c.crate(c.w()-4,c.d()/2);
            }
        }
        switch(id) {
            case "ADMINISTRATION","CITY_SERVICES" -> council(c,id);
            case "BANK" -> bank(c);
            case "JUSTICE_HALL" -> justice(c);
            case "TEMPLE" -> temple(c);
            case "GREAT_HALL" -> greatHall(c);
            case "ARCANE_STUDY" -> arcane(c);
            case "SCHOLASTIC_HALL","SCHOOL","BOOKBINDERY" -> study(c);
            case "SHIPYARD" -> shipyard(c);
            case "DOCK","FERRY_LANDING" -> wharf(c);
            case "SMITHY","ARMORY","BRICKWORKS","MASONRY","POTTERY" -> forge(c,id);
            case "BARRACKS" -> barracks(c);
            case "STABLE" -> stable(c);
            case "TAVERN","INN","SERVICE_HALL" -> tavern(c,id);
            case "BROTHEL" -> privateRooms(c);
            case "PUBLIC_BATH" -> bath(c);
            case "MINE","CANAL_WORKS" -> shaft(c,id);
            case "MARKET" -> market(c);
            case "MILL" -> mill(c);
            case "FIGHTING_PIT" -> arena(c);
            case "FOREST_CAMP" -> logs(c);
            case "COURTYARD" -> courtyard(c);
            case "CHANDLERY" -> candles(c);
            case "CART_DEPOT" -> cart(c);
            case "BREWERY" -> brewery(c);
            case "BUTCHERY" -> butcher(c);
            case "HEALER" -> healer(c);
            case "FARM","GRANARY","PASTURE" -> rural(c,id);
            case "FISHERY" -> fishery(c);
            case "WAREHOUSE" -> warehouse(c);
            case "WEAVERY" -> weavery(c);
            case "BAKERY","KITCHEN" -> kitchen(c);
            case "JEWELER" -> jeweler(c);
            case "LEATHERWORKS" -> leather(c);
            case "ENTERTAINMENT_HALL" -> entertainment(c);
            case "GUARD_TOWER","KEEP" -> defenses(c);
            case "GARDEN","WELL","MONUMENT","RITUAL_GROUND","RUIN" -> outdoor(c,id);
            case "CAMP","TEMPORARY_CAMP" -> camp(c);
            default -> { // Every other trade receives more than generic stations.
                if(!t.openAir() && !"SHELTER".equals(t.family())) trade(c,id);
            }
        }
    }

    private static void facade(Canvas c,KnownWorldC3Architecture.Template t){
        String id=t.id();
        BlockState trim=has(id,"ADMINISTRATION","BANK","JUSTICE_HALL","TEMPLE","GREAT_HALL","ARCANE_STUDY")
            ?Blocks.POLISHED_ANDESITE.defaultBlockState():
              has(id,"SMITHY","ARMORY","BRICKWORKS","BREWERY","MINE")
              ?Blocks.STONE_BRICKS.defaultBlockState():Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        for(int y=1;y<=Math.min(6,t.floors()*6);y++) {
            c.b(0,y,0,trim);c.b(c.w()-1,y,0,trim);
            c.b(0,y,c.d()-1,trim);c.b(c.w()-1,y,c.d()-1,trim);
        }
        for(int y=6;y<=t.floors()*6;y+=6) c.wallBand(trim,y);
        // Reinforced portal columns flanking the C3 south entrance.
        if(c.w()>12){
            int m=c.w()/2;
            int frontZ="NORTH".equals(c.lot.face())?0:c.d()-1;
            for(int y=1;y<=4;y++) {
                c.b(m-2,y,frontZ,trim);
                c.b(m+2,y,frontZ,trim);
            }
            // Recessed civic portico for large institutional buildings.
            if(c.w()>16 && has(id,"ADMINISTRATION","BANK","JUSTICE_HALL","TEMPLE","GREAT_HALL")) {
                int innerZ="NORTH".equals(c.lot.face())?1:c.d()-2;
                for(int dx:new int[]{-6,-3,3,6})for(int y=1;y<=5;y++)
                    c.b(m+dx,y,innerZ,Blocks.POLISHED_ANDESITE.defaultBlockState());
                for(int dx=-6;dx<=6;dx++)c.b(m+dx,6,innerZ,Blocks.STONE_BRICK_SLAB.defaultBlockState());
            }
        }
        // Raised workshop chimneys, unlike ordinary cottage rooflines.
        if(has(id,"SMITHY","BREWERY","BRICKWORKS","BAKERY","KITCHEN","POTTERY","MILL")) {
            int height=Math.min(16,t.floors()*6+6);
            for(int y=1;y<=height;y++)c.b(c.w()-3,y,3,Blocks.BRICKS.defaultBlockState());
            c.b(c.w()-3,height+1,3,Blocks.CAMPFIRE.defaultBlockState());
        }
    }

    private static void council(Canvas c,String id){
        c.fill(3,1,5,c.w()-4,1,5,Blocks.SPRUCE_SLAB.defaultBlockState());
        for(int x=4;x<c.w()-3;x+=3){c.bench(x,7,1);c.b(x,1,3,Blocks.LECTERN.defaultBlockState());}
        c.plinth(c.w()/2,2,3);
        if("ADMINISTRATION".equals(id))c.b(c.w()/2,4,2,Blocks.BELL.defaultBlockState());
    }
    private static void bank(Canvas c){
        c.fill(3,1,6,c.w()-4,2,6,Blocks.POLISHED_ANDESITE.defaultBlockState());
        for(int x=4;x<c.w()-4;x+=3)c.b(x,3,6,Blocks.IRON_BARS.defaultBlockState());
        c.fill(3,1,2,5,3,2,Blocks.IRON_BARS.defaultBlockState());
        c.b(4,1,3,Blocks.CHEST.defaultBlockState());
        c.b(5,1,3,Blocks.CHEST.defaultBlockState());
    }
    private static void justice(Canvas c){
        c.fill(3,1,3,c.w()-4,1,3,Blocks.STONE_BRICK_SLAB.defaultBlockState());
        c.b(c.w()/2,2,3,Blocks.LECTERN.defaultBlockState());
        for(int z=6;z<c.d()-3;z+=3)for(int x=4;x<c.w()-4;x+=4)c.bench(x,z,1);
        c.plinth(c.w()-4,3,3);
    }
    private static void temple(Canvas c){
        int mid=c.w()/2;
        c.fill(mid-2,1,3,mid+2,1,6,Blocks.POLISHED_ANDESITE.defaultBlockState());
        c.plinth(mid,4,4);c.b(mid,6,4,Blocks.GLOWSTONE.defaultBlockState());
        for(int z=9;z<c.d()-2;z+=3)for(int x=3;x<c.w()-3;x+=4){
            if(Math.abs(x-mid)<2)continue;
            c.bench(x,z,1);
        }
        c.b(mid-3,1,5,Blocks.CANDLE.defaultBlockState());
        c.b(mid+3,1,5,Blocks.CANDLE.defaultBlockState());
    }
    private static void greatHall(Canvas c){
        for(int x=5;x<c.w()-5;x+=6)for(int z=7;z<c.d()-6;z+=5){
            c.table(x,z,1);c.bench(x+1,z,1);c.bench(x-1,z,1);
        }
        c.fill(c.w()/2-5,1,3,c.w()/2+5,1,4,Blocks.STONE_BRICKS.defaultBlockState());
        c.b(c.w()/2,2,3,Blocks.LECTERN.defaultBlockState());
    }
    private static void arcane(Canvas c){
        c.b(c.w()/2,1,5,Blocks.ENCHANTING_TABLE.defaultBlockState());
        for(int z=3;z<=7;z++)for(int x=3;x<c.w()-3;x++){
            if((x==3||x==c.w()-4||z==3||z==7)&&(x+z)%2==0)
                c.b(x,1,z,Blocks.BOOKSHELF.defaultBlockState());
        }
        c.b(c.w()-4,1,3,Blocks.BREWING_STAND.defaultBlockState());
        c.b(c.w()-4,1,c.d()-4,Blocks.CAULDRON.defaultBlockState());
    }
    private static void study(Canvas c){
        for(int x=4;x<c.w()-3;x+=4){c.table(x,6,1);c.bench(x+1,6,1);}
        for(int x=3;x<c.w()-3;x+=3)c.b(x,1,2,Blocks.BOOKSHELF.defaultBlockState());
        c.b(c.w()/2,1,3,Blocks.LECTERN.defaultBlockState());
    }
    private static void shipyard(Canvas c){
        int mid=c.w()/2;
        // Broad seaward aperture; roof remains as a weather shelter.
        c.fill(mid-4,1,c.d()-1,mid+4,5,c.d()-1,Blocks.AIR.defaultBlockState());
        c.b(mid-5,1,c.d()-1,Blocks.SPRUCE_LOG.defaultBlockState());
        c.b(mid+5,1,c.d()-1,Blocks.SPRUCE_LOG.defaultBlockState());
        // Half-built boat: keel, raised ribs, and open hull in the slip.
        for(int z=6;z<c.d()-3;z++){
            c.b(mid,1,z,Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
            if(z%2==0) for(int w=1;w<=3;w++){
                c.b(mid-w,1,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
                c.b(mid+w,1,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
                c.b(mid-w,2,z,Blocks.OAK_FENCE.defaultBlockState());
                c.b(mid+w,2,z,Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        c.b(4,1,5,Blocks.CRAFTING_TABLE.defaultBlockState());
        c.b(c.w()-4,1,5,Blocks.BARREL.defaultBlockState());
    }
    private static void wharf(Canvas c){
        // Pier-like loading platform within allocated footprint only.
        c.fill(3,1,5,c.w()-4,1,7,Blocks.SPRUCE_PLANKS.defaultBlockState());
        for(int x=3;x<c.w()-3;x+=3){c.b(x,2,5,Blocks.OAK_FENCE.defaultBlockState());}
        c.b(c.w()-4,1,4,Blocks.BARREL.defaultBlockState());
    }
    private static void forge(Canvas c,String id){
        int mid=c.w()/2;
        c.fill(mid-2,1,4,mid+2,1,4,Blocks.BRICKS.defaultBlockState());
        c.b(mid,2,4,Blocks.BLAST_FURNACE.defaultBlockState());
        c.b(mid+2,1,6,Blocks.ANVIL.defaultBlockState());
        c.b(mid-2,1,6,Blocks.CAULDRON.defaultBlockState());
        // No exposed lava or magma around NPC paths.
        c.b(mid,3,4,Blocks.GLOWSTONE.defaultBlockState());
        if("BRICKWORKS".equals(id)) {
            c.fill(3,1,c.d()-4,6,2,c.d()-4,Blocks.BRICKS.defaultBlockState());
            c.fill(4,1,3,7,1,3,Blocks.CLAY.defaultBlockState());
        } else if("ARMORY".equals(id)) {
            c.b(3,1,7,Blocks.TARGET.defaultBlockState());
            for(int z=3;z<c.d()-3;z+=3)c.b(c.w()-3,1,z,Blocks.IRON_BARS.defaultBlockState());
        } else if("POTTERY".equals(id))c.fill(3,1,8,7,1,8,Blocks.TERRACOTTA.defaultBlockState());
        else if("MASONRY".equals(id))c.b(4,1,8,Blocks.STONECUTTER.defaultBlockState());
    }
    private static void barracks(Canvas c){
        for(int x=4;x<c.w()-4;x+=4){
            c.b(x,1,3,Blocks.TARGET.defaultBlockState());
            c.b(x,2,3,Blocks.TARGET.defaultBlockState());
            c.b(x,1,6,Blocks.SMITHING_TABLE.defaultBlockState());
            c.b(x,2,6,Blocks.IRON_BARS.defaultBlockState());
        }
    }
    private static void stable(Canvas c){
        int middle=c.w()/2;
        // Stalls on both sides, central passage kept clear.
        for(int x=3;x<c.w()-3;x+=4){
            for(int z=3;z<c.d()-3;z++)c.b(x,1,z,Blocks.OAK_FENCE.defaultBlockState());
            c.b(x+1,1,3,Blocks.HAY_BLOCK.defaultBlockState());
            c.b(x+1,1,5,Blocks.CAULDRON.defaultBlockState());
        }
        c.fill(middle-1,1,7,middle+1,2,7,Blocks.AIR.defaultBlockState());
    }
    private static void tavern(Canvas c,String id){
        for(int x=4;x<c.w()-3;x+=4){
            c.table(x,5,1);c.bench(x-1,5,1);c.bench(x+1,5,1);
        }
        c.fill(3,1,2,c.w()-4,1,2,Blocks.SPRUCE_SLAB.defaultBlockState());
        for(int x=4;x<c.w()-4;x+=3)c.b(x,2,2,Blocks.BARREL.defaultBlockState());
        if("INN".equals(id)) for(int x=4;x<c.w()-4;x+=4)
            c.b(x,7,c.d()-3,Blocks.WOOL.white().defaultBlockState());
    }
    private static void privateRooms(Canvas c){
        for(int x=5;x<c.w()-4;x+=4){
            c.fill(x,1,3,x,3,c.d()-4,Blocks.SPRUCE_PLANKS.defaultBlockState());
            BlockState bed=Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            c.b(x+1,1,5,bed.setValue(BedBlock.PART,BedPart.FOOT));
            c.b(x+1,1,6,bed.setValue(BedBlock.PART,BedPart.HEAD));
        }
        c.table(3,4,1);
    }
    private static void bath(Canvas c){
        int minX=4,maxX=c.w()-5,minZ=5,maxZ=c.d()-5;
        if(maxX-minX<4 || maxZ-minZ<4)return;
        // Entirely within reinforced foundations; no flowing exterior water.
        c.fill(minX,-2,minZ,maxX,-2,maxZ,Blocks.STONE_BRICKS.defaultBlockState());
        c.fill(minX,-1,minZ,maxX,0,maxZ,Blocks.WATER.defaultBlockState());
        for(int x=minX-1;x<=maxX+1;x++){
            c.b(x,1,minZ-1,Blocks.STONE_BRICK_SLAB.defaultBlockState());
            c.b(x,1,maxZ+1,Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
        for(int z=minZ-1;z<=maxZ+1;z++){
            c.b(minX-1,1,z,Blocks.STONE_BRICK_SLAB.defaultBlockState());
            c.b(maxX+1,1,z,Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
        c.b(minX,0,minZ,Blocks.STONE_BRICK_STAIRS.defaultBlockState());
    }
    private static void shaft(Canvas c,String id){
        int cx=c.w()/2,cz=c.d()/2;
        c.fill(cx-1,-5,cz-1,cx+1,0,cz+1,Blocks.AIR.defaultBlockState());
        c.fill(cx-2,-5,cz-2,cx-2,0,cz+2,Blocks.STONE_BRICKS.defaultBlockState());
        for(int y=-4;y<=0;y++){
            c.b(cx-1,y,cz,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.EAST));
            c.b(cx-2,y,cz,Blocks.STONE_BRICKS.defaultBlockState());
        }
        c.b(cx+3,1,cz,Blocks.IRON_BARS.defaultBlockState());
        c.b(cx+3,1,cz+1,Blocks.BARREL.defaultBlockState());
        if("CANAL_WORKS".equals(id))c.fill(3,1,3,4,1,c.d()-4,Blocks.STONE_BRICKS.defaultBlockState());
    }
    private static void market(Canvas c){
        for(int x=3;x<c.w()-3;x+=5){
            for(int z=4;z<c.d()-3;z+=5){
                c.fill(x,1,z,x+2,1,z+1,Blocks.SPRUCE_SLAB.defaultBlockState());
                c.b(x,2,z,Blocks.BARREL.defaultBlockState());
                c.b(x+2,2,z,Blocks.HAY_BLOCK.defaultBlockState());
            }
        }
    }
    private static void mill(Canvas c){
        int x=c.w()-1,centerZ=c.d()/2,cy=5;
        for(int dz=-3;dz<=3;dz++)for(int dy=-3;dy<=3;dy++){
            int dist=dz*dz+dy*dy;
            if(dist>=6&&dist<=12)c.b(x,cy+dy,centerZ+dz,Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        for(int dz=-3;dz<=3;dz++)c.b(x,cy,centerZ+dz,Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        c.b(x-1,1,centerZ,Blocks.COMPOSTER.defaultBlockState());
        c.b(x-2,1,centerZ,Blocks.BARREL.defaultBlockState());
    }
    private static void arena(Canvas c){
        // A REAL open-roof fighting yard, with tiered stone seating.
        c.fill(0,7,0,c.w()-1,12,c.d()-1,Blocks.AIR.defaultBlockState());
        c.fill(2,0,2,c.w()-3,0,c.d()-3,Blocks.COARSE_DIRT.defaultBlockState());
        for(int depth=2;depth<=4;depth++){
            int y=5-depth;
            for(int x=depth;x<c.w()-depth;x++){
                c.b(x,y,depth,Blocks.STONE_BRICK_STAIRS.defaultBlockState());
                c.b(x,y,c.d()-depth-1,Blocks.STONE_BRICK_STAIRS.defaultBlockState());
            }
            for(int z=depth;z<c.d()-depth;z++){
                c.b(depth,y,z,Blocks.STONE_BRICK_STAIRS.defaultBlockState());
                c.b(c.w()-depth-1,y,z,Blocks.STONE_BRICK_STAIRS.defaultBlockState());
            }
        }
        c.fill(5,1,5,c.w()-6,1,5,Blocks.OAK_FENCE.defaultBlockState());
        c.b(c.w()/2,1,c.d()/2,Blocks.TARGET.defaultBlockState());
    }
    private static void logs(Canvas c){
        for(int x=3;x<c.w()-3;x+=3)for(int z=3;z<c.d()-3;z+=4){
            for(int run=0;run<3;run++)c.b(x,1,z+run,Blocks.SPRUCE_LOG.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));
        }
        c.b(c.w()/2,1,c.d()/2,Blocks.CRAFTING_TABLE.defaultBlockState());
    }
    private static void courtyard(Canvas c){
        int cx=c.w()/2,cz=c.d()/2;
        c.fill(cx-2,0,cz-2,cx+2,0,cz+2,Blocks.POLISHED_ANDESITE.defaultBlockState());
        c.plinth(cx,cz,5);
        for(int x=4;x<c.w()-3;x+=7){c.bench(x,4,1);c.bench(x,c.d()-5,1);}
        for(int z=4;z<c.d()-3;z+=7){c.bench(4,z,1);c.bench(c.w()-5,z,1);}
    }
    private static void candles(Canvas c){
        for(int x=3;x<c.w()-3;x+=3)for(int z=4;z<c.d()-3;z+=3){
            c.b(x,1,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
            c.b(x,2,z,Blocks.CANDLE.defaultBlockState());
        }
        c.b(c.w()-3,1,3,Blocks.CAULDRON.defaultBlockState());
    }
    private static void cart(Canvas c){
        int x=c.w()/2,z=c.d()/2;
        c.fill(x-2,1,z-2,x+2,1,z+2,Blocks.SPRUCE_SLAB.defaultBlockState());
        for(int dx:new int[]{-2,2})for(int dz:new int[]{-2,2})
            c.b(x+dx,0,z+dz,Blocks.DARK_OAK_LOG.defaultBlockState());
        c.fill(3,1,3,3,1,c.d()-4,Blocks.OAK_FENCE.defaultBlockState());
        c.b(4,1,4,Blocks.HAY_BLOCK.defaultBlockState());
    }
    private static void brewery(Canvas c){
        for(int x=5;x<c.w()-4;x+=5){
            c.fill(x,1,5,x+1,2,6,Blocks.IRON_BLOCK.defaultBlockState());
            c.b(x,3,5,Blocks.CAULDRON.defaultBlockState());
            c.b(x+1,3,6,Blocks.BREWING_STAND.defaultBlockState());
        }
        c.fill(4,1,c.d()-5,c.w()-5,1,c.d()-5,Blocks.BARREL.defaultBlockState());
    }
    private static void butcher(Canvas c){
        int z=c.d()-4;
        for(int x=3;x<c.w()-3;x++){c.b(x,1,z,Blocks.OAK_FENCE.defaultBlockState());}
        for(int zz=5;zz<z;zz++){c.b(3,1,zz,Blocks.OAK_FENCE.defaultBlockState());}
        c.b(4,1,5,Blocks.HAY_BLOCK.defaultBlockState());
        c.b(c.w()-4,1,5,Blocks.SMOKER.defaultBlockState());
    }
    private static void healer(Canvas c){
        c.fill(3,1,5,6,1,5,Blocks.WOOL.white().defaultBlockState());
        c.b(4,1,4,Blocks.CAULDRON.defaultBlockState());
        c.b(6,1,3,Blocks.BREWING_STAND.defaultBlockState());
        c.b(c.w()-4,1,4,Blocks.BOOKSHELF.defaultBlockState());
    }
    private static void rural(Canvas c,String id){
        if("PASTURE".equals(id)){c.b(c.w()/2,1,c.d()/2,Blocks.HAY_BLOCK.defaultBlockState());return;}
        for(int x=3;x<c.w()-3;x+=3){c.b(x,1,4,Blocks.HAY_BLOCK.defaultBlockState());}
        c.b(c.w()-4,1,4,Blocks.COMPOSTER.defaultBlockState());
        c.b(c.w()-4,1,6,Blocks.BARREL.defaultBlockState());
    }
    private static void fishery(Canvas c){
        c.fill(3,1,4,7,1,4,Blocks.SPRUCE_SLAB.defaultBlockState());
        for(int x=3;x<=7;x+=2)c.b(x,2,4,Blocks.CAULDRON.defaultBlockState());
        c.b(c.w()-4,1,5,Blocks.SMOKER.defaultBlockState());
    }
    private static void warehouse(Canvas c){
        for(int x=4;x<c.w()-3;x+=4)for(int z=4;z<c.d()-3;z+=4){c.crate(x,z);}
    }
    private static void weavery(Canvas c){
        for(int x=4;x<c.w()-3;x+=3){c.b(x,1,5,Blocks.LOOM.defaultBlockState());c.b(x,1,8,Blocks.WOOL.white().defaultBlockState());}
    }
    private static void kitchen(Canvas c){
        for(int x=4;x<c.w()-3;x+=4){c.b(x,1,4,Blocks.SMOKER.defaultBlockState());c.table(x,7,1);}
        c.b(3,1,3,Blocks.CAULDRON.defaultBlockState());
    }
    private static void jeweler(Canvas c){
        c.fill(4,1,4,c.w()-4,1,4,Blocks.POLISHED_ANDESITE.defaultBlockState());
        c.b(5,2,4,Blocks.AMETHYST_BLOCK.defaultBlockState());
        c.b(6,2,4,Blocks.GOLD_BLOCK.defaultBlockState());
    }
    private static void leather(Canvas c){
        c.b(4,1,4,Blocks.CAULDRON.defaultBlockState());
        for(int x=5;x<c.w()-3;x+=3)c.b(x,1,6,Blocks.WOOL.brown().defaultBlockState());
    }
    private static void entertainment(Canvas c){
        c.fill(4,1,3,c.w()-5,1,3,Blocks.SPRUCE_PLANKS.defaultBlockState());
        for(int x=4;x<c.w()-4;x+=3)c.b(x,2,3,Blocks.NOTE_BLOCK.defaultBlockState());
        for(int z=7;z<c.d()-3;z+=4)for(int x=5;x<c.w()-4;x+=4)c.bench(x,z,1);
    }
    private static void defenses(Canvas c){
        for(int x=3;x<c.w()-3;x+=4)c.b(x,1,3,Blocks.TARGET.defaultBlockState());
        c.b(c.w()/2,1,5,Blocks.SMITHING_TABLE.defaultBlockState());
        c.b(c.w()-4,1,c.d()-4,Blocks.BARREL.defaultBlockState());
    }
    private static void outdoor(Canvas c,String id){
        if("WELL".equals(id)) {
            for(int x:new int[]{0,c.w()-1})for(int z:new int[]{0,c.d()-1})
                for(int y=1;y<=3;y++)c.b(x,y,z,Blocks.SPRUCE_FENCE.defaultBlockState());
            for(int x=0;x<c.w();x++)for(int z=0;z<c.d();z++)
                c.b(x,4,z,Blocks.SPRUCE_SLAB.defaultBlockState());
            return;
        }
        if("MONUMENT".equals(id)){c.plinth(c.w()/2,c.d()/2,6);return;}
        if("RITUAL_GROUND".equals(id)) {
            c.plinth(c.w()/2+4,c.d()/2,3);
            c.plinth(c.w()/2-4,c.d()/2,3);
            return;
        }
        if("GARDEN".equals(id))c.b(c.w()-2,1,c.d()-2,Blocks.COMPOSTER.defaultBlockState());
        if("RUIN".equals(id))c.b(c.w()/2,1,c.d()/2,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
    }
    private static void camp(Canvas c){
        c.b(c.w()/2,1,c.d()/2,Blocks.CAMPFIRE.defaultBlockState());
        c.b(c.w()/2+2,1,c.d()/2,Blocks.BARREL.defaultBlockState());
    }
    private static void trade(Canvas c,String id){
        int variant=Math.floorMod(id.hashCode(),3);
        for(int x=4;x<c.w()-3;x+=4){
            if(variant==0)c.table(x,5,1);
            else if(variant==1)c.b(x,1,5,Blocks.BARREL.defaultBlockState());
            else c.b(x,1,5,Blocks.CRAFTING_TABLE.defaultBlockState());
        }
        c.b(c.w()-4,1,3,Blocks.CHEST.defaultBlockState());
    }
}

package dev.dtzudontsay.knownworld.world.generation.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import java.util.function.IntBinaryOperator;

/** C4.15 — independently designed, chunk-clipped tiered fortified complexes.
 * Winterfell V3 remains exclusively owned by WinterfellWorldgenV3.
 */
public final class KnownWorldC410Fortifications {
    public record Core(String tier,int outerHalfSpan,int innerHalfSpan,
                       int keepWest,int keepEast,int keepNorth,int keepSouth,
                       int wallHeight,int keepHeight) { }
    private KnownWorldC410Fortifications() { }
    public static Core forTier(String tier) {
        return switch (tier) {
            case "SMALL" -> new Core(tier,56,0,-22,22,-18,18,6,11);
            case "MEDIUM" -> new Core(tier,106,0,-35,35,-24,30,12,21);
            case "MAJOR" -> new Core(tier,171,149,-44,44,35,121,16,30);
            default -> throw new IllegalArgumentException("Unknown fortified core: "+tier);
        };
    }
    public static boolean inCurtain(int rx,int rz,int radius,int thickness) {
        int d=Math.max(Math.abs(rx),Math.abs(rz));return d<=radius&&d>=radius-thickness;
    }
    public static boolean isGate(int rx,int rz,int radius) {
        return Math.abs(rx)<=4 && Math.abs(Math.abs(rz)-radius)<=3
            || Math.abs(rz)<=4 && Math.abs(Math.abs(rx)-radius)<=3;
    }
    public static void paintApprovedNewChunk(ChunkAccess chunk,int centerX,int centerZ,
                                             String tier,IntBinaryOperator naturalHeight) {
        if(chunk==null||naturalHeight==null)throw new IllegalArgumentException("Missing chunk/sampler");
        Core cfg=forTier(tier);
        int bx=chunk.getPos().getMinBlockX(),bz=chunk.getPos().getMinBlockZ();
        int foundation=naturalHeight.applyAsInt(centerX,centerZ);
        for(int iz=0;iz<16;iz++)for(int ix=0;ix<16;ix++) {
            int x=bx+ix,z=bz+iz,rx=x-centerX,rz=z-centerZ;
            if(Math.max(Math.abs(rx),Math.abs(rz))>cfg.outerHalfSpan()+10)continue;
            int h=naturalHeight.applyAsInt(x,z);
            if(inCurtain(rx,rz,cfg.outerHalfSpan(),tier.equals("SMALL")?2:3))
                curtain(chunk,x,z,rx,rz,h,cfg.outerHalfSpan(),cfg.wallHeight(),tier.equals("SMALL"));
            if(cfg.innerHalfSpan()>0 && inCurtain(rx,rz,cfg.innerHalfSpan(),3))
                curtain(chunk,x,z,rx,rz,h,cfg.innerHalfSpan(),cfg.wallHeight()-4,false);
            if(!tier.equals("SMALL")) {
                int radius=cfg.outerHalfSpan();
                // Round, crenellated corner towers and two flank towers on each side.
                int[] positions={-radius,radius};
                for(int tx:positions)for(int tz:positions)
                    watchTower(chunk,x,z,rx,rz,naturalHeight.applyAsInt(centerX+tx,centerZ+tz),tx,tz,6,cfg.wallHeight()+9,false);
                for(int side:positions)for(int half:new int[]{-radius/2,radius/2}) {
                    watchTower(chunk,x,z,rx,rz,naturalHeight.applyAsInt(centerX+side,centerZ+half),side,half,5,cfg.wallHeight()+7,false);
                    watchTower(chunk,x,z,rx,rz,naturalHeight.applyAsInt(centerX+half,centerZ+side),half,side,5,cfg.wallHeight()+7,false);
                }
            }
            switch(tier) {
                case "SMALL" -> fortifiedHall(chunk,x,z,rx,rz,foundation);
                case "MEDIUM" -> minorKeep(chunk,x,z,rx,rz,foundation);
                case "MAJOR" -> majorKeep(chunk,x,z,rx,rz,foundation);
                default -> { }
            }
        }
    }
    private static void curtain(ChunkAccess c,int x,int z,int rx,int rz,int natural,int r,int height,boolean timber) {
        int lateral=Math.abs(rz)>=r-3?rx:rz;
        boolean gate=isGate(rx,rz,r);
        BlockState masonry=timber?Blocks.SPRUCE_PLANKS.defaultBlockState():Blocks.STONE_BRICKS.defaultBlockState();
        if(timber && (rx+rz)%5==0)masonry=Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        for(int dy=-3;dy<=height+2;dy++) {
            BlockState state=Blocks.AIR.defaultBlockState();
            if(dy<=height)state=masonry;
            if(dy==height+1 && ((rx+rz)&3)<2)
                state=timber?Blocks.OAK_FENCE.defaultBlockState():Blocks.STONE_BRICK_WALL.defaultBlockState();
            if(gate && dy>=1 && dy<=8-Math.abs(lateral)/3)state=Blocks.AIR.defaultBlockState();
            if(gate && dy==0)state=Blocks.COBBLESTONE.defaultBlockState();
            if(gate && dy==9-Math.abs(lateral)/3 && !timber)state=Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            put(c,x,natural+dy,z,state);
        }
    }
    private static void watchTower(ChunkAccess c,int x,int z,int rx,int rz,int natural,int tx,int tz,int r,int h,boolean timber) {
        int dx=rx-tx,dz=rz-tz,dist=dx*dx+dz*dz;
        if(dist>(r+1)*(r+1))return;
        boolean edge=dist>=r*r-r*2;
        BlockState stone=timber?Blocks.SPRUCE_LOG.defaultBlockState():Blocks.STONE_BRICKS.defaultBlockState();
        for(int dy=-3;dy<=h+3;dy++) {
            if(dy<=h && (edge||dy<=0))put(c,x,natural+dy,z,stone);
            else if(dy<=h)put(c,x,natural+dy,z,Blocks.AIR.defaultBlockState());
        }
        if(dist<r*r)put(c,x,natural+h,z,Blocks.STONE_BRICKS.defaultBlockState());
        if(edge && (Math.abs(dx+dz)&1)==0)put(c,x,natural+h+1,z,Blocks.STONE_BRICK_WALL.defaultBlockState());
        if(edge&&((dx*13+dz*7)&7)==0)
            for(int dy=4;dy<=6;dy++)put(c,x,natural+dy,z,Blocks.IRON_BARS.defaultBlockState());
    }
    private static void fortifiedHall(ChunkAccess c,int x,int z,int rx,int rz,int floor) {
        // Timber great hall, stone plinth, framed walls, vaulted roof and flanking watch turrets.
        hall(c,x,z,rx,rz,floor,-22,22,-18,18,10,true);
        for(int tx:new int[]{-22,22})for(int tz:new int[]{-18,18})
            watchTower(c,x,z,rx,rz,floor,tx,tz,4,17,true);
        // Fortified entrance and covered porch.
        if(Math.abs(rx)<=5 && rz>=18 && rz<=23) {
            for(int dy=1;dy<=5;dy++) {
                if(Math.abs(rx)==5)put(c,x,floor+dy,z,Blocks.SPRUCE_LOG.defaultBlockState());
                else put(c,x,floor+dy,z,Blocks.AIR.defaultBlockState());
            }
            put(c,x,floor+6,z,Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
    }
    private static void minorKeep(ChunkAccess c,int x,int z,int rx,int rz,int floor) {
        hall(c,x,z,rx,rz,floor,-28,28,-23,27,19,false);
        for(int tx:new int[]{-29,29})for(int tz:new int[]{-23,27})
            watchTower(c,x,z,rx,rz,floor,tx,tz,7,29,false);
        // Low covered side wings distinguish a minor seat from a large castle.
        if(rx>=-39&&rx<=-28 && rz>=-11&&rz<=12)
            hall(c,x,z,rx,rz,floor,-39,-28,-11,12,12,false);
        if(rx>=28&&rx<=39 && rz>=-11&&rz<=12)
            hall(c,x,z,rx,rz,floor,28,39,-11,12,12,false);
        entrance(c,x,z,rx,rz,floor,27,7);
    }
    private static void majorKeep(ChunkAccess c,int x,int z,int rx,int rz,int floor) {
        // Great hall/keep: 89 x 64 stone block, larger and taller than the original generic cube.
        hall(c,x,z,rx,rz,floor,-44,44,58,121,28,false);
        for(int tx:new int[]{-43,43})for(int tz:new int[]{59,120})
            watchTower(c,x,z,rx,rz,floor,tx,tz,7,40,false);
        // Two separate front wings, a grand processional approach and towered gatehouse.
        if(rx>=-39&&rx<=-24 && rz>=34&&rz<=59)
            hall(c,x,z,rx,rz,floor,-39,-24,34,59,17,false);
        if(rx>=24&&rx<=39 && rz>=34&&rz<=59)
            hall(c,x,z,rx,rz,floor,24,39,34,59,17,false);
        if(Math.abs(rx)<=12 && rz>=48 && rz<=59)
            hall(c,x,z,rx,rz,floor,-12,12,48,59,17,false);
        entrance(c,x,z,rx,rz,floor,121,10);
    }
    private static void entrance(ChunkAccess c,int x,int z,int rx,int rz,int floor,int southernWall,int width) {
        if(rz>=southernWall-2&&rz<=southernWall+1&&Math.abs(rx)<=width/2) {
            for(int h=1;h<=7-Math.abs(rx)/2;h++)put(c,x,floor+h,z,Blocks.AIR.defaultBlockState());
            put(c,x,floor,z,Blocks.POLISHED_ANDESITE.defaultBlockState());
            if(rz==southernWall-2 && (Math.abs(rx)==width/2))
                for(int h=2;h<=8;h++)put(c,x,floor+h,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        }
    }
    private static void hall(ChunkAccess c,int x,int z,int rx,int rz,int y,
                             int west,int east,int north,int south,int height,boolean wood) {
        if(rx<west||rx>east||rz<north||rz>south)return;
        int minDist=Math.min(rx-west,east-rx);
        int roofRise=Math.min(wood?9:12,Math.max(0,minDist));
        int roofY=y+height+roofRise;
        boolean edge=rx==west||rx==east||rz==north||rz==south;
        BlockState wall=wood?Blocks.SPRUCE_PLANKS.defaultBlockState():Blocks.STONE_BRICKS.defaultBlockState();
        BlockState roof=wood?Blocks.DARK_OAK_PLANKS.defaultBlockState():Blocks.DEEPSLATE_TILES.defaultBlockState();
        // Foundations; never flatten the entire mountain beyond this exact building footprint.
        for(int h=-4;h<=0;h++)put(c,x,y+h,z,h==0?Blocks.POLISHED_ANDESITE.defaultBlockState():Blocks.COBBLESTONE.defaultBlockState());
        for(int h=1;h<height;h++) {
            boolean framing=wood&&(rx==west||rx==east||rz==north||rz==south) && (((rx-west)%6==0)||((rz-north)%6==0));
            boolean window=edge&&h>=4&&h<=6&&(((rx-west)%9)==4||((rz-north)%9)==4);
            if(edge)put(c,x,y+h,z,window?Blocks.IRON_BARS.defaultBlockState():framing?Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState():wall);
            else put(c,x,y+h,z,Blocks.AIR.defaultBlockState());
        }
        // Intermediate floors, with a central circulation void through each upper storey.
        if(height>=17 && !edge && (height==17||height>=19))
            for(int h=7;h<height;h+=7)if(Math.abs(rx)>3||Math.abs(rz-(north+south)/2)>3)
                put(c,x,y+h,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
        // SOLID PITCHED ROOF over every x/z, including the previously open central area.
        put(c,x,roofY,z,roof);
        // Seal the triangular gables and add a consistent ridge.
        if((rz==north||rz==south)&&roofRise>0)
            for(int h=height;h<height+roofRise;h++)put(c,x,y+h,z,wall);
        if(roofRise==(wood?9:12) && ((rx-west)&3)==0)
            put(c,x,roofY+1,z,Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
        // Ground entrance on southern side; no yawning full-width hollow roof opening.
        if(rz==south&&Math.abs(rx-(west+east)/2)<=2)for(int h=1;h<=4;h++)
            put(c,x,y+h,z,Blocks.AIR.defaultBlockState());
        // Interior light columns on usable floors.
        if(!edge && (rx-west)%13==0 && (rz-north)%15==0)
            put(c,x,y+1,z,Blocks.LANTERN.defaultBlockState());
    }
    private static void put(ChunkAccess c,int x,int y,int z,BlockState block) {
        if(y<c.getMinY()||y>=c.getMaxY())return;
        if((x>>4)!=c.getPos().x()||(z>>4)!=c.getPos().z())return;
        c.setBlockState(new BlockPos(x,y,z),block,0);
    }
}

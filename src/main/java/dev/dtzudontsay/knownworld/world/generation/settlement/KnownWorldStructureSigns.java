package dev.dtzudontsay.knownworld.world.generation.settlement;

import dev.dtzudontsay.knownworld.KnownWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * C4.6 BOUNDED, MAIN-TICK building signage. No sign text is changed during
 * terrain generation or Fabric CHUNK_LOAD (both caused failures in C4.5.x).
 * Worldgen only installs the sign BlockEntity and records a label request.
 * On END_SERVER_TICK, only already-loaded overworld chunks close to players
 * are eligible; at most 16 sign updates per tick; NO forced chunk loads.
 *
 * The job queue is intentionally a session-local, bounded cache. Applied sign
 * text is stored by Minecraft in the sign block entity. For distant-generated
 * chunks that are never loaded before shutdown, regeneration of queued labels
 * from immutable lot IDs will be a later persistence enhancement. Never assert
 * every distant sign is guaranteed to be written by this batch.
 */
public final class KnownWorldStructureSigns {
    private static final int MAX_PENDING=20000;
    private static final int PER_TICK=16;
    private static final int PLAYER_CHUNK_RADIUS=7;
    private static final ConcurrentHashMap<Long,ConcurrentLinkedQueue<Entry>> PENDING=new ConcurrentHashMap<>();
    private static final AtomicInteger COUNT=new AtomicInteger();
    private static final AtomicBoolean REGISTERED=new AtomicBoolean();
    private record Entry(BlockPos pos,String label) { }
    private KnownWorldStructureSigns() { }

    private static long key(int x,int z){return ((long)x<<32) ^ (z&0xffffffffL);}

    public static void register() {
        if(!REGISTERED.compareAndSet(false,true))return;
        ServerTickEvents.END_SERVER_TICK.register(KnownWorldStructureSigns::onTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{PENDING.clear();COUNT.set(0);});
        KnownWorld.LOGGER.info("C4.6 limited main-server-tick signs ready; max 16 writes/tick; no chunk load callbacks");
    }

    private static void onTick(MinecraftServer server) {
        if(COUNT.get()<=0)return;
        int remaining=PER_TICK;
        for(ServerPlayer player:server.getPlayerList().getPlayers()) {
            if(remaining<=0)break;
            ServerLevel level=(ServerLevel)player.level();
            if(!Level.OVERWORLD.equals(level.dimension()))continue;
            int xc=player.chunkPosition().x(),zc=player.chunkPosition().z();
            for(int radius=0;radius<=PLAYER_CHUNK_RADIUS && remaining>0;radius++) {
                for(int dx=-radius;dx<=radius && remaining>0;dx++) {
                    for(int dz=-radius;dz<=radius && remaining>0;dz++) {
                        if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                        int cx=xc+dx,cz=zc+dz;
                        var queue=PENDING.get(key(cx,cz));
                        if(queue==null || !level.hasChunk(cx,cz))continue;
                        Entry entry;
                        while(remaining>0 && (entry=queue.poll())!=null) {
                            COUNT.decrementAndGet();remaining--;
                            if(level.getBlockEntity(entry.pos()) instanceof SignBlockEntity sign
                                    && sign.getLevel()!=null) {
                                var text=SignText.EMPTY.asMutable();
                                List<String> lines=split(entry.label());
                                for(int i=0;i<Math.min(4,lines.size());i++)
                                    text.setLine(i,Component.literal(lines.get(i)));
                                sign.setText(text.asImmutable(),SignTextSlot.FRONT);
                            }
                        }
                        // Keep empty queue entries: worldgen may append from other
                        // threads while this tick drains, and removing the entry
                        // here could silently drop a newly queued label.
                    }
                }
            }
        }
    }

    private static List<String> split(String name) {
        List<String> lines=new ArrayList<>();StringBuilder current=new StringBuilder();
        for(String word:name.replace('_',' ').trim().split(" +")) {
            if(current.length()>0 && current.length()+1+word.length()>15) {
                lines.add(current.toString());current.setLength(0);
            }
            if(current.length()>0)current.append(' ');
            if(word.length()>15){
                if(current.length()>0){lines.add(current.toString());current.setLength(0);}
                for(int i=0;i<word.length();i+=15)lines.add(word.substring(i,Math.min(i+15,word.length())));
            } else current.append(word);
        }
        if(current.length()>0)lines.add(current.toString());
        return lines;
    }

    public static void attach(ChunkAccess chunk,int x,int z,int width,int depth,
                              int floor,String face,String buildingType) {
        if(buildingType==null || buildingType.isBlank() || "FIELD".equals(buildingType)
                || "HOUSE".equals(buildingType) || buildingType.startsWith("HOUSE_")
                || "COTTAGE".equals(buildingType) || "CASTLE_QUARTER".equals(buildingType))return;
        boolean north="NORTH".equals(face);
        boolean outdoors=switch(buildingType) {
            case "GARDEN","PASTURE","WELL","COURTYARD","RITUAL_GROUND","RUIN","MONUMENT" -> true;
            default -> false;
        };
        int sx=outdoors?x+1:x+Math.min(width-2,width/2+2);
        int sz=outdoors?z+1:north?z-1:z+depth;
        BlockPos pos=new BlockPos(sx,floor+(outdoors?1:3),sz);
        if((sx>>4)!=chunk.getPos().x() || (sz>>4)!=chunk.getPos().z())return;
        Direction facing=north?Direction.NORTH:Direction.SOUTH;
        BlockState state=outdoors
                ?Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION,8)
                :Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,facing);
        chunk.setBlockState(pos,state,0);
        chunk.setBlockEntity(new SignBlockEntity(pos,state));
        // No world access here. Retry after real chunks are available to players.
        if(COUNT.incrementAndGet()>MAX_PENDING){COUNT.decrementAndGet();return;}
        String label=buildingType.replace("C3:","").replace('_',' ');
        if("GARDEN".equals(buildingType))label="Cultivated Fields / Garden";
        PENDING.computeIfAbsent(key(sx>>4,sz>>4),k->new ConcurrentLinkedQueue<>())
               .add(new Entry(pos,label));
    }
}

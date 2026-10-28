package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellConversationPackets;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC411GlobalWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * B4.2C: visible-only, deliberately non-authoritative path intent controller.
 * No chunk loading, no teleportation, no change to B4.0 identities or economics.
 * It reuses a route while in progress instead of commanding a new direction
 * every projection scan. Fortification gates are waypoints, not magic portals.
 */
public final class KnownWorldB42PhysicalRoutine {
    public record Result(boolean requested, boolean nearGoal, boolean deferred, String reason) { }
    private static final double WALK_SPEED = 0.48;
    private static final long RETARGET_TICKS = 160;
    private static final int MAX_WAYPOINT = 14;
    private record Intent(String role,int x,int z,long lastRequest,long lastSeen) { }
    private static final Map<UUID,Intent> LIVE = new HashMap<>();
    private KnownWorldB42PhysicalRoutine() { }

    public static void clear() { LIVE.clear(); }

    /** Existing entities are discarded by the projection system; drop old route state. */
    public static void prune(long time) {
        for (Iterator<Map.Entry<UUID,Intent>> it=LIVE.entrySet().iterator();it.hasNext();) {
            Intent state=it.next().getValue();
            if(time-state.lastSeen()>2400 || time<state.lastSeen())it.remove();
        }
    }

    public static Result steer(Villager mob,String role,int targetX,int targetZ,
                               int[] fortification,long tick) {
        if(WinterfellConversationPackets.isConversationActor(mob.getUUID()))
            return new Result(false,false,true,"DIALOGUE_LOCK");
        if(!(mob.level() instanceof ServerLevel world))
            return new Result(false,false,true,"NOT_SERVER_LEVEL");
        double distance=Math.hypot(targetX-mob.getX(),targetZ-mob.getZ());
        if(distance<2.5) {
            LIVE.remove(mob.getUUID());
            mob.getNavigation().stop();
            return new Result(false,true,false,"NEAR_GOAL");
        }
        // The objective is stable for a whole work/leisure phase. No repeated
        // reversal to a house anchor while commuting or while at a worksite.
        Intent old=LIVE.get(mob.getUUID());
        boolean changed=old==null || !old.role().equals(role) ||
                Math.abs(old.x()-targetX)+Math.abs(old.z()-targetZ)>3;
        if(!changed && !mob.getNavigation().isDone() && tick-old.lastRequest()<600) {
            LIVE.put(mob.getUUID(),new Intent(role,targetX,targetZ,old.lastRequest(),tick));
            return new Result(false,false,true,"ROUTE_STILL_IN_PROGRESS");
        }
        if(!changed && tick-old.lastRequest()<RETARGET_TICKS) {
            LIVE.put(mob.getUUID(),new Intent(role,targetX,targetZ,old.lastRequest(),tick));
            return new Result(false,false,true,"REPATH_COOLDOWN");
        }
        int[] portal=portalWaypoint((int)Math.floor(mob.getX()),(int)Math.floor(mob.getZ()),
                                    targetX,targetZ,fortification);
        int tx=portal==null?targetX:portal[0],tz=portal==null?targetZ:portal[1];
        double dx=tx-mob.getX(), dz=tz-mob.getZ();
        double length=Math.hypot(dx,dz);
        if(length<1.5){tx=targetX;tz=targetZ;dx=tx-mob.getX();dz=tz-mob.getZ();length=Math.hypot(dx,dz);}
        if(length<.8)return new Result(false,true,false,"NEAR_WAYPOINT");
        int max=(int)Math.min(MAX_WAYPOINT,Math.ceil(length));
        for(int step=max;step>=3;step-=3) {
            int x=(int)Math.floor(mob.getX()+dx*step/length);
            int z=(int)Math.floor(mob.getZ()+dz*step/length);
            if(!world.isLoaded(new BlockPos(x,64,z)))continue;
            int nativeHeight=KnownWorldC411GlobalWorldgen.initialTerrainHeight(x,z);
            // The top heightmap sees roofs. Try the current walking elevation and
            // pristine ground as well so accessible interiors are not automatically
            // rejected as "too high" rooftops.
            int[] candidateY={mob.blockPosition().getY(),nativeHeight+1,
                    world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)};
            for(int y:candidateY) {
                if(y<=world.getMinY()+2||y>=world.getMaxY()-3||Math.abs(y-nativeHeight)>7)continue;
                BlockPos feet=new BlockPos(x,y,z),floor=feet.below();
                if(world.getBlockState(floor).getCollisionShape(world,floor).isEmpty()||
                    !world.getBlockState(feet).getCollisionShape(world,feet).isEmpty()||
                    !world.getBlockState(feet.above()).getCollisionShape(world,feet.above()).isEmpty())continue;
                // A successful request is not proof of arrival. Vanilla pathfinding
                // is still allowed to reject inaccessible doors or steep terrain.
                if(mob.getNavigation().moveTo(x+.5,y,z+.5,WALK_SPEED)) {
                    LIVE.put(mob.getUUID(),new Intent(role,targetX,targetZ,tick,tick));
                    return new Result(true,false,false,portal==null?"LOCAL_WAYPOINT":"GATE_WAYPOINT");
                }
            }
        }
        LIVE.put(mob.getUUID(),new Intent(role,targetX,targetZ,tick,tick));
        return new Result(false,false,true,"BLOCKED_OR_UNLOADED_PATH");
    }

    /** Shared cardinal gateway geometry from C4.10; gates are 9 blocks wide. */
    public static int[] portalWaypoint(int x,int z,int tx,int tz,int[] fort) {
        if(fort==null||fort.length<4)return null;
        int cx=fort[0],cz=fort[1];
        for(int radius:new int[]{fort[2],fort[3]}) {
            if(radius<=0)continue;
            int current=Math.max(Math.abs(x-cx),Math.abs(z-cz));
            int destination=Math.max(Math.abs(tx-cx),Math.abs(tz-cz));
            boolean hereInside=current<radius-5;
            boolean thereInside=destination<radius-5;
            if(hereInside==thereInside)continue;
            int beyond=thereInside?-9:9;
            int[][] candidates={{cx,cz-radius-beyond},{cx,cz+radius+beyond},
                                {cx-radius-beyond,cz},{cx+radius+beyond,cz}};
            long best=Long.MAX_VALUE;int[] chosen=null;
            for(int[] c:candidates) {
                long score=Math.abs(x-c[0])+Math.abs(z-c[1])+
                           Math.abs(tx-c[0])+Math.abs(tz-c[1]);
                if(score<best){best=score;chosen=c;}
            }
            return chosen;
        }
        return null;
    }
}

package dev.dtzudontsay.knownworld.world.generation.settlement;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2.Lot;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * C4.6 blueprint inspection. Builds every real C3 template on demand in a
 * dedicated, freshly-created superflat review world. Uses the SAME renderer as
 * settlement worldgen, but an explicitly live ServerLevel block sink.
 *
 * Builds ONE blueprint per scheduled server tick interval and never issues
 * /fill on all 61 templates in one tick. No NPCs are created.
 * Not registered as a global/worldgen construction mechanism.
 */
public final class KnownWorldBlueprintGallery {
    private static final int PITCH = 78;
    private static final int COLUMNS = 7;
    private static final int TICKS_BETWEEN_LOTS = 20;
    private static final int MAX_LOTS = 100;
    private static Job pending;
    private static int clock;

    private record Job(ServerLevel level, int x, int z, int floor,
                       List<String> ids, int index) { }

    private KnownWorldBlueprintGallery() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("kwgallery")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.literal("build").executes(ctx -> build(ctx.getSource().getPlayerOrException())))
                    .then(Commands.literal("status").executes(ctx -> status(ctx.getSource().getPlayerOrException())))
                    .then(Commands.literal("cancel").executes(ctx -> cancel(ctx.getSource().getPlayerOrException())))));
        ServerTickEvents.END_SERVER_TICK.register(KnownWorldBlueprintGallery::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { pending=null;clock=0; });
    }

    private static List<String> sortedIds() {
        List<String> result=new ArrayList<>(KnownWorldC3Architecture.allTemplates().keySet());
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private static int build(ServerPlayer player) {
        ServerLevel level=(ServerLevel)player.level();
        if (!Level.OVERWORLD.equals(level.dimension())) {
            player.sendSystemMessage(Component.literal("Gallery requires the Overworld."));return 0;
        }
        // This gallery intentionally modifies blocks. Strong guard against
        // accidentally running inside the GOT custom Known World generator.
        if (level.getChunkSource().getGenerator() instanceof
                dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator) {
            player.sendSystemMessage(Component.literal("Gallery is disabled in Known World terrain. Create a separate superflat test world."));
            return 0;
        }
        if (pending!=null) {
            player.sendSystemMessage(Component.literal("Gallery already building; use /kwgallery status or cancel."));return 0;
        }
        List<String> ids=sortedIds();
        if (ids.size()<43 || ids.size()>MAX_LOTS) {
            player.sendSystemMessage(Component.literal("Unexpected C3 template count: "+ids.size()));return 0;
        }
        int x=player.blockPosition().getX()+10;
        int z=player.blockPosition().getZ()+10;
        int floor=level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)-1;
        if (floor<=level.getMinY()+1 || floor>level.getMaxY()-40) {
            player.sendSystemMessage(Component.literal("Gallery needs safe, fairly level ground."));return 0;
        }
        pending=new Job(level,x,z,floor,List.copyOf(ids),0);
        clock=0;
        player.sendSystemMessage(Component.literal("C4.6 gallery queued "+ids.size()+" genuine C3 programs. "
                +"Size 7 columns x "+((ids.size()+COLUMNS-1)/COLUMNS)+" rows; spacing 78 blocks."
                +" The command builds gradually to protect the server."));
        return 1;
    }

    private static int cancel(ServerPlayer player) {
        if(pending==null){player.sendSystemMessage(Component.literal("No active gallery job."));return 0;}
        pending=null;
        player.sendSystemMessage(Component.literal("Gallery build stopped. Already-built structures remain."));
        return 1;
    }

    private static int status(ServerPlayer player) {
        Job j=pending;
        player.sendSystemMessage(Component.literal(j==null?"No gallery job running.":
                "Gallery: "+j.index()+"/"+j.ids().size()+" blueprints placed"));
        return 1;
    }

    private static void tick(MinecraftServer server) {
        Job job=pending;
        if (job==null || ++clock<TICKS_BETWEEN_LOTS) return;
        clock=0;
        if (job.level().getServer()!=server) {pending=null;return;}
        try {
            int i=job.index();
            if(i>=job.ids().size()){pending=null;return;}
            String id=job.ids().get(i);
            int x=job.x()+(i%COLUMNS)*PITCH;
            int z=job.z()+(i/COLUMNS)*PITCH;
            var template=KnownWorldC3Architecture.template(id);
            // Vanilla superflat has large empty space above a uniform ground
            // plane. Gallery renderer deliberately doesn't need shore/policy
            // authorization; that is handled in real-world generation.
            int[] wh=KnownWorldBlueprintGalleryDimensions.dimension(id);
            Lot lot=new Lot("gallery_"+id,id,x,z,wh[0],wh[1],"SOUTH",0,template.maxStaff());
            KnownWorldC3Architecture.buildLiveGalleryLot(job.level(),lot,job.floor(),"NORTHERN","MEDIUM");
            placeSign(job.level(),new BlockPos(x+wh[0]/2,job.floor()+1,z+wh[1]+3),
                    "#"+(i+1)+" / "+job.ids().size(), id, "Staff: "+template.maxStaff());
            pending=new Job(job.level(),job.x(),job.z(),job.floor(),job.ids(),i+1);
            if (i+1==job.ids().size()) {
                KnownWorld.LOGGER.info("Gallery finished {} building programs",job.ids().size());
                pending=null;
            }
        } catch (RuntimeException ex) {
            KnownWorld.LOGGER.error("C4.6 gallery stopped after blueprint #{}",job.index()+1,ex);
            pending=null; // Never repeat a partial construction indefinitely.
        }
    }

    /** In-gallery signs are authored on a LIVE server world, never a protochunk. */
    private static void placeSign(ServerLevel level, BlockPos pos, String top, String label, String staff) {
        var signState=Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION,8);
        level.setBlock(pos,signState,2);
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity entity)) return;
        var text=SignText.EMPTY.asMutable();
        String name=label.replace('_',' ').trim();
        text.setLine(0,Component.literal(top));
        text.setLine(1,Component.literal(name.length()<=15?name:name.substring(0,15)));
        if(name.length()>15) text.setLine(2,Component.literal(name.substring(15,Math.min(name.length(),30)).trim()));
        text.setLine(3,Component.literal(staff.length()>15?staff.substring(0,15):staff));
        entity.setText(text.asImmutable(),SignTextSlot.FRONT);
    }
}

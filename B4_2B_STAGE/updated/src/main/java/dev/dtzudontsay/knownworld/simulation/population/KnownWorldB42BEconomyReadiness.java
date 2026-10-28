package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Economic 298 AC initial-state calculation for audit, NOT an inventory ledger. */
public final class KnownWorldB42BEconomyReadiness {
    private static final String FILE="/data/knownworld/scenarios/agot_298_ac/world_economy_readiness_b4_2b_v1/manifest.json";
    private KnownWorldB42BEconomyReadiness() { }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(Commands.literal("kweconomyplan")
                .then(Commands.literal("status").executes(c->status(c.getSource())))));
    }
    private static int status(CommandSourceStack src) {
        try(InputStream in=KnownWorldB42BEconomyReadiness.class.getResourceAsStream(FILE)) {
            if(in==null)throw new IllegalStateException("Missing economy readiness document");
            JsonObject j=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            if(!"knownworld.b4_2b.economic_readiness.v1".equals(j.get("schemaVersion").getAsString()) ||
                    !"READ_ONLY_NO_TRANSACTIONS".equals(j.get("activationMode").getAsString()) ||
                    j.get("siteCount").getAsInt()!=248 || j.get("peopleTotal").getAsInt()!=3760684 ||
                    j.get("workersTotal").getAsInt()!=1880341 ||
                    j.get("initialGrainActuallyCreditedKg").getAsLong()!=0)
                throw new IllegalStateException("Economic source/authority mismatch");
            say(src,"[B4.2B] 248 settlements; population 3,760,684; workers 1,880,341. "+
                    "90-day INITIAL RESERVE ESTIMATE is READ-ONLY. Actually credited grain=0 kg.");
            say(src,"[B4.2B] Autonomous production/consumption remains DISABLED until "+
                    "existing InventoryManager stock, holding links, production coefficients and atomic save transaction are verified.");
            return 1;
        } catch(Exception e) {
            say(src,"[B4.2B] BLOCKED: "+e.getMessage()); return 0;
        }
    }
    private static void say(CommandSourceStack src,String m){src.sendSuccess(()->Component.literal(m),false);}
}

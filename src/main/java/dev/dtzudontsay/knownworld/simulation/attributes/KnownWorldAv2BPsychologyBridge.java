package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterDerivedTendency;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyRules;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** AV2B pure adapter: B4.0+AV2A profile -> the SAME rules used by legacy NpcState. */
public final class KnownWorldAv2BPsychologyBridge {
    private KnownWorldAv2BPsychologyBridge() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwpsych")
                .then(Commands.literal("status").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "[AV2B] Shared legacy/B4 psychology rules ACTIVE; 12 derived tendencies; " +
                        "read-only, not a second NpcRegistry; no autonomous actions, memories or economy effects."), false);
                    return 1;
                }))));
    }

    /** Consume the AV2A-updated snapshot; never mutate existing persisted attributes. */
    public static void enrich(JsonObject sheet) {
        if(sheet==null)throw new IllegalArgumentException("Missing B4 profile");
        String state = optionalString(sheet,"av2State");
        if("BLOCKED_CORRUPT_OR_PENDING_SAVE".equals(state) || "SERVER_UNAVAILABLE".equals(state)) {
            sheet.addProperty("av2bPsychology","BLOCKED_UPSTREAM_PROFILE");
            return;
        }
        try {
            CharacterPsychologyRules.Inputs inputs = inputs(sheet);
            Map<CharacterDerivedTendency,Double> scores=CharacterPsychologyRules.evaluate(inputs);
            JsonObject values=new JsonObject();
            for(CharacterDerivedTendency axis:CharacterDerivedTendency.values())
                values.addProperty(axis.name().toLowerCase(Locale.ROOT),scores.get(axis));
            sheet.add("derivedTendencies",values);
            sheet.addProperty("socialSusceptibility",CharacterPsychologyRules.socialSusceptibility(inputs));
            sheet.addProperty("persuasionResistance",CharacterPsychologyRules.persuasionResistance(inputs));
            sheet.addProperty("av2bPsychology","SHARED_DERIVED_MODEL_READ_ONLY");
        } catch (RuntimeException error) {
            // No fake value, partial tendency map, created actor or population mutation.
            sheet.remove("derivedTendencies");
            sheet.remove("socialSusceptibility");
            sheet.remove("persuasionResistance");
            sheet.addProperty("av2bPsychology","BLOCKED_INVALID_PROFILE");
        }
    }

    public static CharacterPsychologyRules.Inputs inputs(JsonObject sheet) {
        JsonObject personality=requireObject(sheet,"personality");
        // B4.1A.1's generated personality scores use [0,1]. Legacy NpcPersonality
        // uses [-1,1]; normalize exactly once at the data-authority boundary.
        NpcPersonality p=new NpcPersonality(
            toSigned(personality,"courage"),toSigned(personality,"ambition"),
            toSigned(personality,"compassion"),toSigned(personality,"honor"),
            toSigned(personality,"patience"),toSigned(personality,"sociability"));
        return new CharacterPsychologyRules.Inputs(p,
            dimensions(requireObject(sheet,"dispositions"),CharacterDisposition.values(),-1,1,false),
            dimensions(requireObject(sheet,"characterValues"),CharacterValue.values(),-1,1,false),
            dimensions(requireObject(sheet,"socialNorms"),CharacterSocialNorm.values(),-1,1,false),
            dimensions(requireObject(sheet,"aptitudes"),CharacterAptitude.values(),0,1,true));
    }

    private static String optionalString(JsonObject json, String field){
        JsonElement x=json.get(field);
        return x==null||!x.isJsonPrimitive()||!x.getAsJsonPrimitive().isString()?"":x.getAsString();
    }
    private static JsonObject requireObject(JsonObject json,String field){
        JsonObject value=json.getAsJsonObject(field);
        if(value==null)throw new IllegalArgumentException("No profile group: "+field);
        return value;
    }
    private static double numeric(JsonObject json,String field,double lower,double upper){
        JsonElement value=json.get(field);
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Missing numeric profile field "+field);
        double x=value.getAsDouble();
        if(!Double.isFinite(x)||x<lower||x>upper)
            throw new IllegalArgumentException("Out-of-range profile field "+field);
        return x;
    }
    private static double toSigned(JsonObject obj,String field){return numeric(obj,field,0,1)*2-1;}
    private static <E extends Enum<E>> Map<E,Double> dimensions(
            JsonObject json,E[] axes,double lower,double upper,boolean aptitudes){
        EnumMap<E,Double> values=new EnumMap<>(axes[0].getDeclaringClass());
        for(E axis:axes){
            String name=axis.name().toLowerCase(Locale.ROOT);
            // Existing B4.1A.1 profile uses 'language'; typed aptitude says LANGUAGES.
            if(aptitudes&&"languages".equals(name))name="language";
            values.put(axis,numeric(json,name,lower,upper));
        }
        return values;
    }
}

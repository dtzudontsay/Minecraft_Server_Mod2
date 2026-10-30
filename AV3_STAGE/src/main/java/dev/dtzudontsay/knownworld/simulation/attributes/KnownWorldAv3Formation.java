package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;

/**
 * AV3: deterministic STARTING-state enrichment for existing B4.0 persons.
 * Never modifies census, IDs, entity lifetime, authored profiles, world saves,
 * occupations, skills, languages, faith, culture, or reported family relations.
 * Authoritative original region bias is already included in B41 generation;
 * we do NOT apply it a second time. AV2A overrides run AFTER this stage.
 *
 * Culture/faith priors are *correlations* estimated from the supplied regional
 * influence data, not hard-coded immutable ethnic/religious stereotypes.
 */
public final class KnownWorldAv3Formation {
    public static final String SCHEMA="knownworld.av3.regional_family_starting_prior.v1";
    private static final double CULTURE_WEIGHT=0.18;
    private static final double FAITH_WEIGHT=0.09;
    private static final double FAMILY_VALUE=0.035;
    private static final double FAMILY_NORM=0.035;
    private static final double FAMILY_DISPOSITION=0.085;
    private static final double FAMILY_APTITUDE=0.055;
    private KnownWorldAv3Formation() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwformation")
                .then(Commands.literal("status").executes(ctx -> {
                    try {
                        var refs=KnownWorldAv3RegionalPriors.index();
                        long influencedCultures=refs.cultures().values().stream().filter(
                            KnownWorldAv3RegionalPriors.Prior::hasEvidence).count();
                        long influencedFaiths=refs.religions().values().stream().filter(
                            KnownWorldAv3RegionalPriors.Prior::hasEvidence).count();
                        ctx.getSource().sendSuccess(()->Component.literal(
                            "[AV3] Baseline-only regional correlation + shared family priors READY; " +
                            "locations="+refs.locations()+" cultures="+refs.cultureCount()+
                            " (with source effects="+influencedCultures+") faiths="+refs.religionCount()+
                            " (with source effects="+influencedFaiths+"); sourceDigest="+refs.sourceDigest().substring(0,16)+". " +
                            "B4 site region not double-applied; AV2A/AV2D overrides preserved. " +
                            "No autonomous generation of new persons, upbringing or skill awards."),false);
                        return 1;
                    } catch(RuntimeException ex) {
                        ctx.getSource().sendFailure(Component.literal(
                            "[AV3] BLOCKED: reference-catalog validation failed: "+ex.getMessage()));
                        return 0;
                    }
                }))));
    }

    /** Applies to BOTH right-click inspection and K-dialogue in B41.enrich(). */
    public static void applyToSheet(JsonObject sheet,String siteId,String personId,
                                    String cultureId,String religionId,String householdId,
                                    String motherId,String fatherId,int birthYear) {
        if(sheet==null)throw new IllegalArgumentException("No B4 baseline profile");
        try {
            String canonicalSite=KnownWorldAv2DChangePolicy.siteOf(personId);
            if(!canonicalSite.equals(siteId))throw new IllegalArgumentException("Foreign B4.0 identity/site");
            var index=KnownWorldAv3RegionalPriors.index();
            var culture=index.culture(normalize(cultureId));
            var faith=index.religion(normalize(religionId));
            // An unlisted identity gets no culture/faith prior; never silently
            // equate a new value with a different catalog entry.
            if(culture==null) culture=new KnownWorldAv3RegionalPriors.Prior(Map.of(),Map.of(),0);
            if(faith==null) faith=new KnownWorldAv3RegionalPriors.Prior(Map.of(),Map.of(),0);
            String familyBasis;
            String familyKey;
            if(known(motherId)||known(fatherId)) {
                familyKey=siteId+"|parent_ids|"+safe(motherId)+"|"+safe(fatherId);
                familyBasis="SHARED_PARENT_IDS_NOT_MEASURED_PARENT_TRAITS";
            } else if(known(householdId)) {
                familyKey=siteId+"|household|"+householdId;
                familyBasis="HOUSEHOLD_SHARED_CONTEXT_NOT_GENETIC_INHERITANCE";
            } else {
                familyKey=personId;
                familyBasis="NO_CONFIRMED_FAMILY_GROUP_PERSONAL_PRIOR_ONLY";
            }
            JsonObject proposed=sheet.deepCopy();
            int changes=0;
            changes+=transform(proposed,"characterValues",names(CharacterValue.values()),
                culture.values(),faith.values(),familyKey,FAMILY_VALUE,-1,1);
            changes+=transform(proposed,"socialNorms",names(CharacterSocialNorm.values()),
                culture.norms(),faith.norms(),familyKey,FAMILY_NORM,-1,1);
            changes+=transform(proposed,"dispositions",names(CharacterDisposition.values()),
                Map.of(),Map.of(),familyKey,FAMILY_DISPOSITION,-1,1);
            changes+=transform(proposed,"aptitudes",names(CharacterAptitude.values()),
                Map.of(),Map.of(),familyKey,FAMILY_APTITUDE,0,1);
            // Do not change skills, personality, relationship links or stored IDs.
            // Commit all four groups only after the complete profile validates.
            for(String key:new String[]{"characterValues","socialNorms","dispositions","aptitudes"})
                sheet.add(key,proposed.get(key));
            JsonObject meta=new JsonObject();
            meta.addProperty("schema",SCHEMA);
            meta.addProperty("regionalSourceDigest",index.sourceDigest());
            meta.addProperty("regionalBaseline","B4_SITE_REGION_ALREADY_APPLIED_ONCE");
            meta.addProperty("culturePrior",culture.locations()>0?"REGIONAL_CORRELATION":"NO_SOURCE_EXPOSURES");
            meta.addProperty("cultureSourceLocations",culture.locations());
            meta.addProperty("faithPrior",faith.locations()>0?"REGIONAL_CORRELATION":"NO_SOURCE_EXPOSURES");
            meta.addProperty("faithSourceLocations",faith.locations());
            meta.addProperty("familyBasis",familyBasis);
            meta.addProperty("adjustedAxes",changes);
            meta.addProperty("birthYearAC",birthYear);
            meta.addProperty("sourceAuthority","REFERENCE_REGIONAL_INFLUENCES_PLUS_SHARED_SEED; MOD_ESTIMATES_NOT_CANON_FACTS");
            sheet.add("av3Formation",meta);
            sheet.addProperty("av3State","APPLIED_STARTING_PRIORS");
        } catch(RuntimeException invalid) {
            // Reject ALL changes rather than mixing partial cultural priors
            // into a person's saved or authored values.
            sheet.addProperty("av3State","BLOCKED_UNVERIFIED_STARTING_PRIORS");
        }
    }

    private static String[] names(Enum<?>[] values) {
        String[] out=new String[values.length];
        for(int i=0;i<values.length;i++)out[i]=values[i].name().toLowerCase(Locale.ROOT);
        return out;
    }
    private static int transform(JsonObject sheet,String field,String[] axes,
                                Map<String,Double> culture,Map<String,Double> faith,
                                String familyKey,double familyWeight,double min,double max) {
        JsonObject group=sheet.getAsJsonObject(field);
        if(group==null)throw new IllegalArgumentException("Missing B4 profile group: "+field);
        int adjusted=0;
        for(String axis:axes){
            String key=axis;
            if("aptitudes".equals(field) && "languages".equals(key))key="language";
            JsonElement raw=group.get(key);
            if(raw==null || !raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("Missing canonical axis: "+field+"/"+key);
            double old=raw.getAsDouble();
            if(!Double.isFinite(old) || old<min || old>max)
                throw new IllegalArgumentException("Invalid B4 axis: "+field+"/"+key);
            double bias=(culture.getOrDefault(axis.toUpperCase(Locale.ROOT),0.0)*CULTURE_WEIGHT)
                +(faith.getOrDefault(axis.toUpperCase(Locale.ROOT),0.0)*FAITH_WEIGHT)
                +(familyVariance(familyKey,field,axis)*familyWeight);
            double next=Math.max(min,Math.min(max,old+bias));
            // Preserve three-decimal B4 presentation resolution; it is not a
            // new random roll, nor an additive mutation persisted every read.
            next=Math.round(next*1000.0)/1000.0;
            if(next!=old){group.addProperty(key,next);adjusted++;}
        }
        return adjusted;
    }
    public static double familyVariance(String group,String domain,String axis) {
        if(group==null || domain==null || axis==null)
            throw new IllegalArgumentException("Null AV3 seed");
        // Stable FNV-1a and final avalanche; no JVM identity hashes or clock.
        String key="av3-v1|"+group+"|"+domain+"|"+axis;
        long value=0xcbf29ce484222325L;
        for(int i=0;i<key.length();i++) {value^=key.charAt(i);value*=0x100000001b3L;}
        value^=value>>>33;value*=0xff51afd7ed558ccdL;
        value^=value>>>33;value*=0xc4ceb9fe1a85ec53L;value^=value>>>33;
        return ((value>>>11)*0x1.0p-53)*2.0-1.0;
    }
    private static String normalize(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT);}
    private static boolean known(String s){return s!=null && !s.isBlank() && !"unknown".equalsIgnoreCase(s) && !"none".equalsIgnoreCase(s);}
    private static String safe(String s){return known(s)?s:"none";}
}

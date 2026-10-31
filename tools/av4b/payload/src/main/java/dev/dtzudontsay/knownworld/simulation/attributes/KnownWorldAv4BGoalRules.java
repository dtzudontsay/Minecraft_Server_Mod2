package dev.dtzudontsay.knownworld.simulation.attributes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * AV4B pure, deterministic, FEASIBILITY-AWARE goal scoring.
 * These scores select INTENT only. No income, events, social links or memories are written.
 * The B4.2 schedule and actual world-resource feasibility always outrank preferences.
 */
public final class KnownWorldAv4BGoalRules {
    public enum Goal {
        SCHEDULED_SLEEP, SCHEDULED_WORK, RETURN_HOME,
        HOUSEHOLD_ACTIVITY, RECOVER, PUBLIC_LEISURE, SOCIAL_VISIT, OPTIONAL_WORK
    }
    public record Inputs(double ambition, double sociability, double courage,
                         double discipline, double empathy, double duty,
                         double wealthSeeking, double familyLoyalty,
                         double friendshipLoyalty, double personalFreedom,
                         double riskTaking) {
        public Inputs {
            for(double v:new double[]{ambition,sociability,courage,discipline,empathy,duty,
                    wealthSeeking,familyLoyalty,friendshipLoyalty,personalFreedom,riskTaking})
                if(!Double.isFinite(v)||v<0||v>1)
                    throw new IllegalArgumentException("AV4B attribute not normalized [0,1]");
        }
    }
    /** recoveryDemand is a schedule-derived recovery PROXY, not measured biological fatigue. */
    public record Context(String activity, boolean hasWorkplace, boolean hasPublicPlace,
                          boolean hasVisibleNeighbor, boolean adult, double recoveryDemand) {
        public Context {
            Objects.requireNonNull(activity,"activity");
            if(!Double.isFinite(recoveryDemand)||recoveryDemand<0||recoveryDemand>1)
                throw new IllegalArgumentException("Invalid recovery proxy");
            switch(activity) {
                case "SLEEP", "COMMUTING_TO_WORK", "MODELED_WORK_SHIFT", "COMMUTING_HOME",
                     "HOME_AND_CARE", "UNVERIFIED_WORKPLACE_OR_HOME", "REST_DAY",
                     "FREE_TIME", "HOME_AND_FREE_TIME" -> { }
                default -> throw new IllegalArgumentException("Unknown B4.2 schedule activity: "+activity);
            }
        }
        public boolean freeTime() {
            return activity.equals("REST_DAY")||activity.equals("FREE_TIME")||
                   activity.equals("HOME_AND_FREE_TIME");
        }
    }
    public record Decision(Goal goal, long campaignDay, String activity,
                           Map<Goal,Double> scores, Map<Goal,String> exclusions,
                           String authority) {
        public Decision {
            Objects.requireNonNull(goal); Objects.requireNonNull(activity);
            if(campaignDay<0)throw new IllegalArgumentException("Negative campaign day");
            scores=Collections.unmodifiableMap(new EnumMap<>(scores));
            exclusions=Collections.unmodifiableMap(new EnumMap<>(exclusions));
            if(!scores.containsKey(goal))throw new IllegalArgumentException("Selected unavailable goal");
        }
    }
    private KnownWorldAv4BGoalRules() { }
    private static double score(double value,long seed,Goal goal) {
        long h=mix(seed+0x9e3779b97f4a7c15L*(goal.ordinal()+1));
        return Math.round((value+((h>>>11)&0xffffL)/65535.0*0.04-0.02)*1000d)/1000d;
    }
    private static long mix(long v) {
        v^=v>>>33;v*=0xff51afd7ed558ccdL;v^=v>>>33;
        v*=0xc4ceb9fe1a85ec53L;v^=v>>>33;return v;
    }
    private static long hash(String text,long day) {
        long h=0xcbf29ce484222325L;
        for(int i=0;i<text.length();i++){h^=text.charAt(i);h*=0x100000001b3L;}
        return mix(h^day);
    }
    private static Decision fixed(Goal goal,String why,long day,String activity) {
        EnumMap<Goal,Double> scores=new EnumMap<>(Goal.class);
        EnumMap<Goal,String> excluded=new EnumMap<>(Goal.class);
        scores.put(goal,10.0);
        for(Goal alternative:Goal.values())if(alternative!=goal)
            excluded.put(alternative,"SCHEDULE_PRIORITY:"+why);
        return new Decision(goal,day,activity,scores,excluded,"SCHEDULE_GUARDED_INTENT_ONLY");
    }
    public static Decision evaluate(Inputs in,Context c,String personId,long campaignDay) {
        Objects.requireNonNull(in);Objects.requireNonNull(c);
        if(personId==null||personId.length()>180||
           !personId.matches("b4:site_[a-z][a-z0-9_]{1,90}:[A-Za-z0-9:_-]+")||
           campaignDay<0)throw new IllegalArgumentException("Invalid B4 person/day");
        switch(c.activity()) {
            case "SLEEP":return fixed(Goal.SCHEDULED_SLEEP,"SLEEP",campaignDay,c.activity());
            case "MODELED_WORK_SHIFT", "COMMUTING_TO_WORK":
                if(!c.hasWorkplace()||!c.adult())
                    throw new IllegalArgumentException("Scheduled work lacks eligible worksite/adult");
                return fixed(Goal.SCHEDULED_WORK,"WORK_DUTY",campaignDay,c.activity());
            case "COMMUTING_HOME":return fixed(Goal.RETURN_HOME,"RETURN_COMMUTE",campaignDay,c.activity());
            case "HOME_AND_CARE", "UNVERIFIED_WORKPLACE_OR_HOME":
                return fixed(Goal.HOUSEHOLD_ACTIVITY,"CARE_OR_UNVERIFIED_WORK",campaignDay,c.activity());
            default:break;
        }
        if(!c.freeTime())throw new IllegalArgumentException("Not a discretionary phase");
        EnumMap<Goal,Double> scores=new EnumMap<>(Goal.class);
        EnumMap<Goal,String> excluded=new EnumMap<>(Goal.class);
        long seed=hash(personId,campaignDay);
        scores.put(Goal.HOUSEHOLD_ACTIVITY,score(.40+.22*in.familyLoyalty()+.15*in.duty()+
                .11*in.discipline()-.12*in.sociability(),seed,Goal.HOUSEHOLD_ACTIVITY));
        scores.put(Goal.RECOVER,score(.30+.52*c.recoveryDemand()+.17*(1-in.ambition())+
                .08*(1-in.riskTaking()),seed,Goal.RECOVER));
        if(c.hasPublicPlace())scores.put(Goal.PUBLIC_LEISURE,score(.32+.29*in.sociability()+
                .16*in.personalFreedom()+.10*in.riskTaking(),seed,Goal.PUBLIC_LEISURE));
        else excluded.put(Goal.PUBLIC_LEISURE,"NO_REGISTERED_NEARBY_PUBLIC_PLACE");
        if(c.hasVisibleNeighbor())scores.put(Goal.SOCIAL_VISIT,score(.30+.25*in.sociability()+
                .20*in.empathy()+.18*in.friendshipLoyalty()+.06*in.courage(),seed,Goal.SOCIAL_VISIT));
        else excluded.put(Goal.SOCIAL_VISIT,"NO_CURRENTLY_LOADED_ELIGIBLE_B4_NEIGHBOR");
        if(!c.adult())excluded.put(Goal.OPTIONAL_WORK,"CHILD_NOT_ELIGIBLE");
        else if(!c.hasWorkplace())excluded.put(Goal.OPTIONAL_WORK,"NO_REGISTERED_ASSIGNED_WORKSITE");
        else scores.put(Goal.OPTIONAL_WORK,score(.15+.38*in.ambition()+
                .22*in.wealthSeeking()+.17*in.discipline()+.11*in.duty()-
                .23*c.recoveryDemand()-(c.activity().equals("REST_DAY")?.11:0.0),
                seed,Goal.OPTIONAL_WORK));
        for(Goal fixed:new Goal[]{Goal.SCHEDULED_SLEEP,Goal.SCHEDULED_WORK,Goal.RETURN_HOME})
            excluded.put(fixed,"NOT_CURRENTLY_SCHEDULED");
        Goal best=null;double highest=Double.NEGATIVE_INFINITY;
        // Stable tie-breaking follows the enum order, not HashMap iteration.
        for(Goal candidate:Goal.values()) {
            Double value=scores.get(candidate);
            if(value!=null && value>highest){highest=value;best=candidate;}
        }
        if(best==null)throw new IllegalStateException("No safe discretionary goal");
        return new Decision(best,campaignDay,c.activity(),scores,excluded,
                "DISCRETIONARY_MOVEMENT_INTENT_NO_VERIFIED_OUTCOME");
    }
}

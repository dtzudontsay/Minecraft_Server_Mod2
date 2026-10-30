package dev.dtzudontsay.knownworld.simulation.attributes;

import java.util.Map;
import java.util.Objects;

/**
 * AV4A: deterministic, pure short-horizon choice for genuine free-time movement.
 * All values are bounded [0,1], including inherited derived tendencies.
 * This is a preference policy, not evidence of a completed social encounter.
 */
public final class KnownWorldAv4DecisionRules {
    public enum Action { HOUSEHOLD_ACTIVITY, PUBLIC_LEISURE, NEIGHBOR_VISIT }

    public record Inputs(double sociability, double empathy, double friendshipLoyalty,
                         double urbanNorm, double isolationistNorm, double intervention,
                         double culturalOpenness, double peerConformity, double riskTaking) {
        public Inputs {
            for (double value : new double[]{sociability,empathy,friendshipLoyalty,urbanNorm,
                    isolationistNorm,intervention,culturalOpenness,peerConformity,riskTaking}) {
                if (!Double.isFinite(value) || value<0 || value>1)
                    throw new IllegalArgumentException("AV4 input outside [0,1]");
            }
        }
    }
    public record Decision(Action action, double homeScore, double publicScore,
                           double neighborScore, long campaignDay) {
        public Decision {
            Objects.requireNonNull(action, "action");
            if(campaignDay<0)throw new IllegalArgumentException("Negative campaign day");
        }
    }
    private KnownWorldAv4DecisionRules() { }

    public static Decision evaluate(Inputs in,String personId,long campaignDay) {
        Objects.requireNonNull(in,"inputs");
        if(personId==null||personId.length()>180||!personId.matches("b4:site_[a-z][a-z0-9_]{1,90}:[A-Za-z0-9:_-]+")
                || campaignDay<0)throw new IllegalArgumentException("Invalid authoritative person/day");
        double home=.38 + .28*(1-in.sociability()) + .16*in.isolationistNorm()
                + .10*(1-in.riskTaking()) + .12*(1-in.peerConformity());
        double pub=.35 + .28*in.sociability() + .19*in.urbanNorm()
                + .18*in.culturalOpenness() + .08*in.peerConformity();
        double neighbor=.33 + .27*in.sociability() + .22*in.friendshipLoyalty()
                + .16*in.empathy() + .16*in.intervention()
                + .08*in.peerConformity() - .12*in.isolationistNorm();
        // Tiny deterministic day-to-day variety, not globally shared RNG state.
        long seed=hash(personId+"|"+campaignDay);
        home+=jitter(seed,0);pub+=jitter(seed,1);neighbor+=jitter(seed,2);
        Action picked=home>=pub&&home>=neighbor?Action.HOUSEHOLD_ACTIVITY:
                pub>=neighbor?Action.PUBLIC_LEISURE:Action.NEIGHBOR_VISIT;
        return new Decision(picked,round(home),round(pub),round(neighbor),campaignDay);
    }
    public static Inputs read(Map<String,Double> fields) {
        Objects.requireNonNull(fields,"fields");
        return new Inputs(required(fields,"sociability"),required(fields,"empathy"),
                required(fields,"friendship_loyalty"),required(fields,"urban_norm"),
                required(fields,"isolationist_norm"),required(fields,"intervention"),
                required(fields,"cultural_openness"),required(fields,"peer_conformity"),
                required(fields,"risk_taking"));
    }
    private static double required(Map<String,Double> fields,String k) {
        Double v=fields.get(k);
        if(v==null)throw new IllegalArgumentException("Missing AV4 input: "+k);
        return v;
    }
    private static double round(double x) {return Math.round(x*1000d)/1000d;}
    private static double jitter(long h,int axis) {return ((h>>>(axis*15))&0x7fffL)/32767d*.04-.02;}
    private static long hash(String text) {
        long h=0xcbf29ce484222325L;
        for(int i=0;i<text.length();i++){h^=text.charAt(i);h*=0x100000001b3L;}
        h^=h>>>33;h*=0xff51afd7ed558ccdL;h^=h>>>33;
        return h;
    }
}

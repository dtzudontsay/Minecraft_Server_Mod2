package dev.dtzudontsay.knownworld.simulation.npc.psychology;

import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * AV2B: ONE stateless implementation of the 12 legacy derived tendencies.
 * Legacy NpcState and worldwide B4 use the SAME evaluator, not two models.
 * A tendency is a derived indicator, not a memory, event, decision or saved stat.
 * This class never constructs an NpcId or changes any profile.
 */
public final class CharacterPsychologyRules {
    public record Inputs(
        NpcPersonality personality,
        Map<CharacterDisposition, Double> dispositions,
        Map<CharacterValue, Double> values,
        Map<CharacterSocialNorm, Double> norms,
        Map<CharacterAptitude, Double> aptitudes
    ) {
        public Inputs {
            Objects.requireNonNull(personality, "personality");
            dispositions = copyAndValidate(dispositions, CharacterDisposition.values(), -1, 1);
            values = copyAndValidate(values, CharacterValue.values(), -1, 1);
            norms = copyAndValidate(norms, CharacterSocialNorm.values(), -1, 1);
            aptitudes = copyAndValidate(aptitudes, CharacterAptitude.values(), 0, 1);
        }
    }

    private CharacterPsychologyRules() { }

    private static <E extends Enum<E>> Map<E, Double> copyAndValidate(
            Map<E, Double> source, E[] values, double min, double max) {
        Objects.requireNonNull(source, "dimension map");
        if (values.length == 0) throw new IllegalStateException("Empty typed domain");
        EnumMap<E,Double> checked = new EnumMap<>(values[0].getDeclaringClass());
        for (E key : values) {
            Double value = source.get(key);
            if (value == null || !Double.isFinite(value) || value < min || value > max)
                throw new IllegalArgumentException("Missing or invalid psychology dimension " + key);
            checked.put(key, value);
        }
        if (source.size() != values.length)
            throw new IllegalArgumentException("Unknown or extra psychology dimensions");
        return Map.copyOf(checked);
    }

    /** Safe typed extraction from the existing legacy profile; no new NpcId. */
    public static Inputs legacy(NpcPersonality personality, CharacterProfile profile) {
        Objects.requireNonNull(profile, "existing character profile");
        EnumMap<CharacterDisposition,Double> d=new EnumMap<>(CharacterDisposition.class);
        EnumMap<CharacterValue,Double> v=new EnumMap<>(CharacterValue.class);
        EnumMap<CharacterSocialNorm,Double> n=new EnumMap<>(CharacterSocialNorm.class);
        EnumMap<CharacterAptitude,Double> a=new EnumMap<>(CharacterAptitude.class);
        for (CharacterDisposition x:CharacterDisposition.values()) d.put(x,profile.disposition(x));
        for (CharacterValue x:CharacterValue.values()) v.put(x,profile.characterValue(x));
        for (CharacterSocialNorm x:CharacterSocialNorm.values()) n.put(x,profile.socialNorm(x));
        for (CharacterAptitude x:CharacterAptitude.values()) a.put(x,profile.aptitude(x));
        return new Inputs(personality,d,v,n,a);
    }

    /** These formulas reproduce CharacterPsychologyService.derivedTendencies as of AV0. */
    public static Map<CharacterDerivedTendency, Double> evaluate(Inputs i) {
        Objects.requireNonNull(i, "psychology inputs");
        NpcPersonality p=i.personality();
        EnumMap<CharacterDerivedTendency,Double> out=new EnumMap<>(CharacterDerivedTendency.class);
        out.put(CharacterDerivedTendency.DECEITFUL_ADVANCEMENT, w(
            u(p.ambition()),.30,u(-p.honor()),.25,u(d(i,CharacterDisposition.DECEITFULNESS)),.25,
            a(i,CharacterAptitude.INTRIGUE),.20));
        out.put(CharacterDerivedTendency.INTERVENTION, w(
            u(p.courage()),.30,u(p.compassion()),.30,u(d(i,CharacterDisposition.EMPATHY)),.20,
            u(v(i,CharacterValue.DUTY)),.20));
        out.put(CharacterDerivedTendency.IMPULSIVE_CONFRONTATION, w(
            u(p.courage()),.25,u(-p.patience()),.25,u(d(i,CharacterDisposition.IMPULSIVITY)),.30,
            u(d(i,CharacterDisposition.AGGRESSION)),.20));
        double social = socialSusceptibility(i);
        out.put(CharacterDerivedTendency.PEER_CONFORMITY, cap(
            social*.60+u(d(i,CharacterDisposition.CONFORMITY))*.25+u(p.sociability())*.15));
        out.put(CharacterDerivedTendency.RISK_TAKING, w(
            u(p.courage()),.35,u(d(i,CharacterDisposition.RISK_TOLERANCE)),.40,
            u(d(i,CharacterDisposition.IMPULSIVITY)),.25));
        out.put(CharacterDerivedTendency.MERCY, w(
            u(p.compassion()),.30,u(d(i,CharacterDisposition.EMPATHY)),.20,
            u(d(i,CharacterDisposition.FORGIVENESS)),.20,u(v(i,CharacterValue.MERCY)),.30));
        out.put(CharacterDerivedTendency.VENGEANCE, w(
            u(d(i,CharacterDisposition.VINDICTIVENESS)),.35,
            u(v(i,CharacterValue.VENGEANCE)),.35,u(d(i,CharacterDisposition.AGGRESSION)),.20,
            u(-p.compassion()),.10));
        out.put(CharacterDerivedTendency.LOYAL_DUTY, w(
            u(p.honor()),.25,u(v(i,CharacterValue.DUTY)),.25,
            u(v(i,CharacterValue.HOUSE_LOYALTY)),.20,u(v(i,CharacterValue.FAMILY_LOYALTY)),.15,
            u(v(i,CharacterValue.DYNASTIC_LOYALTY)),.15));
        out.put(CharacterDerivedTendency.POLITICAL_ASSERTIVENESS, w(
            u(p.ambition()),.30,u(d(i,CharacterDisposition.ASSERTIVENESS)),.25,
            u(d(i,CharacterDisposition.DOMINANCE)),.20,u(v(i,CharacterValue.POWER_SEEKING)),.25));
        out.put(CharacterDerivedTendency.RELIGIOUS_RECEPTIVENESS, cap(
            u(v(i,CharacterValue.RELIGIOSITY))*.35+
            u(n(i,CharacterSocialNorm.RELIGIOUS))*.25+social*.25+
            a(i,CharacterAptitude.RELIGIOUS_STUDY)*.15));
        out.put(CharacterDerivedTendency.CULTURAL_OPENNESS, w(
            u(v(i,CharacterValue.CULTURAL_TOLERANCE)),.30,
            u(n(i,CharacterSocialNorm.COSMOPOLITAN)),.25,
            u(-n(i,CharacterSocialNorm.ISOLATIONIST)),.20,
            u(d(i,CharacterDisposition.CURIOSITY)),.25));
        out.put(CharacterDerivedTendency.MANIPULATION_SUSCEPTIBILITY, cap(
            social*.40+u(d(i,CharacterDisposition.GULLIBILITY))*.25+
            u(-d(i,CharacterDisposition.SUSPICIOUSNESS))*.20+
            u(-d(i,CharacterDisposition.INDEPENDENCE))*.15));
        if(out.size()!=CharacterDerivedTendency.values().length)
            throw new IllegalStateException("Derived tendency coverage is incomplete");
        return Map.copyOf(out);
    }

    public static double socialSusceptibility(Inputs i) {
        return cap(.50-d(i,CharacterDisposition.INDEPENDENCE)*.22+
            d(i,CharacterDisposition.CONFORMITY)*.18+
            d(i,CharacterDisposition.GULLIBILITY)*.15+
            d(i,CharacterDisposition.ADAPTABILITY)*.10+
            i.personality().sociability()*.05-
            d(i,CharacterDisposition.SUSPICIOUSNESS)*.08);
    }

    public static double persuasionResistance(Inputs i) {
        return cap(.50+d(i,CharacterDisposition.INDEPENDENCE)*.22+
            d(i,CharacterDisposition.DISCIPLINE)*.12+
            d(i,CharacterDisposition.EMOTIONAL_STABILITY)*.08+
            d(i,CharacterDisposition.SUSPICIOUSNESS)*.08-
            d(i,CharacterDisposition.GULLIBILITY)*.12-
            d(i,CharacterDisposition.CONFORMITY)*.10);
    }

    private static double d(Inputs i,CharacterDisposition x){return i.dispositions().get(x);}
    private static double v(Inputs i,CharacterValue x){return i.values().get(x);}
    private static double n(Inputs i,CharacterSocialNorm x){return i.norms().get(x);}
    private static double a(Inputs i,CharacterAptitude x){return i.aptitudes().get(x);}
    private static double cap(double x){
        if(!Double.isFinite(x))throw new IllegalArgumentException("Nonfinite tendency");
        return Math.max(0,Math.min(1,x));
    }
    private static double u(double x){return (Math.max(-1,Math.min(1,x))+1)/2;}
    private static double w(double... pairs){
        double sum=0,weight=0;
        for(int k=0;k<pairs.length;k+=2){sum+=cap(pairs[k])*pairs[k+1];weight+=pairs[k+1];}
        return cap(sum/weight);
    }
}

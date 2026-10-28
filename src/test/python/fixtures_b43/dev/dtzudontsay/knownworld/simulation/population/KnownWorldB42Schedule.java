package dev.dtzudontsay.knownworld.simulation.population;

/**
 * Pure, deterministic individual daily schedule, requiring no tick-per-person loop.
 * A schedule is a model, not evidence of actual resource production or witnessing.
 * All times are Minecraft 24,000-tick days. No effects are written to NpcRegistry.
 */
public final class KnownWorldB42Schedule {
    public static final int DAY = 24_000;
    public record Snapshot(String activity, boolean scheduledWorkday, int start, int end,
                           int depart, int returnEnd, int commuteTicks, long modeledWorkdays,
                           double practiceGain, String authority) { }
    private KnownWorldB42Schedule() { }

    /** Skill family selected from a real occupation; not a proof of work output. */
    public static String primarySkill(String occupation) {
        if(occupation==null)return null;
        String j=occupation.toLowerCase(java.util.Locale.ROOT);
        if(j.contains("farm")||j.contains("herd")||j.contains("shepherd")||j.contains("gardener"))return "agriculture";
        if(j.contains("blacksmith")||j.contains("smith")||j.contains("carpent")||j.contains("mason")||
           j.contains("weav")||j.contains("baker")||j.contains("potter"))return "crafting";
        if(j.contains("woodcut")||j.contains("hunter"))return "hunting";
        if(j.contains("fish")||j.contains("sailor")||j.contains("ship"))return "sailing";
        if(j.contains("guard")||j.contains("soldier")||j.contains("knight")||j.contains("warrior"))return "combat";
        if(j.contains("healer")||j.contains("physician")||j.contains("maester"))return "medicine";
        if(j.contains("priest")||j.contains("septon")||j.contains("sept")||j.contains("temple"))return "religious_study";
        if(j.contains("merchant")||j.contains("trader")||j.contains("market"))return "trade";
        if(j.contains("scribe")||j.contains("scholar")||j.contains("teacher"))return "learning";
        if(j.contains("administrator")||j.contains("steward")||j.contains("castellan"))return "stewardship";
        return "crafting";
    }

    public static Snapshot evaluate(String personId, String occupation, int birthYearAC,
                                    int yearAC, long day, int tick, long firstObservedDay,
                                    boolean hasWorkplace, int proxyDistance) {
        if(personId==null || personId.isBlank())throw new IllegalArgumentException("Missing person identity");
        if(tick<0 || tick>=DAY || day<0 || firstObservedDay<0 || proxyDistance<0)
            throw new IllegalArgumentException("Unsafe schedule time/distance");
        int age=Math.max(0,yearAC-birthYearAC);
        boolean child=age<16;
        boolean hasOccupation=occupation!=null && !occupation.isBlank() && !"Unrecorded".equals(occupation);
        boolean work=hasOccupation && !child && hasWorkplace;
        int hashed=personId.hashCode() & 0x7fffffff;
        int restDay=hashed%7;
        boolean onDuty=work && Math.floorMod(day+restDay,7)!=0;
        String job=occupation==null?"":occupation.toLowerCase(java.util.Locale.ROOT);
        int start=2_800,end=10_400;
        if(job.contains("farm")||job.contains("shepherd")||job.contains("herd")||job.contains("woodcut")||job.contains("hunter")||job.contains("fisher")) {
            start=1_900;end=9_300;
        } else if(job.contains("guard")||job.contains("watch")||job.contains("inn")||job.contains("tavern")) {
            start=6_000;end=15_000;
        } else if(job.contains("priest")||job.contains("sept")||job.contains("healer")||job.contains("maester")) {
            start=2_100;end=9_700;
        }
        // Conservative proxy: 4 blocks/s; 600 blocks ~= 150s, not path verified.
        int commute=Math.min(3_000,(int)Math.ceil(proxyDistance/4.0*20.0));
        int depart=Math.max(0,start-commute);
        int returned=Math.min(DAY-1,end+commute);
        String activity;
        if(tick>=19_000 || tick<800)activity="SLEEP";
        else if(!hasOccupation || child)activity=tick<11_000?"HOME_AND_CARE":"FREE_TIME";
        else if(!hasWorkplace)activity="UNVERIFIED_WORKPLACE_OR_HOME";
        else if(!onDuty)activity="REST_DAY";
        else if(tick>=depart && tick<start)activity="COMMUTING_TO_WORK";
        else if(tick>=start && tick<end)activity="MODELED_WORK_SHIFT";
        else if(tick>=end && tick<returned)activity="COMMUTING_HOME";
        else activity="HOME_AND_FREE_TIME";
        long elapsed=Math.max(0,day-firstObservedDay);
        long wholeWeeks=elapsed/7;long extra=elapsed%7;
        long worked=work?wholeWeeks*6:0;
        if(work)for(long i=0;i<extra;i++)if(Math.floorMod(firstObservedDay+wholeWeeks*7+i+restDay,7)!=0)worked++;
        // Initial proficiency + bounded learning potential. NEVER claims successful production.
        double gain=work?Math.min(0.18,worked*0.00012):0.0;
        return new Snapshot(activity,onDuty,start,end,depart,returned,commute,worked,gain,
                "MODELED_OFFSCREEN_ROUTINE_NOT_OBSERVED_EVENT");
    }
}

import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4BGoalRules;
public class Av4bSmoke {
  public static void main(String[] args) {
    var driven=new KnownWorldAv4BGoalRules.Inputs(.99,.15,.22,.91,.25,.80,.99,.25,.15,.50,.25);
    var social=new KnownWorldAv4BGoalRules.Inputs(.18,.99,.55,.30,.98,.20,.10,.50,.99,.80,.55);
    String person="b4:site_ne_007:00000296";
    var duty=new KnownWorldAv4BGoalRules.Context("MODELED_WORK_SHIFT",true,true,true,true,.7);
    var optional=new KnownWorldAv4BGoalRules.Context("FREE_TIME",true,false,false,true,.1);
    var visit=new KnownWorldAv4BGoalRules.Context("FREE_TIME",false,true,true,true,.05);
    var child=new KnownWorldAv4BGoalRules.Context("REST_DAY",false,false,false,false,.25);
    var rules=KnownWorldAv4BGoalRules.class;
    var a=KnownWorldAv4BGoalRules.evaluate(driven,duty,person,42);
    var b=KnownWorldAv4BGoalRules.evaluate(driven,optional,person,42);
    var c=KnownWorldAv4BGoalRules.evaluate(social,visit,person,42);
    var d=KnownWorldAv4BGoalRules.evaluate(driven,child,person,42);
    System.out.println("SCHEDULE="+a.goal()+" OPT_WORK="+b.goal()+" SOCIAL="+c.goal()+" CHILD="+d.goal());
    if(a.goal()!=KnownWorldAv4BGoalRules.Goal.SCHEDULED_WORK||
       b.goal()!=KnownWorldAv4BGoalRules.Goal.OPTIONAL_WORK||
       c.goal()!=KnownWorldAv4BGoalRules.Goal.SOCIAL_VISIT||
       d.scores().containsKey(KnownWorldAv4BGoalRules.Goal.OPTIONAL_WORK))throw new AssertionError("AV4B decisions regression");
    if(!b.equals(KnownWorldAv4BGoalRules.evaluate(driven,optional,person,42)))throw new AssertionError("non deterministic");
    System.out.println("AV4B PURE RULES SMOKE PASSED");
  }
}

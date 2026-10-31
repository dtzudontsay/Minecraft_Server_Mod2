package dev.dtzudontsay.knownworld.simulation.attributes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KnownWorldAv4BGoalRulesTest {
    private static final String PERSON="b4:site_ne_007:00000296";
    private static final KnownWorldAv4BGoalRules.Inputs DRIVEN=
        new KnownWorldAv4BGoalRules.Inputs(.99,.15,.22,.91,.25,.80,.99,.25,.15,.50,.25);
    private static final KnownWorldAv4BGoalRules.Inputs SOCIAL=
        new KnownWorldAv4BGoalRules.Inputs(.18,.99,.55,.30,.98,.20,.10,.50,.99,.80,.55);
    private static KnownWorldAv4BGoalRules.Context context(String phase,boolean work,
                                                               boolean publicPlace,boolean neighbor,
                                                               boolean adult,double recovery) {
        return new KnownWorldAv4BGoalRules.Context(phase,work,publicPlace,neighbor,adult,recovery);
    }
    @Test void strictScheduleCannotBeOverriddenByAmbition() {
        var d=KnownWorldAv4BGoalRules.evaluate(DRIVEN,context("MODELED_WORK_SHIFT",true,true,true,true,.7),PERSON,42);
        assertEquals(KnownWorldAv4BGoalRules.Goal.SCHEDULED_WORK,d.goal());
        assertEquals(1,d.scores().size());
        assertEquals(KnownWorldAv4BGoalRules.Goal.SCHEDULED_SLEEP,
            KnownWorldAv4BGoalRules.evaluate(DRIVEN,context("SLEEP",true,true,true,true,1),PERSON,42).goal());
    }
    @Test void lowCourageCanStillChooseOptionalWorkForHighReward() {
        var d=KnownWorldAv4BGoalRules.evaluate(DRIVEN,context("FREE_TIME",true,false,false,true,.1),PERSON,42);
        assertEquals(KnownWorldAv4BGoalRules.Goal.OPTIONAL_WORK,d.goal());
        assertFalse(d.scores().containsKey(KnownWorldAv4BGoalRules.Goal.SOCIAL_VISIT));
    }
    @Test void sociallyInclinedNpcSelectsFeasibleSocialGoal() {
        var d=KnownWorldAv4BGoalRules.evaluate(SOCIAL,context("FREE_TIME",false,true,true,true,.05),PERSON,42);
        assertEquals(KnownWorldAv4BGoalRules.Goal.SOCIAL_VISIT,d.goal());
    }
    @Test void absentResourcesAndChildrenAreGated() {
        var d=KnownWorldAv4BGoalRules.evaluate(DRIVEN,context("REST_DAY",false,false,false,false,.25),PERSON,42);
        assertFalse(d.scores().containsKey(KnownWorldAv4BGoalRules.Goal.OPTIONAL_WORK));
        assertFalse(d.scores().containsKey(KnownWorldAv4BGoalRules.Goal.PUBLIC_LEISURE));
        assertFalse(d.scores().containsKey(KnownWorldAv4BGoalRules.Goal.SOCIAL_VISIT));
        assertEquals("CHILD_NOT_ELIGIBLE",d.exclusions().get(KnownWorldAv4BGoalRules.Goal.OPTIONAL_WORK));
    }
    @Test void consistentAcrossReloadsAndRepeatedEvaluations() {
        var c=context("HOME_AND_FREE_TIME",true,true,true,true,.63);
        assertEquals(KnownWorldAv4BGoalRules.evaluate(DRIVEN,c,PERSON,700),
                     KnownWorldAv4BGoalRules.evaluate(DRIVEN,c,PERSON,700));
    }
    @Test void invalidProfileAndClockFailClosed() {
        assertThrows(IllegalArgumentException.class,()->
            new KnownWorldAv4BGoalRules.Inputs(Double.NaN,0,0,0,0,0,0,0,0,0,0));
        assertThrows(IllegalArgumentException.class,()->
            KnownWorldAv4BGoalRules.evaluate(DRIVEN,context("FREE_TIME",true,false,false,true,0),"legacy:npc",42));
        assertThrows(IllegalArgumentException.class,()->
            KnownWorldAv4BGoalRules.evaluate(DRIVEN,context("FREE_TIME",true,false,false,true,0),PERSON,-1));
        assertThrows(IllegalArgumentException.class,()->
            KnownWorldAv4BGoalRules.evaluate(DRIVEN,context("MODELED_WORK_SHIFT",false,false,false,true,0),PERSON,7));
    }
}

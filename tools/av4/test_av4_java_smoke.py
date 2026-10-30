#!/usr/bin/env python3
"""Compiles and executes REAL AV4 Java decision-policy class on JDK21."""
import argparse, shutil, subprocess, tempfile
from pathlib import Path

SRC=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv4DecisionRules.java')
PROBE=r'''
import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4DecisionRules;
import java.util.Map;
public class Av4Smoke {
    static void eq(boolean value,String why) {if(!value)throw new AssertionError(why);}
    public static void main(String[] args) {
        var low=new KnownWorldAv4DecisionRules.Inputs(0,0.1,0.1,0.1,1,0.1,0.1,0.1,0.1);
        var high=new KnownWorldAv4DecisionRules.Inputs(1,1,1,0.3,0,1,0.3,1,0.9);
        var urban=new KnownWorldAv4DecisionRules.Inputs(0.8,0.1,0.1,1,0,0.1,1,0.4,0.5);
        var intro=KnownWorldAv4DecisionRules.evaluate(low,"b4:site_ne_001:case1",109000);
        var social=KnownWorldAv4DecisionRules.evaluate(high,"b4:site_ne_001:case2",109000);
        var city=KnownWorldAv4DecisionRules.evaluate(urban,"b4:site_ne_001:case3",109000);
        eq(intro.action()==KnownWorldAv4DecisionRules.Action.HOUSEHOLD_ACTIVITY,"Introvert must prefer home");
        eq(social.action()==KnownWorldAv4DecisionRules.Action.NEIGHBOR_VISIT,"Social personality prefers visiting");
        eq(city.action()==KnownWorldAv4DecisionRules.Action.PUBLIC_LEISURE,"Urban/curious person prefers public place");
        eq(intro.equals(KnownWorldAv4DecisionRules.evaluate(low,"b4:site_ne_001:case1",109000)),"Daily decision must be deterministic");
        eq(!intro.equals(KnownWorldAv4DecisionRules.evaluate(low,"b4:site_ne_001:case1",109001)),"Day identity must enter decision");
        var inputs=KnownWorldAv4DecisionRules.read(Map.of("sociability",0.5,"empathy",0.5,"friendship_loyalty",0.5,
            "urban_norm",0.5,"isolationist_norm",0.5,"intervention",0.5,"cultural_openness",0.5,
            "peer_conformity",0.5,"risk_taking",0.5));
        eq(inputs.sociability()==0.5,"Input mapping");
        try{new KnownWorldAv4DecisionRules.Inputs(Double.NaN,0,0,0,0,0,0,0,0);throw new AssertionError("NaN allowed");}
        catch(IllegalArgumentException expected) { }
        try{KnownWorldAv4DecisionRules.evaluate(low,"b4:site_wrong",1);throw new AssertionError("Invalid id allowed");}
        catch(IllegalArgumentException expected) { }
        try{KnownWorldAv4DecisionRules.evaluate(low,"b4:site_ne_001:case1",-1);throw new AssertionError("Negative day allowed");}
        catch(IllegalArgumentException expected) { }
        System.out.println("AV4 JAVA SMOKE PASS: 3 distinct personality-based actions; deterministic days; finite/range/person validation");
    }
}
'''

def check(stage):
    if shutil.which('javac') is None or shutil.which('java') is None:
        raise AssertionError('JDK21 required on PATH for AV4 smoke')
    with tempfile.TemporaryDirectory(prefix='av4_java_') as temp:
        folder=Path(temp);(folder/'Av4Smoke.java').write_text(PROBE,encoding='utf-8')
        subprocess.run(['javac','--release','21','-d',temp,str(stage/SRC),str(folder/'Av4Smoke.java')],check=True)
        subprocess.run(['java','-cp',temp,'Av4Smoke'],check=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--repo');parser.add_argument('--stage',default='AV4_STAGE');args=parser.parse_args()
    check(Path(args.repo)/args.stage)

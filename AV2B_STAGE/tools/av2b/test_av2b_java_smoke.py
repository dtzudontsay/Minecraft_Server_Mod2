#!/usr/bin/env python3
"""Optional AV2B Java21 formula smoke test; portable stub compile, no user repo changes."""
import argparse,re,shutil,subprocess,tempfile
from pathlib import Path


def main(repo,stage):
    if not shutil.which('javac') or not shutil.which('java'):
        print('AV2B JAVA SMOKE SKIPPED: JDK unavailable; Gradle compile remains mandatory')
        return
    source=(stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/npc/psychology/CharacterPsychologyRules.java')
    common='dev/dtzudontsay/knownworld/simulation/npc/'
    with tempfile.TemporaryDirectory(prefix='av2b-javac-') as t:
        root=Path(t);rootdir=root/'src'
        def write(path,data):
            path=rootdir/path;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(data,encoding='utf8')
            return path
        paths=[write('dev/dtzudontsay/knownworld/simulation/npc/psychology/CharacterPsychologyRules.java',source.read_text(encoding='utf8'))]
        for name in ['CharacterAptitude','CharacterDisposition','CharacterValue','CharacterSocialNorm']:
            old=repo/f'src/main/java/dev/dtzudontsay/knownworld/simulation/npc/profile/{name}.java'
            paths.append(write(f'{common}profile/{name}.java',old.read_text(encoding='utf8')))
        for name in ['CharacterDerivedTendency']:
            old=repo/f'src/main/java/dev/dtzudontsay/knownworld/simulation/npc/psychology/{name}.java'
            paths.append(write(f'{common}psychology/{name}.java',old.read_text(encoding='utf8')))
        paths.append(write(f'{common}personality/NpcPersonality.java', '''package dev.dtzudontsay.knownworld.simulation.npc.personality;
public record NpcPersonality(double courage,double ambition,double compassion,double honor,double patience,double sociability) {}'''))
        paths.append(write(f'{common}profile/CharacterProfile.java', '''package dev.dtzudontsay.knownworld.simulation.npc.profile;
public class CharacterProfile {
 public double disposition(CharacterDisposition x){return 0;}
 public double characterValue(CharacterValue x){return 0;}
 public double socialNorm(CharacterSocialNorm x){return 0;}
 public double aptitude(CharacterAptitude x){return 0.5;}
}'''))
        paths.append(write('Smoke.java', '''import java.util.*;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.profile.*;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.*;
public class Smoke {
 static void require(boolean v,String message){if(!v)throw new AssertionError(message);}
 static CharacterPsychologyRules.Inputs neutral(){
  var d=new EnumMap<CharacterDisposition,Double>(CharacterDisposition.class);
  var v=new EnumMap<CharacterValue,Double>(CharacterValue.class);
  var n=new EnumMap<CharacterSocialNorm,Double>(CharacterSocialNorm.class);
  var a=new EnumMap<CharacterAptitude,Double>(CharacterAptitude.class);
  for(var x:CharacterDisposition.values())d.put(x,0.0);
  for(var x:CharacterValue.values())v.put(x,0.0);
  for(var x:CharacterSocialNorm.values())n.put(x,0.0);
  for(var x:CharacterAptitude.values())a.put(x,0.5);
  return new CharacterPsychologyRules.Inputs(new NpcPersonality(0,0,0,0,0,0),d,v,n,a);
 }
 public static void main(String[] args) {
  var i=neutral();var scores=CharacterPsychologyRules.evaluate(i);
  require(scores.size()==12,"missing axes");
  for(var x:scores.entrySet())require(x.getValue()>=0&&x.getValue()<=1,"out-of-range "+x.getKey());
  require(Math.abs(scores.get(CharacterDerivedTendency.MERCY)-0.5)<1e-10,"neutral mercy");
  require(Math.abs(scores.get(CharacterDerivedTendency.PEER_CONFORMITY)-0.5)<1e-10,"neutral conformity");
  require(Math.abs(CharacterPsychologyRules.socialSusceptibility(i)-0.5)<1e-10,"neutral susceptibility");
  var traits=new EnumMap<CharacterDisposition,Double>(i.dispositions());traits.put(CharacterDisposition.FORGIVENESS,1.0);
  var stronger=new CharacterPsychologyRules.Inputs(i.personality(),traits,i.values(),i.norms(),i.aptitudes());
  require(CharacterPsychologyRules.evaluate(stronger).get(CharacterDerivedTendency.MERCY)>scores.get(CharacterDerivedTendency.MERCY),"forgiveness does not affect mercy");
  require(CharacterPsychologyRules.legacy(i.personality(),new CharacterProfile())!=null,"legacy adapter");
  System.out.println("AV2B JAVA SMOKE PASS: 12 derived tendencies; typed legacy adapter; bounded psychological readouts");
 }
}'''))
        # Compile the Fabric/Gson-facing adapter against minimal API signatures
        # to catch type/Java syntax issues before the user's full Gradle build.
        bridge_source=stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2BPsychologyBridge.java'
        paths.append(write('dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2BPsychologyBridge.java',bridge_source.read_text(encoding='utf8')))
        paths.append(write('com/google/gson/JsonElement.java',"""package com.google.gson;
public class JsonElement {
 public boolean isJsonPrimitive(){return true;}
 public JsonPrimitive getAsJsonPrimitive(){return new JsonPrimitive();}
 public double getAsDouble(){return 0.5;}
 public String getAsString(){return "";}
}"""))
        paths.append(write('com/google/gson/JsonPrimitive.java',"""package com.google.gson;
public class JsonPrimitive extends JsonElement {
 public boolean isString(){return true;}
 public boolean isNumber(){return true;}
}"""))
        paths.append(write('com/google/gson/JsonObject.java',"""package com.google.gson;
public class JsonObject extends JsonElement {
 public JsonElement get(String k){return new JsonPrimitive();}
 public JsonObject getAsJsonObject(String k){return new JsonObject();}
 public void add(String k,JsonObject v){}
 public void addProperty(String k,String v){}
 public void addProperty(String k,double v){}
 public void remove(String k){}
}"""))
        paths.append(write('net/minecraft/network/chat/Component.java',"""package net.minecraft.network.chat;
public class Component { public static Component literal(String x){return new Component();} }
"""))
        paths.append(write('net/minecraft/commands/CommandSourceStack.java',"""package net.minecraft.commands;
public class CommandSourceStack {
 public void sendSuccess(java.util.function.Supplier<net.minecraft.network.chat.Component> s, boolean b){}
}"""))
        paths.append(write('net/minecraft/commands/Commands.java',"""package net.minecraft.commands;
public class Commands {
 public static Node literal(String x){return new Node();}
 public static final class Context {public CommandSourceStack getSource(){return new CommandSourceStack();}}
 @FunctionalInterface public interface Exec {int run(Context ctx);}
 public static final class Node {
  public Node then(Node n){return this;}
  public Node executes(Exec f){return this;}
 }
}"""))
        paths.append(write('net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback.java',"""package net.fabricmc.fabric.api.command.v2;
import net.minecraft.commands.Commands;
public interface CommandRegistrationCallback {
 Event EVENT = new Event();
 class Dispatcher {public void register(Commands.Node n){}}
 @FunctionalInterface interface Callback {void register(Dispatcher d,Object a,Object b);}
 class Event {public void register(Callback cb){}}
}"""))
        build=root/'classes';build.mkdir()
        subprocess.run(['javac','-d',str(build),*(str(x) for x in paths)],check=True,capture_output=True,text=True)
        subprocess.run(['java','-cp',str(build),'Smoke'],check=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--repo',required=True);p.add_argument('--stage',default='AV2B_STAGE');a=p.parse_args()
    main(Path(a.repo),Path(a.repo)/a.stage)

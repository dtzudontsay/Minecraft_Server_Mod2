#!/usr/bin/env python3
"""Compile AV3 source with Java 21 and functional, minimal JSON/class stubs."""
import argparse,csv,shutil,subprocess,tempfile
from pathlib import Path

def run(repo,stage):
    if not shutil.which('javac') or not shutil.which('java'):
        print('AV3 Java smoke SKIPPED: JDK missing; full Gradle compile required');return
    prefix='dev/dtzudontsay/knownworld/simulation/'
    src=stage/'src/main/java'/prefix/'attributes'
    with tempfile.TemporaryDirectory(prefix='av3-j21-') as td:
        root=Path(td);paths=[]
        def write(name,code):
            path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(code,encoding='utf-8');paths.append(path)
        for name in ('KnownWorldAv3RegionalPriors','KnownWorldAv3Formation'):
            write(prefix+'attributes/'+name+'.java',(src/(name+'.java')).read_text(encoding='utf-8'))
        for name,code in {
          'JsonElement.java':'''package com.google.gson;
public class JsonElement {
 public JsonObject getAsJsonObject(){return (JsonObject)this;}
 public JsonArray getAsJsonArray(){return (JsonArray)this;}
 public JsonPrimitive getAsJsonPrimitive(){return (JsonPrimitive)this;}
 public boolean isJsonPrimitive(){return false;}public double getAsDouble(){throw new UnsupportedOperationException();}
 public String getAsString(){throw new UnsupportedOperationException();}
 public JsonElement deepCopy(){return this;}}
''',
          'JsonPrimitive.java':'''package com.google.gson;
public class JsonPrimitive extends JsonElement {
 private final Object value;public JsonPrimitive(Object v){value=v;}
 public boolean isJsonPrimitive(){return true;}public boolean isNumber(){return value instanceof Number;}
 public boolean isString(){return value instanceof String;}
 public double getAsDouble(){return ((Number)value).doubleValue();}public String getAsString(){return value.toString();}
 public JsonElement deepCopy(){return new JsonPrimitive(value);}}
''',
          'JsonObject.java':'''package com.google.gson;
public class JsonObject extends JsonElement {
 private final java.util.LinkedHashMap<String,JsonElement> m=new java.util.LinkedHashMap<>();
 public void add(String k,JsonElement v){m.put(k,v);}public void addProperty(String k,String v){add(k,new JsonPrimitive(v));}
 public void addProperty(String k,int v){add(k,new JsonPrimitive(v));}public void addProperty(String k,double v){add(k,new JsonPrimitive(v));}
 public JsonElement get(String k){return m.get(k);}public JsonObject getAsJsonObject(String k){return (JsonObject)m.get(k);}
 public JsonArray getAsJsonArray(String k){return (JsonArray)m.get(k);}
 public java.util.Set<java.util.Map.Entry<String,JsonElement>> entrySet(){return m.entrySet();}
 public boolean has(String k){return m.containsKey(k);}
 public JsonObject deepCopy(){JsonObject n=new JsonObject();for(var e:m.entrySet())n.add(e.getKey(),e.getValue().deepCopy());return n;}
}
''',
          'JsonArray.java':'''package com.google.gson;
public class JsonArray extends JsonElement implements Iterable<JsonElement> {
 private final java.util.ArrayList<JsonElement> data=new java.util.ArrayList<>();
 public void add(JsonElement e){data.add(e);}public void add(String s){add(new JsonPrimitive(s));}
 public java.util.Iterator<JsonElement> iterator(){return data.iterator();}
 public JsonArray deepCopy(){JsonArray n=new JsonArray();for(JsonElement e:data)n.add(e.deepCopy());return n;}
}
''',
          'JsonParser.java':'''package com.google.gson;
public class JsonParser {public static JsonArray parseReader(java.io.Reader r){return new JsonArray();}}
'''}.items():write('com/google/gson/'+name,code)
        specs=list(csv.DictReader((stage/'docs/architecture/AV3_RUNTIME_AXIS_INVENTORY.csv').open(encoding='utf-8-sig')))
        enums={}
        for row in specs:enums.setdefault(row['Java enum'],[]).append(row['Canonical ID'])
        for name,values in enums.items():
            write(prefix+'npc/profile/'+name+'.java','package dev.dtzudontsay.knownworld.simulation.npc.profile; public enum '+name+' {'+','.join(values)+';}')
        write(prefix+'attributes/KnownWorldAv2DChangePolicy.java','''package dev.dtzudontsay.knownworld.simulation.attributes;
public class KnownWorldAv2DChangePolicy {
 public static String siteOf(String id){if(!id.matches("b4:site_[a-z][a-z0-9_]+:[a-zA-Z0-9:_-]+"))throw new IllegalArgumentException("Invalid ID");return id.substring(3,id.indexOf(':',3));}}
''')
        write('net/minecraft/network/chat/Component.java','''package net.minecraft.network.chat;
public class Component{public static Component literal(String s){return new Component();}}
''')
        write('net/minecraft/commands/Commands.java','''package net.minecraft.commands;
public class Commands {
 public static Node literal(String s){return new Node();}
 public static class Ctx {public Source getSource(){return new Source();}}
 public static class Source {
 public void sendSuccess(java.util.function.Supplier<net.minecraft.network.chat.Component> s,boolean b){}
 public void sendFailure(net.minecraft.network.chat.Component s){}
 }
 @FunctionalInterface public interface Runner {int run(Ctx c);}
 public static class Node {public Node then(Node o){return this;}public Node executes(Runner f){return this;}}
}
''')
        write('net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback.java','''package net.fabricmc.fabric.api.command.v2;
public class CommandRegistrationCallback {
 public static final Event EVENT=new Event();
 @FunctionalInterface public interface Register {void accept(Dispatcher d,Object r,Object e);}
 public static class Event {public void register(Register cb){}}
 public static class Dispatcher {public void register(net.minecraft.commands.Commands.Node n){}}
}
''')
        write('Smoke.java','''import com.google.gson.*;
import dev.dtzudontsay.knownworld.simulation.attributes.*;
import dev.dtzudontsay.knownworld.simulation.npc.profile.*;
import java.lang.reflect.Field;
class Smoke {
 static void require(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 static JsonObject obj(String key,double v){JsonObject o=new JsonObject();o.addProperty(key,v);return o;}
 static JsonObject axis(String kind,double v){JsonObject o=new JsonObject();o.add(kind,obj("value",v));return o;}
 static JsonObject exposure(String kind,double v){JsonObject o=new JsonObject();o.add(kind,obj("weight",v));return o;}
 static JsonObject profile(){
  JsonObject s=new JsonObject();
  for(var entry:new Object[][] { {"characterValues", CharacterValue.values()}, {"socialNorms",CharacterSocialNorm.values()},
      {"dispositions",CharacterDisposition.values()}, {"aptitudes",CharacterAptitude.values()} }) {
   JsonObject group=new JsonObject();String field=(String)entry[0];
   for(Object axis:(Object[])entry[1]){
    String key=((Enum<?>)axis).name().toLowerCase(java.util.Locale.ROOT);
    if(field.equals("aptitudes")&&key.equals("languages"))key="language";
    group.addProperty(key,field.equals("aptitudes")?0.45:0.0);
   }
   s.add(field,group);
  }
  return s;
 }
 static JsonArray catalog(String... ids){JsonArray a=new JsonArray();for(String id:ids){JsonObject row=new JsonObject();row.addProperty("id",id);a.add(row);}return a;}
 public static void main(String[] args)throws Exception {
  JsonObject r=new JsonObject();r.addProperty("locationId","the_north");
  r.add("cultures",exposure("northman",0.9));r.add("religions",exposure("old_gods",0.8));
  r.add("values",axis("TRADITIONALISM",0.8));r.add("socialNorms",axis("RURAL",0.6));
  JsonObject correction=new JsonObject();correction.addProperty("locationId","the_north");
  correction.add("values",axis("TRADITIONALISM",0.5));
  JsonArray base=new JsonArray();base.add(r);JsonArray ext=new JsonArray();ext.add(correction);
  var ix=KnownWorldAv3RegionalPriors.build(catalog("northman","andal"),catalog("old_gods","rhllor"),base,ext);
  require(ix.locations()==1,"extension should not double-count location");
  require(ix.cultureCount()==2&&ix.religionCount()==2,"catalog count");
  require(Math.abs(ix.culture("northman").values().get("TRADITIONALISM")-0.5)<1e-9,"extension override");
  require(!ix.culture("andal").hasEvidence(),"unsupported culture must remain neutral");
  Field field=KnownWorldAv3RegionalPriors.class.getDeclaredField("cached");field.setAccessible(true);field.set(null,ix);
  String id="b4:site_ne_001:person_0001";
  JsonObject a=profile(),b=profile();
  KnownWorldAv3Formation.applyToSheet(a,"site_ne_001",id,"northman","old_gods","house_5","mother_1","father_1",270);
  KnownWorldAv3Formation.applyToSheet(b,"site_ne_001",id,"northman","old_gods","house_5","mother_1","father_1",270);
  require("APPLIED_STARTING_PRIORS".equals(a.get("av3State").getAsString()),"apply status");
  require(a.getAsJsonObject("characterValues").get("traditionalism").getAsDouble()>0.08,"culture prior");
  require(a.getAsJsonObject("characterValues").get("traditionalism").getAsDouble()==b.getAsJsonObject("characterValues").get("traditionalism").getAsDouble(),"determinism");
  require(KnownWorldAv3Formation.familyVariance("same-family","aptitudes","learning")==KnownWorldAv3Formation.familyVariance("same-family","aptitudes","learning"),"family determinism");
  require(KnownWorldAv3Formation.familyVariance("different-family","aptitudes","learning")!=KnownWorldAv3Formation.familyVariance("same-family","aptitudes","learning"),"family variation");
  JsonObject bad=profile();bad.getAsJsonObject("dispositions").addProperty("pride",3.1);
  KnownWorldAv3Formation.applyToSheet(bad,"site_ne_001",id,"northman","old_gods","house_5",null,null,270);
  require("BLOCKED_UNVERIFIED_STARTING_PRIORS".equals(bad.get("av3State").getAsString()),"fails closed");
  require(bad.getAsJsonObject("characterValues").get("traditionalism").getAsDouble()==0.0,"atomic profile apply");
  JsonObject wrong=profile();KnownWorldAv3Formation.applyToSheet(wrong,"site_sw_001",id,"northman","old_gods",null,null,null,270);
  require("BLOCKED_UNVERIFIED_STARTING_PRIORS".equals(wrong.get("av3State").getAsString()),"foreign person/site rejected");
  require(CharacterValue.values().length==33&&CharacterAptitude.values().length==20,"canonical enum coverage");
  System.out.println("AV3 JAVA SMOKE PASS: Java21; catalog+extension merge; source-derived prior; family determinism; 104 axes; atomic failure; person/site guard");
 }}
''')
        classes=root/'classes';classes.mkdir()
        c=subprocess.run(['javac','-encoding','UTF-8','--release','21','-d',str(classes),*[str(x) for x in paths]],capture_output=True,text=True)
        if c.returncode:raise RuntimeError('AV3 Java smoke compilation failed:\n'+c.stderr)
        t=subprocess.run(['java','-cp',str(classes),'Smoke'],capture_output=True,text=True)
        if t.returncode:raise RuntimeError('AV3 Java smoke failed:\n'+t.stdout+'\n'+t.stderr)
        print(t.stdout.strip())

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--repo',required=True);p.add_argument('--stage',default='AV3_STAGE');args=p.parse_args()
    repo=Path(args.repo);run(repo,repo/args.stage)

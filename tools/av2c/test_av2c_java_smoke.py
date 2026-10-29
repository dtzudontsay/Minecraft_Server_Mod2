#!/usr/bin/env python3
"""Compile both real AV2C Java classes under Java 21 using API stubs; exercise event IDs."""
import argparse,shutil,subprocess,tempfile
from pathlib import Path

def run(repo,stage):
    if not shutil.which('javac') or not shutil.which('java'):
        print('AV2C JAVA SMOKE SKIPPED: javac unavailable; full Gradle compile mandatory');return
    base='dev/dtzudontsay/knownworld/simulation/attributes/'
    src=stage/'src/main/java'/base
    with tempfile.TemporaryDirectory(prefix='av2c-java-') as temp:
        root=Path(temp);paths=[]
        def write(name,code):
            f=root/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(code,encoding='utf8');paths.append(f)
        for name in ('KnownWorldAv2CEventPolicy.java','KnownWorldAv2CExperienceJournal.java'):
            write(base+name,(src/name).read_text(encoding='utf8'))
        # Gson API shape: enough to validate source calls, never used at runtime in smoke.
        write('com/google/gson/JsonElement.java','''package com.google.gson;
public class JsonElement {public JsonObject getAsJsonObject(){return new JsonObject();}
 public JsonPrimitive getAsJsonPrimitive(){return new JsonPrimitive();}
 public boolean isJsonPrimitive(){return true;}
 public String getAsString(){return "";}public long getAsLong(){return 0;}
 public JsonElement deepCopy(){return this;}}
''')
        write('com/google/gson/JsonPrimitive.java','''package com.google.gson;
public class JsonPrimitive extends JsonElement {public boolean isNumber(){return true;}
public boolean isString(){return true;}}
''')
        write('com/google/gson/JsonArray.java','''package com.google.gson;
public class JsonArray extends JsonElement implements Iterable<JsonElement>{
public int size(){return 0;}public boolean isEmpty(){return false;}
public JsonElement get(int n){return new JsonObject();}
public void add(JsonElement v){}public java.util.Iterator<JsonElement> iterator(){return java.util.List.<JsonElement>of().iterator();}}
''')
        write('com/google/gson/JsonObject.java','''package com.google.gson;
public class JsonObject extends JsonElement {
public void add(String key,JsonElement value){}
public void addProperty(String key,long value){}public void addProperty(String key,String value){}
public JsonElement get(String key){return new JsonElement();}
public JsonObject getAsJsonObject(String k){return new JsonObject();}
public JsonArray getAsJsonArray(String k){return new JsonArray();}
public JsonObject deepCopy(){return new JsonObject();}}
''')
        write('com/google/gson/JsonParser.java','''package com.google.gson;
public class JsonParser {public static JsonElement parseString(String x){return new JsonObject();}}
''')
        write('com/mojang/brigadier/arguments/StringArgumentType.java','''package com.mojang.brigadier.arguments;
public class StringArgumentType {public static StringArgumentType string(){return new StringArgumentType();}
public static String getString(Object ctx,String key){return "";}}
''')
        write('dev/dtzudontsay/knownworld/KnownWorld.java','''package dev.dtzudontsay.knownworld;
public class KnownWorld {public static final Logger LOGGER=new Logger();
public static class Logger {public void error(String msg,Exception ex){}}}
''')
        write('dev/dtzudontsay/knownworld/simulation/NpcSimulation.java','''package dev.dtzudontsay.knownworld.simulation;
public class NpcSimulation {public static NpcSimulation getNullable(){return null;}
public net.minecraft.server.MinecraftServer server(){return null;}}
''')
        write('net/minecraft/server/MinecraftServer.java','''package net.minecraft.server;
public class MinecraftServer {public java.nio.file.Path getWorldPath(net.minecraft.world.level.storage.LevelResource r){return java.nio.file.Path.of(".");}}
''')
        write('net/minecraft/world/level/storage/LevelResource.java','''package net.minecraft.world.level.storage;
public class LevelResource {public static final LevelResource ROOT=new LevelResource();}
''')
        write('net/minecraft/network/chat/Component.java','''package net.minecraft.network.chat;
public class Component {public static Component literal(String msg){return new Component();}}
''')
        write('net/minecraft/commands/CommandSourceStack.java','''package net.minecraft.commands;
public class CommandSourceStack {
public net.minecraft.server.MinecraftServer getServer(){return new net.minecraft.server.MinecraftServer();}
public void sendSuccess(java.util.function.Supplier<net.minecraft.network.chat.Component> msg,boolean b){}
public void sendFailure(net.minecraft.network.chat.Component msg){}}
''')
        write('net/minecraft/commands/Commands.java','''package net.minecraft.commands;
public class Commands {
public static Node literal(String x){return new Node();}
public static Node argument(String x,Object y){return new Node();}
public static final class Ctx {public CommandSourceStack getSource(){return new CommandSourceStack();}}
@FunctionalInterface public interface Ex {int invoke(Ctx ctx);}
public static final class Node {public Node then(Node n){return this;}
public Node executes(Ex e){return this;}}}
''')
        write('net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback.java','''package net.fabricmc.fabric.api.command.v2;
public class CommandRegistrationCallback {
public static final Event EVENT=new Event();
@FunctionalInterface public interface Callback {void register(Dispatcher d,Object r,Object e);}
public static final class Event {public void register(Callback c){}}
public static final class Dispatcher {public void register(net.minecraft.commands.Commands.Node n){}}}
''')
        write('net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents.java','''package net.fabricmc.fabric.api.event.lifecycle.v1;
public class ServerLifecycleEvents {
public static final Event SERVER_STOPPED=new Event();
public static class Event {public void register(java.util.function.Consumer<net.minecraft.server.MinecraftServer> f){}}}
''')
        write('Smoke.java','''import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2CEventPolicy;
class Smoke {
 static void check(boolean v){if(!v)throw new AssertionError();}
 public static void main(String[] args){
  var a=KnownWorldAv2CEventPolicy.observedAtWork("b4:site_ne_001:person_0001","site_ne_001:work:FARM:0001",12);
  var b=KnownWorldAv2CEventPolicy.observedAtWork("b4:site_ne_001:person_0001","site_ne_001:work:FARM:0001",12);
  check(a.eventId().equals(b.eventId()));check(a.eventId().length()==64);check(a.siteId().equals("site_ne_001"));
  check(KnownWorldAv2CEventPolicy.RECENT_LIMIT==32);
  boolean rejected=false;try{KnownWorldAv2CEventPolicy.observedAtWork("b4:site_ne_001:person_0001","site_nw_001:work:FARM:0001",12);}catch(IllegalArgumentException ex){rejected=true;}check(rejected);
  rejected=false;try{KnownWorldAv2CEventPolicy.observedAtWork("b4:site_ne_001:person_0001","site_ne_001:work:FARM:0001",-1);}catch(IllegalArgumentException ex){rejected=true;}check(rejected);
  rejected=false;try{KnownWorldAv2CEventPolicy.siteOf("b4:../other");}catch(IllegalArgumentException ex){rejected=true;}check(rejected);
  System.out.println("AV2C JAVA SMOKE PASS: policy identity, deterministic IDs, foreign/negative rejection, full Java sources compiled");
 }}
''')
        classes=root/'classes';classes.mkdir()
        cmd=['javac','-encoding','UTF-8','--release','21','-d',str(classes),*(str(f) for f in paths)]
        task=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
        if task.returncode:raise RuntimeError('AV2C Java stub compile failed:\n'+task.stderr)
        subprocess.run(['java','-cp',str(classes),'Smoke'],check=True)

if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--repo',required=True);a.add_argument('--stage',default='AV2C_STAGE')
    x=a.parse_args();run(Path(x.repo),Path(x.repo)/x.stage)

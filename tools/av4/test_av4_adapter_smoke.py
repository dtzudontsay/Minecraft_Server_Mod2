#!/usr/bin/env python3
"""Compile the actual Minecraft-facing adapter against typed API stubs on JDK21.
The full mod still requires the local Gradle build.
"""
import argparse, subprocess, tempfile
from pathlib import Path
STUBS={
'com/google/gson/JsonElement.java':'''package com.google.gson; public class JsonElement { public boolean isJsonPrimitive(){return true;} public JsonPrimitive getAsJsonPrimitive(){return new JsonPrimitive();} public String getAsString(){return "";} public double getAsDouble(){return .5;} }''',
'com/google/gson/JsonPrimitive.java':'''package com.google.gson; public class JsonPrimitive extends JsonElement {public boolean isNumber(){return true;}}''',
'com/google/gson/JsonObject.java':'''package com.google.gson; public class JsonObject {public JsonElement get(String x){return new JsonElement();}public JsonObject getAsJsonObject(String x){return new JsonObject();}public void add(String k,JsonObject v){} public void addProperty(String k,String v){}public void addProperty(String k,long v){} public void addProperty(String k,double v){} }''',
'com/mojang/brigadier/arguments/StringArgumentType.java':'''package com.mojang.brigadier.arguments; import net.minecraft.commands.Commands; public class StringArgumentType {public static Object string(){return null;} public static String getString(Commands.Context c,String n){return "";} }''',
'net/minecraft/network/chat/Component.java':'''package net.minecraft.network.chat; public class Component{public static Component literal(String s){return new Component();}}''',
'net/minecraft/commands/Commands.java':'''package net.minecraft.commands; import java.util.function.ToIntFunction; import java.util.function.Supplier; import net.minecraft.network.chat.Component;public class Commands {public static Builder literal(String k){return new Builder();}public static Builder argument(String k,Object a){return new Builder();}public static class Builder {public Builder then(Builder b){return this;}public Builder executes(ToIntFunction<Context> c){return this;}}public static class Context {public Source getSource(){return new Source();}}public static class Source {public void sendSuccess(Supplier<Component> s,boolean b){} }}''',
'net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback.java':'''package net.fabricmc.fabric.api.command.v2;import net.minecraft.commands.Commands; public interface CommandRegistrationCallback {Event EVENT=new Event();void register(Dispatcher d,Object r,Object e);class Dispatcher{public void register(Commands.Builder b){}}class Event{public void register(CommandRegistrationCallback a){}}}''',
'net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents.java':'''package net.fabricmc.fabric.api.event.lifecycle.v1; import java.util.function.Consumer;public class ServerLifecycleEvents{public static Event SERVER_STOPPED=new Event();public static class Event{public void register(Consumer<Object> c){}}}''',
'dev/dtzudontsay/knownworld/simulation/NpcSimulation.java':'''package dev.dtzudontsay.knownworld.simulation;public class NpcSimulation{public static NpcSimulation getNullable(){return new NpcSimulation();}public Calendar campaignCalendar(){return new Calendar();}public static class Calendar{public long absoluteDay(){return 108000;}}}''',
'dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java':'''package dev.dtzudontsay.knownworld.simulation.population;import com.google.gson.JsonObject;public class KnownWorldB41Activation{public static JsonObject av4ProfileFor(String p,String s){return new JsonObject();}}''',
'dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2DChangePolicy.java':'''package dev.dtzudontsay.knownworld.simulation.attributes;public class KnownWorldAv2DChangePolicy{public static String siteOf(String s){return "site_ne_001";}}''',
}

def check(stage):
    with tempfile.TemporaryDirectory(prefix='av4_adapter_') as temp:
        base=Path(temp)
        sources=[]
        for path,code in STUBS.items():
            p=base/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(code,encoding='utf-8');sources.append(str(p))
        root=stage/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes'
        for name in ('KnownWorldAv4DecisionRules.java','KnownWorldAv4Decisions.java'):
            sources.append(str(root/name))
        subprocess.run(['javac','--release','21','-d',temp,*sources],check=True)
    print('AV4 ADAPTER COMPILE PASS (JDK21 typed stubs; real Fabric compile still required)')
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--repo',required=True);p.add_argument('--stage',default='AV4_STAGE');a=p.parse_args();check(Path(a.repo)/a.stage)

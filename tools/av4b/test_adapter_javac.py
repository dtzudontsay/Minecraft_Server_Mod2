#!/usr/bin/env python3
"""Isolated JDK compile contract for AV4B adapter + pure goal rules.
Not a substitute for Minecraft/Fabric Gradle compileJava on the user's machine.
"""
from pathlib import Path
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parent
SOURCE=ROOT/'payload/src/main/java/dev/dtzudontsay/knownworld/simulation/attributes'
STUBS={
'com/google/gson/JsonElement.java': '''package com.google.gson; public class JsonElement { public boolean isJsonPrimitive(){return true;} public JsonPrimitive getAsJsonPrimitive(){return new JsonPrimitive();} public String getAsString(){return "";} public double getAsDouble(){return 0.0;} }''',
'com/google/gson/JsonPrimitive.java': '''package com.google.gson; public class JsonPrimitive extends JsonElement { public boolean isNumber(){return true;} }''',
'com/google/gson/JsonObject.java': '''package com.google.gson; public class JsonObject extends JsonElement { public JsonElement get(String n){return new JsonElement();} public JsonObject getAsJsonObject(String n){return new JsonObject();} public void addProperty(String k,String v){} public void addProperty(String k,double v){} public void addProperty(String k,long v){} public void add(String k,JsonObject v){} }''',
'com/mojang/brigadier/arguments/StringArgumentType.java': '''package com.mojang.brigadier.arguments; import net.minecraft.commands.CommandSourceStack;import net.minecraft.commands.CommandContext;public class StringArgumentType {public static StringArgumentType string(){return new StringArgumentType();} public static String getString(CommandContext c,String name){return "";} }''',
'net/minecraft/commands/CommandSourceStack.java': '''package net.minecraft.commands;import java.util.function.Supplier;import net.minecraft.network.chat.Component;public class CommandSourceStack {public void sendSuccess(Supplier<Component> msg,boolean flag){} }''',
'net/minecraft/commands/CommandContext.java': '''package net.minecraft.commands;public class CommandContext {public CommandSourceStack getSource(){return new CommandSourceStack();}}''',
'net/minecraft/commands/Commands.java': '''package net.minecraft.commands;public class Commands {public static Node literal(String s){return new Node();} public static Node argument(String s,Object type){return new Node();} public static class Node { public Node then(Node child){return this;} public Node executes(java.util.function.ToIntFunction<CommandContext> cmd){return this;} }}''',
'net/minecraft/network/chat/Component.java': '''package net.minecraft.network.chat; public class Component {public static Component literal(String s){return new Component();} }''',
'net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback.java': '''package net.fabricmc.fabric.api.command.v2;import net.minecraft.commands.Commands;public interface CommandRegistrationCallback {void register(Dispatcher d,Object registry,Object environment); public static final Event EVENT=new Event();public static class Event {public void register(CommandRegistrationCallback callback){} }public static class Dispatcher { public void register(Commands.Node node){} }}''',
'net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents.java': '''package net.fabricmc.fabric.api.event.lifecycle.v1;public class ServerLifecycleEvents {public static final Event SERVER_STOPPED=new Event();public interface Callback {void run(Object server);}public static class Event {public void register(Callback cb){} }}''',
'dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java': '''package dev.dtzudontsay.knownworld.simulation.population;import com.google.gson.JsonObject;public class KnownWorldB41Activation {public static String av2VisibleSite(String s){return s;}public static JsonObject av4ProfileFor(String id,String site){return new JsonObject();}}''',
'dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2DChangePolicy.java': '''package dev.dtzudontsay.knownworld.simulation.attributes;public class KnownWorldAv2DChangePolicy {public static String siteOf(String s){return s;}}''',
}

def main():
    with tempfile.TemporaryDirectory(prefix='av4b_adapter_') as td:
        folder=Path(td);files=[]
        for name,code in STUBS.items():
            dest=folder/name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(code);files.append(str(dest))
        files += [str(SOURCE/'KnownWorldAv4BGoalRules.java'),str(SOURCE/'KnownWorldAv4BGoals.java')]
        dest=folder/'classes';dest.mkdir()
        proc=subprocess.run(['javac','--release','21','-d',str(dest),*files],capture_output=True,text=True)
        if proc.returncode:
            print(proc.stdout,proc.stderr);raise SystemExit(proc.returncode)
        print('AV4B adapter JDK21 javac compilation PASSED (isolated Minecraft API stubs)')

if __name__=='__main__': main()

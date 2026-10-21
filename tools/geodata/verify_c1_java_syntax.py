#!/usr/bin/env python3
"""Local syntax/API-shape smoke test ONLY; actual MC/Fabric Gradle build required."""
from pathlib import Path
import tempfile,subprocess
ROOT=Path(__file__).resolve().parents[2]
TARGET=ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/integration/KnownWorldSettlementPresets.java'
STUBS={
 'com/google/gson/JsonElement.java':'package com.google.gson;public class JsonElement{public JsonObject getAsJsonObject(){return null;}public String getAsString(){return null;}public int getAsInt(){return 0;}public boolean getAsBoolean(){return false;}public boolean isJsonNull(){return false;}}',
 'com/google/gson/JsonObject.java':'package com.google.gson;public class JsonObject extends JsonElement {public JsonElement get(String s){return null;}public JsonArray getAsJsonArray(String s){return null;}public JsonObject getAsJsonObject(String s){return null;}public java.util.Set<java.util.Map.Entry<String,JsonElement>> entrySet(){return null;}public boolean has(String s){return false;}}',
 'com/google/gson/JsonArray.java':'package com.google.gson;public class JsonArray extends JsonElement implements Iterable<JsonElement>{public java.util.Iterator<JsonElement> iterator(){return null;}}',
 'com/google/gson/JsonParser.java':'package com.google.gson;public class JsonParser{public static JsonElement parseReader(java.io.Reader r){return null;}}',
 'com/mojang/brigadier/arguments/StringArgumentType.java':'package com.mojang.brigadier.arguments;import net.minecraft.commands.CommandContext;public class StringArgumentType{public static StringArgumentType word(){return null;}public static String getString(CommandContext c,String s){return null;}}',
 'dev/dtzudontsay/knownworld/KnownWorld.java':'package dev.dtzudontsay.knownworld;public class KnownWorld{public static final Logger LOGGER=new Logger();public static class Logger{public void info(String s){}}}',
 'net/fabricmc/api/ModInitializer.java':'package net.fabricmc.api;public interface ModInitializer{void onInitialize();}',
 'net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback.java':'package net.fabricmc.fabric.api.command.v2;import net.minecraft.commands.Dispatcher;public interface CommandRegistrationCallback{void register(Dispatcher d,Object a,Object e);Event EVENT=new Event();class Event{public void register(CommandRegistrationCallback c){}}}',
 'net/minecraft/commands/CommandSourceStack.java':'package net.minecraft.commands;import net.minecraft.network.chat.Component;import java.util.function.Supplier;public class CommandSourceStack{public void sendSuccess(Supplier<Component> c,boolean x){}public void sendFailure(Component c){}}',
 'net/minecraft/commands/CommandContext.java':'package net.minecraft.commands;public class CommandContext{public CommandSourceStack getSource(){return null;}}',
 'net/minecraft/commands/Dispatcher.java':'package net.minecraft.commands;public class Dispatcher{public Object register(Commands.Builder b){return null;}}',
 'net/minecraft/commands/Commands.java':'package net.minecraft.commands;import com.mojang.brigadier.arguments.StringArgumentType;import java.util.function.Function;public class Commands{public static Builder literal(String s){return new Builder();}public static Builder argument(String s,StringArgumentType t){return new Builder();}public static class Builder{public Builder then(Builder b){return this;}public Builder executes(Function<CommandContext,Integer> f){return this;}}}',
 'net/minecraft/network/chat/Component.java':'package net.minecraft.network.chat;public class Component{public static Component literal(String s){return null;}}',
}
with tempfile.TemporaryDirectory() as td:
    root=Path(td);paths=[]
    for p,body in STUBS.items():
        to=root/p;to.parent.mkdir(parents=True,exist_ok=True);to.write_text(body);paths.append(str(to))
    cmd=['javac','-encoding','UTF-8','-d',str(root/'compiled'),*paths,str(TARGET)]
    result=subprocess.run(cmd,capture_output=True,text=True)
    print(result.stdout,result.stderr)
    if result.returncode:raise SystemExit(result.returncode)
    print('C1 Java 21 stub API-shape smoke compile PASSED; run Gradle for real API compatibility')

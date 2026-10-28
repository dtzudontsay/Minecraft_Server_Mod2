package dev.dtzudontsay.knownworld.debug;

import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC411GlobalWorldgen;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** C4.13 read-only, server-authoritative diagnostics; never generates or rewrites chunks. */
public final class KnownWorldC413ConstructionDiagnostics {
    private static final String GLOBAL_INDEX =
            "data/knownworld/scenarios/agot_298_ac/universal_construction_c4_11_global_v1.json";
    private KnownWorldC413ConstructionDiagnostics() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("kwconstruction")
                        .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                        .then(Commands.literal("anchors").executes(ctx -> anchors(ctx.getSource())))));
    }

    private static void say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[KW Construction] " + message), false);
    }

    private static int status(CommandSourceStack source) {
        ServerLevel overworld = source.getServer().overworld();
        var generator = overworld.getChunkSource().getGenerator();
        boolean custom = generator instanceof KnownWorldChunkGenerator;
        boolean indexAvailable = KnownWorldC411GlobalWorldgen.class.getClassLoader()
                .getResource(GLOBAL_INDEX) != null;
        say(source, "Overworld generator: " + generator.getClass().getName());
        say(source, custom ? "WORLD PRESET OK: KnownWorldChunkGenerator is active."
                : "WORLD PRESET MISMATCH: not KnownWorldChunkGenerator; settlement decorators NEVER run here.");
        say(source, "C4.11 global index packaged: " + indexAvailable);
        if (indexAvailable) {
            try {
                say(source, KnownWorldC411GlobalWorldgen.status());
            } catch (RuntimeException problem) {
                say(source, "C4.11 STATUS ERROR: " + problem.getClass().getSimpleName()
                        + " " + problem.getMessage());
            }
        } else {
            say(source, "Global resource absent: build/resources/main or installed mod jar is missing C4.11 index.");
        }
        say(source, "Current dimension=" + source.getLevel().dimension()
                + " | Existing explored chunks are NOT retrofitted.");
        say(source, "Use /kwinterfell3 status for Winterfell V3 decorated-chunk counter.");
        return 1;
    }

    private static int anchors(CommandSourceStack source) {
        say(source, "Winterfell NW-029 MAJOR: X=-4050173 Z=-1976496 (V3 separate generator)");
        say(source, "Ramsgate NW-031 SMALL: X=-3454784 Z=-1589008 (C2)");
        say(source, "Acorn Hall SW-023 MEDIUM: X=-4115723 Z=-284673 (C2)");
        say(source, "Use creative spectator/fly or atlas auto-height teleport; Y=200 may be underground.");
        return 1;
    }
}

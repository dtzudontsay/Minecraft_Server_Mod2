package dev.dtzudontsay.knownworld.simulation.integration;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC3Architecture;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Read-only architectural catalog inspection. Actual block generation is C2-only. */
public final class KnownWorldC3ArchitectureRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var root = Commands.literal("kwc3");
            root.then(Commands.literal("status")
                    .executes(ctx -> status(ctx.getSource())));
            root.then(Commands.literal("template")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> show(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id")))));
            dispatcher.register(root);
        });
        KnownWorld.LOGGER.info("C3 architecture catalog inspection registered; new-site generation disabled");
    }

    private static int status(CommandSourceStack out) {
        say(out, "[KW C3] executable architecture programs=" + KnownWorldC3Architecture.allTemplates().size()
                + " | physical generation limited to C2 pilots="
                + KnownWorldUniversalWorldgenC2.inspect().size());
        say(out, "C3 places furniture, doors and multi-floor access on newly generated pilot chunks. "
                + "No new sites, NPC workers, invented family signs, or existing-chunk edits.");
        return 1;
    }

    private static int show(CommandSourceStack out, String name) {
        try {
            var t = KnownWorldC3Architecture.template(name.toUpperCase(Locale.ROOT));
            say(out, "[KW C3] " + t.id() + " | form=" + t.family() + " | floors=" + t.floors()
                    + " | staff capacity=" + t.maxStaff() + " (assigned=0)");
            say(out, "Workstation blocks=" + t.stations() + " | open-air=" + t.openAir()
                    + " | needs shore=" + t.requiresShore()
                    + " | needs mountain-resource approval=" + t.requiresMountainResource()
                    + " | extra scenario-policy approval=" + t.requiresPolicy());
            return 1;
        } catch (IllegalArgumentException error) {
            out.sendFailure(Component.literal(error.getMessage()));
            return 0;
        }
    }

    private static void say(CommandSourceStack out, String text) {
        out.sendSuccess(() -> Component.literal(text), false);
    }
}

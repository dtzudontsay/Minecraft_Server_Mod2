package dev.dtzudontsay.knownworld.simulation.integration;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** C2 pilot-only inspection commands; not a live-world construction command. */
public final class KnownWorldC2WorldgenRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var root = Commands.literal("kwc2");
            root.then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));
            root.then(Commands.literal("sites").executes(ctx -> sites(ctx.getSource())));
            root.then(Commands.literal("site").then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> site(ctx.getSource(), StringArgumentType.getString(ctx, "id")))));
            dispatcher.register(root);
        });
        KnownWorld.LOGGER.info("C2 physical pilot commands registered; two sites only");
    }

    private static int status(CommandSourceStack out) {
        var pilots = KnownWorldUniversalWorldgenC2.inspect();
        int safe = pilots.stream().mapToInt(KnownWorldUniversalWorldgenC2.PilotInfo::safeLots).sum();
        int skipped = pilots.stream().mapToInt(KnownWorldUniversalWorldgenC2.PilotInfo::skippedLots).sum();
        say(out, "[KW C2] pilot sites=" + pilots.size() + " (NOT world-wide)"
                + " | survey-safe plots=" + safe + " | unsafe plots=" + skipped
                + " | chunks decorated this session=" + KnownWorldUniversalWorldgenC2.chunksDecorated()
                + " | skipped plot fragments=" + KnownWorldUniversalWorldgenC2.skippedUnsafePlotFragments());
        say(out, "Only newly generated chunks receive blocks. No NPCs/families/jobs spawned. "
                + "Existing Winterfell and existing chunks remain unchanged.");
        return 1;
    }

    private static int sites(CommandSourceStack out) {
        for (var site : KnownWorldUniversalWorldgenC2.inspect()) show(out, site);
        return 1;
    }

    private static int site(CommandSourceStack out, String id) {
        for (var site : KnownWorldUniversalWorldgenC2.inspect()) {
            if (id.equals(site.siteId()) || site.markerIds().contains(id)) {
                show(out, site); return 1;
            }
        }
        out.sendFailure(Component.literal("Not a C2 physical pilot: " + id
                + " — use /kwsites for planned sites"));
        return 0;
    }

    private static void show(CommandSourceStack out, KnownWorldUniversalWorldgenC2.PilotInfo site) {
        say(out, "[KW C2] " + site.name() + " | " + site.tier() + " | " + site.siteId()
                + " | markers=" + site.markerIds());
        say(out, "Anchor X=" + site.anchorX() + " Z=" + site.anchorZ()
                + " | test lots=" + site.lots() + " | indexed chunks=" + site.chunks()
                + " | terrain-valid=" + site.safeLots() + " | skipped=" + site.skippedLots()
                + " | remaining housing demand at least=" + site.housingBacklog());
    }

    private static void say(CommandSourceStack out, String s) {
        out.sendSuccess(() -> Component.literal(s), false);
    }
}

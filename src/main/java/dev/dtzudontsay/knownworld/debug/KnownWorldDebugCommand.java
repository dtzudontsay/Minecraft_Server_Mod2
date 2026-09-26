package dev.dtzudontsay.knownworld.debug;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class KnownWorldDebugCommand {
    private KnownWorldDebugCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("knownworld")
                                .then(Commands.literal("status")
                                        .executes(context -> {
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Known World loaded | Minecraft 26.3 | Fabric | 1 block = 1 metre"
                                                    ),
                                                    false
                                            );
                                            return 1;
                                        }))
                )
        );
    }
}

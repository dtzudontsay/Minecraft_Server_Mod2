package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeeds;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class NpcNeedsDebugCommand {

    private NpcNeedsDebugCommand() {
    }

    public static void register() {

        CommandRegistrationCallback.EVENT.register(
                (
                        dispatcher,
                        registryAccess,
                        environment
                ) ->
                        dispatcher.register(

                                Commands.literal(
                                                "kwneeds"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "show"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                NpcNeedsDebugCommand::executeShow
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "set"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "fatigue",
                                                                                                DoubleArgumentType.doubleArg(
                                                                                                        0.0,
                                                                                                        1.0
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "hunger",
                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                        0.0,
                                                                                                                        1.0
                                                                                                                )
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "social",
                                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                                        0.0,
                                                                                                                                        1.0
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .executes(
                                                                                                                                NpcNeedsDebugCommand::executeSet
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeShow(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId id =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        try {

            NpcNeeds needs =
                    NpcSimulation.get()
                            .needs()
                            .getOrCreate(
                                    id
                            );

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            String.format(
                                                    Locale.ROOT,
                                                    "NPC #%s needs | fatigue %.3f | hunger %.3f | social %.3f",
                                                    id,
                                                    needs.fatigue(),
                                                    needs.hunger(),
                                                    needs.social()
                                            )
                                    ),
                            false
                    );

            return 1;

        } catch (IllegalArgumentException exception) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    exception.getMessage()
                            )
                    );

            return 0;
        }
    }

    private static int executeSet(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId id =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        double fatigue =
                DoubleArgumentType.getDouble(
                        context,
                        "fatigue"
                );

        double hunger =
                DoubleArgumentType.getDouble(
                        context,
                        "hunger"
                );

        double social =
                DoubleArgumentType.getDouble(
                        context,
                        "social"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            simulation.needs()
                    .registerLoaded(
                            id,
                            new NpcNeeds(
                                    fatigue,
                                    hunger,
                                    social
                            )
                    );

            simulation.save();

            return executeShow(
                    context
            );

        } catch (IllegalArgumentException exception) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    exception.getMessage()
                            )
                    );

            return 0;
        }
    }
}
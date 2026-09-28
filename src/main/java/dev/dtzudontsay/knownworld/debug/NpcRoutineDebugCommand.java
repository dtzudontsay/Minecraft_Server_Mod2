package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoleType;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutine;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class NpcRoutineDebugCommand {

    private NpcRoutineDebugCommand() {
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
                                                "kwroutine"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "time"
                                                        )
                                                        .executes(
                                                                NpcRoutineDebugCommand::executeTime
                                                        )
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
                                                                                NpcRoutineDebugCommand::executeShow
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "home"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                NpcRoutineDebugCommand::executeHome
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "work"
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
                                                                                                "role",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "start",
                                                                                                                IntegerArgumentType.integer(
                                                                                                                        0,
                                                                                                                        23999
                                                                                                                )
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "end",
                                                                                                                                IntegerArgumentType.integer(
                                                                                                                                        0,
                                                                                                                                        23999
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .executes(
                                                                                                                                NpcRoutineDebugCommand::executeWork
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeTime(
            CommandContext<CommandSourceStack> context
    ) {
        NpcSimulation simulation =
                NpcSimulation.get();

        long tick =
                simulation.serverTickCounter();

        long day =
                tick
                        / 24000L;

        long tickOfDay =
                Math.floorMod(
                        tick,
                        24000L
                );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "Simulation time | day "
                                                + day
                                                + " | tick "
                                                + tick
                                                + " | tick-of-day "
                                                + tickOfDay
                                ),
                        false
                );

        return 1;
    }

    private static int executeShow(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId id =
                readNpcId(
                        context
                );

        try {
            NpcRoutine routine =
                    NpcSimulation.get()
                            .routines()
                            .getOrCreate(
                                    id
                            );

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "NPC #"
                                                    + id
                                                    + " routine | role "
                                                    + routine.role()
                                                    + " | work "
                                                    + routine.workStartTick()
                                                    + "-"
                                                    + routine.workEndTick()
                                    ),
                            false
                    );

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Home: "
                                                    + formatPosition(
                                                    routine.homePosition()
                                            )
                                    ),
                            false
                    );

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Workplace: "
                                                    + formatPosition(
                                                    routine.workPosition()
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

    private static int executeHome(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId id =
                readNpcId(
                        context
                );

        ServerPlayer player =
                getPlayer(
                        context.getSource()
                );

        if (player == null) {
            return 0;
        }

        try {
            NpcRoutine routine =
                    NpcSimulation.get()
                            .routines()
                            .getOrCreate(
                                    id
                            );

            routine.setHomePosition(
                    playerPosition(
                            player
                    )
            );

            NpcSimulation.get()
                    .save();

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Set home for NPC #"
                                                    + id
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

    private static int executeWork(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId id =
                readNpcId(
                        context
                );

        ServerPlayer player =
                getPlayer(
                        context.getSource()
                );

        if (player == null) {
            return 0;
        }

        String roleText =
                StringArgumentType.getString(
                        context,
                        "role"
                );

        NpcRoleType role;

        try {

            role =
                    NpcRoleType.valueOf(
                            roleText.toUpperCase(
                                    Locale.ROOT
                            )
                    );

        } catch (IllegalArgumentException exception) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "Unknown role: "
                                            + roleText
                            )
                    );

            return 0;
        }

        int start =
                IntegerArgumentType.getInteger(
                        context,
                        "start"
                );

        int end =
                IntegerArgumentType.getInteger(
                        context,
                        "end"
                );

        try {

            NpcRoutine routine =
                    NpcSimulation.get()
                            .routines()
                            .getOrCreate(
                                    id
                            );

            routine.setRole(
                    role
            );

            routine.setWorkPosition(
                    playerPosition(
                            player
                    )
            );

            routine.setWorkSchedule(
                    start,
                    end
            );

            NpcSimulation.get()
                    .save();

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Set NPC #"
                                                    + id
                                                    + " workplace and role "
                                                    + role
                                                    + " | schedule "
                                                    + start
                                                    + "-"
                                                    + end
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

    private static NpcId readNpcId(
            CommandContext<CommandSourceStack> context
    ) {
        return new NpcId(
                LongArgumentType.getLong(
                        context,
                        "npc"
                )
        );
    }

    private static ServerPlayer getPlayer(
            CommandSourceStack source
    ) {
        try {

            return source.getPlayerOrException();

        } catch (Exception exception) {

            source.sendFailure(
                    Component.literal(
                            "This command must be executed by a player."
                    )
            );

            return null;
        }
    }

    private static SimulationPosition playerPosition(
            ServerPlayer player
    ) {
        Vec3 position =
                player.position();

        return new SimulationPosition(
                player.level()
                        .dimension()
                        .identifier()
                        .toString(),
                position.x,
                position.y,
                position.z
        );
    }

    private static String formatPosition(
            SimulationPosition position
    ) {
        if (position == null) {
            return "not set";
        }

        return String.format(
                Locale.ROOT,
                "%s | %.2f %.2f %.2f",
                position.dimension(),
                position.x(),
                position.y(),
                position.z()
        );
    }
}
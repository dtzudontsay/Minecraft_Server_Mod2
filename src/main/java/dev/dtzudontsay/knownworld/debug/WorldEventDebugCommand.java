package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventId;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Locale;

public final class WorldEventDebugCommand {

    private WorldEventDebugCommand() {
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
                                                "kwevent"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "list"
                                                        )
                                                        .executes(
                                                                WorldEventDebugCommand::executeList
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "inspect"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                WorldEventDebugCommand::executeInspect
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "fact"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "radius",
                                                                                DoubleArgumentType.doubleArg(
                                                                                        0.0
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "factKey",
                                                                                                StringArgumentType.string()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "value",
                                                                                                                StringArgumentType.string()
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "importance",
                                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                                        0.0,
                                                                                                                                        1.0
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "summary",
                                                                                                                                                StringArgumentType.greedyString()
                                                                                                                                        )
                                                                                                                                        .executes(
                                                                                                                                                WorldEventDebugCommand::executeFact
                                                                                                                                        )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeFact(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source =
                context.getSource();

        ServerPlayer player;

        try {

            player =
                    source.getPlayerOrException();

        } catch (Exception exception) {

            source.sendFailure(
                    Component.literal(
                            "This command must be executed by a player."
                    )
            );

            return 0;
        }

        double radius =
                DoubleArgumentType.getDouble(
                        context,
                        "radius"
                );

        String factKey =
                StringArgumentType.getString(
                        context,
                        "factKey"
                );

        String value =
                StringArgumentType.getString(
                        context,
                        "value"
                );

        double importance =
                DoubleArgumentType.getDouble(
                        context,
                        "importance"
                );

        String summary =
                StringArgumentType.getString(
                        context,
                        "summary"
                );

        Vec3 playerPosition =
                player.position();

        String dimension =
                player.level()
                        .dimension()
                        .identifier()
                        .toString();

        SimulationPosition position =
                new SimulationPosition(
                        dimension,
                        playerPosition.x,
                        playerPosition.y,
                        playerPosition.z
                );

        NpcSimulation simulation =
                NpcSimulation.get();

        WorldEvent event =
                simulation.events()
                        .create(
                                WorldEventType.GENERAL,
                                summary,
                                position,
                                simulation.serverTickCounter(),
                                importance,
                                null,
                                null,
                                factKey,
                                value
                        );

        int witnesses =
                simulation.observation()
                        .observeNearby(
                                event,
                                radius
                        );

        simulation.save();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Created world event #"
                                        + event.id()
                                        + " | "
                                        + witnesses
                                        + " NPC witness(es)."
                        ),
                false
        );

        return 1;
    }

    private static int executeList(
            CommandContext<CommandSourceStack> context
    ) {
        var events =
                NpcSimulation.get()
                        .events()
                        .all()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        WorldEvent::id
                                )
                        )
                        .toList();

        if (events.isEmpty()) {

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "No world events recorded."
                                    ),
                            false
                    );

            return 1;
        }

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "World events: "
                                                + events.size()
                                ),
                        false
                );

        for (WorldEvent event : events) {

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "#"
                                                    + event.id()
                                                    + " ["
                                                    + event.type()
                                                    + "] "
                                                    + event.summary()
                                    ),
                            false
                    );
        }

        return events.size();
    }

    private static int executeInspect(
            CommandContext<CommandSourceStack> context
    ) {
        WorldEventId id =
                new WorldEventId(
                        LongArgumentType.getLong(
                                context,
                                "id"
                        )
                );

        WorldEvent event =
                NpcSimulation.get()
                        .events()
                        .find(
                                id
                        )
                        .orElse(
                                null
                        );

        if (event == null) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "No world event exists with ID "
                                            + id
                            )
                    );

            return 0;
        }

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "Event #"
                                                + event.id()
                                                + " ["
                                                + event.type()
                                                + "] "
                                                + event.summary()
                                ),
                        false
                );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        String.format(
                                                Locale.ROOT,
                                                "Importance %.2f | Tick %d | Position %s %.2f %.2f %.2f",
                                                event.importance(),
                                                event.occurredTick(),
                                                event.position()
                                                        .dimension(),
                                                event.position()
                                                        .x(),
                                                event.position()
                                                        .y(),
                                                event.position()
                                                        .z()
                                        )
                                ),
                        false
                );

        if (event.containsFact()) {

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Fact: "
                                                    + event.factKey()
                                                    + " = "
                                                    + event.factValue()
                                    ),
                            false
                    );
        }

        return 1;
    }
}
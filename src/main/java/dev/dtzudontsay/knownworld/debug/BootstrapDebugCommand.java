package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class BootstrapDebugCommand {

    private BootstrapDebugCommand() {
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
                                                "kwbootstrap"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "status"
                                                        )
                                                        .executes(
                                                                BootstrapDebugCommand::executeStatus
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "npc"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                BootstrapDebugCommand::executeNpc
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "settlement"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                BootstrapDebugCommand::executeSettlement
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "organization"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                BootstrapDebugCommand::executeOrganization
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "title"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                BootstrapDebugCommand::executeTitle
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeStatus(
            CommandContext<CommandSourceStack> context
    ) {

        NpcSimulation simulation =
                NpcSimulation.get();

        var ids =
                simulation.authoredIds();

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "Scenario: "
                                                + (
                                                ids.scenarioId() == null
                                                        ? "none"
                                                        : ids.scenarioId()
                                        )
                                ),
                        false
                );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "Authored IDs | NPCs "
                                                + ids.npcs().size()
                                                + " | settlements "
                                                + ids.settlements().size()
                                                + " | organizations "
                                                + ids.organizations().size()
                                                + " | titles "
                                                + ids.titles().size()
                                ),
                        false
                );

        return 1;
    }

    private static int executeNpc(
            CommandContext<CommandSourceStack> context
    ) {
        String authoredId =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        return NpcSimulation.get()
                .authoredIds()
                .findNpc(
                        authoredId
                )
                .map(
                        id -> {

                            context.getSource()
                                    .sendSuccess(
                                            () ->
                                                    Component.literal(
                                                            authoredId
                                                                    + " -> NPC #"
                                                                    + id
                                                    ),
                                            false
                                    );

                            return 1;
                        }
                )
                .orElseGet(
                        () -> {

                            context.getSource()
                                    .sendFailure(
                                            Component.literal(
                                                    "Unknown authored NPC ID: "
                                                            + authoredId
                                            )
                                    );

                            return 0;
                        }
                );
    }

    private static int executeSettlement(
            CommandContext<CommandSourceStack> context
    ) {
        String authoredId =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        return NpcSimulation.get()
                .authoredIds()
                .findSettlement(
                        authoredId
                )
                .map(
                        id -> {

                            context.getSource()
                                    .sendSuccess(
                                            () ->
                                                    Component.literal(
                                                            authoredId
                                                                    + " -> Settlement #"
                                                                    + id
                                                    ),
                                            false
                                    );

                            return 1;
                        }
                )
                .orElseGet(
                        () -> {

                            context.getSource()
                                    .sendFailure(
                                            Component.literal(
                                                    "Unknown authored settlement ID: "
                                                            + authoredId
                                            )
                                    );

                            return 0;
                        }
                );
    }

    private static int executeOrganization(
            CommandContext<CommandSourceStack> context
    ) {
        String authoredId =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        return NpcSimulation.get()
                .authoredIds()
                .findOrganization(
                        authoredId
                )
                .map(
                        id -> {

                            context.getSource()
                                    .sendSuccess(
                                            () ->
                                                    Component.literal(
                                                            authoredId
                                                                    + " -> Organization #"
                                                                    + id
                                                    ),
                                            false
                                    );

                            return 1;
                        }
                )
                .orElseGet(
                        () -> {

                            context.getSource()
                                    .sendFailure(
                                            Component.literal(
                                                    "Unknown authored organization ID: "
                                                            + authoredId
                                            )
                                    );

                            return 0;
                        }
                );
    }

    private static int executeTitle(
            CommandContext<CommandSourceStack> context
    ) {
        String authoredId =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        return NpcSimulation.get()
                .authoredIds()
                .findTitle(
                        authoredId
                )
                .map(
                        id -> {

                            context.getSource()
                                    .sendSuccess(
                                            () ->
                                                    Component.literal(
                                                            authoredId
                                                                    + " -> Title #"
                                                                    + id
                                                    ),
                                            false
                                    );

                            return 1;
                        }
                )
                .orElseGet(
                        () -> {

                            context.getSource()
                                    .sendFailure(
                                            Component.literal(
                                                    "Unknown authored title ID: "
                                                            + authoredId
                                            )
                                    );

                            return 0;
                        }
                );
    }
}
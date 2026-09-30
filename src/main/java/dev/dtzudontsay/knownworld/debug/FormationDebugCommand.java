package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.formation.UpbringingRecord;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class FormationDebugCommand {

    private FormationDebugCommand() {
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
                                                "kwformation"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "status"
                                                        )
                                                        .executes(
                                                                FormationDebugCommand::status
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "reconcile"
                                                        )
                                                        .executes(
                                                                FormationDebugCommand::reconcile
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
                                                                                FormationDebugCommand::show
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "guardian"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "child",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "guardian",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                FormationDebugCommand::guardian
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "foster"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "child",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "fosterParent",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                FormationDebugCommand::foster
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "apply_year"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "child",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "age",
                                                                                                IntegerArgumentType.integer(
                                                                                                        1,
                                                                                                        15
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                FormationDebugCommand::applyYear
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) {

        NpcSimulation simulation =
                NpcSimulation.get();

        success(
                context,
                "Formation integration: NPCs="
                        + simulation.registry()
                        .size()
                        + " profiles="
                        + simulation.profiles()
                        .size()
                        + " upbringingRecords="
                        + simulation.upbringing()
                        .size()
        );

        return 1;
    }

    private static int reconcile(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            simulation.formationIntegration()
                    .ensureAll();

            simulation.save();

            success(
                    context,
                    "Formation integration reconciliation complete. Upbringing records="
                            + simulation.upbringing()
                            .size()
            );

            return 1;

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int show(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context,
                            "npc"
                    );

            UpbringingRecord record =
                    NpcSimulation.get()
                            .upbringing()
                            .require(
                                    npc
                            );

            success(
                    context,
                    "Formation NPC #"
                            + npc
            );

            success(
                    context,
                    "birthplace="
                            + record.birthLocationId()
                            + " upbringing="
                            + record.upbringingLocationId()
            );

            success(
                    context,
                    "guardian="
                            + formatNpc(
                            record.guardian()
                    )
                            + " fosterParent="
                            + formatNpc(
                            record.fosterParent()
                    )
            );

            success(
                    context,
                    "lastFormationAge="
                            + record.lastFormationAge()
            );

            return 1;

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int guardian(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId child =
                    npc(
                            context,
                            "child"
                    );

            NpcId guardian =
                    npc(
                            context,
                            "guardian"
                    );

            NpcSimulation.get()
                    .formation()
                    .setGuardian(
                            child,
                            guardian
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Guardian for NPC #"
                            + child
                            + " set to NPC #"
                            + guardian
            );

            return 1;

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int foster(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId child =
                    npc(
                            context,
                            "child"
                    );

            NpcId fosterParent =
                    npc(
                            context,
                            "fosterParent"
                    );

            NpcSimulation.get()
                    .formation()
                    .setFosterParent(
                            child,
                            fosterParent
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Foster parent for NPC #"
                            + child
                            + " set to NPC #"
                            + fosterParent
            );

            return 1;

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int applyYear(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId child =
                    npc(
                            context,
                            "child"
                    );

            int age =
                    IntegerArgumentType.getInteger(
                            context,
                            "age"
                    );

            NpcSimulation.get()
                    .formation()
                    .applyFormationYear(
                            child,
                            age
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Applied formation year "
                            + age
                            + " to NPC #"
                            + child
            );

            return 1;

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static NpcId npc(
            CommandContext<CommandSourceStack> context,
            String argument
    ) {

        return new NpcId(
                LongArgumentType.getLong(
                        context,
                        argument
                )
        );
    }

    private static String formatNpc(
            NpcId npc
    ) {

        return npc == null
                ? "none"
                : "#"
                + npc;
    }

    private static int failure(
            CommandContext<CommandSourceStack> context,
            RuntimeException exception
    ) {

        context.getSource()
                .sendFailure(
                        Component.literal(
                                exception.getMessage() == null
                                        ? exception.getClass()
                                        .getSimpleName()
                                        : exception.getMessage()
                        )
                );

        return 0;
    }

    private static void success(
            CommandContext<CommandSourceStack> context,
            String message
    ) {

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        message
                                ),
                        false
                );
    }
}
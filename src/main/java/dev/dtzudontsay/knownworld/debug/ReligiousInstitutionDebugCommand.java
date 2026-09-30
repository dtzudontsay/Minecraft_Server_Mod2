package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionPracticeService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousInstitutionCatalog;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ReligiousInstitutionDebugCommand {

    private ReligiousInstitutionDebugCommand() {
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
                                                "kwreligionorg"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "list"
                                                        )
                                                        .executes(
                                                                ReligiousInstitutionDebugCommand::list
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "can_join"
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
                                                                                                "order",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                ReligiousInstitutionDebugCommand::canJoin
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "ritual"
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
                                                                                                "ritual",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                ReligiousInstitutionDebugCommand::ritual
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int list(
            CommandContext<CommandSourceStack> context
    ) {

        success(
                context,
                "Religious institutions: "
                        + ReligiousInstitutionCatalog.get()
                        .all()
        );

        return 1;
    }

    private static int canJoin(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    new NpcId(
                            LongArgumentType.getLong(
                                    context,
                                    "npc"
                            )
                    );

            String order =
                    StringArgumentType.getString(
                            context,
                            "order"
                    );

            success(
                    context,
                    "Can join "
                            + order
                            + ": "
                            + practice()
                            .canJoinOrder(
                                    npc,
                                    order
                            )
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

    private static int ritual(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    new NpcId(
                            LongArgumentType.getLong(
                                    context,
                                    "npc"
                            )
                    );

            String ritual =
                    StringArgumentType.getString(
                            context,
                            "ritual"
                    );

            practice()
                    .performRitual(
                            npc,
                            ritual
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Performed ritual "
                            + ritual
                            + " for NPC #"
                            + npc
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

    private static ReligionPracticeService practice() {

        NpcSimulation simulation =
                NpcSimulation.get();

        CharacterPsychologyService psychology =
                new CharacterPsychologyService(
                        simulation.registry(),
                        simulation.profiles(),
                        simulation.relationships()
                );

        ReligionService religion =
                new ReligionService(
                        simulation.registry(),
                        simulation.profiles(),
                        simulation.relationships(),
                        psychology
                );

        return new ReligionPracticeService(
                simulation.profiles(),
                religion,
                simulation.religiousMemberships()
        );
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
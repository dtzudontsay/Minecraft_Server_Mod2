package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class FamilyDebugCommand {

    private FamilyDebugCommand() {
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
                                                "kwfamily"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "parents"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                FamilyDebugCommand::executeParents
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "children"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                FamilyDebugCommand::executeChildren
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "child"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "mother",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "father",
                                                                                                LongArgumentType.longArg(
                                                                                                        0
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "sex",
                                                                                                                StringArgumentType.word()
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "birthYear",
                                                                                                                                IntegerArgumentType.integer()
                                                                                                                        )
                                                                                                                        .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "givenName",
                                                                                                                                                StringArgumentType.word()
                                                                                                                                        )
                                                                                                                                        .executes(
                                                                                                                                                FamilyDebugCommand::executeChild
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

    private static int executeParents(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        try {

            Parentage parentage =
                    NpcSimulation.get()
                            .genealogy()
                            .parentsOf(
                                    npc
                            )
                            .orElse(
                                    null
                            );

            if (parentage == null) {

                context.getSource()
                        .sendSuccess(
                                () ->
                                        Component.literal(
                                                "NPC #"
                                                        + npc
                                                        + " has no recorded parentage."
                                        ),
                                false
                        );

                return 1;
            }

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "NPC #"
                                                    + npc
                                                    + " | mother "
                                                    + formatNpc(
                                                    parentage.mother()
                                            )
                                                    + " | father "
                                                    + formatNpc(
                                                    parentage.father()
                                            )
                                                    + " | birth tick "
                                                    + parentage.birthTick()
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

    private static int executeChildren(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        try {

            var children =
                    NpcSimulation.get()
                            .genealogy()
                            .childrenOf(
                                    npc
                            );

            if (children.isEmpty()) {

                context.getSource()
                        .sendSuccess(
                                () ->
                                        Component.literal(
                                                "NPC #"
                                                        + npc
                                                        + " has no recorded children."
                                        ),
                                false
                        );

                return 1;
            }

            for (NpcId child : children) {

                NpcState state =
                        NpcSimulation.get()
                                .registry()
                                .find(
                                        child
                                )
                                .orElse(
                                        null
                                );

                context.getSource()
                        .sendSuccess(
                                () ->
                                        Component.literal(
                                                "#"
                                                        + child
                                                        + " "
                                                        + (
                                                        state == null
                                                                ? "unknown"
                                                                : state.identity()
                                                                .fullName()
                                                )
                                        ),
                                false
                        );
            }

            return children.size();

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

    private static int executeChild(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId mother =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "mother"
                        )
                );

        long rawFather =
                LongArgumentType.getLong(
                        context,
                        "father"
                );

        NpcId father =
                rawFather == 0
                        ? null
                        : new NpcId(
                        rawFather
                );

        String rawSex =
                StringArgumentType.getString(
                        context,
                        "sex"
                );

        int birthYear =
                IntegerArgumentType.getInteger(
                        context,
                        "birthYear"
                );

        String givenName =
                StringArgumentType.getString(
                        context,
                        "givenName"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            NpcState child;

            if ("auto".equalsIgnoreCase(
                    rawSex
            )) {

                child =
                        simulation.characterGeneration()
                                .createChildAutoSex(
                                        givenName,
                                        birthYear,
                                        mother,
                                        father,
                                        simulation.serverTickCounter()
                                );

            } else {

                NpcSex sex =
                        NpcSex.valueOf(
                                rawSex.toUpperCase(
                                        Locale.ROOT
                                )
                        );

                child =
                        simulation.characterGeneration()
                                .createChild(
                                        givenName,
                                        sex,
                                        birthYear,
                                        mother,
                                        father,
                                        simulation.serverTickCounter()
                                );
            }

            simulation.save();

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Generated NPC #"
                                                    + child.id()
                                                    + " "
                                                    + child.identity()
                                                    .fullName()
                                                    + " ["
                                                    + child.identity()
                                                    .sex()
                                                    + "]"
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

    private static String formatNpc(
            NpcId id
    ) {
        return id == null
                ? "unknown"
                : "#"
                + id;
    }
}
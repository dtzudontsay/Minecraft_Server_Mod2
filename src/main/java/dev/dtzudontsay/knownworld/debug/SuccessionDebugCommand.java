package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionLaw;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class SuccessionDebugCommand {

    private SuccessionDebugCommand() {
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
                                                "kwsuccession"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "law"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "title",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                SuccessionDebugCommand::executeLaw
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "setlaw"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "title",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "law",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                SuccessionDebugCommand::executeSetLaw
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "heirs"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "title",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "holder",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                SuccessionDebugCommand::executeHeirs
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "claims"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                SuccessionDebugCommand::executeClaims
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeLaw(
            CommandContext<CommandSourceStack> context
    ) {
        TitleId title =
                title(
                        context
                );

        try {

            var law =
                    NpcSimulation.get()
                            .successionRules()
                            .lawOf(
                                    title
                            );

            success(
                    context,
                    "Title #"
                            + title
                            + " succession law: "
                            + law
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeSetLaw(
            CommandContext<CommandSourceStack> context
    ) {
        TitleId title =
                title(
                        context
                );

        try {

            SuccessionLaw law =
                    SuccessionLaw.valueOf(
                            StringArgumentType.getString(
                                            context,
                                            "law"
                                    )
                                    .toUpperCase(
                                            Locale.ROOT
                                    )
                    );

            NpcSimulation simulation =
                    NpcSimulation.get();

            simulation.successionRules()
                    .setLaw(
                            title,
                            law
                    );

            simulation.save();

            success(
                    context,
                    "Title #"
                            + title
                            + " succession law set to "
                            + law
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeHeirs(
            CommandContext<CommandSourceStack> context
    ) {
        TitleId title =
                title(
                        context
                );

        NpcId holder =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "holder"
                        )
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            var candidates =
                    simulation.succession()
                            .rankedCandidates(
                                    title,
                                    holder
                            );

            if (candidates.isEmpty()) {

                success(
                        context,
                        "No eligible succession candidates."
                );

                return 1;
            }

            int index =
                    1;

            for (NpcId candidate : candidates) {

                var npc =
                        simulation.registry()
                                .find(
                                        candidate
                                )
                                .orElseThrow();

                success(
                        context,
                        index
                                + ". NPC #"
                                + candidate
                                + " "
                                + npc.identity()
                                .fullName()
                                + " | "
                                + (
                                npc.isAlive()
                                        ? "ALIVE"
                                        : "DEAD"
                        )
                );

                index++;
            }

            return candidates.size();

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeClaims(
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

            var claims =
                    NpcSimulation.get()
                            .claims()
                            .claimsOf(
                                    npc
                            );

            if (claims.isEmpty()) {

                success(
                        context,
                        "NPC #"
                                + npc
                                + " has no claims."
                );

                return 1;
            }

            for (var claim : claims) {

                success(
                        context,
                        "Claim #"
                                + claim.id()
                                + " | title #"
                                + claim.title()
                                + " | "
                                + claim.strength()
                                + " | active "
                                + claim.active()
                                + " | inherited from "
                                + (
                                claim.inheritedFrom()
                                        == null
                                        ? "none"
                                        : "#"
                                        + claim.inheritedFrom()
                        )
                );
            }

            return claims.size();

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static TitleId title(
            CommandContext<CommandSourceStack> context
    ) {
        return new TitleId(
                LongArgumentType.getLong(
                        context,
                        "title"
                )
        );
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
}
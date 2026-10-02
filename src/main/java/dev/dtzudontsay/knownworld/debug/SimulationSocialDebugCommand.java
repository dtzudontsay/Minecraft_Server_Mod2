package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialInteractionService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialInteractionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;

public final class SimulationSocialDebugCommand {

    private SimulationSocialDebugCommand() {
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
                                                "kwsim"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "social-types"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    "Social interaction types: "
                                                                                                            + Arrays.toString(
                                                                                                            SocialInteractionType.values()
                                                                                                    )
                                                                                            ),
                                                                                    false
                                                                            );

                                                                    return 1;
                                                                }
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "social"
                                                        )

                                                        .then(
                                                                Commands.argument(
                                                                                "actor",
                                                                                StringArgumentType.word()
                                                                        )

                                                                        .then(
                                                                                Commands.argument(
                                                                                                "target",
                                                                                                StringArgumentType.word()
                                                                                        )

                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "type",
                                                                                                                StringArgumentType.word()
                                                                                                        )

                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "magnitude",
                                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                                        0.05,
                                                                                                                                        1.0
                                                                                                                                )
                                                                                                                        )

                                                                                                                        .executes(
                                                                                                                                context -> {

                                                                                                                                    NpcSimulation simulation =
                                                                                                                                            NpcSimulation.get();

                                                                                                                                    String actorAuthoredId =
                                                                                                                                            StringArgumentType.getString(
                                                                                                                                                    context,
                                                                                                                                                    "actor"
                                                                                                                                            );

                                                                                                                                    String targetAuthoredId =
                                                                                                                                            StringArgumentType.getString(
                                                                                                                                                    context,
                                                                                                                                                    "target"
                                                                                                                                            );

                                                                                                                                    String rawType =
                                                                                                                                            StringArgumentType.getString(
                                                                                                                                                    context,
                                                                                                                                                    "type"
                                                                                                                                            );

                                                                                                                                    double magnitude =
                                                                                                                                            DoubleArgumentType.getDouble(
                                                                                                                                                    context,
                                                                                                                                                    "magnitude"
                                                                                                                                            );

                                                                                                                                    SocialInteractionType type;

                                                                                                                                    try {

                                                                                                                                        type =
                                                                                                                                                SocialInteractionType.valueOf(
                                                                                                                                                        rawType.toUpperCase(
                                                                                                                                                                Locale.ROOT
                                                                                                                                                        )
                                                                                                                                                );

                                                                                                                                    } catch (
                                                                                                                                            IllegalArgumentException exception
                                                                                                                                    ) {

                                                                                                                                        context.getSource()
                                                                                                                                                .sendFailure(
                                                                                                                                                        Component.literal(
                                                                                                                                                                "Unknown social interaction type '"
                                                                                                                                                                        + rawType
                                                                                                                                                                        + "'. Valid types: "
                                                                                                                                                                        + Arrays.toString(
                                                                                                                                                                        SocialInteractionType.values()
                                                                                                                                                                )
                                                                                                                                                        )
                                                                                                                                                );

                                                                                                                                        return 0;
                                                                                                                                    }

                                                                                                                                    NpcId actor;

                                                                                                                                    NpcId target;

                                                                                                                                    try {

                                                                                                                                        actor =
                                                                                                                                                simulation.authoredIds()
                                                                                                                                                        .requireNpc(
                                                                                                                                                                actorAuthoredId
                                                                                                                                                        );

                                                                                                                                        target =
                                                                                                                                                simulation.authoredIds()
                                                                                                                                                        .requireNpc(
                                                                                                                                                                targetAuthoredId
                                                                                                                                                        );

                                                                                                                                    } catch (
                                                                                                                                            IllegalArgumentException exception
                                                                                                                                    ) {

                                                                                                                                        context.getSource()
                                                                                                                                                .sendFailure(
                                                                                                                                                        Component.literal(
                                                                                                                                                                exception.getMessage()
                                                                                                                                                        )
                                                                                                                                                );

                                                                                                                                        return 0;
                                                                                                                                    }

                                                                                                                                    SocialInteractionService.Result result;

                                                                                                                                    try {

                                                                                                                                        result =
                                                                                                                                                simulation.socialInteractions()
                                                                                                                                                        .perform(
                                                                                                                                                                actor,
                                                                                                                                                                target,
                                                                                                                                                                type,
                                                                                                                                                                magnitude,
                                                                                                                                                                simulation.serverTickCounter()
                                                                                                                                                        );

                                                                                                                                    } catch (
                                                                                                                                            IllegalArgumentException exception
                                                                                                                                    ) {

                                                                                                                                        context.getSource()
                                                                                                                                                .sendFailure(
                                                                                                                                                        Component.literal(
                                                                                                                                                                exception.getMessage()
                                                                                                                                                        )
                                                                                                                                                );

                                                                                                                                        return 0;
                                                                                                                                    }

                                                                                                                                    simulation.save();

                                                                                                                                    context.getSource()
                                                                                                                                            .sendSuccess(
                                                                                                                                                    () ->
                                                                                                                                                            Component.literal(
                                                                                                                                                                    "Created simulation event #"
                                                                                                                                                                            + result.event()
                                                                                                                                                                            .id()
                                                                                                                                                                            + " | "
                                                                                                                                                                            + actorAuthoredId
                                                                                                                                                                            + " "
                                                                                                                                                                            + type
                                                                                                                                                                            + " -> "
                                                                                                                                                                            + targetAuthoredId
                                                                                                                                                                            + " magnitude="
                                                                                                                                                                            + String.format(
                                                                                                                                                                            Locale.ROOT,
                                                                                                                                                                            "%.2f",
                                                                                                                                                                            magnitude
                                                                                                                                                                    )
                                                                                                                                                            ),
                                                                                                                                                    false
                                                                                                                                            );

                                                                                                                                    SocialInteractionService.RelationshipSnapshot before =
                                                                                                                                            result.before();

                                                                                                                                    SocialInteractionService.RelationshipSnapshot after =
                                                                                                                                            result.after();

                                                                                                                                    context.getSource()
                                                                                                                                            .sendSuccess(
                                                                                                                                                    () ->
                                                                                                                                                            Component.literal(
                                                                                                                                                                    String.format(
                                                                                                                                                                            Locale.ROOT,
                                                                                                                                                                            "%s -> %s BEFORE: affection=%.3f trust=%.3f respect=%.3f fear=%.3f familiarity=%.3f",
                                                                                                                                                                            targetAuthoredId,
                                                                                                                                                                            actorAuthoredId,
                                                                                                                                                                            before.affection(),
                                                                                                                                                                            before.trust(),
                                                                                                                                                                            before.respect(),
                                                                                                                                                                            before.fear(),
                                                                                                                                                                            before.familiarity()
                                                                                                                                                                    )
                                                                                                                                                            ),
                                                                                                                                                    false
                                                                                                                                            );

                                                                                                                                    context.getSource()
                                                                                                                                            .sendSuccess(
                                                                                                                                                    () ->
                                                                                                                                                            Component.literal(
                                                                                                                                                                    String.format(
                                                                                                                                                                            Locale.ROOT,
                                                                                                                                                                            "%s -> %s AFTER:  affection=%.3f trust=%.3f respect=%.3f fear=%.3f familiarity=%.3f",
                                                                                                                                                                            targetAuthoredId,
                                                                                                                                                                            actorAuthoredId,
                                                                                                                                                                            after.affection(),
                                                                                                                                                                            after.trust(),
                                                                                                                                                                            after.respect(),
                                                                                                                                                                            after.fear(),
                                                                                                                                                                            after.familiarity()
                                                                                                                                                                    )
                                                                                                                                                            ),
                                                                                                                                                    false
                                                                                                                                            );

                                                                                                                                    return 1;
                                                                                                                                }
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }
}
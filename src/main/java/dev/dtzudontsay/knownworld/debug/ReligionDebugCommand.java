package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.PersonalFaithSnapshot;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionConversionResult;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class ReligionDebugCommand {

    private ReligionDebugCommand() {
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
                                                "kwreligion"
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
                                                                                ReligionDebugCommand::show
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "init"
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
                                                                                                "religion",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                ReligionDebugCommand::init
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "regional"
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
                                                                                                "pressure",
                                                                                                DoubleArgumentType.doubleArg(
                                                                                                        0.0,
                                                                                                        1.0
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                ReligionDebugCommand::regional
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "convert_pressure"
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
                                                                                                "religion",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "pressure",
                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                        0.0,
                                                                                                                        1.0
                                                                                                                )
                                                                                                        )
                                                                                                        .executes(
                                                                                                                ReligionDebugCommand::conversionPressure
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "persuade"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "source",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "target",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "religion",
                                                                                                                StringArgumentType.word()
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "pressure",
                                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                                        0.0,
                                                                                                                                        1.0
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .executes(
                                                                                                                                ReligionDebugCommand::persuade
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "here"
                                                        )
                                                        .executes(
                                                                ReligionDebugCommand::here
                                                        )
                                        )
                        )
        );
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

            PersonalFaithSnapshot faith =
                    service()
                            .snapshot(
                                    npc
                            );

            success(
                    context,
                    "Religion NPC #"
                            + npc
                            + " | "
                            + faith.religionId()
            );

            success(
                    context,
                    "devotion="
                            + format(
                            faith.devotion()
                    )
                            + " doctrine="
                            + format(
                            faith.doctrinalKnowledge()
                    )
                            + " identity="
                            + format(
                            faith.identityImportance()
                    )
                            + " observance="
                            + format(
                            faith.observance()
                    )
            );

            success(
                    context,
                    "tolerance="
                            + format(
                            faith.tolerance()
                    )
                            + " conversionOpenness="
                            + format(
                            faith.conversionOpenness()
                    )
                            + " concealment="
                            + format(
                            faith.concealmentWillingness()
                    )
                            + " syncretism="
                            + format(
                            faith.syncretism()
                    )
            );

            success(
                    context,
                    "faithResistance="
                            + format(
                            service()
                                    .currentFaithResistance(
                                            npc
                                    )
                    )
                            + " effectiveConversionOpenness="
                            + format(
                            service()
                                    .effectiveConversionOpenness(
                                            npc
                                    )
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

    private static int init(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context,
                            "npc"
                    );

            String religion =
                    StringArgumentType.getString(
                            context,
                            "religion"
                    );

            /*
             * Neutral test initialization.
             *
             * This is a debug convenience, not a lore-default
             * generation rule.
             */
            service()
                    .initializeFaithState(
                            npc,
                            religion,
                            0.50,
                            0.35,
                            0.50,
                            0.50,
                            0.35,
                            0.25,
                            0.10,
                            0.45
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Initialized religion state for NPC #"
                            + npc
                            + " as "
                            + religion
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

    private static int regional(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npcId =
                    npc(
                            context,
                            "npc"
                    );

            NpcState npc =
                    requireNpc(
                            npcId
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            ReligionConversionResult result =
                    service()
                            .applyRegionalPressure(
                                    npcId,
                                    npc.position()
                                            .x(),
                                    npc.position()
                                            .z(),
                                    pressure
                            );

            NpcSimulation.get()
                    .save();

            printResult(
                    context,
                    result
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

    private static int conversionPressure(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context,
                            "npc"
                    );

            String religion =
                    StringArgumentType.getString(
                            context,
                            "religion"
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            ReligionConversionResult result =
                    service()
                            .applyConversionPressure(
                                    npc,
                                    null,
                                    religion,
                                    pressure
                            );

            NpcSimulation.get()
                    .save();

            printResult(
                    context,
                    result
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

    private static int persuade(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId source =
                    npc(
                            context,
                            "source"
                    );

            NpcId target =
                    npc(
                            context,
                            "target"
                    );

            String religion =
                    StringArgumentType.getString(
                            context,
                            "religion"
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            ReligionConversionResult result =
                    service()
                            .applyConversionPressure(
                                    target,
                                    source,
                                    religion,
                                    pressure
                            );

            NpcSimulation.get()
                    .save();

            printResult(
                    context,
                    result
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

    private static int here(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            ServerPlayer player =
                    context.getSource()
                            .getPlayerOrException();

            ReligionService religion =
                    service();

            String strongest =
                    religion.strongestRegionalReligion(
                                    player.getX(),
                                    player.getZ()
                            )
                            .orElse(
                                    "none"
                            );

            success(
                    context,
                    "Strongest regional religion: "
                            + strongest
            );

            if (!"none".equals(
                    strongest
            )) {

                success(
                        context,
                        "Effective support="
                                + format(
                                religion.regionalSupport(
                                        strongest,
                                        player.getX(),
                                        player.getZ()
                                )
                        )
                );
            }

            return 1;

        } catch (
                Exception exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static void printResult(
            CommandContext<CommandSourceStack> context,
            ReligionConversionResult result
    ) {

        success(
                context,
                "Religion pressure: "
                        + result.previousReligionId()
                        + " -> "
                        + result.targetReligionId()
        );

        success(
                context,
                "effectivePressure="
                        + format(
                        result.effectivePressure()
                )
                        + " susceptibility="
                        + format(
                        result.socialSusceptibility()
                )
                        + " sourceFactor="
                        + format(
                        result.sourceFactor()
                )
                        + " regionalSupport="
                        + format(
                        result.regionalSupport()
                )
        );

        success(
                context,
                "openness="
                        + format(
                        result.openness()
                )
                        + " faithResistance="
                        + format(
                        result.currentFaithResistance()
                )
                        + " momentum="
                        + format(
                        result.previousMomentum()
                )
                        + " -> "
                        + format(
                        result.newMomentum()
                )
        );

        success(
                context,
                result.converted()
                        ? "CONVERTED to "
                        + result.targetReligionId()
                        : "No conversion yet."
        );
    }

    private static ReligionService service() {

        NpcSimulation simulation =
                NpcSimulation.get();

        CharacterPsychologyService psychology =
                new CharacterPsychologyService(
                        simulation.registry(),
                        simulation.profiles(),
                        simulation.relationships()
                );

        return new ReligionService(
                simulation.registry(),
                simulation.profiles(),
                simulation.relationships(),
                psychology
        );
    }

    private static NpcState requireNpc(
            NpcId npc
    ) {

        return NpcSimulation.get()
                .registry()
                .find(
                        npc
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC "
                                                + npc
                                )
                );
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

    private static String format(
            double value
    ) {

        return String.format(
                Locale.ROOT,
                "%.3f",
                value
        );
    }

    private static int failure(
            CommandContext<CommandSourceStack> context,
            Exception exception
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
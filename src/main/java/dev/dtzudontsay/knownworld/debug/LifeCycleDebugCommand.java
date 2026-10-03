package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.Pregnancy;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class LifeCycleDebugCommand {

    private LifeCycleDebugCommand() {
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
                                                "kwlife"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "date"
                                                        )
                                                        .executes(
                                                                LifeCycleDebugCommand::executeDate
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "scale"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "ticksPerDay",
                                                                                IntegerArgumentType.integer(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                LifeCycleDebugCommand::executeScale
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "age"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                LifeCycleDebugCommand::executeAge
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "fertility"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                LifeCycleDebugCommand::executeFertility
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "pregnancy"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "mother",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                LifeCycleDebugCommand::executePregnancy
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "conceive"
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
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                LifeCycleDebugCommand::executeConceive
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "death"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                LifeCycleDebugCommand::executeDeath
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "advance"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "days",
                                                                                IntegerArgumentType.integer(
                                                                                        1,
                                                                                        5000
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                LifeCycleDebugCommand::executeAdvance
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeDate(
            CommandContext<CommandSourceStack> context
    ) {

        var calendar =
                NpcSimulation.get()
                        .campaignCalendar();

        success(
                context,
                "Campaign date: "
                        + calendar.year()
                        + " AC | day "
                        + (
                        calendar.dayOfYear()
                                + 1
                )
                        + "/"
                        + calendar.daysPerYear()
                        + " | absolute day "
                        + calendar.absoluteDay()
                        + " | "
                        + calendar.ticksPerCampaignDay()
                        + " ticks/day"
        );

        return 1;
    }

    private static int executeScale(
            CommandContext<CommandSourceStack> context
    ) {

        int ticks =
                IntegerArgumentType.getInteger(
                        context,
                        "ticksPerDay"
                );

        NpcSimulation simulation =
                NpcSimulation.get();

        simulation.campaignCalendar()
                .setTicksPerCampaignDay(
                        ticks
                );

        simulation.save();

        success(
                context,
                "Campaign time scale set to "
                        + ticks
                        + " ticks per historical day."
        );

        return 1;
    }

    private static int executeAge(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                npc(
                        context,
                        "npc"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            int age =
                    simulation.lifeHistory()
                            .ageYears(
                                    npc,
                                    simulation.campaignCalendar()
                                            .absoluteDay(),
                                    simulation.campaignCalendar()
                                            .daysPerYear()
                            );

            var stage =
                    simulation.lifeHistory()
                            .lifeStage(
                                    npc,
                                    simulation.campaignCalendar()
                                            .absoluteDay(),
                                    simulation.campaignCalendar()
                                            .daysPerYear()
                            );

            success(
                    context,
                    "NPC #"
                            + npc
                            + " age "
                            + age
                            + " | stage "
                            + stage
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeFertility(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                npc(
                        context,
                        "npc"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            success(
                    context,
                    "NPC #"
                            + npc
                            + " fertility: "
                            + simulation.fertility()
                            .stateOf(
                                    npc
                            )
                            + " | multiplier "
                            + String.format(
                            "%.2f",
                            simulation.fertility()
                                    .fertilityMultiplier(
                                            npc
                                    )
                    )
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executePregnancy(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId mother =
                npc(
                        context,
                        "mother"
                );

        try {

            Pregnancy pregnancy =
                    NpcSimulation.get()
                            .pregnancies()
                            .findByMother(
                                    mother
                            )
                            .orElse(
                                    null
                            );

            if (pregnancy == null) {

                success(
                        context,
                        "NPC #"
                                + mother
                                + " is not pregnant."
                );

                return 1;
            }

            success(
                    context,
                    "NPC #"
                            + mother
                            + " pregnancy | father #"
                            + pregnancy.father()
                            + " | conceived day "
                            + pregnancy.conceivedDay()
                            + " | due day "
                            + pregnancy.dueDay()
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeConceive(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId mother =
                npc(
                        context,
                        "mother"
                );

        NpcId father =
                npc(
                        context,
                        "father"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            Pregnancy pregnancy =
                    simulation.lifeCycle()
                            .forceConception(
                                    mother,
                                    father,
                                    simulation.serverTickCounter()
                            );

            simulation.save();

            success(
                    context,
                    "Pregnancy created | mother #"
                            + mother
                            + " | father #"
                            + father
                            + " | due campaign day "
                            + pregnancy.dueDay()
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeDeath(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                npc(
                        context,
                        "npc"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            String name =
                    simulation.registry()
                            .find(
                                    npc
                            )
                            .map(
                                    state ->
                                            state.identity()
                                                    .fullName()
                            )
                            .orElse(
                                    "NPC #" + npc
                            );

            simulation.lifeCycle()
                    .forceNaturalDeath(
                            npc,
                            simulation.serverTickCounter()
                    );

            simulation.save();

            success(
                    context,
                    "Forced lifecycle death completed for "
                            + name
                            + " [NPC #"
                            + npc
                            + "]."
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeAdvance(
            CommandContext<CommandSourceStack> context
    ) {

        int days =
                IntegerArgumentType.getInteger(
                        context,
                        "days"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            simulation.advanceCampaignDaysForDebug(
                    days
            );

            simulation.save();

            success(
                    context,
                    "Advanced campaign calendar by "
                            + days
                            + " days. Current date: "
                            + simulation.campaignCalendar()
                            .year()
                            + " AC day "
                            + (
                            simulation.campaignCalendar()
                                    .dayOfYear()
                                    + 1
                    )
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static NpcId npc(
            CommandContext<CommandSourceStack> context,
            String name
    ) {

        return new NpcId(
                LongArgumentType.getLong(
                        context,
                        name
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
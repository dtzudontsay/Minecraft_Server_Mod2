package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialHistoryMode;
import dev.dtzudontsay.knownworld.simulation.testing.SimulationFastForwardService;
import dev.dtzudontsay.knownworld.simulation.testing.SimulationReportService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class SimulationHarnessDebugCommand {

    private SimulationHarnessDebugCommand() {
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
                                                "kwsandbox"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "create"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "count",
                                                                                IntegerArgumentType.integer(
                                                                                        2,
                                                                                        10_000
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "cohorts",
                                                                                                IntegerArgumentType.integer(
                                                                                                        1,
                                                                                                        100
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "seed",
                                                                                                                LongArgumentType.longArg()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                context -> {

                                                                                                                    NpcSimulation simulation =
                                                                                                                            NpcSimulation.get();

                                                                                                                    int count =
                                                                                                                            IntegerArgumentType.getInteger(
                                                                                                                                    context,
                                                                                                                                    "count"
                                                                                                                            );

                                                                                                                    int cohorts =
                                                                                                                            IntegerArgumentType.getInteger(
                                                                                                                                    context,
                                                                                                                                    "cohorts"
                                                                                                                            );

                                                                                                                    long seed =
                                                                                                                            LongArgumentType.getLong(
                                                                                                                                    context,
                                                                                                                                    "seed"
                                                                                                                            );

                                                                                                                    try {

                                                                                                                        var created =
                                                                                                                                simulation.syntheticPopulation()
                                                                                                                                        .create(
                                                                                                                                                count,
                                                                                                                                                cohorts,
                                                                                                                                                seed,
                                                                                                                                                simulation.campaignCalendar()
                                                                                                                                                        .year(),
                                                                                                                                                simulation.campaignCalendar()
                                                                                                                                                        .daysPerYear()
                                                                                                                                        );

                                                                                                                        simulation.save();

                                                                                                                        context.getSource()
                                                                                                                                .sendSuccess(
                                                                                                                                        () ->
                                                                                                                                                Component.literal(
                                                                                                                                                        "Created "
                                                                                                                                                                + created.size()
                                                                                                                                                                + " synthetic NPCs across "
                                                                                                                                                                + cohorts
                                                                                                                                                                + " abstract cohorts. Seed="
                                                                                                                                                                + seed
                                                                                                                                                ),
                                                                                                                                        false
                                                                                                                                );

                                                                                                                        return created.size();

                                                                                                                    } catch (
                                                                                                                            RuntimeException exception
                                                                                                                    ) {

                                                                                                                        context.getSource()
                                                                                                                                .sendFailure(
                                                                                                                                        Component.literal(
                                                                                                                                                exception.getMessage()
                                                                                                                                        )
                                                                                                                                );

                                                                                                                        return 0;
                                                                                                                    }
                                                                                                                }
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "status"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    NpcSimulation simulation =
                                                                            NpcSimulation.get();

                                                                    var ids =
                                                                            simulation.syntheticPopulation()
                                                                                    .sandboxNpcIds();

                                                                    long alive =
                                                                            ids.stream()
                                                                                    .map(
                                                                                            simulation.registry()::find
                                                                                    )
                                                                                    .flatMap(
                                                                                            java.util.Optional::stream
                                                                                    )
                                                                                    .filter(
                                                                                            npc ->
                                                                                                    npc.isAlive()
                                                                                    )
                                                                                    .count();

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    "Sandbox population="
                                                                                                            + ids.size()
                                                                                                            + " alive="
                                                                                                            + alive
                                                                                                            + " campaignYear="
                                                                                                            + simulation.campaignCalendar()
                                                                                                            .year()
                                                                                                            + " day="
                                                                                                            + (
                                                                                                            simulation.campaignCalendar()
                                                                                                                    .dayOfYear()
                                                                                                                    + 1
                                                                                                    )
                                                                                            ),
                                                                                    false
                                                                            );

                                                                    return ids.size();
                                                                }
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
                                                                                        36_500
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "history",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                context -> {

                                                                                                    NpcSimulation simulation =
                                                                                                            NpcSimulation.get();

                                                                                                    int days =
                                                                                                            IntegerArgumentType.getInteger(
                                                                                                                    context,
                                                                                                                    "days"
                                                                                                            );

                                                                                                    String rawMode =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "history"
                                                                                                            );

                                                                                                    SocialHistoryMode mode;

                                                                                                    try {

                                                                                                        mode =
                                                                                                                SocialHistoryMode.valueOf(
                                                                                                                        rawMode.toUpperCase(
                                                                                                                                Locale.ROOT
                                                                                                                        )
                                                                                                                );

                                                                                                    } catch (
                                                                                                            IllegalArgumentException exception
                                                                                                    ) {

                                                                                                        context.getSource()
                                                                                                                .sendFailure(
                                                                                                                        Component.literal(
                                                                                                                                "history must be FULL, SIGNIFICANT_ONLY or NONE"
                                                                                                                        )
                                                                                                                );

                                                                                                        return 0;
                                                                                                    }

                                                                                                    var population =
                                                                                                            simulation.syntheticPopulation()
                                                                                                                    .sandboxNpcIds();

                                                                                                    if (population.size()
                                                                                                            < 2) {

                                                                                                        context.getSource()
                                                                                                                .sendFailure(
                                                                                                                        Component.literal(
                                                                                                                                "Create a sandbox population first."
                                                                                                                        )
                                                                                                                );

                                                                                                        return 0;
                                                                                                    }

                                                                                                    long started =
                                                                                                            System.nanoTime();

                                                                                                    SimulationFastForwardService.Result result =
                                                                                                            simulation.fastForward()
                                                                                                                    .advanceDays(
                                                                                                                            days,
                                                                                                                            population,
                                                                                                                            mode
                                                                                                                    );

                                                                                                    simulation.save();

                                                                                                    long elapsedMillis =
                                                                                                            (
                                                                                                                    System.nanoTime()
                                                                                                                            - started
                                                                                                            )
                                                                                                                    / 1_000_000L;

                                                                                                    context.getSource()
                                                                                                            .sendSuccess(
                                                                                                                    () ->
                                                                                                                            Component.literal(
                                                                                                                                    "Advanced "
                                                                                                                                            + result.daysAdvanced()
                                                                                                                                            + " campaign days in "
                                                                                                                                            + elapsedMillis
                                                                                                                                            + " ms"
                                                                                                                            ),
                                                                                                                    false
                                                                                                            );

                                                                                                    context.getSource()
                                                                                                            .sendSuccess(
                                                                                                                    () ->
                                                                                                                            Component.literal(
                                                                                                                                    "Social evaluations="
                                                                                                                                            + result.eligibleActorEvaluations()
                                                                                                                                            + " attempts="
                                                                                                                                            + result.attemptedInteractions()
                                                                                                                                            + " completed="
                                                                                                                                            + result.performedInteractions()
                                                                                                                                            + " historyRecorded="
                                                                                                                                            + result.historyRecords()
                                                                                                                            ),
                                                                                                                    false
                                                                                                            );

                                                                                                    context.getSource()
                                                                                                            .sendSuccess(
                                                                                                                    () ->
                                                                                                                            Component.literal(
                                                                                                                                    "Now campaign year "
                                                                                                                                            + simulation.campaignCalendar()
                                                                                                                                            .year()
                                                                                                                                            + " day "
                                                                                                                                            + (
                                                                                                                                            simulation.campaignCalendar()
                                                                                                                                                    .dayOfYear()
                                                                                                                                                    + 1
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

                                        .then(
                                                Commands.literal(
                                                                "report"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    NpcSimulation simulation =
                                                                            NpcSimulation.get();

                                                                    var population =
                                                                            simulation.syntheticPopulation()
                                                                                    .sandboxNpcIds();

                                                                    if (population.isEmpty()) {

                                                                        context.getSource()
                                                                                .sendFailure(
                                                                                        Component.literal(
                                                                                                "No sandbox population exists."
                                                                                        )
                                                                                );

                                                                        return 0;
                                                                    }

                                                                    SimulationReportService.Report report =
                                                                            simulation.simulationReports()
                                                                                    .build(
                                                                                            population
                                                                                    );

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    String.format(
                                                                                                            Locale.ROOT,
                                                                                                            "Sandbox report | population=%d alive=%d directionalRelations=%d positive=%d hostile=%d saturated=%d",
                                                                                                            report.population(),
                                                                                                            report.alive(),
                                                                                                            report.directionalRelationships(),
                                                                                                            report.positiveRelationships(),
                                                                                                            report.hostileRelationships(),
                                                                                                            report.saturatedRelationships()
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
                                                                                                            "Averages | affection=%.3f trust=%.3f respect=%.3f fear=%.3f",
                                                                                                            report.averageAffection(),
                                                                                                            report.averageTrust(),
                                                                                                            report.averageRespect(),
                                                                                                            report.averageFear()
                                                                                                    )
                                                                                            ),
                                                                                    false
                                                                            );

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    "Strongest positive: "
                                                                                                            + report.strongestPositive()
                                                                                            ),
                                                                                    false
                                                                            );

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    "Strongest negative: "
                                                                                                            + report.strongestNegative()
                                                                                            ),
                                                                                    false
                                                                            );

                                                                    return 1;
                                                                }
                                                        )
                                        )
                        )
        );
    }
}
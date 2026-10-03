package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialActionDecisionService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialHistoryMode;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialInteractionType;
import dev.dtzudontsay.knownworld.simulation.testing.SimulationFastForwardService;
import dev.dtzudontsay.knownworld.simulation.testing.SimulationReportService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.SplittableRandom;

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

                                        /*
                                         * -------------------------------------------------
                                         * CREATE
                                         * -------------------------------------------------
                                         */
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
                                                                                                                                                                + " noble houses with persistent household affiliations. Seed="
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

                                        /*
                                         * -------------------------------------------------
                                         * STATUS
                                         * -------------------------------------------------
                                         */
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
                                                                                            NpcState::isAlive
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

                                        /*
                                         * -------------------------------------------------
                                         * ADVANCE
                                         * -------------------------------------------------
                                         */
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

                                        /*
                                         * -------------------------------------------------
                                         * REPORT
                                         * -------------------------------------------------
                                         */
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
                                                                                                            "Social topology | sameHousehold=%d sameNobleHouse=%d crossNobleHouse=%d",
                                                                                                            report.sameHouseholdRelationships(),
                                                                                                            report.sameNobleHouseRelationships(),
                                                                                                            report.crossNobleHouseRelationships()
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
                                                                                                            "Relationship averages | affection=%.3f trust=%.3f respect=%.3f fear=%.3f",
                                                                                                            report.averageAffection(),
                                                                                                            report.averageTrust(),
                                                                                                            report.averageRespect(),
                                                                                                            report.averageFear()
                                                                                                    )
                                                                                            ),
                                                                                    false
                                                                            );

                                                                    SimulationReportService.SkillReport skills =
                                                                            report.skills();

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    String.format(
                                                                                                            Locale.ROOT,
                                                                                                            "Skill averages | diplomacy=%.5f intrigue=%.5f leadership=%.5f",
                                                                                                            skills.averageDiplomacy(),
                                                                                                            skills.averageIntrigue(),
                                                                                                            skills.averageLeadership()
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
                                                                                                            "Highest learned skill | %s %s = %.5f",
                                                                                                            skills.highestNpc(),
                                                                                                            skills.highestSkill()
                                                                                                                    == null
                                                                                                                    ? "NONE"
                                                                                                                    : skills.highestSkill()
                                                                                                                    .name(),
                                                                                                            skills.highestSkillValue()
                                                                                                    )
                                                                                            ),
                                                                                    false
                                                                            );

                                                                    SimulationReportService.MemoryReport memories =
                                                                            report.memories();

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    String.format(
                                                                                                            Locale.ROOT,
                                                                                                            "Semantic memories | total=%d positive=%d negative=%d betrayal=%d threat=%d",
                                                                                                            memories.semanticMemories(),
                                                                                                            memories.positiveMemories(),
                                                                                                            memories.negativeMemories(),
                                                                                                            memories.betrayalMemories(),
                                                                                                            memories.threatMemories()
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
                                                                                                            "Stored memories | total=%d maxPerNpc=%d softLimit=%d hardLimit=%d",
                                                                                                            memories.storedMemories(),
                                                                                                            memories.maximumMemoriesForOneNpc(),
                                                                                                            memories.softLimitPerNpc(),
                                                                                                            memories.hardLimitPerNpc()
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

                                        /*
                                         * -------------------------------------------------
                                         * SANDBOX SOCIAL ACTION
                                         *
                                         * Example:
                                         *
                                         * /kwsandbox social Sim0001 Sim0002 INSULT 1.0
                                         *
                                         * The first NPC performs the action.
                                         * The second NPC is the target.
                                         * -------------------------------------------------
                                         */
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
                                                                                                                                        0.01,
                                                                                                                                        1.0
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .executes(
                                                                                                                                context -> {

                                                                                                                                    NpcSimulation simulation =
                                                                                                                                            NpcSimulation.get();

                                                                                                                                    String actorToken =
                                                                                                                                            StringArgumentType.getString(
                                                                                                                                                    context,
                                                                                                                                                    "actor"
                                                                                                                                            );

                                                                                                                                    String targetToken =
                                                                                                                                            StringArgumentType.getString(
                                                                                                                                                    context,
                                                                                                                                                    "target"
                                                                                                                                            );

                                                                                                                                    String typeToken =
                                                                                                                                            StringArgumentType.getString(
                                                                                                                                                    context,
                                                                                                                                                    "type"
                                                                                                                                            );

                                                                                                                                    double magnitude =
                                                                                                                                            DoubleArgumentType.getDouble(
                                                                                                                                                    context,
                                                                                                                                                    "magnitude"
                                                                                                                                            );

                                                                                                                                    NpcState actor =
                                                                                                                                            findSandboxNpc(
                                                                                                                                                    simulation,
                                                                                                                                                    actorToken
                                                                                                                                            );

                                                                                                                                    if (actor == null) {

                                                                                                                                        context.getSource()
                                                                                                                                                .sendFailure(
                                                                                                                                                        Component.literal(
                                                                                                                                                                "Unknown sandbox NPC: "
                                                                                                                                                                        + actorToken
                                                                                                                                                        )
                                                                                                                                                );

                                                                                                                                        return 0;
                                                                                                                                    }

                                                                                                                                    NpcState target =
                                                                                                                                            findSandboxNpc(
                                                                                                                                                    simulation,
                                                                                                                                                    targetToken
                                                                                                                                            );

                                                                                                                                    if (target == null) {

                                                                                                                                        context.getSource()
                                                                                                                                                .sendFailure(
                                                                                                                                                        Component.literal(
                                                                                                                                                                "Unknown sandbox NPC: "
                                                                                                                                                                        + targetToken
                                                                                                                                                        )
                                                                                                                                                );

                                                                                                                                        return 0;
                                                                                                                                    }

                                                                                                                                    SocialInteractionType type;

                                                                                                                                    try {

                                                                                                                                        type =
                                                                                                                                                SocialInteractionType.valueOf(
                                                                                                                                                        typeToken.toUpperCase(
                                                                                                                                                                Locale.ROOT
                                                                                                                                                        )
                                                                                                                                                );

                                                                                                                                    } catch (
                                                                                                                                            IllegalArgumentException exception
                                                                                                                                    ) {

                                                                                                                                        context.getSource()
                                                                                                                                                .sendFailure(
                                                                                                                                                        Component.literal(
                                                                                                                                                                "type must be HELP, PRAISE, INSULT, THREATEN or BETRAY"
                                                                                                                                                        )
                                                                                                                                                );

                                                                                                                                        return 0;
                                                                                                                                    }

                                                                                                                                    try {

                                                                                                                                        var result =
                                                                                                                                                simulation.socialInteractions()
                                                                                                                                                        .perform(
                                                                                                                                                                actor.id(),
                                                                                                                                                                target.id(),
                                                                                                                                                                type,
                                                                                                                                                                magnitude,
                                                                                                                                                                simulation.serverTickCounter(),
                                                                                                                                                                SocialHistoryMode.FULL
                                                                                                                                                        );

                                                                                                                                        simulation.save();

                                                                                                                                        context.getSource()
                                                                                                                                                .sendSuccess(
                                                                                                                                                        () ->
                                                                                                                                                                Component.literal(
                                                                                                                                                                        String.format(
                                                                                                                                                                                Locale.ROOT,
                                                                                                                                                                                "%s -> %s %s magnitude=%.2f",
                                                                                                                                                                                actor.identity()
                                                                                                                                                                                        .fullName(),
                                                                                                                                                                                target.identity()
                                                                                                                                                                                        .fullName(),
                                                                                                                                                                                type.name(),
                                                                                                                                                                                magnitude
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
                                                                                                                                                                                "Target relationship BEFORE | affection=%.3f trust=%.3f respect=%.3f fear=%.3f familiarity=%.3f",
                                                                                                                                                                                result.before()
                                                                                                                                                                                        .affection(),
                                                                                                                                                                                result.before()
                                                                                                                                                                                        .trust(),
                                                                                                                                                                                result.before()
                                                                                                                                                                                        .respect(),
                                                                                                                                                                                result.before()
                                                                                                                                                                                        .fear(),
                                                                                                                                                                                result.before()
                                                                                                                                                                                        .familiarity()
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
                                                                                                                                                                                "Target relationship AFTER  | affection=%.3f trust=%.3f respect=%.3f fear=%.3f familiarity=%.3f",
                                                                                                                                                                                result.after()
                                                                                                                                                                                        .affection(),
                                                                                                                                                                                result.after()
                                                                                                                                                                                        .trust(),
                                                                                                                                                                                result.after()
                                                                                                                                                                                        .respect(),
                                                                                                                                                                                result.after()
                                                                                                                                                                                        .fear(),
                                                                                                                                                                                result.after()
                                                                                                                                                                                        .familiarity()
                                                                                                                                                                        )
                                                                                                                                                                ),
                                                                                                                                                        false
                                                                                                                                                );

                                                                                                                                        return 1;

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
                                        )

                                        /*
                                         * -------------------------------------------------
                                         * NPC PAIR INSPECTION
                                         *
                                         * Example:
                                         *
                                         * /kwsandbox npc Sim0002 Sim0001
                                         *
                                         * Subject = NPC whose opinion/memory is inspected.
                                         * Target  = NPC they are thinking about.
                                         * -------------------------------------------------
                                         */
                                        .then(
                                                Commands.literal(
                                                                "npc"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "subject",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "target",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                context -> {

                                                                                                    NpcSimulation simulation =
                                                                                                            NpcSimulation.get();

                                                                                                    String subjectToken =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "subject"
                                                                                                            );

                                                                                                    String targetToken =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "target"
                                                                                                            );

                                                                                                    NpcState subject =
                                                                                                            findSandboxNpc(
                                                                                                                    simulation,
                                                                                                                    subjectToken
                                                                                                            );

                                                                                                    if (subject == null) {

                                                                                                        context.getSource()
                                                                                                                .sendFailure(
                                                                                                                        Component.literal(
                                                                                                                                "Unknown sandbox NPC: "
                                                                                                                                        + subjectToken
                                                                                                                        )
                                                                                                                );

                                                                                                        return 0;
                                                                                                    }

                                                                                                    NpcState target =
                                                                                                            findSandboxNpc(
                                                                                                                    simulation,
                                                                                                                    targetToken
                                                                                                            );

                                                                                                    if (target == null) {

                                                                                                        context.getSource()
                                                                                                                .sendFailure(
                                                                                                                        Component.literal(
                                                                                                                                "Unknown sandbox NPC: "
                                                                                                                                        + targetToken
                                                                                                                        )
                                                                                                                );

                                                                                                        return 0;
                                                                                                    }

                                                                                                    NpcRelationship relationship =
                                                                                                            simulation.relationships()
                                                                                                                    .find(
                                                                                                                            subject.id(),
                                                                                                                            target.id()
                                                                                                                    )
                                                                                                                    .orElse(
                                                                                                                            null
                                                                                                                    );

                                                                                                    context.getSource()
                                                                                                            .sendSuccess(
                                                                                                                    () ->
                                                                                                                            Component.literal(
                                                                                                                                    subject.identity()
                                                                                                                                            .fullName()
                                                                                                                                            + " [id="
                                                                                                                                            + subject.id()
                                                                                                                                            + "] -> "
                                                                                                                                            + target.identity()
                                                                                                                                            .fullName()
                                                                                                                                            + " [id="
                                                                                                                                            + target.id()
                                                                                                                                            + "]"
                                                                                                                            ),
                                                                                                                    false
                                                                                                            );

                                                                                                    if (relationship == null) {

                                                                                                        context.getSource()
                                                                                                                .sendSuccess(
                                                                                                                        () ->
                                                                                                                                Component.literal(
                                                                                                                                        "Relationship | no directional relationship exists yet"
                                                                                                                                ),
                                                                                                                        false
                                                                                                                );

                                                                                                    } else {

                                                                                                        context.getSource()
                                                                                                                .sendSuccess(
                                                                                                                        () ->
                                                                                                                                Component.literal(
                                                                                                                                        String.format(
                                                                                                                                                Locale.ROOT,
                                                                                                                                                "Relationship | affection=%.3f trust=%.3f respect=%.3f fear=%.3f familiarity=%.3f",
                                                                                                                                                relationship.affection(),
                                                                                                                                                relationship.trust(),
                                                                                                                                                relationship.respect(),
                                                                                                                                                relationship.fear(),
                                                                                                                                                relationship.familiarity()
                                                                                                                                        )
                                                                                                                                ),
                                                                                                                        false
                                                                                                                );
                                                                                                    }

                                                                                                    var memory =
                                                                                                            simulation.memoryMeaning()
                                                                                                                    .toward(
                                                                                                                            subject.id(),
                                                                                                                            target.id(),
                                                                                                                            simulation.serverTickCounter()
                                                                                                                    );

                                                                                                    context.getSource()
                                                                                                            .sendSuccess(
                                                                                                                    () ->
                                                                                                                            Component.literal(
                                                                                                                                    String.format(
                                                                                                                                            Locale.ROOT,
                                                                                                                                            "Memory meaning | relevant=%d positive=%.3f negative=%.3f betrayal=%.3f threat=%.3f salience=%.3f valence=%.3f",
                                                                                                                                            memory.relevantMemories(),
                                                                                                                                            memory.positive(),
                                                                                                                                            memory.negative(),
                                                                                                                                            memory.betrayal(),
                                                                                                                                            memory.threat(),
                                                                                                                                            memory.totalSalience(),
                                                                                                                                            memory.signedValence()
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

                                        /*
                                         * -------------------------------------------------
                                         * DECISION INSPECTION
                                         *
                                         * Example:
                                         *
                                         * /kwsandbox decision Sim0002 Sim0001
                                         *
                                         * Does not perform an action.
                                         * -------------------------------------------------
                                         */
                                        .then(
                                                Commands.literal(
                                                                "decision"
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
                                                                                        .executes(
                                                                                                context -> {

                                                                                                    NpcSimulation simulation =
                                                                                                            NpcSimulation.get();

                                                                                                    String actorToken =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "actor"
                                                                                                            );

                                                                                                    String targetToken =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "target"
                                                                                                            );

                                                                                                    NpcState actor =
                                                                                                            findSandboxNpc(
                                                                                                                    simulation,
                                                                                                                    actorToken
                                                                                                            );

                                                                                                    if (actor == null) {

                                                                                                        context.getSource()
                                                                                                                .sendFailure(
                                                                                                                        Component.literal(
                                                                                                                                "Unknown sandbox NPC: "
                                                                                                                                        + actorToken
                                                                                                                        )
                                                                                                                );

                                                                                                        return 0;
                                                                                                    }

                                                                                                    NpcState target =
                                                                                                            findSandboxNpc(
                                                                                                                    simulation,
                                                                                                                    targetToken
                                                                                                            );

                                                                                                    if (target == null) {

                                                                                                        context.getSource()
                                                                                                                .sendFailure(
                                                                                                                        Component.literal(
                                                                                                                                "Unknown sandbox NPC: "
                                                                                                                                        + targetToken
                                                                                                                        )
                                                                                                                );

                                                                                                        return 0;
                                                                                                    }

                                                                                                    long seed =
                                                                                                            actor.id()
                                                                                                                    .value()
                                                                                                                    * 31L
                                                                                                                    +
                                                                                                                    target.id()
                                                                                                                            .value();

                                                                                                    SplittableRandom random =
                                                                                                            new SplittableRandom(
                                                                                                                    seed
                                                                                                            );

                                                                                                    SocialActionDecisionService.Decision decision;

                                                                                                    try {

                                                                                                        decision =
                                                                                                                simulation.socialActionDecisions()
                                                                                                                        .choose(
                                                                                                                                actor.id(),
                                                                                                                                target.id(),
                                                                                                                                simulation.serverTickCounter(),
                                                                                                                                random
                                                                                                                        );

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

                                                                                                    context.getSource()
                                                                                                            .sendSuccess(
                                                                                                                    () ->
                                                                                                                            Component.literal(
                                                                                                                                    actor.identity()
                                                                                                                                            .fullName()
                                                                                                                                            + " -> "
                                                                                                                                            + target.identity()
                                                                                                                                            .fullName()
                                                                                                                            ),
                                                                                                                    false
                                                                                                            );

                                                                                                    context.getSource()
                                                                                                            .sendSuccess(
                                                                                                                    () ->
                                                                                                                            Component.literal(
                                                                                                                                    String.format(
                                                                                                                                            Locale.ROOT,
                                                                                                                                            "Decision weights | HELP=%.4f PRAISE=%.4f INSULT=%.4f THREATEN=%.4f BETRAY=%.4f",
                                                                                                                                            decision.helpWeight(),
                                                                                                                                            decision.praiseWeight(),
                                                                                                                                            decision.insultWeight(),
                                                                                                                                            decision.threatenWeight(),
                                                                                                                                            decision.betrayWeight()
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
                                                                                                                                            "Memory inputs | salience=%.4f positive=%.4f negative=%.4f betrayal=%.4f threat=%.4f",
                                                                                                                                            decision.memorySalience(),
                                                                                                                                            decision.positiveMemory(),
                                                                                                                                            decision.negativeMemory(),
                                                                                                                                            decision.betrayalMemory(),
                                                                                                                                            decision.threatMemory()
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
                                                                                                                                            "Preview choice | %s magnitude=%.4f",
                                                                                                                                            decision.type()
                                                                                                                                                    .name(),
                                                                                                                                            decision.magnitude()
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
        );
    }

    /**
     * Sandbox-native NPC resolver.
     *
     * Accepts generated sandbox names such as:
     *
     * Sim0001
     *
     * or permanent numeric runtime IDs such as:
     *
     * 22
     */
    private static NpcState findSandboxNpc(
            NpcSimulation simulation,
            String token
    ) {

        String wanted =
                token.trim();

        if (wanted.isEmpty()) {

            return null;
        }

        /*
         * First try permanent numeric runtime ID.
         */
        try {

            long rawId =
                    Long.parseLong(
                            wanted
                    );

            if (rawId > 0L) {

                NpcState byId =
                        simulation.registry()
                                .find(
                                        new NpcId(
                                                rawId
                                        )
                                )
                                .orElse(
                                        null
                                );

                if (byId != null
                        &&
                        simulation.syntheticPopulation()
                                .sandboxNpcIds()
                                .contains(
                                        byId.id()
                                )) {

                    return byId;
                }
            }

        } catch (
                NumberFormatException ignored
        ) {

            /*
             * Not numeric.
             * Fall through to generated-name lookup.
             */
        }

        for (
                NpcId id :
                simulation.syntheticPopulation()
                        .sandboxNpcIds()
        ) {

            NpcState npc =
                    simulation.registry()
                            .find(
                                    id
                            )
                            .orElse(
                                    null
                            );

            if (npc == null) {

                continue;
            }

            if (npc.identity()
                    .givenName()
                    .equalsIgnoreCase(
                            wanted
                    )) {

                return npc;
            }

            if (npc.identity()
                    .fullName()
                    .equalsIgnoreCase(
                            wanted
                    )) {

                return npc;
            }
        }

        return null;
    }
}
package dev.dtzudontsay.knownworld.simulation.npc;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoal;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalId;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalType;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcBelief;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemory;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Locale;

public final class NpcDebugCommand {

    private NpcDebugCommand() {
    }

    public static void register() {

        CommandRegistrationCallback.EVENT.register(
                (
                        dispatcher,
                        registryAccess,
                        environment
                ) ->
                        dispatcher.register(

                                Commands.literal("kwnpc")

                                        .then(
                                                Commands.literal("count")
                                                        .executes(
                                                                NpcDebugCommand::executeCount
                                                        )
                                        )

                                        .then(
                                                Commands.literal("list")
                                                        .executes(
                                                                NpcDebugCommand::executeList
                                                        )
                                        )

                                        .then(
                                                Commands.literal("save")
                                                        .executes(
                                                                NpcDebugCommand::executeSave
                                                        )
                                        )

                                        .then(
                                                Commands.literal("inspect")
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                NpcDebugCommand::executeInspect
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("personality")
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                NpcDebugCommand::executePersonality
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("relationship")
                                                        .then(
                                                                Commands.argument(
                                                                                "subject",
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
                                                                                        .executes(
                                                                                                NpcDebugCommand::executeRelationship
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("relationship_change")
                                                        .then(
                                                                Commands.argument(
                                                                                "subject",
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
                                                                                                                "dimension",
                                                                                                                StringArgumentType.word()
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "amount",
                                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                                        -2.0,
                                                                                                                                        2.0
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .executes(
                                                                                                                                NpcDebugCommand::executeRelationshipChange
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("believe")
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "factKey",
                                                                                                StringArgumentType.string()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "value",
                                                                                                                StringArgumentType.string()
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "confidence",
                                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                                        0.0,
                                                                                                                                        1.0
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .executes(
                                                                                                                                NpcDebugCommand::executeBelieve
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("beliefs")
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                NpcDebugCommand::executeBeliefs
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("share")
                                                        .then(
                                                                Commands.argument(
                                                                                "speaker",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "listener",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "factKey",
                                                                                                                StringArgumentType.string()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                NpcDebugCommand::executeShare
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("memories")
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                NpcDebugCommand::executeMemories
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("memory_add")
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "importance",
                                                                                                DoubleArgumentType.doubleArg(
                                                                                                        0.0,
                                                                                                        1.0
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "summary",
                                                                                                                StringArgumentType.greedyString()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                NpcDebugCommand::executeMemoryAdd
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("goals")
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                NpcDebugCommand::executeGoals
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("goal_travel")
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "x",
                                                                                                DoubleArgumentType.doubleArg()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "y",
                                                                                                                DoubleArgumentType.doubleArg()
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "z",
                                                                                                                                DoubleArgumentType.doubleArg()
                                                                                                                        )
                                                                                                                        .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "priority",
                                                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                                                        0.0,
                                                                                                                                                        1.0
                                                                                                                                                )
                                                                                                                                        )
                                                                                                                                        .then(
                                                                                                                                                Commands.argument(
                                                                                                                                                                "description",
                                                                                                                                                                StringArgumentType.greedyString()
                                                                                                                                                        )
                                                                                                                                                        .executes(
                                                                                                                                                                NpcDebugCommand::executeGoalTravel
                                                                                                                                                        )
                                                                                                                                        )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("goal_cancel")
                                                        .then(
                                                                Commands.argument(
                                                                                "goal",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                NpcDebugCommand::executeGoalCancel
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("create")
                                                        .then(
                                                                Commands.argument(
                                                                                "givenName",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "familyName",
                                                                                                StringArgumentType.word()
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
                                                                                                                        .executes(
                                                                                                                                NpcDebugCommand::executeCreate
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeGoalTravel(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcState npc =
                findNpc(
                        source,
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        if (npc == null) {
            return 0;
        }

        double x =
                DoubleArgumentType.getDouble(
                        context,
                        "x"
                );

        double y =
                DoubleArgumentType.getDouble(
                        context,
                        "y"
                );

        double z =
                DoubleArgumentType.getDouble(
                        context,
                        "z"
                );

        double priority =
                DoubleArgumentType.getDouble(
                        context,
                        "priority"
                );

        String description =
                StringArgumentType.getString(
                        context,
                        "description"
                );

        NpcSimulation simulation =
                NpcSimulation.get();

        NpcGoal goal =
                simulation.goals()
                        .create(
                                npc.id(),
                                NpcGoalType.TRAVEL,
                                description,
                                priority,
                                simulation.serverTickCounter(),
                                null,
                                new SimulationPosition(
                                        npc.position()
                                                .dimension(),
                                        x,
                                        y,
                                        z
                                ),
                                null
                        );

        simulation.save();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Created travel goal #"
                                        + goal.id()
                                        + " for NPC #"
                                        + npc.id()
                                        + " -> "
                                        + x
                                        + ", "
                                        + y
                                        + ", "
                                        + z
                        ),
                false
        );

        return 1;
    }

    private static int executeGoals(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        try {

            var goals =
                    NpcSimulation.get()
                            .goals()
                            .goalsOf(
                                    npc
                            );

            if (goals.isEmpty()) {

                source.sendSuccess(
                        () ->
                                Component.literal(
                                        "NPC #"
                                                + npc
                                                + " has no goals."
                                ),
                        false
                );

                return 1;
            }

            for (NpcGoal goal : goals) {

                source.sendSuccess(
                        () ->
                                Component.literal(
                                        String.format(
                                                Locale.ROOT,
                                                "#%s [%s/%s] priority %.2f | %s",
                                                goal.id(),
                                                goal.type(),
                                                goal.status(),
                                                goal.priority(),
                                                goal.description()
                                        )
                                ),
                        false
                );
            }

            return goals.size();

        } catch (IllegalArgumentException exception) {

            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }
    }

    private static int executeGoalCancel(
            CommandContext<CommandSourceStack> context
    ) {

        NpcGoalId id =
                new NpcGoalId(
                        LongArgumentType.getLong(
                                context,
                                "goal"
                        )
                );

        NpcGoal goal =
                NpcSimulation.get()
                        .goals()
                        .find(
                                id
                        )
                        .orElse(
                                null
                        );

        if (goal == null) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "No goal exists with ID "
                                            + id
                            )
                    );

            return 0;
        }

        goal.cancel();

        NpcSimulation.get()
                .save();

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "Cancelled goal #"
                                                + id
                                ),
                        false
                );

        return 1;
    }

    private static int executeCreate(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        ServerPlayer player;

        try {

            player =
                    source.getPlayerOrException();

        } catch (Exception exception) {

            source.sendFailure(
                    Component.literal(
                            "This command must be executed by a player."
                    )
            );

            return 0;
        }

        String givenName =
                StringArgumentType.getString(
                        context,
                        "givenName"
                );

        String familyName =
                StringArgumentType.getString(
                        context,
                        "familyName"
                );

        String sexInput =
                StringArgumentType.getString(
                        context,
                        "sex"
                );

        int birthYear =
                IntegerArgumentType.getInteger(
                        context,
                        "birthYear"
                );

        NpcSex sex;

        try {

            sex =
                    NpcSex.valueOf(
                            sexInput.toUpperCase(
                                    Locale.ROOT
                            )
                    );

        } catch (IllegalArgumentException exception) {

            source.sendFailure(
                    Component.literal(
                            "Sex must be MALE or FEMALE."
                    )
            );

            return 0;
        }

        Vec3 playerPosition =
                player.position();

        String dimension =
                player.level()
                        .dimension()
                        .identifier()
                        .toString();

        SimulationPosition position =
                new SimulationPosition(
                        dimension,
                        playerPosition.x,
                        playerPosition.y,
                        playerPosition.z
                );

        NpcState npc =
                NpcSimulation.get()
                        .registry()
                        .create(
                                givenName,
                                familyName,
                                sex,
                                birthYear,
                                position
                        );

        NpcSimulation.get()
                .save();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Created NPC #"
                                        + npc.id()
                                        + " - "
                                        + npc.identity()
                                        .fullName()
                        ),
                false
        );

        return 1;
    }

    private static int executeInspect(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcState npc =
                findNpc(
                        source,
                        LongArgumentType.getLong(
                                context,
                                "id"
                        )
                );

        if (npc == null) {
            return 0;
        }

        NpcIdentity identity =
                npc.identity();

        SimulationPosition position =
                npc.position();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "NPC #"
                                        + npc.id()
                                        + " - "
                                        + identity.fullName()
                        ),
                false
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Sex: "
                                        + identity.sex()
                                        + " | Birth year: "
                                        + identity.birthYear()
                        ),
                false
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Life: "
                                        + npc.lifeState()
                                        + " | Simulation: "
                                        + npc.simulationLevel()
                        ),
                false
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "Position: %s | %.2f, %.2f, %.2f",
                                        position.dimension(),
                                        position.x(),
                                        position.y(),
                                        position.z()
                                )
                        ),
                false
        );

        return 1;
    }

    private static int executePersonality(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcState npc =
                findNpc(
                        source,
                        LongArgumentType.getLong(
                                context,
                                "id"
                        )
                );

        if (npc == null) {
            return 0;
        }

        NpcPersonality personality =
                npc.personality();

        source.sendSuccess(
                () ->
                        Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "Personality #%s | courage %.2f | ambition %.2f | compassion %.2f | honor %.2f | patience %.2f | sociability %.2f",
                                        npc.id(),
                                        personality.courage(),
                                        personality.ambition(),
                                        personality.compassion(),
                                        personality.honor(),
                                        personality.patience(),
                                        personality.sociability()
                                )
                        ),
                false
        );

        return 1;
    }

    private static int executeRelationship(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcId subject =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "subject"
                        )
                );

        NpcId target =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "target"
                        )
                );

        try {

            NpcRelationship relationship =
                    NpcSimulation.get()
                            .relationships()
                            .getOrCreate(
                                    subject,
                                    target
                            );

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    String.format(
                                            Locale.ROOT,
                                            "%s -> %s | affection %.2f | trust %.2f | respect %.2f | fear %.2f | familiarity %.2f",
                                            subject,
                                            target,
                                            relationship.affection(),
                                            relationship.trust(),
                                            relationship.respect(),
                                            relationship.fear(),
                                            relationship.familiarity()
                                    )
                            ),
                    false
            );

            return 1;

        } catch (IllegalArgumentException exception) {

            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }
    }

    private static int executeRelationshipChange(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcId subject =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "subject"
                        )
                );

        NpcId target =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "target"
                        )
                );

        String dimension =
                StringArgumentType.getString(
                                context,
                                "dimension"
                        )
                        .toLowerCase(
                                Locale.ROOT
                        );

        double amount =
                DoubleArgumentType.getDouble(
                        context,
                        "amount"
                );

        try {

            NpcRelationship relationship =
                    NpcSimulation.get()
                            .relationships()
                            .getOrCreate(
                                    subject,
                                    target
                            );

            switch (dimension) {

                case "affection" ->
                        relationship.changeAffection(
                                amount
                        );

                case "trust" ->
                        relationship.changeTrust(
                                amount
                        );

                case "respect" ->
                        relationship.changeRespect(
                                amount
                        );

                case "fear" ->
                        relationship.changeFear(
                                amount
                        );

                case "familiarity" ->
                        relationship.increaseFamiliarity(
                                amount
                        );

                default -> {

                    source.sendFailure(
                            Component.literal(
                                    "Relationship dimension must be affection, trust, respect, fear or familiarity."
                            )
                    );

                    return 0;
                }
            }

            NpcSimulation.get()
                    .save();

            return executeRelationship(
                    context
            );

        } catch (IllegalArgumentException exception) {

            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }
    }

    private static int executeBelieve(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        String factKey =
                StringArgumentType.getString(
                        context,
                        "factKey"
                );

        String value =
                StringArgumentType.getString(
                        context,
                        "value"
                );

        double confidence =
                DoubleArgumentType.getDouble(
                        context,
                        "confidence"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            simulation.knowledge()
                    .believe(
                            npc,
                            factKey,
                            value,
                            confidence,
                            null,
                            simulation.serverTickCounter()
                    );

            simulation.save();

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "NPC #"
                                            + npc
                                            + " now believes "
                                            + factKey
                                            + " = "
                                            + value
                                            + " with confidence "
                                            + confidence
                            ),
                    false
            );

            return 1;

        } catch (IllegalArgumentException exception) {

            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }
    }

    private static int executeBeliefs(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        if (!NpcSimulation.get()
                .registry()
                .contains(
                        npc
                )) {

            source.sendFailure(
                    Component.literal(
                            "Unknown NPC ID: "
                                    + npc
                    )
            );

            return 0;
        }

        var beliefs =
                NpcSimulation.get()
                        .knowledge()
                        .beliefsOf(
                                npc
                        );

        if (beliefs.isEmpty()) {

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "NPC #"
                                            + npc
                                            + " currently has no stored beliefs."
                            ),
                    false
            );

            return 1;
        }

        for (NpcBelief belief : beliefs) {

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    String.format(
                                            Locale.ROOT,
                                            "%s = %s | confidence %.2f | source %s",
                                            belief.factKey(),
                                            belief.value(),
                                            belief.confidence(),
                                            belief.sourceNpc() == null
                                                    ? "direct/unknown"
                                                    : belief.sourceNpc()
                                                    .toString()
                                    )
                            ),
                    false
            );
        }

        return beliefs.size();
    }

    private static int executeShare(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcId speaker =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "speaker"
                        )
                );

        NpcId listener =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "listener"
                        )
                );

        String factKey =
                StringArgumentType.getString(
                        context,
                        "factKey"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            NpcBelief received =
                    simulation.communication()
                            .shareBelief(
                                    speaker,
                                    listener,
                                    factKey,
                                    simulation.serverTickCounter()
                            );

            simulation.save();

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    String.format(
                                            Locale.ROOT,
                                            "NPC #%s told NPC #%s: %s = %s | listener confidence %.2f",
                                            speaker,
                                            listener,
                                            received.factKey(),
                                            received.value(),
                                            received.confidence()
                                    )
                            ),
                    false
            );

            return 1;

        } catch (IllegalArgumentException exception) {

            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }
    }

    private static int executeMemories(
            CommandContext<CommandSourceStack> context
    ) {

        CommandSourceStack source =
                context.getSource();

        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        try {

            var memories =
                    NpcSimulation.get()
                            .memories()
                            .memoriesOf(
                                    npc
                            );

            if (memories.isEmpty()) {

                source.sendSuccess(
                        () ->
                                Component.literal(
                                        "NPC #"
                                                + npc
                                                + " currently has no stored memories."
                                ),
                        false
                );

                return 1;
            }

            for (NpcMemory memory : memories) {

                source.sendSuccess(
                        () ->
                                Component.literal(
                                        String.format(
                                                Locale.ROOT,
                                                "#%s [%s] importance %.2f | %s",
                                                memory.id(),
                                                memory.type(),
                                                memory.importance(),
                                                memory.summary()
                                        )
                                ),
                        false
                );
            }

            return memories.size();

        } catch (IllegalArgumentException exception) {

            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }
    }

    private static int executeMemoryAdd(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        double importance =
                DoubleArgumentType.getDouble(
                        context,
                        "importance"
                );

        String summary =
                StringArgumentType.getString(
                        context,
                        "summary"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            simulation.memories()
                    .remember(
                            npc,
                            NpcMemoryType.PERSONAL_EXPERIENCE,
                            summary,
                            importance,
                            null,
                            null,
                            simulation.serverTickCounter()
                    );

            simulation.save();

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

    private static int executeList(
            CommandContext<CommandSourceStack> context
    ) {

        var npcs =
                NpcSimulation.get()
                        .registry()
                        .all()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        NpcState::id
                                )
                        )
                        .toList();

        for (NpcState npc : npcs) {

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "#"
                                                    + npc.id()
                                                    + " "
                                                    + npc.identity()
                                                    .fullName()
                                                    + " ["
                                                    + npc.lifeState()
                                                    + "]"
                                    ),
                            false
                    );
        }

        return npcs.size();
    }

    private static int executeCount(
            CommandContext<CommandSourceStack> context
    ) {

        NpcSimulation simulation =
                NpcSimulation.get();

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "NPC population: "
                                                + simulation.registry()
                                                .size()
                                                + " | relationships "
                                                + simulation.relationships()
                                                .size()
                                                + " | beliefs "
                                                + simulation.knowledge()
                                                .size()
                                                + " | memories "
                                                + simulation.memories()
                                                .size()
                                                + " | goals "
                                                + simulation.goals()
                                                .size()
                                ),
                        false
                );

        return simulation.registry()
                .size();
    }

    private static int executeSave(
            CommandContext<CommandSourceStack> context
    ) {

        NpcSimulation.get()
                .save();

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "NPC simulation saved."
                                ),
                        false
                );

        return 1;
    }

    private static NpcState findNpc(
            CommandSourceStack source,
            long rawId
    ) {

        NpcId id =
                new NpcId(
                        rawId
                );

        NpcState npc =
                NpcSimulation.get()
                        .registry()
                        .find(
                                id
                        )
                        .orElse(
                                null
                        );

        if (npc == null) {

            source.sendFailure(
                    Component.literal(
                            "No NPC exists with ID "
                                    + id
                    )
            );
        }

        return npc;
    }
}
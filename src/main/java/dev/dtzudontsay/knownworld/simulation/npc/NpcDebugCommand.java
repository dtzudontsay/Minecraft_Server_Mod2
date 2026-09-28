package dev.dtzudontsay.knownworld.simulation.npc;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
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

        /*
         * Save immediately while the system is under development.
         *
         * Later this will be replaced by dirty-state tracking and
         * scheduled persistence.
         */
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

        long rawId =
                LongArgumentType.getLong(
                        context,
                        "id"
                );

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

    private static int executeList(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source =
                context.getSource();

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

        if (npcs.isEmpty()) {

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "No NPCs exist."
                            ),
                    false
            );

            return 1;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "NPCs: "
                                        + npcs.size()
                        ),
                false
        );

        for (NpcState npc : npcs) {

            source.sendSuccess(
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

        int total =
                simulation.registry()
                        .size();

        long alive =
                simulation.registry()
                        .aliveCount();

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "NPC population: "
                                                + total
                                                + " total, "
                                                + alive
                                                + " alive."
                                ),
                        false
                );

        return total;
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
}
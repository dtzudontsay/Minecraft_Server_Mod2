package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterBirthStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCivilStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCustodyStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterFreedomStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterLegalState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class LegalStateDebugCommand {

    private LegalStateDebugCommand() {
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
                                                "kwlegal"
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
                                                                                LegalStateDebugCommand::show
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "birth"
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
                                                                                                "status",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                LegalStateDebugCommand::setBirth
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "freedom"
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
                                                                                                "status",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                LegalStateDebugCommand::setFreedom
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "custody"
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
                                                                                                "status",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                LegalStateDebugCommand::setCustody
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "civil"
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
                                                                                                "status",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                LegalStateDebugCommand::setCivil
                                                                                        )
                                                                        )
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
                            context
                    );

            CharacterLegalState state =
                    NpcSimulation.get()
                            .legalStates()
                            .getOrCreate(
                                    npc
                            );

            var profile =
                    NpcSimulation.get()
                            .profiles()
                            .getOrCreate(
                                    npc
                            );

            success(
                    context,
                    "Legal state NPC #"
                            + npc
            );

            success(
                    context,
                    "birth="
                            + state.birthStatus()
                            + " freedom="
                            + state.freedomStatus()
                            + " custody="
                            + state.custodyStatus()
                            + " civil="
                            + state.civilStatus()
            );

            success(
                    context,
                    "legacyProfileStatus="
                            + profile.legalStatus()
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

    private static int setBirth(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            CharacterLegalState state =
                    state(
                            context
                    );

            state.setBirthStatus(
                    enumValue(
                            CharacterBirthStatus.class,
                            StringArgumentType.getString(
                                    context,
                                    "status"
                            )
                    )
            );

            NpcSimulation.get()
                    .save();

            return show(
                    context
            );

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int setFreedom(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            CharacterLegalState state =
                    state(
                            context
                    );

            state.setFreedomStatus(
                    enumValue(
                            CharacterFreedomStatus.class,
                            StringArgumentType.getString(
                                    context,
                                    "status"
                            )
                    )
            );

            NpcSimulation.get()
                    .save();

            return show(
                    context
            );

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int setCustody(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            CharacterLegalState state =
                    state(
                            context
                    );

            state.setCustodyStatus(
                    enumValue(
                            CharacterCustodyStatus.class,
                            StringArgumentType.getString(
                                    context,
                                    "status"
                            )
                    )
            );

            NpcSimulation.get()
                    .save();

            return show(
                    context
            );

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int setCivil(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            CharacterLegalState state =
                    state(
                            context
                    );

            state.setCivilStatus(
                    enumValue(
                            CharacterCivilStatus.class,
                            StringArgumentType.getString(
                                    context,
                                    "status"
                            )
                    )
            );

            NpcSimulation.get()
                    .save();

            return show(
                    context
            );

        } catch (
                RuntimeException exception
        ) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static CharacterLegalState state(
            CommandContext<CommandSourceStack> context
    ) {

        return NpcSimulation.get()
                .legalStates()
                .getOrCreate(
                        npc(
                                context
                        )
                );
    }

    private static NpcId npc(
            CommandContext<CommandSourceStack> context
    ) {

        return new NpcId(
                LongArgumentType.getLong(
                        context,
                        "npc"
                )
        );
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value
    ) {

        try {

            return Enum.valueOf(
                    type,
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new IllegalArgumentException(
                    "Unknown "
                            + type.getSimpleName()
                            + ": "
                            + value,
                    exception
            );
        }
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
            String text
    ) {

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        text
                                ),
                        false
                );
    }
}
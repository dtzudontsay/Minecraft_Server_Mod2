package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterInfluenceChannel;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterInfluenceResult;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class CharacterProfileDebugCommand {

    private CharacterProfileDebugCommand() {
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
                                                "kwprofile"
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
                                                                                CharacterProfileDebugCommand::executeShow
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "psychology"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                CharacterProfileDebugCommand::executePsychology
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "value_pressure"
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
                                                                                                "value",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "target",
                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                        -1.0,
                                                                                                                        1.0
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
                                                                                                                        .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "channel",
                                                                                                                                                StringArgumentType.word()
                                                                                                                                        )
                                                                                                                                        .executes(
                                                                                                                                                CharacterProfileDebugCommand::executeValuePressure
                                                                                                                                        )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "norm_pressure"
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
                                                                                                "norm",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "target",
                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                        -1.0,
                                                                                                                        1.0
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
                                                                                                                        .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "channel",
                                                                                                                                                StringArgumentType.word()
                                                                                                                                        )
                                                                                                                                        .executes(
                                                                                                                                                CharacterProfileDebugCommand::executeNormPressure
                                                                                                                                        )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "disposition_pressure"
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
                                                                                                "disposition",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "target",
                                                                                                                DoubleArgumentType.doubleArg(
                                                                                                                        -1.0,
                                                                                                                        1.0
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
                                                                                                                        .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "channel",
                                                                                                                                                StringArgumentType.word()
                                                                                                                                        )
                                                                                                                                        .executes(
                                                                                                                                                CharacterProfileDebugCommand::executeDispositionPressure
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

    private static int executeShow(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                npcArgument(
                        context
                );

        try {

            var profile =
                    NpcSimulation.get()
                            .profiles()
                            .getOrCreate(
                                    npc
                            );

            success(
                    context,
                    "Profile NPC #"
                            + npc
            );

            success(
                    context,
                    "Identity | culture="
                            + profile.culture()
                            + " religion="
                            + profile.religion()
                            + " education="
                            + profile.education()
            );

            success(
                    context,
                    "Formation | birthplace="
                            + profile.birthplaceLocationId()
                            + " upbringing="
                            + profile.upbringingLocationId()
            );

            success(
                    context,
                    "Personal | orientation="
                            + profile.orientation()
                            + " health="
                            + profile.healthState()
                            + " legalStatus="
                            + profile.legalStatus()
            );

            success(
                    context,
                    "Social | wealth="
                            + format(
                            profile.wealth()
                    )
                            + " status="
                            + format(
                            profile.socialStatus()
                    )
                            + " reputation="
                            + format(
                            profile.reputation()
                    )
            );

            success(
                    context,
                    "Dispositions: "
                            + profile.dispositions()
            );

            success(
                    context,
                    "Values: "
                            + profile.characterValues()
            );

            success(
                    context,
                    "Social norms: "
                            + profile.socialNorms()
            );

            success(
                    context,
                    "Aptitudes: "
                            + profile.aptitudes()
            );

            success(
                    context,
                    "Skills: "
                            + profile.skills()
            );

            success(
                    context,
                    "Traits: "
                            + profile.traits()
            );

            success(
                    context,
                    "Aliases: "
                            + profile.aliases()
            );

            success(
                    context,
                    "Languages: "
                            + profile.languages()
            );

            success(
                    context,
                    "Occupations: "
                            + profile.occupations()
            );

            success(
                    context,
                    "Offices: "
                            + profile.offices()
            );

            success(
                    context,
                    "Court roles: "
                            + profile.courtRoles()
            );

            success(
                    context,
                    "Military roles: "
                            + profile.militaryRoles()
            );

            success(
                    context,
                    "Combat specialties: "
                            + profile.combatSpecialties()
            );

            success(
                    context,
                    "Motivations: "
                            + profile.motivations()
            );

            success(
                    context,
                    "Goals: "
                            + profile.goals()
            );

            success(
                    context,
                    "Fears: "
                            + profile.fears()
            );

            success(
                    context,
                    "Desires: "
                            + profile.desires()
            );

            success(
                    context,
                    "Political preferences: "
                            + profile.politicalPreferences()
            );

            success(
                    context,
                    "Legacy/custom values: "
                            + profile.values()
            );

            success(
                    context,
                    "Secrets owned="
                            + profile.secrets()
                            .size()
                            + " known="
                            + profile.knownSecrets()
                            .size()
            );

            success(
                    context,
                    "Facts | public="
                            + profile.publicFacts()
                            .size()
                            + " private="
                            + profile.privateFacts()
                            .size()
            );

            if (
                    !profile.appearanceDescription()
                            .isBlank()
                            ||
                            !profile.hairDescription()
                                    .isBlank()
                            ||
                            !profile.eyeDescription()
                                    .isBlank()
                            ||
                            !profile.buildDescription()
                                    .isBlank()
            ) {

                success(
                        context,
                        "Appearance | general="
                                + profile.appearanceDescription()
                                + " hair="
                                + profile.hairDescription()
                                + " eyes="
                                + profile.eyeDescription()
                                + " build="
                                + profile.buildDescription()
                );
            }

            var dialogue =
                    profile.dialoguePersona();

            success(
                    context,
                    "Dialogue | formality="
                            + format(
                            dialogue.formality()
                    )
                            + " verbosity="
                            + format(
                            dialogue.verbosity()
                    )
                            + " warmth="
                            + format(
                            dialogue.warmth()
                    )
                            + " directness="
                            + format(
                            dialogue.directness()
                    )
            );

            if (!dialogue.preferredAddress()
                    .isBlank()) {

                success(
                        context,
                        "Preferred address: "
                                + dialogue.preferredAddress()
                );
            }

            if (!dialogue.guidance()
                    .isBlank()) {

                success(
                        context,
                        "Dialogue guidance: "
                                + dialogue.guidance()
                );
            }

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

    private static int executePsychology(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                npcArgument(
                        context
                );

        try {

            CharacterPsychologyService psychology =
                    psychology();

            success(
                    context,
                    "Psychology NPC #"
                            + npc
            );

            success(
                    context,
                    "Social susceptibility="
                            + format(
                            psychology.socialSusceptibility(
                                    npc
                            )
                    )
                            + " persuasion resistance="
                            + format(
                            psychology.persuasionResistance(
                                    npc
                            )
                    )
                            + " manipulation susceptibility="
                            + format(
                            psychology.manipulationSusceptibility(
                                    npc
                            )
                    )
            );

            success(
                    context,
                    "Derived tendencies: "
                            + psychology.derivedTendencies(
                            npc
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

    private static int executeValuePressure(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npcArgument(
                            context
                    );

            CharacterValue value =
                    enumArgument(
                            CharacterValue.class,
                            StringArgumentType.getString(
                                    context,
                                    "value"
                            )
                    );

            double target =
                    DoubleArgumentType.getDouble(
                            context,
                            "target"
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            CharacterInfluenceChannel channel =
                    enumArgument(
                            CharacterInfluenceChannel.class,
                            StringArgumentType.getString(
                                    context,
                                    "channel"
                            )
                    );

            CharacterInfluenceResult result =
                    psychology()
                            .applyValuePressure(
                                    npc,
                                    value,
                                    target,
                                    pressure,
                                    channel
                            );

            printInfluenceResult(
                    context,
                    "Value "
                            + value,
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

    private static int executeNormPressure(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npcArgument(
                            context
                    );

            CharacterSocialNorm norm =
                    enumArgument(
                            CharacterSocialNorm.class,
                            StringArgumentType.getString(
                                    context,
                                    "norm"
                            )
                    );

            double target =
                    DoubleArgumentType.getDouble(
                            context,
                            "target"
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            CharacterInfluenceChannel channel =
                    enumArgument(
                            CharacterInfluenceChannel.class,
                            StringArgumentType.getString(
                                    context,
                                    "channel"
                            )
                    );

            CharacterInfluenceResult result =
                    psychology()
                            .applySocialNormPressure(
                                    npc,
                                    norm,
                                    target,
                                    pressure,
                                    channel
                            );

            printInfluenceResult(
                    context,
                    "Social norm "
                            + norm,
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

    private static int executeDispositionPressure(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npcArgument(
                            context
                    );

            CharacterDisposition disposition =
                    enumArgument(
                            CharacterDisposition.class,
                            StringArgumentType.getString(
                                    context,
                                    "disposition"
                            )
                    );

            double target =
                    DoubleArgumentType.getDouble(
                            context,
                            "target"
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            CharacterInfluenceChannel channel =
                    enumArgument(
                            CharacterInfluenceChannel.class,
                            StringArgumentType.getString(
                                    context,
                                    "channel"
                            )
                    );

            CharacterInfluenceResult result =
                    psychology()
                            .applyDispositionPressure(
                                    npc,
                                    disposition,
                                    target,
                                    pressure,
                                    channel
                            );

            printInfluenceResult(
                    context,
                    "Disposition "
                            + disposition,
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

    private static CharacterPsychologyService psychology() {

        NpcSimulation simulation =
                NpcSimulation.get();

        return new CharacterPsychologyService(
                simulation.registry(),
                simulation.profiles(),
                simulation.relationships()
        );
    }

    private static void printInfluenceResult(
            CommandContext<CommandSourceStack> context,
            String label,
            CharacterInfluenceResult result
    ) {

        success(
                context,
                label
                        + " | "
                        + format(
                        result.before()
                )
                        + " -> "
                        + format(
                        result.after()
                )
                        + " toward "
                        + format(
                        result.target()
                )
        );

        success(
                context,
                "delta="
                        + format(
                        result.delta()
                )
                        + " effectiveStrength="
                        + format(
                        result.effectiveStrength()
                )
                        + " susceptibility="
                        + format(
                        result.susceptibility()
                )
                        + " resistance="
                        + format(
                        result.resistance()
                )
                        + " convictionResistance="
                        + format(
                        result.convictionResistance()
                )
                        + " sourceFactor="
                        + format(
                        result.sourceFactor()
                )
        );
    }

    private static NpcId npcArgument(
            CommandContext<CommandSourceStack> context
    ) {

        return new NpcId(
                LongArgumentType.getLong(
                        context,
                        "npc"
                )
        );
    }

    private static <E extends Enum<E>> E enumArgument(
            Class<E> type,
            String raw
    ) {

        try {

            return Enum.valueOf(
                    type,
                    raw.trim()
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
                            + raw,
                    exception
            );
        }
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
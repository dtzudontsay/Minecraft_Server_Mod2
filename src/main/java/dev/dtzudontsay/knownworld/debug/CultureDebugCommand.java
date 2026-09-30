package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.culture.CulturalIdentitySnapshot;
import dev.dtzudontsay.knownworld.simulation.npc.culture.CultureDistanceService;
import dev.dtzudontsay.knownworld.simulation.npc.culture.CultureInfluenceResult;
import dev.dtzudontsay.knownworld.simulation.npc.culture.CultureService;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class CultureDebugCommand {

    private CultureDebugCommand() {
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
                                                "kwculture"
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
                                                                                CultureDebugCommand::show
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
                                                                                                "culture",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                CultureDebugCommand::init
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "here"
                                                        )
                                                        .executes(
                                                                CultureDebugCommand::here
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
                                                                                                CultureDebugCommand::regional
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "pressure"
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
                                                                                                "culture",
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
                                                                                                                CultureDebugCommand::pressure
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
                                                                                                                "culture",
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
                                                                                                                                CultureDebugCommand::persuade
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "parents"
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
                                                                                                CultureDebugCommand::parents
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "distance"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "first",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "second",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                CultureDebugCommand::distance
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
                            context,
                            "npc"
                    );

            CulturalIdentitySnapshot identity =
                    service()
                            .snapshot(
                                    npc
                            );

            success(
                    context,
                    "Culture NPC #"
                            + npc
                            + " | primary="
                            + identity.primaryCultureId()
            );

            success(
                    context,
                    "identity="
                            + format(
                            identity.identityImportance()
                    )
                            + " heritage="
                            + format(
                            identity.heritageAttachment()
                    )
                            + " assimilationOpenness="
                            + format(
                            identity.assimilationOpenness()
                    )
                            + " multicultural="
                            + format(
                            identity.multiculturalIdentity()
                    )
            );

            success(
                    context,
                    "Effective assimilation openness="
                            + format(
                            service()
                                    .effectiveAssimilationOpenness(
                                            npc
                                    )
                    )
                            + " heritage resistance="
                            + format(
                            service()
                                    .heritageResistance(
                                            npc
                                    )
                    )
            );

            success(
                    context,
                    "Cultural affinities: "
                            + identity.culturalAffinities()
            );

            success(
                    context,
                    "Languages: "
                            + identity.languages()
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

            String culture =
                    StringArgumentType.getString(
                            context,
                            "culture"
                    );

            service()
                    .initializeCultureState(
                            npc,
                            culture,
                            0.55,
                            0.55,
                            0.35,
                            0.10
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Initialized culture state for NPC #"
                            + npc
                            + " as "
                            + culture
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

            CultureService service =
                    service();

            String culture =
                    service.strongestRegionalCulture(
                                    player.getX(),
                                    player.getZ()
                            )
                            .orElse(
                                    "none"
                            );

            success(
                    context,
                    "Strongest regional culture: "
                            + culture
            );

            if (!"none".equals(
                    culture
            )) {

                success(
                        context,
                        "Effective regional support="
                                + format(
                                service.regionalCultureSupport(
                                        culture,
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

            int changed =
                    service()
                            .applyRegionalExposure(
                                    npcId,
                                    npc.position()
                                            .x(),
                                    npc.position()
                                            .z(),
                                    pressure
                            );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Applied regional culture exposure to NPC #"
                            + npcId
                            + " | changed dimensions="
                            + changed
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

    private static int pressure(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context,
                            "npc"
                    );

            String culture =
                    StringArgumentType.getString(
                            context,
                            "culture"
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            CultureInfluenceResult result =
                    service()
                            .applyCulturePressure(
                                    npc,
                                    culture,
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

            String culture =
                    StringArgumentType.getString(
                            context,
                            "culture"
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            CultureInfluenceResult result =
                    service()
                            .applyCulturePressure(
                                    target,
                                    source,
                                    culture,
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

    private static int parents(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context,
                            "npc"
                    );

            double pressure =
                    DoubleArgumentType.getDouble(
                            context,
                            "pressure"
                    );

            int changed =
                    service()
                            .applyParentInfluence(
                                    npc,
                                    pressure
                            );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Applied parent cultural influence to NPC #"
                            + npc
                            + " | influence events="
                            + changed
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

    private static int distance(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            String first =
                    StringArgumentType.getString(
                            context,
                            "first"
                    );

            String second =
                    StringArgumentType.getString(
                            context,
                            "second"
                    );

            CultureDistanceService distance =
                    new CultureDistanceService();

            success(
                    context,
                    "Cultural distance "
                            + first
                            + " -> "
                            + second
                            + " = "
                            + format(
                            distance.distance(
                                    first,
                                    second
                            )
                    )
                            + " | sameFamily="
                            + distance.sameCultureFamily(
                            first,
                            second
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

    private static void printResult(
            CommandContext<CommandSourceStack> context,
            CultureInfluenceResult result
    ) {

        success(
                context,
                "Culture pressure: "
                        + result.previousCultureId()
                        + " -> "
                        + result.targetCultureId()
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
                        result.assimilationOpenness()
                )
                        + " heritageResistance="
                        + format(
                        result.heritageResistance()
                )
                        + " culturalDistance="
                        + format(
                        result.culturalDistance()
                )
        );

        success(
                context,
                "affinity="
                        + format(
                        result.previousAffinity()
                )
                        + " -> "
                        + format(
                        result.newAffinity()
                )
                        + " | momentum="
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
                result.cultureChanged()
                        ? "PRIMARY CULTURE CHANGED to "
                        + result.targetCultureId()
                        : "No primary culture change."
        );
    }

    private static CultureService service() {

        NpcSimulation simulation =
                NpcSimulation.get();

        CharacterPsychologyService psychology =
                new CharacterPsychologyService(
                        simulation.registry(),
                        simulation.profiles(),
                        simulation.relationships()
                );

        return new CultureService(
                simulation.registry(),
                simulation.profiles(),
                simulation.genealogy(),
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
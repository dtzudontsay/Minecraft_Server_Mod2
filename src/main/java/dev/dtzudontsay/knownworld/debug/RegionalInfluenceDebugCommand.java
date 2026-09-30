package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.RegionalCharacterInfluenceService;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalExposureWeight;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceCatalog;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceProfile;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolution;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolver;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Map;

public final class RegionalInfluenceDebugCommand {

    private RegionalInfluenceDebugCommand() {
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
                                                "kwinfluence"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "status"
                                                        )
                                                        .executes(
                                                                RegionalInfluenceDebugCommand::status
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "here"
                                                        )
                                                        .executes(
                                                                RegionalInfluenceDebugCommand::here
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "npc"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                RegionalInfluenceDebugCommand::npc
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "apply"
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
                                                                                                "strength",
                                                                                                DoubleArgumentType.doubleArg(
                                                                                                        0.0,
                                                                                                        1.0
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                RegionalInfluenceDebugCommand::apply
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "upbringing"
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
                                                                                                "strength",
                                                                                                DoubleArgumentType.doubleArg(
                                                                                                        0.0,
                                                                                                        1.0
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                RegionalInfluenceDebugCommand::upbringing
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) {

        success(
                context,
                "Regional influence profiles: "
                        + RegionalInfluenceCatalog.get()
                        .size()
        );

        return 1;
    }

    private static int here(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            ServerPlayer player =
                    context.getSource()
                            .getPlayerOrException();

            RegionalInfluenceResolution resolution =
                    RegionalInfluenceResolver.resolveMinecraft(
                                    player.getX(),
                                    player.getZ()
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "No regional influence resolves here."
                                            )
                            );

            printResolution(
                    context,
                    resolution
            );

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

    private static int npc(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcState npc =
                    requireNpc(
                            npcId(
                                    context
                            )
                    );

            RegionalInfluenceResolution resolution =
                    RegionalInfluenceResolver.resolveMinecraft(
                                    npc.position()
                                            .x(),
                                    npc.position()
                                            .z()
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "No regional influence resolves at NPC position."
                                            )
                            );

            printResolution(
                    context,
                    resolution
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

    private static int apply(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npcId =
                    npcId(
                            context
                    );

            NpcState npc =
                    requireNpc(
                            npcId
                    );

            double strength =
                    DoubleArgumentType.getDouble(
                            context,
                            "strength"
                    );

            int changed =
                    service()
                            .applyEnvironmentalInfluence(
                                    npcId,
                                    npc.position()
                                            .x(),
                                    npc.position()
                                            .z(),
                                    strength
                            );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Applied regional influence to NPC #"
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

    private static int upbringing(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npcId =
                    npcId(
                            context
                    );

            NpcState npc =
                    requireNpc(
                            npcId
                    );

            double strength =
                    DoubleArgumentType.getDouble(
                            context,
                            "strength"
                    );

            int changed =
                    service()
                            .applyUpbringingInfluence(
                                    npcId,
                                    npc.position()
                                            .x(),
                                    npc.position()
                                            .z(),
                                    strength
                            );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Applied upbringing influence to NPC #"
                            + npcId
                            + " | location="
                            + NpcSimulation.get()
                            .profiles()
                            .getOrCreate(
                                    npcId
                            )
                            .upbringingLocationId()
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

    private static void printResolution(
            CommandContext<CommandSourceStack> context,
            RegionalInfluenceResolution resolution
    ) {

        success(
                context,
                "Territory: "
                        + resolution.territoryId()
        );

        success(
                context,
                "Subregion: "
                        + (
                        resolution.subregionId()
                                == null
                                ? "none"
                                : resolution.subregionId()
                )
        );

        success(
                context,
                "Subregion refinement profile: "
                        + (
                        resolution.hasRefinement()
                                ? "yes"
                                : "no - territory fallback"
                )
        );

        RegionalInfluenceProfile profile =
                resolution.effectiveProfile();

        success(
                context,
                "Known values: "
                        + profile.characterValues()
                        .entrySet()
                        .stream()
                        .filter(
                                entry ->
                                        entry.getValue()
                                                .known()
                        )
                        .map(
                                entry ->
                                        entry.getKey()
                                                + "="
                                                + format(
                                                entry.getValue()
                                                        .value()
                                        )
                                                + "@"
                                                + format(
                                                entry.getValue()
                                                        .confidence()
                                        )
                        )
                        .toList()
        );

        success(
                context,
                "Known social norms: "
                        + profile.socialNorms()
                        .entrySet()
                        .stream()
                        .filter(
                                entry ->
                                        entry.getValue()
                                                .known()
                        )
                        .map(
                                entry ->
                                        entry.getKey()
                                                + "="
                                                + format(
                                                entry.getValue()
                                                        .value()
                                        )
                                                + "@"
                                                + format(
                                                entry.getValue()
                                                        .confidence()
                                        )
                        )
                        .toList()
        );

        printExposure(
                context,
                "Culture exposure",
                profile.cultureExposure()
        );

        printExposure(
                context,
                "Religion exposure",
                profile.religionExposure()
        );

        printExposure(
                context,
                "Language exposure",
                profile.languageExposure()
        );
    }

    private static void printExposure(
            CommandContext<CommandSourceStack> context,
            String label,
            Map<String, RegionalExposureWeight> exposure
    ) {

        success(
                context,
                label
                        + ": "
                        + exposure.entrySet()
                        .stream()
                        .filter(
                                entry ->
                                        entry.getValue()
                                                .known()
                        )
                        .map(
                                entry ->
                                        entry.getKey()
                                                + "="
                                                + format(
                                                entry.getValue()
                                                        .weight()
                                        )
                        )
                        .toList()
        );
    }

    private static RegionalCharacterInfluenceService service() {

        NpcSimulation simulation =
                NpcSimulation.get();

        CharacterPsychologyService psychology =
                new CharacterPsychologyService(
                        simulation.registry(),
                        simulation.profiles(),
                        simulation.relationships()
                );

        return new RegionalCharacterInfluenceService(
                psychology,
                simulation.profiles()
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

    private static NpcId npcId(
            CommandContext<CommandSourceStack> context
    ) {

        return new NpcId(
                LongArgumentType.getLong(
                        context,
                        "npc"
                )
        );
    }

    private static String format(
            double value
    ) {

        return String.format(
                Locale.ROOT,
                "%.2f",
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
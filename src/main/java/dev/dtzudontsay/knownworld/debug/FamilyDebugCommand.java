package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.family.DynastyInheritanceRule;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageRecord;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class FamilyDebugCommand {

    private FamilyDebugCommand() {
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
                                                "kwfamily"
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
                                                                        .executes(
                                                                                FamilyDebugCommand::executeParents
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "children"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                FamilyDebugCommand::executeChildren
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "spouse"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                FamilyDebugCommand::executeSpouse
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "unions"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                FamilyDebugCommand::executeUnions
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "betroth"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "first",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "second",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "rule",
                                                                                                                StringArgumentType.word()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                FamilyDebugCommand::executeBetroth
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "marry"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "first",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "second",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "rule",
                                                                                                                StringArgumentType.word()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                FamilyDebugCommand::executeMarry
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "end_union"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                FamilyDebugCommand::executeEndUnion
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "child"
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
                                                                                                        0
                                                                                                )
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
                                                                                                                        .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "givenName",
                                                                                                                                                StringArgumentType.word()
                                                                                                                                        )
                                                                                                                                        .executes(
                                                                                                                                                FamilyDebugCommand::executeChild
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

    private static int executeParents(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId npc =
                npcArgument(
                        context,
                        "npc"
                );

        try {

            Parentage parentage =
                    NpcSimulation.get()
                            .genealogy()
                            .parentsOf(
                                    npc
                            )
                            .orElse(
                                    null
                            );

            if (parentage == null) {

                success(
                        context,
                        "NPC #"
                                + npc
                                + " has no recorded parentage."
                );

                return 1;
            }

            success(
                    context,
                    "NPC #"
                            + npc
                            + " | mother "
                            + formatNpc(
                            parentage.mother()
                    )
                            + " | father "
                            + formatNpc(
                            parentage.father()
                    )
                            + " | birth tick "
                            + parentage.birthTick()
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeChildren(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId npc =
                npcArgument(
                        context,
                        "npc"
                );

        try {

            var children =
                    NpcSimulation.get()
                            .genealogy()
                            .childrenOf(
                                    npc
                            );

            if (children.isEmpty()) {

                success(
                        context,
                        "NPC #"
                                + npc
                                + " has no recorded children."
                );

                return 1;
            }

            for (NpcId child : children) {

                NpcState state =
                        NpcSimulation.get()
                                .registry()
                                .find(
                                        child
                                )
                                .orElse(
                                        null
                                );

                success(
                        context,
                        "#"
                                + child
                                + " "
                                + (
                                state == null
                                        ? "unknown"
                                        : state.identity()
                                        .fullName()
                        )
                );
            }

            return children.size();

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeSpouse(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId npc =
                npcArgument(
                        context,
                        "npc"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            NpcId spouse =
                    simulation.marriages()
                            .currentSpouseOf(
                                    npc
                            )
                            .orElse(
                                    null
                            );

            if (spouse == null) {

                success(
                        context,
                        "NPC #"
                                + npc
                                + " is not currently married."
                );

                return 1;
            }

            NpcState spouseState =
                    simulation.registry()
                            .find(
                                    spouse
                            )
                            .orElseThrow();

            success(
                    context,
                    "NPC #"
                            + npc
                            + " spouse: #"
                            + spouse
                            + " "
                            + spouseState.identity()
                            .fullName()
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeUnions(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId npc =
                npcArgument(
                        context,
                        "npc"
                );

        try {

            var unions =
                    NpcSimulation.get()
                            .marriages()
                            .unionsOf(
                                    npc
                            );

            if (unions.isEmpty()) {

                success(
                        context,
                        "NPC #"
                                + npc
                                + " has no union history."
                );

                return 1;
            }

            for (
                    MarriageRecord union :
                    unions
            ) {

                success(
                        context,
                        "Union #"
                                + union.id()
                                + " | "
                                + union.status()
                                + " | partner #"
                                + union.other(
                                npc
                        )
                                + " | "
                                + union.inheritanceRule()
                                + " | created "
                                + union.createdTick()
                                + " | married "
                                + (
                                union.marriedTick()
                                        == null
                                        ? "-"
                                        : union.marriedTick()
                        )
                                + " | ended "
                                + (
                                union.endedTick()
                                        == null
                                        ? "-"
                                        : union.endedTick()
                        )
                );
            }

            return unions.size();

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeBetroth(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId first =
                npcArgument(
                        context,
                        "first"
                );

        NpcId second =
                npcArgument(
                        context,
                        "second"
                );

        try {

            DynastyInheritanceRule rule =
                    ruleArgument(
                            context
                    );

            NpcSimulation simulation =
                    NpcSimulation.get();

            MarriageRecord marriage =
                    simulation.marriageService()
                            .betroth(
                                    first,
                                    second,
                                    rule,
                                    simulation.serverTickCounter()
                            );

            simulation.save();

            success(
                    context,
                    "Created betrothal #"
                            + marriage.id()
                            + " between NPC #"
                            + first
                            + " and NPC #"
                            + second
                            + " ["
                            + marriage.inheritanceRule()
                            + "]"
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeMarry(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId first =
                npcArgument(
                        context,
                        "first"
                );

        NpcId second =
                npcArgument(
                        context,
                        "second"
                );

        try {

            DynastyInheritanceRule rule =
                    ruleArgument(
                            context
                    );

            NpcSimulation simulation =
                    NpcSimulation.get();

            MarriageRecord marriage =
                    simulation.marriageService()
                            .marry(
                                    first,
                                    second,
                                    rule,
                                    simulation.serverTickCounter()
                            );

            simulation.save();

            success(
                    context,
                    "Marriage #"
                            + marriage.id()
                            + " | NPC #"
                            + first
                            + " + NPC #"
                            + second
                            + " ["
                            + marriage.inheritanceRule()
                            + "]"
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeEndUnion(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId npc =
                npcArgument(
                        context,
                        "npc"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            boolean ended =
                    simulation.marriageService()
                            .endCurrentUnion(
                                    npc,
                                    simulation.serverTickCounter()
                            );

            if (!ended) {

                success(
                        context,
                        "NPC #"
                                + npc
                                + " has no active union."
                );

                return 1;
            }

            simulation.save();

            success(
                    context,
                    "Ended current union of NPC #"
                            + npc
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static int executeChild(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId mother =
                npcArgument(
                        context,
                        "mother"
                );

        long rawFather =
                LongArgumentType.getLong(
                        context,
                        "father"
                );

        NpcId father =
                rawFather == 0
                        ? null
                        : new NpcId(
                        rawFather
                );

        String rawSex =
                StringArgumentType.getString(
                        context,
                        "sex"
                );

        int birthYear =
                IntegerArgumentType.getInteger(
                        context,
                        "birthYear"
                );

        String givenName =
                StringArgumentType.getString(
                        context,
                        "givenName"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            NpcState child;

            if ("auto".equalsIgnoreCase(
                    rawSex
            )) {

                child =
                        simulation.characterGeneration()
                                .createChildAutoSex(
                                        givenName,
                                        birthYear,
                                        mother,
                                        father,
                                        simulation.serverTickCounter()
                                );

            } else {

                NpcSex sex =
                        NpcSex.valueOf(
                                rawSex.toUpperCase(
                                        Locale.ROOT
                                )
                        );

                child =
                        simulation.characterGeneration()
                                .createChild(
                                        givenName,
                                        sex,
                                        birthYear,
                                        mother,
                                        father,
                                        simulation.serverTickCounter()
                                );
            }

            simulation.save();

            success(
                    context,
                    "Generated NPC #"
                            + child.id()
                            + " "
                            + child.identity()
                            .fullName()
                            + " ["
                            + child.identity()
                            .sex()
                            + "]"
            );

            return 1;

        } catch (RuntimeException exception) {

            return failure(
                    context,
                    exception
            );
        }
    }

    private static DynastyInheritanceRule ruleArgument(
            CommandContext<CommandSourceStack> context
    ) {
        String raw =
                StringArgumentType.getString(
                        context,
                        "rule"
                );

        return DynastyInheritanceRule.valueOf(
                raw.toUpperCase(
                        Locale.ROOT
                )
        );
    }

    private static NpcId npcArgument(
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

    private static String formatNpc(
            NpcId id
    ) {
        return id == null
                ? "unknown"
                : "#"
                + id;
    }
}
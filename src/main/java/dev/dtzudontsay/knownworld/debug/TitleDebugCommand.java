package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleAssignment;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleDefinition;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleType;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class TitleDebugCommand {

    private TitleDebugCommand() {
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
                                                "kwtitle"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "list"
                                                        )
                                                        .executes(
                                                                TitleDebugCommand::executeList
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "create"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "type",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "authority",
                                                                                                IntegerArgumentType.integer(
                                                                                                        0,
                                                                                                        100
                                                                                                )
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "exclusive",
                                                                                                                IntegerArgumentType.integer(
                                                                                                                        0,
                                                                                                                        1
                                                                                                                )
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.argument(
                                                                                                                                "organization",
                                                                                                                                LongArgumentType.longArg(
                                                                                                                                        0
                                                                                                                                )
                                                                                                                        )
                                                                                                                        .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "settlement",
                                                                                                                                                LongArgumentType.longArg(
                                                                                                                                                        0
                                                                                                                                                )
                                                                                                                                        )
                                                                                                                                        .then(
                                                                                                                                                Commands.argument(
                                                                                                                                                                "name",
                                                                                                                                                                StringArgumentType.greedyString()
                                                                                                                                                        )
                                                                                                                                                        .executes(
                                                                                                                                                                TitleDebugCommand::executeCreate
                                                                                                                                                        )
                                                                                                                                        )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "assign"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "title",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "npc",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                TitleDebugCommand::executeAssign
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "revoke"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "title",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "npc",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                TitleDebugCommand::executeRevoke
                                                                                        )
                                                                        )
                                                        )
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
                                                                                TitleDebugCommand::executeShow
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "holders"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "title",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                TitleDebugCommand::executeHolders
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "authority"
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
                                                                                                "organization",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                TitleDebugCommand::executeAuthority
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
        String rawType =
                StringArgumentType.getString(
                        context,
                        "type"
                );

        TitleType type;

        try {

            type =
                    TitleType.valueOf(
                            rawType.toUpperCase(
                                    Locale.ROOT
                            )
                    );

        } catch (IllegalArgumentException exception) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "Unknown title type: "
                                            + rawType
                            )
                    );

            return 0;
        }

        int authority =
                IntegerArgumentType.getInteger(
                        context,
                        "authority"
                );

        boolean exclusive =
                IntegerArgumentType.getInteger(
                        context,
                        "exclusive"
                ) == 1;

        long rawOrganization =
                LongArgumentType.getLong(
                        context,
                        "organization"
                );

        long rawSettlement =
                LongArgumentType.getLong(
                        context,
                        "settlement"
                );

        OrganizationId organization =
                rawOrganization == 0
                        ? null
                        : new OrganizationId(
                        rawOrganization
                );

        SettlementId settlement =
                rawSettlement == 0
                        ? null
                        : new SettlementId(
                        rawSettlement
                );

        String name =
                StringArgumentType.getString(
                        context,
                        "name"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            TitleDefinition title =
                    simulation.titles()
                            .create(
                                    name,
                                    type,
                                    authority,
                                    exclusive,
                                    organization,
                                    settlement
                            );

            simulation.save();

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Created title #"
                                                    + title.id()
                                                    + " "
                                                    + title.name()
                                                    + " ["
                                                    + title.type()
                                                    + "] authority "
                                                    + title.authority()
                                                    + " exclusive "
                                                    + title.exclusive()
                                    ),
                            false
                    );

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

    private static int executeAssign(
            CommandContext<CommandSourceStack> context
    ) {
        TitleId title =
                titleId(
                        context
                );

        NpcId npc =
                npcId(
                        context
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            simulation.titles()
                    .grant(
                            title,
                            npc,
                            simulation.serverTickCounter()
                    );

            simulation.save();

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Assigned title #"
                                                    + title
                                                    + " to NPC #"
                                                    + npc
                                    ),
                            false
                    );

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

    private static int executeRevoke(
            CommandContext<CommandSourceStack> context
    ) {
        TitleId title =
                titleId(
                        context
                );

        NpcId npc =
                npcId(
                        context
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            boolean changed =
                    simulation.titles()
                            .revoke(
                                    title,
                                    npc,
                                    simulation.serverTickCounter()
                            );

            if (!changed) {

                context.getSource()
                        .sendFailure(
                                Component.literal(
                                        "NPC #"
                                                + npc
                                                + " does not currently hold title #"
                                                + title
                                )
                        );

                return 0;
            }

            simulation.save();

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Revoked title #"
                                                    + title
                                                    + " from NPC #"
                                                    + npc
                                    ),
                            false
                    );

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
        var titles =
                NpcSimulation.get()
                        .titles()
                        .allDefinitions();

        if (titles.isEmpty()) {

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "No titles registered."
                                    ),
                            false
                    );

            return 1;
        }

        for (TitleDefinition title : titles) {

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "#"
                                                    + title.id()
                                                    + " "
                                                    + title.name()
                                                    + " ["
                                                    + title.type()
                                                    + "] | authority "
                                                    + title.authority()
                                                    + " | exclusive "
                                                    + title.exclusive()
                                                    + " | organization "
                                                    + (
                                                    title.organization()
                                                            == null
                                                            ? "none"
                                                            : "#"
                                                            + title.organization()
                                            )
                                                    + " | settlement "
                                                    + (
                                                    title.settlement()
                                                            == null
                                                            ? "none"
                                                            : "#"
                                                            + title.settlement()
                                            )
                                    ),
                            false
                    );
        }

        return titles.size();
    }

    private static int executeShow(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId npc =
                npcId(
                        context
                );

        try {

            var titles =
                    NpcSimulation.get()
                            .titles()
                            .activeTitlesOf(
                                    npc
                            );

            if (titles.isEmpty()) {

                context.getSource()
                        .sendSuccess(
                                () ->
                                        Component.literal(
                                                "NPC #"
                                                        + npc
                                                        + " currently holds no titles."
                                        ),
                                false
                        );

                return 1;
            }

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Active titles of NPC #"
                                                    + npc
                                                    + ":"
                                    ),
                            false
                    );

            for (TitleDefinition title : titles) {

                context.getSource()
                        .sendSuccess(
                                () ->
                                        Component.literal(
                                                "#"
                                                        + title.id()
                                                        + " "
                                                        + title.name()
                                                        + " | authority "
                                                        + title.authority()
                                        ),
                                false
                        );
            }

            return titles.size();

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

    private static int executeHolders(
            CommandContext<CommandSourceStack> context
    ) {
        TitleId titleId =
                titleId(
                        context
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            TitleDefinition title =
                    simulation.titles()
                            .find(
                                    titleId
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Unknown title ID: "
                                                            + titleId
                                            )
                            );

            var holders =
                    simulation.titles()
                            .activeAssignmentsForTitle(
                                    titleId
                            );

            if (holders.isEmpty()) {

                context.getSource()
                        .sendSuccess(
                                () ->
                                        Component.literal(
                                                title.name()
                                                        + " currently has no holder."
                                        ),
                                false
                        );

                return 1;
            }

            for (TitleAssignment assignment : holders) {

                context.getSource()
                        .sendSuccess(
                                () ->
                                        Component.literal(
                                                title.name()
                                                        + " -> NPC #"
                                                        + assignment.holder()
                                                        + " | granted tick "
                                                        + assignment.grantedTick()
                                        ),
                                false
                        );
            }

            return holders.size();

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

    private static int executeAuthority(
            CommandContext<CommandSourceStack> context
    ) {
        NpcId npc =
                npcId(
                        context
                );

        OrganizationId organization =
                new OrganizationId(
                        LongArgumentType.getLong(
                                context,
                                "organization"
                        )
                );

        try {

            int authority =
                    NpcSimulation.get()
                            .authority()
                            .authorityIn(
                                    npc,
                                    organization
                            );

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "NPC #"
                                                    + npc
                                                    + " formal authority in organization #"
                                                    + organization
                                                    + ": "
                                                    + authority
                                    ),
                            false
                    );

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

    private static TitleId titleId(
            CommandContext<CommandSourceStack> context
    ) {
        return new TitleId(
                LongArgumentType.getLong(
                        context,
                        "title"
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
}
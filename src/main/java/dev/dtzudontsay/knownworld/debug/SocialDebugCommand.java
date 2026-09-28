package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.world.settlement.Settlement;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class SocialDebugCommand {

    private SocialDebugCommand() {
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
                                                "kwsocial"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "settlement_create"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "type",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "name",
                                                                                                StringArgumentType.greedyString()
                                                                                        )
                                                                                        .executes(
                                                                                                SocialDebugCommand::executeSettlementCreate
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "settlements"
                                                        )
                                                        .executes(
                                                                SocialDebugCommand::executeSettlements
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "organization_create"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "type",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "seat",
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
                                                                                                                SocialDebugCommand::executeOrganizationCreate
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "organizations"
                                                        )
                                                        .executes(
                                                                SocialDebugCommand::executeOrganizations
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "residence"
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
                                                                                                "settlement",
                                                                                                LongArgumentType.longArg(
                                                                                                        1
                                                                                                )
                                                                                        )
                                                                                        .executes(
                                                                                                SocialDebugCommand::executeResidence
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "household"
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
                                                                                                SocialDebugCommand::executeHousehold
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "house"
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
                                                                                                SocialDebugCommand::executeHouse
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "faction"
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
                                                                                                SocialDebugCommand::executeFaction
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
                                                                                SocialDebugCommand::executeShow
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeSettlementCreate(
            CommandContext<CommandSourceStack> context
    ) {
        ServerPlayer player =
                player(
                        context.getSource()
                );

        if (player == null) {
            return 0;
        }

        String typeText =
                StringArgumentType.getString(
                        context,
                        "type"
                );

        SettlementType type;

        try {

            type =
                    SettlementType.valueOf(
                            typeText.toUpperCase(
                                    Locale.ROOT
                            )
                    );

        } catch (IllegalArgumentException exception) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "Unknown settlement type: "
                                            + typeText
                            )
                    );

            return 0;
        }

        String name =
                StringArgumentType.getString(
                        context,
                        "name"
                );

        NpcSimulation simulation =
                NpcSimulation.get();

        Settlement settlement =
                simulation.settlements()
                        .create(
                                name,
                                type,
                                position(
                                        player
                                )
                        );

        simulation.save();

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        "Created settlement #"
                                                + settlement.id()
                                                + " "
                                                + settlement.name()
                                                + " ["
                                                + settlement.type()
                                                + "]"
                                ),
                        false
                );

        return 1;
    }

    private static int executeSettlements(
            CommandContext<CommandSourceStack> context
    ) {
        var settlements =
                NpcSimulation.get()
                        .settlements()
                        .all();

        for (Settlement settlement : settlements) {

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "#"
                                                    + settlement.id()
                                                    + " "
                                                    + settlement.name()
                                                    + " ["
                                                    + settlement.type()
                                                    + "]"
                                    ),
                            false
                    );
        }

        return settlements.size();
    }

    private static int executeOrganizationCreate(
            CommandContext<CommandSourceStack> context
    ) {
        String typeText =
                StringArgumentType.getString(
                        context,
                        "type"
                );

        OrganizationType type;

        try {

            type =
                    OrganizationType.valueOf(
                            typeText.toUpperCase(
                                    Locale.ROOT
                            )
                    );

        } catch (IllegalArgumentException exception) {

            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "Unknown organization type: "
                                            + typeText
                            )
                    );

            return 0;
        }

        long rawSeat =
                LongArgumentType.getLong(
                        context,
                        "seat"
                );

        SettlementId seat =
                rawSeat == 0
                        ? null
                        : new SettlementId(
                        rawSeat
                );

        String name =
                StringArgumentType.getString(
                        context,
                        "name"
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            Organization organization =
                    simulation.organizations()
                            .create(
                                    name,
                                    type,
                                    seat
                            );

            simulation.save();

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "Created organization #"
                                                    + organization.id()
                                                    + " "
                                                    + organization.name()
                                                    + " ["
                                                    + organization.type()
                                                    + "]"
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

    private static int executeOrganizations(
            CommandContext<CommandSourceStack> context
    ) {
        var organizations =
                NpcSimulation.get()
                        .organizations()
                        .all();

        for (Organization organization : organizations) {

            context.getSource()
                    .sendSuccess(
                            () ->
                                    Component.literal(
                                            "#"
                                                    + organization.id()
                                                    + " "
                                                    + organization.name()
                                                    + " ["
                                                    + organization.type()
                                                    + "] | seat "
                                                    + (
                                                    organization.seatSettlement()
                                                            == null
                                                            ? "none"
                                                            : organization.seatSettlement()
                                            )
                                    ),
                            false
                    );
        }

        return organizations.size();
    }

    private static int executeResidence(
            CommandContext<CommandSourceStack> context
    ) {
        return setAffiliation(
                context,
                AffiliationKind.RESIDENCE
        );
    }

    private static int executeHousehold(
            CommandContext<CommandSourceStack> context
    ) {
        return setAffiliation(
                context,
                AffiliationKind.HOUSEHOLD
        );
    }

    private static int executeHouse(
            CommandContext<CommandSourceStack> context
    ) {
        return setAffiliation(
                context,
                AffiliationKind.HOUSE
        );
    }

    private static int executeFaction(
            CommandContext<CommandSourceStack> context
    ) {
        return setAffiliation(
                context,
                AffiliationKind.FACTION
        );
    }

    private static int setAffiliation(
            CommandContext<CommandSourceStack> context,
            AffiliationKind kind
    ) {
        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                );

        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            switch (kind) {

                case RESIDENCE -> {

                    SettlementId settlement =
                            new SettlementId(
                                    LongArgumentType.getLong(
                                            context,
                                            "settlement"
                                    )
                            );

                    simulation.affiliations()
                            .setResidence(
                                    npc,
                                    settlement
                            );
                }

                case HOUSEHOLD ->

                        simulation.affiliations()
                                .setHousehold(
                                        npc,
                                        new OrganizationId(
                                                LongArgumentType.getLong(
                                                        context,
                                                        "organization"
                                                )
                                        )
                                );

                case HOUSE ->

                        simulation.affiliations()
                                .setNobleHouse(
                                        npc,
                                        new OrganizationId(
                                                LongArgumentType.getLong(
                                                        context,
                                                        "organization"
                                                )
                                        )
                                );

                case FACTION ->

                        simulation.affiliations()
                                .setFaction(
                                        npc,
                                        new OrganizationId(
                                                LongArgumentType.getLong(
                                                        context,
                                                        "organization"
                                                )
                                        )
                                );
            }

            simulation.save();

            return executeShowFor(
                    context.getSource(),
                    npc
            );

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

    private static int executeShow(
            CommandContext<CommandSourceStack> context
    ) {
        return executeShowFor(
                context.getSource(),
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
                )
        );
    }

    private static int executeShowFor(
            CommandSourceStack source,
            NpcId npc
    ) {
        try {

            NpcSimulation simulation =
                    NpcSimulation.get();

            NpcAffiliation affiliation =
                    simulation.affiliations()
                            .getOrCreate(
                                    npc
                            );

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "NPC #"
                                            + npc
                                            + " affiliations:"
                            ),
                    false
            );

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "Residence: "
                                            + settlementName(
                                            simulation,
                                            affiliation.residenceSettlement()
                                    )
                            ),
                    false
            );

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "Household: "
                                            + organizationName(
                                            simulation,
                                            affiliation.household()
                                    )
                            ),
                    false
            );

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "Noble house: "
                                            + organizationName(
                                            simulation,
                                            affiliation.nobleHouse()
                                    )
                            ),
                    false
            );

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "Faction: "
                                            + organizationName(
                                            simulation,
                                            affiliation.faction()
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

    private static String settlementName(
            NpcSimulation simulation,
            SettlementId id
    ) {
        if (id == null) {
            return "none";
        }

        return simulation.settlements()
                .find(
                        id
                )
                .map(
                        settlement ->
                                settlement.name()
                                        + " (#"
                                        + settlement.id()
                                        + ")"
                )
                .orElse(
                        "unknown #" + id
                );
    }

    private static String organizationName(
            NpcSimulation simulation,
            OrganizationId id
    ) {
        if (id == null) {
            return "none";
        }

        return simulation.organizations()
                .find(
                        id
                )
                .map(
                        organization ->
                                organization.name()
                                        + " (#"
                                        + organization.id()
                                        + ")"
                )
                .orElse(
                        "unknown #" + id
                );
    }

    private static ServerPlayer player(
            CommandSourceStack source
    ) {
        try {

            return source.getPlayerOrException();

        } catch (Exception exception) {

            source.sendFailure(
                    Component.literal(
                            "This command must be executed by a player."
                    )
            );

            return null;
        }
    }

    private static SimulationPosition position(
            ServerPlayer player
    ) {
        Vec3 position =
                player.position();

        return new SimulationPosition(
                player.level()
                        .dimension()
                        .identifier()
                        .toString(),
                position.x,
                position.y,
                position.z
        );
    }

    private enum AffiliationKind {
        RESIDENCE,
        HOUSEHOLD,
        HOUSE,
        FACTION
    }
}
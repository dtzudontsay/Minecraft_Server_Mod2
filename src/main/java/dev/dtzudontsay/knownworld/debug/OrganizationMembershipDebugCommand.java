package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.LongArgumentType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class OrganizationMembershipDebugCommand {

    private OrganizationMembershipDebugCommand() {
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
                                                "kwmembership"
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
                                                                                context -> {

                                                                                    NpcId npc =
                                                                                            new NpcId(
                                                                                                    LongArgumentType.getLong(
                                                                                                            context,
                                                                                                            "npc"
                                                                                                    )
                                                                                            );

                                                                                    var runtime =
                                                                                            SocietyStructureRuntime.get();

                                                                                    var memberships =
                                                                                            runtime.memberships()
                                                                                                    .activeMembershipsFor(
                                                                                                            npc
                                                                                                    );

                                                                                    context.getSource()
                                                                                            .sendSuccess(
                                                                                                    () ->
                                                                                                            Component.literal(
                                                                                                                    "NPC #"
                                                                                                                            + npc
                                                                                                                            + " active memberships="
                                                                                                                            + memberships.size()
                                                                                                            ),
                                                                                                    false
                                                                                            );

                                                                                    for (
                                                                                            var membership :
                                                                                            memberships
                                                                                    ) {

                                                                                        String organizationName =
                                                                                                runtime.dynasties()
                                                                                                        .findByOrganization(
                                                                                                                membership.organization()
                                                                                                        )
                                                                                                        .map(
                                                                                                                dynasty ->
                                                                                                                        dynasty.name()
                                                                                                                                + " [dynasty]"
                                                                                                        )
                                                                                                        .orElse(
                                                                                                                "organization #"
                                                                                                                        + membership.organization()
                                                                                                        );

                                                                                        context.getSource()
                                                                                                .sendSuccess(
                                                                                                        () ->
                                                                                                                Component.literal(
                                                                                                                        organizationName
                                                                                                                                + " loyalty="
                                                                                                                                + membership.loyalty()
                                                                                                                                + " roles="
                                                                                                                                + membership.roles()
                                                                                                                ),
                                                                                                        false
                                                                                                );
                                                                                    }

                                                                                    return 1;
                                                                                }
                                                                        )
                                                        )
                                        )
                        )
        );
    }
}
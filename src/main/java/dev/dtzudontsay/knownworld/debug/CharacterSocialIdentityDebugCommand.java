package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.identity.CharacterSocialIdentitySnapshot;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class CharacterSocialIdentityDebugCommand {

    private CharacterSocialIdentityDebugCommand() {
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
                                                "kwidentity"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "stats"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    SocietyStructureRuntime runtime =
                                                                            SocietyStructureRuntime.get();

                                                                    send(
                                                                            context.getSource(),
                                                                            "Character social identities="
                                                                                    + runtime.characterSocialIdentities()
                                                                                    .size()
                                                                                    + " | NPCs="
                                                                                    + NpcSimulation.get()
                                                                                    .registry()
                                                                                    .size()
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Generic organization memberships="
                                                                                    + runtime.memberships()
                                                                                    .size()
                                                                                    + " | religious memberships="
                                                                                    + NpcSimulation.get()
                                                                                    .religiousMemberships()
                                                                                    .size()
                                                                    );

                                                                    return 1;
                                                                }
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "show"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    String authoredNpc =
                                                                                            StringArgumentType.getString(
                                                                                                    context,
                                                                                                    "npc"
                                                                                            );

                                                                                    NpcId npc =
                                                                                            NpcSimulation.get()
                                                                                                    .authoredIds()
                                                                                                    .requireNpc(
                                                                                                            authoredNpc
                                                                                                    );

                                                                                    return showIdentity(
                                                                                            context,
                                                                                            authoredNpc,
                                                                                            npc
                                                                                    );
                                                                                }
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "showid"
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

                                                                                    return showIdentity(
                                                                                            context,
                                                                                            "NPC #" + npc,
                                                                                            npc
                                                                                    );
                                                                                }
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int showIdentity(
            CommandContext<CommandSourceStack> context,
            String label,
            NpcId npc
    ) {

        try {

            CharacterSocialIdentitySnapshot snapshot =
                    SocietyStructureRuntime.get()
                            .characterSocialIdentityService()
                            .snapshot(
                                    npc
                            );

            send(
                    context.getSource(),
                    "=== "
                            + label
                            + " social identity ==="
            );

            send(
                    context.getSource(),
                    "birthDynasty="
                            + dynastyName(
                            snapshot.birthDynasty()
                    )
                            + " currentDynasty="
                            + dynastyName(
                            snapshot.currentDynasty()
                    )
            );

            send(
                    context.getSource(),
                    "marriedInto="
                            + dynastyName(
                            snapshot.marriedIntoDynasty()
                    )
                            + " legalFamily="
                            + dynastyName(
                            snapshot.legalFamilyDynasty()
                    )
            );

            send(
                    context.getSource(),
                    "biologicalParents="
                            + snapshot.biologicalMother()
                            + "/"
                            + snapshot.biologicalFather()
                            + " legalParents="
                            + snapshot.legalMother()
                            + "/"
                            + snapshot.legalFather()
            );

            send(
                    context.getSource(),
                    "household="
                            + snapshot.householdOrganization()
                            + " houseOrg="
                            + snapshot.houseOrganization()
            );

            send(
                    context.getSource(),
                    "allegiance="
                            + snapshot.primaryAllegianceOrganization()
                            + " strength="
                            + snapshot.allegianceStrength()
            );

            send(
                    context.getSource(),
                    "culture="
                            + snapshot.cultureId()
                            + " personalReligion="
                            + snapshot.personalReligionId()
            );

            send(
                    context.getSource(),
                    "organizations="
                            + snapshot.organizationMemberships()
                            .size()
                            + " societies="
                            + snapshot.societyMemberships()
                            .size()
                            + " governments/factions="
                            + snapshot.governmentAndFactionMemberships()
                            .size()
                            + " religiousInstitutions="
                            + snapshot.religiousMemberships()
                            .size()
            );

            snapshot.organizationMemberships()
                    .forEach(
                            membership ->
                                    send(
                                            context.getSource(),
                                            "ORG #"
                                                    + membership.organization()
                                                    + " loyalty="
                                                    + membership.loyalty()
                                                    + " roles="
                                                    + membership.roles()
                                    )
                    );

            snapshot.religiousMemberships()
                    .forEach(
                            membership ->
                                    send(
                                            context.getSource(),
                                            "REL "
                                                    + membership.orderId()
                                                    + " role="
                                                    + membership.roleId()
                                                    + " commitment="
                                                    + membership.commitment()
                                    )
                    );

            return 1;

        } catch (
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
    }

    private static String dynastyName(
            DynastyId dynasty
    ) {

        if (dynasty == null) {
            return "none";
        }

        return SocietyStructureRuntime.get()
                .dynasties()
                .find(
                        dynasty
                )
                .map(
                        Dynasty::name
                )
                .orElse(
                        "#" + dynasty
                );
    }

    private static void send(
            CommandSourceStack source,
            String text
    ) {

        source.sendSuccess(
                () ->
                        Component.literal(
                                text
                        ),
                false
        );
    }
}
package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionPracticeService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousAuthorityService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousInstitutionCatalog;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousMembership;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class ReligiousInstitutionDebugCommand {

    private ReligiousInstitutionDebugCommand() {
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
                                                "kwreligionorg"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "list"
                                                        )
                                                        .executes(
                                                                ReligiousInstitutionDebugCommand::list
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "organizations"
                                                        )
                                                        .executes(
                                                                ReligiousInstitutionDebugCommand::organizations
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
                                                                                ReligiousInstitutionDebugCommand::show
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "can_join"
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
                                                                                                "order",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                ReligiousInstitutionDebugCommand::canJoin
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "join"
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
                                                                                                "order",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .then(
                                                                                                Commands.argument(
                                                                                                                "role",
                                                                                                                StringArgumentType.word()
                                                                                                        )
                                                                                                        .executes(
                                                                                                                ReligiousInstitutionDebugCommand::join
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "leave"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "npc",
                                                                                LongArgumentType.longArg(
                                                                                        1
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                ReligiousInstitutionDebugCommand::leave
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "ritual"
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
                                                                                                "ritual",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                ReligiousInstitutionDebugCommand::ritual
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "taboo"
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
                                                                                                "taboo",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                ReligiousInstitutionDebugCommand::taboo
                                                                                        )
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
                                                                        .executes(
                                                                                ReligiousInstitutionDebugCommand::authority
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "legitimacy"
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
                                                                                                "religion",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                ReligiousInstitutionDebugCommand::legitimacy
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int list(
            CommandContext<CommandSourceStack> context
    ) {

        success(
                context,
                "Religious institution definitions:"
        );

        for (
                var definition :
                ReligiousInstitutionCatalog.get()
                        .all()
        ) {

            success(
                    context,
                    definition.id()
                            + " | "
                            + definition.displayName()
                            + " | religion="
                            + definition.religionId()
                            + " | category="
                            + definition.category()
            );
        }

        return 1;
    }

    private static int organizations(
            CommandContext<CommandSourceStack> context
    ) {

        NpcSimulation simulation =
                NpcSimulation.get();

        success(
                context,
                "Religious organization bindings: "
                        + simulation.religiousInstitutions()
                        .bindings()
        );

        return 1;
    }

    private static int show(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context
                    );

            ReligiousMembership membership =
                    NpcSimulation.get()
                            .religiousMemberships()
                            .find(
                                    npc
                            )
                            .orElse(
                                    null
                            );

            if (membership == null) {

                success(
                        context,
                        "NPC #"
                                + npc
                                + " has no religious membership."
                );

                return 1;
            }

            success(
                    context,
                    "Religious membership NPC #"
                            + npc
            );

            success(
                    context,
                    "order="
                            + membership.orderId()
                            + " organization=#"
                            + membership.organizationId()
                            + " role="
                            + (
                            membership.roleId()
                                    .isBlank()
                                    ? "none"
                                    : membership.roleId()
                    )
            );

            success(
                    context,
                    "commitment="
                            + format(
                            membership.commitment()
                    )
                            + " active="
                            + membership.active()
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

    private static int canJoin(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context
                    );

            String order =
                    StringArgumentType.getString(
                            context,
                            "order"
                    );

            success(
                    context,
                    "Can join "
                            + order
                            + ": "
                            + practice()
                            .canJoinOrder(
                                    npc,
                                    order
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

    private static int join(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context
                    );

            String order =
                    StringArgumentType.getString(
                            context,
                            "order"
                    );

            String role =
                    StringArgumentType.getString(
                            context,
                            "role"
                    );

            ReligiousMembership membership =
                    practice()
                            .joinOrder(
                                    npc,
                                    order,
                                    role
                            );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "NPC #"
                            + npc
                            + " joined "
                            + membership.orderId()
                            + " as "
                            + membership.roleId()
                            + " | organization #"
                            + membership.organizationId()
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

    private static int leave(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context
                    );

            practice()
                    .leaveOrder(
                            npc
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "NPC #"
                            + npc
                            + " left active religious membership."
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

    private static int ritual(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context
                    );

            String ritual =
                    StringArgumentType.getString(
                            context,
                            "ritual"
                    );

            practice()
                    .performRitual(
                            npc,
                            ritual
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Performed ritual "
                            + ritual
                            + " for NPC #"
                            + npc
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

    private static int taboo(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context
                    );

            String taboo =
                    StringArgumentType.getString(
                            context,
                            "taboo"
                    );

            practice()
                    .violateTaboo(
                            npc,
                            taboo
                    );

            NpcSimulation.get()
                    .save();

            success(
                    context,
                    "Recorded taboo violation "
                            + taboo
                            + " for NPC #"
                            + npc
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

    private static int authority(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context
                    );

            success(
                    context,
                    "Religious authority NPC #"
                            + npc
                            + " = "
                            + format(
                            authorityService()
                                    .religiousAuthority(
                                            npc
                                    )
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

    private static int legitimacy(
            CommandContext<CommandSourceStack> context
    ) {

        try {

            NpcId npc =
                    npc(
                            context
                    );

            String religion =
                    StringArgumentType.getString(
                            context,
                            "religion"
                    );

            var result =
                    authorityService()
                            .assessLegitimacy(
                                    npc,
                                    religion
                            );

            success(
                    context,
                    "Religious legitimacy support="
                            + format(
                            result.totalSupport()
                    )
            );

            success(
                    context,
                    "expected="
                            + result.expectedReligionId()
                            + " actual="
                            + result.characterReligionId()
                            + " alignment="
                            + format(
                            result.faithAlignment()
                    )
                            + " devotion="
                            + format(
                            result.devotionSupport()
                    )
                            + " observance="
                            + format(
                            result.observanceSupport()
                    )
                            + " institutional="
                            + format(
                            result.institutionalStanding()
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

    private static ReligionPracticeService practice() {

        NpcSimulation simulation =
                NpcSimulation.get();

        return new ReligionPracticeService(
                simulation.profiles(),
                religionService(),
                simulation.religiousMemberships(),
                simulation.religiousInstitutions()
        );
    }

    private static ReligionService religionService() {

        NpcSimulation simulation =
                NpcSimulation.get();

        CharacterPsychologyService psychology =
                new CharacterPsychologyService(
                        simulation.registry(),
                        simulation.profiles(),
                        simulation.relationships()
                );

        return new ReligionService(
                simulation.registry(),
                simulation.profiles(),
                simulation.relationships(),
                psychology
        );
    }

    private static ReligiousAuthorityService authorityService() {

        NpcSimulation simulation =
                NpcSimulation.get();

        return new ReligiousAuthorityService(
                simulation.profiles(),
                religionService(),
                simulation.religiousMemberships()
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
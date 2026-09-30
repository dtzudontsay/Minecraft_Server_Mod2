package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
                        )
        );
    }

    private static int executeShow(
            CommandContext<CommandSourceStack> context
    ) {

        NpcId npc =
                new NpcId(
                        LongArgumentType.getLong(
                                context,
                                "npc"
                        )
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

    private static String format(
            double value
    ) {

        return String.format(
                "%.2f",
                value
        );
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
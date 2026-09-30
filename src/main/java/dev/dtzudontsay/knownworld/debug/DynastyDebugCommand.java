package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DynastyDebugCommand {

    private DynastyDebugCommand() {
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
                                                "kwdynasty"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "list"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    var manager =
                                                                            SocietyStructureRuntime.get()
                                                                                    .dynasties();

                                                                    context.getSource()
                                                                            .sendSuccess(
                                                                                    () ->
                                                                                            Component.literal(
                                                                                                    "Dynasties: "
                                                                                                            + manager.size()
                                                                                            ),
                                                                                    false
                                                                            );

                                                                    for (
                                                                            Dynasty dynasty :
                                                                            manager.all()
                                                                    ) {

                                                                        context.getSource()
                                                                                .sendSuccess(
                                                                                        () ->
                                                                                                Component.literal(
                                                                                                        "#"
                                                                                                                + dynasty.id()
                                                                                                                + " "
                                                                                                                + dynasty.name()
                                                                                                                + " ["
                                                                                                                + dynasty.type()
                                                                                                                + "]"
                                                                                                ),
                                                                                        false
                                                                                );
                                                                    }

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
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    String authoredId =
                                                                                            StringArgumentType.getString(
                                                                                                    context,
                                                                                                    "id"
                                                                                            );

                                                                                    Dynasty dynasty =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .dynasties()
                                                                                                    .findAuthored(
                                                                                                            authoredId
                                                                                                    )
                                                                                                    .orElseThrow(
                                                                                                            () ->
                                                                                                                    new IllegalArgumentException(
                                                                                                                            "Unknown dynasty "
                                                                                                                                    + authoredId
                                                                                                                    )
                                                                                                    );

                                                                                    context.getSource()
                                                                                            .sendSuccess(
                                                                                                    () ->
                                                                                                            Component.literal(
                                                                                                                    dynasty.name()
                                                                                                                            + " | type="
                                                                                                                            + dynasty.type()
                                                                                                                            + " status="
                                                                                                                            + dynasty.status()
                                                                                                                            + " org=#"
                                                                                                                            + dynasty.organizationId()
                                                                                                            ),
                                                                                                    false
                                                                                            );

                                                                                    context.getSource()
                                                                                            .sendSuccess(
                                                                                                    () ->
                                                                                                            Component.literal(
                                                                                                                    "home="
                                                                                                                            + dynasty.homeLocationId()
                                                                                                                            + " culture="
                                                                                                                            + dynasty.cultureId()
                                                                                                                            + " religion="
                                                                                                                            + dynasty.religionId()
                                                                                                            ),
                                                                                                    false
                                                                                            );

                                                                                    context.getSource()
                                                                                            .sendSuccess(
                                                                                                    () ->
                                                                                                            Component.literal(
                                                                                                                    "parent="
                                                                                                                            + dynasty.parentDynasty()
                                                                                                                            + " liege="
                                                                                                                            + dynasty.liegeDynasty()
                                                                                                                            + " head="
                                                                                                                            + dynasty.head()
                                                                                                                            + " heir="
                                                                                                                            + dynasty.heir()
                                                                                                            ),
                                                                                                    false
                                                                                            );

                                                                                    context.getSource()
                                                                                            .sendSuccess(
                                                                                                    () ->
                                                                                                            Component.literal(
                                                                                                                    "prestige="
                                                                                                                            + dynasty.prestige()
                                                                                                                            + " wealth="
                                                                                                                            + dynasty.wealth()
                                                                                                                            + " military="
                                                                                                                            + dynasty.militaryStrength()
                                                                                                            ),
                                                                                                    false
                                                                                            );

                                                                                    return 1;
                                                                                }
                                                                        )
                                                        )
                                        )
                        )
        );
    }
}
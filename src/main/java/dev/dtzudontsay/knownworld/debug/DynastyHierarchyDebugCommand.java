package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DynastyHierarchyDebugCommand {

    private DynastyHierarchyDebugCommand() {
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
                                                "kwhierarchy"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "show"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "dynasty",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    Dynasty dynasty =
                                                                                            requireDynasty(
                                                                                                    StringArgumentType.getString(
                                                                                                            context,
                                                                                                            "dynasty"
                                                                                                    )
                                                                                            );

                                                                                    var runtime =
                                                                                            SocietyStructureRuntime.get();

                                                                                    Dynasty deJure =
                                                                                            runtime.dynastyAllegiances()
                                                                                                    .deJureLiege(
                                                                                                            dynasty.id()
                                                                                                    )
                                                                                                    .orElse(
                                                                                                            null
                                                                                                    );

                                                                                    Dynasty current =
                                                                                            runtime.dynastyAllegiances()
                                                                                                    .currentLiege(
                                                                                                            dynasty.id()
                                                                                                    )
                                                                                                    .orElse(
                                                                                                            null
                                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "=== "
                                                                                                    + dynasty.name()
                                                                                                    + " ==="
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "deJureLiege="
                                                                                                    + name(
                                                                                                            deJure
                                                                                                    )
                                                                                                    + " currentLiege="
                                                                                                    + name(
                                                                                                            current
                                                                                                    )
                                                                                                    + " override="
                                                                                                    + runtime.dynastyAllegiances()
                                                                                                    .hasOverride(
                                                                                                            dynasty.id()
                                                                                                    )
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "directCurrentVassals="
                                                                                                    + runtime.dynastyAllegiances()
                                                                                                    .directVassals(
                                                                                                            dynasty.id()
                                                                                                    )
                                                                                                    .size()
                                                                                    );

                                                                                    return 1;
                                                                                }
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "vassals"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "dynasty",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    Dynasty dynasty =
                                                                                            requireDynasty(
                                                                                                    StringArgumentType.getString(
                                                                                                            context,
                                                                                                            "dynasty"
                                                                                                    )
                                                                                            );

                                                                                    var vassals =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .dynastyAllegiances()
                                                                                                    .directVassals(
                                                                                                            dynasty.id()
                                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            dynasty.name()
                                                                                                    + " has "
                                                                                                    + vassals.size()
                                                                                                    + " direct current vassal(s):"
                                                                                    );

                                                                                    for (
                                                                                            Dynasty vassal :
                                                                                            vassals
                                                                                    ) {

                                                                                        send(
                                                                                                context.getSource(),
                                                                                                "- "
                                                                                                        + vassal.name()
                                                                                                        + " ["
                                                                                                        + vassal.authoredId()
                                                                                                        + "]"
                                                                                        );
                                                                                    }

                                                                                    return vassals.size();
                                                                                }
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "chain"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "dynasty",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    Dynasty dynasty =
                                                                                            requireDynasty(
                                                                                                    StringArgumentType.getString(
                                                                                                            context,
                                                                                                            "dynasty"
                                                                                                    )
                                                                                            );

                                                                                    var chain =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .dynastyAllegiances()
                                                                                                    .liegeChain(
                                                                                                            dynasty.id()
                                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            dynasty.name()
                                                                                                    + " current liege chain:"
                                                                                    );

                                                                                    if (chain.isEmpty()) {

                                                                                        send(
                                                                                                context.getSource(),
                                                                                                "- independent/root dynasty"
                                                                                        );

                                                                                    } else {

                                                                                        for (
                                                                                                Dynasty liege :
                                                                                                chain
                                                                                        ) {

                                                                                            send(
                                                                                                    context.getSource(),
                                                                                                    "-> "
                                                                                                            + liege.name()
                                                                                                            + " ["
                                                                                                            + liege.authoredId()
                                                                                                            + "]"
                                                                                            );
                                                                                        }
                                                                                    }

                                                                                    return chain.size();
                                                                                }
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static Dynasty requireDynasty(
            String authoredId
    ) {

        return SocietyStructureRuntime.get()
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
    }

    private static String name(
            Dynasty dynasty
    ) {

        return dynasty == null
                ? "none"
                : dynasty.name()
                + " ["
                + dynasty.authoredId()
                + "]";
    }

    private static void send(
            net.minecraft.commands.CommandSourceStack source,
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

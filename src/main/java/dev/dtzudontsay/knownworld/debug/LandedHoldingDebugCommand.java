package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class LandedHoldingDebugCommand {

    private LandedHoldingDebugCommand() {
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
                                                "kwholding"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "stats"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    var runtime =
                                                                            SocietyStructureRuntime.get();

                                                                    send(
                                                                            context.getSource(),
                                                                            "Holdings="
                                                                                    + runtime.holdings()
                                                                                    .size()
                                                                    );

                                                                    long personallyHeld =
                                                                            runtime.holdings()
                                                                                    .all()
                                                                                    .stream()
                                                                                    .filter(
                                                                                            holding ->
                                                                                                    holding.holderNpcId()
                                                                                                            != null
                                                                                    )
                                                                                    .count();

                                                                    long dynastyOwned =
                                                                            runtime.holdings()
                                                                                    .all()
                                                                                    .stream()
                                                                                    .filter(
                                                                                            holding ->
                                                                                                    holding.ownerDynastyId()
                                                                                                            != null
                                                                                    )
                                                                                    .count();

                                                                    send(
                                                                            context.getSource(),
                                                                            "Dynasty-owned="
                                                                                    + dynastyOwned
                                                                                    + " personally-held="
                                                                                    + personallyHeld
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
                                                                                "holding",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    String authoredId =
                                                                                            StringArgumentType.getString(
                                                                                                    context,
                                                                                                    "holding"
                                                                                            );

                                                                                    LandedHolding holding =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .holdings()
                                                                                                    .requireAuthored(
                                                                                                            authoredId
                                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "=== "
                                                                                                    + holding.name()
                                                                                                    + " ==="
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "id="
                                                                                                    + holding.authoredId()
                                                                                                    + " type="
                                                                                                    + holding.type()
                                                                                                    + " status="
                                                                                                    + holding.status()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "location="
                                                                                                    + holding.worldLocationId()
                                                                                                    + " parent="
                                                                                                    + holding.parentHoldingId()
                                                                                                    + " capital="
                                                                                                    + holding.capital()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "deJureDynasty="
                                                                                                    + dynastyName(
                                                                                                    holding.deJureDynastyId()
                                                                                            )
                                                                                                    + " ownerDynasty="
                                                                                                    + dynastyName(
                                                                                                    holding.ownerDynastyId()
                                                                                            )
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "holderNpc="
                                                                                                    + holding.holderNpcId()
                                                                                                    + " government="
                                                                                                    + holding.governmentOrganizationId()
                                                                                                    + " title="
                                                                                                    + holding.linkedTitleId()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "tax="
                                                                                                    + holding.taxBase()
                                                                                                    + " military="
                                                                                                    + holding.militaryValue()
                                                                                                    + " populationWeight="
                                                                                                    + holding.populationWeight()
                                                                                    );

                                                                                    return 1;
                                                                                }
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "dynasty"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "dynasty",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    String dynastyId =
                                                                                            StringArgumentType.getString(
                                                                                                    context,
                                                                                                    "dynasty"
                                                                                            );

                                                                                    Dynasty dynasty =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .dynasties()
                                                                                                    .findAuthored(
                                                                                                            dynastyId
                                                                                                    )
                                                                                                    .orElseThrow(
                                                                                                            () ->
                                                                                                                    new IllegalArgumentException(
                                                                                                                            "Unknown dynasty "
                                                                                                                                    + dynastyId
                                                                                                                    )
                                                                                                    );

                                                                                    var holdings =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .holdings()
                                                                                                    .ownedBy(
                                                                                                            dynasty.id()
                                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            dynasty.name()
                                                                                                    + " owns "
                                                                                                    + holdings.size()
                                                                                                    + " holding(s):"
                                                                                    );

                                                                                    for (
                                                                                            LandedHolding holding :
                                                                                            holdings
                                                                                    ) {

                                                                                        send(
                                                                                                context.getSource(),
                                                                                                "- "
                                                                                                        + holding.name()
                                                                                                        + " ["
                                                                                                        + holding.authoredId()
                                                                                                        + "] holder="
                                                                                                        + holding.holderNpcId()
                                                                                        );
                                                                                    }

                                                                                    return holdings.size();
                                                                                }
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static String dynastyName(
            dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId id
    ) {

        if (id == null) {

            return "none";
        }

        return SocietyStructureRuntime.get()
                .dynasties()
                .find(
                        id
                )
                .map(
                        Dynasty::name
                )
                .orElse(
                        "#" + id
                );
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
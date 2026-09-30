package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.society.KhalasarSuccessionSnapshot;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSociety;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class NonDynasticSocietyDebugCommand {

    private NonDynasticSocietyDebugCommand() {
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
                                                "kwsociety"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "list"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    var manager =
                                                                            SocietyStructureRuntime.get()
                                                                                    .nonDynasticSocieties();

                                                                    send(
                                                                            context.getSource(),
                                                                            "Non-dynastic societies: "
                                                                                    + manager.size()
                                                                    );

                                                                    for (
                                                                            NonDynasticSociety society :
                                                                            manager.all()
                                                                    ) {

                                                                        send(
                                                                                context.getSource(),
                                                                                society.name()
                                                                                        + " ["
                                                                                        + society.type()
                                                                                        + "] status="
                                                                                        + society.status()
                                                                                        + " org=#"
                                                                                        + society.organizationId()
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

                                                                                    String id =
                                                                                            StringArgumentType.getString(
                                                                                                    context,
                                                                                                    "id"
                                                                                            );

                                                                                    NonDynasticSociety society =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .nonDynasticSocieties()
                                                                                                    .findAuthored(
                                                                                                            id
                                                                                                    )
                                                                                                    .orElseThrow(
                                                                                                            () ->
                                                                                                                    new IllegalArgumentException(
                                                                                                                            "Unknown society "
                                                                                                                                    + id
                                                                                                                    )
                                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            society.name()
                                                                                                    + " | type="
                                                                                                    + society.type()
                                                                                                    + " status="
                                                                                                    + society.status()
                                                                                                    + " org=#"
                                                                                                    + society.organizationId()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "home="
                                                                                                    + society.homeLocationId()
                                                                                                    + " culture="
                                                                                                    + society.cultureId()
                                                                                                    + " religion="
                                                                                                    + society.religionId()
                                                                                                    + " mobile="
                                                                                                    + society.mobile()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "primary="
                                                                                                    + society.primaryLeader()
                                                                                                    + " role="
                                                                                                    + society.primaryLeaderRole()
                                                                                                    + " secondary="
                                                                                                    + society.secondaryLeader()
                                                                                                    + " role="
                                                                                                    + society.secondaryLeaderRole()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "population="
                                                                                                    + society.populationEstimate()
                                                                                                    + " military="
                                                                                                    + society.militaryStrength()
                                                                                                    + " cohesion="
                                                                                                    + society.cohesion()
                                                                                    );

                                                                                    return 1;
                                                                                }
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "khalasar"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    String id =
                                                                                            StringArgumentType.getString(
                                                                                                    context,
                                                                                                    "id"
                                                                                            );

                                                                                    NonDynasticSociety society =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .nonDynasticSocieties()
                                                                                                    .findAuthored(
                                                                                                            id
                                                                                                    )
                                                                                                    .orElseThrow();

                                                                                    KhalasarSuccessionSnapshot snapshot =
                                                                                            SocietyStructureRuntime.get()
                                                                                                    .nonDynasticSocietyService()
                                                                                                    .khalasarSuccessionSnapshot(
                                                                                                            society.organizationId()
                                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "Khal="
                                                                                                    + snapshot.khal()
                                                                                                    + " khalakkas="
                                                                                                    + snapshot.khalakkas()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "kos="
                                                                                                    + snapshot.kos()
                                                                                                    + " bloodriders="
                                                                                                    + snapshot.bloodriders()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "khaleesis="
                                                                                                    + snapshot.khaleesis()
                                                                                                    + " canSplinter="
                                                                                                    + snapshot.canSplinter()
                                                                                                    + " hereditaryGuaranteed="
                                                                                                    + snapshot.hereditarySuccessionGuaranteed()
                                                                                    );

                                                                                    return 1;
                                                                                }
                                                                        )
                                                        )
                                        )
                        )
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
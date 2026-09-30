package dev.dtzudontsay.knownworld.debug;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioStartCatalog;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioStartDefinition;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ScenarioStartDebugCommand {

    private ScenarioStartDebugCommand() {
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
                                                "kwstart"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "status"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    ScenarioStartCatalog catalog =
                                                                            ScenarioStartCatalog.get();

                                                                    ScenarioStartDefinition start =
                                                                            catalog.definition();

                                                                    NpcSimulation simulation =
                                                                            NpcSimulation.get();

                                                                    catalog.validateCalendar(
                                                                            simulation.campaignCalendar()
                                                                    );

                                                                    long elapsed =
                                                                            catalog.daysSinceStart(
                                                                                    simulation.campaignCalendar()
                                                                            );

                                                                    send(
                                                                            context.getSource(),
                                                                            "=== Known World Scenario Start ==="
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Scenario="
                                                                                    + start.scenarioId()
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Anchor="
                                                                                    + start.anchorId()
                                                                                    + " — "
                                                                                    + start.displayName()
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Start="
                                                                                    + start.startYear()
                                                                                    + " AC / scenario day "
                                                                                    + start.humanStartDay()
                                                                                    + " | season="
                                                                                    + start.seasonId()
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Current="
                                                                                    + simulation.campaignCalendar()
                                                                                    .year()
                                                                                    + " AC / day "
                                                                                    + (
                                                                                    simulation.campaignCalendar()
                                                                                            .dayOfYear()
                                                                                            + 1
                                                                            )
                                                                                    + " | elapsed="
                                                                                    + elapsed
                                                                                    + " campaign day(s)"
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Chronology certainty="
                                                                                    + start.chronologyCertainty()
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            start.description()
                                                                    );

                                                                    return 1;
                                                                }
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "facts"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    ScenarioStartDefinition start =
                                                                            ScenarioStartCatalog.get()
                                                                                    .definition();

                                                                    send(
                                                                            context.getSource(),
                                                                            "=== Established at Scenario Start ==="
                                                                    );

                                                                    for (
                                                                            String fact :
                                                                            start.establishedFacts()
                                                                    ) {

                                                                        send(
                                                                                context.getSource(),
                                                                                "- "
                                                                                        + fact
                                                                        );
                                                                    }

                                                                    return start.establishedFacts()
                                                                            .size();
                                                                }
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "pending"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    ScenarioStartDefinition start =
                                                                            ScenarioStartCatalog.get()
                                                                                    .definition();

                                                                    send(
                                                                            context.getSource(),
                                                                            "=== Not Yet Occurred at Scenario Start ==="
                                                                    );

                                                                    for (
                                                                            String fact :
                                                                            start.notYetOccurred()
                                                                    ) {

                                                                        send(
                                                                                context.getSource(),
                                                                                "- "
                                                                                        + fact
                                                                        );
                                                                    }

                                                                    return start.notYetOccurred()
                                                                            .size();
                                                                }
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "essos"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    ScenarioStartDefinition start =
                                                                            ScenarioStartCatalog.get()
                                                                                    .definition();

                                                                    send(
                                                                            context.getSource(),
                                                                            "=== Essos Synchronization Rule ==="
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            start.essosSynchronizationNote()
                                                                    );

                                                                    return 1;
                                                                }
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
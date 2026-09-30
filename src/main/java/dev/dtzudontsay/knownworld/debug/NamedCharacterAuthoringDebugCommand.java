package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.bootstrap.NamedCharacterAuthoringRuntime;
import dev.dtzudontsay.knownworld.simulation.npc.scenario.CharacterScenarioState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class NamedCharacterAuthoringDebugCommand {

    private NamedCharacterAuthoringDebugCommand() {
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
                                                "kwcharprep"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "summary"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    NamedCharacterAuthoringRuntime.AuditReport report =
                                                                            NamedCharacterAuthoringRuntime.get()
                                                                                    .audit();

                                                                    send(
                                                                            context.getSource(),
                                                                            "=== Named Character Authoring Audit ==="
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "AuthoredNPCs="
                                                                                    + report.authoredNpcCount()
                                                                                    + " ScenarioStates="
                                                                                    + report.scenarioStateCount()
                                                                                    + " Relationships="
                                                                                    + report.relationshipCount()
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Errors="
                                                                                    + report.errorCount()
                                                                                    + " Warnings="
                                                                                    + report.warningCount()
                                                                                    + " Info="
                                                                                    + report.infoCount()
                                                                    );

                                                                    return report.errorCount() == 0
                                                                            ? 1
                                                                            : 0;
                                                                }
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "errors"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        sendIssues(
                                                                                context.getSource(),
                                                                                NamedCharacterAuthoringRuntime.get()
                                                                                        .audit()
                                                                                        .errors(),
                                                                                "ERRORS"
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "warnings"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        sendIssues(
                                                                                context.getSource(),
                                                                                NamedCharacterAuthoringRuntime.get()
                                                                                        .audit()
                                                                                        .warnings(),
                                                                                "WARNINGS"
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "state"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "character",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    String authoredId =
                                                                                            StringArgumentType.getString(
                                                                                                    context,
                                                                                                    "character"
                                                                                            );

                                                                                    CharacterScenarioState state =
                                                                                            NamedCharacterAuthoringRuntime.get()
                                                                                                    .findAuthored(
                                                                                                            authoredId
                                                                                                    )
                                                                                                    .orElse(
                                                                                                            null
                                                                                                    );

                                                                                    if (state == null) {

                                                                                        send(
                                                                                                context.getSource(),
                                                                                                "No scenario-state record for "
                                                                                                        + authoredId
                                                                                        );

                                                                                        return 0;
                                                                                    }

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "=== "
                                                                                                    + authoredId
                                                                                                    + " ==="
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "Presence="
                                                                                                    + state.presence()
                                                                                                    + " Continuity="
                                                                                                    + state.continuity()
                                                                                                    + " Confidence="
                                                                                                    + state.confidence()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "StartLocation="
                                                                                                    + state.startingWorldLocationId()
                                                                                                    + " CurrentLocation="
                                                                                                    + state.currentWorldLocationId()
                                                                                    );

                                                                                    send(
                                                                                            context.getSource(),
                                                                                            "AuthoringVersion="
                                                                                                    + state.authoredVersion()
                                                                                    );

                                                                                    if (!state.note()
                                                                                            .isBlank()) {

                                                                                        send(
                                                                                                context.getSource(),
                                                                                                state.note()
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

    private static int sendIssues(
            net.minecraft.commands.CommandSourceStack source,
            List<NamedCharacterAuthoringRuntime.AuditIssue> issues,
            String title
    ) {

        send(
                source,
                "=== "
                        + title
                        + " ("
                        + issues.size()
                        + ") ==="
        );

        if (issues.isEmpty()) {

            send(
                    source,
                    "None."
            );

            return 1;
        }

        int shown =
                0;

        for (
                NamedCharacterAuthoringRuntime.AuditIssue issue :
                issues
        ) {

            send(
                    source,
                    "["
                            + issue.code()
                            + "] "
                            + issue.subject()
                            + " — "
                            + issue.message()
            );

            shown++;

            if (shown >= 50) {

                if (issues.size() > shown) {

                    send(
                            source,
                            "... "
                                    + (issues.size() - shown)
                                    + " more issue(s)."
                    );
                }

                break;
            }
        }

        return issues.size();
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
package dev.dtzudontsay.knownworld.debug;

import dev.dtzudontsay.knownworld.world.reference.ReferenceIntegrityValidator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ReferenceValidationDebugCommand {

    private ReferenceValidationDebugCommand() {
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
                                                "kwvalidate"
                                        )
                                        .executes(
                                                context -> {

                                                    try {

                                                        ReferenceIntegrityValidator.validateDefaultScenario();

                                                        context.getSource()
                                                                .sendSuccess(
                                                                        () ->
                                                                                Component.literal(
                                                                                        "Known World reference validation passed."
                                                                                ),
                                                                        false
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
                                        )
                        )
        );
    }
}
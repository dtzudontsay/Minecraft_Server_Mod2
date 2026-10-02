package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.social.relationship.EffectiveDynastyRelationshipResolver;
import dev.dtzudontsay.knownworld.simulation.social.relationship.EffectiveRelationshipResolver;
import dev.dtzudontsay.knownworld.simulation.social.relationship.GlobalRelationshipRuntime;
import dev.dtzudontsay.knownworld.simulation.social.relationship.RelationshipReviewAudit;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class GlobalRelationshipDebugCommand {

    private GlobalRelationshipDebugCommand() {
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
                                                "kwrel"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "stats"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    GlobalRelationshipRuntime runtime =
                                                                            GlobalRelationshipRuntime.get();

                                                                    send(
                                                                            context.getSource(),
                                                                            "=== Global Relationship Runtime ==="
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Character authored="
                                                                                    + runtime.authoredCharacterRelationshipCount()
                                                                                    + " applied="
                                                                                    + runtime.appliedCharacterRelationshipCount()
                                                                                    + " deferred="
                                                                                    + runtime.deferredCharacterRelationshipCount()
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "Dynasty authored="
                                                                                    + runtime.authoredDynastyRelationshipCount()
                                                                                    + " runtime="
                                                                                    + runtime.dynastyRelationships()
                                                                                    .size()
                                                                    );

                                                                    return 1;
                                                                }
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "npc"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "subject",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "target",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                context -> {

                                                                                                    String subject =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "subject"
                                                                                                            );

                                                                                                    String target =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "target"
                                                                                                            );

                                                                                                    EffectiveRelationshipResolver.EffectiveRelationship relationship =
                                                                                                            GlobalRelationshipRuntime.get()
                                                                                                                    .resolve(
                                                                                                                            subject,
                                                                                                                            target
                                                                                                                    );

                                                                                                    send(
                                                                                                            context.getSource(),
                                                                                                            subject
                                                                                                                    + " -> "
                                                                                                                    + target
                                                                                                                    + " source="
                                                                                                                    + relationship.source()
                                                                                                    );

                                                                                                    send(
                                                                                                            context.getSource(),
                                                                                                            "affection="
                                                                                                                    + relationship.affection()
                                                                                                                    + " trust="
                                                                                                                    + relationship.trust()
                                                                                                                    + " respect="
                                                                                                                    + relationship.respect()
                                                                                                                    + " fear="
                                                                                                                    + relationship.fear()
                                                                                                                    + " familiarity="
                                                                                                                    + relationship.familiarity()
                                                                                                    );

                                                                                                    send(
                                                                                                            context.getSource(),
                                                                                                            "subjectDynasty="
                                                                                                                    + relationship.subjectDynasty()
                                                                                                                    + " targetDynasty="
                                                                                                                    + relationship.targetDynasty()
                                                                                                    );

                                                                                                    send(
                                                                                                            context.getSource(),
                                                                                                            "contextModifiers="
                                                                                                                    + relationship.contextModifiers()
                                                                                                    );

                                                                                                    return 1;
                                                                                                }
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "review"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    RelationshipReviewAudit.Report report =
                                                                            GlobalRelationshipRuntime.get()
                                                                                    .relationshipReviewReport();

                                                                    send(
                                                                            context.getSource(),
                                                                            "=== Relationship Review Audit ==="
                                                                    );

                                                                    send(
                                                                            context.getSource(),
                                                                            "total="
                                                                                    + report.total()
                                                                                    + " direct="
                                                                                    + report.count(
                                                                                    RelationshipReviewAudit.Status.HAS_DIRECT_RELATION
                                                                            )
                                                                                    + " fallbackOnly="
                                                                                    + report.count(
                                                                                    RelationshipReviewAudit.Status.FALLBACK_ONLY
                                                                            )
                                                                                    + " noSupported="
                                                                                    + report.count(
                                                                                    RelationshipReviewAudit.Status.NO_SUPPORTED_RELATION_FOUND
                                                                            )
                                                                                    + " specialPending="
                                                                                    + report.count(
                                                                                    RelationshipReviewAudit.Status.SPECIAL_ENTITY_PENDING
                                                                            )
                                                                                    + " unreviewed="
                                                                                    + report.count(
                                                                                    RelationshipReviewAudit.Status.UNREVIEWED
                                                                            )
                                                                    );

                                                                    return 1;
                                                                }
                                                        )

                                                        .then(
                                                                Commands.argument(
                                                                                "status",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> {

                                                                                    String raw =
                                                                                            StringArgumentType.getString(
                                                                                                    context,
                                                                                                    "status"
                                                                                            );

                                                                                    RelationshipReviewAudit.Status status;

                                                                                    try {

                                                                                        status =
                                                                                                RelationshipReviewAudit.Status.valueOf(
                                                                                                        raw.toUpperCase()
                                                                                                );

                                                                                    } catch (
                                                                                            IllegalArgumentException exception
                                                                                    ) {

                                                                                        send(
                                                                                                context.getSource(),
                                                                                                "Unknown review status "
                                                                                                        + raw
                                                                                        );

                                                                                        return 0;
                                                                                    }

                                                                                    for (
                                                                                            RelationshipReviewAudit.Result result :
                                                                                            GlobalRelationshipRuntime.get()
                                                                                                    .relationshipReviewReport()
                                                                                                    .withStatus(
                                                                                                            status
                                                                                                    )
                                                                                    ) {

                                                                                        send(
                                                                                                context.getSource(),
                                                                                                result.rosterName()
                                                                                                        + " ["
                                                                                                        + result.npcId()
                                                                                                        + "] edges="
                                                                                                        + result.directEdgeCount()
                                                                                                        + " fallback="
                                                                                                        + result.fallbackAvailable()
                                                                                        );
                                                                                    }

                                                                                    return 1;
                                                                                }
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "house"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "subject",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "target",
                                                                                                StringArgumentType.word()
                                                                                        )
                                                                                        .executes(
                                                                                                context -> {

                                                                                                    String subject =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "subject"
                                                                                                            );

                                                                                                    String target =
                                                                                                            StringArgumentType.getString(
                                                                                                                    context,
                                                                                                                    "target"
                                                                                                            );

                                                                                                    EffectiveDynastyRelationshipResolver.EffectiveDynastyRelationship relationship =
                                                                                                            GlobalRelationshipRuntime.get()
                                                                                                                    .resolveDynastyRelationship(
                                                                                                                            subject,
                                                                                                                            target
                                                                                                                    );

                                                                                                    send(
                                                                                                            context.getSource(),
                                                                                                            subject
                                                                                                                    + " -> "
                                                                                                                    + target
                                                                                                                    + " source="
                                                                                                                    + relationship.source()
                                                                                                    );

                                                                                                    send(
                                                                                                            context.getSource(),
                                                                                                            "affinity="
                                                                                                                    + relationship.affinity()
                                                                                                                    + " trust="
                                                                                                                    + relationship.trust()
                                                                                                                    + " respect="
                                                                                                                    + relationship.respect()
                                                                                                                    + " fear="
                                                                                                                    + relationship.fear()
                                                                                                                    + " familiarity="
                                                                                                                    + relationship.familiarity()
                                                                                                    );

                                                                                                    send(
                                                                                                            context.getSource(),
                                                                                                            "anchor="
                                                                                                                    + relationship.subjectAnchor()
                                                                                                                    + " -> "
                                                                                                                    + relationship.targetAnchor()
                                                                                                                    + " steps="
                                                                                                                    + relationship.subjectSteps()
                                                                                                                    + "+"
                                                                                                                    + relationship.targetSteps()
                                                                                                                    + " attenuation="
                                                                                                                    + relationship.attenuation()
                                                                                                    );

                                                                                                    return 1;
                                                                                                }
                                                                                        )
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

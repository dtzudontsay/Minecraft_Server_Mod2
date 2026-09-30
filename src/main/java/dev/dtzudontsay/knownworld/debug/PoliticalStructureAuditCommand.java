package dev.dtzudontsay.knownworld.debug;

import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.integrity.PoliticalStructureIntegrityReport;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PoliticalStructureAuditCommand {

    private PoliticalStructureAuditCommand() {
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
                                                "kwaudit"
                                        )
                                        .then(
                                                Commands.literal(
                                                                "summary"
                                                        )
                                                        .executes(
                                                                context -> {

                                                                    PoliticalStructureIntegrityReport report =
                                                                            SocietyStructureRuntime.get()
                                                                                    .auditPoliticalStructure();

                                                                    sendSummary(
                                                                            context.getSource(),
                                                                            report
                                                                    );

                                                                    return report.clean()
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
                                                                                SocietyStructureRuntime.get()
                                                                                        .auditPoliticalStructure()
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
                                                                                SocietyStructureRuntime.get()
                                                                                        .auditPoliticalStructure()
                                                                                        .warnings(),
                                                                                "WARNINGS"
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal(
                                                                "info"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        sendIssues(
                                                                                context.getSource(),
                                                                                SocietyStructureRuntime.get()
                                                                                        .auditPoliticalStructure()
                                                                                        .info(),
                                                                                "INFO"
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static void sendSummary(
            net.minecraft.commands.CommandSourceStack source,
            PoliticalStructureIntegrityReport report
    ) {

        send(
                source,
                "=== Known World Political Structure Audit ==="
        );

        send(
                source,
                "Dynasties="
                        + report.dynastyCount()
                        + " Holdings="
                        + report.holdingCount()
                        + " AllegianceOverrides="
                        + report.allegianceOverrideCount()
        );

        send(
                source,
                "Errors="
                        + report.errorCount()
                        + " Warnings="
                        + report.warningCount()
                        + " Info="
                        + report.infoCount()
        );

        send(
                source,
                report.clean()
                        ? "STRICT RESULT: PASS"
                        : "STRICT RESULT: FAIL"
        );
    }

    private static int sendIssues(
            net.minecraft.commands.CommandSourceStack source,
            List<PoliticalStructureIntegrityReport.Issue> issues,
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
                PoliticalStructureIntegrityReport.Issue issue :
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
                                    + " more issue(s) not shown."
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
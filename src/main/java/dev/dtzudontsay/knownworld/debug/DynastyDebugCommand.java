package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyIntegrityReport;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DynastyDebugCommand {

    private static final int MAX_AUDIT_LINES =
            12;

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
                                                                context ->
                                                                        list(
                                                                                context.getSource()
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "stats"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        stats(
                                                                                context.getSource()
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "audit"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        audit(
                                                                                context.getSource()
                                                                        )
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
                                                                                context ->
                                                                                        show(
                                                                                                context.getSource(),
                                                                                                StringArgumentType.getString(
                                                                                                        context,
                                                                                                        "id"
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int list(
            net.minecraft.commands.CommandSourceStack source
    ) {

        var manager =
                SocietyStructureRuntime.get()
                        .dynasties();

        long active =
                manager.all()
                        .stream()
                        .filter(
                                Dynasty::activeAtScenarioStart
                        )
                        .count();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Dynasties: "
                                        + manager.size()
                                        + " catalogued, "
                                        + active
                                        + " active at 298 AC."
                        ),
                false
        );

        for (
                Dynasty dynasty :
                manager.all()
        ) {

            String line =
                    "#"
                            + dynasty.id()
                            + " "
                            + dynasty.name()
                            + " ["
                            + dynasty.type()
                            + "] "
                            + dynasty.status()
                            + " active="
                            + dynasty.activeAtScenarioStart();

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    line
                            ),
                    false
            );
        }

        return 1;
    }

    private static int stats(
            net.minecraft.commands.CommandSourceStack source
    ) {

        SocietyStructureRuntime runtime =
                SocietyStructureRuntime.get();

        DynastyIntegrityReport report =
                runtime.auditDynasties();

        source.sendSuccess(
                () ->
                        Component.literal(
                                "=== Known World Dynasty Statistics ==="
                        ),
                false
        );

        send(
                source,
                "Total: "
                        + report.totalDynasties()
                        + " | authored="
                        + report.authoredDynasties()
                        + " generated="
                        + report.generatedDynasties()
        );

        send(
                source,
                "298 AC: active="
                        + report.activeAtScenarioStart()
                        + " inactive="
                        + report.inactiveAtScenarioStart()
                        + " organizations="
                        + report.activeRuntimeOrganizations()
        );

        send(
                source,
                "Status: extinct="
                        + report.extinctDynasties()
                        + " exiled="
                        + report.exiledDynasties()
                        + " dispossessed="
                        + report.dispossessedDynasties()
                        + " dormant="
                        + report.dormantDynasties()
                        + " notYetFounded="
                        + report.notYetFoundedDynasties()
                        + " unknown="
                        + report.unknownStatusDynasties()
        );

        send(
                source,
                "Population gaps: noHead="
                        + report.dynastiesWithoutHeads()
                        + " noHeir="
                        + report.dynastiesWithoutHeirs()
        );

        send(
                source,
                "Reference gaps: noHome="
                        + report.dynastiesWithoutHome()
                        + " noCulture="
                        + report.dynastiesWithoutCulture()
                        + " noReligion="
                        + report.dynastiesWithoutReligion()
        );

        send(
                source,
                "Audit: errors="
                        + report.errorCount()
                        + " warnings="
                        + report.warningCount()
        );

        var activation =
                runtime.lastActivationReport();

        if (activation != null) {

            send(
                    source,
                    "Last activation: new="
                            + activation.newlyCreatedDynasties()
                            + " reconciled="
                            + activation.reconciledDynasties()
                            + " staleRemoved="
                            + activation.stalePersistedDynastiesRemoved()
            );

            send(
                    source,
                    "Authored NPC links: heads="
                            + activation.resolvedHeads()
                            + "/"
                            + (activation.resolvedHeads()
                            + activation.unresolvedHeads())
                            + " heirs="
                            + activation.resolvedHeirs()
                            + "/"
                            + (activation.resolvedHeirs()
                            + activation.unresolvedHeirs())
            );
        }

        return report.passed()
                ? 1
                : 0;
    }

    private static int audit(
            net.minecraft.commands.CommandSourceStack source
    ) {

        DynastyIntegrityReport report =
                SocietyStructureRuntime.get()
                        .auditDynasties();

        if (report.passed()) {

            send(
                    source,
                    "Dynasty integrity: PASSED with "
                            + report.warningCount()
                            + " warning(s)."
            );

        } else {

            send(
                    source,
                    "Dynasty integrity: FAILED with "
                            + report.errorCount()
                            + " error(s) and "
                            + report.warningCount()
                            + " warning(s)."
            );
        }

        int shown =
                0;

        for (
                String error :
                report.errors()
        ) {

            if (shown >= MAX_AUDIT_LINES) {
                break;
            }

            send(
                    source,
                    "ERROR: "
                            + error
            );

            shown++;
        }

        for (
                String warning :
                report.warnings()
        ) {

            if (shown >= MAX_AUDIT_LINES) {
                break;
            }

            send(
                    source,
                    "WARN: "
                            + warning
            );

            shown++;
        }

        int remaining =
                report.errorCount()
                        + report.warningCount()
                        - shown;

        if (remaining > 0) {

            send(
                    source,
                    "... "
                            + remaining
                            + " additional audit entries not shown."
            );
        }

        return report.passed()
                ? 1
                : 0;
    }

    private static int show(
            net.minecraft.commands.CommandSourceStack source,
            String authoredId
    ) {

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

        String organization =
                dynasty.hasOrganization()
                        ? "#"
                        + dynasty.organizationId()
                        : "none";

        send(
                source,
                dynasty.name()
                        + " | type="
                        + dynasty.type()
                        + " status="
                        + dynasty.status()
                        + " active298="
                        + dynasty.activeAtScenarioStart()
                        + " org="
                        + organization
        );

        send(
                source,
                "continuities="
                        + dynasty.continuities()
                        + " provenance="
                        + dynasty.provenance()
        );

        send(
                source,
                "home="
                        + dynasty.homeLocationId()
                        + " culture="
                        + dynasty.cultureId()
                        + " religion="
                        + dynasty.religionId()
        );

        send(
                source,
                "parent="
                        + dynasty.parentDynasty()
                        + " liege="
                        + dynasty.liegeDynasty()
                        + " predecessor="
                        + dynasty.predecessorDynasty()
                        + " successor="
                        + dynasty.successorDynasty()
        );

        send(
                source,
                "head="
                        + dynasty.head()
                        + " heir="
                        + dynasty.heir()
                        + " founded="
                        + dynasty.foundedYear()
                        + " extinct="
                        + dynasty.extinctYear()
        );

        send(
                source,
                "prestige="
                        + dynasty.prestige()
                        + " wealth="
                        + dynasty.wealth()
                        + " military="
                        + dynasty.militaryStrength()
        );

        if (!dynasty.words()
                .isBlank()) {

            send(
                    source,
                    "words=\""
                            + dynasty.words()
                            + "\""
            );
        }

        if (!dynasty.sourceNote()
                .isBlank()) {

            send(
                    source,
                    "note="
                            + dynasty.sourceNote()
            );
        }

        return 1;
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
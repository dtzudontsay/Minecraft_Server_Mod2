package dev.dtzudontsay.knownworld.simulation.social.integrity;

import java.util.List;
import java.util.Objects;

public record PoliticalStructureIntegrityReport(
        int holdingCount,
        int dynastyCount,
        int allegianceOverrideCount,
        List<Issue> issues
) {

    public PoliticalStructureIntegrityReport {

        if (holdingCount < 0
                || dynastyCount < 0
                || allegianceOverrideCount < 0) {

            throw new IllegalArgumentException(
                    "Integrity report counts cannot be negative"
            );
        }

        issues =
                List.copyOf(
                        Objects.requireNonNull(
                                issues,
                                "issues"
                        )
                );
    }

    public long errorCount() {

        return count(
                Severity.ERROR
        );
    }

    public long warningCount() {

        return count(
                Severity.WARNING
        );
    }

    public long infoCount() {

        return count(
                Severity.INFO
        );
    }

    public boolean clean() {

        return errorCount() == 0;
    }

    public List<Issue> errors() {

        return issuesOf(
                Severity.ERROR
        );
    }

    public List<Issue> warnings() {

        return issuesOf(
                Severity.WARNING
        );
    }

    public List<Issue> info() {

        return issuesOf(
                Severity.INFO
        );
    }

    private long count(
            Severity severity
    ) {

        return issues.stream()
                .filter(
                        issue ->
                                issue.severity()
                                        == severity
                )
                .count();
    }

    private List<Issue> issuesOf(
            Severity severity
    ) {

        return issues.stream()
                .filter(
                        issue ->
                                issue.severity()
                                        == severity
                )
                .toList();
    }

    public enum Severity {

        ERROR,

        WARNING,

        INFO
    }

    public record Issue(
            Severity severity,
            String code,
            String subject,
            String message
    ) {

        public Issue {

            severity =
                    Objects.requireNonNull(
                            severity,
                            "severity"
                    );

            code =
                    requireText(
                            code,
                            "code"
                    );

            subject =
                    requireText(
                            subject,
                            "subject"
                    );

            message =
                    requireText(
                            message,
                            "message"
                    );
        }

        private static String requireText(
                String value,
                String description
        ) {

            Objects.requireNonNull(
                    value,
                    description
            );

            String trimmed =
                    value.trim();

            if (trimmed.isEmpty()) {

                throw new IllegalArgumentException(
                        description
                                + " cannot be blank"
                );
            }

            return trimmed;
        }
    }
}
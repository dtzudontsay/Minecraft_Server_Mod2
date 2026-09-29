package dev.dtzudontsay.knownworld.simulation.npc.profile;

import java.util.Objects;

public final class DialoguePersona {

    public static final DialoguePersona NEUTRAL =
            new DialoguePersona(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    "",
                    ""
            );

    private double formality;

    private double verbosity;

    private double warmth;

    private double directness;

    private String preferredAddress;

    private String guidance;

    public DialoguePersona(
            double formality,
            double verbosity,
            double warmth,
            double directness,
            String preferredAddress,
            String guidance
    ) {
        this.formality =
                clampSigned(
                        formality
                );

        this.verbosity =
                clampSigned(
                        verbosity
                );

        this.warmth =
                clampSigned(
                        warmth
                );

        this.directness =
                clampSigned(
                        directness
                );

        this.preferredAddress =
                normalizeText(
                        preferredAddress
                );

        this.guidance =
                normalizeText(
                        guidance
                );
    }

    public double formality() {
        return formality;
    }

    public double verbosity() {
        return verbosity;
    }

    public double warmth() {
        return warmth;
    }

    public double directness() {
        return directness;
    }

    public String preferredAddress() {
        return preferredAddress;
    }

    public String guidance() {
        return guidance;
    }

    public void setFormality(
            double formality
    ) {
        this.formality =
                clampSigned(
                        formality
                );
    }

    public void setVerbosity(
            double verbosity
    ) {
        this.verbosity =
                clampSigned(
                        verbosity
                );
    }

    public void setWarmth(
            double warmth
    ) {
        this.warmth =
                clampSigned(
                        warmth
                );
    }

    public void setDirectness(
            double directness
    ) {
        this.directness =
                clampSigned(
                        directness
                );
    }

    public void setPreferredAddress(
            String preferredAddress
    ) {
        this.preferredAddress =
                normalizeText(
                        preferredAddress
                );
    }

    public void setGuidance(
            String guidance
    ) {
        this.guidance =
                normalizeText(
                        guidance
                );
    }

    private static double clampSigned(
            double value
    ) {
        return Math.max(
                -1.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static String normalizeText(
            String value
    ) {
        return Objects.requireNonNullElse(
                value,
                ""
        ).trim();
    }
}
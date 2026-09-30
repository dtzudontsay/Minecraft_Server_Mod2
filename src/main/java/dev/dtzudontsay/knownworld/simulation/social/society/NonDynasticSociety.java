package dev.dtzudontsay.knownworld.simulation.social.society;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.Locale;
import java.util.Objects;

public final class NonDynasticSociety {

    private final OrganizationId organizationId;

    private final String authoredId;

    private final String name;

    private final NonDynasticSocietyType type;

    private NonDynasticSocietyStatus status;

    private final String homeLocationId;

    private final String cultureId;

    private final String religionId;

    private final boolean mobile;

    /**
     * -1 = unknown.
     */
    private int populationEstimate;

    private double militaryStrength;

    private double cohesion;

    private final ReferenceProvenance provenance;

    private final String sourceNote;

    private final String primaryLeaderRole;

    private final String secondaryLeaderRole;

    private NpcId primaryLeader;

    private NpcId secondaryLeader;

    public NonDynasticSociety(
            OrganizationId organizationId,
            String authoredId,
            String name,
            NonDynasticSocietyType type,
            NonDynasticSocietyStatus status,
            String homeLocationId,
            String cultureId,
            String religionId,
            boolean mobile,
            int populationEstimate,
            double militaryStrength,
            double cohesion,
            ReferenceProvenance provenance,
            String sourceNote,
            String primaryLeaderRole,
            String secondaryLeaderRole,
            NpcId primaryLeader,
            NpcId secondaryLeader
    ) {

        this.organizationId =
                Objects.requireNonNull(
                        organizationId,
                        "organizationId"
                );

        this.authoredId =
                normalizeOptionalId(
                        authoredId
                );

        this.name =
                requireText(
                        name,
                        "name"
                );

        this.type =
                Objects.requireNonNull(
                        type,
                        "type"
                );

        this.status =
                Objects.requireNonNull(
                        status,
                        "status"
                );

        this.homeLocationId =
                normalizeOptionalId(
                        homeLocationId
                );

        this.cultureId =
                normalizeOptionalId(
                        cultureId
                );

        this.religionId =
                normalizeOptionalId(
                        religionId
                );

        this.mobile =
                mobile;

        this.populationEstimate =
                populationEstimate < 0
                        ? -1
                        : populationEstimate;

        this.militaryStrength =
                clamp01(
                        militaryStrength
                );

        this.cohesion =
                clamp01(
                        cohesion
                );

        this.provenance =
                Objects.requireNonNull(
                        provenance,
                        "provenance"
                );

        this.sourceNote =
                sourceNote == null
                        ? ""
                        : sourceNote.trim();

        this.primaryLeaderRole =
                normalizeOptionalId(
                        primaryLeaderRole
                );

        this.secondaryLeaderRole =
                normalizeOptionalId(
                        secondaryLeaderRole
                );

        this.primaryLeader =
                primaryLeader;

        this.secondaryLeader =
                secondaryLeader;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public String authoredId() {
        return authoredId;
    }

    public boolean hasAuthoredId() {
        return authoredId != null;
    }

    public String name() {
        return name;
    }

    public NonDynasticSocietyType type() {
        return type;
    }

    public NonDynasticSocietyStatus status() {
        return status;
    }

    public void setStatus(
            NonDynasticSocietyStatus status
    ) {

        this.status =
                Objects.requireNonNull(
                        status,
                        "status"
                );
    }

    public String homeLocationId() {
        return homeLocationId;
    }

    public String cultureId() {
        return cultureId;
    }

    public String religionId() {
        return religionId;
    }

    public boolean mobile() {
        return mobile;
    }

    public int populationEstimate() {
        return populationEstimate;
    }

    public boolean hasPopulationEstimate() {
        return populationEstimate >= 0;
    }

    public void setPopulationEstimate(
            int populationEstimate
    ) {

        this.populationEstimate =
                populationEstimate < 0
                        ? -1
                        : populationEstimate;
    }

    public double militaryStrength() {
        return militaryStrength;
    }

    public void setMilitaryStrength(
            double militaryStrength
    ) {

        this.militaryStrength =
                clamp01(
                        militaryStrength
                );
    }

    public double cohesion() {
        return cohesion;
    }

    public void setCohesion(
            double cohesion
    ) {

        this.cohesion =
                clamp01(
                        cohesion
                );
    }

    public ReferenceProvenance provenance() {
        return provenance;
    }

    public String sourceNote() {
        return sourceNote;
    }

    public String primaryLeaderRole() {
        return primaryLeaderRole;
    }

    public String secondaryLeaderRole() {
        return secondaryLeaderRole;
    }

    public NpcId primaryLeader() {
        return primaryLeader;
    }

    public void setPrimaryLeader(
            NpcId primaryLeader
    ) {

        this.primaryLeader =
                primaryLeader;
    }

    public NpcId secondaryLeader() {
        return secondaryLeader;
    }

    public void setSecondaryLeader(
            NpcId secondaryLeader
    ) {

        this.secondaryLeader =
                secondaryLeader;
    }

    public boolean isOperational() {

        return status == NonDynasticSocietyStatus.ACTIVE
                || status == NonDynasticSocietyStatus.FORMING;
    }

    private static String requireText(
            String value,
            String description
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be empty"
            );
        }

        return value.trim();
    }

    private static String normalizeOptionalId(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!normalized.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid society/reference ID "
                            + value
            );
        }

        return normalized;
    }

    private static double clamp01(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
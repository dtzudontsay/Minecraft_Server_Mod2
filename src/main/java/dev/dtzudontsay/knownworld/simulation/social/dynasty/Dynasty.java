package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.Locale;
import java.util.Objects;

public final class Dynasty {

    private final DynastyId id;

    private final String authoredId;

    private final String name;

    private final DynastyType type;

    private DynastyStatus status;

    private final OrganizationId organizationId;

    private final String homeLocationId;

    private final String cultureId;

    private final String religionId;

    private DynastyId parentDynasty;

    private DynastyId liegeDynasty;

    private NpcId head;

    private NpcId heir;

    private final String words;

    private final String heraldry;

    private double prestige;

    private double wealth;

    private double militaryStrength;

    private final ReferenceProvenance provenance;

    private final String sourceNote;

    public Dynasty(
            DynastyId id,
            String authoredId,
            String name,
            DynastyType type,
            DynastyStatus status,
            OrganizationId organizationId,
            String homeLocationId,
            String cultureId,
            String religionId,
            DynastyId parentDynasty,
            DynastyId liegeDynasty,
            NpcId head,
            NpcId heir,
            String words,
            String heraldry,
            double prestige,
            double wealth,
            double militaryStrength,
            ReferenceProvenance provenance,
            String sourceNote
    ) {

        this.id =
                Objects.requireNonNull(
                        id,
                        "id"
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

        this.organizationId =
                Objects.requireNonNull(
                        organizationId,
                        "organizationId"
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

        this.parentDynasty =
                parentDynasty;

        this.liegeDynasty =
                liegeDynasty;

        this.head =
                head;

        this.heir =
                heir;

        this.words =
                optionalText(
                        words
                );

        this.heraldry =
                optionalText(
                        heraldry
                );

        this.prestige =
                clamp01(
                        prestige
                );

        this.wealth =
                clamp01(
                        wealth
                );

        this.militaryStrength =
                clamp01(
                        militaryStrength
                );

        this.provenance =
                Objects.requireNonNull(
                        provenance,
                        "provenance"
                );

        this.sourceNote =
                optionalText(
                        sourceNote
                );
    }

    public DynastyId id() {
        return id;
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

    public DynastyType type() {
        return type;
    }

    public DynastyStatus status() {
        return status;
    }

    public void setStatus(
            DynastyStatus status
    ) {

        this.status =
                Objects.requireNonNull(
                        status,
                        "status"
                );
    }

    public OrganizationId organizationId() {
        return organizationId;
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

    public DynastyId parentDynasty() {
        return parentDynasty;
    }

    public void setParentDynasty(
            DynastyId parentDynasty
    ) {

        if (id.equals(
                parentDynasty
        )) {

            throw new IllegalArgumentException(
                    "Dynasty cannot be its own parent"
            );
        }

        this.parentDynasty =
                parentDynasty;
    }

    public DynastyId liegeDynasty() {
        return liegeDynasty;
    }

    public void setLiegeDynasty(
            DynastyId liegeDynasty
    ) {

        if (id.equals(
                liegeDynasty
        )) {

            throw new IllegalArgumentException(
                    "Dynasty cannot be its own liege"
            );
        }

        this.liegeDynasty =
                liegeDynasty;
    }

    public NpcId head() {
        return head;
    }

    public void setHead(
            NpcId head
    ) {

        this.head =
                head;
    }

    public NpcId heir() {
        return heir;
    }

    public void setHeir(
            NpcId heir
    ) {

        this.heir =
                heir;
    }

    public String words() {
        return words;
    }

    public String heraldry() {
        return heraldry;
    }

    public double prestige() {
        return prestige;
    }

    public void setPrestige(
            double prestige
    ) {

        this.prestige =
                clamp01(
                        prestige
                );
    }

    public double wealth() {
        return wealth;
    }

    public void setWealth(
            double wealth
    ) {

        this.wealth =
                clamp01(
                        wealth
                );
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

    public ReferenceProvenance provenance() {
        return provenance;
    }

    public String sourceNote() {
        return sourceNote;
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
                    "Invalid dynasty/reference ID: "
                            + value
            );
        }

        return normalized;
    }

    private static String requireText(
            String value,
            String name
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    name
                            + " cannot be empty"
            );
        }

        return value.trim();
    }

    private static String optionalText(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
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
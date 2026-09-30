package dev.dtzudontsay.knownworld.simulation.social.holding;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;

import java.util.Locale;
import java.util.Objects;

public final class LandedHolding {

    private final HoldingId id;

    private final String authoredId;

    private final String name;

    private final HoldingType type;

    /*
     * Static geographical identity.
     */
    private final String worldLocationId;

    /*
     * Long-term/de-jure political association.
     *
     * This may differ from the current owner after conquest,
     * occupation, rebellion, usurpation, etc.
     */
    private final DynastyId deJureDynastyId;

    /*
     * Territorial hierarchy.
     *
     * Example:
     *
     * The North Domain
     *   -> Winterfell Domain
     */
    private HoldingId parentHoldingId;

    /*
     * Runtime state.
     */
    private HoldingStatus status;

    private DynastyId ownerDynastyId;

    private NpcId holderNpcId;

    private OrganizationId governmentOrganizationId;

    private TitleId linkedTitleId;

    /*
     * A capital/seat within its immediate domain.
     */
    private boolean capital;

    /*
     * Abstract baseline values.
     *
     * These are NOT the final economy.
     * They are hooks for the future economy/population simulation.
     */
    private double taxBase;

    private double militaryValue;

    private double populationWeight;

    public LandedHolding(
            HoldingId id,
            String authoredId,
            String name,
            HoldingType type,
            String worldLocationId,
            DynastyId deJureDynastyId,
            HoldingId parentHoldingId,
            HoldingStatus status,
            DynastyId ownerDynastyId,
            NpcId holderNpcId,
            OrganizationId governmentOrganizationId,
            TitleId linkedTitleId,
            boolean capital,
            double taxBase,
            double militaryValue,
            double populationWeight
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

        this.worldLocationId =
                requireId(
                        worldLocationId,
                        "worldLocationId"
                );

        this.deJureDynastyId =
                deJureDynastyId;

        this.parentHoldingId =
                parentHoldingId;

        this.status =
                Objects.requireNonNull(
                        status,
                        "status"
                );

        this.ownerDynastyId =
                ownerDynastyId;

        this.holderNpcId =
                holderNpcId;

        this.governmentOrganizationId =
                governmentOrganizationId;

        this.linkedTitleId =
                linkedTitleId;

        this.capital =
                capital;

        setTaxBase(
                taxBase
        );

        setMilitaryValue(
                militaryValue
        );

        setPopulationWeight(
                populationWeight
        );
    }

    public HoldingId id() {
        return id;
    }

    public String authoredId() {
        return authoredId;
    }

    public String name() {
        return name;
    }

    public HoldingType type() {
        return type;
    }

    public String worldLocationId() {
        return worldLocationId;
    }

    public DynastyId deJureDynastyId() {
        return deJureDynastyId;
    }

    public HoldingId parentHoldingId() {
        return parentHoldingId;
    }

    public void setParentHoldingId(
            HoldingId parentHoldingId
    ) {

        if (id.equals(
                parentHoldingId
        )) {

            throw new IllegalArgumentException(
                    "Holding cannot be its own parent"
            );
        }

        this.parentHoldingId =
                parentHoldingId;
    }

    public HoldingStatus status() {
        return status;
    }

    public void setStatus(
            HoldingStatus status
    ) {

        this.status =
                Objects.requireNonNull(
                        status,
                        "status"
                );
    }

    public DynastyId ownerDynastyId() {
        return ownerDynastyId;
    }

    public void setOwnerDynastyId(
            DynastyId ownerDynastyId
    ) {

        this.ownerDynastyId =
                ownerDynastyId;
    }

    public NpcId holderNpcId() {
        return holderNpcId;
    }

    public void setHolderNpcId(
            NpcId holderNpcId
    ) {

        this.holderNpcId =
                holderNpcId;
    }

    public OrganizationId governmentOrganizationId() {
        return governmentOrganizationId;
    }

    public void setGovernmentOrganizationId(
            OrganizationId governmentOrganizationId
    ) {

        this.governmentOrganizationId =
                governmentOrganizationId;
    }

    public TitleId linkedTitleId() {
        return linkedTitleId;
    }

    public void setLinkedTitleId(
            TitleId linkedTitleId
    ) {

        this.linkedTitleId =
                linkedTitleId;
    }

    public boolean capital() {
        return capital;
    }

    public void setCapital(
            boolean capital
    ) {

        this.capital =
                capital;
    }

    public double taxBase() {
        return taxBase;
    }

    public void setTaxBase(
            double taxBase
    ) {

        this.taxBase =
                requireUnitValue(
                        taxBase,
                        "taxBase"
                );
    }

    public double militaryValue() {
        return militaryValue;
    }

    public void setMilitaryValue(
            double militaryValue
    ) {

        this.militaryValue =
                requireUnitValue(
                        militaryValue,
                        "militaryValue"
                );
    }

    public double populationWeight() {
        return populationWeight;
    }

    public void setPopulationWeight(
            double populationWeight
    ) {

        this.populationWeight =
                requireUnitValue(
                        populationWeight,
                        "populationWeight"
                );
    }

    private static double requireUnitValue(
            double value,
            String description
    ) {

        if (!Double.isFinite(
                value
        )
                || value < 0.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    description
                            + " must be between 0 and 1"
            );
        }

        return value;
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

    private static String requireId(
            String value,
            String description
    ) {

        String normalized =
                requireText(
                        value,
                        description
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!normalized.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid "
                            + description
                            + ": "
                            + value
            );
        }

        return normalized;
    }

    private static String normalizeOptionalId(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return requireId(
                value,
                "authoredId"
        );
    }
}
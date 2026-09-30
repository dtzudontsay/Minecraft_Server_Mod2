package dev.dtzudontsay.knownworld.simulation.social.identity;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;

import java.util.Objects;

public final class CharacterSocialIdentity {

    private final NpcId npc;

    /*
     * ---------------------------------------------------------
     * DYNASTIC IDENTITY
     * ---------------------------------------------------------
     *
     * These are intentionally separate.
     *
     * Example:
     *
     * Catelyn:
     * birth       = House Tully
     * current     = House Stark
     * marriedInto = House Stark
     *
     * Jon Snow:
     * birth       = unknown at scenario start
     * current     = none
     * house org   = House Stark social sphere
     */

    private DynastyId birthDynasty;

    private DynastyId currentDynasty;

    private DynastyId marriedIntoDynasty;

    /**
     * Dynasty legally/socially treated as the character's family.
     *
     * This may differ from biological ancestry and birth dynasty.
     */
    private DynastyId legalFamilyDynasty;

    /*
     * ---------------------------------------------------------
     * LEGAL / SOCIAL PARENTHOOD
     * ---------------------------------------------------------
     *
     * Biological parentage remains permanently stored in
     * GenealogyManager.
     *
     * These fields therefore represent legal/social parenthood.
     */
    private NpcId legalMother;

    private NpcId legalFather;

    /*
     * ---------------------------------------------------------
     * PRIMARY SOCIAL PLACEMENT
     * ---------------------------------------------------------
     */

    private OrganizationId householdOrganization;

    /**
     * House/dynastic organization with which the character is socially
     * associated.
     *
     * This is deliberately separate from currentDynasty.
     *
     * Example:
     * Jon Snow can live within House Stark's social organization
     * without legally being a Stark dynast.
     */
    private OrganizationId houseOrganization;

    /**
     * Primary political allegiance.
     *
     * Other political memberships remain in the generic
     * OrganizationMembershipManager.
     */
    private OrganizationId primaryAllegianceOrganization;

    private double allegianceStrength;

    public CharacterSocialIdentity(
            NpcId npc
    ) {

        this(
                npc,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0.0
        );
    }

    public CharacterSocialIdentity(
            NpcId npc,
            DynastyId birthDynasty,
            DynastyId currentDynasty,
            DynastyId marriedIntoDynasty,
            DynastyId legalFamilyDynasty,
            NpcId legalMother,
            NpcId legalFather,
            OrganizationId householdOrganization,
            OrganizationId houseOrganization,
            OrganizationId primaryAllegianceOrganization,
            double allegianceStrength
    ) {

        this.npc =
                Objects.requireNonNull(
                        npc,
                        "npc"
                );

        this.birthDynasty =
                birthDynasty;

        this.currentDynasty =
                currentDynasty;

        this.marriedIntoDynasty =
                marriedIntoDynasty;

        this.legalFamilyDynasty =
                legalFamilyDynasty;

        this.legalMother =
                legalMother;

        this.legalFather =
                legalFather;

        this.householdOrganization =
                householdOrganization;

        this.houseOrganization =
                houseOrganization;

        this.primaryAllegianceOrganization =
                primaryAllegianceOrganization;

        setAllegianceStrength(
                allegianceStrength
        );
    }

    public NpcId npc() {
        return npc;
    }

    public DynastyId birthDynasty() {
        return birthDynasty;
    }

    public void setBirthDynasty(
            DynastyId birthDynasty
    ) {

        this.birthDynasty =
                birthDynasty;
    }

    public DynastyId currentDynasty() {
        return currentDynasty;
    }

    public void setCurrentDynasty(
            DynastyId currentDynasty
    ) {

        this.currentDynasty =
                currentDynasty;
    }

    public DynastyId marriedIntoDynasty() {
        return marriedIntoDynasty;
    }

    public void setMarriedIntoDynasty(
            DynastyId marriedIntoDynasty
    ) {

        this.marriedIntoDynasty =
                marriedIntoDynasty;
    }

    public DynastyId legalFamilyDynasty() {
        return legalFamilyDynasty;
    }

    public void setLegalFamilyDynasty(
            DynastyId legalFamilyDynasty
    ) {

        this.legalFamilyDynasty =
                legalFamilyDynasty;
    }

    public NpcId legalMother() {
        return legalMother;
    }

    public void setLegalMother(
            NpcId legalMother
    ) {

        this.legalMother =
                legalMother;
    }

    public NpcId legalFather() {
        return legalFather;
    }

    public void setLegalFather(
            NpcId legalFather
    ) {

        this.legalFather =
                legalFather;
    }

    public OrganizationId householdOrganization() {
        return householdOrganization;
    }

    public void setHouseholdOrganization(
            OrganizationId householdOrganization
    ) {

        this.householdOrganization =
                householdOrganization;
    }

    public OrganizationId houseOrganization() {
        return houseOrganization;
    }

    public void setHouseOrganization(
            OrganizationId houseOrganization
    ) {

        this.houseOrganization =
                houseOrganization;
    }

    public OrganizationId primaryAllegianceOrganization() {
        return primaryAllegianceOrganization;
    }

    public void setPrimaryAllegianceOrganization(
            OrganizationId primaryAllegianceOrganization
    ) {

        this.primaryAllegianceOrganization =
                primaryAllegianceOrganization;
    }

    public double allegianceStrength() {
        return allegianceStrength;
    }

    public void setAllegianceStrength(
            double allegianceStrength
    ) {

        if (!Double.isFinite(
                allegianceStrength
        )) {

            throw new IllegalArgumentException(
                    "allegianceStrength must be finite"
            );
        }

        this.allegianceStrength =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                allegianceStrength
                        )
                );
    }
}
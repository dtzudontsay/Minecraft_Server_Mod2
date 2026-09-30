package dev.dtzudontsay.knownworld.simulation.social.dynasty;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class Dynasty {

    private final DynastyId id;

    private final String authoredId;

    private final String name;

    private final DynastyType type;

    private DynastyStatus status;

    /**
     * Optional.
     *
     * Historical/extinct dynasties do not need to generate useless
     * runtime organizations.
     */
    private final OrganizationId organizationId;

    private final String homeLocationId;

    private final String cultureId;

    private final String religionId;

    /**
     * Genealogical/cadet parent.
     */
    private DynastyId parentDynasty;

    /**
     * Political/feudal liege at the scenario start.
     */
    private DynastyId liegeDynasty;

    /**
     * Historical predecessor/successor links.
     */
    private DynastyId predecessorDynasty;

    private DynastyId successorDynasty;

    private NpcId head;

    private NpcId heir;

    /**
     * AC year when known.
     *
     * Null means unknown or inappropriate to express as a precise year.
     */
    private final Integer foundedYear;

    /**
     * AC year of extinction when known.
     */
    private final Integer extinctYear;

    /**
     * Whether this lineage exists as an active social/political entity
     * at the scenario start.
     *
     * Exiled dynasties may still be active.
     */
    private final boolean activeAtScenarioStart;

    private final Set<DynastyContinuity> continuities =
            new LinkedHashSet<>();

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
            DynastyId predecessorDynasty,
            DynastyId successorDynasty,
            NpcId head,
            NpcId heir,
            Integer foundedYear,
            Integer extinctYear,
            boolean activeAtScenarioStart,
            Set<DynastyContinuity> continuities,
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
                organizationId;

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

        this.predecessorDynasty =
                predecessorDynasty;

        this.successorDynasty =
                successorDynasty;

        this.head =
                head;

        this.heir =
                heir;

        this.foundedYear =
                foundedYear;

        this.extinctYear =
                extinctYear;

        this.activeAtScenarioStart =
                activeAtScenarioStart;

        if (continuities != null) {

            this.continuities.addAll(
                    continuities
            );
        }

        if (this.continuities.isEmpty()) {

            throw new IllegalArgumentException(
                    "Dynasty must belong to at least one continuity"
            );
        }

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

        validateTemporalState();
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

    public boolean hasOrganization() {
        return organizationId != null;
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

        validateNotSelf(
                parentDynasty,
                "parent"
        );

        this.parentDynasty =
                parentDynasty;
    }

    public DynastyId liegeDynasty() {
        return liegeDynasty;
    }

    public void setLiegeDynasty(
            DynastyId liegeDynasty
    ) {

        validateNotSelf(
                liegeDynasty,
                "liege"
        );

        this.liegeDynasty =
                liegeDynasty;
    }

    public DynastyId predecessorDynasty() {
        return predecessorDynasty;
    }

    public void setPredecessorDynasty(
            DynastyId predecessorDynasty
    ) {

        validateNotSelf(
                predecessorDynasty,
                "predecessor"
        );

        this.predecessorDynasty =
                predecessorDynasty;
    }

    public DynastyId successorDynasty() {
        return successorDynasty;
    }

    public void setSuccessorDynasty(
            DynastyId successorDynasty
    ) {

        validateNotSelf(
                successorDynasty,
                "successor"
        );

        this.successorDynasty =
                successorDynasty;
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

    public Integer foundedYear() {
        return foundedYear;
    }

    public Integer extinctYear() {
        return extinctYear;
    }

    public boolean activeAtScenarioStart() {
        return activeAtScenarioStart;
    }

    public Set<DynastyContinuity> continuities() {

        return Set.copyOf(
                continuities
        );
    }

    public boolean existsIn(
            DynastyContinuity continuity
    ) {

        return continuities.contains(
                continuity
        );
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

    private void validateTemporalState() {

        if (foundedYear != null
                && extinctYear != null
                && extinctYear < foundedYear) {

            throw new IllegalArgumentException(
                    "Dynasty extinction year cannot precede founding year"
            );
        }

        if (status == DynastyStatus.EXTINCT
                && activeAtScenarioStart) {

            throw new IllegalArgumentException(
                    "Extinct dynasty cannot be active at scenario start"
            );
        }
    }

    private void validateNotSelf(
            DynastyId other,
            String relationship
    ) {

        if (other != null
                && id.equals(
                other
        )) {

            throw new IllegalArgumentException(
                    "Dynasty cannot be its own "
                            + relationship
            );
        }
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
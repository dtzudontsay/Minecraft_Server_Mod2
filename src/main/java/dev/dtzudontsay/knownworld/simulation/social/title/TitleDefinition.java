package dev.dtzudontsay.knownworld.simulation.social.title;

import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Objects;

/**
 * Persistent definition of a title or office.
 *
 * Examples:
 *
 * - King of the Seven Kingdoms
 * - Hand of the King
 * - Head of House Stark
 * - Lord of Winterfell
 * - Warden of the North
 *
 * A title may be scoped to:
 *
 * - an organization
 * - a settlement
 * - both
 * - neither
 *
 * Authority ranges from 0 to 100.
 *
 * Exclusive titles permit only one active holder at a time.
 */
public final class TitleDefinition {

    private final TitleId id;

    private final String name;

    private final TitleType type;

    private final int authority;

    private final boolean exclusive;

    private final OrganizationId organization;

    private final SettlementId settlement;

    public TitleDefinition(
            TitleId id,
            String name,
            TitleType type,
            int authority,
            boolean exclusive,
            OrganizationId organization,
            SettlementId settlement
    ) {
        this.id =
                Objects.requireNonNull(
                        id,
                        "id"
                );

        Objects.requireNonNull(
                name,
                "name"
        );

        this.name =
                name.trim();

        if (this.name.isEmpty()) {
            throw new IllegalArgumentException(
                    "Title name cannot be empty"
            );
        }

        this.type =
                Objects.requireNonNull(
                        type,
                        "type"
                );

        if (authority < 0
                || authority > 100) {

            throw new IllegalArgumentException(
                    "Title authority must be between 0 and 100"
            );
        }

        this.authority =
                authority;

        this.exclusive =
                exclusive;

        this.organization =
                organization;

        this.settlement =
                settlement;
    }

    public TitleId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TitleType type() {
        return type;
    }

    public int authority() {
        return authority;
    }

    public boolean exclusive() {
        return exclusive;
    }

    public OrganizationId organization() {
        return organization;
    }

    public SettlementId settlement() {
        return settlement;
    }
}
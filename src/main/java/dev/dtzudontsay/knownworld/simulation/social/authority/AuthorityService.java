package dev.dtzudontsay.knownworld.simulation.social.authority;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleDefinition;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleManager;

import java.util.Objects;

/**
 * Resolves formal authority derived from titles.
 *
 * This does NOT yet include:
 *
 * - personal loyalty
 * - military chain of command
 * - rebellion
 * - legitimacy
 * - disputed claims
 * - temporary delegated authority
 *
 * Those systems will build on top of this one.
 */
public final class AuthorityService {

    private final OrganizationManager organizations;

    private final TitleManager titles;

    public AuthorityService(
            OrganizationManager organizations,
            TitleManager titles
    ) {
        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );

        this.titles =
                Objects.requireNonNull(
                        titles,
                        "titles"
                );
    }

    /**
     * Highest formal authority score this NPC possesses inside the
     * specified organization.
     */
    public int authorityIn(
            NpcId npc,
            OrganizationId organization
    ) {
        Objects.requireNonNull(
                npc,
                "npc"
        );

        Objects.requireNonNull(
                organization,
                "organization"
        );

        if (organizations.find(
                organization
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown organization ID: "
                            + organization
            );
        }

        return titles.activeTitlesOf(
                        npc
                )
                .stream()
                .filter(
                        title ->
                                organization.equals(
                                        title.organization()
                                )
                )
                .mapToInt(
                        TitleDefinition::authority
                )
                .max()
                .orElse(
                        0
                );
    }

    /**
     * Returns true only when issuer has formal title-derived authority
     * in the organization and strictly outranks the target.
     */
    public boolean formallyOutranks(
            NpcId issuer,
            NpcId target,
            OrganizationId organization
    ) {
        int issuerAuthority =
                authorityIn(
                        issuer,
                        organization
                );

        if (issuerAuthority <= 0) {
            return false;
        }

        int targetAuthority =
                authorityIn(
                        target,
                        organization
                );

        return issuerAuthority
                > targetAuthority;
    }
}
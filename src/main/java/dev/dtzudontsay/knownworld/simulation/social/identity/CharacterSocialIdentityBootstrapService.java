package dev.dtzudontsay.knownworld.simulation.social.identity;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.bootstrap.PopulationPackCatalog;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioBootstrapper;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.Dynasty;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;
import dev.dtzudontsay.knownworld.simulation.social.membership.OrganizationMembershipManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CharacterSocialIdentityBootstrapService {

    private static final Gson GSON =
            new Gson();

    private CharacterSocialIdentityBootstrapService() {
    }

    public static void ensureDefault(
            NpcSimulation simulation,
            CharacterSocialIdentityService service,
            CharacterSocialIdentityManager identities,
            DynastyManager dynasties,
            OrganizationMembershipManager memberships
    ) throws IOException {

        ScenarioIndex scenario =
                readJson(
                        ScenarioBootstrapper.DEFAULT_SCENARIO,
                        ScenarioIndex.class
                );

        PopulationPackCatalog.Plan plan =
                PopulationPackCatalog.load(
                        ScenarioBootstrapper.DEFAULT_SCENARIO,
                        scenario.populationPacks
                );

        List<String> resources =
                plan.socialIdentityResources();

        Set<String> appliedNpcIds =
                new LinkedHashSet<>();

        int applied =
                0;

        for (
                String resource :
                resources
        ) {

            RootData root =
                    readJson(
                            resource,
                            RootData.class
                    );

            if (root.characters == null) {
                continue;
            }

            for (
                    CharacterData data :
                    root.characters
            ) {

                if (data == null
                        || !hasText(
                        data.npcId
                )) {

                    continue;
                }

                String normalizedNpcId =
                        data.npcId.trim()
                                .toLowerCase();

                if (!appliedNpcIds.add(
                        normalizedNpcId
                )) {

                    throw new IllegalStateException(
                            "Duplicate character social identity overlay for "
                                    + normalizedNpcId
                    );
                }

                apply(
                        simulation,
                        service,
                        identities,
                        dynasties,
                        memberships,
                        data
                );

                applied++;
            }
        }

        service.ensureAll();

        KnownWorld.LOGGER.info(
                "Character social identity bootstrap applied {} authored identity overlays from {} population-pack resources; {} total identities active.",
                applied,
                resources.size(),
                identities.size()
        );
    }

    private static void apply(
            NpcSimulation simulation,
            CharacterSocialIdentityService service,
            CharacterSocialIdentityManager identities,
            DynastyManager dynasties,
            OrganizationMembershipManager memberships,
            CharacterData data
    ) {

        NpcId npc =
                simulation.authoredIds()
                        .requireNpc(
                                data.npcId
                        );

        service.ensureIdentity(
                npc
        );

        if (data.clearBirthDynasty) {

            identities.setBirthDynasty(
                    npc,
                    null
            );

        } else if (hasText(
                data.birthDynastyId
        )) {

            identities.setBirthDynasty(
                    npc,
                    requireDynasty(
                            dynasties,
                            data.birthDynastyId
                    )
            );
        }

        if (data.clearCurrentDynasty) {

            identities.setCurrentDynasty(
                    npc,
                    null
            );

        } else if (hasText(
                data.currentDynastyId
        )) {

            identities.setCurrentDynasty(
                    npc,
                    requireDynasty(
                            dynasties,
                            data.currentDynastyId
                    )
            );
        }

        if (data.clearMarriedIntoDynasty) {

            identities.setMarriedIntoDynasty(
                    npc,
                    null
            );

        } else if (hasText(
                data.marriedIntoDynastyId
        )) {

            identities.setMarriedIntoDynasty(
                    npc,
                    requireDynasty(
                            dynasties,
                            data.marriedIntoDynastyId
                    )
            );
        }

        if (data.clearLegalFamilyDynasty) {

            identities.setLegalFamilyDynasty(
                    npc,
                    null
            );

        } else if (hasText(
                data.legalFamilyDynastyId
        )) {

            identities.setLegalFamilyDynasty(
                    npc,
                    requireDynasty(
                            dynasties,
                            data.legalFamilyDynastyId
                    )
            );
        }

        if (data.clearLegalMother) {

            identities.setLegalMother(
                    npc,
                    null
            );

        } else if (hasText(
                data.legalMotherNpcId
        )) {

            identities.setLegalMother(
                    npc,
                    simulation.authoredIds()
                            .requireNpc(
                                    data.legalMotherNpcId
                            )
            );
        }

        if (data.clearLegalFather) {

            identities.setLegalFather(
                    npc,
                    null
            );

        } else if (hasText(
                data.legalFatherNpcId
        )) {

            identities.setLegalFather(
                    npc,
                    simulation.authoredIds()
                            .requireNpc(
                                    data.legalFatherNpcId
                            )
            );
        }

        if (data.clearHouseholdOrganization) {

            identities.setHouseholdOrganization(
                    npc,
                    null
            );

        } else if (hasText(
                data.householdOrganizationId
        )) {

            identities.setHouseholdOrganization(
                    npc,
                    simulation.authoredIds()
                            .requireOrganization(
                                    data.householdOrganizationId
                            )
            );
        }

        if (data.clearHouseOrganization) {

            identities.setHouseOrganization(
                    npc,
                    null
            );

        } else if (hasText(
                data.houseOrganizationId
        )) {

            identities.setHouseOrganization(
                    npc,
                    simulation.authoredIds()
                            .requireOrganization(
                                    data.houseOrganizationId
                            )
            );
        }

        if (data.clearPrimaryAllegiance) {

            identities.setPrimaryAllegiance(
                    npc,
                    null,
                    0.0
            );

        } else if (hasText(
                data.primaryAllegianceOrganizationId
        )) {

            identities.setPrimaryAllegiance(
                    npc,
                    simulation.authoredIds()
                            .requireOrganization(
                                    data.primaryAllegianceOrganizationId
                            ),
                    data.allegianceStrength == null
                            ? 0.70
                            : data.allegianceStrength
            );
        }

        if (data.memberships != null) {

            for (
                    OrganizationMembershipData membership :
                    data.memberships
            ) {

                if (membership == null
                        || !hasText(
                        membership.organizationId
                )) {

                    continue;
                }

                OrganizationId organization =
                        simulation.authoredIds()
                                .requireOrganization(
                                        membership.organizationId
                                );

                memberships.join(
                        npc,
                        organization,
                        simulation.serverTickCounter(),
                        membership.loyalty == null
                                ? 0.70
                                : membership.loyalty
                );

                if (membership.roles != null) {

                    for (
                            String role :
                            membership.roles
                    ) {

                        if (!hasText(
                                role
                        )) {

                            continue;
                        }

                        memberships.addRole(
                                npc,
                                organization,
                                role
                        );
                    }
                }
            }
        }

        if (data.religiousMemberships != null) {

            for (
                    ReligiousMembershipData religious :
                    data.religiousMemberships
            ) {

                if (religious == null
                        || !hasText(
                        religious.orderId
                )) {

                    continue;
                }

                Organization organization =
                        simulation.religiousInstitutions()
                                .ensureOrganization(
                                        religious.orderId
                                );

                simulation.religiousMemberships()
                        .join(
                                npc,
                                religious.orderId,
                                organization.id(),
                                religious.roleId == null
                                        ? ""
                                        : religious.roleId,
                                religious.commitment == null
                                        ? 0.60
                                        : religious.commitment
                        );
            }
        }

        service.ensureIdentity(
                npc
        );
    }

    private static DynastyId requireDynasty(
            DynastyManager dynasties,
            String authoredId
    ) {

        return dynasties.findAuthored(
                        authoredId
                )
                .map(
                        Dynasty::id
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown authored dynasty "
                                                + authoredId
                                )
                );
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        CharacterSocialIdentityBootstrapService.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Character social identity resource not found: "
                                + resource
                );
            }

            try (
                    Reader reader =
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
            ) {

                T result =
                        GSON.fromJson(
                                reader,
                                type
                        );

                if (result == null) {

                    throw new IOException(
                            "Character social identity resource produced null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid character social identity JSON "
                            + resource,
                    exception
            );
        }
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static final class ScenarioIndex {

        String populationPacks;
    }

    private static final class RootData {

        List<CharacterData> characters;
    }

    private static final class CharacterData {

        String npcId;

        String birthDynastyId;

        String currentDynastyId;

        String marriedIntoDynastyId;

        String legalFamilyDynastyId;

        boolean clearBirthDynasty;

        boolean clearCurrentDynasty;

        boolean clearMarriedIntoDynasty;

        boolean clearLegalFamilyDynasty;

        String legalMotherNpcId;

        String legalFatherNpcId;

        boolean clearLegalMother;

        boolean clearLegalFather;

        String householdOrganizationId;

        String houseOrganizationId;

        boolean clearHouseholdOrganization;

        boolean clearHouseOrganization;

        String primaryAllegianceOrganizationId;

        boolean clearPrimaryAllegiance;

        Double allegianceStrength;

        List<OrganizationMembershipData> memberships;

        List<ReligiousMembershipData> religiousMemberships;
    }

    private static final class OrganizationMembershipData {

        String organizationId;

        Double loyalty;

        List<String> roles;
    }

    private static final class ReligiousMembershipData {

        String orderId;

        String roleId;

        Double commitment;
    }
}
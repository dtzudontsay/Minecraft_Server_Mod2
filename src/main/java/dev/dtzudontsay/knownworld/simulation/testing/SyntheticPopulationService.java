package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.LifeHistoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Development-only population generator.
 *
 * Synthetic NPCs are ordinary persistent runtime NPCs.
 *
 * Sandbox houses and households remain genuine persistent organizations.
 *
 * A shared faction provides the controlled wider social context through
 * which members of different noble houses can occasionally encounter one
 * another without permitting arbitrary world-wide random contact.
 */
public final class SyntheticPopulationService {

    public static final String SANDBOX_DIMENSION =
            "knownworld:simulation_sandbox";

    private static final int MIN_AGE =
            18;

    private static final int MAX_AGE =
            60;

    private static final int HOUSEHOLDS_PER_HOUSE =
            2;

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final LifeHistoryManager lifeHistory;

    private final OrganizationManager organizations;

    private final NpcAffiliationManager affiliations;

    public SyntheticPopulationService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
            LifeHistoryManager lifeHistory,
            OrganizationManager organizations,
            NpcAffiliationManager affiliations
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.profiles =
                Objects.requireNonNull(
                        profiles,
                        "profiles"
                );

        this.lifeHistory =
                Objects.requireNonNull(
                        lifeHistory,
                        "lifeHistory"
                );

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );

        this.affiliations =
                Objects.requireNonNull(
                        affiliations,
                        "affiliations"
                );
    }

    public List<NpcId> create(
            int count,
            int cohortCount,
            long seed,
            int currentYear,
            int daysPerYear
    ) {

        if (count < 2
                || count > 10_000) {

            throw new IllegalArgumentException(
                    "Synthetic population count must be between 2 and 10000"
            );
        }

        if (cohortCount < 1
                || cohortCount > count) {

            throw new IllegalArgumentException(
                    "cohortCount must be between 1 and population count"
            );
        }

        if (!sandboxNpcIds()
                .isEmpty()) {

            throw new IllegalStateException(
                    "This world already contains synthetic sandbox NPCs. "
                            + "Use a fresh disposable test world for another population."
            );
        }

        Random random =
                new Random(
                        seed
                );

        Organization sandboxFaction =
                organizations.create(
                        "Sandbox Realm",
                        OrganizationType.FACTION,
                        null
                );

        List<Organization> nobleHouses =
                new ArrayList<>(
                        cohortCount
                );

        List<List<Organization>> households =
                new ArrayList<>(
                        cohortCount
                );

        for (
                int cohort = 0;
                cohort < cohortCount;
                cohort++
        ) {

            Organization nobleHouse =
                    organizations.create(
                            String.format(
                                    "Sandbox House %02d",
                                    cohort + 1
                            ),
                            OrganizationType.NOBLE_HOUSE,
                            null
                    );

            nobleHouses.add(
                    nobleHouse
            );

            List<Organization> houseHouseholds =
                    new ArrayList<>(
                            HOUSEHOLDS_PER_HOUSE
                    );

            for (
                    int householdIndex = 0;
                    householdIndex < HOUSEHOLDS_PER_HOUSE;
                    householdIndex++
            ) {

                Organization household =
                        organizations.create(
                                String.format(
                                        "Sandbox House %02d Household %02d",
                                        cohort + 1,
                                        householdIndex + 1
                                ),
                                OrganizationType.HOUSEHOLD,
                                null
                        );

                houseHouseholds.add(
                        household
                );
            }

            households.add(
                    List.copyOf(
                            houseHouseholds
                    )
            );
        }

        List<NpcId> created =
                new ArrayList<>(
                        count
                );

        for (
                int index = 0;
                index < count;
                index++
        ) {

            int cohort =
                    index
                            % cohortCount;

            int householdIndex =
                    (
                            index
                                    / cohortCount
                    )
                            % HOUSEHOLDS_PER_HOUSE;

            int age =
                    MIN_AGE
                            +
                            random.nextInt(
                                    MAX_AGE
                                            - MIN_AGE
                                            + 1
                            );

            int birthYear =
                    currentYear
                            - age;

            NpcSex sex =
                    random.nextBoolean()
                            ? NpcSex.MALE
                            : NpcSex.FEMALE;

            NpcPersonality personality =
                    new NpcPersonality(
                            signed(
                                    random
                            ),
                            signed(
                                    random
                            ),
                            signed(
                                    random
                            ),
                            signed(
                                    random
                            ),
                            signed(
                                    random
                            ),
                            signed(
                                    random
                            )
                    );

            SimulationPosition position =
                    new SimulationPosition(
                            SANDBOX_DIMENSION,
                            cohort
                                    * 10_000.0
                                    +
                                    householdIndex
                                            * 1_000.0,
                            64.0,
                            0.0
                    );

            NpcState npc =
                    registry.create(
                            String.format(
                                    "Sim%04d",
                                    index + 1
                            ),
                            String.format(
                                    "SandboxHouse%02d",
                                    cohort + 1
                            ),
                            sex,
                            birthYear,
                            position,
                            personality
                    );

            CharacterProfile profile =
                    profiles.getOrCreate(
                            npc.id()
                    );

            initializeProfile(
                    profile,
                    random
            );

            lifeHistory.registerBirth(
                    npc.id(),
                    (
                            (long) birthYear
                    )
                            * daysPerYear
            );

            affiliations.setNobleHouse(
                    npc.id(),
                    nobleHouses.get(
                                    cohort
                            )
                            .id()
            );

            affiliations.setHousehold(
                    npc.id(),
                    households.get(
                                    cohort
                            )
                            .get(
                                    householdIndex
                            )
                            .id()
            );

            affiliations.setFaction(
                    npc.id(),
                    sandboxFaction.id()
            );

            /*
             * Give generated sandbox NPCs exactly the same society-side
             * identity initialization opportunity as naturally born NPCs.
             *
             * Sandbox noble-house organizations intentionally do not require
             * a Dynasty record. CharacterSocialIdentity therefore retains the
             * organization placement even when currentDynasty is null.
             */
            SocietyStructureRuntime society =
                    SocietyStructureRuntime.getNullable();

            if (society != null) {

                society.onNpcCreated(
                        npc.id()
                );
            }

            created.add(
                    npc.id()
            );
        }

        return List.copyOf(
                created
        );
    }

    public List<NpcId> sandboxNpcIds() {

        return registry.all()
                .stream()
                .filter(
                        npc ->
                                SANDBOX_DIMENSION.equals(
                                        npc.position()
                                                .dimension()
                                )
                )
                .map(
                        NpcState::id
                )
                .toList();
    }

    private static void initializeProfile(
            CharacterProfile profile,
            Random random
    ) {

        profile.setEducation(
                "sandbox"
        );

        profile.setWealth(
                signed(
                        random
                )
                        * 0.40
        );

        profile.setSocialStatus(
                signed(
                        random
                )
                        * 0.40
        );

        profile.setReputation(
                signed(
                        random
                )
                        * 0.20
        );

        for (
                CharacterAptitude aptitude :
                CharacterAptitude.values()
        ) {

            profile.setAptitude(
                    aptitude,
                    0.20
                            +
                            random.nextDouble()
                                    * 0.60
            );
        }

        for (
                CharacterDisposition disposition :
                CharacterDisposition.values()
        ) {

            profile.setDisposition(
                    disposition,
                    signed(
                            random
                    )
                            * 0.70
            );
        }

        for (
                CharacterValue value :
                CharacterValue.values()
        ) {

            profile.setCharacterValue(
                    value,
                    signed(
                            random
                    )
                            * 0.60
            );
        }

        for (
                CharacterSocialNorm norm :
                CharacterSocialNorm.values()
        ) {

            profile.setSocialNorm(
                    norm,
                    signed(
                            random
                    )
                            * 0.50
            );
        }
    }

    private static double signed(
            Random random
    ) {

        return random.nextDouble()
                * 2.0
                - 1.0;
    }
}
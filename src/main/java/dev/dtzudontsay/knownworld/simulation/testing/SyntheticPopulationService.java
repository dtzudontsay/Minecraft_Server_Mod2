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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class SyntheticPopulationService {

    public static final String SANDBOX_DIMENSION =
            "knownworld:simulation_sandbox";

    private static final int MIN_AGE =
            18;

    private static final int MAX_AGE =
            60;

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final LifeHistoryManager lifeHistory;

    public SyntheticPopulationService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
            LifeHistoryManager lifeHistory
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

            /*
             * Cohorts are deliberately placed 10 km apart.
             *
             * They will STILL socially interact because the abstract
             * simulator does not depend upon Minecraft block distance.
             *
             * This explicitly tests the architecture we need for the
             * continent-scale Known World.
             */
            SimulationPosition position =
                    new SimulationPosition(
                            SANDBOX_DIMENSION,
                            cohort
                                    * 10_000.0,
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
package dev.dtzudontsay.knownworld.simulation.npc.legal;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterLegalStatus;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CharacterLegalStateManager {

    private final NpcRegistry registry;

    private final Map<NpcId, CharacterLegalState> states =
            new LinkedHashMap<>();

    public CharacterLegalStateManager(
            NpcRegistry registry
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized CharacterLegalState getOrCreate(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        return states.computeIfAbsent(
                npc,
                CharacterLegalState::new
        );
    }

    public synchronized Optional<CharacterLegalState> find(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        return Optional.ofNullable(
                states.get(
                        npc
                )
        );
    }

    public synchronized void registerLoaded(
            CharacterLegalState state
    ) {

        Objects.requireNonNull(
                state,
                "state"
        );

        validateNpc(
                state.owner()
        );

        states.put(
                state.owner(),
                state
        );
    }

    /**
     * Ensures all NPCs have a legal state and migrates information from the
     * old CharacterProfile.legalStatus field without overwriting already
     * explicit multi-axis data.
     */
    public synchronized void ensureAll(
            CharacterProfileManager profiles
    ) {

        Objects.requireNonNull(
                profiles,
                "profiles"
        );

        for (
                var npc :
                registry.all()
        ) {

            CharacterLegalState state =
                    states.computeIfAbsent(
                            npc.id(),
                            CharacterLegalState::new
                    );

            CharacterProfile profile =
                    profiles.getOrCreate(
                            npc.id()
                    );

            applyLegacyIfMissing(
                    state,
                    profile.legalStatus()
            );
        }
    }

    public synchronized List<CharacterLegalState> all() {

        return List.copyOf(
                states.values()
        );
    }

    public synchronized int size() {

        return states.size();
    }

    private static void applyLegacyIfMissing(
            CharacterLegalState state,
            CharacterLegalStatus legacy
    ) {

        if (legacy == null
                || legacy == CharacterLegalStatus.UNSPECIFIED) {

            return;
        }

        switch (legacy) {

            case LEGITIMATE -> {

                if (state.birthStatus()
                        == CharacterBirthStatus.UNSPECIFIED) {

                    state.setBirthStatus(
                            CharacterBirthStatus.LEGITIMATE
                    );
                }
            }

            case ACKNOWLEDGED_BASTARD -> {

                if (state.birthStatus()
                        == CharacterBirthStatus.UNSPECIFIED) {

                    state.setBirthStatus(
                            CharacterBirthStatus.ACKNOWLEDGED_BASTARD
                    );
                }
            }

            case UNACKNOWLEDGED_BASTARD -> {

                if (state.birthStatus()
                        == CharacterBirthStatus.UNSPECIFIED) {

                    state.setBirthStatus(
                            CharacterBirthStatus.UNACKNOWLEDGED_BASTARD
                    );
                }
            }

            case LEGITIMIZED -> {

                if (state.birthStatus()
                        == CharacterBirthStatus.UNSPECIFIED) {

                    state.setBirthStatus(
                            CharacterBirthStatus.LEGITIMIZED
                    );
                }
            }

            case FREEBORN -> {

                if (state.freedomStatus()
                        == CharacterFreedomStatus.UNSPECIFIED) {

                    state.setFreedomStatus(
                            CharacterFreedomStatus.FREEBORN
                    );
                }
            }

            case FREE -> {

                if (state.freedomStatus()
                        == CharacterFreedomStatus.UNSPECIFIED) {

                    state.setFreedomStatus(
                            CharacterFreedomStatus.FREE
                    );
                }
            }

            case FREEDPERSON -> {

                if (state.freedomStatus()
                        == CharacterFreedomStatus.UNSPECIFIED) {

                    state.setFreedomStatus(
                            CharacterFreedomStatus.FREEDPERSON
                    );
                }
            }

            case ENSLAVED -> {

                if (state.freedomStatus()
                        == CharacterFreedomStatus.UNSPECIFIED) {

                    state.setFreedomStatus(
                            CharacterFreedomStatus.ENSLAVED
                    );
                }
            }

            case THRALL -> {

                if (state.freedomStatus()
                        == CharacterFreedomStatus.UNSPECIFIED) {

                    state.setFreedomStatus(
                            CharacterFreedomStatus.THRALL
                    );
                }
            }

            case WARD -> {

                if (state.custodyStatus()
                        == CharacterCustodyStatus.NONE) {

                    state.setCustodyStatus(
                            CharacterCustodyStatus.WARD
                    );
                }
            }

            case HOSTAGE -> {

                if (state.custodyStatus()
                        == CharacterCustodyStatus.NONE) {

                    state.setCustodyStatus(
                            CharacterCustodyStatus.HOSTAGE
                    );
                }
            }

            case PRISONER -> {

                if (state.custodyStatus()
                        == CharacterCustodyStatus.NONE) {

                    state.setCustodyStatus(
                            CharacterCustodyStatus.PRISONER
                    );
                }
            }

            case DISINHERITED -> {

                if (state.civilStatus()
                        == CharacterCivilStatus.NORMAL) {

                    state.setCivilStatus(
                            CharacterCivilStatus.DISINHERITED
                    );
                }
            }

            case OUTLAW -> {

                if (state.civilStatus()
                        == CharacterCivilStatus.NORMAL) {

                    state.setCivilStatus(
                            CharacterCivilStatus.OUTLAW
                    );
                }
            }

            case EXILE -> {

                if (state.civilStatus()
                        == CharacterCivilStatus.NORMAL) {

                    state.setCivilStatus(
                            CharacterCivilStatus.EXILE
                    );
                }
            }

            case CONDEMNED -> {

                if (state.civilStatus()
                        == CharacterCivilStatus.NORMAL) {

                    state.setCivilStatus(
                            CharacterCivilStatus.CONDEMNED
                    );
                }
            }

            case UNSPECIFIED -> {
            }
        }
    }

    private void validateNpc(
            NpcId npc
    ) {

        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (!registry.contains(
                npc
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC "
                            + npc
            );
        }
    }
}
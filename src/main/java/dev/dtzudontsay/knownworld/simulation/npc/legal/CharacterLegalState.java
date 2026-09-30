package dev.dtzudontsay.knownworld.simulation.npc.legal;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;

public final class CharacterLegalState {

    private final NpcId owner;

    private CharacterBirthStatus birthStatus;

    private CharacterFreedomStatus freedomStatus;

    private CharacterCustodyStatus custodyStatus;

    private CharacterCivilStatus civilStatus;

    public CharacterLegalState(
            NpcId owner
    ) {

        this(
                owner,
                CharacterBirthStatus.UNSPECIFIED,
                CharacterFreedomStatus.UNSPECIFIED,
                CharacterCustodyStatus.NONE,
                CharacterCivilStatus.NORMAL
        );
    }

    public CharacterLegalState(
            NpcId owner,
            CharacterBirthStatus birthStatus,
            CharacterFreedomStatus freedomStatus,
            CharacterCustodyStatus custodyStatus,
            CharacterCivilStatus civilStatus
    ) {

        this.owner =
                Objects.requireNonNull(
                        owner,
                        "owner"
                );

        this.birthStatus =
                Objects.requireNonNull(
                        birthStatus,
                        "birthStatus"
                );

        this.freedomStatus =
                Objects.requireNonNull(
                        freedomStatus,
                        "freedomStatus"
                );

        this.custodyStatus =
                Objects.requireNonNull(
                        custodyStatus,
                        "custodyStatus"
                );

        this.civilStatus =
                Objects.requireNonNull(
                        civilStatus,
                        "civilStatus"
                );
    }

    public NpcId owner() {
        return owner;
    }

    public CharacterBirthStatus birthStatus() {
        return birthStatus;
    }

    public void setBirthStatus(
            CharacterBirthStatus birthStatus
    ) {

        this.birthStatus =
                Objects.requireNonNull(
                        birthStatus,
                        "birthStatus"
                );
    }

    public CharacterFreedomStatus freedomStatus() {
        return freedomStatus;
    }

    public void setFreedomStatus(
            CharacterFreedomStatus freedomStatus
    ) {

        this.freedomStatus =
                Objects.requireNonNull(
                        freedomStatus,
                        "freedomStatus"
                );
    }

    public CharacterCustodyStatus custodyStatus() {
        return custodyStatus;
    }

    public void setCustodyStatus(
            CharacterCustodyStatus custodyStatus
    ) {

        this.custodyStatus =
                Objects.requireNonNull(
                        custodyStatus,
                        "custodyStatus"
                );
    }

    public CharacterCivilStatus civilStatus() {
        return civilStatus;
    }

    public void setCivilStatus(
            CharacterCivilStatus civilStatus
    ) {

        this.civilStatus =
                Objects.requireNonNull(
                        civilStatus,
                        "civilStatus"
                );
    }
}
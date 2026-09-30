package dev.dtzudontsay.knownworld.simulation.social.society;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;

import java.util.List;

public record KhalasarSuccessionSnapshot(
        OrganizationId khalasar,
        NpcId khal,
        List<NpcId> khalakkas,
        List<NpcId> kos,
        List<NpcId> bloodriders,
        List<NpcId> khaleesis
) {

    public KhalasarSuccessionSnapshot {

        khalakkas =
                List.copyOf(
                        khalakkas
                );

        kos =
                List.copyOf(
                        kos
                );

        bloodriders =
                List.copyOf(
                        bloodriders
                );

        khaleesis =
                List.copyOf(
                        khaleesis
                );
    }

    /**
     * Khalasar leadership is deliberately not modeled as automatic
     * hereditary succession.
     */
    public boolean hereditarySuccessionGuaranteed() {

        return false;
    }

    /**
     * Kos are the natural structural candidates for a splintering event.
     */
    public boolean canSplinter() {

        return !kos.isEmpty();
    }
}
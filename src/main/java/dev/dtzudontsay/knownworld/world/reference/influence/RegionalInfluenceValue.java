package dev.dtzudontsay.knownworld.world.reference.influence;

import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.Objects;

public record RegionalInfluenceValue(
        double value,
        ReferenceProvenance provenance,
        double confidence,
        String sourceNote
) {

    public static RegionalInfluenceValue unknown() {

        return new RegionalInfluenceValue(
                0.0,
                ReferenceProvenance.UNKNOWN,
                0.0,
                ""
        );
    }

    public RegionalInfluenceValue {

        if (!Double.isFinite(
                value
        )
                || value < -1.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    "Regional influence value must be between -1.0 and 1.0"
            );
        }

        provenance =
                Objects.requireNonNull(
                        provenance,
                        "provenance"
                );

        if (!Double.isFinite(
                confidence
        )
                || confidence < 0.0
                || confidence > 1.0) {

            throw new IllegalArgumentException(
                    "confidence must be between 0.0 and 1.0"
            );
        }

        sourceNote =
                sourceNote == null
                        ? ""
                        : sourceNote.trim();

        if (provenance
                == ReferenceProvenance.UNKNOWN) {

            confidence =
                    0.0;
        }
    }

    public boolean known() {

        return provenance
                != ReferenceProvenance.UNKNOWN
                && confidence > 0.0;
    }
}
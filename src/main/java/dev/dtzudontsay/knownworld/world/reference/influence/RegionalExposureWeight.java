package dev.dtzudontsay.knownworld.world.reference.influence;

import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.Objects;

public record RegionalExposureWeight(
        double weight,
        ReferenceProvenance provenance,
        double confidence,
        String sourceNote
) {

    public static RegionalExposureWeight unknown() {

        return new RegionalExposureWeight(
                0.0,
                ReferenceProvenance.UNKNOWN,
                0.0,
                ""
        );
    }

    public RegionalExposureWeight {

        if (!Double.isFinite(
                weight
        )
                || weight < 0.0
                || weight > 1.0) {

            throw new IllegalArgumentException(
                    "Exposure weight must be between 0.0 and 1.0"
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
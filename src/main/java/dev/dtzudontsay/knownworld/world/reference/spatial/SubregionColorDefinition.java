package dev.dtzudontsay.knownworld.world.reference.spatial;

import java.util.Locale;

public final class SubregionColorDefinition {

    private final String hexColor;

    private final int rgb;

    private final String locationId;

    public SubregionColorDefinition(
            String hexColor,
            String locationId
    ) {
        if (hexColor == null
                || hexColor.isBlank()) {

            throw new IllegalArgumentException(
                    "hexColor cannot be empty"
            );
        }

        if (locationId == null
                || locationId.isBlank()) {

            throw new IllegalArgumentException(
                    "locationId cannot be empty"
            );
        }

        this.hexColor =
                normalizeHex(
                        hexColor
                );

        this.rgb =
                Integer.parseInt(
                        this.hexColor.substring(
                                1
                        ),
                        16
                );

        this.locationId =
                locationId.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );
    }

    public String hexColor() {
        return hexColor;
    }

    public int rgb() {
        return rgb;
    }

    public String locationId() {
        return locationId;
    }

    private static String normalizeHex(
            String raw
    ) {
        String value =
                raw.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (!value.startsWith(
                "#"
        )) {
            value =
                    "#"
                            + value;
        }

        if (!value.matches(
                "^#[0-9A-F]{6}$"
        )) {

            throw new IllegalArgumentException(
                    "Invalid hex color: "
                            + raw
            );
        }

        return value;
    }
}
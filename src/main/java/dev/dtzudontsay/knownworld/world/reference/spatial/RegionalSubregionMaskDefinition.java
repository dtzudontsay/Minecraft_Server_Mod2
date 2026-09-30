package dev.dtzudontsay.knownworld.world.reference.spatial;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RegionalSubregionMaskDefinition {

    private final String zoneId;

    private final String imageResource;

    private final int width;

    private final int height;

    private final int priority;

    private final List<SubregionColorDefinition> entries;

    private final Map<Integer, SubregionColorDefinition> byRgb;

    public RegionalSubregionMaskDefinition(
            String zoneId,
            String imageResource,
            int width,
            int height,
            int priority,
            List<SubregionColorDefinition> entries
    ) {
        if (zoneId == null
                || zoneId.isBlank()) {

            throw new IllegalArgumentException(
                    "zoneId cannot be empty"
            );
        }

        if (imageResource == null
                || imageResource.isBlank()) {

            throw new IllegalArgumentException(
                    "imageResource cannot be empty"
            );
        }

        if (width <= 0
                || height <= 0) {

            throw new IllegalArgumentException(
                    "Mask dimensions must be positive"
            );
        }

        Objects.requireNonNull(
                entries,
                "entries"
        );

        if (entries.isEmpty()) {

            throw new IllegalArgumentException(
                    "Mask entries cannot be empty"
            );
        }

        this.zoneId =
                zoneId.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        this.imageResource =
                imageResource.trim();

        this.width =
                width;

        this.height =
                height;

        this.priority =
                priority;

        this.entries =
                List.copyOf(
                        entries
                );

        Map<Integer, SubregionColorDefinition> index =
                new LinkedHashMap<>();

        for (
                SubregionColorDefinition entry :
                entries
        ) {

            if (index.putIfAbsent(
                    entry.rgb(),
                    entry
            ) != null) {

                throw new IllegalArgumentException(
                        "Duplicate color "
                                + entry.hexColor()
                                + " in subregion mask "
                                + this.zoneId
                );
            }
        }

        this.byRgb =
                Map.copyOf(
                        index
                );
    }

    public String zoneId() {
        return zoneId;
    }

    public String imageResource() {
        return imageResource;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int priority() {
        return priority;
    }

    public List<SubregionColorDefinition> entries() {
        return entries;
    }

    public int entryCount() {
        return entries.size();
    }

    public Optional<SubregionColorDefinition> lookup(
            int rgb
    ) {
        return Optional.ofNullable(
                byRgb.get(
                        rgb & 0xFFFFFF
                )
        );
    }

    public Collection<SubregionColorDefinition> allEntries() {
        return byRgb.values();
    }
}
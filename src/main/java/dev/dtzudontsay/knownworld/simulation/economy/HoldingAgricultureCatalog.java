
package dev.dtzudontsay.knownworld.simulation.economy;

import com.google.gson.Gson;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingStatus;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Batch 22.16: read-only, data-driven agricultural capacity for existing holdings.
 *
 * A region's farmland figures are explicitly labelled scenario estimates, not
 * canonical acreage and not Minecraft terrain measurements.
 *
 * The catalog does not create holdings, inventories, workers or settlements.
 */
public final class HoldingAgricultureCatalog {
    private static final String RESOURCE =
            "/data/knownworld/scenarios/agot_298_ac/agricultural_capacity.json";
    private static final Gson GSON = new Gson();

    public record Capacity(
            String sourceRegionId,
            String provenance,
            long farmableHectares,
            long cultivatedHectares,
            long grainKgPerHectare,
            long grainKgPerWorker,
            int fertilityPermille,
            long harvestPeriodDays,
            long protectedReserveKg,
            long gramsPerPersonPerDay
    ) {
        public Capacity {
            Objects.requireNonNull(sourceRegionId);
            Objects.requireNonNull(provenance);
            if (farmableHectares < 0L || cultivatedHectares < 0L
                    || cultivatedHectares > farmableHectares
                    || grainKgPerHectare <= 0L || grainKgPerWorker <= 0L
                    || fertilityPermille < 0 || fertilityPermille > 2000
                    || harvestPeriodDays <= 0L || protectedReserveKg < 0L
                    || gramsPerPersonPerDay <= 0L) {
                throw new IllegalArgumentException("Invalid holding agricultural capacity");
            }
        }

        /** Workforce comes from the population/labor domain, NOT holding.populationWeight. */
        public AgriculturalYieldModel.Inputs yieldInputs(
                long agriculturalWorkers,
                int weatherPermille,
                int seasonPermille
        ) {
            return new AgriculturalYieldModel.Inputs(
                    cultivatedHectares,
                    agriculturalWorkers,
                    grainKgPerHectare,
                    grainKgPerWorker,
                    fertilityPermille,
                    weatherPermille,
                    seasonPermille
            );
        }
    }

    private record Region(
            String locationId,
            String provenance,
            Map<HoldingType, Long> hectaresByType,
            int cultivationPermille,
            long grainKgPerHectare,
            long grainKgPerWorker,
            int fertilityPermille,
            long harvestPeriodDays,
            long protectedReserveKg,
            long gramsPerPersonPerDay
    ) {
        Region {
            hectaresByType = Map.copyOf(hectaresByType);
        }
    }

    private record Override(
            String holdingId,
            String provenance,
            long farmableHectares,
            long cultivatedHectares,
            Integer fertilityPermille
    ) {}

    // Gson-only input DTOs; scenario data does not become mutable runtime state.
    private static final class Data {
        int schemaVersion;
        List<RegionData> regionalProfiles;
        List<OverrideData> holdingOverrides;
    }

    private static final class RegionData {
        String locationId;
        String provenance;
        Map<String, Long> farmableHectaresByType;
        int cultivationPermille;
        long grainKgPerHectare;
        long grainKgPerWorker;
        int fertilityPermille;
        long harvestPeriodDays;
        long protectedReserveKg;
        long gramsPerPersonPerDay;
    }

    private static final class OverrideData {
        String holdingId;
        String provenance;
        long farmableHectares;
        long cultivatedHectares;
        Integer fertilityPermille;
    }

    private final Map<String, Region> regions;
    private final Map<String, Override> overrides;

    private HoldingAgricultureCatalog(
            Map<String, Region> regions,
            Map<String, Override> overrides
    ) {
        this.regions = Map.copyOf(regions);
        this.overrides = Map.copyOf(overrides);
    }

    /** Loads the resource only when explicitly requested; no startup mutation. */
    public static HoldingAgricultureCatalog loadDefault() throws IOException {
        InputStream input = HoldingAgricultureCatalog.class.getResourceAsStream(RESOURCE);
        if (input == null) {
            throw new IOException("Missing agricultural capacity resource: " + RESOURCE);
        }
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    public static HoldingAgricultureCatalog read(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        Data data = GSON.fromJson(reader, Data.class);
        if (data == null || data.schemaVersion != 1
                || data.regionalProfiles == null || data.holdingOverrides == null) {
            throw new IllegalArgumentException("Invalid agricultural capacity catalog schema");
        }

        Map<String, Region> regions = new HashMap<>();
        for (RegionData row : data.regionalProfiles) {
            String location = requireId(row.locationId);
            String provenance = requireProvenance(row.provenance);
            if (row.farmableHectaresByType == null) {
                throw new IllegalArgumentException("Missing area table for " + location);
            }
            EnumMap<HoldingType, Long> area = new EnumMap<>(HoldingType.class);
            for (Map.Entry<String, Long> entry : row.farmableHectaresByType.entrySet()) {
                HoldingType type = HoldingType.valueOf(entry.getKey());
                if (type == HoldingType.REGION || type == HoldingType.TERRITORY
                        || entry.getValue() == null || entry.getValue() < 0L) {
                    throw new IllegalArgumentException("Invalid farmland type/area for " + location);
                }
                area.put(type, entry.getValue());
            }
            if (row.cultivationPermille < 0 || row.cultivationPermille > 1000) {
                throw new IllegalArgumentException("Invalid cultivation fraction for " + location);
            }
            // Reuse the model's own validation for yield and fertility bounds.
            new Capacity(location, provenance, 0L, 0L,
                    row.grainKgPerHectare, row.grainKgPerWorker,
                    row.fertilityPermille, row.harvestPeriodDays,
                    row.protectedReserveKg, row.gramsPerPersonPerDay);
            Region previous = regions.putIfAbsent(location,
                    new Region(location, provenance, area, row.cultivationPermille,
                            row.grainKgPerHectare, row.grainKgPerWorker,
                            row.fertilityPermille, row.harvestPeriodDays,
                            row.protectedReserveKg, row.gramsPerPersonPerDay));
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate agricultural region: " + location);
            }
        }

        Map<String, Override> overrides = new HashMap<>();
        for (OverrideData row : data.holdingOverrides) {
            String holding = requireId(row.holdingId);
            String provenance = requireProvenance(row.provenance);
            if (row.farmableHectares < 0L || row.cultivatedHectares < 0L
                    || row.cultivatedHectares > row.farmableHectares
                    || (row.fertilityPermille != null
                    && (row.fertilityPermille < 0 || row.fertilityPermille > 2000))) {
                throw new IllegalArgumentException("Invalid agricultural holding override: " + holding);
            }
            if (overrides.putIfAbsent(holding,
                    new Override(holding, provenance, row.farmableHectares,
                            row.cultivatedHectares, row.fertilityPermille)) != null) {
                throw new IllegalArgumentException("Duplicate agricultural holding override: " + holding);
            }
        }
        return new HoldingAgricultureCatalog(regions, overrides);
    }

    public int regionalProfileCount() {
        return regions.size();
    }

    /**
     * Resolve by political/geographic reference ancestry, not physical coordinates.
     * No fallback is invented if this holding has no applicable regional profile.
     */
    public Optional<Capacity> resolve(
            LandedHolding holding,
            WorldReferenceCatalog references
    ) {
        Objects.requireNonNull(references, "references");
        return resolve(holding, references::location);
    }

    public Optional<Capacity> resolve(
            LandedHolding holding,
            Function<String, Optional<WorldLocationDefinition>> locationLookup
    ) {
        Objects.requireNonNull(holding, "holding");
        Objects.requireNonNull(locationLookup, "locationLookup");
        if (holding.status() != HoldingStatus.ACTIVE
                || holding.type() == HoldingType.REGION
                || holding.type() == HoldingType.TERRITORY) {
            return Optional.empty();
        }

        String locationId = holding.worldLocationId();
        Set<String> visited = new HashSet<>();
        Region region = null;
        while (locationId != null && visited.add(locationId)) {
            region = regions.get(locationId);
            if (region != null) {
                break;
            }
            WorldLocationDefinition location = Objects.requireNonNull(
                    locationLookup.apply(locationId), "location lookup result"
            ).orElse(null);
            if (location == null) {
                return Optional.empty();
            }
            locationId = location.parentId();
        }
        if (region == null) {
            return Optional.empty();
        }

        Long estimatedArea = region.hectaresByType().get(holding.type());
        Override replacement = holding.authoredId() == null
                ? null : overrides.get(holding.authoredId());
        if (estimatedArea == null && replacement == null) {
            return Optional.empty();
        }
        long area = replacement == null ? estimatedArea : replacement.farmableHectares();
        long cultivated = replacement == null
                ? Math.multiplyExact(area, region.cultivationPermille()) / 1000L
                : replacement.cultivatedHectares();
        String provenance = replacement == null
                ? region.provenance() : replacement.provenance();
        int fertility = replacement != null && replacement.fertilityPermille() != null
                ? replacement.fertilityPermille() : region.fertilityPermille();
        return Optional.of(new Capacity(
                region.locationId(), provenance, area, cultivated,
                region.grainKgPerHectare(), region.grainKgPerWorker(),
                fertility, region.harvestPeriodDays(), region.protectedReserveKg(),
                region.gramsPerPersonPerDay()
        ));
    }

    private static String requireId(String value) {
        if (value == null || !value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid authored agriculture ID: " + value);
        }
        return value;
    }

    private static String requireProvenance(String value) {
        if (!"MOD_ESTIMATE".equals(value)
                && !"RESEARCHED_ESTIMATE".equals(value)
                && !"USER_AUTHORED".equals(value)) {
            throw new IllegalArgumentException("Invalid agriculture provenance: " + value);
        }
        return value;
    }
}

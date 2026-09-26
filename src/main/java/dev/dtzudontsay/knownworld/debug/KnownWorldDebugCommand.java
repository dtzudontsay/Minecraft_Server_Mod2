package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.data.MasterMapData;
import dev.dtzudontsay.knownworld.world.geography.FeatureGeometryType;
import dev.dtzudontsay.knownworld.world.geography.GeographicBounds;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldDefinition;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.calibration.CalibrationRegistry;
import dev.dtzudontsay.knownworld.world.geography.calibration.CalibrationResult;
import dev.dtzudontsay.knownworld.world.geography.calibration.DistanceCalibrationAnchor;
import dev.dtzudontsay.knownworld.world.geography.calibration.MasterMapCalibration;
import dev.dtzudontsay.knownworld.world.geography.location.FeatureLocation;
import dev.dtzudontsay.knownworld.world.geography.location.FeatureLocationRegistry;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneDefinition;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneId;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneRegistry;
import dev.dtzudontsay.knownworld.world.terrain.TerrainSample;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class KnownWorldDebugCommand {

    private KnownWorldDebugCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("knownworld")

                                        .then(
                                                Commands.literal("status")
                                                        .executes(
                                                                KnownWorldDebugCommand::executeStatus
                                                        )
                                        )

                                        .then(
                                                Commands.literal("geo")
                                                        .executes(
                                                                KnownWorldDebugCommand::executeGeo
                                                        )
                                        )

                                        .then(
                                                Commands.literal("map")
                                                        .executes(
                                                                KnownWorldDebugCommand::executeMap
                                                        )
                                        )

                                        .then(
                                                Commands.literal("calibration")
                                                        .executes(
                                                                KnownWorldDebugCommand::executeCalibration
                                                        )
                                        )

                                        .then(
                                                Commands.literal("features")
                                                        .executes(
                                                                KnownWorldDebugCommand::executeFeatures
                                                        )
                                        )

                                        .then(
                                                Commands.literal("feature")
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                KnownWorldDebugCommand::executeFeature
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("locations")
                                                        .executes(
                                                                KnownWorldDebugCommand::executeLocations
                                                        )
                                        )

                                        .then(
                                                Commands.literal("locate")
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                KnownWorldDebugCommand::executeLocate
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal("zones")
                                                        .executes(
                                                                KnownWorldDebugCommand::executeZones
                                                        )
                                        )

                                        .then(
                                                Commands.literal("zone")
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                KnownWorldDebugCommand::executeZone
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int executeStatus(
            CommandContext<CommandSourceStack> context
    ) {
        GeographicDataManager data =
                GeographicDataManager.getInstance();

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "%s | Dataset: %s"
                                .formatted(
                                        WorldDefinition.PROJECT_NAME,
                                        data.datasetState()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Primary canon: %s | Canon scale: %.1f m/block | Minecraft world scale: %.2f"
                                .formatted(
                                        data.primaryCanon(),
                                        WorldDefinition.HORIZONTAL_METRES_PER_BLOCK,
                                        WorldDefinition.MINECRAFT_WORLD_SCALE
                                )
                ),
                false
        );

        return 1;
    }

    private static int executeGeo(
            CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source =
                context.getSource();

        Vec3 position =
                source.getPosition();

        WorldCoordinate coordinate =
                WorldProjection.fromMinecraft(
                        position.x,
                        position.z
                );

        TerrainSample terrain =
                GeographicDataManager
                        .getInstance()
                        .sample(coordinate);

        source.sendSuccess(
                () -> Component.literal(
                        "Minecraft X %.2f Z %.2f | East %.2f m North %.2f m"
                                .formatted(
                                        position.x,
                                        position.z,
                                        coordinate.eastMetres(),
                                        coordinate.northMetres()
                                )
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Region: %s | Elevation: %.1f m | Data: %s"
                                .formatted(
                                        terrain.region(),
                                        terrain.elevationMetres(),
                                        terrain.dataSource()
                                )
                ),
                false
        );

        return 1;
    }

    private static int executeMap(
            CommandContext<CommandSourceStack> context
    ) {
        MasterMapData map =
                MasterMapData.getInstance();

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Master map: %s | %d x %d px"
                                .formatted(
                                        map.sourceName(),
                                        map.imageWidthPixels(),
                                        map.imageHeightPixels()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Anchors: %d | Outside generation: %s | Boundary: %s"
                                .formatted(
                                        map.anchorCount(),
                                        map.allowsGenerationOutsideMap()
                                                ? "ALLOWED"
                                                : "DISABLED",
                                        map.outerBoundaryMode()
                                )
                ),
                false
        );

        return 1;
    }

    private static int executeCalibration(
            CommandContext<CommandSourceStack> context
    ) {
        CalibrationResult result =
                MasterMapCalibration.result();

        DistanceCalibrationAnchor anchor =
                CalibrationRegistry.primaryAnchor();

        GeographicBounds bounds =
                MasterMapCalibration.worldBounds();

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Calibration: %s | Anchor: %s"
                                .formatted(
                                        result.status(),
                                        anchor.displayName()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Anchor pixels: %.2f px | Canon distance: %.1f m"
                                .formatted(
                                        anchor.pixelDistance(),
                                        anchor.canonicalDistanceMetres()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Scale: %.2f m/px"
                                .formatted(
                                        result.metresPerPixel()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "World rectangle: %.1f km x %.1f km"
                                .formatted(
                                        result.worldWidthMetres() / 1000.0,
                                        result.worldHeightMetres() / 1000.0
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Bounds E/W: %.0f to %.0f m | N/S: %.0f to %.0f m"
                                .formatted(
                                        bounds.minEastMetres(),
                                        bounds.maxEastMetres(),
                                        bounds.minNorthMetres(),
                                        bounds.maxNorthMetres()
                                )
                ),
                false
        );

        return 1;
    }

    private static int executeFeatures(
            CommandContext<CommandSourceStack> context
    ) {
        int total =
                GeographicFeatureRegistry.getFeatureCount();

        long points =
                GeographicFeatureRegistry.countByGeometryType(
                        FeatureGeometryType.POINT
                );

        long lines =
                GeographicFeatureRegistry.countByGeometryType(
                        FeatureGeometryType.LINE
                );

        long areas =
                GeographicFeatureRegistry.countByGeometryType(
                        FeatureGeometryType.AREA
                );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Geographic features loaded: %d"
                                .formatted(total)
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "POINT: %d | LINE: %d | AREA: %d"
                                .formatted(
                                        points,
                                        lines,
                                        areas
                                )
                ),
                false
        );

        return 1;
    }

    private static int executeFeature(
            CommandContext<CommandSourceStack> context
    ) {
        String id =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        Optional<GeographicFeature> result =
                GeographicFeatureRegistry.get(id);

        if (result.isEmpty()) {
            context.getSource().sendFailure(
                    Component.literal(
                            "Unknown geographic feature id: " + id
                    )
            );

            return 0;
        }

        GeographicFeature feature =
                result.get();

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "%s [%s]"
                                .formatted(
                                        feature.displayName(),
                                        feature.id()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Type: %s | Geometry: %s | Entry title: %s"
                                .formatted(
                                        feature.type(),
                                        feature.geometryType(),
                                        feature.showEntryTitle()
                                                ? "YES"
                                                : "NO"
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Confidence: %s | Source: %s"
                                .formatted(
                                        feature.confidence(),
                                        feature.sourceNote()
                                )
                ),
                false
        );

        return 1;
    }

    private static int executeLocations(
            CommandContext<CommandSourceStack> context
    ) {
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Fixed geographic feature locations: %d"
                                .formatted(
                                        FeatureLocationRegistry.locationCount()
                                )
                ),
                false
        );

        return 1;
    }

    private static int executeLocate(
            CommandContext<CommandSourceStack> context
    ) {
        String id =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        Optional<GeographicFeature> featureResult =
                GeographicFeatureRegistry.get(id);

        if (featureResult.isEmpty()) {
            context.getSource().sendFailure(
                    Component.literal(
                            "Unknown geographic feature id: " + id
                    )
            );

            return 0;
        }

        Optional<FeatureLocation> locationResult =
                FeatureLocationRegistry.get(id);

        if (locationResult.isEmpty()) {
            context.getSource().sendFailure(
                    Component.literal(
                            "Feature exists but has no fixed location yet: "
                                    + id
                    )
            );

            return 0;
        }

        GeographicFeature feature =
                featureResult.get();

        FeatureLocation location =
                locationResult.get();

        WorldCoordinate world =
                location.worldCoordinate();

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "%s [%s]"
                                .formatted(
                                        feature.displayName(),
                                        feature.id()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Master pixel: X %.2f Y %.2f"
                                .formatted(
                                        location.masterMapCoordinate().pixelX(),
                                        location.masterMapCoordinate().pixelY()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Canonical world: East %.0f m | North %.0f m"
                                .formatted(
                                        world.eastMetres(),
                                        world.northMetres()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Minecraft: X %.0f | Z %.0f | World scale %.2f"
                                .formatted(
                                        location.minecraftX(),
                                        location.minecraftZ(),
                                        WorldDefinition.MINECRAFT_WORLD_SCALE
                                )
                ),
                false
        );

        return 1;
    }

    private static int executeZones(
            CommandContext<CommandSourceStack> context
    ) {
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Regional map zones registered: %d / %d"
                                .formatted(
                                        MapZoneRegistry.registeredZoneCount(),
                                        MapZoneId.values().length
                                )
                ),
                false
        );

        for (MapZoneId zoneId : MapZoneId.values()) {
            boolean registered =
                    MapZoneRegistry
                            .get(zoneId)
                            .isPresent();

            context.getSource().sendSuccess(
                    () -> Component.literal(
                            "%s [%s] | %s"
                                    .formatted(
                                            zoneId.displayName(),
                                            zoneId.name(),
                                            registered
                                                    ? "REGISTERED"
                                                    : "PENDING"
                                    )
                    ),
                    false
            );
        }

        return 1;
    }

    private static int executeZone(
            CommandContext<CommandSourceStack> context
    ) {
        String rawId =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        final MapZoneId zoneId;

        try {
            zoneId =
                    MapZoneId.fromCode(rawId);
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );

            return 0;
        }

        Optional<MapZoneDefinition> result =
                MapZoneRegistry.get(zoneId);

        if (result.isEmpty()) {
            context.getSource().sendFailure(
                    Component.literal(
                            "Map zone exists but is not calibrated yet: "
                                    + zoneId.name()
                    )
            );

            return 0;
        }

        MapZoneDefinition zone =
                result.get();

        MapCoordinate upperLeft =
                zone.toMaster(
                        new MapCoordinate(
                                0.0,
                                0.0
                        )
                );

        MapCoordinate lowerRight =
                zone.toMaster(
                        new MapCoordinate(
                                zone.regionalWidthPixels(),
                                zone.regionalHeightPixels()
                        )
                );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "%s [%s]"
                                .formatted(
                                        zone.id().displayName(),
                                        zone.id()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Regional raster: %d x %d px"
                                .formatted(
                                        zone.regionalWidthPixels(),
                                        zone.regionalHeightPixels()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Master upper-left: X %.2f Y %.2f"
                                .formatted(
                                        upperLeft.pixelX(),
                                        upperLeft.pixelY()
                                )
                ),
                false
        );

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Master lower-right: X %.2f Y %.2f"
                                .formatted(
                                        lowerRight.pixelX(),
                                        lowerRight.pixelY()
                                )
                ),
                false
        );

        return 1;
    }
}
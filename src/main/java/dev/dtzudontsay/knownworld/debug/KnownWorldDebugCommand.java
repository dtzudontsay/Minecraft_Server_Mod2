package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.data.MasterMapData;
import dev.dtzudontsay.knownworld.world.geography.FeatureGeometryType;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldDefinition;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
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
                        "Primary canon: %s | Horizontal scale: %.1f m/block"
                                .formatted(
                                        data.primaryCanon(),
                                        WorldDefinition.HORIZONTAL_METRES_PER_BLOCK
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
}
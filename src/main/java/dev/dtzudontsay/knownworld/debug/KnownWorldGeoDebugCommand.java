package dev.dtzudontsay.knownworld.debug;

import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public final class KnownWorldGeoDebugCommand {

    private KnownWorldGeoDebugCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("kwgeo")
                                        .executes(
                                                KnownWorldGeoDebugCommand::execute
                                        )
                        )
        );
    }

    private static int execute(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context
    ) {
        CommandSourceStack source =
                context.getSource();

        Vec3 position =
                source.getPosition();

        KnownWorldGeoSample sample =
                KnownWorldGeoSampler.sampleMinecraft(
                        position.x,
                        position.z
                );

        source.sendSuccess(
                () -> Component.literal(
                        "=== Known World Raster Sample ==="
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Minecraft: X %.2f | Z %.2f"
                                .formatted(
                                        position.x,
                                        position.z
                                )
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Canonical: East %.0f m | North %.0f m"
                                .formatted(
                                        sample.worldCoordinate().eastMetres(),
                                        sample.worldCoordinate().northMetres()
                                )
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Logical master: X %.2f | Y %.2f"
                                .formatted(
                                        sample.logicalMapCoordinate().pixelX(),
                                        sample.logicalMapCoordinate().pixelY()
                                )
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Runtime raster: X %.2f | Y %.2f"
                                .formatted(
                                        sample.rasterPixelX(),
                                        sample.rasterPixelY()
                                )
                ),
                false
        );

        if (!sample.insideKnownWorldMap()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "Result: OUTSIDE KNOWN WORLD MAP"
                    ),
                    false
            );

            return 1;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Surface: %s"
                                .formatted(
                                        sample.land()
                                                ? "LAND"
                                                : "WATER"
                                )
                ),
                false
        );

        double coastDistance =
                sample.coastDistanceMetres();

        source.sendSuccess(
                () -> Component.literal(
                        "Signed coast distance: %.0f m | %.2f km"
                                .formatted(
                                        coastDistance,
                                        coastDistance / 1000.0
                                )
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Coast direction: %s"
                                .formatted(
                                        coastDistance > 0.0
                                                ? "INLAND"
                                                : coastDistance < 0.0
                                                ? "OFFSHORE"
                                                : "COASTLINE"
                                )
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Near coast (10 km): %s | Near coast (50 km): %s"
                                .formatted(
                                        sample.nearCoast(
                                                10_000.0
                                        )
                                                ? "YES"
                                                : "NO",
                                        sample.nearCoast(
                                                50_000.0
                                        )
                                                ? "YES"
                                                : "NO"
                                )
                ),
                false
        );

        return 1;
    }
}
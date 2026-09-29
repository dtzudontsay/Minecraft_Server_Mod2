package dev.dtzudontsay.knownworld.debug;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceSettings;
import dev.dtzudontsay.knownworld.world.reference.spatial.LandmassZoneCatalog;
import dev.dtzudontsay.knownworld.world.reference.spatial.LandmassZoneResolver;
import dev.dtzudontsay.knownworld.world.reference.spatial.TerritoryZoneCatalog;
import dev.dtzudontsay.knownworld.world.reference.spatial.TerritoryZoneResolver;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.stream.Collectors;

public final class WorldReferenceDebugCommand {

    private WorldReferenceDebugCommand() {
    }

    public static void register() {

        CommandRegistrationCallback.EVENT.register(
                (
                        dispatcher,
                        registryAccess,
                        environment
                ) ->
                        dispatcher.register(

                                Commands.literal(
                                                "kwref"
                                        )

                                        .then(
                                                Commands.literal(
                                                                "status"
                                                        )
                                                        .executes(
                                                                WorldReferenceDebugCommand::status
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "settings"
                                                        )
                                                        .executes(
                                                                WorldReferenceDebugCommand::settings
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "landmass"
                                                        )
                                                        .executes(
                                                                WorldReferenceDebugCommand::landmass
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "territory"
                                                        )
                                                        .executes(
                                                                WorldReferenceDebugCommand::territory
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "location"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                WorldReferenceDebugCommand::location
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "culture"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                WorldReferenceDebugCommand::culture
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "subcultures"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "enabled",
                                                                                BoolArgumentType.bool()
                                                                        )
                                                                        .executes(
                                                                                WorldReferenceDebugCommand::subcultures
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "languages"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "enabled",
                                                                                BoolArgumentType.bool()
                                                                        )
                                                                        .executes(
                                                                                WorldReferenceDebugCommand::languages
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "common_tongue"
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "everyone",
                                                                                BoolArgumentType.bool()
                                                                        )
                                                                        .executes(
                                                                                WorldReferenceDebugCommand::commonTongue
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) {
        WorldReferenceCatalog catalog =
                WorldReferenceCatalog.get();

        success(
                context,
                "World references | locations="
                        + catalog.locations()
                        .size()
                        + " cultures="
                        + catalog.cultures()
                        .size()
                        + " religions="
                        + catalog.religions()
                        .size()
                        + " orders="
                        + catalog.religiousOrders()
                        .size()
                        + " languages="
                        + catalog.languages()
                        .size()
                        + " occupations="
                        + catalog.occupations()
                        .size()
                        + " roles="
                        + catalog.roles()
                        .size()
                        + " landmassZones="
                        + LandmassZoneCatalog.get()
                        .size()
                        + " territoryZones="
                        + TerritoryZoneCatalog.get()
                        .size()
        );

        return 1;
    }

    private static int settings(
            CommandContext<CommandSourceStack> context
    ) {
        WorldReferenceSettings settings =
                WorldReferenceSettings.get();

        success(
                context,
                "subcultures="
                        + settings.subculturesEnabled()
                        + " languageBarriers="
                        + settings.languageBarriersEnabled()
                        + " everyoneSpeaksCommonTongue="
                        + settings.everyoneSpeaksCommonTongue()
        );

        return 1;
    }

    private static int landmass(
            CommandContext<CommandSourceStack> context
    ) {
        double x =
                context.getSource()
                        .getPosition()
                        .x;

        double z =
                context.getSource()
                        .getPosition()
                        .z;

        var sample =
                KnownWorldGeoSampler.sampleMinecraft(
                        x,
                        z
                );

        if (!sample.insideKnownWorldMap()) {

            failure(
                    context,
                    "Position lies outside the Known World map."
            );

            return 0;
        }

        if (!sample.land()) {

            success(
                    context,
                    "Landmass: none (water)"
            );

            return 1;
        }

        var location =
                LandmassZoneResolver.resolveMinecraft(
                                x,
                                z
                        )
                        .orElse(
                                null
                        );

        if (location == null) {

            success(
                    context,
                    "Landmass: unresolved land"
            );

            return 1;
        }

        success(
                context,
                "Landmass: "
                        + location.displayName()
                        + " ["
                        + location.id()
                        + "]"
        );

        success(
                context,
                "Master map pixel: "
                        + String.format(
                        "%.2f / %.2f",
                        sample.logicalMapCoordinate()
                                .pixelX(),
                        sample.logicalMapCoordinate()
                                .pixelY()
                )
        );

        return 1;
    }

    private static int territory(
            CommandContext<CommandSourceStack> context
    ) {
        double x =
                context.getSource()
                        .getPosition()
                        .x;

        double z =
                context.getSource()
                        .getPosition()
                        .z;

        var sample =
                KnownWorldGeoSampler.sampleMinecraft(
                        x,
                        z
                );

        if (!sample.insideKnownWorldMap()) {

            failure(
                    context,
                    "Position lies outside the Known World map."
            );

            return 0;
        }

        if (!sample.land()) {

            success(
                    context,
                    "Landmass: none (water)"
            );

            success(
                    context,
                    "Territory: none"
            );

            return 1;
        }

        var landmass =
                LandmassZoneResolver.resolveMinecraft(
                                x,
                                z
                        )
                        .orElse(
                                null
                        );

        if (landmass == null) {

            success(
                    context,
                    "Landmass: unresolved land"
            );

            success(
                    context,
                    "Territory: unresolved"
            );

            return 1;
        }

        success(
                context,
                "Landmass: "
                        + landmass.displayName()
                        + " ["
                        + landmass.id()
                        + "]"
        );

        var territory =
                TerritoryZoneResolver.resolveMinecraft(
                                x,
                                z
                        )
                        .orElse(
                                null
                        );

        if (territory == null) {

            success(
                    context,
                    "Territory: none"
            );

            return 1;
        }

        success(
                context,
                "Territory: "
                        + territory.displayName()
                        + " ["
                        + territory.id()
                        + "]"
        );

        success(
                context,
                "Master map pixel: "
                        + String.format(
                        "%.2f / %.2f",
                        sample.logicalMapCoordinate()
                                .pixelX(),
                        sample.logicalMapCoordinate()
                                .pixelY()
                )
        );

        return 1;
    }

    private static int location(
            CommandContext<CommandSourceStack> context
    ) {
        String id =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        WorldReferenceCatalog catalog =
                WorldReferenceCatalog.get();

        var location =
                catalog.location(
                                id
                        )
                        .orElse(
                                null
                        );

        if (location == null) {

            failure(
                    context,
                    "Unknown location: "
                            + id
            );

            return 0;
        }

        String hierarchy =
                catalog.pathToRoot(
                                location.id()
                        )
                        .stream()
                        .map(
                                entry ->
                                        entry.displayName()
                        )
                        .collect(
                                Collectors.joining(
                                        " > "
                                )
                        );

        success(
                context,
                location.displayName()
                        + " | "
                        + location.kind()
                        + " | "
                        + location.provenance()
        );

        success(
                context,
                "Hierarchy: "
                        + hierarchy
        );

        catalog.resolvedMapLocation(
                        location.id()
                )
                .ifPresent(
                        position ->
                                success(
                                        context,
                                        "Map position: "
                                                + String.format(
                                                "%.2f, %.2f",
                                                position.masterMapCoordinate()
                                                        .pixelX(),
                                                position.masterMapCoordinate()
                                                        .pixelY()
                                        )
                                                + " | Minecraft X/Z "
                                                + String.format(
                                                "%.1f / %.1f",
                                                position.minecraftX(),
                                                position.minecraftZ()
                                        )
                                )
                );

        LandmassZoneCatalog.get()
                .find(
                        location.id()
                )
                .ifPresent(
                        zone ->
                                success(
                                        context,
                                        "Landmass authoring zone: "
                                                + zone.polygons()
                                                .size()
                                                + " polygon(s), priority="
                                                + zone.priority()
                                )
                );

        TerritoryZoneCatalog.get()
                .find(
                        location.id()
                )
                .ifPresent(
                        zone ->
                                success(
                                        context,
                                        "Territory authoring zone: "
                                                + zone.polygons()
                                                .size()
                                                + " polygon(s), priority="
                                                + zone.priority()
                                                + ", landmass="
                                                + zone.requiredLandmassId()
                                )
                );

        return 1;
    }

    private static int culture(
            CommandContext<CommandSourceStack> context
    ) {
        String id =
                StringArgumentType.getString(
                        context,
                        "id"
                );

        WorldReferenceCatalog catalog =
                WorldReferenceCatalog.get();

        var culture =
                catalog.culture(
                                id
                        )
                        .orElse(
                                null
                        );

        if (culture == null) {

            failure(
                    context,
                    "Unknown culture: "
                            + id
            );

            return 0;
        }

        success(
                context,
                culture.displayName()
                        + " | category="
                        + culture.category()
                        + " | effective="
                        + catalog.effectiveCultureId(
                        culture.id()
                )
        );

        return 1;
    }

    private static int subcultures(
            CommandContext<CommandSourceStack> context
    ) {
        boolean enabled =
                BoolArgumentType.getBool(
                        context,
                        "enabled"
                );

        WorldReferenceSettings.get()
                .setSubculturesEnabled(
                        enabled
                );

        success(
                context,
                "Subcultures enabled: "
                        + enabled
        );

        return 1;
    }

    private static int languages(
            CommandContext<CommandSourceStack> context
    ) {
        boolean enabled =
                BoolArgumentType.getBool(
                        context,
                        "enabled"
                );

        WorldReferenceSettings.get()
                .setLanguageBarriersEnabled(
                        enabled
                );

        success(
                context,
                "Language barriers enabled: "
                        + enabled
        );

        return 1;
    }

    private static int commonTongue(
            CommandContext<CommandSourceStack> context
    ) {
        boolean everyone =
                BoolArgumentType.getBool(
                        context,
                        "everyone"
                );

        WorldReferenceSettings.get()
                .setEveryoneSpeaksCommonTongue(
                        everyone
                );

        success(
                context,
                "Everyone speaks Common Tongue: "
                        + everyone
        );

        return 1;
    }

    private static void success(
            CommandContext<CommandSourceStack> context,
            String message
    ) {
        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                        message
                                ),
                        false
                );
    }

    private static void failure(
            CommandContext<CommandSourceStack> context,
            String message
    ) {
        context.getSource()
                .sendFailure(
                        Component.literal(
                                message
                        )
                );
    }
}
package dev.dtzudontsay.knownworld.world.reference;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.dtzudontsay.knownworld.KnownWorld;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorldReferenceSettings {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static WorldReferenceSettings instance;

    private boolean subculturesEnabled =
            true;

    private boolean languageBarriersEnabled =
            true;

    private boolean everyoneSpeaksCommonTongue =
            false;

    public static synchronized void bootstrap() {

        if (instance != null) {
            return;
        }

        Path file =
                configFile();

        if (!Files.exists(
                file
        )) {

            instance =
                    new WorldReferenceSettings();

            instance.save();

            return;
        }

        try {

            String json =
                    Files.readString(
                            file,
                            StandardCharsets.UTF_8
                    );

            WorldReferenceSettings loaded =
                    GSON.fromJson(
                            json,
                            WorldReferenceSettings.class
                    );

            instance =
                    loaded == null
                            ? new WorldReferenceSettings()
                            : loaded;

        } catch (
                IOException
                | RuntimeException exception
        ) {

            KnownWorld.LOGGER.error(
                    "Failed to load world reference settings. Using defaults.",
                    exception
            );

            instance =
                    new WorldReferenceSettings();
        }
    }

    public static WorldReferenceSettings get() {

        if (instance == null) {

            bootstrap();
        }

        return instance;
    }

    public boolean subculturesEnabled() {
        return subculturesEnabled;
    }

    public boolean languageBarriersEnabled() {
        return languageBarriersEnabled;
    }

    public boolean everyoneSpeaksCommonTongue() {
        return everyoneSpeaksCommonTongue;
    }

    public boolean effectiveLanguageBarriersEnabled() {

        return languageBarriersEnabled
                && !everyoneSpeaksCommonTongue;
    }

    public void setSubculturesEnabled(
            boolean enabled
    ) {
        this.subculturesEnabled =
                enabled;

        save();
    }

    public void setLanguageBarriersEnabled(
            boolean enabled
    ) {
        this.languageBarriersEnabled =
                enabled;

        save();
    }

    public void setEveryoneSpeaksCommonTongue(
            boolean enabled
    ) {
        this.everyoneSpeaksCommonTongue =
                enabled;

        save();
    }

    public void save() {

        Path file =
                configFile();

        try {

            Files.createDirectories(
                    file.getParent()
            );

            Files.writeString(
                    file,
                    GSON.toJson(
                            this
                    ),
                    StandardCharsets.UTF_8
            );

        } catch (
                IOException exception
        ) {

            KnownWorld.LOGGER.error(
                    "Failed to save world reference settings.",
                    exception
            );
        }
    }

    private static Path configFile() {

        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(
                        "knownworld"
                )
                .resolve(
                        "world_reference.json"
                );
    }
}
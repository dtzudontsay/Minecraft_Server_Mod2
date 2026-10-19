
package dev.dtzudontsay.knownworld.simulation.casestudy;

import com.google.gson.JsonObject;
import dev.dtzudontsay.knownworld.KnownWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Automatic local Ollama model warm-up.
 *
 * Called when Minecraft's integrated or dedicated server starts.
 *
 * Requires an already running Ollama background service.
 * Does not execute external programs or install models.
 *
 * On world start:
 *   Ask Ollama to load the model into memory.
 *
 * On world stop:
 *   Ask Ollama to unload the model and release GPU memory.
 *
 * Both requests are asynchronous and do not block the
 * Minecraft server thread.
 */
public final class WinterfellOllamaPreloader
        implements ModInitializer {

    private static final String DEFAULT_MODEL =
            "qwen3.5:9b";

    private static final String DEFAULT_ENDPOINT =
            "http://127.0.0.1:11434/api/chat";

    private static final HttpClient HTTP =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();

    @Override
    public void onInitialize() {

        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> {

                    if (preloadingEnabled()) {
                        sendModelLifecycleRequest(true);
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> {

                    if (preloadingEnabled()) {
                        sendModelLifecycleRequest(false);
                    }
                }
        );

        KnownWorld.LOGGER.info(
                "Winterfell local AI model preloader registered."
        );
    }

    /**
     * Preload with keep_alive=15m.
     *
     * The conversation service currently sets keep_alive=5m
     * on subsequent chat requests, so the model can unload
     * after that later idle period.
     *
     * Unload requests use keep_alive=0.
     */
    private static void sendModelLifecycleRequest(
            boolean preload
    ) {

        String model = modelName();
        String url = endpoint();

        try {

            JsonObject body = new JsonObject();

            body.addProperty("model", model);
            body.addProperty("stream", false);

            if (preload) {

                body.addProperty("keep_alive", "15m");

                /*
                 * Match the conversation service's context size
                 * to minimize unnecessary model reconfiguration.
                 */
                JsonObject options = new JsonObject();

                options.addProperty(
                        "num_ctx",
                        contextLength()
                );

                body.add("options", options);

            } else {

                body.addProperty("keep_alive", 0);
            }

            /*
             * Ollama treats an empty /api/chat request as
             * a load/unload operation rather than NPC speech.
             */
            HttpRequest request =
                    HttpRequest.newBuilder(
                                    URI.create(url)
                            )
                            .timeout(
                                    preload
                                            ? Duration.ofMinutes(4)
                                            : Duration.ofSeconds(15)
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            body.toString(),
                                            StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            KnownWorld.LOGGER.info(
                    "Winterfell Ollama: {} model {}",
                    preload ? "preloading" : "unloading",
                    model
            );

            HTTP.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            ).whenComplete((response, error) -> {

                if (error != null) {

                    if (preload) {

                        KnownWorld.LOGGER.warn(
                                "Winterfell Ollama preload unavailable. "
                                        + "Ensure the Ollama application is running. Cause: {}",
                                error.toString()
                        );

                    } else {

                        KnownWorld.LOGGER.debug(
                                "Winterfell Ollama unload request failed: {}",
                                error.toString()
                        );
                    }

                    return;
                }

                int code = response.statusCode();

                if (code / 100 != 2) {

                    KnownWorld.LOGGER.warn(
                            "Winterfell Ollama {} returned HTTP {} "
                                    + "for model {}",
                            preload ? "preload" : "unload",
                            code,
                            model
                    );

                    if (preload && code == 404) {

                        KnownWorld.LOGGER.warn(
                                "The configured Ollama model was not found. "
                                        + "Run 'ollama pull {}' on this computer.",
                                model
                        );
                    }

                    return;
                }

                KnownWorld.LOGGER.info(
                        "Winterfell Ollama model {} {}.",
                        model,
                        preload
                                ? "preloaded and ready for NPC dialogue"
                                : "unloaded"
                );
            });

        } catch (RuntimeException error) {

            KnownWorld.LOGGER.warn(
                    "Unable to prepare Winterfell Ollama {} request.",
                    preload ? "preload" : "unload",
                    error
            );
        }
    }

    /**
     * Enabled by default.
     *
     * Set KNOWNWORLD_OLLAMA_PRELOAD=false in the
     * Minecraft server's environment to disable the
     * automatic model preload/unload lifecycle.
     */
    private static boolean preloadingEnabled() {

        String value =
                System.getenv("KNOWNWORLD_OLLAMA_PRELOAD");

        return value == null
                || !value.equalsIgnoreCase("false");
    }

    /**
     * Use exactly the same model setting as
     * WinterfellConversationService.
     */
    private static String modelName() {

        String value =
                System.getenv("KNOWNWORLD_OLLAMA_MODEL");

        return value == null || value.isBlank()
                ? DEFAULT_MODEL
                : value.trim();
    }

    /**
     * Use the same /api/chat endpoint as the
     * dialogue service, including user overrides.
     */
    private static String endpoint() {

        String value =
                System.getenv("KNOWNWORLD_OLLAMA_URL");

        return value == null || value.isBlank()
                ? DEFAULT_ENDPOINT
                : value.trim();
    }

    private static int contextLength() {

        String value =
                System.getenv("KNOWNWORLD_OLLAMA_CONTEXT");

        if (value == null || value.isBlank()) {
            return 8192;
        }

        try {

            int requested =
                    Integer.parseInt(value.trim());

            return Math.max(
                    4096,
                    Math.min(16384, requested)
            );

        } catch (NumberFormatException ignored) {

            return 8192;
        }
    }
}

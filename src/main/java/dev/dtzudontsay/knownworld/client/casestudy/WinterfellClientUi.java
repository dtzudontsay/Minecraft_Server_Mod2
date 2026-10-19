
package dev.dtzudontsay.knownworld.client.casestudy;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dtzudontsay.knownworld.simulation.casestudy.KnownWorldUiPackets;
import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellConversationPackets;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Registered by the client entrypoint;
 * dedicated servers never load this class.
 *
 * Right-click opens the original character inspection screen.
 * K opens the separate LLM dialogue interface.
 */
public final class WinterfellClientUi {

    private WinterfellClientUi() { }

    public static void register() {

        // Preserve the existing right-click character inspection screen.
        ClientPlayNetworking.registerGlobalReceiver(
                KnownWorldUiPackets.CharacterSheet.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().gui.setScreen(
                                new WinterfellCharacterScreen(payload.json())
                        )
                )
        );

        // Open the dedicated dialogue screen after server validation.
        ClientPlayNetworking.registerGlobalReceiver(
                WinterfellConversationPackets.Opened.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().gui.setScreen(
                                new WinterfellDialogueScreen(
                                        payload.session(),
                                        payload.name(),
                                        payload.subtitle(),
                                        payload.entityId()
                                )
                        )
                )
        );

        // Receive a conversation line from the server.
        ClientPlayNetworking.registerGlobalReceiver(
                WinterfellConversationPackets.Line.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().gui.screen()
                            instanceof WinterfellDialogueScreen screen
                            && screen.hasSession(payload.session())) {

                        screen.addLine(
                                payload.speaker(),
                                payload.text()
                        );
                    }
                })
        );

        // Receive dialogue status updates.
        ClientPlayNetworking.registerGlobalReceiver(
                WinterfellConversationPackets.Status.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().gui.screen()
                            instanceof WinterfellDialogueScreen screen
                            && screen.hasSession(payload.session())) {

                        screen.updateStatus(
                                payload.text(),
                                payload.busy()
                        );
                    }
                })
        );

        // Register K as the NPC conversation key.
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(
                        "knownworld",
                        "dialogue"
                )
        );

        KeyMapping talkKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.knownworld.talk_to_npc",
                        InputConstants.Type.KEYBOARD,
                        InputConstants.KEY_K,
                        category
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (talkKey.consumeClick()) {

                if (client.player == null) {
                    continue;
                }

                // Close an existing dialogue screen.
                if (client.gui.screen()
                        instanceof WinterfellDialogueScreen) {

                    client.gui.setScreen(null);

                } else if (client.gui.screen() == null) {

                    // Request a conversation with the targeted entity.
                    if (client.hitResult instanceof EntityHitResult hit) {

                        ClientPlayNetworking.send(
                                new WinterfellConversationPackets.OpenRequest(
                                        hit.getEntity().getId()
                                )
                        );

                    } else {

                        // Minecraft 26.3:
                        // LocalPlayer.displayClientMessage no longer exists.
                        // Display the message through the HUD instead.
                        client.gui.hud.setOverlayMessage(
                                Component.literal(
                                        "Look at a nearby Winterfell NPC and press K to speak."
                                ),
                                false
                        );
                    }
                }
            }
        });
    }
}

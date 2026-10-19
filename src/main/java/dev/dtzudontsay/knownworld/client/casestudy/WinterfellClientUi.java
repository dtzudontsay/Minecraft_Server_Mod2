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

/** Registered by the client entrypoint; dedicated servers never load this class. */
public final class WinterfellClientUi {
    private WinterfellClientUi() { }

    public static void register() {
        // Existing right-click inspector is registered and handled exactly as before.
        ClientPlayNetworking.registerGlobalReceiver(KnownWorldUiPackets.CharacterSheet.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().gui.setScreen(new WinterfellCharacterScreen(payload.json()))));

        ClientPlayNetworking.registerGlobalReceiver(WinterfellConversationPackets.Opened.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().gui.setScreen(new WinterfellDialogueScreen(
                                payload.session(), payload.name(), payload.subtitle()))));

        ClientPlayNetworking.registerGlobalReceiver(WinterfellConversationPackets.Line.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof WinterfellDialogueScreen screen
                            && screen.hasSession(payload.session()))
                        screen.addLine(payload.speaker(), payload.text());
                }));

        ClientPlayNetworking.registerGlobalReceiver(WinterfellConversationPackets.Status.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof WinterfellDialogueScreen screen
                            && screen.hasSession(payload.session()))
                        screen.updateStatus(payload.text(), payload.busy());
                }));

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("knownworld", "dialogue"));
        KeyMapping talkKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.knownworld.talk_to_npc",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_K,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (talkKey.consumeClick()) {
                if (client.player == null) continue;
                if (client.gui.screen() instanceof WinterfellDialogueScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null) {
                    if (client.hitResult instanceof EntityHitResult hit) {
                        ClientPlayNetworking.send(new WinterfellConversationPackets.OpenRequest(
                                hit.getEntity().getId()));
                    } else {
                        client.player.displayClientMessage(Component.literal(
                                "Look at a nearby Winterfell NPC and press K to speak."), true);
                    }
                }
            }
        });
    }
}

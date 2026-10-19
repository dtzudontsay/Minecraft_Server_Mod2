
package dev.dtzudontsay.knownworld.client.atlas;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import dev.dtzudontsay.knownworld.client.casestudy.WinterfellClientUi;

/** Registered only through the client entrypoint in fabric.mod.json. */
public final class KnownWorldAtlasClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WinterfellClientUi.register();

        if (!KnownWorldAtlasConfig.ENABLED) return;

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("knownworld", "atlas"));
        KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.knownworld.open_atlas",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_M,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                if (client.player == null) continue;
                if (client.gui.screen() instanceof KnownWorldAtlasScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null) {
                    client.gui.setScreen(new KnownWorldAtlasScreen());
                }
            }
        });
    }
}

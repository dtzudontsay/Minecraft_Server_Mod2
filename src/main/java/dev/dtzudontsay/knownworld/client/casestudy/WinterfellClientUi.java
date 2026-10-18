package dev.dtzudontsay.knownworld.client.casestudy;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import dev.dtzudontsay.knownworld.simulation.casestudy.KnownWorldUiPackets;

/** Registered by the existing client entrypoint; never loaded on a dedicated server. */
public final class WinterfellClientUi {
    private WinterfellClientUi() { }
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(KnownWorldUiPackets.CharacterSheet.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().gui.setScreen(new WinterfellCharacterScreen(payload.json()))));
    }
}

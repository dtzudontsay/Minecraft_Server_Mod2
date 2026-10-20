
package dev.dtzudontsay.knownworld.simulation.integration;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcBelief;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A4: bounded, local, autonomous propagation of PUBLIC witnessed
 * social events through the existing NPC communication service.
 *
 * No LLM calls are made by this runtime.
 *
 * No private personal beliefs or generic memories are propagated.
 *
 * IMPORTANT:
 * - The event's original eyewitness must already have an A2 belief.
 * - The recipient must not already hold that fact.
 * - One event produces no more than two gossip recipients here.
 * - Existing NpcCommunicationService handles credibility and memory.
 * - The event journal cursor is initialized at the latest existing
 *   event after a server starts, preventing replay on world reload.
 */
public final class KnownWorldPublicRumorRuntime
        implements ModInitializer {

    private static final int SCAN_INTERVAL_TICKS = 100;
    private static final int MAX_EVENTS_PER_PASS = 32;
    private static final int MAX_RECIPIENTS_PER_EVENT = 2;

    private static final double WITNESS_RADIUS = 16.0;
    private static final double GOSSIP_RADIUS = 48.0;

    private static MinecraftServer activeServer;
    private static boolean initialized;
    private static long lastEventId;
    private static long lastScanTick;
    private static long eligibleEvents;
    private static long sharedReports;

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
                KnownWorldPublicRumorRuntime::tick
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {

            if (server == activeServer) {
                reset();
            }
        });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("kwrumor")
                                        .then(
                                                Commands.literal("status")
                                                        .executes(context -> {
                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "[Known World / Rumors] "
                                                                                            + "active=" + initialized
                                                                                            + " | last event="
                                                                                            + lastEventId
                                                                                            + " | eligible actions="
                                                                                            + eligibleEvents
                                                                                            + " | reports shared="
                                                                                            + sharedReports
                                                                            ),
                                                                            false
                                                                    );
                                                            return 1;
                                                        })
                                        )
                        )
        );

        KnownWorld.LOGGER.info(
                "Known World public rumor propagation registered."
        );
    }

    private static void tick(MinecraftServer server) {

        if (activeServer != server) {

            reset();
            activeServer = server;
        }

        NpcSimulation sim = NpcSimulation.getNullable();

        if (sim == null || sim.server() != server) {
            return;
        }

        if (!initialized) {

            for (WorldEvent event : sim.events().all()) {
                lastEventId = Math.max(
                        lastEventId,
                        event.id().value()
                );
            }

            initialized = true;
            lastScanTick = sim.serverTickCounter();

            return;
        }

        long tick = sim.serverTickCounter();

        if (tick - lastScanTick < SCAN_INTERVAL_TICKS) {
            return;
        }

        lastScanTick = tick;

        List<WorldEvent> newEvents = sim.events()
                .all()
                .stream()
                .filter(event -> event.id().value() > lastEventId)
                .sorted(
                        Comparator.comparingLong(
                                event -> event.id().value()
                        )
                )
                .limit(MAX_EVENTS_PER_PASS)
                .toList();

        for (WorldEvent event : newEvents) {

            lastEventId = event.id().value();

            if (!SocialWitnessRuntime.isPublicSocialEvent(event)) {
                continue;
            }

            eligibleEvents++;

            try {

                int count = shareLocally(
                        sim,
                        event,
                        tick
                );

                sharedReports += count;

            } catch (RuntimeException exception) {

                KnownWorld.LOGGER.warn(
                        "Public rumor propagation failed for event {}",
                        event.id().value(),
                        exception
                );
            }
        }
    }

    private static int shareLocally(
            NpcSimulation sim,
            WorldEvent event,
            long tick
    ) {

        String beliefKey = SocialWitnessRuntime.beliefKey(
                event.actorNpc(),
                event.subjectNpc()
        );

        /*
         * Only actual A2 firsthand witnesses are valid
         * potential origin speakers.
         */
        List<NpcState> eyewitnesses = sim.registry()
                .findWithinHorizontalRadius(
                        event.position(),
                        WITNESS_RADIUS
                )
                .stream()
                .filter(NpcState::isAlive)
                .filter(npc ->
                        !npc.id().equals(event.actorNpc())
                                && !npc.id().equals(event.subjectNpc())
                )
                .filter(npc -> {

                    Optional<NpcBelief> belief =
                            sim.knowledge().find(
                                    npc.id(),
                                    beliefKey
                            );

                    return belief.isPresent()
                            && belief.get().sourceNpc() == null
                            && KnownWorldPublicKnowledge.qualifies(
                            belief.get(),
                            event
                    );
                })
                .sorted(
                        Comparator.comparingLong(
                                npc -> npc.id().value()
                        )
                )
                .toList();

        if (eyewitnesses.isEmpty()) {
            return 0;
        }

        /*
         * Public conversation can reach beyond immediate
         * eyewitness range, but not across an entire region.
         *
         * A listener inside the original 16-block range
         * should normally have been a direct witness instead.
         */
        List<NpcState> possibleListeners = sim.registry()
                .findWithinHorizontalRadius(
                        event.position(),
                        GOSSIP_RADIUS
                )
                .stream()
                .filter(NpcState::isAlive)
                .filter(npc ->
                        !npc.id().equals(event.actorNpc())
                                && !npc.id().equals(event.subjectNpc())
                )
                .filter(npc ->
                        npc.position().horizontalDistanceSquared(
                                event.position()
                        ) > WITNESS_RADIUS * WITNESS_RADIUS
                )
                .filter(npc ->
                        sim.knowledge().find(
                                npc.id(),
                                beliefKey
                        ).isEmpty()
                )
                .sorted(
                        Comparator.comparingLong(
                                npc -> npc.id().value()
                        )
                )
                .limit(MAX_RECIPIENTS_PER_EVENT)
                .toList();

        int shared = 0;

        for (NpcState listener : possibleListeners) {

            NpcState speaker = eyewitnesses.get(
                    shared % eyewitnesses.size()
            );

            try {

                /*
                 * The existing service:
                 * 1. Reads the witness's actual belief.
                 * 2. Calculates credibility from relationships.
                 * 3. Saves an attributed HEARD_INFORMATION memory.
                 * 4. Updates familiarity in both directions.
                 */
                sim.communication().shareBelief(
                        speaker.id(),
                        listener.id(),
                        beliefKey,
                        tick
                );

                shared++;

            } catch (IllegalArgumentException ignored) {

                // NPC or belief may have changed before propagation.
            }
        }

        return shared;
    }

    private static void reset() {

        activeServer = null;
        initialized = false;
        lastEventId = 0;
        lastScanTick = 0;
        eligibleEvents = 0;
        sharedReports = 0;
    }
}

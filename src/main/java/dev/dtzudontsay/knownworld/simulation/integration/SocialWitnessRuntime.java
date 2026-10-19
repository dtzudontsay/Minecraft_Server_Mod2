
package dev.dtzudontsay.knownworld.simulation.integration;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Connected Simulation A2: social-event witnesses.
 *
 * Converts selected, objectively recorded social events into
 * subjective records for nearby NPCs.
 *
 * Existing owners:
 * - WorldEventManager: objective history
 * - NpcMemoryManager: personal experiences
 * - NpcKnowledgeManager: individual beliefs
 * - NpcRelationshipManager: actual interpersonal relationships
 *
 * Does not change existing relationships itself:
 * SocialInteractionService already applies those effects.
 *
 * Does not process LLM speech or unverified player claims.
 * Does not reveal private information to conversation models.
 *
 * All mutable state is accessed on the Minecraft server thread.
 */
public final class SocialWitnessRuntime implements ModInitializer {

    private static final int SCAN_INTERVAL_TICKS = 40;
    private static final int MAX_EVENTS_PER_SCAN = 128;
    private static final int MAX_WITNESSES_PER_EVENT = 24;

    private static final double WITNESS_RADIUS = 16.0;

    /**
     * Conservative public-action allowlist.
     *
     * Betrayals are intentionally excluded, since the occurrence
     * of a betrayal does not mean nearby people discovered it.
     */
    private static final Set<String> PUBLIC_ACTION_KEYS = Set.of(
            "social.event.help",
            "social.event.praise",
            "social.event.insult",
            "social.event.threaten"
    );

    private static MinecraftServer activeServer;
    private static boolean initialized;
    private static long lastEventId;
    private static long processedPublicEvents;
    private static long memoryRecordsCreated;
    private static long lastScanTick;

    @Override
    public void onInitialize() {

        ServerTickEvents.END_SERVER_TICK.register(
                SocialWitnessRuntime::tick
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {

            if (activeServer == server) {
                reset();
            }
        });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("kwsocialwitness")
                                        .then(
                                                Commands.literal("status")
                                                        .executes(context ->
                                                                showStatus(
                                                                        context.getSource()
                                                                )
                                                        )
                                        )
                        )
        );

        KnownWorld.LOGGER.info(
                "Connected social witness integration registered"
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

        /*
         * A save may contain thousands of historical events.
         * Those events may already have affected persistent
         * memories and beliefs in earlier sessions.
         *
         * Start observing only events created AFTER activation.
         * Never replay the retained historical event journal
         * merely because the server restarted.
         */
        if (!initialized) {

            for (WorldEvent event : sim.events().all()) {

                lastEventId = Math.max(
                        lastEventId,
                        event.id().value()
                );
            }

            initialized = true;
            lastScanTick = sim.serverTickCounter();

            KnownWorld.LOGGER.info(
                    "Social witness integration initialized "
                            + "after event ID {}",
                    lastEventId
            );

            return;
        }

        long tick = sim.serverTickCounter();

        if (tick - lastScanTick < SCAN_INTERVAL_TICKS) {
            return;
        }

        lastScanTick = tick;

        List<WorldEvent> recent = sim.events()
                .all()
                .stream()
                .filter(event ->
                        event.id().value() > lastEventId
                )
                .sorted(
                        Comparator.comparingLong(
                                event -> event.id().value()
                        )
                )
                .limit(MAX_EVENTS_PER_SCAN)
                .toList();

        for (WorldEvent event : recent) {

            /*
             * Always move past the event, even if it is not
             * an eligible public social action.
             */
            lastEventId = event.id().value();

            if (!isPublicSocialEvent(event)) {
                continue;
            }

            try {

                int observers = observe(
                        sim,
                        event
                );

                processedPublicEvents++;
                memoryRecordsCreated += observers;

            } catch (RuntimeException exception) {

                KnownWorld.LOGGER.error(
                        "Could not process social witnesses "
                                + "for world event {}",
                        event.id().value(),
                        exception
                );
            }
        }
    }

    /**
     * Only events written by the simulation's recognized social
     * interaction path qualify for this first-stage integration.
     *
     * A future visibility/privacy system will replace this coarse
     * event-type allowlist with event-scoped disclosure rules.
     */
    static boolean isPublicSocialEvent(WorldEvent event) {

        return event != null
                && event.type() == WorldEventType.SOCIAL
                && event.actorNpc() != null
                && event.subjectNpc() != null
                && !event.actorNpc().equals(event.subjectNpc())
                && event.factKey() != null
                && PUBLIC_ACTION_KEYS.contains(event.factKey());
    }

    /**
     * Keeps the latest witnessed action for a particular
     * actor-target pair under a stable belief key.
     *
     * This avoids one unbounded belief key per historical event.
     * The personal memory manager retains separately bounded
     * event-specific recollections.
     */
    static String beliefKey(
            NpcId actor,
            NpcId subject
    ) {

        return "social.witness.latest."
                + actor.value()
                + "."
                + subject.value();
    }

    private static int observe(
            NpcSimulation sim,
            WorldEvent event
    ) {

        List<NpcState> candidates =
                sim.registry().findWithinHorizontalRadius(
                        event.position(),
                        WITNESS_RADIUS
                );

        NpcId actor = event.actorNpc();
        NpcId subject = event.subjectNpc();

        List<NpcState> witnesses = candidates
                .stream()
                .filter(NpcState::isAlive)
                .filter(npc -> !npc.id().equals(actor))
                .filter(npc -> !npc.id().equals(subject))
                .sorted(
                        Comparator.comparingLong(
                                npc -> npc.id().value()
                        )
                )
                .limit(MAX_WITNESSES_PER_EVENT)
                .toList();

        for (NpcState witness : witnesses) {

            double distanceSquared =
                    witness.position()
                            .horizontalDistanceSquared(
                                    event.position()
                            );

            double distanceFactor =
                    Math.min(
                            1.0,
                            Math.sqrt(distanceSquared)
                                    / WITNESS_RADIUS
                    );

            double confidence =
                    Math.max(
                            0.60,
                            0.90 - 0.30 * distanceFactor
                    );

            double importance =
                    Math.max(
                            0.15,
                            Math.min(0.65, event.importance())
                    );

            /*
             * This is an observation of an objective,
             * already-validated simulation event.
             *
             * It is NOT generated dialogue, and its belief
             * source is null because the NPC witnessed it
             * rather than hearing it from another NPC.
             */
            sim.knowledge().believe(
                    witness.id(),
                    beliefKey(actor, subject),
                    event.summary(),
                    confidence,
                    null,
                    event.occurredTick()
            );

            /*
             * Personal witness memory.
             *
             * Use PERSONAL_EXPERIENCE, not SOCIAL_INTERACTION:
             * the observer was not the person being insulted,
             * helped, praised or threatened.
             *
             * Avoid giving the witness a false victim memory.
             */
            sim.memories().remember(
                    witness.id(),
                    NpcMemoryType.PERSONAL_EXPERIENCE,
                    "Witnessed: " + event.summary(),
                    importance,
                    actor,
                    "witness.event." + event.id().value(),
                    event.occurredTick()
            );
        }

        return witnesses.size();
    }

    private static int showStatus(
            net.minecraft.commands.CommandSourceStack source
    ) {

        source.sendSuccess(
                () -> Component.literal(
                        "[Known World / Social Witness] "
                                + "active=" + initialized
                                + " | last event=" + lastEventId
                                + " | observed public actions="
                                + processedPublicEvents
                                + " | personal witness records="
                                + memoryRecordsCreated
                                + " | radius="
                                + String.format(
                                Locale.ROOT,
                                "%.0f",
                                WITNESS_RADIUS
                        )
                                + " blocks"
                ),
                false
        );

        return 1;
    }

    private static void reset() {

        activeServer = null;
        initialized = false;
        lastEventId = 0;
        processedPublicEvents = 0;
        memoryRecordsCreated = 0;
        lastScanTick = 0;
    }
}

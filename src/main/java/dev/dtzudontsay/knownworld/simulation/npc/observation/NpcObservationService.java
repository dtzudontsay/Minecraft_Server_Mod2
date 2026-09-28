package dev.dtzudontsay.knownworld.simulation.npc.observation;

import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;

import java.util.List;
import java.util.Objects;

/**
 * Converts objective world events into subjective NPC knowledge
 * and memories.
 */
public final class NpcObservationService {

    private final NpcRegistry registry;

    private final NpcKnowledgeManager knowledge;

    private final NpcMemoryManager memories;

    public NpcObservationService(
            NpcRegistry registry,
            NpcKnowledgeManager knowledge,
            NpcMemoryManager memories
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.knowledge =
                Objects.requireNonNull(
                        knowledge,
                        "knowledge"
                );

        this.memories =
                Objects.requireNonNull(
                        memories,
                        "memories"
                );
    }

    /**
     * Makes every living NPC inside the observation radius witness
     * this event.
     */
    public int observeNearby(
            WorldEvent event,
            double radius
    ) {
        Objects.requireNonNull(
                event,
                "event"
        );

        List<NpcState> nearby =
                registry.findWithinHorizontalRadius(
                        event.position(),
                        radius
                );

        int witnesses =
                0;

        for (NpcState npc : nearby) {

            observe(
                    npc.id(),
                    event
            );

            witnesses++;
        }

        return witnesses;
    }

    /**
     * One NPC directly witnesses an objective event.
     */
    public void observe(
            NpcId observer,
            WorldEvent event
    ) {
        Objects.requireNonNull(
                observer,
                "observer"
        );

        Objects.requireNonNull(
                event,
                "event"
        );

        if (!registry.contains(
                observer
        )) {

            throw new IllegalArgumentException(
                    "Unknown observer NPC ID: "
                            + observer
            );
        }

        memories.remember(
                observer,
                memoryTypeFor(
                        event.type()
                ),
                event.summary(),
                event.importance(),
                relevantNpcFor(
                        observer,
                        event
                ),
                event.factKey(),
                event.occurredTick()
        );

        /*
         * A direct witness gets high-confidence first-hand knowledge.
         *
         * We use 1.0 for now.
         *
         * Later perception, visibility, distance, darkness, disguise,
         * intoxication, confusion, etc. can reduce this.
         */
        if (event.containsFact()) {

            knowledge.believe(
                    observer,
                    event.factKey(),
                    event.factValue(),
                    1.0,
                    null,
                    event.occurredTick()
            );
        }
    }

    private static NpcId relevantNpcFor(
            NpcId observer,
            WorldEvent event
    ) {
        if (event.actorNpc() != null
                && !event.actorNpc()
                .equals(
                        observer
                )) {

            return event.actorNpc();
        }

        if (event.subjectNpc() != null
                && !event.subjectNpc()
                .equals(
                        observer
                )) {

            return event.subjectNpc();
        }

        return null;
    }

    private static NpcMemoryType memoryTypeFor(
            WorldEventType type
    ) {
        return switch (type) {

            case BATTLE,
                 COMBAT ->
                    NpcMemoryType.COMBAT;

            case DEATH,
                 EXECUTION ->
                    NpcMemoryType.DEATH;

            case POLITICAL,
                 ARREST,
                 MARRIAGE ->
                    NpcMemoryType.POLITICAL;

            default ->
                    NpcMemoryType.PERSONAL_EXPERIENCE;
        };
    }
}
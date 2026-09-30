package dev.dtzudontsay.knownworld.simulation.npc.family;

import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.social.SocietyStructureRuntime;

import java.util.Objects;

public final class MarriageService {

    private final NpcRegistry registry;

    private final MarriageManager marriages;

    private final NpcRelationshipManager relationships;

    private final NpcMemoryManager memories;

    private final WorldEventManager events;

    public MarriageService(
            NpcRegistry registry,
            MarriageManager marriages,
            NpcRelationshipManager relationships,
            NpcMemoryManager memories,
            WorldEventManager events
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.marriages =
                Objects.requireNonNull(
                        marriages,
                        "marriages"
                );

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
                );

        this.memories =
                Objects.requireNonNull(
                        memories,
                        "memories"
                );

        this.events =
                Objects.requireNonNull(
                        events,
                        "events"
                );
    }

    public MarriageRecord betroth(
            NpcId first,
            NpcId second,
            DynastyInheritanceRule rule,
            long tick
    ) {

        MarriageRecord record =
                marriages.betroth(
                        first,
                        second,
                        rule,
                        tick
                );

        ensureRelationship(
                first,
                second
        );

        ensureRelationship(
                second,
                first
        );

        memories.remember(
                first,
                NpcMemoryType.POLITICAL,
                "Became betrothed to "
                        + name(
                        second
                )
                        + ".",
                0.75,
                second,
                null,
                tick
        );

        memories.remember(
                second,
                NpcMemoryType.POLITICAL,
                "Became betrothed to "
                        + name(
                        first
                )
                        + ".",
                0.75,
                first,
                null,
                tick
        );

        events.create(
                WorldEventType.SOCIAL,
                name(
                        first
                )
                        + " and "
                        + name(
                        second
                )
                        + " became betrothed.",
                requireNpc(
                        first
                )
                        .position(),
                tick,
                0.55,
                first,
                second,
                null,
                null
        );

        return record;
    }

    public MarriageRecord marry(
            NpcId first,
            NpcId second,
            DynastyInheritanceRule rule,
            long tick
    ) {

        MarriageRecord record =
                marriages.marry(
                        first,
                        second,
                        rule,
                        tick
                );

        NpcRelationship firstToSecond =
                ensureRelationship(
                        first,
                        second
                );

        NpcRelationship secondToFirst =
                ensureRelationship(
                        second,
                        first
                );

        firstToSecond.changeAffection(
                0.10
        );

        firstToSecond.changeTrust(
                0.05
        );

        firstToSecond.increaseFamiliarity(
                0.20
        );

        secondToFirst.changeAffection(
                0.10
        );

        secondToFirst.changeTrust(
                0.05
        );

        secondToFirst.increaseFamiliarity(
                0.20
        );

        memories.remember(
                first,
                NpcMemoryType.PERSONAL_EXPERIENCE,
                "Married "
                        + name(
                        second
                )
                        + ".",
                0.95,
                second,
                null,
                tick
        );

        memories.remember(
                second,
                NpcMemoryType.PERSONAL_EXPERIENCE,
                "Married "
                        + name(
                        first
                )
                        + ".",
                0.95,
                first,
                null,
                tick
        );

        events.create(
                WorldEventType.MARRIAGE,
                name(
                        first
                )
                        + " married "
                        + name(
                        second
                )
                        + ".",
                requireNpc(
                        first
                )
                        .position(),
                tick,
                0.85,
                first,
                second,
                null,
                null
        );

        SocietyStructureRuntime society =
                SocietyStructureRuntime.getNullable();

        if (society != null) {

            society.characterSocialIdentityService()
                    .onMarriage(
                            first,
                            second,
                            rule,
                            tick
                    );
        }

        return record;
    }

    public boolean endCurrentUnion(
            NpcId npc,
            long tick
    ) {

        /*
         * We intentionally do not automatically revert current dynasty
         * or married-into dynasty when a marriage ends.
         *
         * Widows, annulments, repudiation and divorce can all produce
         * different legal/social results. A future marriage-law system
         * must decide those consequences explicitly.
         */
        return marriages.endCurrentUnion(
                npc,
                tick
        );
    }

    private NpcRelationship ensureRelationship(
            NpcId subject,
            NpcId target
    ) {

        return relationships.getOrCreate(
                subject,
                target
        );
    }

    private String name(
            NpcId npc
    ) {

        return requireNpc(
                npc
        )
                .identity()
                .fullName();
    }

    private NpcState requireNpc(
            NpcId npc
    ) {

        return registry.find(
                        npc
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC ID: "
                                                + npc
                                )
                );
    }
}
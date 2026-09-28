package dev.dtzudontsay.knownworld.simulation;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Determines how much simulation detail each NPC currently needs.
 *
 * IMPORTANT:
 *
 * PHYSICAL is intentionally NOT assigned here yet.
 *
 * An NPC only becomes PHYSICAL once we have an actual physical
 * projection/entity system.
 */
public final class NpcActivationManager {

    /**
     * NPCs this close to a player receive high-detail simulation.
     */
    public static final double DETAILED_RADIUS =
            128.0;

    /**
     * NPCs outside detailed range but inside this radius continue
     * participating in abstract simulation.
     */
    public static final double ABSTRACT_RADIUS =
            2048.0;

    private final NpcRegistry registry;

    /*
     * Tracks NPCs that were active during the previous evaluation.
     *
     * This means we do not need to iterate over every NPC in the
     * world merely to return distant NPCs to DORMANT.
     */
    private final Set<NpcId> previouslyActive =
            new HashSet<>();

    public NpcActivationManager(
            NpcRegistry registry
    ) {
        this.registry =
                registry;
    }

    public void update(
            MinecraftServer server
    ) {
        /*
         * First deactivate only NPCs that were active during the
         * previous activation pass.
         */
        for (
                NpcId id :
                previouslyActive
        ) {
            registry.find(id)
                    .ifPresent(
                            npc ->
                                    npc.setSimulationLevel(
                                            SimulationLevel.DORMANT
                                    )
                    );
        }

        previouslyActive.clear();

        /*
         * Each player creates two simulation zones:
         *
         * 0 - 128 blocks:
         *      DETAILED
         *
         * 128 - 2048 blocks:
         *      ABSTRACT
         *
         * beyond:
         *      DORMANT
         */
        for (
                ServerPlayer player :
                server.getPlayerList()
                        .getPlayers()
        ) {
            SimulationPosition playerPosition =
                    positionOf(
                            player
                    );

            for (
                    NpcState npc :
                    registry.findWithinHorizontalRadius(
                            playerPosition,
                            ABSTRACT_RADIUS
                    )
            ) {
                npc.setSimulationLevel(
                        SimulationLevel.ABSTRACT
                );

                previouslyActive.add(
                        npc.id()
                );
            }

            /*
             * Detailed pass runs second so DETAILED wins whenever
             * an NPC belongs to both ranges.
             */
            for (
                    NpcState npc :
                    registry.findWithinHorizontalRadius(
                            playerPosition,
                            DETAILED_RADIUS
                    )
            ) {
                npc.setSimulationLevel(
                        SimulationLevel.DETAILED
                );

                previouslyActive.add(
                        npc.id()
                );
            }
        }
    }

    public int activeCount() {
        return previouslyActive.size();
    }

    private static SimulationPosition positionOf(
            ServerPlayer player
    ) {
        Vec3 position =
                player.position();

        String dimension =
                player.level()
                        .dimension()
                        .identifier()
                        .toString();

        return new SimulationPosition(
                dimension,
                position.x,
                position.y,
                position.z
        );
    }
}
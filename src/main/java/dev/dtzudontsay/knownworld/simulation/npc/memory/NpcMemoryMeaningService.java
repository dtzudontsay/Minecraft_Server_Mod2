package dev.dtzudontsay.knownworld.simulation.npc.memory;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.List;
import java.util.Objects;

/**
 * Converts persistent factual memory records into decision-relevant meaning.
 *
 * The memory itself remains a generic persistent record.
 *
 * This service interprets standardized semantic fact keys such as:
 *
 * social.action.help
 * social.action.praise
 * social.action.insult
 * social.action.threaten
 * social.action.betray
 *
 * Future systems can add further semantic namespaces without changing the
 * core NpcMemory record:
 *
 * combat.saved_life
 * family.parent_died
 * politics.granted_title
 * politics.revoked_title
 * war.defeated_in_battle
 * crime.murdered_kin
 * economy.paid_debt
 *
 * Old memories without semantic fact keys remain valid; they simply carry
 * salience without a known positive/negative interpretation.
 */
public final class NpcMemoryMeaningService {

    public static final String SOCIAL_HELP =
            "social.action.help";

    public static final String SOCIAL_PRAISE =
            "social.action.praise";

    public static final String SOCIAL_INSULT =
            "social.action.insult";

    public static final String SOCIAL_THREATEN =
            "social.action.threaten";

    public static final String SOCIAL_BETRAY =
            "social.action.betray";

    private final NpcMemoryManager memories;

    public NpcMemoryMeaningService(
            NpcMemoryManager memories
    ) {

        this.memories =
                Objects.requireNonNull(
                        memories,
                        "memories"
                );
    }

    /**
     * Builds the remembered meaning of one other NPC from the owner's
     * persistent memories.
     */
    public Meaning toward(
            NpcId owner,
            NpcId relatedNpc,
            long currentTick
    ) {

        Objects.requireNonNull(
                owner,
                "owner"
        );

        Objects.requireNonNull(
                relatedNpc,
                "relatedNpc"
        );

        List<NpcMemory> ownerMemories =
                memories.memoriesOf(
                        owner
                );

        double positive =
                0.0;

        double negative =
                0.0;

        double betrayal =
                0.0;

        double threat =
                0.0;

        double total =
                0.0;

        int relevant =
                0;

        for (
                NpcMemory memory :
                ownerMemories
        ) {

            if (memory.relatedNpc() == null
                    ||
                    !memory.relatedNpc()
                            .equals(
                                    relatedNpc
                            )) {

                continue;
            }

            relevant++;

            double salience =
                    effectiveSalience(
                            memory,
                            currentTick
                    );

            total +=
                    salience;

            MeaningContribution contribution =
                    interpret(
                            memory
                    );

            if (contribution.valence()
                    > 0.0) {

                positive +=
                        salience
                                * contribution.valence();

            } else if (contribution.valence()
                    < 0.0) {

                negative +=
                        salience
                                * -contribution.valence();
            }

            betrayal +=
                    salience
                            * contribution.betrayal();

            threat +=
                    salience
                            * contribution.threat();
        }

        return new Meaning(
                clampUnit(
                        positive
                ),
                clampUnit(
                        negative
                ),
                clampUnit(
                        betrayal
                ),
                clampUnit(
                        threat
                ),
                clampUnit(
                        total / 3.0
                ),
                relevant
        );
    }

    /**
     * Semantic interpretation of an individual memory.
     */
    public MeaningContribution interpret(
            NpcMemory memory
    ) {

        Objects.requireNonNull(
                memory,
                "memory"
        );

        String factKey =
                memory.factKey();

        if (factKey == null) {

            return MeaningContribution.NEUTRAL;
        }

        return switch (
                factKey
                ) {

            case SOCIAL_HELP ->
                    new MeaningContribution(
                            0.85,
                            0.0,
                            0.0
                    );

            case SOCIAL_PRAISE ->
                    new MeaningContribution(
                            0.55,
                            0.0,
                            0.0
                    );

            case SOCIAL_INSULT ->
                    new MeaningContribution(
                            -0.55,
                            0.0,
                            0.05
                    );

            case SOCIAL_THREATEN ->
                    new MeaningContribution(
                            -0.85,
                            0.0,
                            0.90
                    );

            case SOCIAL_BETRAY ->
                    new MeaningContribution(
                            -1.0,
                            1.0,
                            0.20
                    );

            default ->
                    MeaningContribution.NEUTRAL;
        };
    }

    /**
     * Importance remains the primary weight.
     *
     * Recency fades the immediate behavioral impact, but even an old major
     * event retains part of its salience.
     */
    private static double effectiveSalience(
            NpcMemory memory,
            long currentTick
    ) {

        long age =
                Math.max(
                        0L,
                        currentTick
                                - memory.createdTick()
                );

        double recency =
                1.0
                        /
                        (
                                1.0
                                        +
                                        age
                                                / 24000.0
                        );

        return clampUnit(
                memory.importance()
                        *
                        (
                                0.45
                                        +
                                        recency
                                                * 0.55
                        )
        );
    }

    private static double clampUnit(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    public record Meaning(
            double positive,
            double negative,
            double betrayal,
            double threat,
            double totalSalience,
            int relevantMemories
    ) {

        public double signedValence() {

            return Math.max(
                    -1.0,
                    Math.min(
                            1.0,
                            positive
                                    - negative
                    )
            );
        }
    }

    public record MeaningContribution(
            double valence,
            double betrayal,
            double threat
    ) {

        public static final MeaningContribution NEUTRAL =
                new MeaningContribution(
                        0.0,
                        0.0,
                        0.0
                );

        public MeaningContribution {

            if (!Double.isFinite(
                    valence
            )
                    ||
                    valence < -1.0
                    ||
                    valence > 1.0) {

                throw new IllegalArgumentException(
                        "Memory valence must be between -1 and 1"
                );
            }

            if (!Double.isFinite(
                    betrayal
            )
                    ||
                    betrayal < 0.0
                    ||
                    betrayal > 1.0) {

                throw new IllegalArgumentException(
                        "Memory betrayal must be between 0 and 1"
                );
            }

            if (!Double.isFinite(
                    threat
            )
                    ||
                    threat < 0.0
                    ||
                    threat > 1.0) {

                throw new IllegalArgumentException(
                        "Memory threat must be between 0 and 1"
                );
            }
        }
    }
}
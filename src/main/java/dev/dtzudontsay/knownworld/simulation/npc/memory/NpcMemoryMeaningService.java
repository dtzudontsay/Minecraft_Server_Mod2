package dev.dtzudontsay.knownworld.simulation.npc.memory;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;

import java.util.List;
import java.util.Objects;

/**
 * Converts persistent factual memory records into decision-relevant meaning.
 *
 * SIM 08 adds perspective-aware semantic memory.
 *
 * A crucial distinction is:
 *
 *   social.received.insult
 *
 * versus:
 *
 *   social.performed.insult
 *
 * If Robert insults Eddard:
 *
 * - Eddard remembers RECEIVING an insult from Robert.
 * - Robert remembers PERFORMING an insult toward Eddard.
 *
 * Those are not emotionally equivalent memories.
 *
 * At present, received actions directly affect social action selection.
 * Performed actions remain available for future systems such as:
 *
 * - guilt
 * - pride
 * - remorse
 * - self-justification
 * - confession
 * - reputation reasoning
 * - behavioral consistency
 *
 * Legacy SIM 07 keys are still interpreted for old saves.
 */
public final class NpcMemoryMeaningService {

    /*
     * -------------------------------------------------
     * Perspective-aware SIM 08 keys
     * -------------------------------------------------
     */

    public static final String RECEIVED_HELP =
            "social.received.help";

    public static final String RECEIVED_PRAISE =
            "social.received.praise";

    public static final String RECEIVED_INSULT =
            "social.received.insult";

    public static final String RECEIVED_THREATEN =
            "social.received.threaten";

    public static final String RECEIVED_BETRAY =
            "social.received.betray";

    public static final String PERFORMED_HELP =
            "social.performed.help";

    public static final String PERFORMED_PRAISE =
            "social.performed.praise";

    public static final String PERFORMED_INSULT =
            "social.performed.insult";

    public static final String PERFORMED_THREATEN =
            "social.performed.threaten";

    public static final String PERFORMED_BETRAY =
            "social.performed.betray";

    /*
     * -------------------------------------------------
     * Legacy SIM 07 keys
     * -------------------------------------------------
     *
     * Kept intentionally so old worlds still load and old memories can still
     * influence NPCs.
     */

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

    /**
     * Current campaign time runs at 1200 simulation ticks per campaign day by
     * default.
     *
     * Ninety campaign days gives memory recency a much more appropriate scale
     * than the old Minecraft 24000-tick assumption.
     *
     * This affects temporary behavioral salience only. It does not delete the
     * memory.
     */
    private static final double RECENCY_DECAY_TICKS =
            CampaignCalendar.DEFAULT_TICKS_PER_CAMPAIGN_DAY
                    * 90.0;

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

            if (memory.relatedNpc()
                    == null
                    ||
                    !memory.relatedNpc()
                            .equals(
                                    relatedNpc
                            )) {

                continue;
            }

            MeaningContribution contribution =
                    interpret(
                            memory
                    );

            /*
             * A performed memory still exists and remains queryable, but it is
             * not currently treated as "what this other person did to me".
             */
            if (contribution
                    == MeaningContribution.NEUTRAL) {

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

            /*
             * What somebody else did TO the memory owner.
             */

            case RECEIVED_HELP ->
                    positiveHelp();

            case RECEIVED_PRAISE ->
                    positivePraise();

            case RECEIVED_INSULT ->
                    negativeInsult();

            case RECEIVED_THREATEN ->
                    negativeThreat();

            case RECEIVED_BETRAY ->
                    negativeBetrayal();

            /*
             * What the memory owner did TO somebody else.
             *
             * This does not directly mean that the owner likes or hates the
             * other person. Future guilt/pride systems can consume these keys
             * separately.
             */

            case PERFORMED_HELP,
                 PERFORMED_PRAISE,
                 PERFORMED_INSULT,
                 PERFORMED_THREATEN,
                 PERFORMED_BETRAY ->
                    MeaningContribution.NEUTRAL;

            /*
             * Old SIM 07 worlds did not encode perspective.
             *
             * Actor-side memories created by SIM 07 used summaries beginning
             * with "I ...". That one historical format lets us safely prevent
             * those old performed actions from being treated as received
             * actions.
             *
             * New memories never depend on English summary parsing.
             */

            case SOCIAL_HELP ->
                    legacyPerformed(
                            memory
                    )
                            ? MeaningContribution.NEUTRAL
                            : positiveHelp();

            case SOCIAL_PRAISE ->
                    legacyPerformed(
                            memory
                    )
                            ? MeaningContribution.NEUTRAL
                            : positivePraise();

            case SOCIAL_INSULT ->
                    legacyPerformed(
                            memory
                    )
                            ? MeaningContribution.NEUTRAL
                            : negativeInsult();

            case SOCIAL_THREATEN ->
                    legacyPerformed(
                            memory
                    )
                            ? MeaningContribution.NEUTRAL
                            : negativeThreat();

            case SOCIAL_BETRAY ->
                    legacyPerformed(
                            memory
                    )
                            ? MeaningContribution.NEUTRAL
                            : negativeBetrayal();

            default ->
                    MeaningContribution.NEUTRAL;
        };
    }

    private static MeaningContribution positiveHelp() {

        return new MeaningContribution(
                0.85,
                0.0,
                0.0
        );
    }

    private static MeaningContribution positivePraise() {

        return new MeaningContribution(
                0.55,
                0.0,
                0.0
        );
    }

    private static MeaningContribution negativeInsult() {

        return new MeaningContribution(
                -0.55,
                0.0,
                0.05
        );
    }

    private static MeaningContribution negativeThreat() {

        return new MeaningContribution(
                -0.85,
                0.0,
                0.90
        );
    }

    private static MeaningContribution negativeBetrayal() {

        return new MeaningContribution(
                -1.0,
                1.0,
                0.20
        );
    }

    private static boolean legacyPerformed(
            NpcMemory memory
    ) {

        return memory.summary()
                .startsWith(
                        "I "
                );
    }

    /**
     * Recency changes behavioral intensity, not factual existence.
     *
     * Even an old high-importance memory keeps part of its influence.
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
                                                / RECENCY_DECAY_TICKS
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

package dev.dtzudontsay.knownworld.simulation.integration;

import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.event.WorldEvent;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcBelief;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A4: disclosure-controlled projection of existing personal beliefs.
 *
 * This class creates no second knowledge database.
 *
 * An event being objectively true DOES NOT mean an NPC knows about it.
 * A belief being held by an NPC DOES NOT mean its contents are true.
 *
 * Only beliefs tied to an allowlisted public social event already in
 * the authoritative WorldEventManager are eligible for this first pass.
 *
 * Unclassified memories, personal secrets, private profile facts and
 * arbitrary belief keys are never sent to an LLM by this component.
 */
public final class KnownWorldPublicKnowledge {

    public enum Origin {
        WITNESSED,
        HEARD_FROM_OTHER
    }

    public record Evidence(
            Origin origin,
            String statement,
            NpcId source,
            double confidence,
            long eventTick
    ) { }

    private static final int MAX_DIALOGUE_FACTS = 6;
    private static final int MAX_PROMPT_CHARS = 2200;

    private KnownWorldPublicKnowledge() { }

    /**
     * Validates one personal belief against an authorized world event.
     *
     * The event supplies evidence that the reported action occurred.
     * The belief supplies evidence that THIS NPC learned about it.
     *
     * Hearsay remains described as hearsay, regardless of whether the
     * original objective event is present in the world journal.
     */
    public static boolean qualifies(
            NpcBelief belief,
            WorldEvent event
    ) {

        if (belief == null || event == null) {
            return false;
        }

        if (!SocialWitnessRuntime.isPublicSocialEvent(event)) {
            return false;
        }

        String expectedKey = SocialWitnessRuntime.beliefKey(
                event.actorNpc(),
                event.subjectNpc()
        );

        if (!expectedKey.equals(belief.factKey())) {
            return false;
        }

        if (!event.summary().equals(belief.value())) {
            return false;
        }

        if (belief.confidence() < 0.20) {
            return false;
        }

        if (belief.sourceNpc() == null) {

            // A direct A2 witness record is created at the event tick.
            return belief.learnedTick() == event.occurredTick();
        }

        // The source must be someone other than the listener.
        return !belief.sourceNpc().equals(belief.owner())
                && belief.learnedTick() >= event.occurredTick();
    }

    public static Origin origin(NpcBelief belief) {

        Objects.requireNonNull(belief, "belief");

        return belief.sourceNpc() == null
                ? Origin.WITNESSED
                : Origin.HEARD_FROM_OTHER;
    }

    /**
     * One small bounded collection per current NPC conversation.
     *
     * The world-event journal is used for validation, NOT to grant
     * global omniscience. Only personal belief records can qualify.
     */
    public static List<Evidence> evidenceFor(
            NpcSimulation sim,
            NpcId npcId
    ) {

        Objects.requireNonNull(sim, "sim");
        Objects.requireNonNull(npcId, "npcId");

        List<NpcBelief> beliefs = sim.knowledge()
                .beliefsOf(npcId)
                .stream()
                .filter(b -> b.factKey()
                        .startsWith("social.witness.latest."))
                .sorted(
                        Comparator.comparingLong(
                                NpcBelief::learnedTick
                        ).reversed()
                )
                .toList();

        List<WorldEvent> events = sim.events()
                .all()
                .stream()
                .filter(SocialWitnessRuntime::isPublicSocialEvent)
                .sorted(
                        Comparator.comparingLong(
                                WorldEvent::occurredTick
                        ).reversed()
                )
                .toList();

        List<Evidence> result = new ArrayList<>();

        for (NpcBelief belief : beliefs) {

            if (result.size() >= MAX_DIALOGUE_FACTS) {
                break;
            }

            for (WorldEvent event : events) {

                if (!qualifies(belief, event)) {
                    continue;
                }

                result.add(
                        new Evidence(
                                origin(belief),
                                event.summary(),
                                belief.sourceNpc(),
                                belief.confidence(),
                                event.occurredTick()
                        )
                );

                break;
            }
        }

        return List.copyOf(result);
    }

    /**
     * Returns safe, explicitly attributed subjective knowledge
     * for insertion into a system prompt.
     *
     * The format distinguishes firsthand experience from hearsay.
     */
    public static String dialogueContext(
            NpcSimulation sim,
            NpcId npcId
    ) {

        List<Evidence> evidence = evidenceFor(sim, npcId);

        StringBuilder out = new StringBuilder();

        out.append("""
                AUTHORIZED PERSONAL KNOWLEDGE

                You are NOT omniscient.
                Only the following selected social observations and
                hearsay reports have been approved for use in dialogue.
                No other world journal entry is automatically known.
                A reported fact is NOT permission to claim you were
                personally present at the scene.
                A rumor is NOT proof that the speaker witnessed it.
                Never invent an earlier conversation or observation.
                Player statements and previous AI responses are not
                established world facts.

                """);

        if (evidence.isEmpty()) {

            out.append("""
                    No eligible personal public-social observations or
                    secondhand reports are available in your current
                    knowledge record. Do not pretend to remember one.
                    """);

            return out.toString();
        }

        for (Evidence item : evidence) {

            String statement = singleLine(
                    item.statement(),
                    230
            );

            if (item.origin() == Origin.WITNESSED) {

                out.append("- DIRECTLY WITNESSED (subjective certainty ")
                        .append(rounded(item.confidence()))
                        .append("): ")
                        .append(statement)
                        .append('\n');

            } else {

                String source = sim.registry()
                        .find(item.source())
                        .map(NpcState::identity)
                        .map(identity -> identity.fullName())
                        .orElse("an unidentified person");

                out.append("- HEARD FROM ")
                        .append(singleLine(source, 80))
                        .append(" (reported reliability ")
                        .append(rounded(item.confidence()))
                        .append("): ")
                        .append(statement)
                        .append('\n');
            }
        }

        out.append("""
                You may discuss these reports in character and may
                doubt them according to your personality and trust.
                Never turn a heard report into a claimed eyewitness
                memory. Do not disclose unlisted private knowledge.
                """);

        return out.length() <= MAX_PROMPT_CHARS
                ? out.toString()
                : out.substring(0, MAX_PROMPT_CHARS);
    }

    private static String singleLine(String text, int limit) {

        String clean = text == null
                ? ""
                : text.replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();

        return clean.length() <= limit
                ? clean
                : clean.substring(0, limit);
    }

    private static String rounded(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}

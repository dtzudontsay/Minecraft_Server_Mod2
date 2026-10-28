package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellDialogueGuard;
import java.util.regex.Pattern;

/**
 * Conservative speech gate for the worldwide B4 population.
 * This does not claim to solve general LLM hallucination. In particular, an
 * absence of event evidence must NEVER turn generated speech into a world fact.
 */
public final class KnownWorldB41DialogueTruth {
    private static final Pattern FABRICATED_PLAYER_WITNESS = Pattern.compile(
            "\\b(?:I|we)\\s+(?:(?:personally\\s+)?(?:saw|watched|witnessed|observed)\\s+(?:you|your)\\b|"
            + "(?:remember|recall)\\s+(?:you|your)\\s+(?:killing|stealing|attacking|murdering|entering)\\b)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FABRICATED_SHARED_PAST = Pattern.compile(
            "\\b(?:we\\s+(?:met|spoke|talked|fought|travelled|traveled)\\s+(?:before|last|yesterday|earlier)|"
            + "I\\s+(?:met|spoke\\s+with|talked\\s+to)\\s+you\\s+(?:before|last|yesterday|earlier))\\b",
            Pattern.CASE_INSENSITIVE);

    public record Decision(boolean accepted, String reply, String reason) { }
    private KnownWorldB41DialogueTruth() { }

    public static Decision review(String npcId, String proposed, String currentMonarch,
                                  boolean hasPriorPlayerConversation) {
        String text = proposed == null ? "" : proposed.strip();
        if (text.isEmpty()) return reject("empty model reply");
        if (FABRICATED_PLAYER_WITNESS.matcher(text).find())
            return reject("unsupported firsthand player observation");
        if (!hasPriorPlayerConversation && FABRICATED_SHARED_PAST.matcher(text).find())
            return reject("invented prior meeting");
        WinterfellDialogueGuard.Decision guard = WinterfellDialogueGuard.review(npcId, text, currentMonarch);
        if (!guard.accepted()) return reject("historical/privacy guard: " + guard.reason());
        return new Decision(true, text, "");
    }
    private static Decision reject(String reason) {
        return new Decision(false,
                "Forgive me, I spoke without knowing. I cannot claim to have witnessed that.", reason);
    }
}

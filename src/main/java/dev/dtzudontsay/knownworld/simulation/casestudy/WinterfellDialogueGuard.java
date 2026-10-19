
package dev.dtzudontsay.knownworld.simulation.casestudy;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deterministic guard against a small set of high-impact
 * historical contradictions and private revelations.
 *
 * This is NOT intended to replace the NPC knowledge system.
 * It does not claim to recognize every possible hallucination.
 *
 * Rejected dialogue must not be added to conversation history.
 */
public final class WinterfellDialogueGuard {

    public record Decision(
            boolean accepted,
            String reply,
            String reason
    ) { }

    private static final Pattern FALSE_KING =
            Pattern.compile(
                    "\\bking\\s+(?:aemon|ramon)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern FALSE_HOUSE =
            Pattern.compile(
                    "\\bhouse\\s+snow\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern FALSE_FAMILY =
            Pattern.compile(
                    "\\b(?:ramon\\s+(?:tully|snow|arryn)"
                            + "|aemon\\s+snow)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern LYANNAS_CHILDBIRTH =
            Pattern.compile(
                    "\\blyanna\\b[^.!?]{0,170}"
                            + "\\b(?:gave\\s+birth|giving\\s+birth"
                            + "|bore\\s+(?:a\\s+)?(?:son|child)"
                            + "|mother\\s+of\\s+jon"
                            + "|jon\\s+snow)\\b"
                            + "|\\b(?:gave\\s+birth|giving\\s+birth"
                            + "|bore\\s+(?:a\\s+)?(?:son|child))\\b"
                            + "[^.!?]{0,170}\\blyanna\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern HIDDEN_PARENTAGE =
            Pattern.compile(
                    "\\b(?:jon\\s+snow|lyanna\\s+stark)\\b"
                            + "[^.!?]{0,180}"
                            + "\\b(?:rhaegar|targaryen\\s+blood"
                            + "|true\\s+parentage|real\\s+father)\\b"
                            + "|\\brhaegar\\b[^.!?]{0,180}"
                            + "\\b(?:jon\\s+snow|lyanna\\s+stark)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private WinterfellDialogueGuard() { }

    /**
     * Validate a proposed NPC utterance.
     *
     * The fallback is an in-character response and is safe
     * to add to session history.
     */
    public static Decision review(
            String authoredId,
            String rawReply,
            String currentMonarch
    ) {

        String reply = rawReply == null ? "" : rawReply.trim();

        if (reply.isBlank()) {

            return reject(
                    "empty reply",
                    "I seem to have lost my words. "
                            + "What was it you wished to ask?"
            );
        }

        if (FALSE_KING.matcher(reply).find()
                || FALSE_HOUSE.matcher(reply).find()
                || FALSE_FAMILY.matcher(reply).find()) {

            return reject(
                    "unsupported name or title",
                    "Forgive me, I spoke carelessly. "
                            + "House Stark holds Winterfell. "
                            + monarchSentence(currentMonarch)
            );
        }

        if (LYANNAS_CHILDBIRTH.matcher(reply).find()
                || HIDDEN_PARENTAGE.matcher(reply).find()) {

            return reject(
                    "protected lineage disclosure",
                    "Some matters of family are private, "
                            + "and I will not speak of them "
                            + "to someone I scarcely know."
            );
        }

        String lower = reply.toLowerCase(Locale.ROOT);

        /*
         * Only enforce the status of Robert if the live
         * title system confirms he currently holds kingship.
         */
        if ("robert baratheon".equalsIgnoreCase(currentMonarch)) {

            boolean falseDeath =
                    lower.contains("robert baratheon is dead")
                            || lower.contains("robert baratheon has died")
                            || lower.contains("king robert is dead")
                            || lower.contains("king robert has died")
                            || lower.contains("robert is long dead")
                            || lower.contains("robert is long gone and")
                            || lower.contains("king robert is long gone");

            boolean falseSuccessor =
                    lower.contains("his son now wears the crown")
                            || lower.contains("his son is king now")
                            || lower.contains("joffrey is king")
                            || lower.contains("joffrey wears the crown");

            if (falseDeath || falseSuccessor) {

                return reject(
                        "contradiction of current royal title",
                        "King Robert Baratheon still holds "
                                + "the Iron Throne, so far as "
                                + "the realm's present order stands."
                );
            }
        }

        return new Decision(true, reply, "");
    }

    private static String monarchSentence(String currentMonarch) {

        if (currentMonarch == null || currentMonarch.isBlank()) {
            return "I will not invent the name of a king.";
        }

        return currentMonarch + " holds the royal title.";
    }

    private static Decision reject(
            String reason,
            String replacement
    ) {

        return new Decision(
                false,
                replacement,
                reason
        );
    }
}

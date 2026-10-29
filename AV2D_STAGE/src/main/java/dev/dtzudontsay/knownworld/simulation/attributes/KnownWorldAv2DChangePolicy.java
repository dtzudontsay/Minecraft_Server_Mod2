package dev.dtzudontsay.knownworld.simulation.attributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/**
 * AV2D: deterministic, fail-closed identity and numeric rules for completed,
 * externally verified outcomes. Observed presence is NOT a completed outcome.
 * No outcomes are automatically produced by this class or by AV2D.
 */
public final class KnownWorldAv2DChangePolicy {
    public static final String SCHEMA = "knownworld.av2d.confirmed_attribute_changes.v1";
    public static final int MAX_RECEIPTS_PER_PERSON = 512;
    public static final double MAX_ABSOLUTE_CHANGE = 0.025;
    public static final Set<String> DOMAINS = Set.of(
        "CHARACTER_SKILL", "NATURAL_APTITUDE", "DISPOSITION",
        "CHARACTER_VALUE", "SOCIAL_NORM");
    public static final Set<String> AUTHORITIES = Set.of(
        "VERIFIED_COMPLETED_PRACTICE", "VERIFIED_FORMATION_OUTCOME",
        "VERIFIED_SOCIAL_OUTCOME");
    private KnownWorldAv2DChangePolicy() { }

    /** This is an intent, not an authority certificate. Caller must supply proof. */
    public record Intent(String personId, String domain, String attribute,
                         double delta, long campaignDay, String authority,
                         String sourceEventId) {
        public Intent {
            siteOf(personId);
            if (domain == null || !DOMAINS.contains(domain))
                throw new IllegalArgumentException("Unsupported writable domain");
            if (attribute == null || !attribute.matches("[A-Z][A-Z0-9_]{0,80}"))
                throw new IllegalArgumentException("Attribute must be canonical enum ID");
            if (!Double.isFinite(delta) || Math.abs(delta) > MAX_ABSOLUTE_CHANGE || delta == 0.0)
                throw new IllegalArgumentException("Invalid event change magnitude");
            if (campaignDay < 0) throw new IllegalArgumentException("Negative campaign day");
            if (authority == null || !AUTHORITIES.contains(authority))
                throw new IllegalArgumentException("Unapproved outcome authority");
            if (sourceEventId == null || !sourceEventId.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("Missing canonical source event receipt");
        }
        public String receiptId() {
            // Event outcome identity excludes the delta: replaying with a different
            // amount cannot create another receipt for the same evidence + axis.
            return sha256("av2d-v1\u0000"+personId+"\u0000"+authority+"\u0000"+
                          sourceEventId+"\u0000"+domain+"\u0000"+attribute);
        }
    }

    /** Must be implemented by the actual source subsystem, not by a UI/LLM. */
    @FunctionalInterface
    public interface VerifiedOutcomeSource {
        boolean confirmsCompletedOutcome(Intent intent);
    }

    public static String siteOf(String personId) {
        if (personId == null || personId.length() > 180 ||
            !personId.matches("b4:site_[a-z][a-z0-9_]{1,90}:[A-Za-z0-9:_-]+"))
            throw new IllegalArgumentException("Invalid canonical B4 person ID");
        return personId.substring(3, personId.indexOf(':',3));
    }
    public static String profileField(String domain) {
        return switch (domain) {
            case "CHARACTER_SKILL" -> "skills";
            case "NATURAL_APTITUDE" -> "aptitudes";
            case "DISPOSITION" -> "dispositions";
            case "CHARACTER_VALUE" -> "characterValues";
            case "SOCIAL_NORM" -> "socialNorms";
            default -> throw new IllegalArgumentException("Unknown profile domain: " + domain);
        };
    }
    public static String profileKey(String domain,String canonical) {
        if ("NATURAL_APTITUDE".equals(domain) && "LANGUAGES".equals(canonical))
            return "language"; // Existing B4 projection calls this dimension language.
        return canonical.toLowerCase(Locale.ROOT);
    }
    public static double applyBounded(String domain,double before,double delta) {
        if (!DOMAINS.contains(domain) || !Double.isFinite(before) || !Double.isFinite(delta))
            throw new IllegalArgumentException("Invalid profile value");
        double lo = ("CHARACTER_SKILL".equals(domain) || "NATURAL_APTITUDE".equals(domain)) ? 0 : -1;
        if (before < lo || before > 1) throw new IllegalArgumentException("Invalid starting value");
        return Math.max(lo,Math.min(1,before+delta));
    }
    public static String sha256(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch (NoSuchAlgorithmException ex) {throw new IllegalStateException("SHA-256 unavailable",ex);}
    }
}

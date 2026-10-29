package dev.dtzudontsay.knownworld.simulation.attributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * AV2C: the exact eligibility and provenance contract for server-confirmed B4
 * experiences. This stateless policy does NOT create mutable skills or events.
 */
public final class KnownWorldAv2CEventPolicy {
    public static final String KIND_WORK_PRESENCE = "WORKPLACE_PRESENCE";
    public static final String SOURCE_WORK_PRESENCE = "B4_2B_PHYSICAL_WORK_ARRIVAL";
    public static final int RECENT_LIMIT = 32;

    private KnownWorldAv2CEventPolicy() { }

    public record Evidence(String eventId, String personId, String siteId,
                           String kind, String source, String workplaceId,
                           long campaignDay) {
        public Evidence {
            Objects.requireNonNull(eventId);
            Objects.requireNonNull(personId);
            Objects.requireNonNull(siteId);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(source);
            Objects.requireNonNull(workplaceId);
            validateSubject(siteId, personId);
            if (!KIND_WORK_PRESENCE.equals(kind) || !SOURCE_WORK_PRESENCE.equals(source))
                throw new IllegalArgumentException("Unsupported AV2C evidence kind/source");
            if (!workplaceId.startsWith(siteId + ":work:") || workplaceId.length() > 180 ||
                !workplaceId.matches("[A-Za-z0-9:_-]+"))
                throw new IllegalArgumentException("Foreign or invalid workplace");
            if (campaignDay < 0) throw new IllegalArgumentException("Negative campaign day");
            if (!eventId.equals(KnownWorldAv2CEventPolicy.eventId(personId, kind, source, workplaceId, campaignDay)))
                throw new IllegalArgumentException("AV2C evidence ID mismatch");
        }
    }

    public static String siteOf(String personId) {
        if (personId == null || personId.length() > 180 ||
            !personId.matches("b4:site_[a-z][a-z0-9_]{1,90}:[A-Za-z0-9:_-]+"))
            throw new IllegalArgumentException("Not a canonical B4 person ID");
        return personId.substring(3, personId.indexOf(':', 3));
    }

    public static void validateSubject(String siteId, String personId) {
        if (siteId == null || !siteId.matches("site_[a-z][a-z0-9_]{1,90}") ||
            !siteId.equals(siteOf(personId)))
            throw new IllegalArgumentException("Cross-site AV2C person ID");
    }

    public static Evidence observedAtWork(String personId, String workplaceId, long day) {
        String siteId = siteOf(personId);
        return new Evidence(eventId(personId, KIND_WORK_PRESENCE, SOURCE_WORK_PRESENCE,
                                    workplaceId, day), personId, siteId,
                            KIND_WORK_PRESENCE, SOURCE_WORK_PRESENCE, workplaceId, day);
    }

    public static String eventId(String personId, String kind, String source,
                                 String workplaceId, long day) {
        return sha256("av2c-v1\u0000" + personId + "\u0000" + kind + "\u0000" + source +
                      "\u0000" + workplaceId + "\u0000" + day);
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}

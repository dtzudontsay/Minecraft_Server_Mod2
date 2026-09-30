package dev.dtzudontsay.knownworld.simulation.npc.scenario;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Locale;
import java.util.Objects;

public final class CharacterScenarioState {

    private final NpcId npc;

    private int authoredVersion;

    private Presence presence;

    private Continuity continuity;

    private Confidence confidence;

    private String startingWorldLocationId;

    private String currentWorldLocationId;

    private String note;

    public CharacterScenarioState(
            NpcId npc,
            int authoredVersion,
            Presence presence,
            Continuity continuity,
            Confidence confidence,
            String startingWorldLocationId,
            String currentWorldLocationId,
            String note
    ) {

        this.npc =
                Objects.requireNonNull(
                        npc,
                        "npc"
                );

        if (authoredVersion < 0) {

            throw new IllegalArgumentException(
                    "authoredVersion cannot be negative"
            );
        }

        this.authoredVersion =
                authoredVersion;

        this.presence =
                Objects.requireNonNull(
                        presence,
                        "presence"
                );

        this.continuity =
                Objects.requireNonNull(
                        continuity,
                        "continuity"
                );

        this.confidence =
                Objects.requireNonNull(
                        confidence,
                        "confidence"
                );

        this.startingWorldLocationId =
                normalizeOptionalId(
                        startingWorldLocationId
                );

        this.currentWorldLocationId =
                normalizeOptionalId(
                        currentWorldLocationId
                );

        this.note =
                normalizeText(
                        note
                );
    }

    public NpcId npc() {
        return npc;
    }

    public int authoredVersion() {
        return authoredVersion;
    }

    public Presence presence() {
        return presence;
    }

    public Continuity continuity() {
        return continuity;
    }

    public Confidence confidence() {
        return confidence;
    }

    public String startingWorldLocationId() {
        return startingWorldLocationId;
    }

    public String currentWorldLocationId() {
        return currentWorldLocationId;
    }

    public String note() {
        return note;
    }

    public boolean hasStartingWorldLocation() {
        return startingWorldLocationId != null;
    }

    public boolean hasCurrentWorldLocation() {
        return currentWorldLocationId != null;
    }

    public void applyAuthoredRevision(
            int version,
            Presence newPresence,
            Continuity newContinuity,
            Confidence newConfidence,
            String newStartingWorldLocationId,
            String newNote
    ) {

        if (version <= authoredVersion) {
            return;
        }

        String normalizedNewStart =
                normalizeOptionalId(
                        newStartingWorldLocationId
                );

        boolean currentStillAtOldStart =
                Objects.equals(
                        currentWorldLocationId,
                        startingWorldLocationId
                );

        authoredVersion =
                version;

        presence =
                Objects.requireNonNull(
                        newPresence,
                        "newPresence"
                );

        continuity =
                Objects.requireNonNull(
                        newContinuity,
                        "newContinuity"
                );

        confidence =
                Objects.requireNonNull(
                        newConfidence,
                        "newConfidence"
                );

        startingWorldLocationId =
                normalizedNewStart;

        note =
                normalizeText(
                        newNote
                );

        /*
         * Authored revisions must not teleport a character who has
         * already moved during gameplay. Current location follows a
         * revised start only while the character is still at the old
         * authored starting location.
         */
        if (currentStillAtOldStart
                || currentWorldLocationId == null) {

            currentWorldLocationId =
                    normalizedNewStart;
        }
    }

    public void setCurrentWorldLocationId(
            String locationId
    ) {

        currentWorldLocationId =
                normalizeOptionalId(
                        locationId
                );
    }

    private static String normalizeOptionalId(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        String normalized =
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!normalized.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid world location ID: "
                            + value
            );
        }

        return normalized;
    }

    private static String normalizeText(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }

    public enum Presence {

        /**
         * Alive and part of the world at Scenario Day 1.
         */
        ALIVE,

        /**
         * The historical character exists in the database but died
         * before Scenario Day 1.
         */
        DEAD_BEFORE_START,

        /**
         * Kept for authoring/audit purposes only.
         *
         * A NOT_YET_BORN entry should normally NOT be instantiated as
         * a live NPC in the scenario.
         */
        NOT_YET_BORN,

        /**
         * Sources are insufficient to determine whether this person
         * already existed at the exact scenario anchor.
         */
        EXISTENCE_UNCERTAIN
    }

    public enum Continuity {

        BOOK_CANON,

        HBO_ADAPTATION,

        SHARED_BOOK_AND_HBO,

        COMPOSITE_ADAPTATION,

        UNKNOWN
    }

    public enum Confidence {

        CANON,

        STRONGLY_INFERRED,

        INFERRED,

        PLAUSIBLE_RECONSTRUCTION,

        UNKNOWN
    }
}
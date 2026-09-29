package dev.dtzudontsay.knownworld.simulation.npc.profile;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CharacterProfile {

    private final NpcId owner;

    private String culture;

    private String religion;

    private String education;

    private final EnumMap<CharacterSkill, Double> skills =
            new EnumMap<>(
                    CharacterSkill.class
            );

    private final Set<String> traits =
            new LinkedHashSet<>();

    private final Map<String, Double> values =
            new LinkedHashMap<>();

    private final List<String> motivations =
            new ArrayList<>();

    private DialoguePersona dialoguePersona;

    public CharacterProfile(
            NpcId owner
    ) {
        this(
                owner,
                "unknown",
                "unknown",
                "none",
                DialoguePersona.NEUTRAL
        );
    }

    public CharacterProfile(
            NpcId owner,
            String culture,
            String religion,
            String education,
            DialoguePersona dialoguePersona
    ) {
        this.owner =
                Objects.requireNonNull(
                        owner,
                        "owner"
                );

        this.culture =
                normalizeId(
                        culture,
                        "unknown"
                );

        this.religion =
                normalizeId(
                        religion,
                        "unknown"
                );

        this.education =
                normalizeId(
                        education,
                        "none"
                );

        this.dialoguePersona =
                Objects.requireNonNull(
                        dialoguePersona,
                        "dialoguePersona"
                );

        for (
                CharacterSkill skill :
                CharacterSkill.values()
        ) {

            skills.put(
                    skill,
                    0.0
            );
        }
    }

    public NpcId owner() {
        return owner;
    }

    public String culture() {
        return culture;
    }

    public String religion() {
        return religion;
    }

    public String education() {
        return education;
    }

    public DialoguePersona dialoguePersona() {
        return dialoguePersona;
    }

    public void setCulture(
            String culture
    ) {
        this.culture =
                normalizeId(
                        culture,
                        "unknown"
                );
    }

    public void setReligion(
            String religion
    ) {
        this.religion =
                normalizeId(
                        religion,
                        "unknown"
                );
    }

    public void setEducation(
            String education
    ) {
        this.education =
                normalizeId(
                        education,
                        "none"
                );
    }

    public void setDialoguePersona(
            DialoguePersona dialoguePersona
    ) {
        this.dialoguePersona =
                Objects.requireNonNull(
                        dialoguePersona,
                        "dialoguePersona"
                );
    }

    public double skill(
            CharacterSkill skill
    ) {
        Objects.requireNonNull(
                skill,
                "skill"
        );

        return skills.getOrDefault(
                skill,
                0.0
        );
    }

    public void setSkill(
            CharacterSkill skill,
            double value
    ) {
        skills.put(
                Objects.requireNonNull(
                        skill,
                        "skill"
                ),
                clampUnit(
                        value
                )
        );
    }

    public Map<CharacterSkill, Double> skills() {
        return Map.copyOf(
                skills
        );
    }

    public void addTrait(
            String trait
    ) {
        traits.add(
                normalizeId(
                        trait,
                        null
                )
        );
    }

    public void removeTrait(
            String trait
    ) {
        if (trait == null) {
            return;
        }

        traits.remove(
                trait.trim()
                        .toLowerCase()
        );
    }

    public boolean hasTrait(
            String trait
    ) {
        if (trait == null) {
            return false;
        }

        return traits.contains(
                trait.trim()
                        .toLowerCase()
        );
    }

    public Set<String> traits() {
        return Set.copyOf(
                traits
        );
    }

    public double value(
            String key
    ) {
        return values.getOrDefault(
                normalizeId(
                        key,
                        null
                ),
                0.0
        );
    }

    public void setValue(
            String key,
            double value
    ) {
        values.put(
                normalizeId(
                        key,
                        null
                ),
                clampUnit(
                        value
                )
        );
    }

    public Map<String, Double> values() {
        return Map.copyOf(
                values
        );
    }

    public void addMotivation(
            String motivation
    ) {
        String normalized =
                normalizeText(
                        motivation
                );

        if (!normalized.isEmpty()
                && !motivations.contains(
                normalized
        )) {

            motivations.add(
                    normalized
            );
        }
    }

    public void clearMotivations() {
        motivations.clear();
    }

    public List<String> motivations() {
        return List.copyOf(
                motivations
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

    private static String normalizeId(
            String value,
            String fallback
    ) {
        if (value == null
                || value.isBlank()) {

            if (fallback != null) {
                return fallback;
            }

            throw new IllegalArgumentException(
                    "Profile identifier cannot be empty"
            );
        }

        String normalized =
                value.trim()
                        .toLowerCase();

        if (!normalized.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid profile identifier: "
                            + value
            );
        }

        return normalized;
    }

    private static String normalizeText(
            String value
    ) {
        return Objects.requireNonNullElse(
                value,
                ""
        ).trim();
    }
}
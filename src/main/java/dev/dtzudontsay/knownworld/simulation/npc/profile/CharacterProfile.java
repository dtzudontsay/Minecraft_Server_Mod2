package dev.dtzudontsay.knownworld.simulation.npc.profile;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CharacterProfile {

    private final NpcId owner;

    /*
     * ---------------------------------------------------------
     * IDENTITY / FORMATION
     * ---------------------------------------------------------
     */

    private String culture;

    private String religion;

    private String education;

    private String birthplaceLocationId =
            "unknown";

    private String upbringingLocationId =
            "unknown";

    private CharacterOrientation orientation =
            CharacterOrientation.UNSPECIFIED;

    private CharacterHealthState healthState =
            CharacterHealthState.HEALTHY;

    private CharacterLegalStatus legalStatus =
            CharacterLegalStatus.UNSPECIFIED;

    /*
     * ---------------------------------------------------------
     * SOCIAL STATE
     * ---------------------------------------------------------
     */

    private double wealth;

    private double socialStatus;

    private double reputation;

    /*
     * ---------------------------------------------------------
     * NUMERIC CHARACTER DIMENSIONS
     * ---------------------------------------------------------
     */

    private final EnumMap<CharacterSkill, Double> skills =
            new EnumMap<>(
                    CharacterSkill.class
            );

    private final EnumMap<CharacterAptitude, Double> aptitudes =
            new EnumMap<>(
                    CharacterAptitude.class
            );

    private final EnumMap<CharacterDisposition, Double> dispositions =
            new EnumMap<>(
                    CharacterDisposition.class
            );

    private final EnumMap<CharacterValue, Double> characterValues =
            new EnumMap<>(
                    CharacterValue.class
            );

    private final EnumMap<CharacterSocialNorm, Double> socialNorms =
            new EnumMap<>(
                    CharacterSocialNorm.class
            );

    /*
     * Legacy / scenario-specific extensibility.
     *
     * Existing scenario JSON uses these values and therefore this
     * map remains supported.
     *
     * Range: [0,1]
     */
    private final Map<String, Double> customValues =
            new LinkedHashMap<>();

    /*
     * Political preference keys are intentionally open-ended.
     *
     * Examples later:
     *
     * crown.centralization
     * feudal.autonomy
     * succession.primogeniture
     *
     * Range: [-1,1]
     */
    private final Map<String, Double> politicalPreferences =
            new LinkedHashMap<>();

    /*
     * ---------------------------------------------------------
     * TAGS / ROLES
     * ---------------------------------------------------------
     */

    private final Set<String> traits =
            new LinkedHashSet<>();

    private final Set<String> aliases =
            new LinkedHashSet<>();

    private final Set<String> occupations =
            new LinkedHashSet<>();

    private final Set<String> offices =
            new LinkedHashSet<>();

    private final Set<String> courtRoles =
            new LinkedHashSet<>();

    private final Set<String> militaryRoles =
            new LinkedHashSet<>();

    private final Set<String> combatSpecialties =
            new LinkedHashSet<>();

    /*
     * ---------------------------------------------------------
     * LANGUAGES
     * ---------------------------------------------------------
     *
     * language ID -> proficiency [0,1]
     */

    private final Map<String, Double> languages =
            new LinkedHashMap<>();

    /*
     * ---------------------------------------------------------
     * MOTIVATION / PSYCHOLOGICAL CONTENT
     * ---------------------------------------------------------
     */

    private final List<String> motivations =
            new ArrayList<>();

    private final List<String> goals =
            new ArrayList<>();

    private final List<String> fears =
            new ArrayList<>();

    private final List<String> desires =
            new ArrayList<>();

    /*
     * ---------------------------------------------------------
     * SECRETS / FACTS
     * ---------------------------------------------------------
     *
     * These are schema foundations only.
     *
     * The later secrets/knowledge system will decide discovery,
     * propagation, secrecy and confidence.
     */

    private final Set<String> secrets =
            new LinkedHashSet<>();

    private final Set<String> knownSecrets =
            new LinkedHashSet<>();

    private final Map<String, String> publicFacts =
            new LinkedHashMap<>();

    private final Map<String, String> privateFacts =
            new LinkedHashMap<>();

    /*
     * ---------------------------------------------------------
     * APPEARANCE
     * ---------------------------------------------------------
     */

    private String appearanceDescription =
            "";

    private String hairDescription =
            "";

    private String eyeDescription =
            "";

    private String buildDescription =
            "";

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

        initializeNumericDimensions();
    }

    private void initializeNumericDimensions() {

        for (
                CharacterSkill skill :
                CharacterSkill.values()
        ) {

            skills.put(
                    skill,
                    0.0
            );
        }

        for (
                CharacterAptitude aptitude :
                CharacterAptitude.values()
        ) {

            aptitudes.put(
                    aptitude,
                    0.0
            );
        }

        for (
                CharacterDisposition disposition :
                CharacterDisposition.values()
        ) {

            dispositions.put(
                    disposition,
                    0.0
            );
        }

        for (
                CharacterValue value :
                CharacterValue.values()
        ) {

            characterValues.put(
                    value,
                    0.0
            );
        }

        for (
                CharacterSocialNorm norm :
                CharacterSocialNorm.values()
        ) {

            socialNorms.put(
                    norm,
                    0.0
            );
        }
    }

    public NpcId owner() {
        return owner;
    }

    /*
     * ---------------------------------------------------------
     * IDENTITY
     * ---------------------------------------------------------
     */

    public String culture() {
        return culture;
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

    public String religion() {
        return religion;
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

    public String education() {
        return education;
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

    public String birthplaceLocationId() {
        return birthplaceLocationId;
    }

    public void setBirthplaceLocationId(
            String locationId
    ) {
        birthplaceLocationId =
                normalizeId(
                        locationId,
                        "unknown"
                );
    }

    public String upbringingLocationId() {
        return upbringingLocationId;
    }

    public void setUpbringingLocationId(
            String locationId
    ) {
        upbringingLocationId =
                normalizeId(
                        locationId,
                        "unknown"
                );
    }

    public CharacterOrientation orientation() {
        return orientation;
    }

    public void setOrientation(
            CharacterOrientation orientation
    ) {
        this.orientation =
                Objects.requireNonNull(
                        orientation,
                        "orientation"
                );
    }

    public CharacterHealthState healthState() {
        return healthState;
    }

    public void setHealthState(
            CharacterHealthState healthState
    ) {
        this.healthState =
                Objects.requireNonNull(
                        healthState,
                        "healthState"
                );
    }

    public CharacterLegalStatus legalStatus() {
        return legalStatus;
    }

    public void setLegalStatus(
            CharacterLegalStatus legalStatus
    ) {
        this.legalStatus =
                Objects.requireNonNull(
                        legalStatus,
                        "legalStatus"
                );
    }

    /*
     * ---------------------------------------------------------
     * SOCIAL STATE
     * ---------------------------------------------------------
     */

    public double wealth() {
        return wealth;
    }

    public void setWealth(
            double wealth
    ) {
        this.wealth =
                clampSigned(
                        wealth
                );
    }

    public double socialStatus() {
        return socialStatus;
    }

    public void setSocialStatus(
            double socialStatus
    ) {
        this.socialStatus =
                clampSigned(
                        socialStatus
                );
    }

    public double reputation() {
        return reputation;
    }

    public void setReputation(
            double reputation
    ) {
        this.reputation =
                clampSigned(
                        reputation
                );
    }

    /*
     * ---------------------------------------------------------
     * SKILLS
     * ---------------------------------------------------------
     */

    public double skill(
            CharacterSkill skill
    ) {
        return skills.getOrDefault(
                Objects.requireNonNull(
                        skill,
                        "skill"
                ),
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

    /*
     * ---------------------------------------------------------
     * APTITUDES
     * ---------------------------------------------------------
     */

    public double aptitude(
            CharacterAptitude aptitude
    ) {
        return aptitudes.getOrDefault(
                Objects.requireNonNull(
                        aptitude,
                        "aptitude"
                ),
                0.0
        );
    }

    public void setAptitude(
            CharacterAptitude aptitude,
            double value
    ) {
        aptitudes.put(
                Objects.requireNonNull(
                        aptitude,
                        "aptitude"
                ),
                clampUnit(
                        value
                )
        );
    }

    public Map<CharacterAptitude, Double> aptitudes() {
        return Map.copyOf(
                aptitudes
        );
    }

    /*
     * ---------------------------------------------------------
     * DISPOSITIONS
     * ---------------------------------------------------------
     */

    public double disposition(
            CharacterDisposition disposition
    ) {
        return dispositions.getOrDefault(
                Objects.requireNonNull(
                        disposition,
                        "disposition"
                ),
                0.0
        );
    }

    public void setDisposition(
            CharacterDisposition disposition,
            double value
    ) {
        dispositions.put(
                Objects.requireNonNull(
                        disposition,
                        "disposition"
                ),
                clampSigned(
                        value
                )
        );
    }

    public Map<CharacterDisposition, Double> dispositions() {
        return Map.copyOf(
                dispositions
        );
    }

    /*
     * ---------------------------------------------------------
     * TYPED VALUES
     * ---------------------------------------------------------
     */

    public double characterValue(
            CharacterValue value
    ) {
        return characterValues.getOrDefault(
                Objects.requireNonNull(
                        value,
                        "value"
                ),
                0.0
        );
    }

    public void setCharacterValue(
            CharacterValue value,
            double amount
    ) {
        characterValues.put(
                Objects.requireNonNull(
                        value,
                        "value"
                ),
                clampSigned(
                        amount
                )
        );
    }

    public Map<CharacterValue, Double> characterValues() {
        return Map.copyOf(
                characterValues
        );
    }

    /*
     * ---------------------------------------------------------
     * SOCIAL NORMS
     * ---------------------------------------------------------
     */

    public double socialNorm(
            CharacterSocialNorm norm
    ) {
        return socialNorms.getOrDefault(
                Objects.requireNonNull(
                        norm,
                        "norm"
                ),
                0.0
        );
    }

    public void setSocialNorm(
            CharacterSocialNorm norm,
            double value
    ) {
        socialNorms.put(
                Objects.requireNonNull(
                        norm,
                        "norm"
                ),
                clampSigned(
                        value
                )
        );
    }

    public Map<CharacterSocialNorm, Double> socialNorms() {
        return Map.copyOf(
                socialNorms
        );
    }

    /*
     * ---------------------------------------------------------
     * LEGACY CUSTOM VALUES
     * ---------------------------------------------------------
     */

    public double value(
            String key
    ) {
        return customValues.getOrDefault(
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
        customValues.put(
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
                customValues
        );
    }

    /*
     * ---------------------------------------------------------
     * POLITICAL PREFERENCES
     * ---------------------------------------------------------
     */

    public double politicalPreference(
            String key
    ) {
        return politicalPreferences.getOrDefault(
                normalizeId(
                        key,
                        null
                ),
                0.0
        );
    }

    public void setPoliticalPreference(
            String key,
            double value
    ) {
        politicalPreferences.put(
                normalizeId(
                        key,
                        null
                ),
                clampSigned(
                        value
                )
        );
    }

    public Map<String, Double> politicalPreferences() {
        return Map.copyOf(
                politicalPreferences
        );
    }

    /*
     * ---------------------------------------------------------
     * TRAITS
     * ---------------------------------------------------------
     */

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
        removeNormalizedId(
                traits,
                trait
        );
    }

    public boolean hasTrait(
            String trait
    ) {
        return containsNormalizedId(
                traits,
                trait
        );
    }

    public Set<String> traits() {
        return Set.copyOf(
                traits
        );
    }

    /*
     * ---------------------------------------------------------
     * ALIASES
     * ---------------------------------------------------------
     */

    public void addAlias(
            String alias
    ) {
        addText(
                aliases,
                alias
        );
    }

    public Set<String> aliases() {
        return Set.copyOf(
                aliases
        );
    }

    /*
     * ---------------------------------------------------------
     * OCCUPATIONS / ROLES
     * ---------------------------------------------------------
     */

    public void addOccupation(
            String id
    ) {
        occupations.add(
                normalizeId(
                        id,
                        null
                )
        );
    }

    public Set<String> occupations() {
        return Set.copyOf(
                occupations
        );
    }

    public void addOffice(
            String id
    ) {
        offices.add(
                normalizeId(
                        id,
                        null
                )
        );
    }

    public Set<String> offices() {
        return Set.copyOf(
                offices
        );
    }

    public void addCourtRole(
            String id
    ) {
        courtRoles.add(
                normalizeId(
                        id,
                        null
                )
        );
    }

    public Set<String> courtRoles() {
        return Set.copyOf(
                courtRoles
        );
    }

    public void addMilitaryRole(
            String id
    ) {
        militaryRoles.add(
                normalizeId(
                        id,
                        null
                )
        );
    }

    public Set<String> militaryRoles() {
        return Set.copyOf(
                militaryRoles
        );
    }

    public void addCombatSpecialty(
            String id
    ) {
        combatSpecialties.add(
                normalizeId(
                        id,
                        null
                )
        );
    }

    public Set<String> combatSpecialties() {
        return Set.copyOf(
                combatSpecialties
        );
    }

    /*
     * ---------------------------------------------------------
     * LANGUAGES
     * ---------------------------------------------------------
     */

    public void setLanguageProficiency(
            String languageId,
            double proficiency
    ) {
        languages.put(
                normalizeId(
                        languageId,
                        null
                ),
                clampUnit(
                        proficiency
                )
        );
    }

    public double languageProficiency(
            String languageId
    ) {
        return languages.getOrDefault(
                normalizeId(
                        languageId,
                        null
                ),
                0.0
        );
    }

    public Map<String, Double> languages() {
        return Map.copyOf(
                languages
        );
    }

    /*
     * ---------------------------------------------------------
     * MOTIVATIONS / GOALS / FEARS / DESIRES
     * ---------------------------------------------------------
     */

    public void addMotivation(
            String motivation
    ) {
        addUniqueText(
                motivations,
                motivation
        );
    }

    public void clearMotivations() {
        motivations.clear();
    }

    public List<String> motivations() {
        return List.copyOf(
                motivations
        );
    }

    public void addGoal(
            String goal
    ) {
        addUniqueText(
                goals,
                goal
        );
    }

    public void clearGoals() {
        goals.clear();
    }

    public List<String> goals() {
        return List.copyOf(
                goals
        );
    }

    public void addFear(
            String fear
    ) {
        addUniqueText(
                fears,
                fear
        );
    }

    public void clearFears() {
        fears.clear();
    }

    public List<String> fears() {
        return List.copyOf(
                fears
        );
    }

    public void addDesire(
            String desire
    ) {
        addUniqueText(
                desires,
                desire
        );
    }

    public void clearDesires() {
        desires.clear();
    }

    public List<String> desires() {
        return List.copyOf(
                desires
        );
    }

    /*
     * ---------------------------------------------------------
     * SECRETS
     * ---------------------------------------------------------
     */

    public void addSecret(
            String secretId
    ) {
        secrets.add(
                normalizeId(
                        secretId,
                        null
                )
        );
    }

    public Set<String> secrets() {
        return Set.copyOf(
                secrets
        );
    }

    public void addKnownSecret(
            String secretId
    ) {
        knownSecrets.add(
                normalizeId(
                        secretId,
                        null
                )
        );
    }

    public Set<String> knownSecrets() {
        return Set.copyOf(
                knownSecrets
        );
    }

    /*
     * ---------------------------------------------------------
     * PUBLIC / PRIVATE FACTS
     * ---------------------------------------------------------
     */

    public void setPublicFact(
            String key,
            String value
    ) {
        publicFacts.put(
                normalizeId(
                        key,
                        null
                ),
                normalizeText(
                        value
                )
        );
    }

    public Map<String, String> publicFacts() {
        return Map.copyOf(
                publicFacts
        );
    }

    public void setPrivateFact(
            String key,
            String value
    ) {
        privateFacts.put(
                normalizeId(
                        key,
                        null
                ),
                normalizeText(
                        value
                )
        );
    }

    public Map<String, String> privateFacts() {
        return Map.copyOf(
                privateFacts
        );
    }

    /*
     * ---------------------------------------------------------
     * APPEARANCE
     * ---------------------------------------------------------
     */

    public String appearanceDescription() {
        return appearanceDescription;
    }

    public void setAppearanceDescription(
            String appearanceDescription
    ) {
        this.appearanceDescription =
                normalizeText(
                        appearanceDescription
                );
    }

    public String hairDescription() {
        return hairDescription;
    }

    public void setHairDescription(
            String hairDescription
    ) {
        this.hairDescription =
                normalizeText(
                        hairDescription
                );
    }

    public String eyeDescription() {
        return eyeDescription;
    }

    public void setEyeDescription(
            String eyeDescription
    ) {
        this.eyeDescription =
                normalizeText(
                        eyeDescription
                );
    }

    public String buildDescription() {
        return buildDescription;
    }

    public void setBuildDescription(
            String buildDescription
    ) {
        this.buildDescription =
                normalizeText(
                        buildDescription
                );
    }

    /*
     * ---------------------------------------------------------
     * DIALOGUE
     * ---------------------------------------------------------
     */

    public DialoguePersona dialoguePersona() {
        return dialoguePersona;
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

    /*
     * ---------------------------------------------------------
     * HELPERS
     * ---------------------------------------------------------
     */

    private static double clampUnit(
            double value
    ) {
        requireFinite(
                value
        );

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static double clampSigned(
            double value
    ) {
        requireFinite(
                value
        );

        return Math.max(
                -1.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static void requireFinite(
            double value
    ) {
        if (!Double.isFinite(
                value
        )) {

            throw new IllegalArgumentException(
                    "Profile value must be finite"
            );
        }
    }

    private static String normalizeId(
            String value,
            String fallback
    ) {
        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            if (fallback != null) {
                return fallback;
            }

            throw new IllegalArgumentException(
                    "Profile identifier cannot be empty"
            );
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

    private static void addUniqueText(
            List<String> target,
            String value
    ) {
        String normalized =
                normalizeText(
                        value
                );

        if (
                !normalized.isEmpty()
                        &&
                        !target.contains(
                                normalized
                        )
        ) {

            target.add(
                    normalized
            );
        }
    }

    private static void addText(
            Set<String> target,
            String value
    ) {
        String normalized =
                normalizeText(
                        value
                );

        if (!normalized.isEmpty()) {

            target.add(
                    normalized
            );
        }
    }

    private static void removeNormalizedId(
            Set<String> target,
            String value
    ) {
        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return;
        }

        target.remove(
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
        );
    }

    private static boolean containsNormalizedId(
            Set<String> target,
            String value
    ) {
        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return false;
        }

        return target.contains(
                value.trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
        );
    }
}
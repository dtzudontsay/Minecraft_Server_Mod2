package dev.dtzudontsay.knownworld.simulation.npc.formation;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.culture.CultureService;
import dev.dtzudontsay.knownworld.simulation.npc.family.GenealogyManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.LifeHistoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterInfluenceChannel;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.CharacterPsychologyService;
import dev.dtzudontsay.knownworld.simulation.npc.psychology.RegionalCharacterInfluenceService;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionService;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolution;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolver;

import java.util.Objects;

public final class ChildFormationService {

    private static final int MAX_FORMATION_AGE =
            15;

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final GenealogyManager genealogy;

    private final LifeHistoryManager lifeHistory;

    private final CampaignCalendar calendar;

    private final UpbringingManager upbringing;

    private final CharacterPsychologyService psychology;

    private final RegionalCharacterInfluenceService regionalInfluence;

    private final CultureService culture;

    private final ReligionService religion;

    public ChildFormationService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
            GenealogyManager genealogy,
            LifeHistoryManager lifeHistory,
            CampaignCalendar calendar,
            UpbringingManager upbringing,
            CharacterPsychologyService psychology,
            RegionalCharacterInfluenceService regionalInfluence,
            CultureService culture,
            ReligionService religion
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.profiles =
                Objects.requireNonNull(
                        profiles,
                        "profiles"
                );

        this.genealogy =
                Objects.requireNonNull(
                        genealogy,
                        "genealogy"
                );

        this.lifeHistory =
                Objects.requireNonNull(
                        lifeHistory,
                        "lifeHistory"
                );

        this.calendar =
                Objects.requireNonNull(
                        calendar,
                        "calendar"
                );

        this.upbringing =
                Objects.requireNonNull(
                        upbringing,
                        "upbringing"
                );

        this.psychology =
                Objects.requireNonNull(
                        psychology,
                        "psychology"
                );

        this.regionalInfluence =
                Objects.requireNonNull(
                        regionalInfluence,
                        "regionalInfluence"
                );

        this.culture =
                Objects.requireNonNull(
                        culture,
                        "culture"
                );

        this.religion =
                Objects.requireNonNull(
                        religion,
                        "religion"
                );
    }

    /*
     * =========================================================
     * BIRTH
     * =========================================================
     */

    public void initializeAtBirth(
            NpcId child,
            NpcId mother,
            NpcId father
    ) {

        NpcState childState =
                requireNpc(
                        child
                );

        CharacterProfile profile =
                profiles.getOrCreate(
                        child
                );

        String birthLocation =
                resolveLocation(
                        childState
                );

        upbringing.createBirthRecord(
                child,
                birthLocation,
                mother
        );

        profile.setBirthplaceLocationId(
                birthLocation
        );

        profile.setUpbringingLocationId(
                birthLocation
        );

        initializeCultureAtBirth(
                child,
                profile
        );

        initializeReligionAtBirth(
                child,
                profile
        );

        if (genealogy.parentsOf(
                child
        ).isPresent()) {

            culture.applyParentInfluence(
                    child,
                    0.20
            );
        }
    }

    private void initializeCultureAtBirth(
            NpcId child,
            CharacterProfile profile
    ) {

        if ("unknown".equals(
                profile.culture()
        )) {

            return;
        }

        culture.initializeCultureState(
                child,
                profile.culture(),
                0.10,
                0.15,
                0.70,
                0.05
        );
    }

    private void initializeReligionAtBirth(
            NpcId child,
            CharacterProfile profile
    ) {

        if ("unknown".equals(
                profile.religion()
        )) {

            return;
        }

        religion.initializeFaithState(
                child,
                profile.religion(),
                0.02,
                0.00,
                0.08,
                0.50,
                0.75,
                0.05,
                0.10,
                0.02
        );
    }

    /*
     * =========================================================
     * COMPATIBILITY MIGRATION
     * =========================================================
     */

    public void ensureExistingChildren() {

        lifeHistory.ensureAll(
                calendar.daysPerYear()
        );

        for (
                NpcState npc :
                registry.all()
        ) {

            if (!npc.isAlive()) {
                continue;
            }

            int age =
                    lifeHistory.ageYears(
                            npc.id(),
                            calendar.absoluteDay(),
                            calendar.daysPerYear()
                    );

            if (age > MAX_FORMATION_AGE) {
                continue;
            }

            CharacterProfile profile =
                    profiles.getOrCreate(
                            npc.id()
                    );

            String birthLocation =
                    knownLocationOrUnknown(
                            profile.birthplaceLocationId()
                    );

            String upbringingLocation =
                    knownLocationOrCurrent(
                            profile.upbringingLocationId(),
                            npc
                    );

            NpcId defaultGuardian =
                    defaultGuardian(
                            npc.id()
                    );

            UpbringingRecord record =
                    upbringing.ensureMigrated(
                            npc.id(),
                            birthLocation,
                            upbringingLocation,
                            defaultGuardian,
                            age
                    );

            if (!"unknown".equals(
                    record.upbringingLocationId()
            )) {

                profile.setUpbringingLocationId(
                        record.upbringingLocationId()
                );
            }

            /*
             * Never invent a historical birthplace from current
             * position.
             */
            if (!"unknown".equals(
                    record.birthLocationId()
            )) {

                profile.setBirthplaceLocationId(
                        record.birthLocationId()
                );
            }
        }
    }

    /*
     * =========================================================
     * DAILY ENTRY POINT — ACTUAL WORK ONLY ON NEW AGE
     * =========================================================
     */

    public void onNewCampaignDay() {

        for (
                UpbringingRecord record :
                upbringing.all()
        ) {

            NpcState child =
                    registry.find(
                                    record.child()
                            )
                            .orElse(
                                    null
                            );

            if (child == null
                    || !child.isAlive()) {

                continue;
            }

            int age =
                    lifeHistory.ageYears(
                            child.id(),
                            calendar.absoluteDay(),
                            calendar.daysPerYear()
                    );

            if (age <= 0
                    || age > MAX_FORMATION_AGE) {

                continue;
            }

            if (age <= record.lastFormationAge()) {

                continue;
            }

            applyFormationYear(
                    child.id(),
                    age
            );

            record.setLastFormationAge(
                    age
            );
        }
    }

    /*
     * =========================================================
     * ONE DEVELOPMENT YEAR
     * =========================================================
     */

    public void applyFormationYear(
            NpcId child,
            int age
    ) {

        if (age < 1
                || age > MAX_FORMATION_AGE) {

            throw new IllegalArgumentException(
                    "Formation age must be between 1 and "
                            + MAX_FORMATION_AGE
            );
        }

        NpcState childState =
                requireNpc(
                        child
                );

        UpbringingRecord record =
                upbringing.require(
                        child
                );

        String location =
                resolveLocation(
                        childState
                );

        record.setUpbringingLocationId(
                location
        );

        profiles.getOrCreate(
                        child
                )
                .setUpbringingLocationId(
                        location
                );

        double strength =
                stageStrength(
                        age
                );

        applyRegionalFormation(
                child,
                childState,
                strength
        );

        Parentage parentage =
                genealogy.parentsOf(
                                child
                        )
                        .orElse(
                                null
                        );

        applyBiologicalParentFormation(
                child,
                parentage,
                strength
        );

        /*
         * Do not double-count a biological parent merely because
         * they are also the recorded guardian.
         */
        if (!isBiologicalParent(
                record.guardian(),
                parentage
        )) {

            applyMentorFormation(
                    child,
                    record.guardian(),
                    strength,
                    CharacterInfluenceChannel.GUARDIAN
            );
        }

        if (record.fosterParent() != null
                && !Objects.equals(
                record.guardian(),
                record.fosterParent()
        )
                && !isBiologicalParent(
                record.fosterParent(),
                parentage
        )) {

            applyMentorFormation(
                    child,
                    record.fosterParent(),
                    strength
                            * 0.80,
                    CharacterInfluenceChannel.HOUSEHOLD
            );
        }
    }

    private void applyRegionalFormation(
            NpcId child,
            NpcState childState,
            double strength
    ) {

        double x =
                childState.position()
                        .x();

        double z =
                childState.position()
                        .z();

        RegionalInfluenceResolution resolution =
                RegionalInfluenceResolver.resolveMinecraft(
                                x,
                                z
                        )
                        .orElse(
                                null
                        );

        if (resolution == null) {
            return;
        }

        regionalInfluence.applyUpbringingInfluence(
                child,
                x,
                z,
                strength
        );

        culture.applyRegionalExposure(
                child,
                x,
                z,
                strength
        );

        if (religion.strongestRegionalReligion(
                x,
                z
        ).isPresent()) {

            religion.applyRegionalPressure(
                    child,
                    x,
                    z,
                    strength
                            * 0.65
            );
        }
    }

    private void applyBiologicalParentFormation(
            NpcId child,
            Parentage parentage,
            double strength
    ) {

        if (parentage == null) {
            return;
        }

        culture.applyParentInfluence(
                child,
                strength
                        * 0.85
        );

        if (parentage.mother() != null) {

            applyParentWorldview(
                    child,
                    parentage.mother(),
                    strength
                            * 0.80
            );
        }

        if (parentage.father() != null) {

            applyParentWorldview(
                    child,
                    parentage.father(),
                    strength
                            * 0.70
            );
        }
    }

    private void applyParentWorldview(
            NpcId child,
            NpcId parent,
            double strength
    ) {

        NpcState parentState =
                registry.find(
                                parent
                        )
                        .orElse(
                                null
                        );

        if (parentState == null) {
            return;
        }

        CharacterProfile parentProfile =
                profiles.getOrCreate(
                        parent
                );

        applyProfileInfluence(
                child,
                parent,
                parentProfile,
                strength,
                CharacterInfluenceChannel.PARENTAL
        );

        if (!"unknown".equals(
                parentProfile.religion()
        )) {

            religion.applyConversionPressure(
                    child,
                    parent,
                    parentProfile.religion(),
                    strength
                            * 0.55
            );
        }
    }

    /*
     * =========================================================
     * GUARDIAN / FOSTER FORMATION
     * =========================================================
     */

    private void applyMentorFormation(
            NpcId child,
            NpcId mentor,
            double strength,
            CharacterInfluenceChannel channel
    ) {

        if (mentor == null) {
            return;
        }

        NpcState mentorState =
                registry.find(
                                mentor
                        )
                        .orElse(
                                null
                        );

        if (mentorState == null
                || !mentorState.isAlive()) {

            return;
        }

        CharacterProfile mentorProfile =
                profiles.getOrCreate(
                        mentor
                );

        applyProfileInfluence(
                child,
                mentor,
                mentorProfile,
                strength,
                channel
        );

        if (!"unknown".equals(
                mentorProfile.culture()
        )) {

            culture.applyCulturePressure(
                    child,
                    mentor,
                    mentorProfile.culture(),
                    strength
                            * 0.75
            );
        }

        if (!"unknown".equals(
                mentorProfile.religion()
        )) {

            religion.applyConversionPressure(
                    child,
                    mentor,
                    mentorProfile.religion(),
                    strength
                            * 0.65
            );
        }

        applySkillMentoring(
                child,
                mentorProfile,
                strength
        );
    }

    private void applyProfileInfluence(
            NpcId child,
            NpcId source,
            CharacterProfile sourceProfile,
            double strength,
            CharacterInfluenceChannel channel
    ) {

        for (
                CharacterValue value :
                CharacterValue.values()
        ) {

            psychology.applyValuePressure(
                    child,
                    source,
                    value,
                    sourceProfile.characterValue(
                            value
                    ),
                    strength,
                    channel
            );
        }

        for (
                CharacterSocialNorm norm :
                CharacterSocialNorm.values()
        ) {

            psychology.applySocialNormPressure(
                    child,
                    source,
                    norm,
                    sourceProfile.socialNorm(
                            norm
                    ),
                    strength,
                    channel
            );
        }
    }

    /*
     * =========================================================
     * EDUCATION / SKILL DEVELOPMENT
     * =========================================================
     */

    private void applySkillMentoring(
            NpcId child,
            CharacterProfile mentor,
            double strength
    ) {

        CharacterProfile childProfile =
                profiles.getOrCreate(
                        child
                );

        for (
                CharacterSkill skill :
                CharacterSkill.values()
        ) {

            double mentorSkill =
                    mentor.skill(
                            skill
                    );

            if (mentorSkill <= 0.0) {
                continue;
            }

            CharacterAptitude aptitude =
                    matchingAptitude(
                            skill
                    );

            double naturalAptitude =
                    aptitude == null
                            ? 0.50
                            : childProfile.aptitude(
                            aptitude
                    );

            double learningStrength =
                    clampUnit(
                            strength
                                    * (
                                    0.08
                                            + naturalAptitude
                                            * 0.12
                            )
                    );

            double current =
                    childProfile.skill(
                            skill
                    );

            double target =
                    Math.max(
                            current,
                            mentorSkill
                    );

            childProfile.setSkill(
                    skill,
                    moveToward(
                            current,
                            target,
                            learningStrength
                    )
            );
        }
    }

    private static CharacterAptitude matchingAptitude(
            CharacterSkill skill
    ) {

        return switch (skill) {

            case MARTIAL ->
                    CharacterAptitude.MARTIAL;

            case DIPLOMACY ->
                    CharacterAptitude.DIPLOMACY;

            case STEWARDSHIP ->
                    CharacterAptitude.STEWARDSHIP;

            case INTRIGUE ->
                    CharacterAptitude.INTRIGUE;

            case LEARNING ->
                    CharacterAptitude.LEARNING;

            case COMBAT ->
                    CharacterAptitude.COMBAT;

            case LEADERSHIP ->
                    CharacterAptitude.LEADERSHIP;

            case RIDING ->
                    CharacterAptitude.RIDING;

            case SAILING ->
                    CharacterAptitude.SAILING;

            case NAVIGATION ->
                    CharacterAptitude.NAVIGATION;

            case TRADE ->
                    CharacterAptitude.TRADE;

            case CRAFTING ->
                    CharacterAptitude.CRAFTING;

            case AGRICULTURE ->
                    CharacterAptitude.AGRICULTURE;

            case HUNTING ->
                    CharacterAptitude.HUNTING;

            case MEDICINE ->
                    CharacterAptitude.MEDICINE;

            case RELIGIOUS_STUDY ->
                    CharacterAptitude.RELIGIOUS_STUDY;

            case LANGUAGE ->
                    CharacterAptitude.LANGUAGES;

            case PERFORMANCE ->
                    CharacterAptitude.PERFORMANCE;

            case COMMAND ->
                    CharacterAptitude.COMMAND;

            case LOGISTICS ->
                    CharacterAptitude.LOGISTICS;
        };
    }

    /*
     * =========================================================
     * GUARDIANSHIP
     * =========================================================
     */

    public void setGuardian(
            NpcId child,
            NpcId guardian
    ) {

        requireNpc(
                child
        );

        requireNpc(
                guardian
        );

        upbringing.setGuardian(
                child,
                guardian
        );
    }

    public void setFosterParent(
            NpcId child,
            NpcId fosterParent
    ) {

        requireNpc(
                child
        );

        requireNpc(
                fosterParent
        );

        upbringing.setFosterParent(
                child,
                fosterParent
        );
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private NpcState requireNpc(
            NpcId npc
    ) {

        return registry.find(
                        Objects.requireNonNull(
                                npc,
                                "npc"
                        )
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC "
                                                + npc
                                )
                );
    }

    private NpcId defaultGuardian(
            NpcId child
    ) {

        Parentage parentage =
                genealogy.parentsOf(
                                child
                        )
                        .orElse(
                                null
                        );

        if (parentage == null) {
            return null;
        }

        if (parentage.mother() != null) {
            return parentage.mother();
        }

        return parentage.father();
    }

    private static boolean isBiologicalParent(
            NpcId npc,
            Parentage parentage
    ) {

        if (npc == null
                || parentage == null) {

            return false;
        }

        return npc.equals(
                parentage.mother()
        )
                || npc.equals(
                parentage.father()
        );
    }

    private static String knownLocationOrUnknown(
            String location
    ) {

        if (location == null
                || location.isBlank()) {

            return "unknown";
        }

        return location;
    }

    private static String knownLocationOrCurrent(
            String location,
            NpcState npc
    ) {

        if (location != null
                && !location.isBlank()
                && !"unknown".equals(
                location
        )) {

            return location;
        }

        return resolveLocation(
                npc
        );
    }

    private static String resolveLocation(
            NpcState npc
    ) {

        return RegionalInfluenceResolver.resolveMinecraft(
                        npc.position()
                                .x(),
                        npc.position()
                                .z()
                )
                .map(
                        RegionalInfluenceResolution::mostSpecificLocationId
                )
                .orElse(
                        "unknown"
                );
    }

    private static double stageStrength(
            int age
    ) {

        if (age <= 5) {
            return 0.22;
        }

        if (age <= 11) {
            return 0.30;
        }

        return 0.24;
    }

    private static double moveToward(
            double current,
            double target,
            double strength
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        current
                                + (
                                target - current
                        )
                                * clampUnit(
                                strength
                        )
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
}
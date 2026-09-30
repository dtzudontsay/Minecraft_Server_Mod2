package dev.dtzudontsay.knownworld.simulation.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterHealthState;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterLegalStatus;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterOrientation;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.profile.DialoguePersona;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Batch 18G.
 *
 * Applies versioned, authored high-detail character state from the same
 * character JSON files already used by ScenarioBootstrapper.
 *
 * The original scenario bootstrapper intentionally remains backwards
 * compatible with the older compact schema. This service understands the
 * expanded 18B+ profile vocabulary.
 *
 * Each authoring version is applied once per character and marked inside
 * CharacterProfile custom values. Therefore:
 *
 * - old saves can receive newly-authored character state,
 * - fresh scenarios receive the complete state,
 * - later restarts do not reset simulation changes,
 * - a future authoringVersion 2 can migrate a character once again.
 */
public final class AuthoredCharacterProfileOverlayService {

    private static final Gson GSON =
            new Gson();

    private static final String DEFAULT_SCENARIO =
            ScenarioBootstrapper.DEFAULT_SCENARIO;

    private AuthoredCharacterProfileOverlayService() {
    }

    public static void registerLifecycle() {

        /*
         * KnownWorld registers this AFTER NpcSimulation.registerLifecycle().
         *
         * Therefore the simulation's SERVER_STARTED listener initializes
         * and loads the save first. This listener then applies authored
         * profile overlays.
         */
        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> {

                    NpcSimulation simulation =
                            NpcSimulation.getNullable();

                    if (simulation == null) {

                        KnownWorld.LOGGER.error(
                                "Cannot apply authored character profiles because NPC simulation is not running."
                        );

                        return;
                    }

                    try {

                        int applied =
                                applyScenario(
                                        simulation,
                                        DEFAULT_SCENARIO
                                );

                        if (applied > 0) {

                            simulation.save();

                            KnownWorld.LOGGER.info(
                                    "Applied {} authored character profile overlay(s).",
                                    applied
                            );

                        } else {

                            KnownWorld.LOGGER.info(
                                    "Authored character profiles already current."
                            );
                        }

                    } catch (
                            IOException | RuntimeException exception
                    ) {

                        KnownWorld.LOGGER.error(
                                "Failed to apply authored character profile overlays.",
                                exception
                        );
                    }
                }
        );
    }

    public static int applyDefaultNow(
            NpcSimulation simulation
    ) throws IOException {

        return applyScenario(
                simulation,
                DEFAULT_SCENARIO
        );
    }

    private static int applyScenario(
            NpcSimulation simulation,
            String scenarioResource
    ) throws IOException {

        ScenarioIndex scenario =
                readJson(
                        scenarioResource,
                        ScenarioIndex.class
                );

        if (scenario.characters == null
                || scenario.characters.isEmpty()) {

            return 0;
        }

        String base =
                parentPath(
                        scenarioResource
                );

        int applied =
                0;

        for (
                String characterResource :
                scenario.characters
        ) {

            if (characterResource == null
                    || characterResource.isBlank()) {

                continue;
            }

            CharacterData data =
                    readJson(
                            resolve(
                                    base,
                                    characterResource
                            ),
                            CharacterData.class
                    );

            if (data.id == null
                    || data.id.isBlank()
                    || data.authoringVersion <= 0) {

                continue;
            }

            NpcId npc =
                    simulation.authoredIds()
                            .findNpc(
                                    data.id
                            )
                            .orElse(
                                    null
                            );

            if (npc == null) {

                /*
                 * This allows future scenarios to omit characters without
                 * making the whole server fail.
                 */
                KnownWorld.LOGGER.warn(
                        "Authored character overlay references NPC '{}' which is not present in the current scenario.",
                        data.id
                );

                continue;
            }

            CharacterProfile profile =
                    simulation.profiles()
                            .getOrCreate(
                                    npc
                            );

            String marker =
                    versionMarker(
                            data.authoringVersion
                    );

            if (profile.values()
                    .containsKey(
                            marker
                    )) {

                continue;
            }

            applyProfile(
                    profile,
                    data.profile
            );

            applySocial(
                    simulation,
                    npc,
                    data.social
            );

            applyTitles(
                    simulation,
                    npc,
                    data.titles
            );

            applyRelationships(
                    simulation,
                    npc,
                    data.relationships
            );

            /*
             * Must be last.
             *
             * If application throws above, no completion marker is written
             * and the migration will be attempted again after the problem
             * is fixed.
             */
            profile.setValue(
                    marker,
                    1.0
            );

            applied++;

            KnownWorld.LOGGER.info(
                    "Applied authored character profile v{} to {} (NPC #{}).",
                    data.authoringVersion,
                    data.id,
                    npc
            );
        }

        return applied;
    }

    private static void applyProfile(
            CharacterProfile profile,
            ProfileData data
    ) {

        if (data == null) {
            return;
        }

        if (hasText(
                data.culture
        )) {

            requireCulture(
                    data.culture
            );

            profile.setCulture(
                    data.culture
            );
        }

        if (hasText(
                data.religion
        )) {

            requireReligion(
                    data.religion
            );

            profile.setReligion(
                    data.religion
            );
        }

        if (hasText(
                data.education
        )) {

            profile.setEducation(
                    data.education
            );
        }

        if (hasText(
                data.birthplaceLocationId
        )) {

            profile.setBirthplaceLocationId(
                    data.birthplaceLocationId
            );
        }

        if (hasText(
                data.upbringingLocationId
        )) {

            profile.setUpbringingLocationId(
                    data.upbringingLocationId
            );
        }

        if (hasText(
                data.orientation
        )) {

            profile.setOrientation(
                    enumValue(
                            CharacterOrientation.class,
                            data.orientation,
                            "orientation"
                    )
            );
        }

        if (hasText(
                data.healthState
        )) {

            profile.setHealthState(
                    enumValue(
                            CharacterHealthState.class,
                            data.healthState,
                            "health state"
                    )
            );
        }

        if (hasText(
                data.legalStatus
        )) {

            profile.setLegalStatus(
                    enumValue(
                            CharacterLegalStatus.class,
                            data.legalStatus,
                            "legal status"
                    )
            );
        }

        if (data.wealth != null) {

            profile.setWealth(
                    data.wealth
            );
        }

        if (data.socialStatus != null) {

            profile.setSocialStatus(
                    data.socialStatus
            );
        }

        if (data.reputation != null) {

            profile.setReputation(
                    data.reputation
            );
        }

        if (data.skills != null) {

            for (
                    Map.Entry<String, Double> entry :
                    data.skills.entrySet()
            ) {

                profile.setSkill(
                        enumValue(
                                CharacterSkill.class,
                                entry.getKey(),
                                "character skill"
                        ),
                        entry.getValue()
                );
            }
        }

        if (data.aptitudes != null) {

            for (
                    Map.Entry<String, Double> entry :
                    data.aptitudes.entrySet()
            ) {

                profile.setAptitude(
                        enumValue(
                                CharacterAptitude.class,
                                entry.getKey(),
                                "character aptitude"
                        ),
                        entry.getValue()
                );
            }
        }

        if (data.dispositions != null) {

            for (
                    Map.Entry<String, Double> entry :
                    data.dispositions.entrySet()
            ) {

                profile.setDisposition(
                        enumValue(
                                CharacterDisposition.class,
                                entry.getKey(),
                                "character disposition"
                        ),
                        entry.getValue()
                );
            }
        }

        if (data.characterValues != null) {

            for (
                    Map.Entry<String, Double> entry :
                    data.characterValues.entrySet()
            ) {

                profile.setCharacterValue(
                        enumValue(
                                CharacterValue.class,
                                entry.getKey(),
                                "character value"
                        ),
                        entry.getValue()
                );
            }
        }

        if (data.socialNorms != null) {

            for (
                    Map.Entry<String, Double> entry :
                    data.socialNorms.entrySet()
            ) {

                profile.setSocialNorm(
                        enumValue(
                                CharacterSocialNorm.class,
                                entry.getKey(),
                                "character social norm"
                        ),
                        entry.getValue()
                );
            }
        }

        /*
         * Backwards-compatible custom values.
         *
         * This is also where the current culture/religion systems keep
         * their detailed personal state.
         */
        if (data.values != null) {

            for (
                    Map.Entry<String, Double> entry :
                    data.values.entrySet()
            ) {

                profile.setValue(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        if (data.politicalPreferences != null) {

            for (
                    Map.Entry<String, Double> entry :
                    data.politicalPreferences.entrySet()
            ) {

                profile.setPoliticalPreference(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        if (data.traits != null) {

            for (
                    String trait :
                    data.traits
            ) {

                if (hasText(
                        trait
                )) {

                    profile.addTrait(
                            trait
                    );
                }
            }
        }

        if (data.aliases != null) {

            for (
                    String alias :
                    data.aliases
            ) {

                if (hasText(
                        alias
                )) {

                    profile.addAlias(
                            alias
                    );
                }
            }
        }

        if (data.occupations != null) {

            for (
                    String occupation :
                    data.occupations
            ) {

                if (hasText(
                        occupation
                )) {

                    profile.addOccupation(
                            occupation
                    );
                }
            }
        }

        if (data.offices != null) {

            for (
                    String office :
                    data.offices
            ) {

                if (hasText(
                        office
                )) {

                    profile.addOffice(
                            office
                    );
                }
            }
        }

        if (data.courtRoles != null) {

            for (
                    String role :
                    data.courtRoles
            ) {

                if (hasText(
                        role
                )) {

                    profile.addCourtRole(
                            role
                    );
                }
            }
        }

        if (data.militaryRoles != null) {

            for (
                    String role :
                    data.militaryRoles
            ) {

                if (hasText(
                        role
                )) {

                    profile.addMilitaryRole(
                            role
                    );
                }
            }
        }

        if (data.combatSpecialties != null) {

            for (
                    String specialty :
                    data.combatSpecialties
            ) {

                if (hasText(
                        specialty
                )) {

                    profile.addCombatSpecialty(
                            specialty
                    );
                }
            }
        }

        if (data.languages != null) {

            for (
                    Map.Entry<String, Double> entry :
                    data.languages.entrySet()
            ) {

                requireLanguage(
                        entry.getKey()
                );

                profile.setLanguageProficiency(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        /*
         * These lists are authored baselines.
         *
         * Clear the old compact scenario versions before applying the
         * richer gold-standard versions.
         */
        if (data.motivations != null) {

            profile.clearMotivations();

            for (
                    String motivation :
                    data.motivations
            ) {

                profile.addMotivation(
                        motivation
                );
            }
        }

        if (data.goals != null) {

            profile.clearGoals();

            for (
                    String goal :
                    data.goals
            ) {

                profile.addGoal(
                        goal
                );
            }
        }

        if (data.fears != null) {

            profile.clearFears();

            for (
                    String fear :
                    data.fears
            ) {

                profile.addFear(
                        fear
                );
            }
        }

        if (data.desires != null) {

            profile.clearDesires();

            for (
                    String desire :
                    data.desires
            ) {

                profile.addDesire(
                        desire
                );
            }
        }

        if (data.secrets != null) {

            for (
                    String secret :
                    data.secrets
            ) {

                if (hasText(
                        secret
                )) {

                    profile.addSecret(
                            secret
                    );
                }
            }
        }

        if (data.knownSecrets != null) {

            for (
                    String secret :
                    data.knownSecrets
            ) {

                if (hasText(
                        secret
                )) {

                    profile.addKnownSecret(
                            secret
                    );
                }
            }
        }

        if (data.publicFacts != null) {

            for (
                    Map.Entry<String, String> entry :
                    data.publicFacts.entrySet()
            ) {

                profile.setPublicFact(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        if (data.privateFacts != null) {

            for (
                    Map.Entry<String, String> entry :
                    data.privateFacts.entrySet()
            ) {

                profile.setPrivateFact(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        if (data.appearance != null) {

            if (data.appearance.description != null) {

                profile.setAppearanceDescription(
                        data.appearance.description
                );
            }

            if (data.appearance.hair != null) {

                profile.setHairDescription(
                        data.appearance.hair
                );
            }

            if (data.appearance.eyes != null) {

                profile.setEyeDescription(
                        data.appearance.eyes
                );
            }

            if (data.appearance.build != null) {

                profile.setBuildDescription(
                        data.appearance.build
                );
            }
        }

        if (data.dialogue != null) {

            profile.setDialoguePersona(
                    new DialoguePersona(
                            valueOrZero(
                                    data.dialogue.formality
                            ),
                            valueOrZero(
                                    data.dialogue.verbosity
                            ),
                            valueOrZero(
                                    data.dialogue.warmth
                            ),
                            valueOrZero(
                                    data.dialogue.directness
                            ),
                            data.dialogue.preferredAddress,
                            data.dialogue.guidance
                    )
            );
        }
    }

    private static void applySocial(
            NpcSimulation simulation,
            NpcId npc,
            SocialData social
    ) {

        if (social == null) {
            return;
        }

        if (hasText(
                social.residence
        )) {

            simulation.affiliations()
                    .setResidence(
                            npc,
                            simulation.authoredIds()
                                    .requireSettlement(
                                            social.residence
                                    )
                    );
        }

        if (hasText(
                social.household
        )) {

            simulation.affiliations()
                    .setHousehold(
                            npc,
                            simulation.authoredIds()
                                    .requireOrganization(
                                            social.household
                                    )
                    );
        }

        if (hasText(
                social.nobleHouse
        )) {

            simulation.affiliations()
                    .setNobleHouse(
                            npc,
                            simulation.authoredIds()
                                    .requireOrganization(
                                            social.nobleHouse
                                    )
                    );
        }

        if (hasText(
                social.faction
        )) {

            simulation.affiliations()
                    .setFaction(
                            npc,
                            simulation.authoredIds()
                                    .requireOrganization(
                                            social.faction
                                    )
                    );
        }
    }

    private static void applyTitles(
            NpcSimulation simulation,
            NpcId npc,
            List<String> titles
    ) {

        if (titles == null) {
            return;
        }

        for (
                String title :
                titles
        ) {

            if (!hasText(
                    title
            )) {

                continue;
            }

            simulation.titles()
                    .grant(
                            simulation.authoredIds()
                                    .requireTitle(
                                            title
                                    ),
                            npc,
                            simulation.serverTickCounter()
                    );
        }
    }

    private static void applyRelationships(
            NpcSimulation simulation,
            NpcId subject,
            List<RelationshipData> relationships
    ) {

        if (relationships == null) {
            return;
        }

        for (
                RelationshipData relationship :
                relationships
        ) {

            if (!hasText(
                    relationship.target
            )) {

                continue;
            }

            NpcId target =
                    simulation.authoredIds()
                            .requireNpc(
                                    relationship.target
                            );

            if (subject.equals(
                    target
            )) {

                throw new IllegalArgumentException(
                        "Authored character cannot have a relationship with itself"
                );
            }

            simulation.relationships()
                    .registerLoaded(
                            new NpcRelationship(
                                    subject,
                                    target,
                                    relationship.affection,
                                    relationship.trust,
                                    relationship.respect,
                                    relationship.fear,
                                    relationship.familiarity
                            )
                    );
        }
    }

    private static void requireCulture(
            String cultureId
    ) {

        if (WorldReferenceCatalog.get()
                .culture(
                        cultureId
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown authored culture: "
                            + cultureId
            );
        }
    }

    private static void requireReligion(
            String religionId
    ) {

        if (WorldReferenceCatalog.get()
                .religion(
                        religionId
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown authored religion: "
                            + religionId
            );
        }
    }

    private static void requireLanguage(
            String languageId
    ) {

        if (WorldReferenceCatalog.get()
                .language(
                        languageId
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown authored language: "
                            + languageId
            );
        }
    }

    private static String versionMarker(
            int version
    ) {

        return "authoring.character_overlay_v"
                + version;
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String raw,
            String description
    ) {

        if (!hasText(
                raw
        )) {

            throw new IllegalArgumentException(
                    "Missing "
                            + description
            );
        }

        try {

            return Enum.valueOf(
                    type,
                    raw.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {

            throw new IllegalArgumentException(
                    "Unknown "
                            + description
                            + ": "
                            + raw,
                    exception
            );
        }
    }

    private static double valueOrZero(
            Double value
    ) {

        return value == null
                ? 0.0
                : value;
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static String parentPath(
            String resource
    ) {

        int index =
                resource.lastIndexOf(
                        '/'
                );

        return index < 0
                ? ""
                : resource.substring(
                0,
                index + 1
        );
    }

    private static String resolve(
            String base,
            String child
    ) {

        if (child == null
                || child.isBlank()) {

            throw new IllegalArgumentException(
                    "Scenario character resource path cannot be empty"
            );
        }

        return base
                + child;
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        AuthoredCharacterProfileOverlayService.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Resource not found: "
                                + resource
                );
            }

            try (
                    Reader reader =
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
            ) {

                T result =
                        GSON.fromJson(
                                reader,
                                type
                        );

                if (result == null) {

                    throw new IOException(
                            "JSON resource produced null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid JSON in "
                            + resource,
                    exception
            );
        }
    }

    private static final class ScenarioIndex {

        List<String> characters;
    }

    private static final class CharacterData {

        String id;

        int authoringVersion;

        ProfileData profile;

        SocialData social;

        List<String> titles;

        List<RelationshipData> relationships;
    }

    private static final class ProfileData {

        String culture;

        String religion;

        String education;

        String birthplaceLocationId;

        String upbringingLocationId;

        String orientation;

        String healthState;

        String legalStatus;

        Double wealth;

        Double socialStatus;

        Double reputation;

        Map<String, Double> skills;

        Map<String, Double> aptitudes;

        Map<String, Double> dispositions;

        Map<String, Double> characterValues;

        Map<String, Double> socialNorms;

        Map<String, Double> values;

        Map<String, Double> politicalPreferences;

        List<String> traits;

        List<String> aliases;

        List<String> occupations;

        List<String> offices;

        List<String> courtRoles;

        List<String> militaryRoles;

        List<String> combatSpecialties;

        Map<String, Double> languages;

        List<String> motivations;

        List<String> goals;

        List<String> fears;

        List<String> desires;

        List<String> secrets;

        List<String> knownSecrets;

        Map<String, String> publicFacts;

        Map<String, String> privateFacts;

        AppearanceData appearance;

        DialogueData dialogue;
    }

    private static final class AppearanceData {

        String description;

        String hair;

        String eyes;

        String build;
    }

    private static final class DialogueData {

        Double formality;

        Double verbosity;

        Double warmth;

        Double directness;

        String preferredAddress;

        String guidance;
    }

    private static final class SocialData {

        String residence;

        String household;

        String nobleHouse;

        String faction;
    }

    private static final class RelationshipData {

        String target;

        double affection;

        double trust;

        double respect;

        double fear;

        double familiarity;
    }
}
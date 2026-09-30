package dev.dtzudontsay.knownworld.simulation.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterBirthStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCivilStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCustodyStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterFreedomStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterLegalState;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AuthoredCharacterProfileOverlayService {

    private static final Gson GSON =
            new Gson();

    private static final String DEFAULT_SCENARIO =
            ScenarioBootstrapper.DEFAULT_SCENARIO;

    private AuthoredCharacterProfileOverlayService() {
    }

    public static void registerLifecycle() {

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

        String base =
                parentPath(
                        scenarioResource
                );

        PopulationPackCatalog.Plan populationPlan =
                PopulationPackCatalog.load(
                        scenarioResource,
                        scenario.populationPacks
                );

        List<String> resources =
                collectCharacterResources(
                        base,
                        scenario.characters,
                        populationPlan.characterResources()
                );

        int applied =
                0;

        for (
                String resource :
                resources
        ) {

            CharacterData data =
                    readJson(
                            resource,
                            CharacterData.class
                    );

            if (!hasText(
                    data.id
            )
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

            applyLegal(
                    simulation,
                    npc,
                    data.legal
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

    @SuppressWarnings("deprecation")
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
                            "legacy legal status"
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
                    String value :
                    data.traits
            ) {

                if (hasText(
                        value
                )) {

                    profile.addTrait(
                            value
                    );
                }
            }
        }

        if (data.aliases != null) {

            for (
                    String value :
                    data.aliases
            ) {

                if (hasText(
                        value
                )) {

                    profile.addAlias(
                            value
                    );
                }
            }
        }

        if (data.occupations != null) {

            for (
                    String value :
                    data.occupations
            ) {

                if (!hasText(
                        value
                )) {

                    continue;
                }

                requireOccupation(
                        value
                );

                profile.addOccupation(
                        value
                );
            }
        }

        if (data.offices != null) {

            for (
                    String value :
                    data.offices
            ) {

                if (hasText(
                        value
                )) {

                    profile.addOffice(
                            value
                    );
                }
            }
        }

        if (data.courtRoles != null) {

            for (
                    String value :
                    data.courtRoles
            ) {

                if (hasText(
                        value
                )) {

                    profile.addCourtRole(
                            value
                    );
                }
            }
        }

        if (data.militaryRoles != null) {

            for (
                    String value :
                    data.militaryRoles
            ) {

                if (!hasText(
                        value
                )) {

                    continue;
                }

                requireRole(
                        value
                );

                profile.addMilitaryRole(
                        value
                );
            }
        }

        if (data.combatSpecialties != null) {

            for (
                    String value :
                    data.combatSpecialties
            ) {

                if (hasText(
                        value
                )) {

                    profile.addCombatSpecialty(
                            value
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

        if (data.motivations != null) {

            profile.clearMotivations();

            for (
                    String value :
                    data.motivations
            ) {

                profile.addMotivation(
                        value
                );
            }
        }

        if (data.goals != null) {

            profile.clearGoals();

            for (
                    String value :
                    data.goals
            ) {

                profile.addGoal(
                        value
                );
            }
        }

        if (data.fears != null) {

            profile.clearFears();

            for (
                    String value :
                    data.fears
            ) {

                profile.addFear(
                        value
                );
            }
        }

        if (data.desires != null) {

            profile.clearDesires();

            for (
                    String value :
                    data.desires
            ) {

                profile.addDesire(
                        value
                );
            }
        }

        if (data.secrets != null) {

            for (
                    String value :
                    data.secrets
            ) {

                if (hasText(
                        value
                )) {

                    profile.addSecret(
                            value
                    );
                }
            }
        }

        if (data.knownSecrets != null) {

            for (
                    String value :
                    data.knownSecrets
            ) {

                if (hasText(
                        value
                )) {

                    profile.addKnownSecret(
                            value
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

    private static void applyLegal(
            NpcSimulation simulation,
            NpcId npc,
            LegalData data
    ) {

        if (data == null) {
            return;
        }

        CharacterLegalState state =
                simulation.legalStates()
                        .getOrCreate(
                                npc
                        );

        if (hasText(
                data.birth
        )) {

            state.setBirthStatus(
                    enumValue(
                            CharacterBirthStatus.class,
                            data.birth,
                            "birth status"
                    )
            );
        }

        if (hasText(
                data.freedom
        )) {

            state.setFreedomStatus(
                    enumValue(
                            CharacterFreedomStatus.class,
                            data.freedom,
                            "freedom status"
                    )
            );
        }

        if (hasText(
                data.custody
        )) {

            state.setCustodyStatus(
                    enumValue(
                            CharacterCustodyStatus.class,
                            data.custody,
                            "custody status"
                    )
            );
        }

        if (hasText(
                data.civil
        )) {

            state.setCivilStatus(
                    enumValue(
                            CharacterCivilStatus.class,
                            data.civil,
                            "civil status"
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
                        "Authored character cannot have relationship with itself"
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

    private static List<String> collectCharacterResources(
            String base,
            List<String> legacy,
            List<String> packed
    ) {

        List<String> result =
                new ArrayList<>();

        Set<String> seen =
                new LinkedHashSet<>();

        if (legacy != null) {

            for (
                    String resource :
                    legacy
            ) {

                if (!hasText(
                        resource
                )) {

                    continue;
                }

                String resolved =
                        resolve(
                                base,
                                resource
                        );

                if (seen.add(
                        resolved
                )) {

                    result.add(
                            resolved
                    );
                }
            }
        }

        for (
                String resource :
                packed
        ) {

            if (!seen.add(
                    resource
            )) {

                throw new IllegalArgumentException(
                        "Duplicate character overlay resource "
                                + resource
                );
            }

            result.add(
                    resource
            );
        }

        return List.copyOf(
                result
        );
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

    private static void requireOccupation(
            String occupationId
    ) {

        if (WorldReferenceCatalog.get()
                .occupation(
                        occupationId
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown authored occupation: "
                            + occupationId
            );
        }
    }

    private static void requireRole(
            String roleId
    ) {

        if (WorldReferenceCatalog.get()
                .role(
                        roleId
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown authored role: "
                            + roleId
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
                index
        );
    }

    private static String resolve(
            String base,
            String child
    ) {

        if (!hasText(
                child
        )) {

            throw new IllegalArgumentException(
                    "Scenario character resource path cannot be empty"
            );
        }

        if (child.startsWith(
                "data/"
        )) {

            return child;
        }

        return base
                + "/"
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

        String populationPacks;

        List<String> characters;
    }

    private static final class CharacterData {

        String id;

        int authoringVersion;

        ProfileData profile;

        LegalData legal;

        SocialData social;

        List<String> titles;

        List<RelationshipData> relationships;
    }

    private static final class LegalData {

        String birth;

        String freedom;

        String custody;

        String civil;
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
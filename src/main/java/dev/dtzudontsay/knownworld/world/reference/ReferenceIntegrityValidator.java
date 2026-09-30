package dev.dtzudontsay.knownworld.world.reference;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.bootstrap.PopulationPackCatalog;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioBootstrapper;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.family.DynastyInheritanceRule;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterBirthStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCivilStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCustodyStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterFreedomStatus;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoleType;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReferenceIntegrityValidator {

    private static final Gson GSON =
            new Gson();

    private ReferenceIntegrityValidator() {
    }

    public static void validateDefaultScenario() {

        try {

            validateScenario(
                    ScenarioBootstrapper.DEFAULT_SCENARIO
            );

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Known World reference integrity validation failed",
                    exception
            );
        }
    }

    public static void validateScenario(
            String scenarioResource
    ) throws IOException {

        ScenarioData scenario =
                readJson(
                        scenarioResource,
                        ScenarioData.class
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

        Set<String> settlementIds =
                loadSettlementIds(
                        base,
                        scenario.settlements
                );

        Set<String> organizationIds =
                loadAndValidateOrganizations(
                        base,
                        scenario.organizations,
                        settlementIds
                );

        Set<String> titleIds =
                loadAndValidateTitles(
                        base,
                        scenario.titles,
                        organizationIds,
                        settlementIds
                );

        List<String> characterResources =
                collectCharacterResources(
                        base,
                        scenario.characters,
                        populationPlan.characterResources()
                );

        List<CharacterData> characters =
                loadCharacters(
                        characterResources
                );

        Set<String> characterIds =
                new LinkedHashSet<>();

        for (
                CharacterData character :
                characters
        ) {

            String id =
                    requireId(
                            character.id,
                            "character id"
                    );

            if (!characterIds.add(
                    id
            )) {

                throw new IllegalStateException(
                        "Duplicate authored character ID "
                                + id
                );
            }
        }

        for (
                CharacterData character :
                characters
        ) {

            validateCharacter(
                    character,
                    characterIds,
                    organizationIds,
                    settlementIds,
                    titleIds
            );
        }

        List<String> parentageResources =
                collectSingleAndPackResources(
                        base,
                        scenario.parentages,
                        populationPlan.parentageResources()
                );

        List<String> marriageResources =
                collectSingleAndPackResources(
                        base,
                        scenario.marriages,
                        populationPlan.marriageResources()
                );

        int parentageCount =
                validateParentages(
                        parentageResources,
                        characterIds
                );

        int marriageCount =
                validateMarriages(
                        marriageResources,
                        characterIds
                );

        KnownWorld.LOGGER.info(
                "Reference integrity validation passed for scenario {}: {} population packs, {} characters, {} parentages, {} marriages, {} settlements, {} organizations and {} titles.",
                scenarioResource,
                populationPlan.enabledPackCount(),
                characterIds.size(),
                parentageCount,
                marriageCount,
                settlementIds.size(),
                organizationIds.size(),
                titleIds.size()
        );
    }

    private static Set<String> loadSettlementIds(
            String base,
            String resource
    ) throws IOException {

        if (!hasText(
                resource
        )) {

            return Set.of();
        }

        SettlementData[] entries =
                readJson(
                        resolve(
                                base,
                                resource
                        ),
                        SettlementData[].class
                );

        Set<String> result =
                new LinkedHashSet<>();

        for (
                SettlementData entry :
                entries
        ) {

            String id =
                    requireId(
                            entry.id,
                            "settlement id"
                    );

            if (!result.add(
                    id
            )) {

                throw new IllegalStateException(
                        "Duplicate scenario settlement "
                                + id
                );
            }
        }

        return Set.copyOf(
                result
        );
    }

    private static Set<String> loadAndValidateOrganizations(
            String base,
            String resource,
            Set<String> settlementIds
    ) throws IOException {

        if (!hasText(
                resource
        )) {

            return Set.of();
        }

        OrganizationData[] entries =
                readJson(
                        resolve(
                                base,
                                resource
                        ),
                        OrganizationData[].class
                );

        Set<String> result =
                new LinkedHashSet<>();

        for (
                OrganizationData entry :
                entries
        ) {

            String id =
                    requireId(
                            entry.id,
                            "organization id"
                    );

            if (!result.add(
                    id
            )) {

                throw new IllegalStateException(
                        "Duplicate scenario organization "
                                + id
                );
            }

            enumValue(
                    OrganizationType.class,
                    entry.type,
                    "organization type for "
                            + id
            );

            if (hasText(
                    entry.seatSettlement
            )
                    && !settlementIds.contains(
                    normalizeId(
                            entry.seatSettlement
                    )
            )) {

                throw new IllegalStateException(
                        "Organization "
                                + id
                                + " references unknown settlement "
                                + entry.seatSettlement
                );
            }
        }

        return Set.copyOf(
                result
        );
    }

    private static Set<String> loadAndValidateTitles(
            String base,
            String resource,
            Set<String> organizationIds,
            Set<String> settlementIds
    ) throws IOException {

        if (!hasText(
                resource
        )) {

            return Set.of();
        }

        TitleData[] entries =
                readJson(
                        resolve(
                                base,
                                resource
                        ),
                        TitleData[].class
                );

        Set<String> result =
                new LinkedHashSet<>();

        for (
                TitleData entry :
                entries
        ) {

            String id =
                    requireId(
                            entry.id,
                            "title id"
                    );

            if (!result.add(
                    id
            )) {

                throw new IllegalStateException(
                        "Duplicate scenario title "
                                + id
                );
            }

            enumValue(
                    TitleType.class,
                    entry.type,
                    "title type for "
                            + id
            );

            if (entry.authority < 0
                    || entry.authority > 100) {

                throw new IllegalStateException(
                        "Title "
                                + id
                                + " has authority outside 0..100"
                );
            }

            if (hasText(
                    entry.organization
            )
                    && !organizationIds.contains(
                    normalizeId(
                            entry.organization
                    )
            )) {

                throw new IllegalStateException(
                        "Title "
                                + id
                                + " references unknown organization "
                                + entry.organization
                );
            }

            if (hasText(
                    entry.settlement
            )
                    && !settlementIds.contains(
                    normalizeId(
                            entry.settlement
                    )
            )) {

                throw new IllegalStateException(
                        "Title "
                                + id
                                + " references unknown settlement "
                                + entry.settlement
                );
            }
        }

        return Set.copyOf(
                result
        );
    }

    private static List<CharacterData> loadCharacters(
            List<String> resources
    ) throws IOException {

        List<CharacterData> result =
                new ArrayList<>();

        for (
                String resource :
                resources
        ) {

            result.add(
                    readJson(
                            resource,
                            CharacterData.class
                    )
            );
        }

        return List.copyOf(
                result
        );
    }

    private static void validateCharacter(
            CharacterData character,
            Set<String> characterIds,
            Set<String> organizationIds,
            Set<String> settlementIds,
            Set<String> titleIds
    ) {

        String id =
                requireId(
                        character.id,
                        "character id"
                );

        if (character.identity == null) {

            throw new IllegalStateException(
                    "Character "
                            + id
                            + " has no identity"
            );
        }

        if (!hasText(
                character.identity.givenName
        )) {

            throw new IllegalStateException(
                    "Character "
                            + id
                            + " has no givenName"
            );
        }

        if (!hasText(
                character.identity.givenName
        )) {

            throw new IllegalStateException(
                    "Character "
                            + id
                            + " has no givenName"
            );
        }

        enumValue(
                NpcSex.class,
                character.identity.sex,
                "NPC sex for "
                        + id
        );


        if (hasText(
                character.startingSettlement
        )
                && !settlementIds.contains(
                normalizeId(
                        character.startingSettlement
                )
        )) {

            throw new IllegalStateException(
                    "Character "
                            + id
                            + " references unknown starting settlement "
                            + character.startingSettlement
            );
        }

        if (!hasText(
                character.startingSettlement
        )
                && character.position == null) {

            throw new IllegalStateException(
                    "Character "
                            + id
                            + " has neither startingSettlement nor position"
            );
        }

        WorldReferenceCatalog references =
                WorldReferenceCatalog.get();

        ProfileData profile =
                character.profile;

        if (profile != null) {

            if (hasText(
                    profile.culture
            )
                    && references.culture(
                    profile.culture
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Character "
                                + id
                                + " references unknown culture "
                                + profile.culture
                );
            }

            if (hasText(
                    profile.religion
            )
                    && references.religion(
                    profile.religion
            ).isEmpty()) {

                throw new IllegalStateException(
                        "Character "
                                + id
                                + " references unknown religion "
                                + profile.religion
                );
            }

            if (profile.skills != null) {

                for (
                        String skill :
                        profile.skills.keySet()
                ) {

                    enumValue(
                            CharacterSkill.class,
                            skill,
                            "character skill for "
                                    + id
                    );
                }
            }

            if (profile.languages != null) {

                for (
                        String language :
                        profile.languages.keySet()
                ) {

                    if (references.language(
                            language
                    ).isEmpty()) {

                        throw new IllegalStateException(
                                "Character "
                                        + id
                                        + " references unknown language "
                                        + language
                        );
                    }
                }
            }

            if (profile.occupations != null) {

                for (
                        String occupation :
                        profile.occupations
                ) {

                    if (references.occupation(
                            occupation
                    ).isEmpty()) {

                        throw new IllegalStateException(
                                "Character "
                                        + id
                                        + " references unknown occupation "
                                        + occupation
                        );
                    }
                }
            }

            validateRoleOrTitleList(
                    id,
                    "office",
                    profile.offices,
                    references,
                    titleIds
            );

            validateRoleOrTitleList(
                    id,
                    "court role",
                    profile.courtRoles,
                    references,
                    titleIds
            );

            if (profile.militaryRoles != null) {

                for (
                        String role :
                        profile.militaryRoles
                ) {

                    if (references.role(
                            role
                    ).isEmpty()) {

                        throw new IllegalStateException(
                                "Character "
                                        + id
                                        + " references unknown military role "
                                        + role
                        );
                    }
                }
            }
        }

        if (character.legal != null) {

            if (hasText(
                    character.legal.birth
            )) {

                enumValue(
                        CharacterBirthStatus.class,
                        character.legal.birth,
                        "birth status for "
                                + id
                );
            }

            if (hasText(
                    character.legal.freedom
            )) {

                enumValue(
                        CharacterFreedomStatus.class,
                        character.legal.freedom,
                        "freedom status for "
                                + id
                );
            }

            if (hasText(
                    character.legal.custody
            )) {

                enumValue(
                        CharacterCustodyStatus.class,
                        character.legal.custody,
                        "custody status for "
                                + id
                );
            }

            if (hasText(
                    character.legal.civil
            )) {

                enumValue(
                        CharacterCivilStatus.class,
                        character.legal.civil,
                        "civil status for "
                                + id
                );
            }
        }

        if (character.social != null) {

            validateOrganizationReference(
                    id,
                    "household",
                    character.social.household,
                    organizationIds
            );

            validateOrganizationReference(
                    id,
                    "noble house",
                    character.social.nobleHouse,
                    organizationIds
            );

            validateOrganizationReference(
                    id,
                    "faction",
                    character.social.faction,
                    organizationIds
            );

            if (hasText(
                    character.social.residence
            )
                    && !settlementIds.contains(
                    normalizeId(
                            character.social.residence
                    )
            )) {

                throw new IllegalStateException(
                        "Character "
                                + id
                                + " references unknown residence "
                                + character.social.residence
                );
            }
        }

        if (character.routine != null) {

            if (hasText(
                    character.routine.role
            )) {

                enumValue(
                        NpcRoleType.class,
                        character.routine.role,
                        "NPC routine role for "
                                + id
                );
            }

            validateSettlementReference(
                    id,
                    "routine home settlement",
                    character.routine.homeSettlement,
                    settlementIds
            );

            validateSettlementReference(
                    id,
                    "routine work settlement",
                    character.routine.workSettlement,
                    settlementIds
            );
        }

        if (character.titles != null) {

            for (
                    String title :
                    character.titles
            ) {

                if (!titleIds.contains(
                        normalizeId(
                                title
                        )
                )) {

                    throw new IllegalStateException(
                            "Character "
                                    + id
                                    + " references unknown title "
                                    + title
                    );
                }
            }
        }

        if (character.relationships != null) {

            for (
                    RelationshipData relationship :
                    character.relationships
            ) {

                requireKnownCharacter(
                        id,
                        "relationship target",
                        relationship.target,
                        characterIds
                );
            }
        }

        if (character.beliefs != null) {

            for (
                    BeliefData belief :
                    character.beliefs
            ) {

                if (hasText(
                        belief.sourceNpc
                )) {

                    requireKnownCharacter(
                            id,
                            "belief source",
                            belief.sourceNpc,
                            characterIds
                    );
                }
            }
        }

        if (character.memories != null) {

            for (
                    MemoryData memory :
                    character.memories
            ) {

                if (hasText(
                        memory.relatedNpc
                )) {

                    requireKnownCharacter(
                            id,
                            "memory related NPC",
                            memory.relatedNpc,
                            characterIds
                    );
                }
            }
        }
    }

    private static int validateParentages(
            List<String> resources,
            Set<String> characterIds
    ) throws IOException {

        int count =
                0;

        Set<String> children =
                new HashSet<>();

        for (
                String resource :
                resources
        ) {

            ParentageData[] entries =
                    readJson(
                            resource,
                            ParentageData[].class
                    );

            for (
                    ParentageData entry :
                    entries
            ) {

                String child =
                        requireId(
                                entry.child,
                                "parentage child"
                        );

                if (!characterIds.contains(
                        child
                )) {

                    throw new IllegalStateException(
                            "Parentage references unknown child "
                                    + child
                    );
                }

                if (!children.add(
                        child
                )) {

                    throw new IllegalStateException(
                            "Duplicate parentage definition for "
                                    + child
                    );
                }

                if (hasText(
                        entry.mother
                )) {

                    requireKnownCharacter(
                            child,
                            "mother",
                            entry.mother,
                            characterIds
                    );
                }

                if (hasText(
                        entry.father
                )) {

                    requireKnownCharacter(
                            child,
                            "father",
                            entry.father,
                            characterIds
                    );
                }

                count++;
            }
        }

        return count;
    }

    private static int validateMarriages(
            List<String> resources,
            Set<String> characterIds
    ) throws IOException {

        int count =
                0;

        for (
                String resource :
                resources
        ) {

            MarriageData[] entries =
                    readJson(
                            resource,
                            MarriageData[].class
                    );

            for (
                    MarriageData entry :
                    entries
            ) {

                String first =
                        requireId(
                                entry.first,
                                "marriage first"
                        );

                String second =
                        requireId(
                                entry.second,
                                "marriage second"
                        );

                if (!characterIds.contains(
                        first
                )
                        || !characterIds.contains(
                        second
                )) {

                    throw new IllegalStateException(
                            "Marriage references unknown NPC(s): "
                                    + first
                                    + " / "
                                    + second
                    );
                }

                if (first.equals(
                        second
                )) {

                    throw new IllegalStateException(
                            "NPC cannot marry itself: "
                                    + first
                    );
                }

                if (hasText(
                        entry.inheritanceRule
                )) {

                    enumValue(
                            DynastyInheritanceRule.class,
                            entry.inheritanceRule,
                            "dynasty inheritance rule"
                    );
                }

                count++;
            }
        }

        return count;
    }

    private static void validateRoleOrTitleList(
            String characterId,
            String description,
            List<String> values,
            WorldReferenceCatalog references,
            Set<String> titleIds
    ) {

        if (values == null) {
            return;
        }

        for (
                String value :
                values
        ) {

            String normalized =
                    normalizeId(
                            value
                    );

            if (references.role(
                    normalized
            ).isPresent()
                    || titleIds.contains(
                    normalized
            )) {

                continue;
            }

            throw new IllegalStateException(
                    "Character "
                            + characterId
                            + " references unknown "
                            + description
                            + " "
                            + value
            );
        }
    }

    private static void validateOrganizationReference(
            String characterId,
            String description,
            String organizationId,
            Set<String> organizationIds
    ) {

        if (!hasText(
                organizationId
        )) {

            return;
        }

        if (!organizationIds.contains(
                normalizeId(
                        organizationId
                )
        )) {

            throw new IllegalStateException(
                    "Character "
                            + characterId
                            + " references unknown "
                            + description
                            + " "
                            + organizationId
            );
        }
    }

    private static void validateSettlementReference(
            String characterId,
            String description,
            String settlementId,
            Set<String> settlementIds
    ) {

        if (!hasText(
                settlementId
        )) {

            return;
        }

        if (!settlementIds.contains(
                normalizeId(
                        settlementId
                )
        )) {

            throw new IllegalStateException(
                    "Character "
                            + characterId
                            + " references unknown "
                            + description
                            + " "
                            + settlementId
            );
        }
    }

    private static void requireKnownCharacter(
            String owner,
            String description,
            String target,
            Set<String> characterIds
    ) {

        if (!hasText(
                target
        )) {

            throw new IllegalStateException(
                    owner
                            + " has blank "
                            + description
            );
        }

        String normalized =
                normalizeId(
                        target
                );

        if (!characterIds.contains(
                normalized
        )) {

            throw new IllegalStateException(
                    owner
                            + " references unknown "
                            + description
                            + " "
                            + target
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

                if (!seen.add(
                        resolved
                )) {

                    throw new IllegalStateException(
                            "Duplicate character resource "
                                    + resolved
                    );
                }

                result.add(
                        resolved
                );
            }
        }

        for (
                String resource :
                packed
        ) {

            if (!seen.add(
                    resource
            )) {

                throw new IllegalStateException(
                        "Duplicate character resource "
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

    private static List<String> collectSingleAndPackResources(
            String base,
            String legacy,
            List<String> packed
    ) {

        List<String> result =
                new ArrayList<>();

        Set<String> seen =
                new LinkedHashSet<>();

        if (hasText(
                legacy
        )) {

            String resolved =
                    resolve(
                            base,
                            legacy
                    );

            seen.add(
                    resolved
            );

            result.add(
                    resolved
            );
        }

        for (
                String resource :
                packed
        ) {

            if (!seen.add(
                    resource
            )) {

                throw new IllegalStateException(
                        "Duplicate population resource "
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

    private static String requireId(
            String value,
            String description
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalStateException(
                    "Missing "
                            + description
            );
        }

        return normalizeId(
                value
        );
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static String normalizeId(
            String value
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalArgumentException(
                    "ID cannot be blank"
            );
        }

        return value.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String raw,
            String description
    ) {

        if (!hasText(
                raw
        )) {

            throw new IllegalStateException(
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

            throw new IllegalStateException(
                    "Unknown "
                            + description
                            + ": "
                            + raw,
                    exception
            );
        }
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
                    "Scenario resource reference cannot be blank"
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
                        ReferenceIntegrityValidator.class
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

                T value =
                        GSON.fromJson(
                                reader,
                                type
                        );

                if (value == null) {

                    throw new IOException(
                            "JSON returned null: "
                                    + resource
                    );
                }

                return value;
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

    private static final class ScenarioData {

        String settlements;

        String organizations;

        String titles;

        String populationPacks;

        List<String> characters;

        String parentages;

        String marriages;
    }

    private static final class SettlementData {

        String id;
    }

    private static final class OrganizationData {

        String id;

        String type;

        String seatSettlement;
    }

    private static final class TitleData {

        String id;

        String type;

        int authority;

        String organization;

        String settlement;
    }

    private static final class CharacterData {

        String id;

        IdentityData identity;

        String startingSettlement;

        PositionData position;

        ProfileData profile;

        LegalData legal;

        SocialData social;

        RoutineData routine;

        List<String> titles;

        List<RelationshipData> relationships;

        List<BeliefData> beliefs;

        List<MemoryData> memories;
    }

    private static final class IdentityData {

        String givenName;

        String familyName;

        String sex;

        int birthYear;
    }

    private static final class PositionData {

        String dimension;

        double x;

        double y;

        double z;
    }

    private static final class ProfileData {

        String culture;

        String religion;

        Map<String, Double> skills;

        Map<String, Double> languages;

        List<String> occupations;

        List<String> offices;

        List<String> courtRoles;

        List<String> militaryRoles;
    }

    private static final class LegalData {

        String birth;

        String freedom;

        String custody;

        String civil;
    }

    private static final class SocialData {

        String residence;

        String household;

        String nobleHouse;

        String faction;
    }

    private static final class RoutineData {

        String role;

        String homeSettlement;

        String workSettlement;
    }

    private static final class RelationshipData {

        String target;
    }

    private static final class BeliefData {

        String sourceNpc;
    }

    private static final class MemoryData {

        String relatedNpc;
    }

    private static final class ParentageData {

        String child;

        String mother;

        String father;
    }

    private static final class MarriageData {

        String first;

        String second;

        String inheritanceRule;
    }
}
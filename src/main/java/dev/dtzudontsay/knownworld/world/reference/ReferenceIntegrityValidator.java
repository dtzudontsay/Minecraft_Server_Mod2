package dev.dtzudontsay.knownworld.world.reference;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.bootstrap.ScenarioBootstrapper;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterBirthStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCivilStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterCustodyStatus;
import dev.dtzudontsay.knownworld.simulation.npc.legal.CharacterFreedomStatus;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

        List<CharacterData> characters =
                loadCharacters(
                        base,
                        scenario.characters
                );

        Set<String> characterIds =
                new HashSet<>();

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

        KnownWorld.LOGGER.info(
                "Reference integrity validation passed for scenario {}: {} characters, {} settlements, {} organizations and {} titles.",
                scenarioResource,
                characterIds.size(),
                settlementIds.size(),
                organizationIds.size(),
                titleIds.size()
        );
    }

    /*
     * =========================================================
     * SETTLEMENTS
     * =========================================================
     */

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
                new HashSet<>();

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

    /*
     * =========================================================
     * ORGANIZATIONS
     * =========================================================
     */

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
                new HashSet<>();

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

    /*
     * =========================================================
     * TITLES
     * =========================================================
     */

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
                new HashSet<>();

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

    /*
     * =========================================================
     * CHARACTERS
     * =========================================================
     */

    private static List<CharacterData> loadCharacters(
            String base,
            List<String> resources
    ) throws IOException {

        if (resources == null) {
            return List.of();
        }

        java.util.ArrayList<CharacterData> result =
                new java.util.ArrayList<>();

        for (
                String resource :
                resources
        ) {

            if (!hasText(
                    resource
            )) {

                continue;
            }

            result.add(
                    readJson(
                            resolve(
                                    base,
                                    resource
                            ),
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

            /*
             * Offices and courtRoles can legitimately correspond to
             * scenario title IDs rather than global social-role IDs.
             */
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

            /*
             * Military roles should be reusable shared roles rather than
             * ad-hoc scenario title strings.
             */
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
                        "birth legal status for "
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

                if (!hasText(
                        relationship.target
                )) {

                    throw new IllegalStateException(
                            "Character "
                                    + id
                                    + " has relationship without target"
                    );
                }

                if (!characterIds.contains(
                        normalizeId(
                                relationship.target
                        )
                )) {

                    throw new IllegalStateException(
                            "Character "
                                    + id
                                    + " references unknown relationship target "
                                    + relationship.target
                    );
                }
            }
        }
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
            ).isPresent()) {

                continue;
            }

            if (titleIds.contains(
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

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

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

        if (value == null
                || value.isBlank()) {

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
                index + 1
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

        return base
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

    /*
     * =========================================================
     * JSON STRUCTURES
     * =========================================================
     */

    private static final class ScenarioData {

        String settlements;

        String organizations;

        String titles;

        List<String> characters;
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

        ProfileData profile;

        LegalData legal;

        SocialData social;

        List<String> titles;

        List<RelationshipData> relationships;
    }

    private static final class ProfileData {

        String culture;

        String religion;

        java.util.Map<String, Double> languages;

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

    private static final class RelationshipData {

        String target;
    }
}
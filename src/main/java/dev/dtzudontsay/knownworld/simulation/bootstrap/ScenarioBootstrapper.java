package dev.dtzudontsay.knownworld.simulation.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.family.DynastyInheritanceRule;
import dev.dtzudontsay.knownworld.simulation.npc.knowledge.NpcKnowledgeManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
import dev.dtzudontsay.knownworld.simulation.npc.profile.DialoguePersona;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoleType;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoutine;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionLaw;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleDefinition;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleType;
import dev.dtzudontsay.knownworld.simulation.world.settlement.Settlement;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;

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
import java.util.Objects;
import java.util.Set;

public final class ScenarioBootstrapper {

    public static final String DEFAULT_SCENARIO =
            "data/knownworld/scenarios/agot_298_ac/scenario.json";

    private static final Gson GSON =
            new Gson();

    private final NpcSimulation simulation;

    private final AuthoredIdRegistry ids;

    private ScenarioBootstrapper(
            NpcSimulation simulation,
            AuthoredIdRegistry ids
    ) {

        this.simulation =
                Objects.requireNonNull(
                        simulation,
                        "simulation"
                );

        this.ids =
                Objects.requireNonNull(
                        ids,
                        "ids"
                );
    }

    public static void bootstrapDefault(
            NpcSimulation simulation,
            AuthoredIdRegistry ids
    ) throws IOException {

        new ScenarioBootstrapper(
                simulation,
                ids
        )
                .bootstrap(
                        DEFAULT_SCENARIO
                );
    }

    private void bootstrap(
            String scenarioResource
    ) throws IOException {

        ScenarioFile scenario =
                readJson(
                        scenarioResource,
                        ScenarioFile.class
                );

        requireText(
                scenario.id,
                "scenario.id"
        );

        String base =
                parentPath(
                        scenarioResource
                );

        ids.setScenarioId(
                scenario.id
        );

        PopulationPackCatalog.Plan populationPlan =
                PopulationPackCatalog.load(
                        scenarioResource,
                        scenario.populationPacks
                );

        List<SettlementData> settlements =
                hasText(
                        scenario.settlements
                )
                        ? readJsonList(
                        resolve(
                                base,
                                scenario.settlements
                        ),
                        SettlementData[].class
                )
                        : List.of();

        List<OrganizationData> organizations =
                hasText(
                        scenario.organizations
                )
                        ? readJsonList(
                        resolve(
                                base,
                                scenario.organizations
                        ),
                        OrganizationData[].class
                )
                        : List.of();

        List<TitleData> titles =
                hasText(
                        scenario.titles
                )
                        ? readJsonList(
                        resolve(
                                base,
                                scenario.titles
                        ),
                        TitleData[].class
                )
                        : List.of();

        List<String> characterResources =
                collectResources(
                        base,
                        scenario.characters,
                        populationPlan.characterResources()
                );

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

        List<CharacterData> characters =
                new ArrayList<>();

        for (
                String resource :
                characterResources
        ) {

            characters.add(
                    readJson(
                            resource,
                            CharacterData.class
                    )
            );
        }

        List<ParentageData> parentages =
                new ArrayList<>();

        for (
                String resource :
                parentageResources
        ) {

            parentages.addAll(
                    readJsonList(
                            resource,
                            ParentageData[].class
                    )
            );
        }

        List<MarriageData> marriages =
                new ArrayList<>();

        for (
                String resource :
                marriageResources
        ) {

            marriages.addAll(
                    readJsonList(
                            resource,
                            MarriageData[].class
                    )
            );
        }

        createSettlements(
                settlements
        );

        createOrganizations(
                organizations
        );

        createTitles(
                titles
        );

        /*
         * All characters from all packs are created before ANY
         * relationships/parentage/marriages are applied.
         *
         * This allows cross-pack references such as:
         *
         * Eddard Stark -> Robert Baratheon
         * without imposing a pack loading dependency.
         */
        createCharacters(
                characters
        );

        applyBasicCharacterState(
                characters
        );

        applyParentages(
                parentages
        );

        applyMarriages(
                marriages
        );

        applyKnowledgeAndRelationships(
                characters
        );

        KnownWorld.LOGGER.info(
                "Bootstrapped scenario '{}' with {} population packs, {} settlements, {} organizations, {} titles, {} characters, {} parentage records and {} marriages.",
                scenario.id,
                populationPlan.enabledPackCount(),
                settlements.size(),
                organizations.size(),
                titles.size(),
                characters.size(),
                parentages.size(),
                marriages.size()
        );
    }

    private void createSettlements(
            List<SettlementData> data
    ) {

        for (
                SettlementData entry :
                data
        ) {

            requireText(
                    entry.id,
                    "settlement.id"
            );

            requireText(
                    entry.name,
                    "settlement.name"
            );

            requireText(
                    entry.type,
                    "settlement.type"
            );

            Settlement settlement =
                    simulation.settlements()
                            .create(
                                    entry.name,
                                    enumValue(
                                            SettlementType.class,
                                            entry.type,
                                            "settlement type"
                                    ),
                                    requirePosition(
                                            entry.position,
                                            "settlement "
                                                    + entry.id
                                                    + " position"
                                    )
                            );

            ids.registerSettlement(
                    entry.id,
                    settlement.id()
            );
        }
    }

    private void createOrganizations(
            List<OrganizationData> data
    ) {

        for (
                OrganizationData entry :
                data
        ) {

            requireText(
                    entry.id,
                    "organization.id"
            );

            requireText(
                    entry.name,
                    "organization.name"
            );

            requireText(
                    entry.type,
                    "organization.type"
            );

            Organization organization =
                    simulation.organizations()
                            .create(
                                    entry.name,
                                    enumValue(
                                            OrganizationType.class,
                                            entry.type,
                                            "organization.type"
                                    ),
                                    optionalSettlement(
                                            entry.seatSettlement
                                    )
                            );

            ids.registerOrganization(
                    entry.id,
                    organization.id()
            );
        }
    }

    private void createTitles(
            List<TitleData> data
    ) {

        for (
                TitleData entry :
                data
        ) {

            requireText(
                    entry.id,
                    "title.id"
            );

            requireText(
                    entry.name,
                    "title.name"
            );

            requireText(
                    entry.type,
                    "title.type"
            );

            TitleDefinition title =
                    simulation.titles()
                            .create(
                                    entry.name,
                                    enumValue(
                                            TitleType.class,
                                            entry.type,
                                            "title type"
                                    ),
                                    entry.authority,
                                    entry.exclusive,
                                    optionalOrganization(
                                            entry.organization
                                    ),
                                    optionalSettlement(
                                            entry.settlement
                                    )
                            );

            ids.registerTitle(
                    entry.id,
                    title.id()
            );

            simulation.successionRules()
                    .setLaw(
                            title.id(),
                            hasText(
                                    entry.successionLaw
                            )
                                    ? enumValue(
                                    SuccessionLaw.class,
                                    entry.successionLaw,
                                    "succession law"
                            )
                                    : SuccessionLaw.NONE
                    );
        }
    }

    private void createCharacters(
            List<CharacterData> characters
    ) {

        Set<String> createdIds =
                new LinkedHashSet<>();

        for (
                CharacterData character :
                characters
        ) {

            requireText(
                    character.id,
                    "character.id"
            );

            String normalizedId =
                    character.id.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (!createdIds.add(
                    normalizedId
            )) {

                throw new IllegalArgumentException(
                        "Duplicate authored character "
                                + character.id
                );
            }

            if (character.identity == null) {

                throw new IllegalArgumentException(
                        "Character "
                                + character.id
                                + " has no identity"
                );
            }

            requireText(
                    character.identity.givenName,
                    character.id
                            + ".identity.givenName"
            );

            requireText(
                    character.identity.familyName,
                    character.id
                            + ".identity.familyName"
            );

            requireText(
                    character.identity.sex,
                    character.id
                            + ".identity.sex"
            );

            NpcState npc =
                    simulation.registry()
                            .create(
                                    character.identity.givenName,
                                    character.identity.familyName,
                                    enumValue(
                                            NpcSex.class,
                                            character.identity.sex,
                                            "NPC sex"
                                    ),
                                    character.identity.birthYear,
                                    resolveStartingPosition(
                                            character
                                    ),
                                    personalityOf(
                                            character.personality
                                    )
                            );

            ids.registerNpc(
                    character.id,
                    npc.id()
            );
        }
    }

    private void applyBasicCharacterState(
            List<CharacterData> characters
    ) {

        long tick =
                simulation.serverTickCounter();

        for (
                CharacterData character :
                characters
        ) {

            NpcId npc =
                    ids.requireNpc(
                            character.id
                    );

            applyProfile(
                    npc,
                    character.profile
            );

            applySocial(
                    npc,
                    character.social
            );

            applyRoutine(
                    npc,
                    character.routine
            );

            applyTitles(
                    npc,
                    character.titles,
                    tick
            );
        }
    }

    private void applyKnowledgeAndRelationships(
            List<CharacterData> characters
    ) {

        long tick =
                simulation.serverTickCounter();

        for (
                CharacterData character :
                characters
        ) {

            NpcId npc =
                    ids.requireNpc(
                            character.id
                    );

            applyRelationships(
                    npc,
                    character.relationships
            );

            applyBeliefs(
                    npc,
                    character.beliefs,
                    tick
            );

            applyMemories(
                    npc,
                    character.memories,
                    tick
            );
        }
    }

    private void applyParentages(
            List<ParentageData> parentages
    ) {

        long tick =
                simulation.serverTickCounter();

        for (
                ParentageData parentage :
                parentages
        ) {

            requireText(
                    parentage.child,
                    "parentage.child"
            );

            NpcId child =
                    ids.requireNpc(
                            parentage.child
                    );

            NpcId mother =
                    hasText(
                            parentage.mother
                    )
                            ? ids.requireNpc(
                            parentage.mother
                    )
                            : null;

            NpcId father =
                    hasText(
                            parentage.father
                    )
                            ? ids.requireNpc(
                            parentage.father
                    )
                            : null;

            simulation.genealogy()
                    .registerBirth(
                            child,
                            mother,
                            father,
                            tick
                    );
        }
    }

    private void applyMarriages(
            List<MarriageData> marriages
    ) {

        long tick =
                simulation.serverTickCounter();

        for (
                MarriageData marriage :
                marriages
        ) {

            requireText(
                    marriage.first,
                    "marriage.first"
            );

            requireText(
                    marriage.second,
                    "marriage.second"
            );

            DynastyInheritanceRule rule =
                    hasText(
                            marriage.inheritanceRule
                    )
                            ? enumValue(
                            DynastyInheritanceRule.class,
                            marriage.inheritanceRule,
                            "dynasty inheritance rule"
                    )
                            : DynastyInheritanceRule.PATRILINEAL;

            simulation.marriageService()
                    .marry(
                            ids.requireNpc(
                                    marriage.first
                            ),
                            ids.requireNpc(
                                    marriage.second
                            ),
                            rule,
                            tick
                    );
        }
    }

    private void applyProfile(
            NpcId npc,
            ProfileData data
    ) {

        CharacterProfile profile =
                simulation.profiles()
                        .getOrCreate(
                                npc
                        );

        if (data == null) {
            return;
        }

        if (hasText(
                data.culture
        )) {

            profile.setCulture(
                    data.culture
            );
        }

        if (hasText(
                data.religion
        )) {

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

        if (data.motivations != null) {

            for (
                    String motivation :
                    data.motivations
            ) {

                if (hasText(
                        motivation
                )) {

                    profile.addMotivation(
                            motivation
                    );
                }
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

    private void applySocial(
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
                            ids.requireSettlement(
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
                            ids.requireOrganization(
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
                            ids.requireOrganization(
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
                            ids.requireOrganization(
                                    social.faction
                            )
                    );
        }
    }

    private void applyRoutine(
            NpcId npc,
            RoutineData data
    ) {

        if (data == null) {
            return;
        }

        NpcRoutine routine =
                simulation.routines()
                        .getOrCreate(
                                npc
                        );

        if (hasText(
                data.role
        )) {

            routine.setRole(
                    enumValue(
                            NpcRoleType.class,
                            data.role,
                            "NPC role"
                    )
            );
        }

        if (hasText(
                data.homeSettlement
        )) {

            routine.setHomePosition(
                    settlementCenter(
                            data.homeSettlement
                    )
            );
        }

        if (data.homePosition != null) {

            routine.setHomePosition(
                    requirePosition(
                            data.homePosition,
                            "routine homePosition"
                    )
            );
        }

        if (hasText(
                data.workSettlement
        )) {

            routine.setWorkPosition(
                    settlementCenter(
                            data.workSettlement
                    )
            );
        }

        if (data.workPosition != null) {

            routine.setWorkPosition(
                    requirePosition(
                            data.workPosition,
                            "routine workPosition"
                    )
            );
        }

        if (data.workStartTick != null
                || data.workEndTick != null) {

            int start =
                    data.workStartTick == null
                            ? routine.workStartTick()
                            : data.workStartTick;

            int end =
                    data.workEndTick == null
                            ? routine.workEndTick()
                            : data.workEndTick;

            routine.setWorkSchedule(
                    start,
                    end
            );
        }
    }

    private void applyTitles(
            NpcId npc,
            List<String> titles,
            long tick
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
                            ids.requireTitle(
                                    title
                            ),
                            npc,
                            tick
                    );
        }
    }

    private void applyRelationships(
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
                    ids.requireNpc(
                            relationship.target
                    );

            if (subject.equals(
                    target
            )) {

                throw new IllegalArgumentException(
                        "Authored NPC cannot have a relationship with itself"
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

    private void applyBeliefs(
            NpcId owner,
            List<BeliefData> beliefs,
            long tick
    ) {

        if (beliefs == null) {
            return;
        }

        NpcKnowledgeManager manager =
                simulation.knowledge();

        for (
                BeliefData belief :
                beliefs
        ) {

            manager.believe(
                    owner,
                    belief.factKey,
                    belief.value,
                    belief.confidence,
                    hasText(
                            belief.sourceNpc
                    )
                            ? ids.requireNpc(
                            belief.sourceNpc
                    )
                            : null,
                    tick
            );
        }
    }

    private void applyMemories(
            NpcId owner,
            List<MemoryData> memories,
            long tick
    ) {

        if (memories == null) {
            return;
        }

        NpcMemoryManager manager =
                simulation.memories();

        for (
                MemoryData memory :
                memories
        ) {

            manager.remember(
                    owner,
                    enumValue(
                            NpcMemoryType.class,
                            memory.type,
                            "memory type"
                    ),
                    memory.summary,
                    memory.importance,
                    hasText(
                            memory.relatedNpc
                    )
                            ? ids.requireNpc(
                            memory.relatedNpc
                    )
                            : null,
                    emptyToNull(
                            memory.factKey
                    ),
                    tick
            );
        }
    }

    private SimulationPosition resolveStartingPosition(
            CharacterData character
    ) {

        if (character.position != null) {

            return requirePosition(
                    character.position,
                    character.id
                            + ".position"
            );
        }

        if (hasText(
                character.startingSettlement
        )) {

            return settlementCenter(
                    character.startingSettlement
            );
        }

        throw new IllegalArgumentException(
                "Character "
                        + character.id
                        + " requires position or startingSettlement"
        );
    }

    private SimulationPosition settlementCenter(
            String authoredSettlement
    ) {

        SettlementId id =
                ids.requireSettlement(
                        authoredSettlement
                );

        return simulation.settlements()
                .find(
                        id
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Mapped settlement no longer exists: "
                                                + authoredSettlement
                                )
                )
                .center();
    }

    private OrganizationId optionalOrganization(
            String authoredId
    ) {

        return hasText(
                authoredId
        )
                ? ids.requireOrganization(
                authoredId
        )
                : null;
    }

    private SettlementId optionalSettlement(
            String authoredId
    ) {

        return hasText(
                authoredId
        )
                ? ids.requireSettlement(
                authoredId
        )
                : null;
    }

    private static NpcPersonality personalityOf(
            PersonalityData data
    ) {

        if (data == null) {

            return NpcPersonality.NEUTRAL;
        }

        return new NpcPersonality(
                valueOrZero(
                        data.courage
                ),
                valueOrZero(
                        data.ambition
                ),
                valueOrZero(
                        data.compassion
                ),
                valueOrZero(
                        data.honor
                ),
                valueOrZero(
                        data.patience
                ),
                valueOrZero(
                        data.sociability
                )
        );
    }

    private static List<String> collectResources(
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

                    throw new IllegalArgumentException(
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

                throw new IllegalArgumentException(
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

                throw new IllegalArgumentException(
                        "Duplicate scenario population resource "
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

    private static double valueOrZero(
            Double value
    ) {

        return value == null
                ? 0.0
                : value;
    }

    private static SimulationPosition requirePosition(
            PositionData data,
            String description
    ) {

        if (data == null) {

            throw new IllegalArgumentException(
                    description
                            + " is missing"
            );
        }

        return new SimulationPosition(
                hasText(
                        data.dimension
                )
                        ? data.dimension
                        : SimulationPosition.OVERWORLD,
                data.x,
                data.y,
                data.z
        );
    }

    private static <E extends Enum<E>> E enumValue(
            Class<E> type,
            String value,
            String description
    ) {

        requireText(
                value,
                description
        );

        try {

            return Enum.valueOf(
                    type,
                    value.trim()
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
                            + value,
                    exception
            );
        }
    }

    private static <T> T readJson(
            String resource,
            Class<T> type
    ) throws IOException {

        try (
                InputStream input =
                        ScenarioBootstrapper.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (input == null) {

                throw new IOException(
                        "Scenario resource not found: "
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
                            "Scenario JSON produced null: "
                                    + resource
                    );
                }

                return result;
            }

        } catch (
                JsonParseException exception
        ) {

            throw new IOException(
                    "Invalid JSON in scenario resource: "
                            + resource,
                    exception
            );
        }
    }

    private static <T> List<T> readJsonList(
            String resource,
            Class<T[]> type
    ) throws IOException {

        return List.of(
                readJson(
                        resource,
                        type
                )
        );
    }

    private static String resolve(
            String base,
            String child
    ) {

        requireText(
                child,
                "scenario resource path"
        );

        if (child.startsWith(
                "data/"
        )) {

            return child;
        }

        return base
                + "/"
                + child;
    }

    private static String parentPath(
            String resource
    ) {

        int separator =
                resource.lastIndexOf(
                        '/'
                );

        if (separator < 0) {

            return "";
        }

        return resource.substring(
                0,
                separator
        );
    }

    private static boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private static String emptyToNull(
            String value
    ) {

        return hasText(
                value
        )
                ? value.trim()
                : null;
    }

    private static void requireText(
            String value,
            String description
    ) {

        if (!hasText(
                value
        )) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be empty"
            );
        }
    }

    private static final class ScenarioFile {

        String id;

        String settlements;

        String organizations;

        String titles;

        String populationPacks;

        /*
         * Legacy migration support.
         *
         * New scenario data should use populationPacks.
         */
        List<String> characters;

        String parentages;

        String marriages;
    }

    private static final class SettlementData {

        String id;

        String name;

        String type;

        PositionData position;
    }

    private static final class OrganizationData {

        String id;

        String name;

        String type;

        String seatSettlement;
    }

    private static final class TitleData {

        String id;

        String name;

        String type;

        int authority;

        boolean exclusive;

        String organization;

        String settlement;

        String successionLaw;
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

    private static final class CharacterData {

        String id;

        IdentityData identity;

        String startingSettlement;

        PositionData position;

        PersonalityData personality;

        ProfileData profile;

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

    private static final class PersonalityData {

        Double courage;

        Double ambition;

        Double compassion;

        Double honor;

        Double patience;

        Double sociability;
    }

    private static final class ProfileData {

        String culture;

        String religion;

        String education;

        Map<String, Double> skills;

        List<String> traits;

        Map<String, Double> values;

        List<String> motivations;

        DialogueData dialogue;
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

    private static final class RoutineData {

        String role;

        String homeSettlement;

        PositionData homePosition;

        String workSettlement;

        PositionData workPosition;

        Integer workStartTick;

        Integer workEndTick;
    }

    private static final class RelationshipData {

        String target;

        double affection;

        double trust;

        double respect;

        double fear;

        double familiarity;
    }

    private static final class BeliefData {

        String factKey;

        String value;

        double confidence;

        String sourceNpc;
    }

    private static final class MemoryData {

        String type;

        String summary;

        double importance;

        String relatedNpc;

        String factKey;
    }

    private static final class PositionData {

        String dimension;

        double x;

        double y;

        double z;
    }
}
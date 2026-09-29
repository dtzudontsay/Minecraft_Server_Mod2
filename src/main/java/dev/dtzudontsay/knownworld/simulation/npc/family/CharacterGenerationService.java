package dev.dtzudontsay.knownworld.simulation.npc.family;

import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;

import java.util.Objects;
import java.util.Random;

/**
 * Runtime generation of new persistent people.
 *
 * Generated characters do NOT require authored JSON files.
 *
 * Once created they are ordinary NPCs and are persisted by the same
 * simulation save system as authored characters.
 */
public final class CharacterGenerationService {

    private static final double PERSONALITY_VARIATION =
            0.15;

    private final NpcRegistry registry;

    private final GenealogyManager genealogy;

    private final NpcAffiliationManager affiliations;

    private final NpcRelationshipManager relationships;

    private final NpcMemoryManager memories;

    private final WorldEventManager events;

    public CharacterGenerationService(
            NpcRegistry registry,
            GenealogyManager genealogy,
            NpcAffiliationManager affiliations,
            NpcRelationshipManager relationships,
            NpcMemoryManager memories,
            WorldEventManager events
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.genealogy =
                Objects.requireNonNull(
                        genealogy,
                        "genealogy"
                );

        this.affiliations =
                Objects.requireNonNull(
                        affiliations,
                        "affiliations"
                );

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
                );

        this.memories =
                Objects.requireNonNull(
                        memories,
                        "memories"
                );

        this.events =
                Objects.requireNonNull(
                        events,
                        "events"
                );
    }

    public NpcState createChild(
            String givenName,
            NpcSex sex,
            int birthYear,
            NpcId mother,
            NpcId father,
            long tick
    ) {
        NpcState motherState =
                requireNpc(
                        mother
                );

        NpcState fatherState =
                father == null
                        ? null
                        : requireNpc(
                        father
                );

        NpcPersonality personality =
                inheritPersonality(
                        motherState,
                        fatherState,
                        tick
                );

        String familyName =
                determineFamilyName(
                        motherState,
                        fatherState
                );

        NpcState child =
                registry.create(
                        givenName,
                        familyName,
                        sex,
                        birthYear,
                        motherState.position(),
                        personality
                );

        genealogy.registerBirth(
                child.id(),
                mother,
                father,
                tick
        );

        inheritAffiliations(
                child.id(),
                motherState,
                fatherState
        );

        initializeFamilyRelationships(
                child.id(),
                mother,
                father
        );

        createBirthMemories(
                child,
                mother,
                father,
                tick
        );

        events.create(
                WorldEventType.BIRTH,
                child.identity()
                        .fullName()
                        + " was born.",
                child.position(),
                tick,
                0.75,
                mother,
                child.id(),
                null,
                null
        );

        return child;
    }

    public NpcState createChildAutoSex(
            String givenName,
            int birthYear,
            NpcId mother,
            NpcId father,
            long tick
    ) {
        Random random =
                randomFor(
                        mother,
                        father,
                        tick,
                        0x56B17L
                );

        NpcSex sex =
                random.nextBoolean()
                        ? NpcSex.MALE
                        : NpcSex.FEMALE;

        return createChild(
                givenName,
                sex,
                birthYear,
                mother,
                father,
                tick
        );
    }

    private NpcPersonality inheritPersonality(
            NpcState mother,
            NpcState father,
            long tick
    ) {
        Random random =
                randomFor(
                        mother.id(),
                        father == null
                                ? null
                                : father.id(),
                        tick,
                        0x91A7C3L
                );

        NpcPersonality motherPersonality =
                mother.personality();

        NpcPersonality fatherPersonality =
                father == null
                        ? motherPersonality
                        : father.personality();

        return new NpcPersonality(
                inheritTrait(
                        motherPersonality.courage(),
                        fatherPersonality.courage(),
                        random
                ),
                inheritTrait(
                        motherPersonality.ambition(),
                        fatherPersonality.ambition(),
                        random
                ),
                inheritTrait(
                        motherPersonality.compassion(),
                        fatherPersonality.compassion(),
                        random
                ),
                inheritTrait(
                        motherPersonality.honor(),
                        fatherPersonality.honor(),
                        random
                ),
                inheritTrait(
                        motherPersonality.patience(),
                        fatherPersonality.patience(),
                        random
                ),
                inheritTrait(
                        motherPersonality.sociability(),
                        fatherPersonality.sociability(),
                        random
                )
        );
    }

    private static double inheritTrait(
            double mother,
            double father,
            Random random
    ) {
        double parentalMean =
                (
                        mother
                                + father
                )
                        / 2.0;

        double variation =
                (
                        random.nextDouble()
                                * 2.0
                                - 1.0
                )
                        * PERSONALITY_VARIATION;

        return clampSigned(
                parentalMean
                        + variation
        );
    }

    private String determineFamilyName(
            NpcState mother,
            NpcState father
    ) {
        if (father != null
                && !father.identity()
                .familyName()
                .isBlank()) {

            return father.identity()
                    .familyName();
        }

        return mother.identity()
                .familyName();
    }

    private void inheritAffiliations(
            NpcId child,
            NpcState mother,
            NpcState father
    ) {
        NpcAffiliation motherAffiliation =
                affiliations.getOrCreate(
                        mother.id()
                );

        NpcAffiliation childAffiliation =
                affiliations.getOrCreate(
                        child
                );

        childAffiliation.setResidenceSettlement(
                motherAffiliation.residenceSettlement()
        );

        childAffiliation.setHousehold(
                motherAffiliation.household()
        );

        childAffiliation.setFaction(
                motherAffiliation.faction()
        );

        OrganizationId nobleHouse =
                null;

        if (father != null) {

            nobleHouse =
                    affiliations.getOrCreate(
                                    father.id()
                            )
                            .nobleHouse();
        }

        if (nobleHouse == null) {

            nobleHouse =
                    motherAffiliation.nobleHouse();
        }

        childAffiliation.setNobleHouse(
                nobleHouse
        );
    }

    private void initializeFamilyRelationships(
            NpcId child,
            NpcId mother,
            NpcId father
    ) {
        createParentRelationship(
                mother,
                child
        );

        createChildRelationship(
                child,
                mother
        );

        if (father != null) {

            createParentRelationship(
                    father,
                    child
            );

            createChildRelationship(
                    child,
                    father
            );
        }
    }

    private void createParentRelationship(
            NpcId parent,
            NpcId child
    ) {
        relationships.registerLoaded(
                new NpcRelationship(
                        parent,
                        child,
                        0.65,
                        0.40,
                        0.20,
                        0.00,
                        1.00
                )
        );
    }

    private void createChildRelationship(
            NpcId child,
            NpcId parent
    ) {
        relationships.registerLoaded(
                new NpcRelationship(
                        child,
                        parent,
                        0.50,
                        0.50,
                        0.20,
                        0.00,
                        1.00
                )
        );
    }

    private void createBirthMemories(
            NpcState child,
            NpcId mother,
            NpcId father,
            long tick
    ) {
        memories.remember(
                mother,
                NpcMemoryType.PERSONAL_EXPERIENCE,
                "Gave birth to "
                        + child.identity()
                        .fullName()
                        + ".",
                0.95,
                child.id(),
                null,
                tick
        );

        if (father != null) {

            memories.remember(
                    father,
                    NpcMemoryType.PERSONAL_EXPERIENCE,
                    child.identity()
                            .fullName()
                            + " was born.",
                    0.90,
                    child.id(),
                    null,
                    tick
            );
        }
    }

    private NpcState requireNpc(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        return registry.find(
                        id
                )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Unknown NPC ID: "
                                                + id
                                )
                );
    }

    private static Random randomFor(
            NpcId mother,
            NpcId father,
            long tick,
            long salt
    ) {
        long seed =
                salt;

        seed =
                seed * 31L
                        + mother.value();

        seed =
                seed * 31L
                        + (
                        father == null
                                ? 0L
                                : father.value()
                );

        seed =
                seed * 31L
                        + tick;

        return new Random(
                seed
        );
    }

    private static double clampSigned(
            double value
    ) {
        return Math.max(
                -1.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
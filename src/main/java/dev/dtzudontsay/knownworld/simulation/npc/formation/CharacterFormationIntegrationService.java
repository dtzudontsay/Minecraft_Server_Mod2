package dev.dtzudontsay.knownworld.simulation.npc.formation;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.culture.CultureService;
import dev.dtzudontsay.knownworld.simulation.npc.family.GenealogyManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.Parentage;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.LifeHistoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligionService;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolution;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolver;

import java.util.Objects;

public final class CharacterFormationIntegrationService {

    private static final int MAX_FORMATION_AGE =
            15;

    private static final String CULTURE_IDENTITY_IMPORTANCE =
            "culture.identity_importance";

    private static final String RELIGION_DEVOTION =
            "religion.devotion";

    private final NpcRegistry registry;

    private final CharacterProfileManager profiles;

    private final GenealogyManager genealogy;

    private final LifeHistoryManager lifeHistory;

    private final CampaignCalendar calendar;

    private final UpbringingManager upbringing;

    private final CultureService culture;

    private final ReligionService religion;

    public CharacterFormationIntegrationService(
            NpcRegistry registry,
            CharacterProfileManager profiles,
            GenealogyManager genealogy,
            LifeHistoryManager lifeHistory,
            CampaignCalendar calendar,
            UpbringingManager upbringing,
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

    /**
     * Reconciles all current NPCs with the post-18F formation model.
     *
     * This method is deliberately idempotent and may safely run before
     * every save. Existing authored state is preserved.
     */
    public void ensureAll() {

        /*
         * This also gives newly debug-created NPCs a life record.
         */
        lifeHistory.ensureAll(
                calendar.daysPerYear()
        );

        profiles.ensureAll();

        for (
                NpcState npc :
                registry.all()
        ) {

            ensureCharacter(
                    npc.id()
            );
        }
    }

    public void ensureCharacter(
            NpcId npcId
    ) {

        NpcState npc =
                registry.find(
                                Objects.requireNonNull(
                                        npcId,
                                        "npcId"
                                )
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown NPC "
                                                        + npcId
                                        )
                        );

        CharacterProfile profile =
                profiles.getOrCreate(
                        npcId
                );

        int age =
                lifeHistory.ageYears(
                        npcId,
                        calendar.absoluteDay(),
                        calendar.daysPerYear()
                );

        ensureCultureBaseline(
                npcId,
                profile,
                age
        );

        ensureReligionBaseline(
                npcId,
                profile,
                age
        );

        if (!npc.isAlive()
                || age > MAX_FORMATION_AGE) {

            return;
        }

        ensureChildFormationRecord(
                npc,
                profile,
                age
        );
    }

    private void ensureCultureBaseline(
            NpcId npc,
            CharacterProfile profile,
            int age
    ) {

        if ("unknown".equals(
                profile.culture()
        )) {

            return;
        }

        if (profile.values()
                .containsKey(
                        CULTURE_IDENTITY_IMPORTANCE
                )) {

            return;
        }

        if (age <= 5) {

            culture.initializeCultureState(
                    npc,
                    profile.culture(),
                    0.15,
                    0.20,
                    0.65,
                    0.05
            );

            return;
        }

        if (age <= 11) {

            culture.initializeCultureState(
                    npc,
                    profile.culture(),
                    0.30,
                    0.35,
                    0.55,
                    0.08
            );

            return;
        }

        if (age <= MAX_FORMATION_AGE) {

            culture.initializeCultureState(
                    npc,
                    profile.culture(),
                    0.45,
                    0.50,
                    0.45,
                    0.10
            );

            return;
        }

        /*
         * Generic compatibility baseline for authored adults that
         * pre-date 18E.
         *
         * Gold-standard authored characters may override every one
         * of these values later.
         */
        culture.initializeCultureState(
                npc,
                profile.culture(),
                0.55,
                0.55,
                0.35,
                0.10
        );
    }

    private void ensureReligionBaseline(
            NpcId npc,
            CharacterProfile profile,
            int age
    ) {

        if ("unknown".equals(
                profile.religion()
        )) {

            return;
        }

        if (profile.values()
                .containsKey(
                        RELIGION_DEVOTION
                )) {

            return;
        }

        if (age <= 5) {

            religion.initializeFaithState(
                    npc,
                    profile.religion(),
                    0.08,
                    0.02,
                    0.12,
                    0.50,
                    0.70,
                    0.05,
                    0.10,
                    0.08
            );

            return;
        }

        if (age <= 11) {

            religion.initializeFaithState(
                    npc,
                    profile.religion(),
                    0.20,
                    0.12,
                    0.25,
                    0.50,
                    0.55,
                    0.10,
                    0.10,
                    0.20
            );

            return;
        }

        if (age <= MAX_FORMATION_AGE) {

            religion.initializeFaithState(
                    npc,
                    profile.religion(),
                    0.35,
                    0.25,
                    0.40,
                    0.50,
                    0.45,
                    0.15,
                    0.10,
                    0.35
            );

            return;
        }

        /*
         * Generic compatibility baseline for adults created before
         * personal faith state existed.
         */
        religion.initializeFaithState(
                npc,
                profile.religion(),
                0.50,
                0.35,
                0.50,
                0.50,
                0.35,
                0.25,
                0.10,
                0.45
        );
    }

    private void ensureChildFormationRecord(
            NpcState child,
            CharacterProfile profile,
            int age
    ) {

        String knownBirthplace =
                normalizeKnownLocation(
                        profile.birthplaceLocationId()
                );

        String knownUpbringing =
                normalizeKnownLocation(
                        profile.upbringingLocationId()
                );

        String currentLocation =
                resolveCurrentLocation(
                        child
                );

        /*
         * Current position can reasonably establish current
         * upbringing location for a migrated child.
         *
         * It must NOT be silently treated as the child's birthplace.
         */
        String upbringingLocation =
                knownUpbringing == null
                        ? currentLocation
                        : knownUpbringing;

        NpcId defaultGuardian =
                defaultGuardian(
                        child.id()
                );

        UpbringingRecord record =
                upbringing.ensureMigrated(
                        child.id(),
                        knownBirthplace == null
                                ? "unknown"
                                : knownBirthplace,
                        upbringingLocation,
                        defaultGuardian,
                        age
                );

        /*
         * Preserve UNKNOWN birthplace when source data does not know it.
         */
        if (knownBirthplace != null) {

            profile.setBirthplaceLocationId(
                    knownBirthplace
            );
        }

        if (!"unknown".equals(
                record.upbringingLocationId()
        )) {

            profile.setUpbringingLocationId(
                    record.upbringingLocationId()
            );
        }
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

    private static String normalizeKnownLocation(
            String locationId
    ) {

        if (locationId == null
                || locationId.isBlank()
                || "unknown".equals(
                locationId
        )) {

            return null;
        }

        return locationId;
    }

    private static String resolveCurrentLocation(
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
}
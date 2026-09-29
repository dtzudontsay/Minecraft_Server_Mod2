package dev.dtzudontsay.knownworld.simulation.npc.lifecycle;

import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.family.CharacterGenerationService;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageManager;
import dev.dtzudontsay.knownworld.simulation.npc.family.MarriageRecord;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionService;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class LifeCycleService {

    private final NpcRegistry registry;

    private final CampaignCalendar calendar;

    private final LifeHistoryManager lifeHistory;

    private final FertilityService fertility;

    private final PregnancyManager pregnancies;

    private final MarriageManager marriages;

    private final CharacterGenerationService characterGeneration;

    private final GeneratedNameService names;

    private final NpcMemoryManager memories;

    private final WorldEventManager events;

    private final SuccessionService succession;

    public LifeCycleService(
            NpcRegistry registry,
            CampaignCalendar calendar,
            LifeHistoryManager lifeHistory,
            FertilityService fertility,
            PregnancyManager pregnancies,
            MarriageManager marriages,
            CharacterGenerationService characterGeneration,
            GeneratedNameService names,
            NpcMemoryManager memories,
            WorldEventManager events,
            SuccessionService succession
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.calendar =
                Objects.requireNonNull(
                        calendar,
                        "calendar"
                );

        this.lifeHistory =
                Objects.requireNonNull(
                        lifeHistory,
                        "lifeHistory"
                );

        this.fertility =
                Objects.requireNonNull(
                        fertility,
                        "fertility"
                );

        this.pregnancies =
                Objects.requireNonNull(
                        pregnancies,
                        "pregnancies"
                );

        this.marriages =
                Objects.requireNonNull(
                        marriages,
                        "marriages"
                );

        this.characterGeneration =
                Objects.requireNonNull(
                        characterGeneration,
                        "characterGeneration"
                );

        this.names =
                Objects.requireNonNull(
                        names,
                        "names"
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

        this.succession =
                Objects.requireNonNull(
                        succession,
                        "succession"
                );
    }

    public void onNewCampaignDay(
            long simulationTick
    ) {
        lifeHistory.ensureAll(
                calendar.daysPerYear()
        );

        processPregnancies(
                simulationTick
        );

        processNaturalDeaths(
                simulationTick
        );

        processConceptions(
                simulationTick
        );
    }

    public Pregnancy forceConception(
            NpcId mother,
            NpcId father,
            long simulationTick
    ) {
        validateParents(
                mother,
                father
        );

        Pregnancy pregnancy =
                pregnancies.conceive(
                        mother,
                        father,
                        calendar.absoluteDay()
                );

        memories.remember(
                mother,
                NpcMemoryType.PERSONAL_EXPERIENCE,
                "Became pregnant.",
                0.80,
                father,
                null,
                simulationTick
        );

        return pregnancy;
    }

    private void processConceptions(
            long simulationTick
    ) {
        for (
                MarriageRecord marriage :
                marriages.all()
        ) {

            if (!marriage.isMarried()) {
                continue;
            }

            NpcState first =
                    registry.find(
                                    marriage.first()
                            )
                            .orElse(
                                    null
                            );

            NpcState second =
                    registry.find(
                                    marriage.second()
                            )
                            .orElse(
                                    null
                            );

            if (first == null
                    || second == null
                    || !first.isAlive()
                    || !second.isAlive()) {

                continue;
            }

            NpcState mother;
            NpcState father;

            if (first.identity()
                    .sex()
                    == NpcSex.FEMALE
                    && second.identity()
                    .sex()
                    == NpcSex.MALE) {

                mother =
                        first;

                father =
                        second;

            } else if (
                    second.identity()
                            .sex()
                            == NpcSex.FEMALE
                            && first.identity()
                            .sex()
                            == NpcSex.MALE
            ) {

                mother =
                        second;

                father =
                        first;

            } else {

                continue;
            }

            if (pregnancies.isPregnant(
                    mother.id()
            )) {

                continue;
            }

            double chance =
                    fertility.conceptionChancePerDay(
                            mother.id(),
                            father.id()
                    );

            if (chance <= 0.0) {
                continue;
            }

            Random random =
                    randomFor(
                            calendar.absoluteDay(),
                            mother.id(),
                            father.id(),
                            0xC0ACE1L
                    );

            if (random.nextDouble()
                    >= chance) {

                continue;
            }

            pregnancies.conceive(
                    mother.id(),
                    father.id(),
                    calendar.absoluteDay()
            );

            memories.remember(
                    mother.id(),
                    NpcMemoryType.PERSONAL_EXPERIENCE,
                    "Became pregnant.",
                    0.80,
                    father.id(),
                    null,
                    simulationTick
            );
        }
    }

    private void processPregnancies(
            long simulationTick
    ) {
        List<Pregnancy> due =
                new ArrayList<>();

        for (
                Pregnancy pregnancy :
                pregnancies.all()
        ) {

            if (calendar.absoluteDay()
                    >= pregnancy.dueDay()) {

                due.add(
                        pregnancy
                );
            }
        }

        for (Pregnancy pregnancy : due) {

            NpcState mother =
                    registry.find(
                                    pregnancy.mother()
                            )
                            .orElse(
                                    null
                            );

            NpcState father =
                    registry.find(
                                    pregnancy.father()
                            )
                            .orElse(
                                    null
                            );

            if (mother == null
                    || father == null
                    || !mother.isAlive()) {

                pregnancies.complete(
                        pregnancy.mother()
                );

                continue;
            }

            Random random =
                    randomFor(
                            calendar.absoluteDay(),
                            pregnancy.mother(),
                            pregnancy.father(),
                            0xB17A5L
                    );

            NpcSex sex =
                    random.nextBoolean()
                            ? NpcSex.MALE
                            : NpcSex.FEMALE;

            String givenName =
                    names.generate(
                            sex,
                            pregnancy.mother(),
                            pregnancy.father(),
                            calendar.absoluteDay(),
                            registry.size()
                    );

            NpcState child =
                    characterGeneration.createChild(
                            givenName,
                            sex,
                            calendar.year(),
                            pregnancy.mother(),
                            pregnancy.father(),
                            simulationTick
                    );

            lifeHistory.registerBirth(
                    child.id(),
                    calendar.absoluteDay()
            );

            pregnancies.complete(
                    pregnancy.mother()
            );
        }
    }

    private void processNaturalDeaths(
            long simulationTick
    ) {
        List<NpcState> deaths =
                new ArrayList<>();

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

            double chance =
                    naturalDeathChancePerDay(
                            age
                    );

            if (chance <= 0.0) {
                continue;
            }

            Random random =
                    randomFor(
                            calendar.absoluteDay(),
                            npc.id(),
                            null,
                            0xDEA7L
                    );

            if (random.nextDouble()
                    < chance) {

                deaths.add(
                        npc
                );
            }
        }

        for (NpcState npc : deaths) {

            naturalDeath(
                    npc,
                    simulationTick
            );
        }
    }

    private void naturalDeath(
            NpcState npc,
            long simulationTick
    ) {
        NpcId spouse =
                marriages.currentSpouseOf(
                                npc.id()
                        )
                        .orElse(
                                null
                        );

        marriages.endCurrentUnion(
                npc.id(),
                simulationTick
        );

        npc.markDead();

        lifeHistory.markDead(
                npc.id(),
                calendar.absoluteDay()
        );

        succession.handleDeath(
                npc.id(),
                simulationTick
        );

        if (spouse != null) {

            NpcState spouseState =
                    registry.find(
                                    spouse
                            )
                            .orElse(
                                    null
                            );

            if (spouseState != null
                    && spouseState.isAlive()) {

                memories.remember(
                        spouse,
                        NpcMemoryType.DEATH,
                        npc.identity()
                                .fullName()
                                + ", my spouse, died.",
                        1.0,
                        npc.id(),
                        null,
                        simulationTick
                );
            }
        }

        events.create(
                WorldEventType.DEATH,
                npc.identity()
                        .fullName()
                        + " died of natural causes.",
                npc.position(),
                simulationTick,
                0.70,
                npc.id(),
                npc.id(),
                "person.death",
                "natural_causes"
        );
    }

    private void validateParents(
            NpcId mother,
            NpcId father
    ) {
        NpcState motherState =
                requireAlive(
                        mother
                );

        NpcState fatherState =
                requireAlive(
                        father
                );

        if (motherState.identity()
                .sex()
                != NpcSex.FEMALE) {

            throw new IllegalArgumentException(
                    "Mother NPC must be FEMALE"
            );
        }

        if (fatherState.identity()
                .sex()
                != NpcSex.MALE) {

            throw new IllegalArgumentException(
                    "Father NPC must be MALE"
            );
        }

        if (fertility.conceptionChancePerDay(
                mother,
                father
        ) <= 0.0) {

            throw new IllegalArgumentException(
                    "These NPCs are not currently fertile"
            );
        }
    }

    private NpcState requireAlive(
            NpcId npc
    ) {
        NpcState state =
                registry.find(
                                npc
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown NPC ID: "
                                                        + npc
                                        )
                        );

        if (!state.isAlive()) {

            throw new IllegalArgumentException(
                    "NPC #"
                            + npc
                            + " is dead"
            );
        }

        return state;
    }

    private static double naturalDeathChancePerDay(
            int age
    ) {
        if (age < 50) {
            return 0.00002;
        }

        if (age < 60) {
            return 0.00010;
        }

        if (age < 70) {
            return 0.00030;
        }

        if (age < 80) {
            return 0.00080;
        }

        if (age < 90) {
            return 0.00200;
        }

        return 0.00500;
    }

    private static Random randomFor(
            long day,
            NpcId first,
            NpcId second,
            long salt
    ) {
        long seed =
                day;

        seed =
                seed * 31L
                        + first.value();

        seed =
                seed * 31L
                        + (
                        second == null
                                ? 0L
                                : second.value()
                );

        seed =
                seed * 31L
                        + salt;

        return new Random(
                seed
        );
    }
}
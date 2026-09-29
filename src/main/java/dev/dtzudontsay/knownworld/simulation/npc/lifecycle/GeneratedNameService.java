package dev.dtzudontsay.knownworld.simulation.npc.lifecycle;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;

import java.util.List;
import java.util.Random;

public final class GeneratedNameService {

    private static final List<String> MALE_NAMES =
            List.of(
                    "Edric",
                    "Jonos",
                    "Rickard",
                    "Brandon",
                    "Torrhen",
                    "Rodrik",
                    "Benjen",
                    "Martyn",
                    "Harwin",
                    "Garlan",
                    "Lymond",
                    "Raymun",
                    "Olyvar",
                    "Donnel",
                    "Willam",
                    "Alyn",
                    "Theomar",
                    "Jory",
                    "Lucas",
                    "Robin"
            );

    private static final List<String> FEMALE_NAMES =
            List.of(
                    "Alys",
                    "Jeyne",
                    "Lyanna",
                    "Marya",
                    "Bethany",
                    "Wylla",
                    "Alyssa",
                    "Lyarra",
                    "Dacey",
                    "Myrielle",
                    "Elinor",
                    "Serra",
                    "Mya",
                    "Alysanne",
                    "Rhaena",
                    "Joy",
                    "Cassana",
                    "Lynesse",
                    "Roslin",
                    "Melesa"
            );

    public String generate(
            NpcSex sex,
            NpcId mother,
            NpcId father,
            long campaignDay,
            long salt
    ) {
        long seed =
                campaignDay;

        seed =
                seed * 31L
                        + mother.value();

        seed =
                seed * 31L
                        + father.value();

        seed =
                seed * 31L
                        + salt;

        Random random =
                new Random(
                        seed
                );

        List<String> source =
                sex == NpcSex.MALE
                        ? MALE_NAMES
                        : FEMALE_NAMES;

        return source.get(
                random.nextInt(
                        source.size()
                )
        );
    }
}
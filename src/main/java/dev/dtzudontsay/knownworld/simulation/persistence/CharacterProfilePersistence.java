package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterHealthState;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterLegalStatus;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterOrientation;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.profile.DialoguePersona;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CharacterProfilePersistence {

    private static final String HEADER_V1 =
            "KNOWNWORLD_CHARACTER_PROFILES\t1";

    private static final String HEADER_V2 =
            "KNOWNWORLD_CHARACTER_PROFILES\t2";

    private static final String FILE_NAME =
            "character_profiles.tsv";

    private static final String TEMP_FILE_NAME =
            "character_profiles.tsv.tmp";

    private final Path directory;

    private final Path file;

    public CharacterProfilePersistence(
            Path directory
    ) {
        this.directory =
                directory;

        this.file =
                directory.resolve(
                        FILE_NAME
                );
    }

    public void save(
            CharacterProfileManager manager
    ) throws IOException {

        Files.createDirectories(
                directory
        );

        Path temporary =
                directory.resolve(
                        TEMP_FILE_NAME
                );

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                temporary,
                                StandardCharsets.UTF_8
                        )
        ) {

            writer.write(
                    HEADER_V2
            );

            writer.newLine();

            for (
                    CharacterProfile profile :
                    manager.all()
            ) {

                writeProfile(
                        writer,
                        profile
                );
            }
        }

        try {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } catch (
                IOException exception
        ) {

            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    public void loadInto(
            CharacterProfileManager manager
    ) throws IOException {

        if (!Files.exists(
                file
        )) {

            return;
        }

        Map<NpcId, CharacterProfile> loaded =
                new LinkedHashMap<>();

        boolean legacyV1;

        try (
                BufferedReader reader =
                        Files.newBufferedReader(
                                file,
                                StandardCharsets.UTF_8
                        )
        ) {

            String header =
                    reader.readLine();

            if (HEADER_V1.equals(
                    header
            )) {

                legacyV1 =
                        true;

            } else if (
                    HEADER_V2.equals(
                            header
                    )
            ) {

                legacyV1 =
                        false;

            } else {

                throw new IOException(
                        "Unsupported character profile save format"
                );
            }

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                if (line.isBlank()) {
                    continue;
                }

                decodeLine(
                        loaded,
                        line,
                        legacyV1
                );
            }
        }

        for (
                CharacterProfile profile :
                loaded.values()
        ) {

            manager.registerLoaded(
                    profile
            );
        }
    }

    private static void writeProfile(
            BufferedWriter writer,
            CharacterProfile profile
    ) throws IOException {

        DialoguePersona dialogue =
                profile.dialoguePersona();

        write(
                writer,
                "PROFILE",
                Long.toString(
                        profile.owner()
                                .value()
                ),
                escape(
                        profile.culture()
                ),
                escape(
                        profile.religion()
                ),
                escape(
                        profile.education()
                ),
                escape(
                        profile.birthplaceLocationId()
                ),
                escape(
                        profile.upbringingLocationId()
                ),
                profile.orientation()
                        .name(),
                profile.healthState()
                        .name(),
                profile.legalStatus()
                        .name(),
                Double.toString(
                        profile.wealth()
                ),
                Double.toString(
                        profile.socialStatus()
                ),
                Double.toString(
                        profile.reputation()
                ),
                Double.toString(
                        dialogue.formality()
                ),
                Double.toString(
                        dialogue.verbosity()
                ),
                Double.toString(
                        dialogue.warmth()
                ),
                Double.toString(
                        dialogue.directness()
                ),
                escape(
                        dialogue.preferredAddress()
                ),
                escape(
                        dialogue.guidance()
                ),
                escape(
                        profile.appearanceDescription()
                ),
                escape(
                        profile.hairDescription()
                ),
                escape(
                        profile.eyeDescription()
                ),
                escape(
                        profile.buildDescription()
                )
        );

        for (
                var entry :
                profile.skills()
                        .entrySet()
        ) {

            write(
                    writer,
                    "SKILL",
                    owner(
                            profile
                    ),
                    entry.getKey()
                            .name(),
                    Double.toString(
                            entry.getValue()
                    )
            );
        }

        for (
                var entry :
                profile.aptitudes()
                        .entrySet()
        ) {

            write(
                    writer,
                    "APTITUDE",
                    owner(
                            profile
                    ),
                    entry.getKey()
                            .name(),
                    Double.toString(
                            entry.getValue()
                    )
            );
        }

        for (
                var entry :
                profile.dispositions()
                        .entrySet()
        ) {

            write(
                    writer,
                    "DISPOSITION",
                    owner(
                            profile
                    ),
                    entry.getKey()
                            .name(),
                    Double.toString(
                            entry.getValue()
                    )
            );
        }

        for (
                var entry :
                profile.characterValues()
                        .entrySet()
        ) {

            write(
                    writer,
                    "CHARACTER_VALUE",
                    owner(
                            profile
                    ),
                    entry.getKey()
                            .name(),
                    Double.toString(
                            entry.getValue()
                    )
            );
        }

        for (
                var entry :
                profile.socialNorms()
                        .entrySet()
        ) {

            write(
                    writer,
                    "SOCIAL_NORM",
                    owner(
                            profile
                    ),
                    entry.getKey()
                            .name(),
                    Double.toString(
                            entry.getValue()
                    )
            );
        }

        for (
                var entry :
                profile.values()
                        .entrySet()
        ) {

            write(
                    writer,
                    "VALUE",
                    owner(
                            profile
                    ),
                    escape(
                            entry.getKey()
                    ),
                    Double.toString(
                            entry.getValue()
                    )
            );
        }

        for (
                var entry :
                profile.politicalPreferences()
                        .entrySet()
        ) {

            write(
                    writer,
                    "POLITICAL",
                    owner(
                            profile
                    ),
                    escape(
                            entry.getKey()
                    ),
                    Double.toString(
                            entry.getValue()
                    )
            );
        }

        for (
                String trait :
                profile.traits()
        ) {

            write(
                    writer,
                    "TRAIT",
                    owner(
                            profile
                    ),
                    escape(
                            trait
                    )
            );
        }

        for (
                String alias :
                profile.aliases()
        ) {

            write(
                    writer,
                    "ALIAS",
                    owner(
                            profile
                    ),
                    escape(
                            alias
                    )
            );
        }

        for (
                String value :
                profile.occupations()
        ) {

            write(
                    writer,
                    "OCCUPATION",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.offices()
        ) {

            write(
                    writer,
                    "OFFICE",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.courtRoles()
        ) {

            write(
                    writer,
                    "COURT_ROLE",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.militaryRoles()
        ) {

            write(
                    writer,
                    "MILITARY_ROLE",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.combatSpecialties()
        ) {

            write(
                    writer,
                    "COMBAT_SPECIALTY",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                var entry :
                profile.languages()
                        .entrySet()
        ) {

            write(
                    writer,
                    "LANGUAGE",
                    owner(
                            profile
                    ),
                    escape(
                            entry.getKey()
                    ),
                    Double.toString(
                            entry.getValue()
                    )
            );
        }

        for (
                String value :
                profile.motivations()
        ) {

            write(
                    writer,
                    "MOTIVATION",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.goals()
        ) {

            write(
                    writer,
                    "GOAL",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.fears()
        ) {

            write(
                    writer,
                    "FEAR",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.desires()
        ) {

            write(
                    writer,
                    "DESIRE",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.secrets()
        ) {

            write(
                    writer,
                    "SECRET",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                String value :
                profile.knownSecrets()
        ) {

            write(
                    writer,
                    "KNOWN_SECRET",
                    owner(
                            profile
                    ),
                    escape(
                            value
                    )
            );
        }

        for (
                var entry :
                profile.publicFacts()
                        .entrySet()
        ) {

            write(
                    writer,
                    "PUBLIC_FACT",
                    owner(
                            profile
                    ),
                    escape(
                            entry.getKey()
                    ),
                    escape(
                            entry.getValue()
                    )
            );
        }

        for (
                var entry :
                profile.privateFacts()
                        .entrySet()
        ) {

            write(
                    writer,
                    "PRIVATE_FACT",
                    owner(
                            profile
                    ),
                    escape(
                            entry.getKey()
                    ),
                    escape(
                            entry.getValue()
                    )
            );
        }
    }

    private static void decodeLine(
            Map<NpcId, CharacterProfile> loaded,
            String line,
            boolean legacyV1
    ) {

        String[] p =
                line.split(
                        "\t",
                        -1
                );

        switch (p[0]) {

            case "PROFILE" -> {

                if (legacyV1) {

                    decodeLegacyProfile(
                            loaded,
                            p
                    );

                } else {

                    decodeProfileV2(
                            loaded,
                            p
                    );
                }
            }

            case "SKILL" -> {

                requireLength(
                        p,
                        4,
                        "SKILL"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setSkill(
                                CharacterSkill.valueOf(
                                        p[2]
                                ),
                                Double.parseDouble(
                                        p[3]
                                )
                        );
            }

            case "APTITUDE" -> {

                requireLength(
                        p,
                        4,
                        "APTITUDE"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setAptitude(
                                CharacterAptitude.valueOf(
                                        p[2]
                                ),
                                Double.parseDouble(
                                        p[3]
                                )
                        );
            }

            case "DISPOSITION" -> {

                requireLength(
                        p,
                        4,
                        "DISPOSITION"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setDisposition(
                                CharacterDisposition.valueOf(
                                        p[2]
                                ),
                                Double.parseDouble(
                                        p[3]
                                )
                        );
            }

            case "CHARACTER_VALUE" -> {

                requireLength(
                        p,
                        4,
                        "CHARACTER_VALUE"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setCharacterValue(
                                CharacterValue.valueOf(
                                        p[2]
                                ),
                                Double.parseDouble(
                                        p[3]
                                )
                        );
            }

            case "SOCIAL_NORM" -> {

                requireLength(
                        p,
                        4,
                        "SOCIAL_NORM"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setSocialNorm(
                                CharacterSocialNorm.valueOf(
                                        p[2]
                                ),
                                Double.parseDouble(
                                        p[3]
                                )
                        );
            }

            case "VALUE" -> {

                requireLength(
                        p,
                        4,
                        "VALUE"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setValue(
                                unescape(
                                        p[2]
                                ),
                                Double.parseDouble(
                                        p[3]
                                )
                        );
            }

            case "POLITICAL" -> {

                requireLength(
                        p,
                        4,
                        "POLITICAL"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setPoliticalPreference(
                                unescape(
                                        p[2]
                                ),
                                Double.parseDouble(
                                        p[3]
                                )
                        );
            }

            case "TRAIT" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addTrait(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "ALIAS" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addAlias(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "OCCUPATION" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addOccupation(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "OFFICE" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addOffice(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "COURT_ROLE" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addCourtRole(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "MILITARY_ROLE" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addMilitaryRole(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "COMBAT_SPECIALTY" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addCombatSpecialty(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "LANGUAGE" -> {

                requireLength(
                        p,
                        4,
                        "LANGUAGE"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setLanguageProficiency(
                                unescape(
                                        p[2]
                                ),
                                Double.parseDouble(
                                        p[3]
                                )
                        );
            }

            case "MOTIVATION" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addMotivation(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "GOAL" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addGoal(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "FEAR" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addFear(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "DESIRE" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addDesire(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "SECRET" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addSecret(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "KNOWN_SECRET" ->
                    requireProfile(
                            loaded,
                            p[1]
                    )
                            .addKnownSecret(
                                    unescape(
                                            p[2]
                                    )
                            );

            case "PUBLIC_FACT" -> {

                requireLength(
                        p,
                        4,
                        "PUBLIC_FACT"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setPublicFact(
                                unescape(
                                        p[2]
                                ),
                                unescape(
                                        p[3]
                                )
                        );
            }

            case "PRIVATE_FACT" -> {

                requireLength(
                        p,
                        4,
                        "PRIVATE_FACT"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .setPrivateFact(
                                unescape(
                                        p[2]
                                ),
                                unescape(
                                        p[3]
                                )
                        );
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unknown profile record type: "
                                    + p[0]
                    );
        }
    }

    private static void decodeLegacyProfile(
            Map<NpcId, CharacterProfile> loaded,
            String[] p
    ) {

        if (p.length != 11) {

            throw new IllegalArgumentException(
                    "Expected 11 legacy PROFILE columns"
            );
        }

        NpcId npc =
                npcId(
                        p[1]
                );

        CharacterProfile profile =
                new CharacterProfile(
                        npc,
                        unescape(
                                p[2]
                        ),
                        unescape(
                                p[3]
                        ),
                        unescape(
                                p[4]
                        ),
                        new DialoguePersona(
                                Double.parseDouble(
                                        p[5]
                                ),
                                Double.parseDouble(
                                        p[6]
                                ),
                                Double.parseDouble(
                                        p[7]
                                ),
                                Double.parseDouble(
                                        p[8]
                                ),
                                unescape(
                                        p[9]
                                ),
                                unescape(
                                        p[10]
                                )
                        )
                );

        loaded.put(
                npc,
                profile
        );
    }

    private static void decodeProfileV2(
            Map<NpcId, CharacterProfile> loaded,
            String[] p
    ) {

        requireLength(
                p,
                23,
                "PROFILE"
        );

        NpcId npc =
                npcId(
                        p[1]
                );

        CharacterProfile profile =
                new CharacterProfile(
                        npc,
                        unescape(
                                p[2]
                        ),
                        unescape(
                                p[3]
                        ),
                        unescape(
                                p[4]
                        ),
                        new DialoguePersona(
                                Double.parseDouble(
                                        p[13]
                                ),
                                Double.parseDouble(
                                        p[14]
                                ),
                                Double.parseDouble(
                                        p[15]
                                ),
                                Double.parseDouble(
                                        p[16]
                                ),
                                unescape(
                                        p[17]
                                ),
                                unescape(
                                        p[18]
                                )
                        )
                );

        profile.setBirthplaceLocationId(
                unescape(
                        p[5]
                )
        );

        profile.setUpbringingLocationId(
                unescape(
                        p[6]
                )
        );

        profile.setOrientation(
                CharacterOrientation.valueOf(
                        p[7]
                )
        );

        profile.setHealthState(
                CharacterHealthState.valueOf(
                        p[8]
                )
        );

        profile.setLegalStatus(
                CharacterLegalStatus.valueOf(
                        p[9]
                )
        );

        profile.setWealth(
                Double.parseDouble(
                        p[10]
                )
        );

        profile.setSocialStatus(
                Double.parseDouble(
                        p[11]
                )
        );

        profile.setReputation(
                Double.parseDouble(
                        p[12]
                )
        );

        profile.setAppearanceDescription(
                unescape(
                        p[19]
                )
        );

        profile.setHairDescription(
                unescape(
                        p[20]
                )
        );

        profile.setEyeDescription(
                unescape(
                        p[21]
                )
        );

        profile.setBuildDescription(
                unescape(
                        p[22]
                )
        );

        loaded.put(
                npc,
                profile
        );
    }

    private static CharacterProfile requireProfile(
            Map<NpcId, CharacterProfile> loaded,
            String rawNpc
    ) {

        NpcId npc =
                npcId(
                        rawNpc
                );

        CharacterProfile profile =
                loaded.get(
                        npc
                );

        if (profile == null) {

            throw new IllegalStateException(
                    "Profile child record appeared before PROFILE for NPC "
                            + npc
            );
        }

        return profile;
    }

    private static NpcId npcId(
            String value
    ) {

        return new NpcId(
                Long.parseLong(
                        value
                )
        );
    }

    private static String owner(
            CharacterProfile profile
    ) {

        return Long.toString(
                profile.owner()
                        .value()
        );
    }

    private static void write(
            BufferedWriter writer,
            String... parts
    ) throws IOException {

        writer.write(
                String.join(
                        "\t",
                        parts
                )
        );

        writer.newLine();
    }

    private static void requireLength(
            String[] parts,
            int expected,
            String type
    ) {

        if (parts.length != expected) {

            throw new IllegalArgumentException(
                    "Expected "
                            + expected
                            + " "
                            + type
                            + " columns but got "
                            + parts.length
            );
        }
    }

    private static String escape(
            String value
    ) {

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\t",
                        "\\t"
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }

    private static String unescape(
            String value
    ) {

        StringBuilder result =
                new StringBuilder();

        boolean escaped =
                false;

        for (
                char character :
                value.toCharArray()
        ) {

            if (escaped) {

                switch (character) {

                    case 't' ->
                            result.append(
                                    '\t'
                            );

                    case 'n' ->
                            result.append(
                                    '\n'
                            );

                    case 'r' ->
                            result.append(
                                    '\r'
                            );

                    case '\\' ->
                            result.append(
                                    '\\'
                            );

                    default -> {

                        result.append(
                                '\\'
                        );

                        result.append(
                                character
                        );
                    }
                }

                escaped =
                        false;

            } else if (
                    character == '\\'
            ) {

                escaped =
                        true;

            } else {

                result.append(
                        character
                );
            }
        }

        if (escaped) {

            result.append(
                    '\\'
            );
        }

        return result.toString();
    }
}
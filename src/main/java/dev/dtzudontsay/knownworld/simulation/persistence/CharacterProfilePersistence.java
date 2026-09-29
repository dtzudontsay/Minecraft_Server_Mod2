package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfile;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
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

    private static final String HEADER =
            "KNOWNWORLD_CHARACTER_PROFILES\t1";

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
                    HEADER
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

        try (
                BufferedReader reader =
                        Files.newBufferedReader(
                                file,
                                StandardCharsets.UTF_8
                        )
        ) {
            String header =
                    reader.readLine();

            if (!HEADER.equals(
                    header
            )) {

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
                        line
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

        writer.write(
                String.join(
                        "\t",
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
                        )
                )
        );

        writer.newLine();

        for (
                var entry :
                profile.skills()
                        .entrySet()
        ) {

            writer.write(
                    String.join(
                            "\t",
                            "SKILL",
                            Long.toString(
                                    profile.owner()
                                            .value()
                            ),
                            entry.getKey()
                                    .name(),
                            Double.toString(
                                    entry.getValue()
                            )
                    )
            );

            writer.newLine();
        }

        for (
                String trait :
                profile.traits()
        ) {

            writer.write(
                    String.join(
                            "\t",
                            "TRAIT",
                            Long.toString(
                                    profile.owner()
                                            .value()
                            ),
                            escape(
                                    trait
                            )
                    )
            );

            writer.newLine();
        }

        for (
                var entry :
                profile.values()
                        .entrySet()
        ) {

            writer.write(
                    String.join(
                            "\t",
                            "VALUE",
                            Long.toString(
                                    profile.owner()
                                            .value()
                            ),
                            escape(
                                    entry.getKey()
                            ),
                            Double.toString(
                                    entry.getValue()
                            )
                    )
            );

            writer.newLine();
        }

        for (
                String motivation :
                profile.motivations()
        ) {

            writer.write(
                    String.join(
                            "\t",
                            "MOTIVATION",
                            Long.toString(
                                    profile.owner()
                                            .value()
                            ),
                            escape(
                                    motivation
                            )
                    )
            );

            writer.newLine();
        }
    }

    private static void decodeLine(
            Map<NpcId, CharacterProfile> loaded,
            String line
    ) {
        String[] p =
                line.split(
                        "\t",
                        -1
                );

        switch (p[0]) {

            case "PROFILE" -> {

                if (p.length != 11) {

                    throw new IllegalArgumentException(
                            "Expected 11 PROFILE columns"
                    );
                }

                NpcId npc =
                        new NpcId(
                                Long.parseLong(
                                        p[1]
                                )
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

            case "SKILL" -> {

                requireLength(
                        p,
                        4,
                        "SKILL"
                );

                CharacterProfile profile =
                        requireProfile(
                                loaded,
                                p[1]
                        );

                profile.setSkill(
                        CharacterSkill.valueOf(
                                p[2]
                        ),
                        Double.parseDouble(
                                p[3]
                        )
                );
            }

            case "TRAIT" -> {

                requireLength(
                        p,
                        3,
                        "TRAIT"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .addTrait(
                                unescape(
                                        p[2]
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

            case "MOTIVATION" -> {

                requireLength(
                        p,
                        3,
                        "MOTIVATION"
                );

                requireProfile(
                        loaded,
                        p[1]
                )
                        .addMotivation(
                                unescape(
                                        p[2]
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

    private static CharacterProfile requireProfile(
            Map<NpcId, CharacterProfile> loaded,
            String rawNpc
    ) {
        NpcId npc =
                new NpcId(
                        Long.parseLong(
                                rawNpc
                        )
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
                            + " columns"
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
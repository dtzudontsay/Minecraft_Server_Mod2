package dev.dtzudontsay.knownworld.simulation.npc.psychology;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterProfileManager;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceProfile;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolution;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceResolver;
import dev.dtzudontsay.knownworld.world.reference.influence.RegionalInfluenceValue;

import java.util.Objects;
import java.util.Optional;

public final class RegionalCharacterInfluenceService {

    private final CharacterPsychologyService psychology;

    private final CharacterProfileManager profiles;

    public RegionalCharacterInfluenceService(
            CharacterPsychologyService psychology,
            CharacterProfileManager profiles
    ) {

        this.psychology =
                Objects.requireNonNull(
                        psychology,
                        "psychology"
                );

        this.profiles =
                Objects.requireNonNull(
                        profiles,
                        "profiles"
                );
    }

    public Optional<RegionalInfluenceResolution> resolve(
            double minecraftX,
            double minecraftZ
    ) {

        return RegionalInfluenceResolver.resolveMinecraft(
                minecraftX,
                minecraftZ
        );
    }

    public int applyEnvironmentalInfluence(
            NpcId npc,
            double minecraftX,
            double minecraftZ,
            double strength
    ) {

        RegionalInfluenceResolution resolution =
                resolve(
                        minecraftX,
                        minecraftZ
                )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No regional influence profile resolves at this position"
                                        )
                        );

        return applyEnvironmentalInfluence(
                npc,
                resolution,
                strength
        );
    }

    public int applyEnvironmentalInfluence(
            NpcId npc,
            RegionalInfluenceResolution resolution,
            double strength
    ) {

        Objects.requireNonNull(
                npc,
                "npc"
        );

        Objects.requireNonNull(
                resolution,
                "resolution"
        );

        double normalizedStrength =
                clampUnit(
                        strength
                );

        RegionalInfluenceProfile profile =
                resolution.effectiveProfile();

        int changed =
                0;

        for (
                var entry :
                profile.characterValues()
                        .entrySet()
        ) {

            RegionalInfluenceValue influence =
                    entry.getValue();

            if (!influence.known()) {
                continue;
            }

            CharacterInfluenceResult result =
                    psychology.applyValuePressure(
                            npc,
                            entry.getKey(),
                            influence.value(),
                            normalizedStrength
                                    * influence.confidence(),
                            CharacterInfluenceChannel.REGIONAL
                    );

            if (result.changed()) {
                changed++;
            }
        }

        for (
                var entry :
                profile.socialNorms()
                        .entrySet()
        ) {

            RegionalInfluenceValue influence =
                    entry.getValue();

            if (!influence.known()) {
                continue;
            }

            CharacterInfluenceResult result =
                    psychology.applySocialNormPressure(
                            npc,
                            entry.getKey(),
                            influence.value(),
                            normalizedStrength
                                    * influence.confidence(),
                            CharacterInfluenceChannel.REGIONAL
                    );

            if (result.changed()) {
                changed++;
            }
        }

        return changed;
    }

    public int applyUpbringingInfluence(
            NpcId npc,
            double minecraftX,
            double minecraftZ,
            double strength
    ) {

        RegionalInfluenceResolution resolution =
                resolve(
                        minecraftX,
                        minecraftZ
                )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No regional influence profile resolves at this position"
                                        )
                        );

        profiles.getOrCreate(
                        npc
                )
                .setUpbringingLocationId(
                        resolution.mostSpecificLocationId()
                );

        return applyEnvironmentalInfluence(
                npc,
                resolution,
                strength
        );
    }

    private static double clampUnit(
            double value
    ) {

        if (!Double.isFinite(
                value
        )) {

            throw new IllegalArgumentException(
                    "Influence strength must be finite"
            );
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
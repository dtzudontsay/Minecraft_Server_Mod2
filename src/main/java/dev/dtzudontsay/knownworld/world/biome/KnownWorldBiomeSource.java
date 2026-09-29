package dev.dtzudontsay.knownworld.world.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;


public final class KnownWorldBiomeSource extends BiomeSource {

    public static final MapCodec<KnownWorldBiomeSource> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            RegistryOps.retrieveGetter(
                                                    Registries.BIOME
                                            )
                                    )
                                    .apply(
                                            instance,
                                            KnownWorldBiomeSource::new
                                    )
            );


    private static final List<String> AUTOMATIC_BIOME_IDS =
            List.of(
                    "minecraft:ocean",

                    "minecraft:snowy_plains",
                    "minecraft:ice_spikes",
                    "minecraft:snowy_taiga",

                    "minecraft:taiga",
                    "minecraft:old_growth_pine_taiga",
                    "minecraft:old_growth_spruce_taiga",

                    "minecraft:forest",
                    "minecraft:birch_forest",
                    "minecraft:plains",

                    "minecraft:savanna",
                    "minecraft:savanna_plateau",
                    "minecraft:windswept_savanna",

                    "minecraft:desert",
                    "minecraft:badlands",

                    "minecraft:sparse_jungle",
                    "minecraft:jungle",
                    "minecraft:bamboo_jungle",

                    "minecraft:swamp",
                    "minecraft:mangrove_swamp",

                    "minecraft:grove",
                    "minecraft:meadow",
                    "minecraft:snowy_slopes",
                    "minecraft:frozen_peaks",
                    "minecraft:jagged_peaks",
                    "minecraft:stony_peaks"
            );


    private final HolderGetter<Biome> biomeRegistry;

    private final KnownWorldBiomeResolver resolver;

    private final KnownWorldBiomeRasterData rasterData;


    public KnownWorldBiomeSource(
            HolderGetter<Biome> biomeRegistry
    ) {

        this.biomeRegistry =
                biomeRegistry;

        this.resolver =
                new KnownWorldBiomeResolver();

        this.rasterData =
                KnownWorldBiomeRasterData.getInstance();
    }


    @Override
    protected MapCodec<? extends BiomeSource> codec() {

        return CODEC;
    }


    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {

        Stream<String> automatic =
                AUTOMATIC_BIOME_IDS.stream();


        Stream<String> overrides =
                rasterData
                        .overrideBiomeIds()
                        .stream();


        return Stream
                .concat(
                        automatic,
                        overrides
                )
                .distinct()
                .map(
                        this::getBiomeHolder
                );
    }


    /*
     * Minecraft 26.3:
     *
     * BiomeSource now creates a BiomeResolver.
     *
     * The Climate.Sampler is provided here when the resolver is
     * created, but the BiomeResolver itself is queried only with
     * quart X/Y/Z coordinates.
     *
     * Our geography is based entirely on canonical X/Z position,
     * so quartY and the climate sampler are not needed here yet.
     */
    @Override
    public BiomeResolver createResolver(
            Climate.Sampler sampler
    ) {

        return (
                quartX,
                quartY,
                quartZ
        ) -> {

            /*
             * One quart coordinate represents four Minecraft blocks.
             *
             * Sample approximately at the centre of the 4x4 biome
             * column.
             */

            int blockX =
                    quartX
                            * 4
                            + 2;


            int blockZ =
                    quartZ
                            * 4
                            + 2;


            String biomeId =
                    resolver.resolveBiomeId(
                            blockX,
                            blockZ
                    );


            return getBiomeHolder(
                    biomeId
            );
        };
    }


    private Holder<Biome> getBiomeHolder(
            String biomeId
    ) {

        ResourceKey<Biome> key =
                ResourceKey.create(
                        Registries.BIOME,
                        identifier(
                                biomeId
                        )
                );


        return biomeRegistry.getOrThrow(
                key
        );
    }


    private static Identifier identifier(
            String value
    ) {

        int separator =
                value.indexOf(
                        ':'
                );


        if (
                separator < 0
        ) {

            return Identifier.fromNamespaceAndPath(
                    "minecraft",
                    value
            );
        }


        String namespace =
                value.substring(
                        0,
                        separator
                );


        String path =
                value.substring(
                        separator + 1
                );


        return Identifier.fromNamespaceAndPath(
                namespace,
                path
        );
    }
}
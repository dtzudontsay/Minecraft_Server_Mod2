from dataclasses import dataclass
from typing import Dict, Tuple


@dataclass(frozen=True)
class BiomeOverride:
    code: int
    biome_id: str
    display_name: str
    color: Tuple[int, int, int]


BIOME_OVERRIDES = (

    # ---------------------------------------------------------
    # TEMPERATE / FOREST
    # ---------------------------------------------------------

    BiomeOverride(
        1,
        "minecraft:plains",
        "Plains",
        (255, 23, 68),
    ),

    BiomeOverride(
        2,
        "minecraft:sunflower_plains",
        "Sunflower Plains",
        (255, 109, 0),
    ),

    BiomeOverride(
        3,
        "minecraft:forest",
        "Forest",
        (27, 94, 32),
    ),

    BiomeOverride(
        4,
        "minecraft:flower_forest",
        "Flower Forest",
        (170, 0, 255),
    ),

    BiomeOverride(
        5,
        "minecraft:birch_forest",
        "Birch Forest",
        (118, 255, 3),
    ),

    BiomeOverride(
        6,
        "minecraft:old_growth_birch_forest",
        "Old Growth Birch Forest",
        (198, 255, 0),
    ),

    BiomeOverride(
        7,
        "minecraft:dark_forest",
        "Dark Forest",
        (0, 77, 64),
    ),

    BiomeOverride(
        8,
        "minecraft:pale_garden",
        "Pale Garden",
        (176, 190, 197),
    ),

    BiomeOverride(
        9,
        "minecraft:cherry_grove",
        "Cherry Grove",
        (255, 64, 129),
    ),

    BiomeOverride(
        10,
        "minecraft:meadow",
        "Meadow",
        (224, 64, 251),
    ),

    BiomeOverride(
        11,
        "minecraft:dappled_forest",
        "Dappled Forest",
        (255, 138, 101),
    ),


    # ---------------------------------------------------------
    # COLD / TAIGA / MOUNTAIN
    # ---------------------------------------------------------

    BiomeOverride(
        12,
        "minecraft:snowy_plains",
        "Snowy Plains",
        (0, 229, 255),
    ),

    BiomeOverride(
        13,
        "minecraft:ice_spikes",
        "Ice Spikes",
        (41, 121, 255),
    ),

    BiomeOverride(
        14,
        "minecraft:taiga",
        "Taiga",
        (0, 137, 123),
    ),

    BiomeOverride(
        15,
        "minecraft:snowy_taiga",
        "Snowy Taiga",
        (77, 208, 225),
    ),

    BiomeOverride(
        16,
        "minecraft:old_growth_pine_taiga",
        "Old Growth Pine Taiga",
        (0, 105, 92),
    ),

    BiomeOverride(
        17,
        "minecraft:old_growth_spruce_taiga",
        "Old Growth Spruce Taiga",
        (46, 125, 50),
    ),

    BiomeOverride(
        18,
        "minecraft:grove",
        "Grove",
        (0, 172, 193),
    ),

    BiomeOverride(
        19,
        "minecraft:snowy_slopes",
        "Snowy Slopes",
        (130, 177, 255),
    ),

    BiomeOverride(
        20,
        "minecraft:frozen_peaks",
        "Frozen Peaks",
        (48, 79, 254),
    ),

    BiomeOverride(
        21,
        "minecraft:jagged_peaks",
        "Jagged Peaks",
        (101, 31, 255),
    ),

    BiomeOverride(
        22,
        "minecraft:stony_peaks",
        "Stony Peaks",
        (96, 125, 139),
    ),

    BiomeOverride(
        23,
        "minecraft:windswept_hills",
        "Windswept Hills",
        (141, 110, 99),
    ),

    BiomeOverride(
        24,
        "minecraft:windswept_gravelly_hills",
        "Windswept Gravelly Hills",
        (120, 144, 156),
    ),

    BiomeOverride(
        25,
        "minecraft:windswept_forest",
        "Windswept Forest",
        (51, 105, 30),
    ),


    # ---------------------------------------------------------
    # WARM / DRY
    # ---------------------------------------------------------

    BiomeOverride(
        26,
        "minecraft:desert",
        "Desert",
        (255, 214, 0),
    ),

    BiomeOverride(
        27,
        "minecraft:savanna",
        "Savanna",
        (249, 168, 37),
    ),

    BiomeOverride(
        28,
        "minecraft:savanna_plateau",
        "Savanna Plateau",
        (245, 127, 23),
    ),

    BiomeOverride(
        29,
        "minecraft:windswept_savanna",
        "Windswept Savanna",
        (230, 81, 0),
    ),

    BiomeOverride(
        30,
        "minecraft:badlands",
        "Badlands",
        (216, 67, 21),
    ),

    BiomeOverride(
        31,
        "minecraft:eroded_badlands",
        "Eroded Badlands",
        (191, 54, 12),
    ),

    BiomeOverride(
        32,
        "minecraft:wooded_badlands",
        "Wooded Badlands",
        (161, 136, 127),
    ),


    # ---------------------------------------------------------
    # JUNGLE / WETLAND
    # ---------------------------------------------------------

    BiomeOverride(
        33,
        "minecraft:jungle",
        "Jungle",
        (0, 191, 165),
    ),

    BiomeOverride(
        34,
        "minecraft:sparse_jungle",
        "Sparse Jungle",
        (105, 240, 174),
    ),

    BiomeOverride(
        35,
        "minecraft:bamboo_jungle",
        "Bamboo Jungle",
        (0, 230, 118),
    ),

    BiomeOverride(
        36,
        "minecraft:swamp",
        "Swamp",
        (0, 200, 83),
    ),

    BiomeOverride(
        37,
        "minecraft:mangrove_swamp",
        "Mangrove Swamp",
        (100, 221, 23),
    ),


    # ---------------------------------------------------------
    # COAST / RIVER / SPECIAL
    # ---------------------------------------------------------

    BiomeOverride(
        38,
        "minecraft:beach",
        "Beach",
        (255, 234, 0),
    ),

    BiomeOverride(
        39,
        "minecraft:snowy_beach",
        "Snowy Beach",
        (179, 229, 252),
    ),

    BiomeOverride(
        40,
        "minecraft:stony_shore",
        "Stony Shore",
        (84, 110, 122),
    ),

    BiomeOverride(
        41,
        "minecraft:river",
        "River",
        (0, 176, 255),
    ),

    BiomeOverride(
        42,
        "minecraft:frozen_river",
        "Frozen River",
        (64, 196, 255),
    ),

    BiomeOverride(
        43,
        "minecraft:mushroom_fields",
        "Mushroom Fields",
        (124, 77, 255),
    ),


    # ---------------------------------------------------------
    # NETHER
    # ---------------------------------------------------------

    BiomeOverride(
        44,
        "minecraft:nether_wastes",
        "Nether Wastes",
        (183, 28, 28),
    ),

    BiomeOverride(
        45,
        "minecraft:soul_sand_valley",
        "Soul Sand Valley",
        (121, 85, 72),
    ),

    BiomeOverride(
        46,
        "minecraft:crimson_forest",
        "Crimson Forest",
        (197, 17, 98),
    ),

    BiomeOverride(
        47,
        "minecraft:warped_forest",
        "Warped Forest",
        (0, 184, 212),
    ),

    BiomeOverride(
        48,
        "minecraft:basalt_deltas",
        "Basalt Deltas",
        (38, 50, 56),
    ),
)


BIOME_BY_CODE: Dict[int, BiomeOverride] = {
    biome.code: biome
    for biome in BIOME_OVERRIDES
}


BIOME_BY_ID: Dict[str, BiomeOverride] = {
    biome.biome_id: biome
    for biome in BIOME_OVERRIDES
}

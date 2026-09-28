from dataclasses import dataclass
from typing import Dict, Tuple


@dataclass(frozen=True)
class ClimateCategory:
    code: int
    name: str
    color: Tuple[int, int, int]


CLIMATE_CATEGORIES = (
    ClimateCategory(
        1,
        "POLAR",
        (0, 255, 255),
    ),
    ClimateCategory(
        2,
        "TUNDRA",
        (128, 255, 255),
    ),
    ClimateCategory(
        3,
        "BOREAL",
        (0, 255, 102),
    ),
    ClimateCategory(
        4,
        "COOL_TEMPERATE",
        (102, 255, 0),
    ),
    ClimateCategory(
        5,
        "TEMPERATE",
        (255, 255, 0),
    ),
    ClimateCategory(
        6,
        "WARM_TEMPERATE",
        (255, 204, 0),
    ),
    ClimateCategory(
        7,
        "MEDITERRANEAN",
        (255, 153, 0),
    ),
    ClimateCategory(
        8,
        "STEPPE",
        (204, 255, 0),
    ),
    ClimateCategory(
        9,
        "SEMI_ARID",
        (255, 102, 0),
    ),
    ClimateCategory(
        10,
        "DESERT",
        (255, 0, 102),
    ),
    ClimateCategory(
        11,
        "SUBTROPICAL",
        (0, 255, 204),
    ),
    ClimateCategory(
        12,
        "TROPICAL",
        (0, 204, 102),
    ),
    ClimateCategory(
        13,
        "WETLAND",
        (0, 102, 255),
    ),
)


CLIMATE_BY_CODE: Dict[int, ClimateCategory] = {
    category.code: category
    for category in CLIMATE_CATEGORIES
}


CLIMATE_BY_NAME: Dict[str, ClimateCategory] = {
    category.name: category
    for category in CLIMATE_CATEGORIES
}


def color_for_code(
    code: int
) -> Tuple[int, int, int]:

    category = CLIMATE_BY_CODE.get(
        int(code)
    )

    if category is None:
        return (
            0,
            0,
            0,
        )

    return category.color

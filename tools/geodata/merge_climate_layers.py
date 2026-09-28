from pathlib import Path

import numpy as np
from PIL import Image

from climate_palette import CLIMATE_CATEGORIES
from zone_transforms import (
    ZONES,
    ZONE_ORDER,
)


ROOT = Path(__file__).resolve().parent

INPUT_DIR = (
    ROOT
    / "output"
    / "validated_climate_masks"
)

OUTPUT_DIR = (
    ROOT
    / "output"
)

MASTER_ART_PATH = (
    ROOT
    / "input"
    / "master"
    / "known_world_master.jpg"
)

CANONICAL_LAND_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_canonical_land_mask.png"
)

MASTER_CLIMATE_PATH = (
    OUTPUT_DIR
    / "known_world_climate.png"
)

MASTER_PREVIEW_PATH = (
    OUTPUT_DIR
    / "known_world_climate_preview.png"
)

MASTER_OVERLAY_PATH = (
    OUTPUT_DIR
    / "known_world_climate_overlay.png"
)

MASTER_COVERAGE_PATH = (
    OUTPUT_DIR
    / "known_world_climate_coverage.png"
)


LOGICAL_MASTER_WIDTH = 2048.0
LOGICAL_MASTER_HEIGHT = 1357.0


def logical_to_actual_x(
    logical_x,
    width
):
    return (
        logical_x
        / LOGICAL_MASTER_WIDTH
        * width
    )


def logical_to_actual_y(
    logical_y,
    height
):
    return (
        logical_y
        / LOGICAL_MASTER_HEIGHT
        * height
    )


def actual_to_logical_x(
    actual_x,
    width
):
    return (
        actual_x
        / width
        * LOGICAL_MASTER_WIDTH
    )


def actual_to_logical_y(
    actual_y,
    height
):
    return (
        actual_y
        / height
        * LOGICAL_MASTER_HEIGHT
    )


def load_zone_codes(
    zone_id
):
    path = (
        INPUT_DIR
        / f"{zone_id}_climate.png"
    )

    if not path.exists():
        return None

    image = Image.open(
        path
    ).convert(
        "L"
    )

    zone = ZONES[
        zone_id
    ]

    if image.size != (
        zone.width,
        zone.height,
    ):
        raise ValueError(
            f"{zone_id.upper()} climate mask size mismatch.\n"
            f"Expected: "
            f"{zone.width} x {zone.height}\n"
            f"Actual: "
            f"{image.width} x {image.height}"
        )

    return np.asarray(
        image,
        dtype=np.uint8,
    )


def master_bounds(
    zone,
    master_width,
    master_height
):
    logical_min_x, logical_min_y, logical_max_x, logical_max_y = (
        zone.master_bounds()
    )

    min_x = int(
        np.floor(
            logical_to_actual_x(
                logical_min_x,
                master_width,
            )
        )
    )

    min_y = int(
        np.floor(
            logical_to_actual_y(
                logical_min_y,
                master_height,
            )
        )
    )

    max_x = int(
        np.ceil(
            logical_to_actual_x(
                logical_max_x,
                master_width,
            )
        )
    )

    max_y = int(
        np.ceil(
            logical_to_actual_y(
                logical_max_y,
                master_height,
            )
        )
    )

    padding = 4

    return (
        max(
            0,
            min_x - padding,
        ),
        max(
            0,
            min_y - padding,
        ),
        min(
            master_width,
            max_x + padding,
        ),
        min(
            master_height,
            max_y + padding,
        ),
    )


def merge_zone(
    zone_id,
    source,
    master_codes,
    master_priority,
    master_width,
    master_height
):
    zone = ZONES[
        zone_id
    ]

    (
        min_x,
        min_y,
        max_x,
        max_y,
    ) = master_bounds(
        zone,
        master_width,
        master_height,
    )

    determinant = (
        zone.a
        * zone.e
        - zone.b
        * zone.d
    )

    if abs(
        determinant
    ) < 1e-12:
        raise ValueError(
            f"{zone_id} transform is not invertible"
        )

    actual_x = np.arange(
        min_x,
        max_x,
        dtype=np.float64,
    )

    logical_x = actual_to_logical_x(
        actual_x + 0.5,
        master_width,
    )

    inserted = 0
    conflicts = 0

    normalization = float(
        max(
            1,
            min(
                zone.width,
                zone.height,
            )
            // 2,
        )
    )

    for actual_y in range(
        min_y,
        max_y,
    ):
        logical_y = actual_to_logical_y(
            actual_y + 0.5,
            master_height,
        )

        px = (
            logical_x
            - zone.c
        )

        py = (
            logical_y
            - zone.f
        )

        regional_x = (
            zone.e
            * px
            - zone.b
            * py
        ) / determinant

        regional_y = (
            -zone.d
            * px
            + zone.a
            * py
        ) / determinant

        sample_x = np.rint(
            regional_x
        ).astype(
            np.int32
        )

        sample_y = np.rint(
            regional_y
        ).astype(
            np.int32
        )

        valid = (
            (sample_x >= 0)
            &
            (sample_x < zone.width)
            &
            (sample_y >= 0)
            &
            (sample_y < zone.height)
        )

        if not np.any(
            valid
        ):
            continue

        positions = np.nonzero(
            valid
        )[0]

        sx = sample_x[
            valid
        ]

        sy = sample_y[
            valid
        ]

        codes = source[
            sy,
            sx,
        ]

        painted = (
            codes > 0
        )

        if not np.any(
            painted
        ):
            continue

        positions = positions[
            painted
        ]

        sx = sx[
            painted
        ]

        sy = sy[
            painted
        ]

        codes = codes[
            painted
        ]

        target_x = (
            min_x
            + positions
        )

        edge_distance = np.minimum.reduce(
            (
                sx,
                zone.width
                - 1
                - sx,
                sy,
                zone.height
                - 1
                - sy,
            )
        ).astype(
            np.float64
        )

        priority = np.clip(
            edge_distance
            / normalization
            * 65534.0
            + 1.0,
            1.0,
            65535.0,
        ).astype(
            np.uint16
        )

        old_codes = master_codes[
            actual_y,
            target_x,
        ]

        old_priority = master_priority[
            actual_y,
            target_x,
        ]

        conflicts += int(
            np.count_nonzero(
                (old_codes > 0)
                &
                (old_codes != codes)
            )
        )

        replace = (
            (old_codes == 0)
            |
            (priority > old_priority)
        )

        if not np.any(
            replace
        ):
            continue

        replace_x = target_x[
            replace
        ]

        master_codes[
            actual_y,
            replace_x,
        ] = codes[
            replace
        ]

        master_priority[
            actual_y,
            replace_x,
        ] = priority[
            replace
        ]

        inserted += int(
            np.count_nonzero(
                replace
            )
        )

    print(
        f"  inserted/replaced: {inserted:,}"
    )

    print(
        f"  overlap conflicts seen: {conflicts:,}"
    )


def build_color_preview(
    codes
):
    preview = np.zeros(
        (
            codes.shape[0],
            codes.shape[1],
            3,
        ),
        dtype=np.uint8,
    )

    for category in CLIMATE_CATEGORIES:
        preview[
            codes
            == category.code
        ] = category.color

    return preview


def build_overlay(
    artwork,
    codes
):
    source = np.asarray(
        artwork.convert(
            "RGB"
        ),
        dtype=np.float32,
    )

    preview = build_color_preview(
        codes
    ).astype(
        np.float32
    )

    painted = (
        codes > 0
    )[
        :,
        :,
        None
    ].astype(
        np.float32
    )

    alpha = (
        painted
        * 0.58
    )

    output = (
        source
        * (
            1.0
            - alpha
        )
        +
        preview
        * alpha
    )

    return Image.fromarray(
        np.clip(
            output,
            0,
            255,
        ).astype(
            np.uint8
        ),
        mode="RGB",
    )


def main():
    print(
        "Known World master climate merger"
    )

    master_art = Image.open(
        MASTER_ART_PATH
    ).convert(
        "RGB"
    )

    land_image = Image.open(
        CANONICAL_LAND_MASK_PATH
    ).convert(
        "L"
    )

    if master_art.size != land_image.size:
        raise ValueError(
            "Master artwork and land mask sizes differ"
        )

    master_width, master_height = (
        master_art.size
    )

    canonical_land = (
        np.asarray(
            land_image,
            dtype=np.uint8,
        )
        >= 128
    )

    master_codes = np.zeros(
        (
            master_height,
            master_width,
        ),
        dtype=np.uint8,
    )

    master_priority = np.zeros(
        (
            master_height,
            master_width,
        ),
        dtype=np.uint16,
    )

    loaded = 0

    for zone_id in ZONE_ORDER:
        print()
        print(
            f"{zone_id.upper()} - "
            f"{ZONES[zone_id].name}"
        )

        source = load_zone_codes(
            zone_id
        )

        if source is None:
            print(
                "  skipped - no validated climate mask"
            )

            continue

        loaded += 1

        merge_zone(
            zone_id,
            source,
            master_codes,
            master_priority,
            master_width,
            master_height,
        )

    before_clip = int(
        np.count_nonzero(
            master_codes
        )
    )

    master_codes[
        ~canonical_land
    ] = 0

    after_clip = int(
        np.count_nonzero(
            master_codes
        )
    )

    removed_over_water = (
        before_clip
        - after_clip
    )

    Image.fromarray(
        master_codes,
        mode="L",
    ).save(
        MASTER_CLIMATE_PATH
    )

    preview = build_color_preview(
        master_codes
    )

    Image.fromarray(
        preview,
        mode="RGB",
    ).save(
        MASTER_PREVIEW_PATH
    )

    overlay = build_overlay(
        master_art,
        master_codes,
    )

    overlay.save(
        MASTER_OVERLAY_PATH
    )

    coverage = np.where(
        master_codes > 0,
        255,
        0,
    ).astype(
        np.uint8
    )

    Image.fromarray(
        coverage,
        mode="L",
    ).save(
        MASTER_COVERAGE_PATH
    )

    land_pixels = int(
        np.count_nonzero(
            canonical_land
        )
    )

    assigned_land_pixels = int(
        np.count_nonzero(
            master_codes
        )
    )

    percentage = (
        assigned_land_pixels
        / land_pixels
        * 100.0
        if land_pixels > 0
        else 0.0
    )

    print()
    print(
        "=" * 70
    )

    print(
        "CLIMATE MERGE COMPLETE"
    )

    print()

    print(
        f"Regional masks loaded: {loaded}"
    )

    print(
        f"Canonical land pixels: "
        f"{land_pixels:,}"
    )

    print(
        f"Climate-assigned land: "
        f"{assigned_land_pixels:,} "
        f"({percentage:.2f}%)"
    )

    print(
        f"Paint removed over water: "
        f"{removed_over_water:,}"
    )

    print()

    print(
        "Generated:"
    )

    print(
        MASTER_CLIMATE_PATH
    )

    print(
        MASTER_PREVIEW_PATH
    )

    print(
        MASTER_OVERLAY_PATH
    )

    print(
        MASTER_COVERAGE_PATH
    )


if __name__ == "__main__":
    main()

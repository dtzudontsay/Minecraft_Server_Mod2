from pathlib import Path

import numpy as np
from PIL import Image

from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

INPUT_DIR = (
    ROOT
    / "output"
    / "validated_relief_masks"
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

MASTER_RELIEF_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_authored_relief_mask.png"
)

MASTER_RELIEF_OVERLAY_PATH = (
    OUTPUT_DIR
    / "known_world_authored_relief_overlay.png"
)

MASTER_RELIEF_COVERAGE_PATH = (
    OUTPUT_DIR
    / "known_world_authored_relief_coverage.png"
)


LOGICAL_MASTER_WIDTH = 2048
LOGICAL_MASTER_HEIGHT = 1357


def actual_to_logical_x(
    actual_x,
    master_width
):
    return (
        actual_x
        / master_width
        * LOGICAL_MASTER_WIDTH
    )


def actual_to_logical_y(
    actual_y,
    master_height
):
    return (
        actual_y
        / master_height
        * LOGICAL_MASTER_HEIGHT
    )


def logical_to_actual_x(
    logical_x,
    master_width
):
    return (
        logical_x
        / LOGICAL_MASTER_WIDTH
        * master_width
    )


def logical_to_actual_y(
    logical_y,
    master_height
):
    return (
        logical_y
        / LOGICAL_MASTER_HEIGHT
        * master_height
    )


def load_zone_mask(zone_id):
    path = (
        INPUT_DIR
        / f"{zone_id}_relief_mask.png"
    )

    if not path.exists():
        raise FileNotFoundError(
            f"Missing validated relief mask:\n{path}"
        )

    image = Image.open(
        path
    ).convert(
        "L"
    )

    zone = ZONES[
        zone_id
    ]

    expected_size = (
        zone.width,
        zone.height
    )

    if image.size != expected_size:
        raise ValueError(
            f"{zone_id.upper()} relief-mask size mismatch.\n"
            f"Expected: {expected_size}\n"
            f"Actual:   {image.size}\n"
            f"File:     {path}"
        )

    return (
        np.asarray(
            image,
            dtype=np.uint8
        )
        >= 128
    )


def calculate_actual_bounds(
    zone,
    master_width,
    master_height
):
    logical_min_x, logical_min_y, logical_max_x, logical_max_y = (
        zone.master_bounds()
    )

    actual_min_x = int(
        np.floor(
            logical_to_actual_x(
                logical_min_x,
                master_width
            )
        )
    )

    actual_max_x = int(
        np.ceil(
            logical_to_actual_x(
                logical_max_x,
                master_width
            )
        )
    )

    actual_min_y = int(
        np.floor(
            logical_to_actual_y(
                logical_min_y,
                master_height
            )
        )
    )

    actual_max_y = int(
        np.ceil(
            logical_to_actual_y(
                logical_max_y,
                master_height
            )
        )
    )

    padding = 4

    actual_min_x = max(
        0,
        actual_min_x - padding
    )

    actual_min_y = max(
        0,
        actual_min_y - padding
    )

    actual_max_x = min(
        master_width,
        actual_max_x + padding
    )

    actual_max_y = min(
        master_height,
        actual_max_y + padding
    )

    return (
        actual_min_x,
        actual_min_y,
        actual_max_x,
        actual_max_y
    )


def merge_zone(
    zone_id,
    master_relief,
    master_coverage,
    master_width,
    master_height
):
    zone = ZONES[
        zone_id
    ]

    source = load_zone_mask(
        zone_id
    )

    (
        min_x,
        min_y,
        max_x,
        max_y
    ) = calculate_actual_bounds(
        zone,
        master_width,
        master_height
    )

    determinant = (
        zone.a * zone.e
        - zone.b * zone.d
    )

    if abs(
        determinant
    ) < 1e-12:
        raise ValueError(
            f"Transform for {zone_id} is not invertible."
        )

    actual_x = np.arange(
        min_x,
        max_x,
        dtype=np.float64
    )

    logical_x = actual_to_logical_x(
        actual_x + 0.5,
        master_width
    )

    affected_pixels = 0

    for actual_y in range(
        min_y,
        max_y
    ):
        logical_y = actual_to_logical_y(
            actual_y + 0.5,
            master_height
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
            zone.e * px
            - zone.b * py
        ) / determinant

        regional_y = (
            -zone.d * px
            + zone.a * py
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

        valid_indices = np.nonzero(
            valid
        )[0]

        source_values = source[
            sample_y[
                valid
            ],
            sample_x[
                valid
            ]
        ]

        target_x = (
            min_x
            + valid_indices
        )

        master_coverage[
            actual_y,
            target_x
        ] = True

        relief_target_x = target_x[
            source_values
        ]

        master_relief[
            actual_y,
            relief_target_x
        ] = True

        affected_pixels += int(
            np.count_nonzero(
                source_values
            )
        )

    print(
        f"  Added relief samples: "
        f"{affected_pixels:,}"
    )


def build_overlay(
    artwork,
    relief_mask
):
    source = np.asarray(
        artwork.convert("RGB"),
        dtype=np.float32
    )

    output = source.copy()

    relief = relief_mask[
        :,
        :,
        None
    ].astype(
        np.float32
    )

    tint = np.zeros_like(
        output
    )

    tint[:, :, 0] = 255.0
    tint[:, :, 1] = 0.0
    tint[:, :, 2] = 255.0

    alpha = (
        relief
        * 0.65
    )

    output = (
        output
        * (
            1.0
            - alpha
        )
        +
        tint
        * alpha
    )

    return Image.fromarray(
        np.clip(
            output,
            0.0,
            255.0
        ).astype(
            np.uint8
        ),
        mode="RGB"
    )


def main():
    print(
        "Known World authored relief merger"
    )

    print()

    master_art = Image.open(
        MASTER_ART_PATH
    ).convert(
        "RGB"
    )

    canonical_land_image = Image.open(
        CANONICAL_LAND_MASK_PATH
    ).convert(
        "L"
    )

    if master_art.size != canonical_land_image.size:
        raise ValueError(
            "Master artwork and canonical land mask "
            "do not have matching dimensions.\n"
            f"Master artwork: {master_art.size}\n"
            f"Land mask:      {canonical_land_image.size}"
        )

    master_width, master_height = (
        master_art.size
    )

    print(
        f"Master resolution: "
        f"{master_width} x {master_height}"
    )

    canonical_land = (
        np.asarray(
            canonical_land_image,
            dtype=np.uint8
        )
        >= 128
    )

    master_relief = np.zeros(
        (
            master_height,
            master_width
        ),
        dtype=bool
    )

    master_coverage = np.zeros(
        (
            master_height,
            master_width
        ),
        dtype=bool
    )

    for zone_id in ZONE_ORDER:
        print()
        print(
            f"Merging "
            f"{zone_id.upper()} - "
            f"{ZONES[zone_id].name}"
        )

        merge_zone(
            zone_id,
            master_relief,
            master_coverage,
            master_width,
            master_height
        )

    # Union has already occurred naturally:
    #
    # if any transformed regional source marks a master pixel
    # as relief, that master pixel remains relief.

    before_land_clip = int(
        np.count_nonzero(
            master_relief
        )
    )

    master_relief &= canonical_land

    after_land_clip = int(
        np.count_nonzero(
            master_relief
        )
    )

    removed_over_water = (
        before_land_clip
        - after_land_clip
    )

    relief_encoded = np.where(
        master_relief,
        255,
        0
    ).astype(
        np.uint8
    )

    coverage_encoded = np.where(
        master_coverage,
        255,
        0
    ).astype(
        np.uint8
    )

    Image.fromarray(
        relief_encoded,
        mode="L"
    ).save(
        MASTER_RELIEF_MASK_PATH
    )

    Image.fromarray(
        coverage_encoded,
        mode="L"
    ).save(
        MASTER_RELIEF_COVERAGE_PATH
    )

    overlay = build_overlay(
        master_art,
        master_relief
    )

    overlay.save(
        MASTER_RELIEF_OVERLAY_PATH
    )

    land_pixels = int(
        np.count_nonzero(
            canonical_land
        )
    )

    relief_pixels = int(
        np.count_nonzero(
            master_relief
        )
    )

    relief_percentage = (
        relief_pixels
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
        "MERGE COMPLETE"
    )

    print()

    print(
        f"Canonical land pixels: "
        f"{land_pixels:,}"
    )

    print(
        f"Authored relief pixels: "
        f"{relief_pixels:,}"
    )

    print(
        f"Relief coverage: "
        f"{relief_percentage:.2f}% of canonical land"
    )

    print(
        f"Removed over-water relief: "
        f"{removed_over_water:,}"
    )

    print()

    print(
        "Generated:"
    )

    print(
        MASTER_RELIEF_MASK_PATH
    )

    print(
        MASTER_RELIEF_OVERLAY_PATH
    )

    print(
        MASTER_RELIEF_COVERAGE_PATH
    )


if __name__ == "__main__":
    main()

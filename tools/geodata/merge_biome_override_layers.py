from pathlib import Path
import json

import numpy as np
from PIL import Image

from biome_override_palette import BIOME_OVERRIDES
from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

INPUT_DIR = (
    ROOT
    / "output"
    / "validated_biome_override_masks"
)

OUTPUT_DIR = (
    ROOT
    / "output"
)

MASTER_DIR = (
    ROOT
    / "input"
    / "master"
)

LAND_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_canonical_land_mask.png"
)

OUTPUT_RASTER = (
    OUTPUT_DIR
    / "known_world_biome_override.png"
)

OUTPUT_PREVIEW = (
    OUTPUT_DIR
    / "known_world_biome_override_preview.png"
)

OUTPUT_OVERLAY = (
    OUTPUT_DIR
    / "known_world_biome_override_overlay.png"
)

OUTPUT_COVERAGE = (
    OUTPUT_DIR
    / "known_world_biome_override_coverage.png"
)

OUTPUT_LEGEND = (
    OUTPUT_DIR
    / "known_world_biome_override_legend.json"
)


LOGICAL_MASTER_WIDTH =
        2048.0

LOGICAL_MASTER_HEIGHT =
        1357.0


def find_master_art(
    expected_size
):

    candidates = []

    for path in MASTER_DIR.iterdir():

        if (
            not path.is_file()
            or
            path.suffix.lower()
            not in (
                ".png",
                ".jpg",
                ".jpeg",
            )
        ):
            continue

        try:

            with Image.open(
                path
            ) as image:

                if image.size == expected_size:

                    score = (
                        0
                        if "master"
                        in path.name.lower()
                        else 1
                    )

                    candidates.append(
                        (
                            score,
                            path,
                        )
                    )

        except Exception:
            pass

    if not candidates:

        return None

    candidates.sort(
        key=lambda value:
        (
            value[0],
            value[1].name,
        )
    )

    return candidates[
        0
    ][1]


def logical_to_actual_x(
    value,
    width
):

    return (
        value
        /
        LOGICAL_MASTER_WIDTH
        *
        width
    )


def logical_to_actual_y(
    value,
    height
):

    return (
        value
        /
        LOGICAL_MASTER_HEIGHT
        *
        height
    )


def actual_to_logical_x(
    value,
    width
):

    return (
        value
        /
        width
        *
        LOGICAL_MASTER_WIDTH
    )


def actual_to_logical_y(
    value,
    height
):

    return (
        value
        /
        height
        *
        LOGICAL_MASTER_HEIGHT
    )


def load_zone_codes(
    zone_id
):

    path = (
        INPUT_DIR
        / f"{zone_id}_biome_override.png"
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
            f"{zone_id.upper()} override "
            f"resolution mismatch."
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

    (
        logical_min_x,
        logical_min_y,
        logical_max_x,
        logical_max_y,
    ) = zone.master_bounds()

    padding =
            4

    min_x = max(
        0,
        int(
            np.floor(
                logical_to_actual_x(
                    logical_min_x,
                    master_width,
                )
            )
        )
        -
        padding,
    )

    min_y = max(
        0,
        int(
            np.floor(
                logical_to_actual_y(
                    logical_min_y,
                    master_height,
                )
            )
        )
        -
        padding,
    )

    max_x = min(
        master_width,
        int(
            np.ceil(
                logical_to_actual_x(
                    logical_max_x,
                    master_width,
                )
            )
        )
        +
        padding,
    )

    max_y = min(
        master_height,
        int(
            np.ceil(
                logical_to_actual_y(
                    logical_max_y,
                    master_height,
                )
            )
        )
        +
        padding,
    )

    return (
        min_x,
        min_y,
        max_x,
        max_y,
    )


def merge_zone(
    zone_id,
    source,
    master_codes,
    master_priority,
    master_width,
    master_height,
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
        *
        zone.e
        -
        zone.b
        *
        zone.d
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

    inserted =
            0

    conflicts =
            0

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
            -
            zone.c
        )

        py = (
            logical_y
            -
            zone.f
        )

        regional_x = (
            zone.e
            *
            px
            -
            zone.b
            *
            py
        ) / determinant

        regional_y = (
            -zone.d
            *
            px
            +
            zone.a
            *
            py
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
            +
            positions
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
            /
            normalization
            *
            65534.0
            +
            1.0,
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
        f"  overlap conflicts: {conflicts:,}"
    )


def create_preview(
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

    for biome in BIOME_OVERRIDES:

        preview[
            codes
            == biome.code
        ] = biome.color

    return preview


def create_overlay(
    artwork,
    preview,
    codes
):

    source = np.asarray(
        artwork.convert(
            "RGB"
        ),
        dtype=np.float32,
    )

    target = preview.astype(
        np.float32
    )

    alpha = (
        (
            codes > 0
        )[
            :,
            :,
            None
        ].astype(
            np.float32
        )
        *
        0.62
    )

    result = (
        source
        *
        (
            1.0
            -
            alpha
        )
        +
        target
        *
        alpha
    )

    return Image.fromarray(
        np.clip(
            result,
            0,
            255,
        ).astype(
            np.uint8
        ),
        mode="RGB",
    )


def write_legend():

    data = {
        "formatVersion": 1,
        "zeroMeans": "NO_OVERRIDE",
        "biomes": [
            {
                "code": biome.code,
                "biome": biome.biome_id,
                "name": biome.display_name,
                "color": (
                    "#"
                    f"{biome.color[0]:02X}"
                    f"{biome.color[1]:02X}"
                    f"{biome.color[2]:02X}"
                ),
            }
            for biome
            in BIOME_OVERRIDES
        ],
    }

    with open(
        OUTPUT_LEGEND,
        "w",
        encoding="utf-8",
    ) as file:

        json.dump(
            data,
            file,
            indent=2,
        )


def main():

    print(
        "Known World master biome override merger"
    )

    land_image = Image.open(
        LAND_MASK_PATH
    ).convert(
        "L"
    )

    master_width, master_height = (
        land_image.size
    )

    land = (
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

    loaded =
            0

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
                "  skipped - no biome overrides"
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
        ~land
    ] = 0

    after_clip = int(
        np.count_nonzero(
            master_codes
        )
    )

    Image.fromarray(
        master_codes,
        mode="L",
    ).save(
        OUTPUT_RASTER
    )

    preview = create_preview(
        master_codes
    )

    Image.fromarray(
        preview,
        mode="RGB",
    ).save(
        OUTPUT_PREVIEW
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
        OUTPUT_COVERAGE
    )

    master_art_path = find_master_art(
        (
            master_width,
            master_height,
        )
    )

    if master_art_path is not None:

        master_art = Image.open(
            master_art_path
        ).convert(
            "RGB"
        )

        overlay = create_overlay(
            master_art,
            preview,
            master_codes,
        )

        overlay.save(
            OUTPUT_OVERLAY
        )

        print(
            f"Master artwork: {master_art_path}"
        )

    else:

        print(
            "WARNING: no matching master artwork "
            "found; overlay was not generated."
        )

    write_legend()

    print()
    print(
        "=" * 70
    )

    print(
        "BIOME OVERRIDE MERGE COMPLETE"
    )

    print(
        f"Regional masks loaded: {loaded}"
    )

    print(
        f"Override land pixels: "
        f"{after_clip:,}"
    )

    print(
        f"Overrides removed over water: "
        f"{before_clip - after_clip:,}"
    )

    print()
    print(
        "Generated:"
    )

    print(
        OUTPUT_RASTER
    )

    print(
        OUTPUT_PREVIEW
    )

    print(
        OUTPUT_COVERAGE
    )

    print(
        OUTPUT_LEGEND
    )


if __name__ == "__main__":
    main()

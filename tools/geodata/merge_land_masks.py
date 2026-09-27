from pathlib import Path

from PIL import Image

from zone_transforms import (
    ZONES,
    ZONE_ORDER,
)


ROOT = Path(__file__).resolve().parent

MASTER_PATH = (
    ROOT
    / "input"
    / "master"
    / "known_world_master.jpg"
)

MASTER_MASK_PATH = (
    ROOT
    / "output"
    / "known_world_land_mask_preview.png"
)

REGIONAL_MASK_DIR = (
    ROOT
    / "output"
    / "regional_masks"
)

OUTPUT_DIR = (
    ROOT
    / "output"
)

MERGED_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_merged_land_mask.png"
)

MERGED_OVERLAY_PATH = (
    OUTPUT_DIR
    / "known_world_merged_overlay.png"
)

COVERAGE_PATH = (
    OUTPUT_DIR
    / "known_world_regional_coverage.png"
)


# ------------------------------------------------------------
# LOGICAL MASTER COORDINATE SYSTEM
# ------------------------------------------------------------
#
# All zone transforms were calibrated against this logical
# master-map raster.
#
LOGICAL_MASTER_WIDTH = 2048
LOGICAL_MASTER_HEIGHT = 1357


def load_master_mask():
    if not MASTER_MASK_PATH.exists():
        raise FileNotFoundError(
            f"Master fallback mask missing:\n"
            f"{MASTER_MASK_PATH}\n\n"
            f"Run build_land_mask.py first."
        )

    return Image.open(
        MASTER_MASK_PATH
    ).convert(
        "L"
    )


def load_master_art():
    if not MASTER_PATH.exists():
        raise FileNotFoundError(
            f"Master artwork missing:\n"
            f"{MASTER_PATH}"
        )

    return Image.open(
        MASTER_PATH
    ).convert(
        "RGB"
    )


def load_regional_mask(
    zone_id
):
    path = (
        REGIONAL_MASK_DIR
        / f"{zone_id}_land_mask.png"
    )

    if not path.exists():
        raise FileNotFoundError(
            f"Regional land mask missing:\n"
            f"{path}\n\n"
            f"Run build_regional_masks.py first."
        )

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
        zone.height
    ):
        raise ValueError(
            f"{zone_id.upper()} mask dimensions are wrong.\n"
            f"Expected: {zone.width} x {zone.height}\n"
            f"Actual:   {image.width} x {image.height}"
        )

    return image


def logical_to_actual_x(
    logical_x,
    actual_width
):
    return (
        logical_x
        / LOGICAL_MASTER_WIDTH
        * actual_width
    )


def logical_to_actual_y(
    logical_y,
    actual_height
):
    return (
        logical_y
        / LOGICAL_MASTER_HEIGHT
        * actual_height
    )


def actual_to_logical_x(
    actual_x,
    actual_width
):
    return (
        actual_x
        / actual_width
        * LOGICAL_MASTER_WIDTH
    )


def actual_to_logical_y(
    actual_y,
    actual_height
):
    return (
        actual_y
        / actual_height
        * LOGICAL_MASTER_HEIGHT
    )


def merge_zone(
    master_mask,
    coverage,
    zone_id
):
    zone = ZONES[
        zone_id
    ]

    regional_mask = load_regional_mask(
        zone_id
    )

    regional_pixels = regional_mask.load()
    master_pixels = master_mask.load()
    coverage_pixels = coverage.load()

    actual_width, actual_height = (
        master_mask.size
    )

    logical_min_x, logical_min_y, logical_max_x, logical_max_y = (
        zone.master_bounds()
    )

    actual_min_x = int(
        logical_to_actual_x(
            logical_min_x,
            actual_width
        )
    )

    actual_min_y = int(
        logical_to_actual_y(
            logical_min_y,
            actual_height
        )
    )

    actual_max_x = int(
        logical_to_actual_x(
            logical_max_x,
            actual_width
        )
    ) + 1

    actual_max_y = int(
        logical_to_actual_y(
            logical_max_y,
            actual_height
        )
    ) + 1

    actual_min_x = max(
        0,
        actual_min_x
    )

    actual_min_y = max(
        0,
        actual_min_y
    )

    actual_max_x = min(
        actual_width - 1,
        actual_max_x
    )

    actual_max_y = min(
        actual_height - 1,
        actual_max_y
    )

    written = 0

    for actual_y in range(
        actual_min_y,
        actual_max_y + 1
    ):
        for actual_x in range(
            actual_min_x,
            actual_max_x + 1
        ):

            logical_x = actual_to_logical_x(
                actual_x + 0.5,
                actual_width
            )

            logical_y = actual_to_logical_y(
                actual_y + 0.5,
                actual_height
            )

            regional_x, regional_y = (
                zone.master_to_regional(
                    logical_x,
                    logical_y
                )
            )

            rx = int(
                round(
                    regional_x
                )
            )

            ry = int(
                round(
                    regional_y
                )
            )

            if not (
                0 <= rx < zone.width
                and 0 <= ry < zone.height
            ):
                continue

            master_pixels[
                actual_x,
                actual_y
            ] = regional_pixels[
                rx,
                ry
            ]

            coverage_pixels[
                actual_x,
                actual_y
            ] = 255

            written += 1

    print(
        f"[{zone_id.upper()}] "
        f"{zone.name}: "
        f"{written:,} actual master pixels replaced"
    )


def build_overlay(
    master_art,
    merged_mask,
    coverage
):
    source = master_art.copy()

    source_pixels = source.load()
    mask_pixels = merged_mask.load()
    coverage_pixels = coverage.load()

    width, height = source.size

    for y in range(
        height
    ):
        for x in range(
            width
        ):

            r, g, b = source_pixels[
                x,
                y
            ]

            is_land = (
                mask_pixels[
                    x,
                    y
                ]
                >= 128
            )

            has_regional_data = (
                coverage_pixels[
                    x,
                    y
                ]
                >= 128
            )

            if is_land:

                if has_regional_data:
                    overlay = (
                        30,
                        225,
                        70
                    )
                else:
                    overlay = (
                        170,
                        205,
                        55
                    )

            else:

                if has_regional_data:
                    overlay = (
                        30,
                        110,
                        240
                    )
                else:
                    overlay = (
                        80,
                        120,
                        175
                    )

            source_pixels[
                x,
                y
            ] = (
                int(
                    r * 0.58
                    + overlay[0] * 0.42
                ),
                int(
                    g * 0.58
                    + overlay[1] * 0.42
                ),
                int(
                    b * 0.58
                    + overlay[2] * 0.42
                ),
            )

    return source


def main():

    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    print(
        "Known World regional-mask merger"
    )

    master_mask = load_master_mask()

    master_art = load_master_art()

    if master_mask.size != master_art.size:
        raise ValueError(
            "Master artwork and master mask dimensions differ.\n"
            f"Artwork: {master_art.width} x {master_art.height}\n"
            f"Mask:    {master_mask.width} x {master_mask.height}"
        )

    actual_width, actual_height = (
        master_mask.size
    )

    print(
        f"Actual master raster: "
        f"{actual_width} x {actual_height}"
    )

    print(
        f"Logical transform raster: "
        f"{LOGICAL_MASTER_WIDTH} x {LOGICAL_MASTER_HEIGHT}"
    )

    print(
        f"Scale actual/logical: "
        f"X {actual_width / LOGICAL_MASTER_WIDTH:.6f} | "
        f"Y {actual_height / LOGICAL_MASTER_HEIGHT:.6f}"
    )

    merged_mask = master_mask.copy()

    coverage = Image.new(
        "L",
        (
            actual_width,
            actual_height
        ),
        0
    )

    print()
    print(
        "Merging regional masks..."
    )

    for zone_id in ZONE_ORDER:

        merge_zone(
            merged_mask,
            coverage,
            zone_id
        )

    merged_mask.save(
        MERGED_MASK_PATH
    )

    coverage.save(
        COVERAGE_PATH
    )

    print()
    print(
        "Building merged debug overlay..."
    )

    overlay = build_overlay(
        master_art,
        merged_mask,
        coverage
    )

    overlay.save(
        MERGED_OVERLAY_PATH
    )

    print()
    print(
        "Generated:"
    )

    print(
        MERGED_MASK_PATH
    )

    print(
        COVERAGE_PATH
    )

    print(
        MERGED_OVERLAY_PATH
    )

    print()
    print(
        "Merged mask:"
    )

    print(
        "  WHITE = land"
    )

    print(
        "  BLACK = water"
    )

    print()
    print(
        "Overlay colours:"
    )

    print(
        "  bright green/blue = regional high-resolution data"
    )

    print(
        "  dull green/blue = master fallback data"
    )

    print()
    print(
        "Regional transforms remain calibrated in "
        "2048x1357 logical master coordinates."
    )


if __name__ == "__main__":
    main()

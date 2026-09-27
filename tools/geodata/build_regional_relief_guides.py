from pathlib import Path
import traceback

import numpy as np
from PIL import Image
from scipy.ndimage import (
    binary_opening,
    binary_propagation,
    gaussian_filter,
    label,
)

from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

INPUT_DIR = (
    ROOT
    / "input"
    / "regions"
)

LAND_MASK_DIR = (
    ROOT
    / "output"
    / "regional_masks"
)

OUTPUT_DIR = (
    ROOT
    / "output"
    / "regional_relief_guides"
)

OUTPUT_DIR.mkdir(
    parents=True,
    exist_ok=True
)


SUPPORT_THRESHOLD = 0.20
CORE_THRESHOLD = 0.48

WEAK_RELIEF_THRESHOLD = 0.10
STRONG_RELIEF_THRESHOLD = 0.50

MIN_CORE_AREA = 90
MIN_FINAL_AREA = 450

BORDER_EXCLUSION_PIXELS = 10


# ------------------------------------------------------------
# PURPOSE
# ------------------------------------------------------------
#
# Diagnostic relative-relief extraction.
#
# This script DOES NOT generate final elevation data.
#
# Strategy:
#
# 1. Calculate a multi-scale terrain-texture score.
# 2. Find HIGH-confidence, physically thick relief cores.
# 3. Remove tiny cores such as settlement icons and letters.
# 4. Grow relief only from those trusted cores into surrounding
#    lower-confidence terrain texture.
# 5. Remove final components that are still too small.
#
# This is intentionally conservative.
#
# ------------------------------------------------------------


def find_source_file(zone_id):
    candidates = (
        INPUT_DIR / f"{zone_id}.png",
        INPUT_DIR / f"{zone_id}.jpg",
        INPUT_DIR / f"{zone_id}.jpeg",
        INPUT_DIR / f"{zone_id.upper()}.png",
        INPUT_DIR / f"{zone_id.upper()}.jpg",
        INPUT_DIR / f"{zone_id.upper()}.jpeg",
    )

    for path in candidates:
        if path.exists():
            return path

    return None


def find_land_mask(zone_id):
    candidates = (
        LAND_MASK_DIR / f"{zone_id}_land_mask.png",
        LAND_MASK_DIR / f"{zone_id}_mask.png",
        LAND_MASK_DIR / f"{zone_id}.png",
    )

    for path in candidates:
        if path.exists():
            return path

    matching = sorted(
        LAND_MASK_DIR.glob(
            f"{zone_id}*land*mask*.png"
        )
    )

    if len(matching) == 1:
        return matching[0]

    return None


def make_disk(radius):
    y, x = np.ogrid[
        -radius:radius + 1,
        -radius:radius + 1
    ]

    return (
        x * x
        + y * y
        <= radius * radius
    )


def load_land_mask(
    path,
    expected_size
):
    image = Image.open(
        path
    ).convert(
        "L"
    )

    if image.size != expected_size:
        raise ValueError(
            "Regional land mask has wrong dimensions.\n"
            f"Expected: {expected_size}\n"
            f"Actual:   {image.size}\n"
            f"File:     {path}"
        )

    values = np.asarray(
        image,
        dtype=np.uint8
    )

    return values >= 128


def image_to_rgb(image):
    return (
        np.asarray(
            image.convert("RGB"),
            dtype=np.float32
        )
        / 255.0
    )


def rgb_to_luminance(rgb):
    red = rgb[:, :, 0]
    green = rgb[:, :, 1]
    blue = rgb[:, :, 2]

    return (
        red * 0.2126
        + green * 0.7152
        + blue * 0.0722
    ).astype(
        np.float32
    )


def normalize_percentile(
    values,
    valid_mask,
    low_percentile,
    high_percentile
):
    selected = values[
        valid_mask
    ]

    if selected.size == 0:
        return np.zeros_like(
            values,
            dtype=np.float32
        )

    low = float(
        np.percentile(
            selected,
            low_percentile
        )
    )

    high = float(
        np.percentile(
            selected,
            high_percentile
        )
    )

    if high <= low:
        return np.zeros_like(
            values,
            dtype=np.float32
        )

    normalized = (
        values - low
    ) / (
        high - low
    )

    return np.clip(
        normalized,
        0.0,
        1.0
    ).astype(
        np.float32
    )


def calculate_saturation(rgb):
    maximum = np.max(
        rgb,
        axis=2
    )

    minimum = np.min(
        rgb,
        axis=2
    )

    return (
        maximum - minimum
    ).astype(
        np.float32
    )


def build_forest_suppression(rgb):
    red = rgb[:, :, 0]
    green = rgb[:, :, 1]
    blue = rgb[:, :, 2]

    saturation = calculate_saturation(
        rgb
    )

    greenish = (
        (green > red * 1.04)
        &
        (green > blue * 1.02)
        &
        (saturation > 0.06)
    )

    density = gaussian_filter(
        greenish.astype(
            np.float32
        ),
        sigma=6.0
    )

    return np.clip(
        density * 2.5,
        0.0,
        1.0
    )


def build_raw_relief_score(
    artwork,
    land
):
    rgb = image_to_rgb(
        artwork
    )

    luminance = rgb_to_luminance(
        rgb
    )

    blur_small = gaussian_filter(
        luminance,
        sigma=1.2
    )

    blur_medium = gaussian_filter(
        luminance,
        sigma=4.0
    )

    blur_large = gaussian_filter(
        luminance,
        sigma=10.0
    )

    local_detail = np.abs(
        blur_small
        - blur_medium
    )

    dark_detail = np.maximum(
        blur_large
        - luminance,
        0.0
    )

    texture_density = gaussian_filter(
        local_detail,
        sigma=6.0
    )

    broad_texture_density = gaussian_filter(
        local_detail,
        sigma=15.0
    )

    detail_norm = normalize_percentile(
        local_detail,
        land,
        58.0,
        99.7
    )

    dark_norm = normalize_percentile(
        dark_detail,
        land,
        60.0,
        99.7
    )

    texture_norm = normalize_percentile(
        texture_density,
        land,
        48.0,
        99.5
    )

    broad_norm = normalize_percentile(
        broad_texture_density,
        land,
        42.0,
        99.5
    )

    score = (
        detail_norm * 0.10
        + dark_norm * 0.12
        + texture_norm * 0.38
        + broad_norm * 0.40
    )

    forest_suppression = build_forest_suppression(
        rgb
    )

    score *= (
        1.0
        - forest_suppression * 0.82
    )

    score[
        ~land
    ] = 0.0

    return np.clip(
        score,
        0.0,
        1.0
    ).astype(
        np.float32
    )


def remove_border_candidates(mask):
    result = mask.copy()

    border = BORDER_EXCLUSION_PIXELS

    result[
        :border,
        :
    ] = False

    result[
        -border:,
        :
    ] = False

    result[
        :,
        :border
    ] = False

    result[
        :,
        -border:
    ] = False

    return result


def retain_large_components(
    mask,
    minimum_area
):
    labelled, count = label(
        mask
    )

    result = np.zeros_like(
        mask,
        dtype=bool
    )

    for component_id in range(
        1,
        count + 1
    ):
        component = (
            labelled
            == component_id
        )

        area = int(
            np.count_nonzero(
                component
            )
        )

        if area < minimum_area:
            continue

        ys, xs = np.where(
            component
        )

        if xs.size == 0:
            continue

        width = int(
            xs.max()
            - xs.min()
            + 1
        )

        height = int(
            ys.max()
            - ys.min()
            + 1
        )

        short_side = min(
            width,
            height
        )

        long_side = max(
            width,
            height
        )

        aspect_ratio = (
            long_side
            / max(
                1,
                short_side
            )
        )

        # Very long and extremely narrow regions are generally
        # roads, rivers, seams, borders, or text strokes.
        if (
            aspect_ratio > 12.0
            and short_side < 10
        ):
            continue

        result[
            component
        ] = True

    return result


def build_relief_region(
    score,
    land
):
    support = (
        score
        >= SUPPORT_THRESHOLD
    )

    core = (
        score
        >= CORE_THRESHOLD
    )

    support[
        ~land
    ] = False

    core[
        ~land
    ] = False

    support = remove_border_candidates(
        support
    )

    core = remove_border_candidates(
        core
    )

    # A disk opening requires the core to have actual physical
    # thickness. Thin letters, roads and river lines disappear.
    core = binary_opening(
        core,
        structure=make_disk(
            3
        )
    )

    # Settlement icons can still contain a thick centre, so also
    # reject isolated cores that are too small.
    core = retain_large_components(
        core,
        MIN_CORE_AREA
    )

    if not np.any(
        core
    ):
        return (
            np.zeros_like(
                score,
                dtype=bool
            ),
            core
        )

    # Hysteresis growth:
    #
    # Lower-confidence terrain may join the final region only if
    # connected to one of our trusted thick cores.
    grown = binary_propagation(
        core,
        structure=np.ones(
            (
                3,
                3
            ),
            dtype=bool
        ),
        mask=support
    )

    grown = retain_large_components(
        grown,
        MIN_FINAL_AREA
    )

    grown[
        ~land
    ] = False

    return grown, core


def build_relief_guide(
    artwork,
    land
):
    raw_score = build_raw_relief_score(
        artwork,
        land
    )

    accepted_region, core = build_relief_region(
        raw_score,
        land
    )

    guide = np.zeros_like(
        raw_score,
        dtype=np.float32
    )

    if np.any(
        accepted_region
    ):
        accepted_score = np.clip(
            (
                raw_score
                - SUPPORT_THRESHOLD
            )
            / (
                1.0
                - SUPPORT_THRESHOLD
            ),
            0.0,
            1.0
        )

        guide[
            accepted_region
        ] = accepted_score[
            accepted_region
        ]

        guide = gaussian_filter(
            guide,
            sigma=2.5
        )

        guide[
            ~accepted_region
        ] = 0.0

    guide[
        ~land
    ] = 0.0

    return (
        np.clip(
            guide,
            0.0,
            1.0
        ).astype(
            np.float32
        ),
        core
    )


def save_grayscale(
    values,
    path
):
    encoded = np.clip(
        values * 255.0,
        0.0,
        255.0
    ).astype(
        np.uint8
    )

    Image.fromarray(
        encoded,
        mode="L"
    ).save(
        path
    )


def save_binary_mask(
    mask,
    path
):
    encoded = np.where(
        mask,
        255,
        0
    ).astype(
        np.uint8
    )

    Image.fromarray(
        encoded,
        mode="L"
    ).save(
        path
    )


def save_threshold_mask(
    values,
    threshold,
    path
):
    encoded = np.where(
        values >= threshold,
        255,
        0
    ).astype(
        np.uint8
    )

    Image.fromarray(
        encoded,
        mode="L"
    ).save(
        path
    )


def build_overlay(
    artwork,
    relief
):
    source = np.asarray(
        artwork.convert("RGB"),
        dtype=np.float32
    )

    tint = np.zeros_like(
        source
    )

    tint[:, :, 0] = 255.0
    tint[:, :, 1] = 0.0
    tint[:, :, 2] = 255.0

    alpha = (
        np.clip(
            relief,
            0.0,
            1.0
        )[:, :, None]
        * 0.82
    )

    output = (
        source
        * (
            1.0
            - alpha
        )
        + tint
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


def process_zone(zone_id):
    zone = ZONES[
        zone_id
    ]

    source_path = find_source_file(
        zone_id
    )

    if source_path is None:
        raise FileNotFoundError(
            f"No regional artwork found for {zone_id}."
        )

    land_mask_path = find_land_mask(
        zone_id
    )

    if land_mask_path is None:
        raise FileNotFoundError(
            "No regional land mask found for "
            f"{zone_id} in:\n{LAND_MASK_DIR}"
        )

    artwork = Image.open(
        source_path
    ).convert(
        "RGB"
    )

    if artwork.size != (
        zone.width,
        zone.height
    ):
        raise ValueError(
            f"{zone_id.upper()} artwork dimensions do not match "
            "zone calibration.\n"
            f"Expected: {zone.width} x {zone.height}\n"
            f"Actual:   {artwork.width} x {artwork.height}"
        )

    land = load_land_mask(
        land_mask_path,
        artwork.size
    )

    relief, core = build_relief_guide(
        artwork,
        land
    )

    guide_path = (
        OUTPUT_DIR
        / f"{zone_id}_relief_guide.png"
    )

    overlay_path = (
        OUTPUT_DIR
        / f"{zone_id}_relief_overlay.png"
    )

    core_path = (
        OUTPUT_DIR
        / f"{zone_id}_relief_core_mask.png"
    )

    weak_path = (
        OUTPUT_DIR
        / f"{zone_id}_relief_weak_mask.png"
    )

    strong_path = (
        OUTPUT_DIR
        / f"{zone_id}_relief_strong_mask.png"
    )

    save_grayscale(
        relief,
        guide_path
    )

    save_binary_mask(
        core,
        core_path
    )

    save_threshold_mask(
        relief,
        WEAK_RELIEF_THRESHOLD,
        weak_path
    )

    save_threshold_mask(
        relief,
        STRONG_RELIEF_THRESHOLD,
        strong_path
    )

    build_overlay(
        artwork,
        relief
    ).save(
        overlay_path
    )

    total_land = int(
        np.count_nonzero(
            land
        )
    )

    core_pixels = int(
        np.count_nonzero(
            core
        )
    )

    weak_pixels = int(
        np.count_nonzero(
            relief
            >= WEAK_RELIEF_THRESHOLD
        )
    )

    strong_pixels = int(
        np.count_nonzero(
            relief
            >= STRONG_RELIEF_THRESHOLD
        )
    )

    print()
    print(
        f"{zone_id.upper()} - {zone.name}"
    )

    print(
        f"  Artwork: {artwork.width} x {artwork.height}"
    )

    print(
        f"  Land pixels: {total_land:,}"
    )

    print(
        f"  Trusted core pixels: {core_pixels:,}"
    )

    print(
        f"  Weak relief pixels: {weak_pixels:,}"
    )

    print(
        f"  Strong relief pixels: {strong_pixels:,}"
    )

    print(
        f"  Guide:       {guide_path}"
    )

    print(
        f"  Overlay:     {overlay_path}"
    )

    print(
        f"  Core mask:   {core_path}"
    )

    print(
        f"  Weak mask:   {weak_path}"
    )

    print(
        f"  Strong mask: {strong_path}"
    )


def main():
    print(
        "Known World hysteresis relief extractor"
    )

    print()
    print(
        "This is still a diagnostic pass only."
    )

    print(
        "Only lower-confidence texture connected to trusted "
        "thick relief cores is retained."
    )

    successful = 0
    failed = 0

    for zone_id in ZONE_ORDER:
        try:
            process_zone(
                zone_id
            )

            successful += 1

        except Exception as error:
            failed += 1

            print()
            print("=" * 70)

            print(
                f"ERROR IN ZONE {zone_id.upper()}"
            )

            print(
                f"{type(error).__name__}: {error}"
            )

            traceback.print_exc()

    print()
    print("=" * 70)

    print(
        "DONE"
    )

    print(
        f"Successful zones: {successful}"
    )

    print(
        f"Failed zones:     {failed}"
    )

    print()
    print(
        f"Output directory:\n{OUTPUT_DIR}"
    )


if __name__ == "__main__":
    main()

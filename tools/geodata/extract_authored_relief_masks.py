from pathlib import Path

import numpy as np
from PIL import Image

from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

AUTHORING_DIR = (
    ROOT
    / "input"
    / "relief_authoring_new"
)

LAND_MASK_DIR = (
    ROOT
    / "output"
    / "regional_masks"
)

OUTPUT_DIR = (
    ROOT
    / "output"
    / "validated_relief_masks"
)

OUTPUT_DIR.mkdir(
    parents=True,
    exist_ok=True
)


# ------------------------------------------------------------
# AUTHORING COLOR
# ------------------------------------------------------------
#
# Paint high-relief / mountainous terrain with:
#
#     #FF00FF
#     RGB(255, 0, 255)
#
# A small tolerance is allowed in case the editor slightly
# modifies painted pixels.
#
# ------------------------------------------------------------

MAGENTA_RED_MIN = 245
MAGENTA_GREEN_MAX = 20
MAGENTA_BLUE_MIN = 245


def find_authoring_file(zone_id):
    candidates = (
        AUTHORING_DIR / f"{zone_id}.png",
        AUTHORING_DIR / f"{zone_id.upper()}.png",
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


def extract_magenta_mask(image):
    rgb = np.asarray(
        image.convert("RGB"),
        dtype=np.uint8
    )

    red = rgb[:, :, 0]
    green = rgb[:, :, 1]
    blue = rgb[:, :, 2]

    return (
        (red >= MAGENTA_RED_MIN)
        &
        (green <= MAGENTA_GREEN_MAX)
        &
        (blue >= MAGENTA_BLUE_MIN)
    )


def process_zone(zone_id):
    zone = ZONES[
        zone_id
    ]

    authoring_path = find_authoring_file(
        zone_id
    )

    if authoring_path is None:
        print(
            "  SKIPPED: authoring image does not exist."
        )

        return "missing"

    authoring = Image.open(
        authoring_path
    ).convert(
        "RGB"
    )

    expected_size = (
        zone.width,
        zone.height
    )

    if authoring.size != expected_size:
        raise ValueError(
            f"{zone_id.upper()} authoring image size mismatch.\n"
            f"Expected: {expected_size}\n"
            f"Actual:   {authoring.size}\n"
            f"File:     {authoring_path}"
        )

    authored_relief = extract_magenta_mask(
        authoring
    )

    painted_pixels = int(
        np.count_nonzero(
            authored_relief
        )
    )

    # Untouched regional copies are intentionally allowed to
    # remain in relief_authoring_new.
    #
    # If there is no magenta paint, this zone simply has not
    # been authored yet.

    if painted_pixels == 0:
        print(
            "  SKIPPED: no #FF00FF authoring paint detected."
        )

        return "unpainted"

    land_mask_path = find_land_mask(
        zone_id
    )

    if land_mask_path is None:
        raise FileNotFoundError(
            f"Missing regional land mask for {zone_id}."
        )

    land_image = Image.open(
        land_mask_path
    ).convert(
        "L"
    )

    if land_image.size != expected_size:
        raise ValueError(
            f"{zone_id.upper()} land-mask size mismatch.\n"
            f"Expected: {expected_size}\n"
            f"Actual:   {land_image.size}\n"
            f"File:     {land_mask_path}"
        )

    canonical_land = (
        np.asarray(
            land_image,
            dtype=np.uint8
        )
        >= 128
    )

    outside_land = (
        authored_relief
        &
        ~canonical_land
    )

    outside_count = int(
        np.count_nonzero(
            outside_land
        )
    )

    authored_relief &= canonical_land

    valid_relief_pixels = int(
        np.count_nonzero(
            authored_relief
        )
    )

    canonical_land_pixels = int(
        np.count_nonzero(
            canonical_land
        )
    )

    coverage_percentage = (
        valid_relief_pixels
        / canonical_land_pixels
        * 100.0
        if canonical_land_pixels > 0
        else 0.0
    )

    encoded = np.where(
        authored_relief,
        255,
        0
    ).astype(
        np.uint8
    )

    output_path = (
        OUTPUT_DIR
        / f"{zone_id}_relief_mask.png"
    )

    Image.fromarray(
        encoded,
        mode="L"
    ).save(
        output_path
    )

    print(
        f"  Source: {authoring_path}"
    )

    print(
        f"  Resolution: "
        f"{authoring.width} x {authoring.height}"
    )

    print(
        f"  Painted magenta pixels: "
        f"{painted_pixels:,}"
    )

    if outside_count > 0:
        print(
            f"  Removed outside canonical land: "
            f"{outside_count:,}"
        )

    print(
        f"  Valid relief pixels: "
        f"{valid_relief_pixels:,}"
    )

    print(
        f"  Relief coverage: "
        f"{coverage_percentage:.2f}% of canonical land"
    )

    print(
        f"  Output: {output_path}"
    )

    return "processed"


def main():
    print(
        "Known World manually authored relief extractor"
    )

    print()
    print(
        f"Authoring directory:\n{AUTHORING_DIR}"
    )

    print()
    print(
        "Authoring color:"
    )

    print(
        "  #FF00FF / RGB(255, 0, 255)"
    )

    processed = 0
    unpainted = 0
    missing = 0
    failed = 0

    for zone_id in ZONE_ORDER:
        print()
        print(
            f"{zone_id.upper()} - "
            f"{ZONES[zone_id].name}"
        )

        try:
            result = process_zone(
                zone_id
            )

            if result == "processed":
                processed += 1

            elif result == "unpainted":
                unpainted += 1

            elif result == "missing":
                missing += 1

        except Exception as error:
            failed += 1

            print(
                f"  ERROR: "
                f"{type(error).__name__}: "
                f"{error}"
            )

    print()
    print(
        "=" * 70
    )

    print(
        f"Processed zones:  {processed}"
    )

    print(
        f"Unpainted zones:  {unpainted}"
    )

    print(
        f"Missing zones:    {missing}"
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

from pathlib import Path

import numpy as np
from PIL import Image

from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

AUTHORING_DIR = (
    ROOT
    / "input"
    / "relief_authoring"
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


def validate_zone(zone_id):
    zone = ZONES[
        zone_id
    ]

    mask_path = (
        AUTHORING_DIR
        / f"{zone_id}_relief_mask.png"
    )

    if not mask_path.exists():
        raise FileNotFoundError(
            f"Missing authored relief mask: {mask_path}"
        )

    land_path = find_land_mask(
        zone_id
    )

    if land_path is None:
        raise FileNotFoundError(
            f"Missing land mask for {zone_id}."
        )

    authored = Image.open(
        mask_path
    ).convert(
        "L"
    )

    land = Image.open(
        land_path
    ).convert(
        "L"
    )

    expected_size = (
        zone.width,
        zone.height
    )

    if authored.size != expected_size:
        raise ValueError(
            f"{zone_id.upper()} authored mask size mismatch.\n"
            f"Expected: {expected_size}\n"
            f"Actual:   {authored.size}"
        )

    if land.size != expected_size:
        raise ValueError(
            f"{zone_id.upper()} land mask size mismatch."
        )

    authored_array = np.asarray(
        authored,
        dtype=np.uint8
    )

    land_array = np.asarray(
        land,
        dtype=np.uint8
    )

    relief = (
        authored_array >= 128
    )

    canonical_land = (
        land_array >= 128
    )

    outside_land = (
        relief
        &
        ~canonical_land
    )

    outside_count = int(
        np.count_nonzero(
            outside_land
        )
    )

    if outside_count > 0:
        print(
            f"  WARNING: "
            f"{outside_count:,} authored relief pixels "
            f"fall outside canonical land and will be removed."
        )

    relief &= canonical_land

    encoded = np.where(
        relief,
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

    relief_count = int(
        np.count_nonzero(
            relief
        )
    )

    land_count = int(
        np.count_nonzero(
            canonical_land
        )
    )

    percentage = (
        relief_count
        / land_count
        * 100.0
        if land_count > 0
        else 0.0
    )

    print(
        f"  Valid relief pixels: "
        f"{relief_count:,} "
        f"({percentage:.2f}% of land)"
    )

    print(
        f"  Output: {output_path}"
    )


def main():
    print(
        "Known World authored-relief validator"
    )

    successful = 0
    failed = 0

    for zone_id in ZONE_ORDER:
        print()
        print(
            f"{zone_id.upper()} - "
            f"{ZONES[zone_id].name}"
        )

        try:
            validate_zone(
                zone_id
            )

            successful += 1

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
        f"Successful zones: {successful}"
    )

    print(
        f"Failed zones:     {failed}"
    )


if __name__ == "__main__":
    main()

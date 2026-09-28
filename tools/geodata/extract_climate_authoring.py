from pathlib import Path

import numpy as np
from PIL import Image

from climate_palette import (
    CLIMATE_CATEGORIES,
    color_for_code,
)
from zone_transforms import (
    ZONES,
    ZONE_ORDER,
)


ROOT = Path(__file__).resolve().parent

AUTHORING_DIR = (
    ROOT
    / "input"
    / "climate_authoring"
)

OUTPUT_DIR = (
    ROOT
    / "output"
    / "validated_climate_masks"
)

PREVIEW_DIR = (
    ROOT
    / "output"
    / "validated_climate_previews"
)


# Allows tiny RGB deviations while remaining strict enough that
# ordinary map artwork should not be interpreted as climate paint.
COLOR_TOLERANCE = 4


def classify_pixels(
    image
):
    rgb = np.asarray(
        image.convert(
            "RGB"
        ),
        dtype=np.int16,
    )

    height, width, _ = (
        rgb.shape
    )

    codes = np.zeros(
        (
            height,
            width,
        ),
        dtype=np.uint8,
    )

    best_distance = np.full(
        (
            height,
            width,
        ),
        1_000_000,
        dtype=np.int32,
    )

    for category in CLIMATE_CATEGORIES:
        target = np.array(
            category.color,
            dtype=np.int16,
        )

        delta = (
            rgb
            - target[
                None,
                None,
                :
            ]
        )

        distance = np.sum(
            delta
            * delta,
            axis=2,
        )

        maximum_distance = (
            COLOR_TOLERANCE
            * COLOR_TOLERANCE
            * 3
        )

        matches = (
            distance
            <= maximum_distance
        )

        better = (
            matches
            &
            (
                distance
                < best_distance
            )
        )

        codes[
            better
        ] = category.code

        best_distance[
            better
        ] = distance[
            better
        ]

    return codes


def create_preview(
    codes
):
    height, width = (
        codes.shape
    )

    preview = np.zeros(
        (
            height,
            width,
            3,
        ),
        dtype=np.uint8,
    )

    for category in CLIMATE_CATEGORIES:
        preview[
            codes
            == category.code
        ] = category.color

    return Image.fromarray(
        preview,
        mode="RGB",
    )


def process_zone(
    zone_id
):
    zone = ZONES[
        zone_id
    ]

    source_path = (
        AUTHORING_DIR
        / f"{zone_id}.png"
    )

    if not source_path.exists():
        print(
            "  MISSING"
        )

        return "missing"

    image = Image.open(
        source_path
    ).convert(
        "RGB"
    )

    expected_size = (
        zone.width,
        zone.height,
    )

    if image.size != expected_size:
        raise ValueError(
            f"{zone_id.upper()} climate image size mismatch.\n"
            f"Expected: {expected_size}\n"
            f"Actual:   {image.size}"
        )

    codes = classify_pixels(
        image
    )

    authored_pixels = int(
        np.count_nonzero(
            codes
        )
    )

    if authored_pixels == 0:
        print(
            "  UNPAINTED"
        )

        return "unpainted"

    output_path = (
        OUTPUT_DIR
        / f"{zone_id}_climate.png"
    )

    Image.fromarray(
        codes,
        mode="L",
    ).save(
        output_path
    )

    preview = create_preview(
        codes
    )

    preview_path = (
        PREVIEW_DIR
        / f"{zone_id}_climate_preview.png"
    )

    preview.save(
        preview_path
    )

    print(
        f"  Painted pixels: {authored_pixels:,}"
    )

    for category in CLIMATE_CATEGORIES:
        count = int(
            np.count_nonzero(
                codes
                == category.code
            )
        )

        if count > 0:
            print(
                f"    "
                f"{category.name:<18} "
                f"{count:,}"
            )

    print(
        f"  Output: {output_path}"
    )

    return "processed"


def main():
    print(
        "Known World climate authoring extractor"
    )

    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    PREVIEW_DIR.mkdir(
        parents=True,
        exist_ok=True
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
        f"Processed: {processed}"
    )

    print(
        f"Unpainted: {unpainted}"
    )

    print(
        f"Missing:   {missing}"
    )

    print(
        f"Failed:    {failed}"
    )


if __name__ == "__main__":
    main()

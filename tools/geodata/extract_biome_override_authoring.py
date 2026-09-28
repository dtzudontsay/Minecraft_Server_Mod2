from pathlib import Path

import numpy as np
from PIL import Image

from biome_override_palette import BIOME_OVERRIDES
from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

REGION_DIR = (
    ROOT
    / "input"
    / "regions"
)

AUTHORING_DIR = (
    ROOT
    / "input"
    / "biome_override_authoring"
)

OUTPUT_DIR = (
    ROOT
    / "output"
    / "validated_biome_override_masks"
)

PREVIEW_DIR = (
    ROOT
    / "output"
    / "validated_biome_override_previews"
)


COLOR_TOLERANCE = 4


def find_region_file(
    zone_id
):
    direct_candidates = (
        REGION_DIR / f"{zone_id}.png",
        REGION_DIR / f"{zone_id.upper()}.png",
        REGION_DIR / f"{zone_id}.jpg",
        REGION_DIR / f"{zone_id.upper()}.jpg",
        REGION_DIR / f"{zone_id}.jpeg",
        REGION_DIR / f"{zone_id.upper()}.jpeg",
    )

    for candidate in direct_candidates:
        if candidate.exists():
            return candidate

    zone = ZONES[
        zone_id
    ]

    matching = []

    for candidate in REGION_DIR.iterdir():

        if not candidate.is_file():
            continue

        if candidate.suffix.lower() not in (
            ".png",
            ".jpg",
            ".jpeg",
        ):
            continue

        try:
            with Image.open(
                candidate
            ) as image:

                if image.size == (
                    zone.width,
                    zone.height,
                ):
                    matching.append(
                        candidate
                    )

        except Exception:
            pass

    if len(
        matching
    ) == 1:
        return matching[
            0
        ]

    if len(
        matching
    ) > 1:
        raise RuntimeError(
            f"Multiple regional source images match "
            f"{zone_id.upper()} resolution "
            f"{zone.width}x{zone.height}: "
            f"{matching}"
        )

    return None


def classify_pixels(
    authored,
    original
):
    authored_rgb = np.asarray(
        authored.convert(
            "RGB"
        ),
        dtype=np.int32,
    )

    original_rgb = np.asarray(
        original.convert(
            "RGB"
        ),
        dtype=np.int32,
    )

    height, width, _ = (
        authored_rgb.shape
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

    # Only pixels that differ from the original source map can
    # become biome overrides.
    #
    # This prevents colors already present in the map artwork from
    # accidentally being interpreted as biome override paint.
    changed = (
        np.max(
            np.abs(
                authored_rgb
                - original_rgb
            ),
            axis=2,
        )
        > COLOR_TOLERANCE
    )

    maximum_distance = (
        COLOR_TOLERANCE
        * COLOR_TOLERANCE
        * 3
    )

    for biome in BIOME_OVERRIDES:

        target = np.array(
            biome.color,
            dtype=np.int32,
        )

        delta = (
            authored_rgb
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

        matches = (
            changed
            &
            (
                distance
                <= maximum_distance
            )
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
        ] = biome.code

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

    for biome in BIOME_OVERRIDES:

        preview[
            codes
            == biome.code
        ] = biome.color

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

    authored_path = (
        AUTHORING_DIR
        / f"{zone_id}.png"
    )

    if not authored_path.exists():

        print(
            "  MISSING"
        )

        return "missing"

    original_path = find_region_file(
        zone_id
    )

    if original_path is None:

        raise FileNotFoundError(
            f"Unable to locate original regional map "
            f"for {zone_id.upper()}"
        )

    with Image.open(
        authored_path
    ) as authored_image:

        authored = authored_image.convert(
            "RGB"
        )

    with Image.open(
        original_path
    ) as original_image:

        original = original_image.convert(
            "RGB"
        )

    expected_size = (
        zone.width,
        zone.height,
    )

    if authored.size != expected_size:

        raise ValueError(
            f"{zone_id.upper()} override map "
            f"resolution mismatch.\n"
            f"Expected: "
            f"{expected_size[0]} x "
            f"{expected_size[1]}\n"
            f"Actual:   "
            f"{authored.width} x "
            f"{authored.height}"
        )

    if original.size != expected_size:

        raise ValueError(
            f"{zone_id.upper()} original map "
            f"resolution mismatch.\n"
            f"Expected: "
            f"{expected_size[0]} x "
            f"{expected_size[1]}\n"
            f"Actual:   "
            f"{original.width} x "
            f"{original.height}"
        )

    codes = classify_pixels(
        authored,
        original,
    )

    painted_pixels = int(
        np.count_nonzero(
            codes
        )
    )

    if painted_pixels == 0:

        print(
            "  No biome overrides painted"
        )

        return "unpainted"

    output_path = (
        OUTPUT_DIR
        / f"{zone_id}_biome_override.png"
    )

    Image.fromarray(
        codes,
        mode="L",
    ).save(
        output_path
    )

    preview_path = (
        PREVIEW_DIR
        / f"{zone_id}_biome_override_preview.png"
    )

    create_preview(
        codes
    ).save(
        preview_path
    )

    print(
        f"  Override pixels: "
        f"{painted_pixels:,}"
    )

    for biome in BIOME_OVERRIDES:

        count = int(
            np.count_nonzero(
                codes
                == biome.code
            )
        )

        if count > 0:

            print(
                f"    "
                f"{biome.biome_id:<42} "
                f"{count:,}"
            )

    print(
        f"  Output: "
        f"{output_path}"
    )

    print(
        f"  Preview: "
        f"{preview_path}"
    )

    return "processed"


def main():

    print(
        "Known World exact biome override extractor"
    )

    OUTPUT_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    PREVIEW_DIR.mkdir(
        parents=True,
        exist_ok=True,
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
        "BIOME OVERRIDE EXTRACTION COMPLETE"
    )

    print()

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

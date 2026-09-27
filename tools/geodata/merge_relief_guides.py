from pathlib import Path

import numpy as np
from PIL import Image

from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

INPUT_DIR = (
    ROOT
    / "output"
    / "regional_relief_guides"
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

MASTER_RELIEF_PATH = (
    OUTPUT_DIR
    / "known_world_relief_guide.png"
)

MASTER_OVERLAY_PATH = (
    OUTPUT_DIR
    / "known_world_relief_overlay.png"
)

MASTER_COVERAGE_PATH = (
    OUTPUT_DIR
    / "known_world_relief_coverage.png"
)

MASTER_WEAK_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_relief_weak_mask.png"
)

MASTER_STRONG_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_relief_strong_mask.png"
)


LOGICAL_WIDTH = 2048
LOGICAL_HEIGHT = 1357

EXPECTED_MASTER_WIDTH = 9050
EXPECTED_MASTER_HEIGHT = 6000

WEAK_RELIEF_THRESHOLD = 0.10
STRONG_RELIEF_THRESHOLD = 0.50


def master_logical_to_actual(
    master_x,
    master_y
):
    x = (
        master_x
        / LOGICAL_WIDTH
        * EXPECTED_MASTER_WIDTH
    )

    y = (
        master_y
        / LOGICAL_HEIGHT
        * EXPECTED_MASTER_HEIGHT
    )

    return x, y


def load_relief(
    zone_id
):
    path = (
        INPUT_DIR
        / f"{zone_id}_relief_guide.png"
    )

    if not path.exists():
        raise FileNotFoundError(
            f"Missing regional relief guide:\n{path}"
        )

    return Image.open(
        path
    ).convert(
        "L"
    )


def splat_zone(
    zone_id,
    master_relief,
    master_coverage
):
    zone = ZONES[
        zone_id
    ]

    source_image = load_relief(
        zone_id
    )

    if source_image.size != (
        zone.width,
        zone.height
    ):
        raise ValueError(
            f"{zone_id.upper()} relief guide dimensions do not "
            "match calibrated zone dimensions."
        )

    source = (
        np.asarray(
            source_image,
            dtype=np.float32
        )
        / 255.0
    )

    source_y, source_x = np.nonzero(
        source > 0.0
    )

    source_values = source[
        source_y,
        source_x
    ]

    for (
        regional_x,
        regional_y,
        value
    ) in zip(
        source_x,
        source_y,
        source_values
    ):
        logical_x, logical_y = (
            zone.regional_to_master(
                float(
                    regional_x
                ),
                float(
                    regional_y
                )
            )
        )

        master_x, master_y = (
            master_logical_to_actual(
                logical_x,
                logical_y
            )
        )

        x = int(
            round(
                master_x
            )
        )

        y = int(
            round(
                master_y
            )
        )

        if not (
            0 <= x < EXPECTED_MASTER_WIDTH
            and
            0 <= y < EXPECTED_MASTER_HEIGHT
        ):
            continue

        if value > master_relief[
            y,
            x
        ]:
            master_relief[
                y,
                x
            ] = value

        master_coverage[
            y,
            x
        ] = 1.0


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
        source * (
            1.0 - alpha
        )
        + tint * alpha
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


def save_binary_mask(
    relief,
    threshold,
    path
):
    encoded = np.where(
        relief >= threshold,
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


def main():
    print(
        "Known World regional-relief merger"
    )

    artwork = Image.open(
        MASTER_ART_PATH
    ).convert(
        "RGB"
    )

    if artwork.size != (
        EXPECTED_MASTER_WIDTH,
        EXPECTED_MASTER_HEIGHT
    ):
        raise ValueError(
            "Unexpected master artwork dimensions.\n"
            f"Expected: "
            f"{EXPECTED_MASTER_WIDTH} x "
            f"{EXPECTED_MASTER_HEIGHT}\n"
            f"Actual: {artwork.width} x {artwork.height}"
        )

    master_relief = np.zeros(
        (
            EXPECTED_MASTER_HEIGHT,
            EXPECTED_MASTER_WIDTH
        ),
        dtype=np.float32
    )

    master_coverage = np.zeros(
        (
            EXPECTED_MASTER_HEIGHT,
            EXPECTED_MASTER_WIDTH
        ),
        dtype=np.float32
    )

    for zone_id in ZONE_ORDER:
        print(
            f"Merging {zone_id.upper()}..."
        )

        splat_zone(
            zone_id,
            master_relief,
            master_coverage
        )

    relief_encoded = np.clip(
        master_relief * 255.0,
        0.0,
        255.0
    ).astype(
        np.uint8
    )

    coverage_encoded = np.where(
        master_coverage > 0.0,
        255,
        0
    ).astype(
        np.uint8
    )

    Image.fromarray(
        relief_encoded,
        mode="L"
    ).save(
        MASTER_RELIEF_PATH
    )

    Image.fromarray(
        coverage_encoded,
        mode="L"
    ).save(
        MASTER_COVERAGE_PATH
    )

    save_binary_mask(
        master_relief,
        WEAK_RELIEF_THRESHOLD,
        MASTER_WEAK_MASK_PATH
    )

    save_binary_mask(
        master_relief,
        STRONG_RELIEF_THRESHOLD,
        MASTER_STRONG_MASK_PATH
    )

    overlay = build_overlay(
        artwork,
        master_relief
    )

    overlay.save(
        MASTER_OVERLAY_PATH
    )

    weak_count = int(
        np.count_nonzero(
            master_relief
            >= WEAK_RELIEF_THRESHOLD
        )
    )

    strong_count = int(
        np.count_nonzero(
            master_relief
            >= STRONG_RELIEF_THRESHOLD
        )
    )

    print()
    print(
        "Generated:"
    )

    print(
        MASTER_RELIEF_PATH
    )

    print(
        MASTER_OVERLAY_PATH
    )

    print(
        MASTER_COVERAGE_PATH
    )

    print(
        MASTER_WEAK_MASK_PATH
    )

    print(
        MASTER_STRONG_MASK_PATH
    )

    print()
    print(
        f"Weak-relief master pixels: "
        f"{weak_count:,}"
    )

    print(
        f"Strong-relief master pixels: "
        f"{strong_count:,}"
    )


if __name__ == "__main__":
    main()

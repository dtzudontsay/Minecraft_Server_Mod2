from pathlib import Path
import json

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parent

OUTPUT_DIR = (
    ROOT
    / "output"
)

RUNTIME_GEODATA_DIR = (
    ROOT.parent.parent
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "knownworld"
    / "geodata"
)

CANONICAL_LAND_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_canonical_land_mask.png"
)

MASTER_ART_PATH = (
    ROOT
    / "input"
    / "master"
    / "known_world_master.jpg"
)

COAST_DISTANCE_PATH = (
    RUNTIME_GEODATA_DIR
    / "coast_distance.png"
)

GEODATA_JSON_PATH = (
    RUNTIME_GEODATA_DIR
    / "geodata.json"
)

MACRO_ELEVATION_PATH = (
    OUTPUT_DIR
    / "known_world_macro_elevation.png"
)

MACRO_ELEVATION_OVERLAY_PATH = (
    OUTPUT_DIR
    / "known_world_macro_elevation_overlay.png"
)


MIN_STORED_ELEVATION_METRES = -64.0
MAX_STORED_ELEVATION_METRES = 2048.0

SEA_LEVEL_METRES = 63.0

COASTAL_LAND_ELEVATION_METRES = 66.0
INLAND_BASE_ELEVATION_METRES = 105.0

SHALLOW_OCEAN_FLOOR_METRES = 52.0
DEEP_OCEAN_FLOOR_METRES = 34.0

LAND_TRANSITION_DISTANCE_METRES = 120_000.0
OCEAN_TRANSITION_DISTANCE_METRES = 150_000.0


def load_json(path):
    if not path.exists():
        raise FileNotFoundError(
            f"Missing JSON file:\n{path}"
        )

    with path.open(
        "r",
        encoding="utf-8"
    ) as handle:
        return json.load(
            handle
        )


def load_binary_mask(path):
    if not path.exists():
        raise FileNotFoundError(
            f"Missing mask:\n{path}"
        )

    image = Image.open(
        path
    ).convert(
        "L"
    )

    return (
        np.asarray(
            image,
            dtype=np.uint8
        )
        >= 128
    )


def load_coast_distance(path):
    if not path.exists():
        raise FileNotFoundError(
            f"Missing coast-distance raster:\n{path}"
        )

    image = Image.open(
        path
    ).convert(
        "L"
    )

    return np.asarray(
        image,
        dtype=np.uint8
    )


def smoothstep(values):
    values = np.clip(
        values,
        0.0,
        1.0
    )

    return (
        values
        * values
        * (
            3.0
            - 2.0
            * values
        )
    )


def decode_coast_distance_metres(
    raw,
    metres_per_pixel
):
    signed_pixels = (
        raw.astype(
            np.float32
        )
        - 128.0
    )

    magnitude_pixels = np.maximum(
        np.abs(
            signed_pixels
        )
        - 0.5,
        0.0
    )

    magnitude_metres = (
        magnitude_pixels
        * metres_per_pixel
    )

    return np.where(
        signed_pixels >= 0.0,
        magnitude_metres,
        -magnitude_metres
    ).astype(
        np.float32
    )


def build_land_base(
    coast_distance_metres
):
    inland_distance = np.maximum(
        coast_distance_metres,
        0.0
    )

    factor = np.clip(
        inland_distance
        / LAND_TRANSITION_DISTANCE_METRES,
        0.0,
        1.0
    )

    factor = smoothstep(
        factor
    )

    return (
        COASTAL_LAND_ELEVATION_METRES
        +
        (
            INLAND_BASE_ELEVATION_METRES
            - COASTAL_LAND_ELEVATION_METRES
        )
        * factor
    ).astype(
        np.float32
    )


def build_ocean_base(
    coast_distance_metres
):
    offshore_distance = np.maximum(
        -coast_distance_metres,
        0.0
    )

    factor = np.clip(
        offshore_distance
        / OCEAN_TRANSITION_DISTANCE_METRES,
        0.0,
        1.0
    )

    factor = smoothstep(
        factor
    )

    return (
        SHALLOW_OCEAN_FLOOR_METRES
        +
        (
            DEEP_OCEAN_FLOOR_METRES
            - SHALLOW_OCEAN_FLOOR_METRES
        )
        * factor
    ).astype(
        np.float32
    )


def build_macro_elevation(
    canonical_land,
    coast_distance_metres
):
    land_base = build_land_base(
        coast_distance_metres
    )

    ocean_base = build_ocean_base(
        coast_distance_metres
    )

    elevation = ocean_base.copy()

    elevation[
        canonical_land
    ] = land_base[
        canonical_land
    ]

    return elevation.astype(
        np.float32
    )


def encode_runtime_elevation(
    elevation
):
    factor = (
        elevation
        - MIN_STORED_ELEVATION_METRES
    ) / (
        MAX_STORED_ELEVATION_METRES
        - MIN_STORED_ELEVATION_METRES
    )

    factor = np.clip(
        factor,
        0.0,
        1.0
    )

    encoded = (
        1.0
        + factor
        * 65534.0
    )

    return np.round(
        encoded
    ).astype(
        np.uint16
    )


def build_overlay(
    artwork,
    canonical_land,
    elevation
):
    source = np.asarray(
        artwork.convert("RGB"),
        dtype=np.float32
    )

    output = source.copy()

    land_factor = np.clip(
        (
            elevation
            - COASTAL_LAND_ELEVATION_METRES
        )
        / (
            INLAND_BASE_ELEVATION_METRES
            - COASTAL_LAND_ELEVATION_METRES
        ),
        0.0,
        1.0
    )

    tint = np.zeros_like(
        source
    )

    tint[:, :, 0] = 255.0
    tint[:, :, 1] = 185.0
    tint[:, :, 2] = 0.0

    alpha = (
        (
            0.08
            + land_factor * 0.22
        )[
            :,
            :,
            None
        ]
        * canonical_land[
            :,
            :,
            None
        ].astype(
            np.float32
        )
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
            0,
            255
        ).astype(
            np.uint8
        ),
        mode="RGB"
    )


def main():
    print(
        "Known World macro BASE elevation builder"
    )

    metadata = load_json(
        GEODATA_JSON_PATH
    )

    width = int(
        metadata[
            "width"
        ]
    )

    height = int(
        metadata[
            "height"
        ]
    )

    metres_x = float(
        metadata[
            "metresPerSourcePixelX"
        ]
    )

    metres_z = float(
        metadata[
            "metresPerSourcePixelZ"
        ]
    )

    average_metres_per_pixel = (
        metres_x
        + metres_z
    ) * 0.5

    canonical_land = load_binary_mask(
        CANONICAL_LAND_MASK_PATH
    )

    coast_raw = load_coast_distance(
        COAST_DISTANCE_PATH
    )

    expected_shape = (
        height,
        width
    )

    if canonical_land.shape != expected_shape:
        raise ValueError(
            f"Land mask mismatch: {canonical_land.shape}"
        )

    if coast_raw.shape != expected_shape:
        raise ValueError(
            f"Coast raster mismatch: {coast_raw.shape}"
        )

    artwork = Image.open(
        MASTER_ART_PATH
    ).convert(
        "RGB"
    )

    if artwork.size != (
        width,
        height
    ):
        raise ValueError(
            f"Master artwork mismatch: {artwork.size}"
        )

    coast_distance_metres = (
        decode_coast_distance_metres(
            coast_raw,
            average_metres_per_pixel
        )
    )

    elevation = build_macro_elevation(
        canonical_land,
        coast_distance_metres
    )

    encoded = encode_runtime_elevation(
        elevation
    )

    Image.fromarray(
        encoded,
        mode="I;16"
    ).save(
        MACRO_ELEVATION_PATH
    )

    overlay = build_overlay(
        artwork,
        canonical_land,
        elevation
    )

    overlay.save(
        MACRO_ELEVATION_OVERLAY_PATH
    )

    land_values = elevation[
        canonical_land
    ]

    ocean_values = elevation[
        ~canonical_land
    ]

    print()
    print(
        "=" * 70
    )

    print(
        "MACRO BASE ELEVATION COMPLETE"
    )

    print()

    print(
        f"Land minimum: "
        f"{float(np.min(land_values)):.1f} m"
    )

    print(
        f"Land mean: "
        f"{float(np.mean(land_values)):.1f} m"
    )

    print(
        f"Land maximum: "
        f"{float(np.max(land_values)):.1f} m"
    )

    print()

    print(
        f"Ocean minimum: "
        f"{float(np.min(ocean_values)):.1f} m"
    )

    print(
        f"Ocean maximum: "
        f"{float(np.max(ocean_values)):.1f} m"
    )

    print()

    print(
        "Generated:"
    )

    print(
        MACRO_ELEVATION_PATH
    )

    print(
        MACRO_ELEVATION_OVERLAY_PATH
    )

    print()

    print(
        "IMPORTANT:"
    )

    print(
        "This raster contains NO mountain geometry."
    )

    print(
        "Mountains are now generated at Minecraft block scale."
    )


if __name__ == "__main__":
    main()

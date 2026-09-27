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

RELIEF_INTENSITY_PATH = (
    OUTPUT_DIR
    / "known_world_relief_intensity.png"
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


# ------------------------------------------------------------
# STORAGE FORMAT
# ------------------------------------------------------------
#
# This matches RasterElevationProvider.java exactly:
#
#   raw 0     = no-data
#   raw 1     = -64 metres
#   raw 65535 = +2048 metres
#
# ------------------------------------------------------------

MIN_STORED_ELEVATION_METRES = -64.0
MAX_STORED_ELEVATION_METRES = 2048.0


# ------------------------------------------------------------
# FIRST-PASS TERRAIN PARAMETERS
# ------------------------------------------------------------
#
# These are deliberately conservative.
#
# They are not claims about exact canonical elevations.
#
# This stage creates:
#
# - coastal-to-inland macro land rise
# - shallow-to-deep ocean floor
# - broad mountain/highland elevation from authored relief
#
# Rivers, erosion, local hills and small-scale terrain variation
# come later.
#
# ------------------------------------------------------------

SEA_LEVEL_METRES = 63.0

COASTAL_LAND_ELEVATION_METRES = 66.0
INLAND_BASE_ELEVATION_METRES = 105.0

SHALLOW_OCEAN_FLOOR_METRES = 52.0
DEEP_OCEAN_FLOOR_METRES = 34.0

LAND_BASE_TRANSITION_METRES = 120_000.0
OCEAN_BASE_TRANSITION_METRES = 150_000.0

MAX_RELIEF_ADDITION_METRES = 1_200.0

RELIEF_POWER = 2.0


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
            f"Missing required mask:\n{path}"
        )

    image = Image.open(
        path
    ).convert(
        "L"
    )

    values = np.asarray(
        image,
        dtype=np.uint8
    )

    return (
        values >= 128
    )


def load_relief_intensity(path):
    if not path.exists():
        raise FileNotFoundError(
            f"Missing relief intensity raster:\n{path}"
        )

    image = Image.open(
        path
    )

    values = np.asarray(
        image,
        dtype=np.uint16
    )

    return (
        values.astype(
            np.float32
        )
        / 65535.0
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
        / LAND_BASE_TRANSITION_METRES,
        0.0,
        1.0
    )

    factor = smoothstep(
        factor
    )

    return (
        COASTAL_LAND_ELEVATION_METRES
        + (
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
        / OCEAN_BASE_TRANSITION_METRES,
        0.0,
        1.0
    )

    factor = smoothstep(
        factor
    )

    return (
        SHALLOW_OCEAN_FLOOR_METRES
        + (
            DEEP_OCEAN_FLOOR_METRES
            - SHALLOW_OCEAN_FLOOR_METRES
        )
        * factor
    ).astype(
        np.float32
    )


def build_relief_addition(
    relief_intensity
):
    shaped = np.power(
        np.clip(
            relief_intensity,
            0.0,
            1.0
        ),
        RELIEF_POWER
    )

    return (
        shaped
        * MAX_RELIEF_ADDITION_METRES
    ).astype(
        np.float32
    )


def build_macro_elevation(
    canonical_land,
    coast_distance_metres,
    relief_intensity
):
    land_base = build_land_base(
        coast_distance_metres
    )

    ocean_base = build_ocean_base(
        coast_distance_metres
    )

    relief_addition = build_relief_addition(
        relief_intensity
    )

    elevation = ocean_base.copy()

    land_elevation = (
        land_base
        + relief_addition
    )

    land_elevation = np.maximum(
        land_elevation,
        SEA_LEVEL_METRES + 1.0
    )

    elevation[
        canonical_land
    ] = land_elevation[
        canonical_land
    ]

    elevation[
        ~canonical_land
    ] = np.minimum(
        elevation[
            ~canonical_land
        ],
        SEA_LEVEL_METRES - 1.0
    )

    return np.clip(
        elevation,
        MIN_STORED_ELEVATION_METRES,
        MAX_STORED_ELEVATION_METRES
    ).astype(
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


def save_runtime_encoded_elevation(
    elevation,
    path
):
    encoded = encode_runtime_elevation(
        elevation
    )

    Image.fromarray(
        encoded,
        mode="I;16"
    ).save(
        path
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

    land_elevation = np.maximum(
        elevation
        - SEA_LEVEL_METRES,
        0.0
    )

    land_strength = np.clip(
        land_elevation
        / 1_200.0,
        0.0,
        1.0
    )

    land_strength = np.sqrt(
        land_strength
    )

    land_tint = np.zeros_like(
        source
    )

    land_tint[:, :, 0] = 255.0
    land_tint[:, :, 1] = 105.0
    land_tint[:, :, 2] = 0.0

    land_alpha = (
        land_strength[
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
        * 0.72
    )

    output = (
        output
        * (
            1.0
            - land_alpha
        )
        +
        land_tint
        * land_alpha
    )

    ocean_depth = np.maximum(
        SEA_LEVEL_METRES
        - elevation,
        0.0
    )

    ocean_strength = np.clip(
        ocean_depth
        / 40.0,
        0.0,
        1.0
    )

    ocean_tint = np.zeros_like(
        source
    )

    ocean_tint[:, :, 0] = 0.0
    ocean_tint[:, :, 1] = 120.0
    ocean_tint[:, :, 2] = 255.0

    ocean_alpha = (
        ocean_strength[
            :,
            :,
            None
        ]
        * (
            ~canonical_land
        )[
            :,
            :,
            None
        ].astype(
            np.float32
        )
        * 0.38
    )

    output = (
        output
        * (
            1.0
            - ocean_alpha
        )
        +
        ocean_tint
        * ocean_alpha
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


def print_statistics(
    canonical_land,
    relief_intensity,
    elevation
):
    land_values = elevation[
        canonical_land
    ]

    ocean_values = elevation[
        ~canonical_land
    ]

    relief_land = (
        canonical_land
        &
        (
            relief_intensity > 0.01
        )
    )

    strong_relief_land = (
        canonical_land
        &
        (
            relief_intensity >= 0.75
        )
    )

    print()
    print(
        "=" * 70
    )

    print(
        "MACRO ELEVATION PREVIEW COMPLETE"
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
        f"Ocean floor minimum: "
        f"{float(np.min(ocean_values)):.1f} m"
    )

    print(
        f"Ocean floor maximum: "
        f"{float(np.max(ocean_values)):.1f} m"
    )

    if np.any(
        relief_land
    ):
        relief_values = elevation[
            relief_land
        ]

        print()

        print(
            f"Relief-area mean elevation: "
            f"{float(np.mean(relief_values)):.1f} m"
        )

        print(
            f"Relief-area maximum elevation: "
            f"{float(np.max(relief_values)):.1f} m"
        )

    if np.any(
        strong_relief_land
    ):
        strong_values = elevation[
            strong_relief_land
        ]

        print(
            f"Strong-relief mean elevation: "
            f"{float(np.mean(strong_values)):.1f} m"
        )


def main():
    print(
        "Known World macro elevation preview builder"
    )

    metadata = load_json(
        GEODATA_JSON_PATH
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

    relief_intensity = load_relief_intensity(
        RELIEF_INTENSITY_PATH
    )

    coast_distance_raw = load_coast_distance(
        COAST_DISTANCE_PATH
    )

    expected_shape = (
        int(
            metadata[
                "height"
            ]
        ),
        int(
            metadata[
                "width"
            ]
        )
    )

    if canonical_land.shape != expected_shape:
        raise ValueError(
            "Canonical land mask dimensions do not match "
            "geodata metadata.\n"
            f"Expected: {expected_shape}\n"
            f"Actual:   {canonical_land.shape}"
        )

    if relief_intensity.shape != expected_shape:
        raise ValueError(
            "Relief intensity dimensions do not match "
            "geodata metadata.\n"
            f"Expected: {expected_shape}\n"
            f"Actual:   {relief_intensity.shape}"
        )

    if coast_distance_raw.shape != expected_shape:
        raise ValueError(
            "Coast-distance dimensions do not match "
            "geodata metadata.\n"
            f"Expected: {expected_shape}\n"
            f"Actual:   {coast_distance_raw.shape}"
        )

    artwork = Image.open(
        MASTER_ART_PATH
    ).convert(
        "RGB"
    )

    expected_size = (
        expected_shape[1],
        expected_shape[0]
    )

    if artwork.size != expected_size:
        raise ValueError(
            "Master artwork dimensions do not match "
            "geodata metadata.\n"
            f"Expected: {expected_size}\n"
            f"Actual:   {artwork.size}"
        )

    print(
        f"Master resolution: "
        f"{expected_size[0]} x {expected_size[1]}"
    )

    print(
        f"Metres per source pixel X: "
        f"{metres_x:.3f}"
    )

    print(
        f"Metres per source pixel Z: "
        f"{metres_z:.3f}"
    )

    print(
        f"Average metres per source pixel: "
        f"{average_metres_per_pixel:.3f}"
    )

    print()

    print(
        f"Land baseline: "
        f"{COASTAL_LAND_ELEVATION_METRES:.1f} m "
        f"to {INLAND_BASE_ELEVATION_METRES:.1f} m"
    )

    print(
        f"Maximum relief addition: "
        f"{MAX_RELIEF_ADDITION_METRES:.1f} m"
    )

    print(
        f"Ocean floor: "
        f"{SHALLOW_OCEAN_FLOOR_METRES:.1f} m "
        f"to {DEEP_OCEAN_FLOOR_METRES:.1f} m"
    )

    coast_distance_metres = (
        decode_coast_distance_metres(
            coast_distance_raw,
            average_metres_per_pixel
        )
    )

    elevation = build_macro_elevation(
        canonical_land,
        coast_distance_metres,
        relief_intensity
    )

    save_runtime_encoded_elevation(
        elevation,
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

    print_statistics(
        canonical_land,
        relief_intensity,
        elevation
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
        "NOTE:"
    )

    print(
        "This preview is encoded exactly like the future "
        "runtime elevation.png, but it has NOT been copied "
        "into the runtime resource directory."
    )


if __name__ == "__main__":
    main()

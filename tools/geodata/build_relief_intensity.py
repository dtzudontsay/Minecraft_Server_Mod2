from pathlib import Path
import json

import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt, gaussian_filter


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

RELIEF_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_authored_relief_mask.png"
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

GEODATA_JSON_PATH = (
    RUNTIME_GEODATA_DIR
    / "geodata.json"
)

RELIEF_INTENSITY_PATH = (
    OUTPUT_DIR
    / "known_world_relief_intensity.png"
)

RELIEF_INTENSITY_OVERLAY_PATH = (
    OUTPUT_DIR
    / "known_world_relief_intensity_overlay.png"
)


# ------------------------------------------------------------
# IMPORTANT
# ------------------------------------------------------------
#
# This script creates RELATIVE relief intensity.
#
# It does NOT claim actual elevation in metres.
#
# 0.0 = no influence from authored major relief
# 1.0 = deep interior of an authored major-relief system
#
# A small outward feather creates foothill transitions around
# authored relief footprints.
#
# ------------------------------------------------------------


FOOTHILL_DISTANCE_METRES = 35_000.0

INTERIOR_RISE_DISTANCE_METRES = 90_000.0

SMOOTHING_DISTANCE_METRES = 8_000.0


def load_binary_mask(
    path
):
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


def load_metres_per_pixel():
    if not GEODATA_JSON_PATH.exists():
        raise FileNotFoundError(
            f"Missing runtime geodata metadata:\n"
            f"{GEODATA_JSON_PATH}"
        )

    with GEODATA_JSON_PATH.open(
        "r",
        encoding="utf-8"
    ) as handle:
        metadata = json.load(
            handle
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

    return (
        metres_x,
        metres_z
    )


def smoothstep(
    values
):
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


def build_intensity(
    relief,
    canonical_land,
    metres_x,
    metres_z
):
    sampling = (
        metres_z,
        metres_x
    )

    inside_distance = distance_transform_edt(
        relief,
        sampling=sampling
    )

    outside_distance = distance_transform_edt(
        ~relief,
        sampling=sampling
    )

    intensity = np.zeros(
        relief.shape,
        dtype=np.float32
    )

    inside_factor = np.clip(
        inside_distance
        / INTERIOR_RISE_DISTANCE_METRES,
        0.0,
        1.0
    )

    inside_factor = smoothstep(
        inside_factor
    )

    # Authored relief begins above zero at the edge so that the
    # actual painted footprint already represents noticeable
    # elevated terrain.
    intensity[
        relief
    ] = (
        0.35
        + inside_factor[
            relief
        ]
        * 0.65
    )

    foothill_zone = (
        ~relief
        &
        canonical_land
        &
        (
            outside_distance
            <= FOOTHILL_DISTANCE_METRES
        )
    )

    foothill_factor = (
        1.0
        - (
            outside_distance
            / FOOTHILL_DISTANCE_METRES
        )
    )

    foothill_factor = smoothstep(
        foothill_factor
    )

    intensity[
        foothill_zone
    ] = (
        foothill_factor[
            foothill_zone
        ]
        * 0.35
    )

    average_metres_per_pixel = (
        metres_x
        + metres_z
    ) * 0.5

    smoothing_sigma = (
        SMOOTHING_DISTANCE_METRES
        / average_metres_per_pixel
    )

    if smoothing_sigma > 0.0:
        intensity = gaussian_filter(
            intensity,
            sigma=smoothing_sigma
        )

    intensity[
        ~canonical_land
    ] = 0.0

    return np.clip(
        intensity,
        0.0,
        1.0
    ).astype(
        np.float32
    )


def save_16_bit_intensity(
    intensity,
    path
):
    encoded = np.round(
        intensity
        * 65535.0
    ).astype(
        np.uint16
    )

    image = Image.fromarray(
        encoded,
        mode="I;16"
    )

    image.save(
        path
    )


def build_overlay(
    artwork,
    intensity
):
    source = np.asarray(
        artwork.convert("RGB"),
        dtype=np.float32
    )

    output = source.copy()

    # Cyan is deliberately chosen because it is easy to
    # distinguish from the original map artwork and from the
    # magenta authoring overlays used earlier.
    tint = np.zeros_like(
        source
    )

    tint[:, :, 0] = 0.0
    tint[:, :, 1] = 220.0
    tint[:, :, 2] = 255.0

    alpha = (
        intensity[
            :,
            :,
            None
        ]
        * 0.72
    )

    output = (
        source
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
            0.0,
            255.0
        ).astype(
            np.uint8
        ),
        mode="RGB"
    )


def main():
    print(
        "Known World continuous relief-intensity builder"
    )

    relief = load_binary_mask(
        RELIEF_MASK_PATH
    )

    canonical_land = load_binary_mask(
        CANONICAL_LAND_MASK_PATH
    )

    if relief.shape != canonical_land.shape:
        raise ValueError(
            "Relief mask and canonical land mask "
            "have different dimensions.\n"
            f"Relief: {relief.shape}\n"
            f"Land:   {canonical_land.shape}"
        )

    artwork = Image.open(
        MASTER_ART_PATH
    ).convert(
        "RGB"
    )

    expected_size = (
        relief.shape[1],
        relief.shape[0]
    )

    if artwork.size != expected_size:
        raise ValueError(
            "Master artwork dimensions do not match "
            "the relief raster.\n"
            f"Artwork: {artwork.size}\n"
            f"Raster:  {expected_size}"
        )

    metres_x, metres_z = (
        load_metres_per_pixel()
    )

    print(
        f"Master resolution: "
        f"{expected_size[0]} x {expected_size[1]}"
    )

    print(
        f"Metres per pixel X: "
        f"{metres_x:.3f}"
    )

    print(
        f"Metres per pixel Z: "
        f"{metres_z:.3f}"
    )

    print(
        f"Foothill transition: "
        f"{FOOTHILL_DISTANCE_METRES / 1000.0:.1f} km"
    )

    print(
        f"Interior rise distance: "
        f"{INTERIOR_RISE_DISTANCE_METRES / 1000.0:.1f} km"
    )

    intensity = build_intensity(
        relief,
        canonical_land,
        metres_x,
        metres_z
    )

    save_16_bit_intensity(
        intensity,
        RELIEF_INTENSITY_PATH
    )

    overlay = build_overlay(
        artwork,
        intensity
    )

    overlay.save(
        RELIEF_INTENSITY_OVERLAY_PATH
    )

    active = (
        intensity > 0.01
    )

    strong = (
        intensity >= 0.75
    )

    land_pixels = int(
        np.count_nonzero(
            canonical_land
        )
    )

    active_pixels = int(
        np.count_nonzero(
            active
        )
    )

    strong_pixels = int(
        np.count_nonzero(
            strong
        )
    )

    active_percentage = (
        active_pixels
        / land_pixels
        * 100.0
        if land_pixels > 0
        else 0.0
    )

    strong_percentage = (
        strong_pixels
        / land_pixels
        * 100.0
        if land_pixels > 0
        else 0.0
    )

    print()
    print(
        "=" * 70
    )

    print(
        "RELIEF INTENSITY COMPLETE"
    )

    print()

    print(
        f"Land pixels: "
        f"{land_pixels:,}"
    )

    print(
        f"Relief-influenced pixels: "
        f"{active_pixels:,} "
        f"({active_percentage:.2f}% of land)"
    )

    print(
        f"Strong relief pixels: "
        f"{strong_pixels:,} "
        f"({strong_percentage:.2f}% of land)"
    )

    print()

    print(
        "Generated:"
    )

    print(
        RELIEF_INTENSITY_PATH
    )

    print(
        RELIEF_INTENSITY_OVERLAY_PATH
    )


if __name__ == "__main__":
    main()

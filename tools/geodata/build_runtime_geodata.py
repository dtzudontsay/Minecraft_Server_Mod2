from pathlib import Path
import json
import shutil

import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt


ROOT = Path(__file__).resolve().parent

PROJECT_ROOT = ROOT.parent.parent

INPUT_MASK = (
    ROOT
    / "output"
    / "known_world_canonical_land_mask.png"
)

RUNTIME_DIR = (
    PROJECT_ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "knownworld"
    / "geodata"
)

RUNTIME_LAND_MASK = (
    RUNTIME_DIR
    / "land_mask.png"
)

RUNTIME_COAST_DISTANCE = (
    RUNTIME_DIR
    / "coast_distance.png"
)

RUNTIME_METADATA = (
    RUNTIME_DIR
    / "geodata.json"
)


# ------------------------------------------------------------
# CALIBRATION
# ------------------------------------------------------------

# The regional transforms and previous calibration use the
# logical master map coordinate system:
#
#     2048 x 1357
#
LOGICAL_WIDTH = 2048
LOGICAL_HEIGHT = 1357

# Current high-resolution canonical master raster.
EXPECTED_WIDTH = 9050
EXPECTED_HEIGHT = 6000

# Existing geographic calibration.
#
# Approximate real-world metres represented by one logical
# master pixel.
LOGICAL_METRES_PER_PIXEL = 6000.69

# Stored signed distance range.
#
# Runtime coast_distance.png:
#
#     128 = coastline
#     >128 = inland
#     <128 = offshore
#
# Values are clipped to +/- this many high-resolution pixels.
MAX_DISTANCE_PIXELS = 127


def load_canonical_mask():
    if not INPUT_MASK.exists():
        raise FileNotFoundError(
            "Canonical land mask does not exist:\n"
            f"{INPUT_MASK}\n\n"
            "Run finalize_land_mask.py first."
        )

    image = Image.open(
        INPUT_MASK
    ).convert(
        "L"
    )

    print(
        f"Canonical mask: "
        f"{image.width} x {image.height}"
    )

    if image.size != (
        EXPECTED_WIDTH,
        EXPECTED_HEIGHT
    ):
        raise ValueError(
            "Canonical mask dimensions are unexpected.\n"
            f"Expected: {EXPECTED_WIDTH} x {EXPECTED_HEIGHT}\n"
            f"Actual:   {image.width} x {image.height}\n\n"
            "Do not resize the mask. "
            "Stop here and check the source files."
        )

    return image


def make_binary_land_array(image):
    source = np.asarray(
        image,
        dtype=np.uint8
    )

    # True = land
    # False = water
    return source >= 128


def calculate_coast_distance(land):
    """
    Produce signed distance in source-raster pixels.

    Positive values = inland
    Negative values = offshore

    scipy distance_transform_edt gives the distance to the
    nearest zero-valued cell.
    """

    print()
    print(
        "Calculating inland distance..."
    )

    inland_distance = distance_transform_edt(
        land
    )

    inland_distance = np.minimum(
        inland_distance,
        MAX_DISTANCE_PIXELS
    )

    print(
        "Calculating offshore distance..."
    )

    offshore_distance = distance_transform_edt(
        ~land
    )

    offshore_distance = np.minimum(
        offshore_distance,
        MAX_DISTANCE_PIXELS
    )

    print(
        "Combining signed coastal distance..."
    )

    signed = (
        inland_distance
        - offshore_distance
    )

    # We only need integer pixel distance for runtime.
    signed = np.rint(
        signed
    ).astype(
        np.int16
    )

    signed = np.clip(
        signed,
        -MAX_DISTANCE_PIXELS,
        MAX_DISTANCE_PIXELS
    )

    return signed


def encode_distance_as_byte(signed):
    """
    Encode signed -127..+127 into unsigned 1..255.

        1   = far offshore
        128 = shoreline / transition
        255 = far inland
    """

    encoded = (
        signed.astype(np.int16)
        + 128
    )

    encoded = np.clip(
        encoded,
        1,
        255
    )

    return encoded.astype(
        np.uint8
    )


def save_runtime_land_mask(image):
    """
    Copy the canonical mask into the mod resources.

    Keep it binary:
        255 = land
        0   = water
    """

    binary = image.point(
        lambda value:
        255 if value >= 128 else 0
    )

    binary.save(
        RUNTIME_LAND_MASK,
        optimize=True
    )


def save_distance_map(encoded):
    image = Image.fromarray(
        encoded,
        mode="L"
    )

    image.save(
        RUNTIME_COAST_DISTANCE,
        optimize=True
    )


def build_metadata(width, height):
    metres_per_source_pixel_x = (
        LOGICAL_METRES_PER_PIXEL
        * LOGICAL_WIDTH
        / width
    )

    metres_per_source_pixel_z = (
        LOGICAL_METRES_PER_PIXEL
        * LOGICAL_HEIGHT
        / height
    )

    metadata = {
        "formatVersion": 1,

        "width": width,
        "height": height,

        "logicalMasterWidth": LOGICAL_WIDTH,
        "logicalMasterHeight": LOGICAL_HEIGHT,

        "logicalMetresPerPixel": LOGICAL_METRES_PER_PIXEL,

        "metresPerSourcePixelX": metres_per_source_pixel_x,
        "metresPerSourcePixelZ": metres_per_source_pixel_z,

        "landMask": {
            "file": "land_mask.png",
            "waterValue": 0,
            "landValue": 255,
        },

        "coastDistance": {
            "file": "coast_distance.png",
            "encoding": "signed_byte_offset_128",
            "zeroValue": 128,
            "minimumStoredDistancePixels": -127,
            "maximumStoredDistancePixels": 127,
        },

        "coordinateConvention": {
            "imageX": "west_to_east",
            "imageY": "north_to_south",
            "minecraftX": "west_to_east",
            "minecraftZ": "north_to_south",
        },
    }

    with open(
        RUNTIME_METADATA,
        "w",
        encoding="utf-8"
    ) as file:
        json.dump(
            metadata,
            file,
            indent=2
        )

        file.write("\n")

    return metadata


def print_stats(land, signed, metadata):
    total = land.size

    land_pixels = int(
        np.count_nonzero(
            land
        )
    )

    water_pixels = (
        total - land_pixels
    )

    print()
    print(
        "Runtime geodata statistics:"
    )

    print(
        f"  Pixels: "
        f"{total:,}"
    )

    print(
        f"  Land: "
        f"{land_pixels:,} "
        f"({land_pixels / total:.2%})"
    )

    print(
        f"  Water: "
        f"{water_pixels:,} "
        f"({water_pixels / total:.2%})"
    )

    print(
        f"  Distance range: "
        f"{signed.min()} .. {signed.max()} source pixels"
    )

    print(
        f"  Metres/source pixel X: "
        f"{metadata['metresPerSourcePixelX']:.3f}"
    )

    print(
        f"  Metres/source pixel Z: "
        f"{metadata['metresPerSourcePixelZ']:.3f}"
    )

    print(
        f"  Maximum stored coastal distance X: "
        f"{metadata['metresPerSourcePixelX'] * MAX_DISTANCE_PIXELS / 1000:.1f} km"
    )

    print(
        f"  Maximum stored coastal distance Z: "
        f"{metadata['metresPerSourcePixelZ'] * MAX_DISTANCE_PIXELS / 1000:.1f} km"
    )


def main():
    print(
        "Known World runtime-geodata builder"
    )

    RUNTIME_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    canonical = load_canonical_mask()

    land = make_binary_land_array(
        canonical
    )

    signed = calculate_coast_distance(
        land
    )

    encoded = encode_distance_as_byte(
        signed
    )

    print()
    print(
        "Writing runtime assets..."
    )

    save_runtime_land_mask(
        canonical
    )

    save_distance_map(
        encoded
    )

    metadata = build_metadata(
        canonical.width,
        canonical.height
    )

    print_stats(
        land,
        signed,
        metadata
    )

    print()
    print(
        "Generated:"
    )

    print(
        RUNTIME_LAND_MASK
    )

    print(
        RUNTIME_COAST_DISTANCE
    )

    print(
        RUNTIME_METADATA
    )

    print()
    print(
        "Encoding:"
    )

    print(
        "  land_mask.png:"
    )

    print(
        "    BLACK = water"
    )

    print(
        "    WHITE = land"
    )

    print()
    print(
        "  coast_distance.png:"
    )

    print(
        "    < 128 = offshore"
    )

    print(
        "      128 = coastline"
    )

    print(
        "    > 128 = inland"
    )


if __name__ == "__main__":
    main()

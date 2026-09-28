from pathlib import Path
import json

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parent

SOURCE_PATH = (
    ROOT
    / "output"
    / "known_world_relief_intensity.png"
)

RUNTIME_DIR = (
    ROOT.parent.parent
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "knownworld"
    / "geodata"
)

TARGET_PATH = (
    RUNTIME_DIR
    / "relief_intensity.png"
)

GEODATA_JSON_PATH = (
    RUNTIME_DIR
    / "geodata.json"
)


def main():
    print(
        "Known World runtime relief publisher"
    )

    if not SOURCE_PATH.exists():
        raise FileNotFoundError(
            f"Missing source relief raster:\n"
            f"{SOURCE_PATH}"
        )

    if not GEODATA_JSON_PATH.exists():
        raise FileNotFoundError(
            f"Missing geodata metadata:\n"
            f"{GEODATA_JSON_PATH}"
        )

    with GEODATA_JSON_PATH.open(
        "r",
        encoding="utf-8"
    ) as handle:
        metadata = json.load(
            handle
        )

    expected_width = int(
        metadata[
            "width"
        ]
    )

    expected_height = int(
        metadata[
            "height"
        ]
    )

    source_image = Image.open(
        SOURCE_PATH
    )

    if source_image.size != (
        expected_width,
        expected_height
    ):
        raise ValueError(
            "Relief raster dimensions do not match runtime "
            "geodata.\n"
            f"Expected: "
            f"{expected_width} x {expected_height}\n"
            f"Actual: "
            f"{source_image.width} x {source_image.height}"
        )

    source = np.asarray(
        source_image,
        dtype=np.uint16
    )

    runtime = np.round(
        source.astype(
            np.float32
        )
        / 65535.0
        * 255.0
    ).astype(
        np.uint8
    )

    RUNTIME_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    Image.fromarray(
        runtime,
        mode="L"
    ).save(
        TARGET_PATH
    )

    print()
    print(
        f"Source: {SOURCE_PATH}"
    )

    print(
        f"Target: {TARGET_PATH}"
    )

    print(
        f"Resolution: "
        f"{expected_width} x {expected_height}"
    )

    print()

    print(
        "Runtime relief intensity published."
    )


if __name__ == "__main__":
    main()

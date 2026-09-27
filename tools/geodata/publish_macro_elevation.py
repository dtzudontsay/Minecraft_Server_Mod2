from pathlib import Path
import shutil

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

SOURCE_PATH = (
    OUTPUT_DIR
    / "known_world_macro_elevation.png"
)

TARGET_PATH = (
    RUNTIME_GEODATA_DIR
    / "elevation.png"
)


EXPECTED_WIDTH = 9050
EXPECTED_HEIGHT = 6000


def validate_source():
    if not SOURCE_PATH.exists():
        raise FileNotFoundError(
            f"Missing macro elevation preview:\n{SOURCE_PATH}"
        )

    image = Image.open(
        SOURCE_PATH
    )

    if image.size != (
        EXPECTED_WIDTH,
        EXPECTED_HEIGHT
    ):
        raise ValueError(
            "Unexpected macro elevation dimensions.\n"
            f"Expected: {EXPECTED_WIDTH} x {EXPECTED_HEIGHT}\n"
            f"Actual:   {image.width} x {image.height}"
        )

    if image.mode not in (
        "I;16",
        "I;16B",
        "I;16L",
        "I"
    ):
        raise ValueError(
            "Macro elevation preview is not a 16-bit grayscale image.\n"
            f"Mode: {image.mode}"
        )

    return image


def main():
    print(
        "Known World macro elevation publisher"
    )

    image = validate_source()

    RUNTIME_GEODATA_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    shutil.copy2(
        SOURCE_PATH,
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
        f"Resolution: {image.width} x {image.height}"
    )

    print(
        f"Mode: {image.mode}"
    )

    print()
    print(
        "Runtime elevation raster published successfully."
    )


if __name__ == "__main__":
    main()

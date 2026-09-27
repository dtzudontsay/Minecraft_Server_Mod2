from pathlib import Path
import shutil

from PIL import Image, ImageDraw

from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

REGION_INPUT_DIR = (
    ROOT
    / "input"
    / "regions"
)

AUTO_GUIDE_DIR = (
    ROOT
    / "output"
    / "regional_relief_guides"
)

AUTHORING_DIR = (
    ROOT
    / "input"
    / "relief_authoring"
)

REFERENCE_DIR = (
    ROOT
    / "output"
    / "relief_authoring_references"
)


def find_region_source(zone_id):
    candidates = (
        REGION_INPUT_DIR / f"{zone_id}.png",
        REGION_INPUT_DIR / f"{zone_id}.jpg",
        REGION_INPUT_DIR / f"{zone_id}.jpeg",
        REGION_INPUT_DIR / f"{zone_id.upper()}.png",
        REGION_INPUT_DIR / f"{zone_id.upper()}.jpg",
        REGION_INPUT_DIR / f"{zone_id.upper()}.jpeg",
    )

    for path in candidates:
        if path.exists():
            return path

    return None


def load_auto_guide(zone_id, size):
    path = (
        AUTO_GUIDE_DIR
        / f"{zone_id}_relief_guide.png"
    )

    if not path.exists():
        return Image.new(
            "L",
            size,
            0
        )

    image = Image.open(
        path
    ).convert(
        "L"
    )

    if image.size != size:
        raise ValueError(
            f"Automatic relief guide for {zone_id} has "
            f"unexpected size {image.size}; expected {size}."
        )

    return image


def build_reference(
    artwork,
    auto_guide
):
    artwork = artwork.convert(
        "RGB"
    )

    width, height = artwork.size

    reference = Image.new(
        "RGB",
        (
            width * 2,
            height
        ),
        (
            0,
            0,
            0
        )
    )

    reference.paste(
        artwork,
        (
            0,
            0
        )
    )

    tinted = artwork.copy()

    source = tinted.load()
    guide = auto_guide.load()

    for y in range(height):
        for x in range(width):
            strength = (
                guide[x, y]
                / 255.0
            )

            if strength <= 0.0:
                continue

            r, g, b = source[
                x,
                y
            ]

            alpha = (
                strength
                * 0.75
            )

            source[
                x,
                y
            ] = (
                int(
                    r * (
                        1.0 - alpha
                    )
                    + 255
                    * alpha
                ),
                int(
                    g * (
                        1.0 - alpha
                    )
                ),
                int(
                    b * (
                        1.0 - alpha
                    )
                    + 255
                    * alpha
                ),
            )

    reference.paste(
        tinted,
        (
            width,
            0
        )
    )

    draw = ImageDraw.Draw(
        reference
    )

    draw.rectangle(
        (
            0,
            0,
            width - 1,
            38
        ),
        fill=(
            0,
            0,
            0
        )
    )

    draw.rectangle(
        (
            width,
            0,
            width * 2 - 1,
            38
        ),
        fill=(
            0,
            0,
            0
        )
    )

    draw.text(
        (
            12,
            10
        ),
        "CANONICAL SOURCE MAP",
        fill=(
            255,
            255,
            255
        )
    )

    draw.text(
        (
            width + 12,
            10
        ),
        "AUTO DETECTION - REFERENCE ONLY",
        fill=(
            255,
            255,
            255
        )
    )

    return reference


def create_authoring_mask(
    path,
    size
):
    if path.exists():
        print(
            f"  Existing authored mask preserved: {path}"
        )

        return

    image = Image.new(
        "L",
        size,
        0
    )

    image.save(
        path
    )

    print(
        f"  Created blank authored mask: {path}"
    )


def process_zone(zone_id):
    zone = ZONES[
        zone_id
    ]

    source_path = find_region_source(
        zone_id
    )

    if source_path is None:
        raise FileNotFoundError(
            f"No regional source artwork found for {zone_id}."
        )

    artwork = Image.open(
        source_path
    ).convert(
        "RGB"
    )

    expected_size = (
        zone.width,
        zone.height
    )

    if artwork.size != expected_size:
        raise ValueError(
            f"{zone_id.upper()} artwork size mismatch.\n"
            f"Expected: {expected_size}\n"
            f"Actual:   {artwork.size}"
        )

    auto_guide = load_auto_guide(
        zone_id,
        expected_size
    )

    mask_path = (
        AUTHORING_DIR
        / f"{zone_id}_relief_mask.png"
    )

    reference_path = (
        REFERENCE_DIR
        / f"{zone_id}_relief_reference.png"
    )

    create_authoring_mask(
        mask_path,
        expected_size
    )

    reference = build_reference(
        artwork,
        auto_guide
    )

    reference.save(
        reference_path
    )

    print(
        f"  Reference: {reference_path}"
    )


def main():
    print(
        "Known World canonical relief authoring setup"
    )

    AUTHORING_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    REFERENCE_DIR.mkdir(
        parents=True,
        exist_ok=True
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
            process_zone(
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

    print()
    print(
        "Authoring masks:"
    )

    print(
        AUTHORING_DIR
    )

    print()
    print(
        "Reference images:"
    )

    print(
        REFERENCE_DIR
    )


if __name__ == "__main__":
    main()

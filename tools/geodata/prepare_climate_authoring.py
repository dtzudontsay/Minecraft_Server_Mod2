from pathlib import Path
import shutil

from PIL import Image, ImageDraw, ImageFont

from climate_palette import CLIMATE_CATEGORIES
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
    / "climate_authoring"
)

REFERENCE_DIR = (
    ROOT
    / "output"
    / "climate_authoring_references"
)


def find_region_file(
    zone_id
):
    candidates = (
        REGION_DIR / f"{zone_id}.png",
        REGION_DIR / f"{zone_id.upper()}.png",
    )

    for path in candidates:
        if path.exists():
            return path

    return None


def create_palette_reference():
    width = 720
    row_height = 58

    height = (
        60
        + len(CLIMATE_CATEGORIES)
        * row_height
    )

    image = Image.new(
        "RGB",
        (
            width,
            height,
        ),
        (
            32,
            32,
            32,
        ),
    )

    draw = ImageDraw.Draw(
        image
    )

    font = ImageFont.load_default()

    draw.text(
        (
            20,
            20,
        ),
        "Known World Climate Authoring Palette",
        fill=(
            255,
            255,
            255,
        ),
        font=font,
    )

    y = 60

    for category in CLIMATE_CATEGORIES:
        draw.rectangle(
            (
                20,
                y,
                120,
                y + 38,
            ),
            fill=category.color,
        )

        hex_color = (
            "#"
            f"{category.color[0]:02X}"
            f"{category.color[1]:02X}"
            f"{category.color[2]:02X}"
        )

        label = (
            f"{category.code:02d}  "
            f"{category.name:<18}  "
            f"{hex_color}"
        )

        draw.text(
            (
                140,
                y + 12,
            ),
            label,
            fill=(
                255,
                255,
                255,
            ),
            font=font,
        )

        y += row_height

    output_path = (
        REFERENCE_DIR
        / "climate_palette_reference.png"
    )

    image.save(
        output_path
    )

    print(
        f"Palette reference: {output_path}"
    )


def main():
    print(
        "Known World climate-authoring preparation"
    )

    AUTHORING_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    REFERENCE_DIR.mkdir(
        parents=True,
        exist_ok=True
    )

    copied = 0
    existing = 0

    for zone_id in ZONE_ORDER:
        zone = ZONES[
            zone_id
        ]

        source = find_region_file(
            zone_id
        )

        if source is None:
            raise FileNotFoundError(
                f"Missing regional source for {zone_id}"
            )

        target = (
            AUTHORING_DIR
            / f"{zone_id}.png"
        )

        with Image.open(
            source
        ) as image:
            if image.size != (
                zone.width,
                zone.height,
            ):
                raise ValueError(
                    f"{zone_id.upper()} source resolution mismatch.\n"
                    f"Expected: {zone.width} x {zone.height}\n"
                    f"Actual:   {image.width} x {image.height}\n"
                    f"File:     {source}"
                )

        if target.exists():
            print(
                f"{zone_id.upper()}: already exists - left untouched"
            )

            existing += 1

            continue

        shutil.copy2(
            source,
            target,
        )

        print(
            f"{zone_id.upper()}: copied -> {target}"
        )

        copied += 1

    print()

    create_palette_reference()

    print()
    print(
        f"Copied:   {copied}"
    )

    print(
        f"Existing: {existing}"
    )

    print()
    print(
        "IMPORTANT:"
    )

    print(
        "Existing authoring files are never overwritten."
    )


if __name__ == "__main__":
    main()

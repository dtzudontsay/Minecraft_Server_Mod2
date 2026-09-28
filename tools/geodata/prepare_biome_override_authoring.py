from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

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

REFERENCE_DIR = (
    ROOT
    / "output"
    / "biome_override_authoring_references"
)


def find_region_file(zone_id):
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

    zone = ZONES[zone_id]

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
            with Image.open(candidate) as image:
                if image.size == (
                    zone.width,
                    zone.height,
                ):
                    matching.append(candidate)

        except Exception:
            pass

    if len(matching) == 1:
        return matching[0]

    if len(matching) > 1:
        raise RuntimeError(
            f"Multiple regional images match "
            f"{zone_id.upper()} resolution "
            f"{zone.width}x{zone.height}: "
            f"{matching}"
        )

    return None


def create_palette_reference():
    width = 900
    row_height = 48

    height = (
        70
        + len(BIOME_OVERRIDES)
        * row_height
    )

    image = Image.new(
        "RGB",
        (
            width,
            height,
        ),
        (
            30,
            30,
            30,
        ),
    )

    draw = ImageDraw.Draw(image)

    font = ImageFont.load_default()

    draw.text(
        (
            20,
            20,
        ),
        "Known World Exact Biome Override Palette",
        fill=(
            255,
            255,
            255,
        ),
        font=font,
    )

    y = 65

    for biome in BIOME_OVERRIDES:
        draw.rectangle(
            (
                20,
                y,
                120,
                y + 30,
            ),
            fill=biome.color,
        )

        hex_color = (
            "#"
            f"{biome.color[0]:02X}"
            f"{biome.color[1]:02X}"
            f"{biome.color[2]:02X}"
        )

        label = (
            f"{biome.code:02d}   "
            f"{hex_color}   "
            f"{biome.biome_id}"
        )

        draw.text(
            (
                140,
                y + 8,
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
        / "biome_override_palette_reference.png"
    )

    image.save(output_path)

    print(
        f"Palette reference: {output_path}"
    )


def main():
    print(
        "Known World biome-override authoring preparation"
    )

    AUTHORING_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    REFERENCE_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    copied = 0
    existing = 0

    for zone_id in ZONE_ORDER:
        zone = ZONES[zone_id]

        source = find_region_file(
            zone_id
        )

        if source is None:
            raise FileNotFoundError(
                f"Unable to find regional source "
                f"for {zone_id.upper()} "
                f"({zone.width}x{zone.height})"
            )

        target = (
            AUTHORING_DIR
            / f"{zone_id}.png"
        )

        if target.exists():
            print(
                f"{zone_id.upper()}: "
                f"already exists - untouched"
            )

            existing += 1

            continue

        with Image.open(source) as source_image:
            converted = source_image.convert(
                "RGB"
            )

            converted.save(
                target
            )

        print(
            f"{zone_id.upper()}: "
            f"created -> {target}"
        )

        copied += 1

    print()

    create_palette_reference()

    print()

    print(
        f"Created:  {copied}"
    )

    print(
        f"Existing: {existing}"
    )

    print()

    print(
        "Existing biome-authoring files "
        "are NEVER overwritten."
    )


if __name__ == "__main__":
    main()

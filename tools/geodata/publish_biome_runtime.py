from pathlib import Path
import shutil

from PIL import Image


ROOT = Path(__file__).resolve().parent

OUTPUT_DIR = (
    ROOT
    / "output"
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


CLIMATE_SOURCE = (
    OUTPUT_DIR
    / "known_world_climate.png"
)

BIOME_OVERRIDE_SOURCE = (
    OUTPUT_DIR
    / "known_world_biome_override.png"
)

BIOME_LEGEND_SOURCE = (
    OUTPUT_DIR
    / "known_world_biome_override_legend.json"
)

LAND_MASK_SOURCE = (
    OUTPUT_DIR
    / "known_world_canonical_land_mask.png"
)


CLIMATE_TARGET = (
    RUNTIME_DIR
    / "climate.png"
)

BIOME_OVERRIDE_TARGET = (
    RUNTIME_DIR
    / "biome_override.png"
)

BIOME_LEGEND_TARGET = (
    RUNTIME_DIR
    / "biome_override_legend.json"
)


def require_file(
    path
):
    if not path.exists():
        raise FileNotFoundError(
            f"Missing required file: {path}"
        )


def image_size(
    path
):
    with Image.open(
        path
    ) as image:
        return image.size


def main():
    print(
        "Known World climate/biome runtime publisher"
    )

    require_file(
        LAND_MASK_SOURCE
    )

    require_file(
        CLIMATE_SOURCE
    )

    require_file(
        BIOME_OVERRIDE_SOURCE
    )

    require_file(
        BIOME_LEGEND_SOURCE
    )


    canonical_size = image_size(
        LAND_MASK_SOURCE
    )

    climate_size = image_size(
        CLIMATE_SOURCE
    )

    override_size = image_size(
        BIOME_OVERRIDE_SOURCE
    )


    if climate_size != canonical_size:
        raise ValueError(
            "Climate raster does not match "
            "canonical geography.\n"
            f"Canonical: {canonical_size}\n"
            f"Climate:   {climate_size}"
        )


    if override_size != canonical_size:
        raise ValueError(
            "Biome override raster does not match "
            "canonical geography.\n"
            f"Canonical: {canonical_size}\n"
            f"Override:  {override_size}"
        )


    RUNTIME_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )


    shutil.copy2(
        CLIMATE_SOURCE,
        CLIMATE_TARGET,
    )

    shutil.copy2(
        BIOME_OVERRIDE_SOURCE,
        BIOME_OVERRIDE_TARGET,
    )

    shutil.copy2(
        BIOME_LEGEND_SOURCE,
        BIOME_LEGEND_TARGET,
    )


    print()

    print(
        f"Published climate: "
        f"{CLIMATE_TARGET}"
    )

    print(
        f"Published biome overrides: "
        f"{BIOME_OVERRIDE_TARGET}"
    )

    print(
        f"Published biome legend: "
        f"{BIOME_LEGEND_TARGET}"
    )

    print()

    print(
        f"Runtime raster size: "
        f"{canonical_size[0]}x{canonical_size[1]}"
    )


if __name__ == "__main__":
    main()

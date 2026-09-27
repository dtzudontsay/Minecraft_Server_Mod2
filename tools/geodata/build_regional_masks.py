from pathlib import Path
from collections import deque
import colorsys
import traceback

from PIL import Image, ImageFilter

from zone_transforms import ZONES, ZONE_ORDER


ROOT = Path(__file__).resolve().parent

INPUT_DIR = ROOT / "input" / "regions"
OUTPUT_DIR = ROOT / "output" / "regional_masks"

OUTPUT_DIR.mkdir(parents=True, exist_ok=True)


def find_source_file(zone_id):
    candidates = [
        INPUT_DIR / f"{zone_id}.png",
        INPUT_DIR / f"{zone_id}.jpg",
        INPUT_DIR / f"{zone_id}.jpeg",
        INPUT_DIR / f"{zone_id.upper()}.png",
        INPUT_DIR / f"{zone_id.upper()}.jpg",
        INPUT_DIR / f"{zone_id.upper()}.jpeg",
    ]

    for path in candidates:
        if path.exists():
            return path

    return None


def rgb_to_hsv(r, g, b):
    return colorsys.rgb_to_hsv(
        r / 255.0,
        g / 255.0,
        b / 255.0
    )


def looks_like_water(r, g, b):
    h, s, v = rgb_to_hsv(r, g, b)

    return (
        0.48 <= h <= 0.73
        and s >= 0.09
        and v >= 0.15
        and b >= r * 1.025
        and b >= g * 0.94
    )


def build_raw_water_mask(image):
    rgb = image.convert("RGB")

    width, height = rgb.size

    output = Image.new(
        "L",
        (width, height),
        0
    )

    source = rgb.load()
    target = output.load()

    for y in range(height):
        for x in range(width):
            r, g, b = source[x, y]

            if looks_like_water(r, g, b):
                target[x, y] = 255

    return output


def clean_candidate_water(mask):
    """
    Light closing/opening.

    Goal:
    - reconnect small gaps caused by lettering/scan noise
    - remove very thin blue artifacts
    - avoid heavily changing the coastline
    """

    result = mask.filter(
        ImageFilter.MaxFilter(size=3)
    )

    result = result.filter(
        ImageFilter.MinFilter(size=3)
    )

    result = result.filter(
        ImageFilter.MinFilter(size=3)
    )

    result = result.filter(
        ImageFilter.MaxFilter(size=3)
    )

    return result


def keep_border_connected_water(mask):
    """
    Keep only candidate water that is connected to the outside
    of the regional crop.

    This is ideal for the coastline/silhouette milestone:

        external seas/oceans -> retained
        bays/straits connected to them -> retained
        mountain-blue artifacts -> removed
        rivers -> mostly removed
        inland lakes -> intentionally ignored for now

    Uses 8-neighbour connectivity.
    """

    width, height = mask.size
    src = mask.load()

    output = Image.new(
        "L",
        (width, height),
        0
    )

    dst = output.load()

    visited = bytearray(
        width * height
    )

    queue = deque()

    def index(x, y):
        return y * width + x

    def add_seed(x, y):
        i = index(x, y)

        if visited[i]:
            return

        visited[i] = 1

        if src[x, y] >= 128:
            queue.append((x, y))

    # Every water pixel on the crop boundary can seed the ocean.
    for x in range(width):
        add_seed(x, 0)
        add_seed(x, height - 1)

    for y in range(height):
        add_seed(0, y)
        add_seed(width - 1, y)

    neighbours = (
        (-1, -1), (0, -1), (1, -1),
        (-1,  0),          (1,  0),
        (-1,  1), (0,  1), (1,  1),
    )

    while queue:
        x, y = queue.popleft()

        dst[x, y] = 255

        for dx, dy in neighbours:
            nx = x + dx
            ny = y + dy

            if not (
                0 <= nx < width
                and 0 <= ny < height
            ):
                continue

            i = index(nx, ny)

            if visited[i]:
                continue

            visited[i] = 1

            if src[nx, ny] >= 128:
                queue.append((nx, ny))

    return output


def water_to_land_mask(water_mask):
    """
    Final silhouette convention:

        WHITE = land
        BLACK = external water
    """

    return water_mask.point(
        lambda value:
        0 if value >= 128 else 255
    )


def build_overlay(original, land_mask):
    """
    Debug overlay:

        green = classified land
        blue  = classified water
    """

    output = original.convert("RGB").copy()

    pixels = output.load()
    mask = land_mask.load()

    width, height = output.size

    for y in range(height):
        for x in range(width):

            r, g, b = pixels[x, y]

            if mask[x, y] >= 128:
                overlay = (60, 210, 70)
            else:
                overlay = (40, 100, 230)

            pixels[x, y] = (
                int(r * 0.65 + overlay[0] * 0.35),
                int(g * 0.65 + overlay[1] * 0.35),
                int(b * 0.65 + overlay[2] * 0.35),
            )

    return output


def count_white_pixels(mask):
    histogram = mask.histogram()

    return sum(
        histogram[128:]
    )


def process_zone(zone_id):
    zone = ZONES[zone_id]

    print()
    print("=" * 60)
    print(f"Processing {zone_id.upper()} - {zone.name}")

    source_path = find_source_file(zone_id)

    if source_path is None:
        raise FileNotFoundError(
            f"No source image found for {zone_id.upper()} in:\n"
            f"{INPUT_DIR}"
        )

    print(f"Source: {source_path}")

    image = Image.open(
        source_path
    ).convert("RGB")

    print(
        f"Actual raster:   {image.width} x {image.height}"
    )

    print(
        f"Expected raster: {zone.width} x {zone.height}"
    )

    if image.size != (
        zone.width,
        zone.height
    ):
        raise ValueError(
            f"{zone_id.upper()} SIZE MISMATCH: "
            f"expected {zone.width}x{zone.height}, "
            f"got {image.width}x{image.height}"
        )

    print("Building raw candidate-water mask...")

    raw_water = build_raw_water_mask(
        image
    )

    raw_path = (
        OUTPUT_DIR
        / f"{zone_id}_raw_water.png"
    )

    raw_water.save(raw_path)

    print(f"Saved: {raw_path.name}")

    print("Cleaning candidate water...")

    cleaned_water = clean_candidate_water(
        raw_water
    )

    candidate_path = (
        OUTPUT_DIR
        / f"{zone_id}_candidate_water.png"
    )

    cleaned_water.save(
        candidate_path
    )

    print(
        f"Saved: {candidate_path.name}"
    )

    print("Finding external sea/ocean water...")

    external_water = keep_border_connected_water(
        cleaned_water
    )

    external_path = (
        OUTPUT_DIR
        / f"{zone_id}_water_mask.png"
    )

    external_water.save(
        external_path
    )

    print(
        f"Saved: {external_path.name}"
    )

    print("Building land mask...")

    land_mask = water_to_land_mask(
        external_water
    )

    land_path = (
        OUTPUT_DIR
        / f"{zone_id}_land_mask.png"
    )

    land_mask.save(
        land_path
    )

    print(
        f"Saved: {land_path.name}"
    )

    print("Building debug overlay...")

    overlay = build_overlay(
        image,
        land_mask
    )

    overlay_path = (
        OUTPUT_DIR
        / f"{zone_id}_overlay.png"
    )

    overlay.save(
        overlay_path
    )

    print(
        f"Saved: {overlay_path.name}"
    )

    total_pixels = (
        image.width * image.height
    )

    water_pixels = count_white_pixels(
        external_water
    )

    land_pixels = (
        total_pixels - water_pixels
    )

    print(
        f"External water: "
        f"{water_pixels:,} px "
        f"({water_pixels / total_pixels:.1%})"
    )

    print(
        f"Provisional land: "
        f"{land_pixels:,} px "
        f"({land_pixels / total_pixels:.1%})"
    )

    print(
        f"{zone_id.upper()} COMPLETE"
    )


def main():
    print(
        "Known World regional coastline-mask builder"
    )

    print(
        f"Input directory:  {INPUT_DIR}"
    )

    print(
        f"Output directory: {OUTPUT_DIR}"
    )

    successful = 0
    failed = 0

    for zone_id in ZONE_ORDER:
        try:
            process_zone(
                zone_id
            )

            successful += 1

        except Exception as error:
            failed += 1

            print()
            print("!!! ERROR !!!")
            print(
                f"Zone: {zone_id.upper()}"
            )
            print(
                f"{type(error).__name__}: {error}"
            )
            print()

            traceback.print_exc()

            print()
            print(
                "Continuing with next zone."
            )

    print()
    print("=" * 60)
    print("DONE")
    print(
        f"Successful zones: {successful}"
    )
    print(
        f"Failed zones:     {failed}"
    )
    print("=" * 60)


if __name__ == "__main__":
    main()

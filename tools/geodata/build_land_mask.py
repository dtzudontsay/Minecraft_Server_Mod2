from pathlib import Path
from collections import deque

from PIL import Image, ImageFilter
import colorsys


ROOT = Path(__file__).resolve().parent

MASTER_PATH = (
    ROOT
    / "input"
    / "master"
    / "known_world_master.jpg"
)

OUTPUT_DIR = ROOT / "output"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

MASTER_PREVIEW = (
    OUTPUT_DIR
    / "known_world_master_preview.png"
)

RAW_WATER_PREVIEW = (
    OUTPUT_DIR
    / "known_world_raw_water_preview.png"
)

CLEANED_WATER_PREVIEW = (
    OUTPUT_DIR
    / "known_world_cleaned_water_preview.png"
)

EXTERNAL_WATER_PREVIEW = (
    OUTPUT_DIR
    / "known_world_external_water_preview.png"
)

FINAL_LAND_MASK = (
    OUTPUT_DIR
    / "known_world_land_mask_preview.png"
)


def rgb_to_hsv(r, g, b):
    return colorsys.rgb_to_hsv(
        r / 255.0,
        g / 255.0,
        b / 255.0
    )


def looks_like_water(r, g, b):
    """
    Broad classifier for blue/cyan water.

    It is intentionally permissive.
    False positives will be handled by connected-component
    analysis rather than overly strict colour thresholds.
    """

    h, s, v = rgb_to_hsv(r, g, b)

    return (
        0.48 <= h <= 0.72
        and s >= 0.10
        and v >= 0.16
        and b >= r * 1.03
        and b >= g * 0.95
    )


def build_raw_water_mask(image):
    rgb = image.convert("RGB")

    width, height = rgb.size

    out = Image.new(
        "L",
        (width, height),
        0
    )

    source = rgb.load()
    target = out.load()

    for y in range(height):
        for x in range(width):
            r, g, b = source[x, y]

            if looks_like_water(r, g, b):
                target[x, y] = 255

    return out


def clean_water_mask(mask):
    """
    Close small breaks in ocean classification without
    aggressively eroding coastlines.

    MaxFilter grows water slightly.
    MinFilter contracts it again.
    """

    mask = mask.filter(
        ImageFilter.MaxFilter(size=3)
    )

    mask = mask.filter(
        ImageFilter.MinFilter(size=3)
    )

    return mask


def find_largest_water_component(mask):
    """
    Find the largest connected component of candidate water.

    For this map, that component should represent the
    interconnected world ocean.

    Uses 8-neighbour connectivity.
    """

    width, height = mask.size
    pixels = mask.load()

    visited = bytearray(
        width * height
    )

    neighbours = (
        (-1, -1), (0, -1), (1, -1),
        (-1,  0),          (1,  0),
        (-1,  1), (0,  1), (1,  1),
    )

    def index(x, y):
        return y * width + x

    largest_component = []
    largest_size = 0

    component_count = 0

    for y in range(height):
        for x in range(width):

            i = index(x, y)

            if visited[i]:
                continue

            visited[i] = 1

            if pixels[x, y] < 128:
                continue

            component_count += 1

            queue = deque()
            queue.append((x, y))

            component = []

            while queue:
                cx, cy = queue.popleft()
                component.append((cx, cy))

                for dx, dy in neighbours:
                    nx = cx + dx
                    ny = cy + dy

                    if not (
                        0 <= nx < width
                        and 0 <= ny < height
                    ):
                        continue

                    ni = index(nx, ny)

                    if visited[ni]:
                        continue

                    visited[ni] = 1

                    if pixels[nx, ny] >= 128:
                        queue.append(
                            (nx, ny)
                        )

            if len(component) > largest_size:
                largest_size = len(component)
                largest_component = component

    print(
        f"Candidate water components: "
        f"{component_count}"
    )

    print(
        f"Largest water component: "
        f"{largest_size} pixels"
    )

    result = Image.new(
        "L",
        (width, height),
        0
    )

    result_pixels = result.load()

    for x, y in largest_component:
        result_pixels[x, y] = 255

    return result


def make_land_mask(external_water):
    """
    Convert:

        white = main world ocean
        black = everything else

    into:

        white = land
        black = water
    """

    return external_water.point(
        lambda value:
        0 if value >= 128 else 255
    )


def clear_outer_frame(mask, border=4):
    """
    The decorative frame around the map is not geography.
    Force the very outside edge to water/black.
    """

    width, height = mask.size
    pixels = mask.load()

    for y in range(height):
        for x in range(width):

            if (
                x < border
                or y < border
                or x >= width - border
                or y >= height - border
            ):
                pixels[x, y] = 0

    return mask


def main():
    if not MASTER_PATH.exists():
        raise FileNotFoundError(
            f"Master map not found:\n{MASTER_PATH}"
        )

    master = Image.open(
        MASTER_PATH
    ).convert("RGB")

    print(
        f"Loaded master map: "
        f"{master.width} x {master.height}"
    )

    master.save(
        MASTER_PREVIEW
    )

    raw_water = build_raw_water_mask(
        master
    )

    raw_water.save(
        RAW_WATER_PREVIEW
    )

    cleaned_water = clean_water_mask(
        raw_water
    )

    cleaned_water.save(
        CLEANED_WATER_PREVIEW
    )

    external_water = find_largest_water_component(
        cleaned_water
    )

    external_water.save(
        EXTERNAL_WATER_PREVIEW
    )

    land_mask = make_land_mask(
        external_water
    )

    land_mask = clear_outer_frame(
        land_mask,
        border=4
    )

    land_mask.save(
        FINAL_LAND_MASK
    )

    print()
    print("Generated:")
    print(MASTER_PREVIEW)
    print(RAW_WATER_PREVIEW)
    print(CLEANED_WATER_PREVIEW)
    print(EXTERNAL_WATER_PREVIEW)
    print(FINAL_LAND_MASK)

    print()
    print(
        "External water preview:"
    )
    print(
        "  WHITE = largest connected water system"
    )

    print()
    print(
        "Final land mask:"
    )
    print(
        "  WHITE = provisional land"
    )
    print(
        "  BLACK = provisional ocean"
    )


if __name__ == "__main__":
    main()

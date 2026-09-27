from pathlib import Path
from collections import deque

from PIL import Image


ROOT = Path(__file__).resolve().parent
OUTPUT_DIR = ROOT / "output"

MERGED_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_merged_land_mask.png"
)

COVERAGE_PATH = (
    OUTPUT_DIR
    / "known_world_regional_coverage.png"
)

MASTER_ART_PATH = (
    ROOT
    / "input"
    / "master"
    / "known_world_master.jpg"
)

FINAL_MASK_PATH = (
    OUTPUT_DIR
    / "known_world_canonical_land_mask.png"
)

FINAL_OVERLAY_PATH = (
    OUTPUT_DIR
    / "known_world_canonical_overlay.png"
)

REMOVED_ARTIFACTS_PATH = (
    OUTPUT_DIR
    / "known_world_removed_artifacts.png"
)


# ------------------------------------------------------------
# LOGICAL MASTER COORDINATE SYSTEM
# ------------------------------------------------------------

LOGICAL_WIDTH = 2048
LOGICAL_HEIGHT = 1357


def load_required(path, mode):
    if not path.exists():
        raise FileNotFoundError(
            f"Required file missing:\n{path}"
        )

    return Image.open(path).convert(mode)


def logical_rect_to_actual(
    rect,
    width,
    height
):
    x1, y1, x2, y2 = rect

    return (
        int(x1 / LOGICAL_WIDTH * width),
        int(y1 / LOGICAL_HEIGHT * height),
        int(x2 / LOGICAL_WIDTH * width),
        int(y2 / LOGICAL_HEIGHT * height),
    )


def clear_rectangle(
    mask,
    removed,
    rect
):
    width, height = mask.size

    x1, y1, x2, y2 = logical_rect_to_actual(
        rect,
        width,
        height
    )

    pixels = mask.load()
    removed_pixels = removed.load()

    x1 = max(0, x1)
    y1 = max(0, y1)
    x2 = min(width - 1, x2)
    y2 = min(height - 1, y2)

    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):

            if pixels[x, y] >= 128:
                removed_pixels[x, y] = 255

            pixels[x, y] = 0


def clear_outer_frame(
    mask,
    removed,
    thickness=12
):
    """
    Remove decorative printed border.

    thickness is specified in logical 2048x1357 pixels.
    """

    width, height = mask.size

    tx = max(
        1,
        int(
            thickness
            / LOGICAL_WIDTH
            * width
        )
    )

    ty = max(
        1,
        int(
            thickness
            / LOGICAL_HEIGHT
            * height
        )
    )

    pixels = mask.load()
    removed_pixels = removed.load()

    for y in range(height):
        for x in range(width):

            if (
                x < tx
                or x >= width - tx
                or y < ty
                or y >= height - ty
            ):
                if pixels[x, y] >= 128:
                    removed_pixels[x, y] = 255

                pixels[x, y] = 0


def component_cleanup_in_fallback(
    mask,
    coverage,
    removed
):
    """
    Removes small / text-like land fragments ONLY in areas that
    are not covered by one of our regional high-resolution maps.

    We deliberately do NOT perform this cleanup inside regional
    coverage because those masks contain legitimate small islands.

    This pass is conservative:
    - tiny isolated fragments disappear
    - long thin text-like shapes disappear
    - larger land/island components remain
    """

    width, height = mask.size

    mask_pixels = mask.load()
    coverage_pixels = coverage.load()
    removed_pixels = removed.load()

    visited = bytearray(
        width * height
    )

    def idx(x, y):
        return y * width + x

    neighbours = (
        (-1, 0),
        (1, 0),
        (0, -1),
        (0, 1),
    )

    removed_components = 0
    retained_components = 0

    for start_y in range(height):
        for start_x in range(width):

            index = idx(
                start_x,
                start_y
            )

            if visited[index]:
                continue

            visited[index] = 1

            if mask_pixels[
                start_x,
                start_y
            ] < 128:
                continue

            queue = deque(
                [
                    (
                        start_x,
                        start_y
                    )
                ]
            )

            component = []

            min_x = start_x
            max_x = start_x

            min_y = start_y
            max_y = start_y

            touches_regional = False

            while queue:

                x, y = queue.popleft()

                component.append(
                    (
                        x,
                        y
                    )
                )

                min_x = min(
                    min_x,
                    x
                )

                max_x = max(
                    max_x,
                    x
                )

                min_y = min(
                    min_y,
                    y
                )

                max_y = max(
                    max_y,
                    y
                )

                if coverage_pixels[
                    x,
                    y
                ] >= 128:
                    touches_regional = True

                for dx, dy in neighbours:

                    nx = x + dx
                    ny = y + dy

                    if not (
                        0 <= nx < width
                        and 0 <= ny < height
                    ):
                        continue

                    ni = idx(
                        nx,
                        ny
                    )

                    if visited[ni]:
                        continue

                    visited[ni] = 1

                    if mask_pixels[
                        nx,
                        ny
                    ] >= 128:

                        queue.append(
                            (
                                nx,
                                ny
                            )
                        )

            if touches_regional:
                retained_components += 1
                continue

            area = len(
                component
            )

            box_width = (
                max_x
                - min_x
                + 1
            )

            box_height = (
                max_y
                - min_y
                + 1
            )

            long_side = max(
                box_width,
                box_height
            )

            short_side = max(
                1,
                min(
                    box_width,
                    box_height
                )
            )

            aspect_ratio = (
                long_side
                / short_side
            )

            box_area = (
                box_width
                * box_height
            )

            fill_ratio = (
                area
                / box_area
            )

            # Scale thresholds from logical resolution
            # to whatever raster we're processing.

            pixel_scale = (
                width
                / LOGICAL_WIDTH
            ) * (
                height
                / LOGICAL_HEIGHT
            )

            tiny_limit = int(
                35
                * pixel_scale
            )

            small_limit = int(
                130
                * pixel_scale
            )

            remove_component = False

            # Tiny isolated specks / punctuation.
            if area <= tiny_limit:
                remove_component = True

            # Text strokes tend to be narrow or sparse.
            elif (
                area <= small_limit
                and (
                    aspect_ratio >= 3.0
                    or fill_ratio <= 0.28
                )
            ):
                remove_component = True

            if remove_component:

                for x, y in component:

                    mask_pixels[
                        x,
                        y
                    ] = 0

                    removed_pixels[
                        x,
                        y
                    ] = 255

                removed_components += 1

            else:
                retained_components += 1

    print(
        f"Fallback components removed: "
        f"{removed_components}"
    )

    print(
        f"Land components retained: "
        f"{retained_components}"
    )


def build_overlay(
    artwork,
    mask
):
    result = artwork.copy()

    source = result.load()
    mask_pixels = mask.load()

    width, height = result.size

    for y in range(height):
        for x in range(width):

            r, g, b = source[
                x,
                y
            ]

            if mask_pixels[
                x,
                y
            ] >= 128:

                tint = (
                    45,
                    225,
                    70
                )

            else:

                tint = (
                    30,
                    105,
                    225
                )

            source[
                x,
                y
            ] = (
                int(
                    r * 0.62
                    + tint[0] * 0.38
                ),
                int(
                    g * 0.62
                    + tint[1] * 0.38
                ),
                int(
                    b * 0.62
                    + tint[2] * 0.38
                ),
            )

    return result


def main():

    print(
        "Known World canonical land-mask cleanup"
    )

    mask = load_required(
        MERGED_MASK_PATH,
        "L"
    )

    coverage = load_required(
        COVERAGE_PATH,
        "L"
    )

    artwork = load_required(
        MASTER_ART_PATH,
        "RGB"
    )

    if (
        mask.size != coverage.size
        or mask.size != artwork.size
    ):
        raise ValueError(
            "Master files do not have matching dimensions.\n"
            f"Mask:     {mask.size}\n"
            f"Coverage: {coverage.size}\n"
            f"Artwork:  {artwork.size}"
        )

    # Hard binary mask.
    mask = mask.point(
        lambda value:
        255
        if value >= 128
        else 0
    )

    removed = Image.new(
        "L",
        mask.size,
        0
    )

    print(
        f"Raster: "
        f"{mask.width} x {mask.height}"
    )

    print()
    print(
        "Removing decorative frame..."
    )

    clear_outer_frame(
        mask,
        removed,
        thickness=10
    )

    print(
        "Removing title/cartouche..."
    )

    # Upper-right title and legend.
    #
    # This is intentionally restricted to the known decorative
    # area. It does not touch geographic terrain.
    clear_rectangle(
        mask,
        removed,
        (
            1630,
            25,
            1998,
            244,
        )
    )

    print(
        "Cleaning isolated fallback artifacts..."
    )

    component_cleanup_in_fallback(
        mask,
        coverage,
        removed
    )

    print()
    print(
        "Saving canonical mask..."
    )

    mask.save(
        FINAL_MASK_PATH
    )

    removed.save(
        REMOVED_ARTIFACTS_PATH
    )

    overlay = build_overlay(
        artwork,
        mask
    )

    overlay.save(
        FINAL_OVERLAY_PATH
    )

    print()
    print(
        "Generated:"
    )

    print(
        FINAL_MASK_PATH
    )

    print(
        FINAL_OVERLAY_PATH
    )

    print(
        REMOVED_ARTIFACTS_PATH
    )

    print()
    print(
        "Canonical mask:"
    )

    print(
        "  WHITE = geographic land"
    )

    print(
        "  BLACK = water / outside geography"
    )

    print()
    print(
        "Removed-artifacts image:"
    )

    print(
        "  WHITE = pixels removed during final cleanup"
    )


if __name__ == "__main__":
    main()

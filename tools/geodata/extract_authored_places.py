#!/usr/bin/env python3
r"""Find preliminary red-X centres in tools/geodata/input/cities/*.png.

Run from repository root:
    py tools\geodata\extract_authored_places.py

Needs: Pillow, numpy, opencv-python, and adjacent zone_transforms.py.
Markers and world coordinates are UNVERIFIED until reviewed against the maps.
"""
from __future__ import annotations

import argparse
import csv
import json
import math
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from zone_transforms import ZONES, ZONE_ORDER

HERE = Path(__file__).resolve().parent
RED = np.array((237, 28, 36), dtype=np.uint8)  # #ED1C24
METRES_PER_MASTER_PIXEL = 482_803.2 / math.hypot(80.0, -7.5)


def red_mask(img: Image.Image) -> np.ndarray:
    return cv2.inRange(np.asarray(img.convert("RGB")), RED, RED) // 255


def components(mask):
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    for index in range(1, count):
        x, y, w, h, area = map(int, stats[index])
        yield (x, y, w, h, area, labels[y:y+h, x:x+w] == index)


def reference_templates(ne_mask):
    result = []
    for x, y, w, h, area, shape in components(ne_mask):
        if 75 <= area <= 230 and 10 <= w <= 29 and 10 <= h <= 29:
            for size in (17, 19, 21, 23):
                item = cv2.resize(
                    shape.astype(np.float32),
                    (size, size),
                    interpolation=cv2.INTER_NEAREST,
                )
                result.append(np.pad(item, 2))
    if not result:
        raise ValueError("Could not find example crosses in ne.png")
    return result


def candidates(mask, templates):
    compact, connected = [], []

    for x, y, w, h, area, _ in components(mask):
        if (
            50 <= area <= 250
            and 8 <= w <= 31
            and 9 <= h <= 31
            and 0.5 <= w / h <= 1.9
        ):
            compact.append((
                (x + (w-1)/2, y + (h-1)/2),
                "COMPONENT",
                1.0,
            ))

        elif area >= 40:
            x0, y0 = max(0, x-15), max(0, y-15)
            x1 = min(mask.shape[1], x+w+15)
            y1 = min(mask.shape[0], y+h+15)
            region = mask[y0:y1, x0:x1].astype(np.float32)

            for template in templates:
                if min(
                    region.shape[0] - template.shape[0],
                    region.shape[1] - template.shape[1],
                ) < 0:
                    continue

                response = cv2.matchTemplate(
                    region,
                    template,
                    cv2.TM_CCOEFF_NORMED,
                )
                maxima = cv2.dilate(
                    response,
                    np.ones((15, 15), np.uint8),
                )
                ys, xs = np.where(
                    (response >= 0.56)
                    & (response >= maxima - 1e-6)
                )

                for px, py in zip(xs.tolist(), ys.tolist()):
                    connected.append((
                        (
                            x0 + px + (template.shape[1]-1)/2,
                            y0 + py + (template.shape[0]-1)/2,
                        ),
                        "TEMPLATE",
                        float(response[py, px]),
                    ))

    found = compact[:]

    for item in sorted(connected, key=lambda c: -c[2]):
        (x, y), _, _ = item
        if all(
            (x - prev[0][0])**2
            + (y - prev[0][1])**2 > 121
            for prev in found
        ):
            found.append(item)

    return sorted(found, key=lambda c: (c[0][1], c[0][0]))


def calculate_world(zone, x, y):
    mx, my = ZONES[zone].regional_to_master(x, y)
    return (
        round(mx, 5),
        round(my, 5),
        round((mx - 1024) * METRES_PER_MASTER_PIXEL),
        round((my - 678.5) * METRES_PER_MASTER_PIXEL),
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--input",
        type=Path,
        default=HERE / "input" / "cities",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=HERE / "output" / "authored_places",
    )
    args = parser.parse_args()

    images = {}

    for zone in ZONE_ORDER:
        path = args.input / f"{zone}.png"
        if not path.is_file():
            parser.error(f"Missing source image: {path}")

        with Image.open(path) as opened:
            images[zone] = opened.convert("RGB")

        expected = (ZONES[zone].width, ZONES[zone].height)

        if images[zone].size != expected:
            parser.error(
                f"{path} size {images[zone].size}; "
                f"expected {expected}"
            )

    templates = reference_templates(red_mask(images["ne"]))
    args.output.mkdir(parents=True, exist_ok=True)

    all_rows, by_zone = [], {}

    try:
        font = ImageFont.truetype("DejaVuSans.ttf", 16)
    except OSError:
        font = ImageFont.load_default()

    for zone in ZONE_ORDER:
        image = images[zone]
        found = candidates(red_mask(image), templates)

        preview = image.copy()
        draw = ImageDraw.Draw(preview)
        rows = []

        for idx, (point, method, score) in enumerate(found, start=1):
            x, y = point
            mx, my, world_x, world_z = calculate_world(zone, x, y)
            marker_id = f"{zone.upper()}-{idx:03d}"

            row = {
                "id": marker_id,
                "zone": zone.upper(),
                "pixel_x": round(x, 2),
                "pixel_y": round(y, 2),
                "master_x": mx,
                "master_y": my,
                "minecraft_x": world_x,
                "minecraft_z": world_z,
                "detection": method,
                "score": round(score, 4),
                "name": "",
                "status": "NEEDS_REVIEW",
            }
            rows.append(row)

            color = (
                "#00ffff"
                if method == "COMPONENT"
                else "#ffbb00"
            )

            draw.ellipse(
                (x-14, y-14, x+14, y+14),
                outline=color,
                width=2,
            )

            draw.text(
                (x+16, y-22),
                f"{idx:03d}",
                fill="white",
                font=font,
                stroke_width=2,
                stroke_fill="black",
            )

        preview.save(args.output / f"{zone}_review.png")
        by_zone[zone.upper()] = len(rows)
        all_rows.extend(rows)

        print(
            f"{zone.upper()}: "
            f"{len(rows)} unverified candidates"
        )

    fields = (
        "id", "zone", "pixel_x", "pixel_y",
        "master_x", "master_y",
        "minecraft_x", "minecraft_z",
        "detection", "score",
        "name", "status",
    )

    with (args.output / "marker_candidates.csv").open(
        "w",
        newline="",
        encoding="utf-8",
    ) as out:
        writer = csv.DictWriter(out, fieldnames=fields)
        writer.writeheader()
        writer.writerows(all_rows)

    (args.output / "marker_candidates.json").write_text(
        json.dumps(
            all_rows,
            ensure_ascii=False,
            indent=2,
        ) + "\n",
        encoding="utf-8",
    )

    (args.output / "summary.json").write_text(
        json.dumps(
            {
                "total_candidates": len(all_rows),
                "by_zone": by_zone,
                "warning": (
                    "Candidates are not validated settlements."
                ),
            },
            indent=2,
        ),
        encoding="utf-8",
    )

    print(
        "\nReview PNGs and coordinates saved to: "
        f"{args.output.resolve()}"
    )
    print(
        "Some Xs are missed and some red text/lines "
        "are false positives."
    )
    print(
        "Names, coastline corrections and duplicates "
        "require verification."
    )


if __name__ == "__main__":
    main()

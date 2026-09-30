from pathlib import Path
import json
import shutil
from PIL import Image
import numpy as np

ROOT = Path(__file__).resolve().parents[2]

REFERENCE_DIR = ROOT / "tools" / "geodata" / "input" / "regions"
PAINTED_DIR = ROOT / "authoring" / "maps" / "Realms"
RUNTIME_DIR = ROOT / "src" / "main" / "resources" / "assets" / "knownworld" / "subregions"
MASK_JSON = ROOT / "src" / "main" / "resources" / "data" / "knownworld" / "reference" / "subregion_masks.json"

OUTPUT_DIR = ROOT / "authoring" / "maps" / "Realms_corrected"
BACKUP_DIR = ROOT / "authoring" / "maps" / "Realms_runtime_backup"

MIN_EXACT_MATCH_FRACTION = 0.15
MIN_MARGIN_OVER_SECOND_BEST = 0.02


def load_masks():
    with MASK_JSON.open("r", encoding="utf-8") as f:
        entries = json.load(f)

    result = {}
    for item in entries:
        zone = item["zoneId"].lower()
        colors = set()
        for e in item["entries"]:
            h = e["hexColor"].lstrip("#")
            colors.add(tuple(int(h[i:i+2], 16) for i in (0, 2, 4)))
        result[zone] = {
            "width": int(item["width"]),
            "height": int(item["height"]),
            "colors": colors,
        }
    return result


def find_reference(zone):
    candidates = [
        REFERENCE_DIR / f"{zone}.png",
        REFERENCE_DIR / f"{zone.upper()}.png",
        REFERENCE_DIR / f"{zone}.jpg",
        REFERENCE_DIR / f"{zone.upper()}.jpg",
        REFERENCE_DIR / f"{zone}.jpeg",
        REFERENCE_DIR / f"{zone.upper()}.jpeg",
    ]
    for p in candidates:
        if p.exists():
            return p
    return None


def rgb_array(path):
    with Image.open(path) as im:
        return np.asarray(im.convert("RGB"), dtype=np.uint8)


def paint_mask(arr, palette):
    if not palette:
        return np.zeros(arr.shape[:2], dtype=bool)

    m = np.zeros(arr.shape[:2], dtype=bool)
    for color in palette:
        c = np.array(color, dtype=np.uint8)
        m |= np.all(arr == c, axis=2)
    return m


def score_crop(reference, painted_crop, palette):
    # Ignore exact authored fill colors; compare only surviving cartography.
    authored = paint_mask(painted_crop, palette)
    valid = ~authored

    # Subsample for speed but keep deterministic coverage.
    valid[1::2, :] = False
    valid[:, 1::2] = False

    count = int(valid.sum())
    if count == 0:
        return 0.0, 0.0

    exact = np.all(reference == painted_crop, axis=2) & valid
    exact_fraction = float(exact.sum()) / count

    # Robust secondary score from absolute RGB error.
    diff = np.abs(reference.astype(np.int16) - painted_crop.astype(np.int16))
    mae = float(diff[valid].mean()) if count else 255.0
    similarity = 1.0 - min(mae / 255.0, 1.0)

    # Exact identity is the strongest signal; similarity breaks ties.
    score = exact_fraction * 0.85 + similarity * 0.15
    return score, exact_fraction


def best_crop(reference, painted, target_w, target_h, palette):
    ph, pw = painted.shape[:2]
    extra_x = pw - target_w
    extra_y = ph - target_h

    if extra_x < 0 or extra_y < 0:
        raise ValueError(
            f"Painted image is smaller than calibrated source: "
            f"{pw}x{ph} vs {target_w}x{target_h}"
        )

    candidates = []

    for y0 in range(extra_y + 1):
        for x0 in range(extra_x + 1):
            crop = painted[y0:y0 + target_h, x0:x0 + target_w]
            score, exact_fraction = score_crop(reference, crop, palette)
            candidates.append((score, exact_fraction, x0, y0))

    candidates.sort(reverse=True)

    best = candidates[0]
    second = candidates[1] if len(candidates) > 1 else None

    return best, second


def main():
    masks = load_masks()

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    RUNTIME_DIR.mkdir(parents=True, exist_ok=True)

    print("Known World subregion canvas repair")
    print("=" * 72)

    repaired = 0
    unchanged = 0

    for zone, meta in masks.items():
        painted_path = PAINTED_DIR / f"{zone}.png"
        if not painted_path.exists():
            raise FileNotFoundError(f"Missing painted map: {painted_path}")

        reference_path = find_reference(zone)
        if reference_path is None:
            raise FileNotFoundError(
                f"Missing calibrated regional reference for {zone.upper()} in {REFERENCE_DIR}"
            )

        reference = rgb_array(reference_path)
        painted = rgb_array(painted_path)

        target_w = meta["width"]
        target_h = meta["height"]

        rh, rw = reference.shape[:2]
        if (rw, rh) != (target_w, target_h):
            raise ValueError(
                f"{zone.upper()} reference has wrong dimensions: "
                f"{rw}x{rh}, expected {target_w}x{target_h}"
            )

        ph, pw = painted.shape[:2]

        print()
        print(f"{zone.upper()}:")
        print(f"  reference: {rw}x{rh}")
        print(f"  painted:   {pw}x{ph}")

        output_path = OUTPUT_DIR / f"{zone}.png"

        if (pw, ph) == (target_w, target_h):
            shutil.copy2(painted_path, output_path)
            print("  already correct; copied unchanged")
            unchanged += 1
        else:
            best, second = best_crop(
                reference,
                painted,
                target_w,
                target_h,
                meta["colors"],
            )

            score, exact_fraction, x0, y0 = best
            second_score = second[0] if second else 0.0
            margin = score - second_score

            print(f"  best crop origin: x={x0}, y={y0}")
            print(f"  exact unchanged-pixel match: {exact_fraction:.3%}")
            print(f"  score margin over second best: {margin:.5f}")

            if exact_fraction < MIN_EXACT_MATCH_FRACTION:
                raise RuntimeError(
                    f"{zone.upper()} alignment confidence too low "
                    f"({exact_fraction:.2%} exact match). Refusing to guess."
                )

            if second and margin < MIN_MARGIN_OVER_SECOND_BEST:
                print(
                    "  WARNING: best alignment is not far ahead of second best.\n"
                    "  The exact-match fraction is still shown above; inspect manually "
                    "  before publishing if this map was heavily painted."
                )

            crop = painted[
                y0:y0 + target_h,
                x0:x0 + target_w
            ]

            Image.fromarray(crop, mode="RGB").save(output_path)
            repaired += 1

        # Backup current runtime version before replacing it.
        runtime_path = RUNTIME_DIR / f"{zone}.png"
        backup_path = BACKUP_DIR / f"{zone}.png"

        if runtime_path.exists() and not backup_path.exists():
            shutil.copy2(runtime_path, backup_path)

        shutil.copy2(output_path, runtime_path)

        # Final verification.
        with Image.open(runtime_path) as check:
            if check.size != (target_w, target_h):
                raise RuntimeError(
                    f"Final runtime image for {zone.upper()} is "
                    f"{check.width}x{check.height}, expected {target_w}x{target_h}"
                )

        print(f"  runtime updated: {runtime_path}")

    print()
    print("=" * 72)
    print(f"Repaired:  {repaired}")
    print(f"Unchanged: {unchanged}")
    print()
    print(f"Corrected authoring copies: {OUTPUT_DIR}")
    print(f"Runtime backups:            {BACKUP_DIR}")
    print()
    print("Next:")
    print(r"  .\gradlew.bat clean build")
    print(r"  .\gradlew.bat runClient")


if __name__ == "__main__":
    main()

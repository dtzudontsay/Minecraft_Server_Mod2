#!/usr/bin/env python3
"""Build the client-only Known World atlas from authored place candidates and runtime rasters.

Run from repository root (Pillow required, already used by geodata tools):
    py tools/geodata/build_known_world_atlas.py

Marker-only test (no geodata raster needed):
    py tools/geodata/build_known_world_atlas.py --markers-only

Does not touch chunk/worldgen data, original CSV, or any existing Minecraft world.
Never silently labels a marker by proximity to a book's named castle.
"""
from __future__ import annotations
import argparse
import csv
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT_ASSETS = ROOT / 'src/main/resources/assets/knownworld'
DEFAULT_SOURCE = ROOT / 'tools/geodata/output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv'
DEFAULT_PAIRS = ROOT / 'tools/geodata/output/authored_places/reconciled_2026-10-17/known_shared_site_keys.csv'
DEFAULT_OVERRIDES = ROOT / 'tools/geodata/input/atlas_name_overrides.csv'
DEFAULT_SW_NAMES = ROOT / 'tools/geodata/sw_marker_name_crosswalk.csv'
DEFAULT_RESEARCH_NAMES = ROOT / 'tools/geodata/research_marker_name_index.csv'
RESEARCH = ROOT / 'docs/research/geography'


def read_csv(path: Path):
    with path.open(newline='', encoding='utf-8-sig') as file:
        return list(csv.DictReader(file))


def clean_research_name(text: str) -> str:
    text = re.sub(r'[*`]', '', text).strip()
    # The first table entry sometimes contains commentary about tentative occupation.
    text = re.split(r'\s+—\s+(?:active|ruined|claimed|legendary|island|inhabited|functioning)', text,
                    maxsplit=1, flags=re.I)[0]
    text = re.sub(r'\s*\(`[^`]+`\)', '', text)
    text = re.sub(r'\s*\([^)]*`[^)]*\)', '', text)
    return text.strip().strip('“”')


def gather_research_names() -> dict[str, str]:
    """Reuse explicitly keyed source research, never guess names for SW source Xs."""
    mapping = {}
    for zone in ['NE', 'NWE', 'SWE', 'SE', 'NEE', 'SEE', 'SI', 'SO']:
        path = RESEARCH / f'KNOWN_WORLD_RESEARCH_2_1_{zone}_298AC.md'
        if not path.exists():
            continue
        row_pattern = re.compile(r'^\|\s*(' + zone + r'-\d{3})\s*\|\s*([^|]+)\|')
        for line in path.read_text(encoding='utf-8').splitlines():
            match = row_pattern.match(line)
            if match and match.group(1) not in mapping:
                mapping[match.group(1)] = clean_research_name(match.group(2))
    return mapping


def make_atlas_image(assets: Path) -> None:
    try:
        from PIL import Image, ImageFilter
    except ImportError as error:
        raise SystemExit('Pillow is required: py -m pip install Pillow') from error

    source = ROOT / 'src/main/resources/assets/knownworld/geodata'
    mask_file = source / 'land_mask.png'
    climate_file = source / 'climate.png'
    if not mask_file.exists():
        raise SystemExit(f'Missing runtime land mask: {mask_file}')

    dimensions = (2048, 1357)
    with Image.open(mask_file) as mask_source:
        # Avoid a 9050x6000 GUI texture, which can exceed GPU limits.
        mask = mask_source.convert('L').resize(dimensions, Image.Resampling.NEAREST)
        land = mask.point(lambda value: 255 if value >= 128 else 0)

    ocean = Image.new('RGB', dimensions, (28, 69, 98))
    land_color = Image.new('RGB', dimensions, (124, 141, 102))
    if climate_file.exists():
        with Image.open(climate_file) as climate_source:
            climate = climate_source.convert('RGB').resize(dimensions, Image.Resampling.NEAREST)
        # The runtime climate layer is actual authored mod geography, not explored chunks.
        land_color = Image.blend(land_color, climate, 0.49)
        del climate
    image = Image.composite(land_color, ocean, land)
    coast = land.filter(ImageFilter.FIND_EDGES)
    image.paste((204, 203, 151), mask=coast.point(lambda value: 180 if value > 90 else 0))
    texture = assets / 'textures/gui/known_world_atlas.png'
    texture.parent.mkdir(parents=True, exist_ok=True)
    image.save(texture, optimize=True)
    print('Atlas texture:', texture, dimensions)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--input', type=Path, default=DEFAULT_SOURCE)
    ap.add_argument('--pairs', type=Path, default=DEFAULT_PAIRS)
    ap.add_argument('--overrides', type=Path, default=DEFAULT_OVERRIDES)
    ap.add_argument('--sw-names', type=Path, default=DEFAULT_SW_NAMES)
    ap.add_argument('--research-names', type=Path, default=DEFAULT_RESEARCH_NAMES)
    ap.add_argument('--markers-only', action='store_true')
    args = ap.parse_args()

    rows = read_csv(args.input)
    if not rows or len({r['id'] for r in rows}) != len(rows):
        raise SystemExit('Empty source or duplicate marker IDs')
    if any(r.get('review_status') == 'FALSE_POSITIVE_REMOVED' for r in rows):
        raise SystemExit('Use the ACTIVE catalogue, not the original detector CSV')
    by_id = {r['id']: r for r in rows}
    # Indexed place names are transcribed from the existing Step 2.1 source tables.
    # The live research docs, when present, may refresh them.
    research = {}
    if args.research_names.exists():
        research.update({r['id']: r['name'] for r in read_csv(args.research_names) if r.get('id') and r.get('name')})
    for key, value in gather_research_names().items():
        research.setdefault(key, value)
    if args.sw_names.exists():
        for record in read_csv(args.sw_names):
            if record.get('id') and record.get('name'):
                research[record['id']] = record['name'].strip()
    pairs = read_csv(args.pairs)
    group_by_marker = {}
    for pair in pairs:
        ids = [v.strip() for v in pair['marker_ids'].split(';') if v.strip()]
        if len(ids) != 2 or any(v not in by_id or v in group_by_marker for v in ids):
            raise SystemExit('Invalid crossmap pair: ' + repr(pair))
        site_key = pair['canonical_place_id'].strip()
        if not site_key:
            raise SystemExit('Pair without canonical_place_id')
        for marker_id in ids:
            group_by_marker[marker_id] = site_key

    # This user-editable file is never overwritten once created.
    if args.overrides.exists():
        overrides = {r['id']: r.get('name', '').strip() for r in read_csv(args.overrides)
                     if r.get('id') and r.get('name', '').strip()}
    else:
        overrides = {}
        args.overrides.parent.mkdir(parents=True, exist_ok=True)
        with args.overrides.open('w', encoding='utf-8', newline='') as file:
            writer = csv.writer(file)
            writer.writerow(['id', 'name'])
            writer.writerows((r['id'], '') for r in rows)
        print('Created name override template:', args.overrides)

    merged = defaultdict(list)
    for row in rows:
        site = group_by_marker.get(row['id'], 'source:' + row['id'])
        merged[site].append(row)
    if len(rows) != 253 or len(merged) != 248:
        print(f'NOTE: active observations={len(rows)}, displayed site groups={len(merged)} '
              '(expect 253/248 with this dataset)')

    output = []
    for site, obs in merged.items():
        names = []
        for item in obs:
            identifier = item['id']
            if identifier in overrides:
                names.append((3, overrides[identifier], 'MANUAL_OVERRIDE'))
            elif item.get('name', '').strip():
                names.append((2, item['name'].strip(), 'CATALOGUE_NAME'))
            elif identifier in research:
                names.append((1, research[identifier], 'RESEARCH_TABLE_PROVISIONAL'))
        names.sort(reverse=True)
        raw = obs[0]  # never average distinct map observations into a fabricated exact point
        name = names[0][1] if names else 'Unlinked ' + raw['id']
        linked = bool(names)
        stable = (group_by_marker.get(raw['id']) or raw.get('canonical_place_id', '').strip()
                  or 'unlinked:' + raw['id'])
        if names and names[0][2] == 'RESEARCH_TABLE_PROVISIONAL':
            status = 'RESEARCH_NAME_NEEDS_REVIEW'
        elif names:
            status = names[0][2] + '_PLACEMENT_PROVISIONAL'
        else:
            status = 'NAME_UNLINKED_PLACEMENT_PROVISIONAL'
        output.append({
            'id': raw['id'], 'name': name, 'named': linked,
            'sources': ','.join(row['id'] for row in obs),
            'masterX': float(raw['master_x']), 'masterY': float(raw['master_y']),
            'placeId': stable, 'status': status
        })
    output.sort(key=lambda item: (item['sources'].split(',')[0].split('-')[0], item['id']))

    target = OUT_ASSETS / 'maps/atlas_markers.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps({'schemaVersion': 1,
                                  'sourceObservations': len(rows),
                                  'siteGroups': len(output),
                                  'buildCoordinatesApproved': 0,
                                  'markers': output},
                                 indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print('Marker resource:', target)
    print('Active source observations:', len(rows))
    print('Displayed site groups:', len(output))
    print('Named in source/research/overrides:', sum(item['named'] for item in output))
    print('Still requiring data join:', sum(not item['named'] for item in output))
    if not args.markers_only:
        make_atlas_image(OUT_ASSETS)

if __name__ == '__main__':
    main()

#!/usr/bin/env python3
r"""Read-only QA of output/authored_places markers against Minecraft's land mask.

Place this file at tools/geodata/audit_authored_places.py, and run from the repo
root with: py tools\geodata\audit_authored_places.py

The script intentionally DOES NOT write settlements, modify source candidates,
resolve names, approve land snaps, or create Minecraft structures.
"""
from __future__ import annotations

import argparse
import csv
import json
import math
from collections import Counter
from pathlib import Path

from PIL import Image

MASTER_WIDTH = 2048
MASTER_HEIGHT = 1357
M_PER_MASTER_PX = 482_803.2 / math.hypot(80.0, -7.5)


def read_candidates(p: Path) -> list[dict]:
    if not p.is_file():
        raise SystemExit(f"Missing marker candidates: {p}")
    with p.open(newline="", encoding="utf-8-sig") as f:
        rows = list(csv.DictReader(f))
    expected = {"id", "zone", "pixel_x", "pixel_y", "master_x", "master_y", "minecraft_x", "minecraft_z", "name", "status"}
    if not rows or not expected.issubset(rows[0]):
        raise SystemExit(f"Unexpected marker_candidates.csv format: {p}")
    if len({r['id'] for r in rows}) != len(rows):
        raise SystemExit("Candidate IDs are duplicated. Stopping.")
    return rows


def get_geo_position(row: dict, width: int, height: int) -> tuple[int, int]:
    # Identical coordinate direction and floor indexing to KnownWorldGeoSampler.
    mx, my = float(row["master_x"]), float(row["master_y"])
    rx, ry = math.floor(mx / MASTER_WIDTH * width), math.floor(my / MASTER_HEIGHT * height)
    return rx, ry


def world_position_from_raster(rx: int, ry: int, width: int, height: int) -> tuple[int, int]:
    # Use center of selected raster cell, not its top-left edge.
    mx = (rx + 0.5) / width * MASTER_WIDTH
    my = (ry + 0.5) / height * MASTER_HEIGHT
    return round((mx - MASTER_WIDTH / 2) * M_PER_MASTER_PX), round((my - MASTER_HEIGHT / 2) * M_PER_MASTER_PX)


def closest_land_point(mask, cx: int, cy: int, width: int, height: int, max_m: float):
    metres_x = MASTER_WIDTH * M_PER_MASTER_PX / width
    metres_y = MASTER_HEIGHT * M_PER_MASTER_PX / height
    dx = math.ceil(max_m / metres_x)
    dy = math.ceil(max_m / metres_y)
    best = None
    # This is a bounded candidate proposal, not a place-location correction.
    for y in range(max(0, cy-dy), min(height-1, cy+dy)+1):
        for x in range(max(0, cx-dx), min(width-1, cx+dx)+1):
            if mask[x,y] < 128:
                continue
            dist = math.hypot((x-cx)*metres_x, (y-cy)*metres_y)
            if dist <= max_m and (best is None or dist < best[0]):
                best = (dist,x,y)
    return best


def main():
    script = Path(__file__).resolve()
    repo = script.parents[2]  # script installed as tools/geodata/audit_authored_places.py
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--input', type=Path, default=repo/'tools/geodata/output/authored_places/marker_candidates.csv')
    ap.add_argument('--land-mask', type=Path, default=repo/'src/main/resources/assets/knownworld/geodata/land_mask.png')
    ap.add_argument('--output-dir', type=Path, default=repo/'tools/geodata/output/authored_places')
    ap.add_argument('--max-snap-metres', type=float, default=15000.0,
                    help='Only PROPOSE a nearby land point within this distance; never auto-approve')
    ap.add_argument('--crossmap-radius-metres', type=float, default=10000.0)
    args=ap.parse_args()
    if args.max_snap_metres < 0 or args.crossmap_radius_metres < 0:
        ap.error('Distances must be nonnegative')
    rows=read_candidates(args.input)
    args.output_dir.mkdir(parents=True,exist_ok=True)

    image = None
    land = None
    w=h=0
    if args.land_mask.is_file():
        image = Image.open(args.land_mask).convert('L')
        w,h=image.size
        land=image.load()
        print(f'Using runtime land mask {w}x{h}: {args.land_mask}')
    else:
        print(f'WARNING: land mask not available at {args.land_mask}; land/water checks skipped.')

    output=[]
    for row in rows:
        mr_x = float(row['master_x'])
        mr_y = float(row['master_y'])
        x,z=int(row['minecraft_x']),int(row['minecraft_z'])
        projected_x=round((mr_x - 1024)*M_PER_MASTER_PX)
        projected_z=round((mr_y - 678.5)*M_PER_MASTER_PX)
        coord_delta=math.hypot(x-projected_x,z-projected_z)
        out=dict(row)
        out.update({
            'coordinate_check': 'CONSISTENT' if coord_delta<=10 else 'PROJECTION_MISMATCH',
            'projection_difference_metres':round(coord_delta,2),
            'land_mask_status':'NOT_CHECKED',
            'mask_pixel_x':'','mask_pixel_y':'','snap_distance_metres':'',
            'snap_proposal_x':'','snap_proposal_z':'',
            'final_approved_x':'','final_approved_z':'',
            'final_approved_y':'','canonical_place_id':'',
            'review_notes':'NOT APPROVED FOR SPAWNING',
        })
        if not (0<=mr_x<MASTER_WIDTH and 0<=mr_y<MASTER_HEIGHT):
            out['land_mask_status']='OUTSIDE_MASTER_MAP'
        elif land is None:
            out['land_mask_status']='NO_LAND_MASK'
        else:
            rx,ry=get_geo_position(row,w,h)
            out['mask_pixel_x']=rx
            out['mask_pixel_y']=ry
            if not(0<=rx<w and 0<=ry<h):
                out['land_mask_status']='OUTSIDE_RASTER'
            elif land[rx,ry]>=128:
                out['land_mask_status']='LAND_AT_RAW_X'
                out['review_notes']='Terrain/slope/structures/naming/duplicates still need validation'
            else:
                near=closest_land_point(land,rx,ry,w,h,args.max_snap_metres)
                if near is None:
                    out['land_mask_status']='WATER_NO_NEARBY_LAND'
                    out['review_notes']='Do not snap across sea; verify identity and island by map'
                else:
                    distance,sx,sy=near
                    proposed_x,proposed_z=world_position_from_raster(sx,sy,w,h)
                    out['land_mask_status']='WATER_SNAP_PROPOSAL_ONLY'
                    out['snap_distance_metres']=round(distance)
                    out['snap_proposal_x']=proposed_x
                    out['snap_proposal_z']=proposed_z
                    out['review_notes']='Verify same island/shore/site; never auto-accept the nearest land'
        output.append(out)

    audit=args.output_dir/'georeference_audit.csv'
    with audit.open('w',newline='',encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=list(output[0]));writer.writeheader();writer.writerows(output)

    pairs=[]
    for i,a in enumerate(output):
        for b in output[i+1:]:
            if a['zone']==b['zone']:
                continue
            dist=math.hypot(int(a['minecraft_x'])-int(b['minecraft_x']),int(a['minecraft_z'])-int(b['minecraft_z']))
            if dist<=args.crossmap_radius_metres:
                pairs.append({'id_a':a['id'],'zone_a':a['zone'],'name_a':a['name'],
                              'id_b':b['id'],'zone_b':b['zone'],'name_b':b['name'],
                              'distance_metres':round(dist),
                              'resolution':'CANDIDATE_ONLY_VERIFY_SAME_PLACE'})
    pairs.sort(key=lambda x:x['distance_metres'])
    dpath=args.output_dir/'crossmap_near_pairs.csv'
    with dpath.open('w',newline='',encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=['id_a','zone_a','name_a','id_b','zone_b','name_b','distance_metres','resolution'])
        writer.writeheader();writer.writerows(pairs)

    summary={
        'candidate_count':len(output),
        'original_names_filled':sum(bool(x['name'].strip()) for x in output),
        'original_statuses':dict(Counter(x['status'] for x in output)),
        'mask_used':str(args.land_mask) if land is not None else None,
        'land_audit':dict(Counter(x['land_mask_status'] for x in output)),
        'coordinate_checks':dict(Counter(x['coordinate_check'] for x in output)),
        'crossmap_near_pair_count':len(pairs),
        'crossmap_pairs_threshold_metres':args.crossmap_radius_metres,
        'warning':'NO LOCATIONS HAVE BEEN APPROVED; snapping suggestions require human map verification. Y not computed.',
    }
    (args.output_dir/'georeference_audit_summary.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(summary,indent=2))
    print('Wrote: ',audit,'\n        ',dpath)

if __name__=='__main__':
    main()

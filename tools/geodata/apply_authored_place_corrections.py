#!/usr/bin/env python3
"""Apply user-reviewed authored places corrections NON-DESTRUCTIVELY.

Usage from D:\\GameOfThronesMod2 (after placing this script beside zone_transforms.py):
    py tools\\geodata\\apply_authored_place_corrections.py

Put KNOWN_WORLD_MARKER_CORRECTIONS_2026-10-17.json beside this script.
Reads tools/geodata/output/authored_places/marker_candidates.csv and (if present)
       tools/geodata/output/authored_places/georeference_audit.csv.
Writes tools/geodata/output/authored_places/reconciled_2026-10-17/...
Does not change original review files, land mask, world terrain or runtime JSON.
"""
from __future__ import annotations
import argparse
import csv
import json
import math
from pathlib import Path
from collections import Counter
from zone_transforms import ZONES

HERE = Path(__file__).resolve().parent
SCALE = 482_803.2 / math.hypot(80.0, -7.5)
ORIGIN_X, ORIGIN_Y = 1024.0, 678.5


def world_from_pixel(zone, px, py):
    mx, my = ZONES[zone.lower()].regional_to_master(float(px), float(py))
    return round(mx, 5), round(my, 5), round((mx - ORIGIN_X) * SCALE), round((my - ORIGIN_Y) * SCALE)


def csv_read(path):
    with path.open(newline="", encoding="utf-8-sig") as fh:
        return list(csv.DictReader(fh))


def csv_write(path, rows, fields):
    with path.open("w", newline="", encoding="utf-8") as fh:
        w=csv.DictWriter(fh, fieldnames=fields, extrasaction="ignore")
        w.writeheader();w.writerows(rows)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--input', type=Path, default=HERE/'output'/'authored_places')
    ap.add_argument('--manifest', type=Path, default=HERE/'KNOWN_WORLD_MARKER_CORRECTIONS_2026-10-17.json')
    ap.add_argument('--output', type=Path, default=None)
    args=ap.parse_args()
    root=args.input
    out=args.output or root/'reconciled_2026-10-17'
    out.mkdir(parents=True, exist_ok=True)
    manifest=json.loads(args.manifest.read_text(encoding='utf-8'))
    rows=csv_read(root/'marker_candidates.csv')
    if len(rows)!=manifest['input_expected_count']:
        raise ValueError(f"Input is {len(rows)} rows, expected {manifest['input_expected_count']}. Refusing to apply to different detector run.")
    ids=[r['id'] for r in rows]
    if len(ids)!=len(set(ids)):
        raise ValueError('Duplicate marker ID found in raw CSV')
    byid={r['id']:r for r in rows}
    exclusion=set(manifest['false_positive_ids'])
    overrides=manifest['user_confirmed_names']
    visual=manifest.get('visually_recovered_names_to_verify', {})
    grouping=manifest['multi_site_group']
    known=set(ids)
    for key in exclusion|set(overrides)|set(grouping)|set(visual):
        if key not in known:
            raise ValueError(f"Manifest refers to non-existent marker {key}")
    if exclusion & (set(overrides)|set(grouping)|set(visual)):
        raise ValueError('A false positive must not also carry a site identity')
    old_audit=root/'georeference_audit.csv'
    audit={x['id']:x for x in csv_read(old_audit)} if old_audit.exists() else {}
    if audit and set(audit)!=set(ids):
        raise ValueError('Audit markers differ from raw input; rerun audit on correct source')
    extra_fields=[
        'canonical_place_id','shared_site_key','group_id','source_name_status',
        'review_status','source_type','land_mask_status','snap_distance_metres',
        'snap_proposal_x','snap_proposal_z','final_approved_x','final_approved_y',
        'final_approved_z','placement_status','review_notes'
    ]
    fields=list(rows[0]); fields+= [x for x in extra_fields if x not in fields]
    result=[];source=[];excluded=[]
    for row in rows:
        row=dict(row)
        marker=row['id']
        check=world_from_pixel(row['zone'],row['pixel_x'],row['pixel_y'])
        if any(abs(float(row[c])-b)> (0.02 if i<2 else 1.5) for i,(c,b) in enumerate(zip(('master_x','master_y','minecraft_x','minecraft_z'),check))):
            raise ValueError(f"Projection mismatch for {marker} — check zone_transforms.py and detector input")
        row.update({
            'canonical_place_id':'','shared_site_key':'','group_id':'',
            'source_name_status':'UNASSIGNED_IN_SOURCE_CSV',
            'review_status':'NEEDS_NAME_LINK','source_type':'DETECTOR',
            'land_mask_status':audit.get(marker,{}).get('land_mask_status','NOT_AUDITED'),
            'snap_distance_metres':audit.get(marker,{}).get('snap_distance_metres',''),
            'snap_proposal_x':audit.get(marker,{}).get('snap_proposal_x',''),
            'snap_proposal_z':audit.get(marker,{}).get('snap_proposal_z',''),
            'final_approved_x':'','final_approved_y':'','final_approved_z':'',
            'placement_status':'NOT_APPROVED',
            'review_notes':audit.get(marker,{}).get('review_notes',''),
        })
        if marker in exclusion:
            row['review_status']='FALSE_POSITIVE_REMOVED'
            row['source_name_status']='NOT_A_REAL_X_USER_CONFIRMED'
            row['review_notes']='User reports line / red name annotation, not a genuine place X.'
            excluded.append(row)
        elif marker in overrides:
            row.update(overrides[marker]);row['source_name_status']='USER_CONFIRMED'
            row['review_status']='IDENTITY_USER_CONFIRMED_COORDINATES_PROVISIONAL'
        elif marker in grouping:
            row.update(grouping[marker]);row['source_name_status']='USER_CONFIRMED_GROUP'
            row['review_status']='IDENTITY_USER_CONFIRMED_COORDINATES_PROVISIONAL'
        elif marker in visual:
            row.update(visual[marker]);row['source_name_status']='IMAGE_READ_ASSISTANT_NEEDS_REVIEW'
            row['review_status']='NAME_SUGGESTED_NOT_APPROVED'
        source.append(row);result.append(row)
    for g in manifest['crossmap_same_site_verified_in_prior_research']:
        for marker in g['ids']:
            if marker not in byid:
                raise ValueError(f'Unknown crossmap id: {marker}')
            row=next(r for r in result if r['id']==marker)
            if row['review_status']=='FALSE_POSITIVE_REMOVED':
                raise ValueError(f'Crossmap marker {marker} was removed')
            row['name']=g['name'];row['canonical_place_id']=g['canonical_place_id']
            row['shared_site_key']=g['canonical_place_id']
            row['source_name_status']='PRIOR_RESEARCH_CROSSMAP_MATCH'
            row['review_status']='CROSSMAP_IDENTITY_LINKED_LOCATION_PENDING'
    for new in manifest['manual_additions']:
        id=new['id'];zone=new['zone']
        if id in byid: raise ValueError(f'Duplicate manual id: {id}')
        check=world_from_pixel(zone,new['pixel_x'],new['pixel_y'])
        row={f:'' for f in fields}
        row.update({'id':id,'zone':zone,'pixel_x':new['pixel_x'],'pixel_y':new['pixel_y'],
                    'master_x':check[0],'master_y':check[1],
                    'minecraft_x':check[2],'minecraft_z':check[3],
                    'detection':'MANUAL_MAP_REVIEW','score':'',
                    'name':new['name'],'status':'NEEDS_REVIEW',
                    'canonical_place_id':new['canonical_place_id'],
                    'shared_site_key':new['canonical_place_id'],
                    'group_id':'',
                    'source_name_status':'USER_CONFIRMED_IDENTITY_NOT_PIXEL',
                    'review_status':'MANUAL_ESTIMATE_NEEDS_PIXEL_CONFIRMATION',
                    'source_type':'ADDED_MISSED_X',
                    'land_mask_status':'NOT_AUDITED',
                    'placement_status':'NOT_APPROVED',
                    'review_notes':new['note']})
        result.append(row)
    active=[r for r in result if r['review_status']!='FALSE_POSITIVE_REMOVED']
    if len(source)!=259 or len(excluded)!=11 or len(result)!=264 or len(active)!=253:
        raise AssertionError(f'Result count mismatch {len(source)},{len(excluded)},{len(result)},{len(active)}')
    assert len(set(r['id'] for r in result))==len(result)
    for r in active:
        if r['id'].startswith('NW-MISSING-WALL-'):
            assert r['land_mask_status']=='NOT_AUDITED'
    place_map={}
    for r in active:
        key=r['canonical_place_id']
        if key:
            place_map.setdefault(key,[]).append(r['id'])
    duplicated={k:v for k,v in place_map.items() if len(v)>1}
    known_cross={x['canonical_place_id'] for x in manifest['crossmap_same_site_verified_in_prior_research']}
    if not known_cross.issubset(duplicated):
        raise AssertionError('Missing expected duplicate reconciliation groups')
    csv_write(out/'marker_catalog_all_with_exclusions.csv',result,fields)
    csv_write(out/'marker_catalog_active_253.csv',active,fields)
    csv_write(out/'excluded_false_positives_11.csv',excluded,fields)
    csv_write(out/'pending_manual_pixel_centres_5.csv',active[-5:],fields)
    csv_write(out/'needs_name_link.csv',[r for r in active if not r['canonical_place_id']],fields)
    summary={
      'raw_candidates':len(source),'false_positives':len(excluded),
      'new_manually_recognized_xs':len(manifest['manual_additions']),
      'active_source_observations':len(active),
      'active_by_submap':dict(sorted(Counter(r['zone'] for r in active).items())),
      'name_link_coverage_by_evidence':dict(Counter(r['source_name_status'] for r in active)),
      'five_confirmed_crossmap_pairs':len(known_cross),
      'current_canonical_identity_groups':len(place_map),
      'other_duplicate_ids_review_not_yet_performed':True,
      'remaining_approved_placement_count':0,
      'existing_land_audit_statuses_of_active':dict(Counter(r['land_mask_status'] for r in active)),
      'new_manual_points_have_no_land_audit':True,
      'warning':'All final_approved_XYZ intentionally empty. Run land audit after integrating five points; do not use as settled spawn coordinates.'
    }
    (out/'reconciliation_summary.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
    with (out/'known_shared_site_keys.csv').open('w',encoding='utf-8',newline='') as fh:
        w=csv.DictWriter(fh,fieldnames=['canonical_place_id','marker_ids','chosen_preferred_marker','notes']);w.writeheader()
        for key,markers in sorted(duplicated.items()):
            w.writerow({'canonical_place_id':key,'marker_ids':';'.join(markers),'chosen_preferred_marker':'',
            'notes':'One site; do not automatically average positions. Select verified source marker + land/terrain footprint.'})
    print(json.dumps(summary,indent=2))
    print('Wrote',out)

if __name__=='__main__':main()

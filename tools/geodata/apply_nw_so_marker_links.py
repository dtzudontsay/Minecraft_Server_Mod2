#!/usr/bin/env python3
"""Non-destructive reapplication of 17 Oct 2026 user-authoritative NW/SO atlas links.
Does not replace detector raw pixels, legal holdings or approved XYZ. Run at repo root.
"""
from pathlib import Path
import csv, json, sys
ROOT = Path(__file__).resolve().parents[2]
INPUT = ROOT / 'tools/geodata/output/authored_places/reconciled_2026-10-17/marker_catalog_active_253.csv'
MANIFEST = ROOT / 'tools/geodata/atlas_user_confirmed_2026-10-17.json'

def main():
    change = json.loads(MANIFEST.read_text('utf8'))
    confirmations = {x['id']:x for x in change['confirmed_markers']}
    with INPUT.open(newline='', encoding='utf-8-sig') as f:
        reader = csv.DictReader(f)
        fields = list(reader.fieldnames)
        entries = list(reader)
    for field in ('atlas_master_x', 'atlas_master_y', 'atlas_coordinate_status', 'atlas_original_name'):
        if field not in fields: fields.append(field)
    changed=[]
    for row in entries:
        if row['id'] in confirmations:
            record=confirmations[row['id']]
            if not row.get('atlas_original_name'):
                row['atlas_original_name']=row.get('name','')
            row.update(name=record['display_name'],canonical_place_id=record['canonical_place_id'],
                       source_name_status='USER_CONFIRMED_OCT_2026',review_status='USER_CONFIRMED_NAME_POSITION_PROVISIONAL',
                       status='IDENTITY_CONFIRMED_PLACEMENT_PENDING')
            changed.append(row['id'])
        if row['id']=='NW-008':
            # Original raw source coords are ALWAYS preserved; atlas placement is *provisional*.
            proposed=change['provisional_anchor_overrides']['NW-008']
            row['atlas_master_x']=str(proposed['master_x'])
            row['atlas_master_y']=str(proposed['master_y'])
            row['atlas_coordinate_status']='USER_DIRECTION_APPROXIMATE_NOT_BUILD_APPROVED'
            row['placement_status']='NOT_APPROVED'
            row['status']='CASTLE_BLACK_ATLAS_ANCHOR_PROVISIONAL'
            changed.append('NW-008:anchor')
    if set(confirmations) - {r['id'] for r in entries}:
        raise SystemExit('Bad marker id(s): '+str(set(confirmations)-{r['id'] for r in entries}))
    if len(entries)!=253:
        raise SystemExit(f'Expected 253 active observations, got {len(entries)}; stop and review before changing source')
    backup=INPUT.with_name('marker_catalog_active_253_BEFORE_2026-10-17_NW_SO.csv')
    if not backup.exists(): backup.write_bytes(INPUT.read_bytes())
    with INPUT.open('w',newline='',encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=fields);writer.writeheader();writer.writerows(entries)
    print('Applied',len(changed),'data updates:',', '.join(changed))
    print('Preserved original raw/master pixel coords and all final approved XYZ. Backup:',backup)
    print('Remaining explicitly UNLINKED NW-049: proposed Strongsong, requires confirmation')

if __name__=='__main__': main()

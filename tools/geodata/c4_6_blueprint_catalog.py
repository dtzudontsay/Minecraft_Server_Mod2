#!/usr/bin/env python3
"""Export all real C3 templates in reproducible alphabetical gallery order."""
from pathlib import Path
import json,csv
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac/architecture_templates_c3_v1.json'
OUT=ROOT/'tools/geodata/output/c4_6_winterfell/C46_ALL_BUILDINGS_REVIEW.csv'

def main():
    j=json.loads(SOURCE.read_text(encoding='utf8'))
    t=sorted(j['templates'],key=lambda x:x['id'])
    if len(t)<43 or len({x['id'] for x in t})!=len(t):raise ValueError('Invalid C3 catalogue')
    OUT.parent.mkdir(parents=True,exist_ok=True)
    fields=['gallery_number','building_id','family','width_blocks','depth_blocks',
            'floors','max_staff','stations','shore_only','mountain_resource_only',
            'policy_required','visual_review','wanted_changes','comment']
    with OUT.open('w',newline='',encoding='utf-8-sig') as f:
        writer=csv.DictWriter(f,fieldnames=fields,delimiter=';');writer.writeheader()
        for i,entry in enumerate(t,1):
            row=dict(gallery_number=i,building_id=entry['id'],family=entry['family'],
                     width_blocks=entry['c1Width'],depth_blocks=entry['c1Depth'],
                     floors=entry['floors'],max_staff=entry['maxStaff'],
                     stations=', '.join(entry['stations']),shore_only=entry['requiresShore'],
                     mountain_resource_only=entry['requiresMountainResource'],
                     policy_required=entry['requiresPolicy'],visual_review='',wanted_changes='',comment='')
            writer.writerow(row)
    print('Gallery blueprint catalogue:',len(t),'building types;',OUT)
if __name__=='__main__':main()

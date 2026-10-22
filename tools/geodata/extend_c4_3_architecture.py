#!/usr/bin/env python3
"""Prepare extra C4.3 C3-compatible physical programs; never alter a world save.

Default reports proposed output under tools/geodata/output/c4_3_jobs.
--apply-resource explicitly replaces existing C3 template JSON after a backup.
Java loader count guard also needs tools/geodata/patch_c4_3_java_guards.py.
"""
import argparse, json, shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SC=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
SOURCE=SC/'architecture_templates_c3_v1.json'
OUT=ROOT/'tools/geodata/output/c4_3_jobs'
# ID -> family, footprint, storeys, staff, stations, geographically restricted
NEW={
 'ARCANE_STUDY':('HALL',15,13,2,6,['LECTERN','BOOKSHELF','CAULDRON'],False),
 'BANK':('HALL',18,15,2,9,['LECTERN','BARREL','BOOKSHELF'],False),
 'BOOKBINDERY':('WORKSHOP',14,12,1,8,['LECTERN','BOOKSHELF','CRAFTING'],False),
 'BREWERY':('WORKSHOP',21,17,2,12,['BARREL','CAULDRON','SMOKER'],False),
 'BRICKWORKS':('WORKSHOP',22,18,1,20,['FURNACE','STONECUTTER','CRAFTING'],False),
 'BUTCHERY':('STORE',14,13,1,8,['SMOKER','BARREL','CRAFTING'],False),
 'CANAL_WORKS':('WORKSHOP',21,13,1,12,['CRAFTING','BARREL'],True),
 'CHANDLERY':('STORE',13,12,1,8,['CRAFTING','CAULDRON','BARREL'],False),
 'CITY_SERVICES':('HALL',19,15,1,18,['BARREL','CRAFTING','LECTERN'],False),
 'ENTERTAINMENT_HALL':('HALL',25,19,2,16,['NOTE_BLOCK','LECTERN','BARREL'],False),
 'FERRY_LANDING':('WORKSHOP',15,12,1,6,['BARREL','CRAFTING'],True),
 'FIGHTING_PIT':('HALL',29,29,1,40,['BARREL','CRAFTING'],True),
 'JEWELER':('STORE',13,12,1,7,['CRAFTING','SMITHING','BARREL'],False),
 'JUSTICE_HALL':('HALL',21,16,2,10,['LECTERN','BARREL'],False),
 'LEATHERWORKS':('WORKSHOP',18,15,1,12,['CAULDRON','CRAFTING','BARREL'],False),
 'POTTERY':('WORKSHOP',18,14,1,10,['FURNACE','CRAFTING','BARREL'],False),
 'SCHOLASTIC_HALL':('HALL',19,16,2,12,['LECTERN','BOOKSHELF','CRAFTING'],False),
 'SHIPYARD':('WORKSHOP',29,22,1,24,['CRAFTING','BARREL','SMITHING'],True),
}

def main():
 p=argparse.ArgumentParser();p.add_argument('--apply-resource',action='store_true');a=p.parse_args()
 root=json.loads(SOURCE.read_text(encoding='utf8'))
 existing={j['id'] for j in root['templates']};base=int(root['templateCount'])
 # Buildings previously declared with zero staff still need caretakers.
 civic_staff={'GRANARY':5,'WAREHOUSE':15,'PUBLIC_BATH':8,'SCHOOL':10,'GUARD_TOWER':6}
 for t in root['templates']:
  if t['id'] in civic_staff and not t.get('maxStaff'):
   t['maxStaff']=civic_staff[t['id']]
 for k,(family,w,d,floors,staff,stations,shore) in NEW.items():
  if k in existing:continue
  root['templates'].append({'id':k,'family':family,'floors':floors,'openAir':False,
      'maxStaff':staff,'requiresShore':shore,'requiresMountainResource':False,
      'requiresPolicy': k in ('FIGHTING_PIT','ARCANE_STUDY'),
      'stations':stations,'sourceEvidence':'MOD_ESTIMATE',
      'c1Width':w,'c1Depth':d,
      'description':'C4.3 generic executable footprint pending job-specific interior refinement'})
 root['templateCount']=len(root['templates'])
 root['generationPolicy']='C2_SELECTED_PILOTS_ONLY'
 root['doesNotAuthoriseNewSites']=True
 OUT.mkdir(parents=True,exist_ok=True)
 content=json.dumps(root,indent=2,ensure_ascii=False)+'\n'
 dest=OUT/'architecture_templates_c4_3_preview.json';dest.write_text(content,encoding='utf8')
 print('Preview',root['templateCount'],'templates, base',base,'at',dest)
 if a.apply_resource:
  backup=SOURCE.with_suffix('.c4_3_before.json')
  if not backup.exists():shutil.copyfile(SOURCE,backup)
  SOURCE.write_text(content,encoding='utf8');print('Updated C3 resource; backup:',backup)
if __name__=='__main__':main()

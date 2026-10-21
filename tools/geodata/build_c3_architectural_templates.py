#!/usr/bin/env python3
"""C3: compile all 43 already-authored C1 modules into runnable architectural programs.

Programs are rendering instructions, not permissions to generate new settlements.
The C2 two-pilot whitelist still determines where any structures are constructed.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'tools/geodata/c1_building_grammars.json'
SCENE=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
OUTPUT=SCENE/'architecture_templates_c3_v1.json'

FAMILY={
 'FARM':'FARMHOUSE','GARDEN':'GARDEN','PASTURE':'PASTURE','FISHERY':'WORKSHOP',
 'FOREST_CAMP':'SHELTER','SMITHY':'WORKSHOP','CARPENTRY':'WORKSHOP',
 'MASONRY':'WORKSHOP','WEAVERY':'WORKSHOP','MINE':'WORKSHOP','MARKET':'HALL',
 'CART_DEPOT':'SHED','DOCK':'SHED','KITCHEN':'SERVICE','SERVICE_HALL':'HALL',
 'INN':'HALL','BARRACKS':'BARRACKS','ADMINISTRATION':'HALL',
 'TEMPLE':'HALL','BAKERY':'SERVICE','ARMORY':'WORKSHOP','MILL':'TOWER',
 'TAVERN':'HALL','STABLE':'STABLE','BROTHEL':'HALL','HEALER':'SERVICE',
 'WELL':'WELL','GRANARY':'STORE','WAREHOUSE':'STORE','GUARD_TOWER':'TOWER',
 'GREAT_HALL':'HALL','KEEP':'KEEP','PUBLIC_BATH':'SERVICE','SCHOOL':'HALL',
 'COURTYARD':'COURTYARD','RITUAL_GROUND':'COURTYARD','MONUMENT':'MONUMENT',
 'RUIN':'RUIN','CAMP':'SHELTER','HOUSE_SMALL':'HOME',
 'HOUSE_URBAN':'URBAN_HOME','HOUSE_MEDIUM':'HOME',
 'TEMPORARY_CAMP':'SHELTER'
}
# These are placed as real working Minecraft blocks; not an economic simulation.
STATIONS={
 'FARM':['COMPOSTER','BARREL','HAY'], 'GARDEN':['COMPOSTER','BARREL'],
 'PASTURE':['HAY','WATER'],'FISHERY':['BARREL','SMOKER'],
 'FOREST_CAMP':['CAMPFIRE','CRAFTING'], 'SMITHY':['BLAST_FURNACE','ANVIL','GRINDSTONE'],
 'CARPENTRY':['CRAFTING','SAW_STONE','BARREL'], 'MASONRY':['STONECUTTER','BARREL'],
 'WEAVERY':['LOOM','BARREL'], 'MINE':['STONECUTTER','BLAST_FURNACE'],
 'MARKET':['BARREL','CRAFTING'], 'CART_DEPOT':['BARREL','CRAFTING'],
 'DOCK':['BARREL','CRAFTING'], 'KITCHEN':['SMOKER','FURNACE','BARREL'],
 'SERVICE_HALL':['SMOKER','BARREL'], 'INN':['SMOKER','BARREL','CRAFTING'],
 'BARRACKS':['SMITHING','BARREL'], 'ADMINISTRATION':['LECTERN','BOOKSHELF'],
 'TEMPLE':['LECTERN','BOOKSHELF'], 'BAKERY':['FURNACE','SMOKER','BARREL'],
 'ARMORY':['SMITHING','GRINDSTONE','BARREL'], 'MILL':['COMPOSTER','BARREL','CRAFTING'],
 'TAVERN':['BARREL','SMOKER'], 'STABLE':['HAY','CAULDRON'],
 'BROTHEL':['BARREL','CRAFTING'], 'HEALER':['BREWING','CAULDRON','BOOKSHELF'],
 'WELL':['WATER'], 'GRANARY':['BARREL','HAY'], 'WAREHOUSE':['BARREL','CHEST'],
 'GUARD_TOWER':['BARREL','CRAFTING'], 'GREAT_HALL':['LECTERN','BARREL'],
 'KEEP':['LECTERN','BARREL','SMITHING'], 'PUBLIC_BATH':['CAULDRON','WATER'],
 'SCHOOL':['LECTERN','BOOKSHELF'], 'COURTYARD':['CRAFTING'],
 'RITUAL_GROUND':['CAMPFIRE'],'MONUMENT':['STONE'], 'RUIN':['STONE'],
 'CAMP':['CAMPFIRE','BARREL'], 'HOUSE_SMALL':['CRAFTING','BARREL'],
 'HOUSE_URBAN':['CRAFTING','BARREL'], 'HOUSE_MEDIUM':['CRAFTING','BARREL'],
 'TEMPORARY_CAMP':['CAMPFIRE','BARREL']
}
FLOORS={'HOUSE_URBAN':2,'HOUSE_MEDIUM':2,'INN':2,'BARRACKS':2,
        'ADMINISTRATION':2,'GREAT_HALL':2,'KEEP':3,'GUARD_TOWER':3,'MILL':2,
        'WAREHOUSE':2,'SCHOOL':2,'SERVICE_HALL':2,'TEMPLE':2}
OPEN={'GARDEN','PASTURE','WELL','COURTYARD','RITUAL_GROUND','MONUMENT','RUIN'}
ALIASES={'HOUSE':'HOUSE_SMALL','FIELD':'GARDEN'}

def build(source):
    modules=source.get('buildingModules',{})
    if len(modules)!=43 or set(modules)!=set(FAMILY) or set(modules)!=set(STATIONS):
        raise ValueError('Expected exactly 43 named C1 modules; do not omit/invent an archetype')
    result=[]
    for kind in sorted(modules):
        c=modules[kind]
        family=FAMILY[kind]
        floors=FLOORS.get(kind,1)
        # Three levels only for keep & military towers. Restrict footprint sizes
        # to the conservative pilot dimensions; later C4 can use grand templates.
        result.append({'id':kind,'family':family,'floors':floors,
            'openAir':kind in OPEN,'maxStaff':int(c['maxStaff']),
            'requiresShore':bool(c.get('requiresShore',False)),
            'requiresMountainResource':bool(c.get('requiresMountainResource',False)),
            'requiresPolicy':bool(c.get('requiresScenarioPolicy', False) or c.get('ownerFactionRequired',False)),
            'stations':STATIONS[kind],
            'sourceEvidence':'INVENTED',
            'c1Width':int(c['w']),'c1Depth':int(c['d']),
            'description':'C3 executable block grammar; C1 footprint and job constraints retained'})
    digest=hashlib.sha256(json.dumps(result,sort_keys=True,separators=(',',':')).encode()).hexdigest()
    return {'schemaVersion':1,'source':'C1_43_MODULES','generationPolicy':'C2_SELECTED_PILOTS_ONLY',
            'doesNotAuthoriseNewSites':True,'templateCount':43,'inputDigest':digest,
            'aliases':ALIASES,'templates':result}

def main():
    p=argparse.ArgumentParser();p.add_argument('--check',action='store_true');args=p.parse_args()
    if not SOURCE.exists():raise SystemExit('C1 source absent. Run C1 installer first')
    data=build(json.loads(SOURCE.read_text(encoding='utf-8-sig')))
    payload=json.dumps(data,ensure_ascii=False,indent=2)+'\n'
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding='utf-8')!=payload:
            raise SystemExit('C3 templates missing or stale; rerun without --check')
        print('C3 CHECK PASSED: 43 canonical executable templates')
    else:
        OUTPUT.parent.mkdir(parents=True,exist_ok=True)
        OUTPUT.write_text(payload,encoding='utf-8')
        print('Created',OUTPUT,'with',len(data['templates']),'templates; no world generation triggered')

if __name__=='__main__':main()

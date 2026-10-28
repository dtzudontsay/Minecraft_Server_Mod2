#!/usr/bin/env python3
"""Strict local B4.1A.1 prerequisite verification; zero GitHub/network writes.

B4.1A.1 replaces KnownWorldB41Activation.java with the packaged version and
reuses the ALREADY installed B4.1A hooks, avoiding unsafe text injection.
"""
from __future__ import annotations
import argparse,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
MAIN=Path('src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java')
GLOBAL=Path('src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java')
CLIENT=Path('src/main/java/dev/dtzudontsay/knownworld/client/casestudy/WinterfellCharacterScreen.java')
ACTOR=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java')
SOURCE_B4=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1/manifest.json')
SOURCE_C4=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/universal_construction_c4_11_global_v1.json')
SOURCE_B2=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac/world_sites_b2_b3_v1.json')

def audit(root):
    root=Path(root)
    missing=[str(p) for p in [MAIN,GLOBAL,CLIENT,ACTOR,SOURCE_B4,SOURCE_C4,SOURCE_B2] if not (root/p).is_file()]
    if missing:raise ValueError('Install C4.11, B4.0 and B4.1A first; missing: '+', '.join(missing))
    main=(root/MAIN).read_text(encoding='utf-8-sig')
    global_source=(root/GLOBAL).read_text(encoding='utf-8-sig')
    ui=(root/CLIENT).read_text(encoding='utf-8-sig')
    runtime=(root/ACTOR).read_text(encoding='utf-8-sig')
    if main.count('KnownWorldB41Activation.register();')!=1:
        raise ValueError('B4.1A onInitialize hook missing or duplicated in KnownWorld.java')
    if 'b41ApprovedPlacementForInspection(' not in global_source:
        raise ValueError('B4.1A C4.11 placement hook missing; rerun B4.1A install first')
    if 'value("id").startsWith("b4:")' not in ui:
        raise ValueError('Existing character UI lacks B4.1A Known World title adjustment')
    if 'world_projection_b4_1a1_v1' not in runtime or 'MAX_ACTORS = 128' not in runtime:
        raise ValueError('Wrong worldwide activation Java file; extract the B4.1A.1 ZIP with overwrite')
    b4=json.loads((root/SOURCE_B4).read_text(encoding='utf-8-sig'))
    c4=json.loads((root/SOURCE_C4).read_text(encoding='utf-8-sig'))
    if not (b4.get('peopleTotal')==3760684 and b4.get('householdsTotal')==799343
            and b4.get('modeledWorkersTotal')==1880341 and b4.get('siteCount')==248):
        raise ValueError('B4.0 source census mismatch')
    if c4.get('siteCount')!=248 or not c4.get('globalEnabled'):
        raise ValueError('C4.11 world construction not enabled')
    return {'status':'PRECHECK_OK','populationSource':'UNMODIFIED','worldgenHook':'PRESERVED',
            'npcRegistration':'PRESERVED','physicalBudget':128}

def main():
    a=argparse.ArgumentParser();a.add_argument('--repo',type=Path,default=ROOT)
    args=a.parse_args();print(json.dumps(audit(args.repo),indent=2))
if __name__=='__main__':main()

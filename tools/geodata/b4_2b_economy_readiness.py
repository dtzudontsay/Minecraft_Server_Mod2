#!/usr/bin/env python3
"""B4.2B *preview only* of coherent 298 AC starting settlement reserves.
Never writes an existing EconomyRuntime inventory, creates coin or causes famine.
Historical reserve coefficients explicitly provisional until agriculture calibrated.
"""
from __future__ import annotations
import argparse,json,os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SC=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
SOURCE=SC/'world_daily_b4_2_v1/manifest.json'
OUT=SC/'world_economy_readiness_b4_2b_v1/manifest.json'

def build(root=ROOT,destination=None):
    repo=Path(root).resolve();out=Path(destination or repo/OUT).resolve()
    doc=json.loads((repo/SOURCE).read_text(encoding='utf8'))
    if doc.get('schemaVersion')!='knownworld.b4_2.daily_worksites.v1' or doc.get('siteCount')!=248 or doc.get('peopleTotal')!=3760684 or doc.get('workersTotal')!=1880341:raise ValueError('Wrong B4.2 source')
    sites=[]
    for s in doc['sites']:
        residents=s['people'];workers=s['workers']
        if residents<0 or workers<0 or workers>residents:raise ValueError('Invalid site workforce')
        # 90 days of *indicative* grain-equivalent reserve at 1kg/person/day.
        # This is intentionally not booked to any real inventory without
        # proving holding allocation, calibrated production and atomic saves.
        sites.append({'siteId':s['siteId'],'residents':residents,'workers':workers,
           'indicativeGrainReserveKg':residents*90,'indicativeFoodDays':90 if residents else 0,
           'requiresHoldingMapping':True,'requiresProductionCalibration':True})
    if len(sites)!=248 or sum(s['residents'] for s in sites)!=3760684 or sum(s['workers'] for s in sites)!=1880341:raise ValueError('Workforce totals changed')
    result={'schemaVersion':'knownworld.b4_2b.economic_readiness.v1',
       'activationMode':'READ_ONLY_NO_TRANSACTIONS','authoritativeEconomicOwner':'EXISTING_ECONOMY_RUNTIME',
       'historicalStartYearAC':298,'baselineReferenceFoodDays':90,'baselineReferenceGramsPerPersonPerDay':1000,
       'initialGrainActuallyCreditedKg':0,'peopleTotal':3760684,'workersTotal':1880341,'siteCount':248,
       'provenance':'B4.2 workplace manifest, illustrative not verified inventory quantities',
       'guardrails':['no invented settlement stock','no duplicate initial endowment','no food consumption before calibrated source and stock','existing inventory journal remains sole resource authority','atomic cross-domain checkpoint required before autonomy'],
       'sites':sites}
    if out.exists():raise FileExistsError('Refusing to replace economic preview')
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
    return {'sites':248,'residents':3760684,'workers':1880341,'readOnly':True}
if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--repo',type=Path,default=ROOT);a.add_argument('--output',type=Path);opt=a.parse_args();print(json.dumps(build(opt.repo,opt.output)))

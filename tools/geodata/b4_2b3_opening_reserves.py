#!/usr/bin/env python3
"""B4.2B.3 298 AC *proposed* opening public-reserve plan for all 248 sites.

Nothing here modifies Minecraft saves, inventories, wages, or households.
Native EconomyRuntime alone may journal an opted-in, validated grant.
"""
from __future__ import annotations
import argparse, hashlib, json, os, re, shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SC=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
POP=SC/'world_population_b4_v1/manifest.json'
STANDING=SC/'world_household_economy_b4_2b2_v1/manifest.json'
OUT=SC/'world_opening_reserves_b4_2b3_v1'
SCHEMA='knownworld.b4_2b3.opening_public_reserves.v1'
IDS=re.compile(r'site_[a-z][a-z0-9_]{1,90}')

def load(path):
    return json.loads(path.read_text(encoding='utf-8'))

def build(root=ROOT, output=None):
    root=Path(root).resolve(); dest=(Path(output) if output else root/OUT).resolve()
    pop=load(root/POP); economic=load(root/STANDING)
    if (pop.get('schemaVersion'),pop.get('siteCount'),pop.get('peopleTotal'),pop.get('householdsTotal'),pop.get('modeledWorkersTotal')) != ('knownworld.b4_0.identity_baseline.v1',248,3760684,799343,1880341):
        raise ValueError('B4.0 source is not the approved 298 AC baseline')
    if (economic.get('schemaVersion'),economic.get('siteCount'),economic.get('populationTotal'),economic.get('householdsTotal'),economic.get('workersTotal'),economic.get('authority')) != ('knownworld.b4_2b2.worldwide_household_standing.v1',248,3760684,799343,1880341,'READ_ONLY_BASELINE_NOT_ECONOMY_RUNTIME_STOCK'):
        raise ValueError('B4.2B.2 economic source is not verified/read-only')
    a={s['siteId']:s for s in pop['sites']};b={s['siteId']:s for s in economic['sites']}
    if len(a)!=248 or len(b)!=248 or set(a)!=set(b):raise ValueError('Global site set mismatch')
    if dest.exists() or dest.with_name(dest.name+'.pending').exists():raise FileExistsError('Opening-reserves plan already exists; no silent overwrite')
    pending=dest.with_name(dest.name+'.pending');pending.mkdir(parents=True)
    sites=[];total={'people':0,'households':0,'workers':0,'grainKg':0,'treasuryCoinUnits':0}
    try:
        for sid in sorted(a):
            p=a[sid];e=b[sid]
            if not IDS.fullmatch(sid):raise ValueError('Invalid site ID: '+sid)
            people=p['personCount'];hh=p.get('householdCount',p.get('households'));workers=p['modeledWorkerCount']
            if min(people,hh,workers)<0 or workers>people or hh>people:
                raise ValueError('Invalid demographic counts: '+sid)
            if (e['people'],e['households'],e['workers'])!=(people,hh,workers):raise ValueError('B4.2B.2 site count disagrees: '+sid)
            # 0.75 kg grain-equivalent / person / day, 90 reserve days;
            # these are explicit tunable assumptions, NOT proof of historic stock.
            grain=people*75*90//100
            coin=people*12+workers*45
            if people and grain<=0:raise ValueError('Nonempty site has no reserve')
            if people==0 and (grain or coin):raise ValueError('Empty site has assets')
            special='PRESERVE_SEPARATE_WINTERFELL_AUTHORITY' if sid=='site_nw_029' else 'UNRESOLVED_CANONICAL_SETTLEMENT_LINK'
            sites.append({'siteId':sid,'population':people,'households':hh,'modeledWorkers':workers,
              'grainKg':grain,'treasuryCoinUnits':coin,'stockPurpose':'PUBLIC_GRANARY_AND_SETTLEMENT_TREASURY_ONLY',
              'individualHouseholdBalancesCreated':False,'canonicalSettlementId':None,
              'automaticCreditAuthorized':False,'startingStockVerification':special,
              'legalBusinessOwnersVerified':False})
            for k,v in [('people',people),('households',hh),('workers',workers),('grainKg',grain),('treasuryCoinUnits',coin)]:total[k]+=v
        if (total['people'],total['households'],total['workers'])!=(3760684,799343,1880341):raise ValueError('Census conservation failure')
        source_hashes={'b40ManifestSHA256':hashlib.sha256((root/POP).read_bytes()).hexdigest(),
                       'b42b2ManifestSHA256':hashlib.sha256((root/STANDING).read_bytes()).hexdigest()}
        manifest={'schemaVersion':SCHEMA,'historicalStartAC':298,'siteCount':248,'peopleTotal':total['people'],
          'householdsTotal':total['households'],'workersTotal':total['workers'],
          'proposedPublicGrainKg':total['grainKg'],'proposedPublicCoinUnits':total['treasuryCoinUnits'],
          'initialReserveDays':90,'grainGramsPerPersonDay':750,'resourceCatalogIds':['grain','coin'],
          'actualGrainCreditedKg':0,'actualCoinCreditedUnits':0,'autorun':False,
          'autonomousProduction':False,'autonomousConsumption':False,'wagesActivated':False,
          'grantScope':'VALIDATED_CANONICAL_SETTLEMENT_PUBLIC_RESERVES_ONLY',
          'authority':'READ_ONLY_PROPOSAL_UNTIL_EXPLICIT_SNAPSHOT_TRANSACTION',
          'sitePlans':sites, 'sourceHashes':source_hashes,
          'warnings':['Starting amounts are scenario assumptions, not previously owned real stock.',
              'EconomicOwner has no canonical B4.0 household mapping yet; no household balances or wages.',
              'Do not use aggregate public reserves as proof households can afford food.',
              'Unresolved site->SettlementId mapping must fail closed.']}
        (pending/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
        pending.rename(dest)
        return {'sites':248,'people':total['people'],'households':total['households'],'proposedGrainKg':total['grainKg'],
                'proposedTreasuryCoinUnits':total['treasuryCoinUnits'],'actualCredited':0,'readOnly':True}
    except BaseException:
        shutil.rmtree(pending,ignore_errors=True)
        raise

def main():
    p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,default=ROOT);p.add_argument('--output',type=Path)
    a=p.parse_args();print(json.dumps(build(a.repo,a.output),indent=2))
if __name__=='__main__':main()

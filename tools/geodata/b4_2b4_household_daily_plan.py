#!/usr/bin/env python3
"""B4.2B.4 worldwide household allocation & daily demand PLANS, not a second ledger.
Source: B4.2B.2 household baseline + B4.2B.3 public reserves.
No grant, no ownership invented, no automatic consumption.
"""
from __future__ import annotations
import argparse, gzip, hashlib, json, math, re, shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SC=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac')
HOUSE=SC/'world_household_economy_b4_2b2_v1'
RESERVES=SC/'world_opening_reserves_b4_2b3_v1/manifest.json'
OUT=SC/'world_household_daily_b4_2b4_v1'
SCHEMA='knownworld.b4_2b4.household_daily_plan.v1'
SITEID=re.compile(r'site_[a-z][a-z0-9_]{1,90}')

def load(p):return json.loads(p.read_text(encoding='utf-8'))
def canonical_json(data):return json.dumps(data,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode('utf-8')
def save_gz(p,data):
 p.parent.mkdir(parents=True,exist_ok=True)
 with p.open('wb') as f:
  with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0,compresslevel=5) as z:z.write(canonical_json(data))
def read_gz(p):
 with gzip.open(p,'rt',encoding='utf-8') as f:return json.load(f)

def build(root=ROOT,output=None):
 root=Path(root).resolve(); target=(Path(output) if output else root/OUT).resolve()
 if target.exists() or target.with_name(target.name+'.pending').exists():raise FileExistsError('B4.2B.4 index exists: refuse overwrite')
 hbase=root/HOUSE;rbase=root/RESERVES
 h=load(hbase/'manifest.json');r=load(rbase)
 if (h.get('schemaVersion'),h.get('siteCount'),h.get('populationTotal'),h.get('householdsTotal'),h.get('workersTotal'),h.get('authority')) != ('knownworld.b4_2b2.worldwide_household_standing.v1',248,3760684,799343,1880341,'READ_ONLY_BASELINE_NOT_ECONOMY_RUNTIME_STOCK'):
  raise ValueError('Unverified B4.2B.2 worldwide census')
 if (r.get('schemaVersion'),r.get('siteCount'),r.get('peopleTotal'),r.get('householdsTotal'),r.get('workersTotal'),r.get('actualGrainCreditedKg'),r.get('actualCoinCreditedUnits')) != ('knownworld.b4_2b3.opening_public_reserves.v1',248,3760684,799343,1880341,0,0):
  raise ValueError('Unverified B4.2B.3 public-stock authority')
 hs={s['siteId']:s for s in h['sites']};rs={s['siteId']:s for s in r['sitePlans']}
 if len(hs)!=248 or len(rs)!=248 or set(hs)!=set(rs):raise ValueError('Site set mismatch')
 target.parent.mkdir(parents=True,exist_ok=True)
 pending=target.with_name(target.name+'.pending');pending.mkdir()
 summary=[];counts={'people':0,'households':0,'workers':0,'indexedHouseholds':0,'indexedPeople':0,'allocatedGrainKg':0,'allocatedCoinUnits':0,'dailyDemandGrams':0,'estimatedDailyWages':0}
 try:
  for i,sid in enumerate(sorted(hs),1):
   if not SITEID.fullmatch(sid):raise ValueError('Unsafe site name')
   site=hs[sid];stock=rs[sid]
   if (site['people'],site['households'],site['workers'])!=(stock['population'],stock['households'],stock['modeledWorkers']):raise ValueError('Site census mismatch '+sid)
   for key,field in [('people','people'),('households','households'),('workers','workers')]:counts[key]+=site[field]
   rows={};weights={};parts=site.get('parts',[])
   if site['status']=='DERIVED_PROVISIONAL_NOT_ECONOMY_AUTHORITY':
    if any(not isinstance(v,int) or not (0<=v<16) for v in parts) or len(set(parts))!=len(parts):raise ValueError('Invalid source partitions')
    for p in parts:
     path=hbase/sid/f'p{p:02d}.json.gz';data=read_gz(path)
     for hid,v in data['households'].items():
      if hid in rows or not hid or v['members']<=0 or v['workers']<0 or v['workers']>v['members'] or not v['headPersonId']:
       raise ValueError('Invalid household '+hid)
      rows[hid]=v
    if len(rows)!=site['households'] or sum(v['members'] for v in rows.values())!=site['people'] or sum(v['workers'] for v in rows.values())!=site['workers']:
     raise ValueError('Projected household census mismatched '+sid)
   elif parts or site['status'] not in ('WINTERFELL_V3_SEPARATELY_AUTHORED_NO_BOOTSTRAP','EMPTY_NO_RESIDENTS'):
    raise ValueError('Unexpected source coverage status '+sid)
   allocation={};ordered=sorted(rows)
   grain_reserved=0;demand=0;wages=0
   # Fourteen days of household pantry allocations TRANSFERRED from public granary if approved.
   # Reserve B4.2B.3's other 76 days; no second grain creation.
   for hid in ordered:
    v=rows[hid];members=int(v['members']);workers=int(v['workers']);prosperity=int(v['prosperityIndex'])
    need=members*750;ration=(need*14+999)//1000
    weight=max(1,members+workers*3+prosperity//100)
    allocation[hid]={'members':members,'workers':workers,'headPersonId':v['headPersonId'],
      'standing':v['standing'],'workplaceOwnership':'UNRESOLVED','nativeHouseholdOwnerId':None,
      'nativeInventoryId':None,'grainAllocationFromPublicKg':ration,
      'dailyGrainNeedGrams':need,'proposedDailyWagesCoinUnits':workers*8,
      'openingCoinTransferUnits':0,'authoritativeTransactions':False}
    weights[hid]=weight;grain_reserved+=ration;demand+=need;wages+=workers*8
   if grain_reserved>stock['grainKg']:raise ValueError('Household grain exceeds public reserves '+sid)
   # Allocate 40% of B4.2B.3 public treasury without inventing extra coin.
   budget=stock['treasuryCoinUnits']*40//100 if allocation else 0
   if budget:
    totalweight=sum(weights.values());used=0
    for hid in ordered:
     amount=budget*weights[hid]//totalweight
     allocation[hid]['openingCoinTransferUnits']=amount;used+=amount
    # Deterministic residual distribution, bounded by household count.
    for hid in ordered[:budget-used]:allocation[hid]['openingCoinTransferUnits']+=1
   if sum(x['openingCoinTransferUnits'] for x in allocation.values())!=budget:raise ValueError('Coin conservation broken')
   if sum(x['grainAllocationFromPublicKg'] for x in allocation.values())!=grain_reserved:raise ValueError('Grain conservation broken')
   partitions=[{} for _ in range(16)]
   for hid,v in allocation.items():partitions[int.from_bytes(hashlib.sha256(hid.encode('utf8')).digest()[:2],'big')%16][hid]=v
   partids=[]
   for pi,records in enumerate(partitions):
    if records:
     save_gz(pending/sid/f'p{pi:02d}.json.gz',{'households':records})
     partids.append(pi)
   report={'siteId':sid,'people':site['people'],'households':site['households'],'workers':site['workers'],
     'indexedHouseholds':len(allocation),'indexedPeople':sum(x['members'] for x in allocation.values()),
     'parts':partids,'householdGrainFromPublicKg':grain_reserved,'publicGrainRemainingKg':stock['grainKg']-grain_reserved,
     'householdCoinFromPublicUnits':budget,'publicCoinRemainingUnits':stock['treasuryCoinUnits']-budget,
     'dailyHouseholdDemandGrams':demand,'estimatedDailyGrossWagesCoinUnits':wages,
     'status':'READ_ONLY_TRANSFERS_REQUIRE_CANONICAL_HOUSEHOLD_INVENTORIES' if allocation else site['status']}
   summary.append(report)
   for k,field in [('indexedHouseholds','indexedHouseholds'),('indexedPeople','indexedPeople'),('allocatedGrainKg','householdGrainFromPublicKg'),('allocatedCoinUnits','householdCoinFromPublicUnits'),('dailyDemandGrams','dailyHouseholdDemandGrams'),('estimatedDailyWages','estimatedDailyGrossWagesCoinUnits')]:counts[k]+=report[field]
   if i%32==0:print(f'B4.2B.4 {i}/248 sites, {counts["indexedHouseholds"]:,} indexed households',flush=True)
  if (counts['people'],counts['households'],counts['workers'])!=(3760684,799343,1880341):raise ValueError('WORLD CENSUS FAILURE')
  m={'schemaVersion':SCHEMA,'siteCount':248,'peopleTotal':counts['people'],'householdsTotal':counts['households'],
    'workersTotal':counts['workers'],'indexedHouseholds':counts['indexedHouseholds'],'indexedPeople':counts['indexedPeople'],
    'unresolvedHouseholds':counts['households']-counts['indexedHouseholds'],
    'proposedGrainTransfersKg':counts['allocatedGrainKg'],'proposedCoinTransfersUnits':counts['allocatedCoinUnits'],
    'estimatedDailyGrainDemandGrams':counts['dailyDemandGrams'],'estimatedDailyGrossWagesCoinUnits':counts['estimatedDailyWages'],
    'dailyTransactionsActivated':False,'actualGrainDebitedKg':0,'actualCoinDebitedUnits':0,
    'publicStockNotDuplicated':True,'sourceB42B2SHA256':hashlib.sha256((hbase/'manifest.json').read_bytes()).hexdigest(),
    'sourceB42B3SHA256':hashlib.sha256(rbase.read_bytes()).hexdigest(),
    'authority':'HOUSEHOLD_TRANSFER_PLANNING_NATIVE_INVENTORY_ONLY',
    'policy':{'householdPantryDays':14,'dailyGrainGramsPerPerson':750,'publicTreasuryToHouseholdsPercent':40,'dailyWageUnitsPerModeledWorker':8},
    'sites':summary}
  (pending/'manifest.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
  pending.rename(target)
  return {**counts,'sites':248,'readOnly':True}
 except BaseException:
  shutil.rmtree(pending,ignore_errors=True);raise

def main():
 p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,default=ROOT);p.add_argument('--output',type=Path)
 args=p.parse_args();print(json.dumps(build(args.repo,args.output),indent=2))
if __name__=='__main__':main()

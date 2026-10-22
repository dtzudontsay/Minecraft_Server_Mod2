#!/usr/bin/env python3
"""Opt-in FULL-DEMAND C4.3 layout for *two* existing pilots only.

Fresh ungenerated chunks only. Does not touch Winterfell V3, Minecraft saves,
or any other settlement. Default --dry-run; --install-resource explicitly
replaces the C4 JSON after creating a backup. Requires C3 architecture additions.

Experiment: no arbitrary home/shop/farm caps; distribute workplaces among homes;
24 * 31x31 field annexes per farmhouse (23,064 m2 visual farm annex), with
strict terrain rejection still authoritative in Java.
"""
from __future__ import annotations
import argparse, collections, copy, json, math, shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SC=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
OUT=ROOT/'tools/geodata/output/c4_3_full_pilots'
C1=SC/'settlement_presets_c1_v1.json';C2=SC/'settlement_construction_c2_v1.json'
C3=SC/'architecture_templates_c3_v1.json'
DEMAND=ROOT/'tools/geodata/output/c4_3_jobs/settlement_job_demand_c4_3_v1.json'
TARGET=SC/'settlement_construction_c4_v1.json'
RURAL={'FARM','GARDEN','PASTURE','FOREST_CAMP','MINE','FISHERY'}
RESTRICTED={'DOCK','FISHERY','MINE','SHIPYARD','FERRY_LANDING','CANAL_WORKS','FIGHTING_PIT','ARCANE_STUDY','BROTHEL'}
# 3 blocks between each 31-block field, 24 plots per farmhouse.
FIELDS_COLUMNS,FIELDS_ROWS=6,4
FIELD_SIDE=31; FIELD_PITCH=34


def read(p):return json.loads(p.read_text(encoding='utf8'))
def rect_intersects(a,b):
 return a['x']<b['x']+b['width'] and b['x']<a['x']+a['width'] and a['z']<b['z']+b['depth'] and b['z']<a['z']+a['depth']

def add(lots,site,kind,x,z,w,d,face='NORTH',capacity=0,staff=0):
 w,d=int(w),int(d)
 if not (5<=w<=64 and 5<=d<=64):raise ValueError('Unsupported plot size: '+kind)
 new={'id':f'{site}_c43_{kind.lower()}_{sum(l["kind"]==kind for l in lots):04d}',
      'kind':kind,'x':int(x),'z':int(z),'width':w,'depth':d,
      'face':face,'capacity':capacity,'staff':staff,
      'householdId':None,'plannedOnlyHouseholdName':kind=='HOUSE'}
 for old in lots:
  if rect_intersects(new,old):raise ValueError('Plot overlap '+new['id']+' / '+old['id'])
 lots.append(new)
 return new

def paint_line(roads,x1,z1,x2,z2,width=3):
 if x1!=x2 and z1!=z2:raise ValueError('No diagonal road')
 rad=width//2
 if x1==x2:
  for z in range(min(z1,z2),max(z1,z2)+1):
   for x in range(x1-rad,x1+rad+1):roads.add((x,z))
 else:
  for x in range(min(x1,x2),max(x1,x2)+1):
   for z in range(z1-rad,z1+rad+1):roads.add((x,z))

def build(site,base,templates):
 if site['siteId'] not in {'site_nw_031','site_sw_023'}:raise ValueError('Not an existing C2 pilot')
 if site['mode']!='GENERAL_COMPACT_SETTLEMENT':raise ValueError('Not a regular pilot')
 sid=site['siteId'];tier=site['tier'];anchor=base['anchor'];ax,az=anchor['x'],anchor['z']
 if sid=='site_nw_029' or tier not in ('SMALL','MEDIUM'):raise ValueError('Winterfell / other site denied')
 planned=collections.Counter();skipped={}
 for kind,p in site['workplaces'].items():
  amount=int(p['buildings'])
  if amount==0:continue
  t=templates.get(kind)
  if kind in RESTRICTED or not t or t['requiresShore'] or t['requiresMountainResource'] or t['requiresPolicy']:
   skipped[kind]={'buildings':amount,'why':'needs site-specific physical policy, resource or architecture approval'}
   continue
  planned[kind]=amount
 # Supplement non-staffed civic units from the existing population-based blueprint.
 source_site=read(C1)
 lookup={p['siteId']:p for p in source_site['physicalSitePresets']}
 c1d=lookup[sid]['buildingDemand']
 for k in ('WELL','GRANARY','GUARD_TOWER'):
  planned[k]=max(planned[k],int(c1d['workplaceCounts'].get(k,0)))
 # Special hall is integrated *within* town rather than left unconnected.
 extras=collections.Counter()
 if tier=='MEDIUM' and templates.get('GREAT_HALL'):extras['GREAT_HALL']=1
 if tier=='MEDIUM' and templates.get('COURTYARD'):extras['COURTYARD']=1
 homes=int(site['housingBuildingsRequired']) # full C4.3 demand: no test ceiling
 if homes<=0:raise ValueError('No housing')
 farmhouses=planned.pop('FARM',0)
 # Approved generic rural modules also join the dedicated outer belt.
 rural_plots=[]
 for k in tuple(planned):
  if k in ('GARDEN','PASTURE','FOREST_CAMP'):
   rural_plots.extend([k]*planned.pop(k))
  elif k in RURAL:
   skipped[k]={'buildings':planned.pop(k),'why':'resource/shore-specific rural zoning required'}
 # Balanced work/home mix, interleaving workplaces in the town instead of one shop strip.
 shops=[]
 for kind,n in planned.items():shops.extend([kind]*n)
 shops.sort(key=lambda k: (k not in ('WELL','MARKET','BAKERY','ADMINISTRATION','SMITHY'),k))
 total=homes+len(shops)
 lanes=max(4,math.ceil(math.sqrt(total/7)))
 columns=math.ceil(total/(lanes*2))
 columns=max(columns,8)
 roadzs=[az+(j-(lanes-1)//2)*80 for j in range(lanes)]
 centx=ax-(columns-1)*34//2
 slots=[]
 for j,z in enumerate(roadzs):
  for i in range(columns):
   x=centx+i*34
   for side in ('SOUTH','NORTH'):slots.append((j,i,side,x,z))
 slots.sort(key=lambda a:(abs(a[0]-(lanes-1)/2),abs(a[1]-(columns-1)/2),a[0],a[1],a[2]))
 # A deterministic, interspersed lottery over sites; fixed priority shops near inner streets.
 from hashlib import sha256
 shops.sort(key=lambda k:(k not in ('WELL','MARKET','BAKERY','ADMINISTRATION'), sha256((sid+k).encode()).hexdigest()))
 types=[]
 # Real structural mix: repeat a shop every ceil(houses/shops) residences.
 spacing=max(2,math.ceil((homes+len(shops))/max(1,len(shops))))
 while homes>0 or shops:
  if shops and (len(types)%spacing==0 or homes==0):types.append(shops.pop(0))
  elif homes>0:types.append('HOUSE');homes-=1
  else:types.append(shops.pop(0))
 if len(types)>len(slots):raise ValueError('Grid has insufficient lots')
 lots=[]
 for slot,kind in zip(slots,types):
  lane,col,side,cx,rz=slot
  t=templates['HOUSE_MEDIUM' if tier=='MEDIUM' else 'HOUSE_SMALL'] if kind=='HOUSE' else templates[kind]
  w,d=int(t['c1Width']),int(t['c1Depth'])
  z=rz+9 if side=='NORTH' else rz-9-d
  add(lots,sid,kind,cx-w//2,z,w,d,side,10 if kind=='HOUSE' and tier=='MEDIUM' else 5 if kind=='HOUSE' else 0,
      0 if kind=='HOUSE' else int(t['maxStaff']))
 roads=set()
 left=centx-19;right=centx+(columns-1)*34+19
 for rz in roadzs:paint_line(roads,left,rz,right,rz,5)
 for x in (left,right):paint_line(roads,x,min(roadzs),x,max(roadzs),3)
 for l in lots:
  doorx=l['x']+l['width']//2
  doorz=l['z']-1 if l['face']=='NORTH' else l['z']+l['depth']
  nearest=min(roadzs,key=lambda rz:abs(rz-doorz))
  paint_line(roads,doorx,doorz,doorx,nearest)
 # Main civic hall near the internal connector, but away from residential slots.
 if extras:
  hallz=max(roadzs)+56
  for k,n in extras.items():
   t=templates[k];w,d=t['c1Width'],t['c1Depth']
   hx=ax+(0 if k=='GREAT_HALL' else 50)
   add(lots,sid,k,hx-w//2,hallz,w,d,'SOUTH',0,int(t['maxStaff']))
   paint_line(roads,hx,hallz+d,hx,hallz+d+8)
   paint_line(roads,hx,hallz+d+8,right,hallz+d+8)
   paint_line(roads,right,max(roadzs),right,hallz+d+8)
 # Farms occupy an OUTER BELT beyond southern district; 24 larger plots per farmhouse.
 farmroad=min(roadzs)-90
 farm_pitch=FIELDS_COLUMNS*FIELD_PITCH+30
 fleft=ax-((farmhouses-1)*farm_pitch)//2
 if farmhouses:
  paint_line(roads,min(left,fleft-10),farmroad,max(right,fleft+(farmhouses-1)*farm_pitch+FIELDS_COLUMNS*FIELD_PITCH),farmroad,5)
  paint_line(roads,right,min(roadzs),right,farmroad,3)
 for i in range(farmhouses):
  cx=fleft+i*farm_pitch
  t=templates['FARM'];w,d=t['c1Width'],t['c1Depth']
  add(lots,sid,'FARM',cx, farmroad-10-d,w,d,'SOUTH',0,int(t['maxStaff']))
  paint_line(roads,cx+w//2,farmroad-10,cx+w//2,farmroad)
  for rz in range(FIELDS_ROWS):
   for rx in range(FIELDS_COLUMNS):
    fx=cx+rx*FIELD_PITCH
    fz=farmroad-70-(rz+1)*FIELD_PITCH
    add(lots,sid,'FIELD',fx,fz,FIELD_SIDE,FIELD_SIDE,'NORTH')
  # Narrow tracks beside fields (not through farmland) and to the town.
  fx=cx+FIELDS_COLUMNS*FIELD_PITCH+4
  paint_line(roads,fx,farmroad,fx,farmroad-70-FIELDS_ROWS*FIELD_PITCH,3)
 # Additional rural orchards, fenced pastures and forest work camps.
 if rural_plots:
  farleft=fleft-80
  for i,kind in enumerate(rural_plots):
   t=templates[kind]
   w,d=int(t['c1Width']),int(t['c1Depth'])
   if kind=='PASTURE':w,d=31,31
   if kind=='GARDEN':w,d=21,21
   rx=farleft-i*38
   rz=farmroad-12-d
   add(lots,sid,kind,rx,rz,w,d,'SOUTH',0,int(t['maxStaff']))
   paint_line(roads,rx+w//2,rz+d,rx+w//2,farmroad,3)
  paint_line(roads,farleft-(len(rural_plots)-1)*38-24,farmroad,max(right,farleft+40),farmroad,5)
  paint_line(roads,right,farmroad,right,min(roadzs),3)
 # Assert no road/plot overlap and no lost worker housing demand.
 used=set()
 for l in lots:
  for x in range(l['x'],l['x']+l['width']):
   for z in range(l['z'],l['z']+l['depth']):used.add((x,z))
 clash=used.intersection(roads)
 if clash:raise ValueError(f'{sid}: {len(clash)} road/plot collisions, refuse to install')
 cxindex=collections.defaultdict(lambda:{'r':set(),'b':set()})
 for x,z in roads:
  cx,cz=x//16,z//16;cxindex[(cx,cz)]['r'].add((z-cz*16)*16+(x-cx*16))
 for i,l in enumerate(lots):
  for cx in range(l['x']//16,(l['x']+l['width']-1)//16+1):
   for cz in range(l['z']//16,(l['z']+l['depth']-1)//16+1):cxindex[(cx,cz)]['b'].add(i)
 new=copy.deepcopy(base);new['lots']=lots
 new['chunks']=[{'cx':x,'cz':z,'r':sorted(v['r']),'b':sorted(v['b'])} for (x,z),v in sorted(cxindex.items())]
 xs=[x for x,z in roads];zs=[z for x,z in roads]
 new['bounds']=[min(min(xs),*(l['x'] for l in lots))-44,max(max(xs),*(l['x']+l['width'] for l in lots))+44,
                min(min(zs),*(l['z'] for l in lots))-44,max(max(zs),*(l['z']+l['depth'] for l in lots))+44]
 n_homes=sum(l['kind']=='HOUSE' for l in lots)
 new['planningPeople']=site['peopleTarget'];new['planningHomesNeeded']=int(c1d['housingBuildingsDemand'])
 new['plannedHomesHere']=n_homes;new['housingBacklogAtLeast']=max(0,new['planningHomesNeeded']-n_homes)
 new['c4']={'phase':'C4_3_FULL_DEMAND_TWO_PILOT_FRESH_WORLD_STRESS','realBuildingsPlanned':len(lots),
            'homes':n_homes,'housingCapacityEstimate':sum(l['capacity'] for l in lots),
            'workplaces':dict(sorted(collections.Counter(l['kind'] for l in lots if l['kind'] not in ('HOUSE','FIELD')).items())),
            'farmFields':sum(l['kind']=='FIELD' for l in lots),
            'unbuiltRestrictedDemands':skipped,'roadsConnectedInPlan':True,
            'roadsTerrainApproved':False,'plotYApproved':False,'actualNpcsCreated':0,
            'actualWorkersAssigned':0,'newWorldOnly':True,'householdNamesNotInvented':True}
 return new

def main():
 p=argparse.ArgumentParser();p.add_argument('--install-resource',action='store_true');a=p.parse_args()
 catalog={p['siteId']:p for p in read(DEMAND)['plans']}
 raw=read(C2);template={t['id']:t for t in read(C3)['templates']}
 for k in ('site_nw_031','site_sw_023'):
  if k not in catalog:raise ValueError('Missing site plan '+k)
 result=copy.deepcopy(raw);result['purpose']='C4_3_TWO_PILOT_FULL_HOUSING_STRESS_NOT_GLOBAL'
 result['pilots']=[build(catalog[p['siteId']],p,template) for p in raw['pilots']]
 if {p['siteId'] for p in result['pilots']}!={'site_nw_031','site_sw_023'}:
  raise ValueError('Only two established pilots permitted')
 result['c4Policy']={'twoOriginalPilotsOnly':True,'preserveWinterfellV3':True,'newChunksOnly':True,
                    'doesNotActivateOtherSites':True,'noNpcCreation':True,
                    'experimentNotMigration':True}
 OUT.mkdir(parents=True,exist_ok=True)
 content=json.dumps(result,ensure_ascii=False,separators=(',',':'))+'\n'
 (OUT/'settlement_construction_c4_3_preview.json').write_text(content,encoding='utf8')
 summary=[{'name':x['name'],'homes':x['c4']['homes'],'fieldTiles':x['c4']['farmFields'],
           'lots':len(x['lots']),'indexedChunks':len(x['chunks']),
           'unbuilt':x['c4']['unbuiltRestrictedDemands']} for x in result['pilots']]
 (OUT/'C4_3_FULL_PILOT_REPORT.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf8')
 print(json.dumps(summary,indent=2))
 if a.install_resource:
  backup=TARGET.with_suffix('.before_c4_3.json')
  if not backup.exists():shutil.copyfile(TARGET,backup)
  TARGET.write_text(content,encoding='utf8')
  print('UPDATED existing two-pilot manifest; backup:',backup)
 else:print('DRY RUN - no active worldgen resource modified.')
if __name__=='__main__':main()

#!/usr/bin/env python3
"""C4.11 worldwide construction manifest: exact C4.9 quotas, tiled C4.10A lots.

All eligible sites are selected automatically; no pilot budget, no physical-count
cap. Existing physical Winterfell and C2 pilot sites are intentionally retained,
not duplicated. Exceptional locations are *attempted* with a documented generic
core fallback; unavailable templates, impossible terrain, and non-buildable modes
are explicit exceptions, never silently removed from census/demand.

--audit: calculate planning demand and write audit to tools/output only.
--install: write ALL tile resources plus index with atomic index publication.
Does not spawn NPCs or modify Minecraft saves.
"""
from __future__ import annotations
import argparse, collections, importlib.util, json, os, shutil, sys, tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SC=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
SOURCE=ROOT/'tools/geodata/output/c4_9_settlement_ledgers/C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json'
ARCH=SC/'architecture_templates_c3_v1.json'
PRESETS=SC/'settlement_presets_c1_v1.json'
PILOT=SC/'settlement_construction_c4_v1.json'
INDEX=SC/'universal_construction_c4_11_global_v1.json'
TILES=SC/'c4_11_global_tiles'
AUDIT=ROOT/'tools/geodata/output/c4_11_global/C4_11_WORLDWIDE_CONSTRUCTION_AUDIT.json'
TILE_SPAN=512
PROTECTED={'site_nw_029','site_nw_031','site_sw_023'}
BLOCKED={'RUIN_OR_LANDMARK_ONLY','UNVERIFIED_NO_CONSTRUCTION','MOBILE_NO_FIXED_FOOTPRINT'}

def load(path):return json.loads(Path(path).read_text(encoding='utf-8-sig'))

def layout_module():
    src=ROOT/'tools/geodata/c4_10_shared_layout.py'
    spec=importlib.util.spec_from_file_location('c410_global_layout',src)
    module=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def physical_tier(site):
    """Preserve canonical tier; supply visually viable provisional core to SPECIALs."""
    tier=site['tier']
    if tier in ('SMALL','MEDIUM','MAJOR'):return tier
    if tier!='SPECIAL':raise ValueError('Unknown physical tier '+tier)
    pop=int(site['populationTarget'])
    if pop>=20000:return 'MAJOR'
    if pop>=1500:return 'MEDIUM'
    return 'SMALL'

def site_record(site,templates,palette,layout,protected,emit_tiles=True):
    sid=site['siteId'];original_tier=site['tier'];mode=site['blueprintMode']
    pop=int(site['populationTarget']);houses=int(site['residentialBuildingsTarget'])
    works=int(site['workplaceBuildingsTarget']); workers=int(site['workingPopulationTarget'])
    assert workers==sum(int(v) for v in site['occupationTargetsNonzero'].values())
    meta={'siteId':sid,'name':site['name'],'tier':original_tier,'blueprintMode':mode,
          'populationTarget':pop,'workingPopulationTarget':workers,
          'householdsTarget':int(site['householdsTarget']),
          'homesTarget':houses,'workplacesTarget':works,'originalX':int(site['mapAnchor']['x']),
          'originalZ':int(site['mapAnchor']['z']),'markerIds':site['markerIds'],
          'physicalTier':None,'palette':palette,'builtNpcCount':0,'status':'',
          'physicalAvailableLots':0,'deferredTemplateLots':0,'tiles':[]}
    if pop<=0:
        meta['status']='UNINHABITED_RUIN_OR_MARKER_NO_CONSTRUCTION'
        return meta,{}
    if sid in protected:
        meta['status']='EXISTING_CONSTRUCTION_PROTECTED_NO_DUPLICATE'
        return meta,{}
    if mode in BLOCKED:
        meta['status']='NON_BUILDABLE_MODE_UNMET_DEMAND'
        return meta,{}
    eff=physical_tier(site)
    meta['physicalTier']=eff
    fallback = original_tier=='SPECIAL' or mode!='GENERAL_COMPACT_SETTLEMENT'
    # C4.10A can plan a SPECIAL site by using its provisional physical core;
    # retain the original tier and mode in source metadata, audit any fallback.
    proxy=dict(site,tier=eff)
    tiles=collections.defaultdict(list)
    lots_seen=0; physical=0; deferred=0; mnx=mnz=10**12; mxx=mxz=-10**12
    def on_lot(lot):
        nonlocal lots_seen,physical,deferred,mnx,mnz,mxx,mxz
        is_green=lot['kind']=='GREEN_POCKET'
        if not is_green:lots_seen+=1
        x,z=int(lot['x']),int(lot['z']);w,d=int(lot['width']),int(lot['depth'])
        mnx=min(mnx,x);mnz=min(mnz,z);mxx=max(mxx,x+w);mxz=max(mxz,z+d)
        available=lot['physicalPermission']=='C3_MODULE_AVAILABLE'
        if not is_green:
            if available:physical+=1
            else:deferred+=1
        if not emit_tiles:return
        # Compact actual Minecraft draw contract; household IDs stay in the
        # C4.9 / C4.10A ledgers and link by the same deterministic lot ID.
        item={'id':lot['lotId'],'kind':lot['kind'],'t':lot['template'],
              'x':x,'z':z,'w':w,'d':d,'f':lot['face'],
              's':int(lot['reservedWorkerSlots']),'r':lot['region'],
              'permission':lot['physicalPermission']}
        if lot.get('grid') is not None:item['g']=lot['grid']
        if not available and not is_green:return
        for tx in range((x-36)//TILE_SPAN,(x+w+35)//TILE_SPAN+1):
            for tz in range((z-36)//TILE_SPAN,(z+d+35)//TILE_SPAN+1):
                tiles[(tx,tz)].append(item)
    summary=layout.plan(proxy,templates,on_lot,emit_feature=on_lot)
    if lots_seen!=houses+works:
        raise ValueError(f'{sid}: construction demand dropped ({lots_seen}!={houses+works})')
    if int(summary['linkedHouseholdReservations'])!=site['householdsTarget']:
        raise ValueError(f'{sid}: household reservations lost')
    meta['physicalAvailableLots']=physical;meta['deferredTemplateLots']=deferred
    meta['greenPocketsReserved']=int(summary.get('greenPocketsReserved',0))
    meta['status']='PROVISIONAL_SPECIAL_GEOMETRY' if fallback else 'GLOBAL_TERRAIN_SURVEY_PENDING'
    meta['fallbackGenericCore']=fallback
    meta['bounds']=[int(min(mnx,meta['originalX']-layout.CORE[eff]['outerWallHalfSpan']-8)),
                    int(max(mxx,meta['originalX']+layout.CORE[eff]['outerWallHalfSpan']+8)),
                    int(min(mnz,meta['originalZ']-layout.CORE[eff]['outerWallHalfSpan']-8)),
                    int(max(mxz,meta['originalZ']+layout.CORE[eff]['outerWallHalfSpan']+8))]
    meta['tiles']=[f'{tx}_{tz}' for tx,tz in sorted(tiles)]
    return meta,tiles

def build(sites,templates,palettes,pilots,save=False,index=INDEX,tiles_root=TILES):
    if len(sites)!=248 or len({x['siteId'] for x in sites})!=248:
        raise ValueError('Expected 248 unique physical sites')
    existing=PROTECTED|{p['siteId'] for p in pilots}
    summaries=[];meta_sites=[]; totals=collections.Counter(); all_tiles=0
    tiles_root=Path(tiles_root);index=Path(index)
    # Prepare tile resources first. Publish the index last, so incomplete runs
    # cannot advertise a half-built global resource. Repeated runs overwrite
    # all tiles for deterministic, idempotent regeneration.
    for site in sites:
        sid=site['siteId']; record,buckets=site_record(site,templates,
            palettes.get(sid,'NORTHERN'),layout_module_cached(),existing,emit_tiles=save)
        meta_sites.append(record)
        totals['physicalSites']+=1
        for key,val in [('people','populationTarget'),('workers','workingPopulationTarget'),
                        ('households','householdsTarget'),('homes','homesTarget'),
                        ('workplaces','workplacesTarget')]:totals[key]+=int(record[val])
        totals['physicalAvailableLots']+=record['physicalAvailableLots']
        totals['deferredTemplateLots']+=record['deferredTemplateLots']
        if record['status'] in ('PROVISIONAL_SPECIAL_GEOMETRY','GLOBAL_TERRAIN_SURVEY_PENDING'):
            totals['activatedSiteAttempts']+=1
        else:totals['notActivatedSites']+=1
        summaries.append({k:record[k] for k in ('siteId','name','tier','physicalTier',
            'blueprintMode','populationTarget','homesTarget','workplacesTarget',
            'physicalAvailableLots','deferredTemplateLots','status')})
        if save:
            dest=tiles_root/sid
            dest.mkdir(parents=True,exist_ok=True)
            written=set()
            for (tx,tz),items in buckets.items():
                path=dest/f'{tx}_{tz}.json';written.add(path.name)
                tmp=path.with_suffix('.json.tmp')
                tmp.write_text(json.dumps({'siteId':sid,'lots':items},separators=(',',':'),ensure_ascii=False),encoding='utf-8')
                tmp.replace(path);all_tiles+=1
            # Eliminate stale tile files after revised planners change footprints.
            for obsolete in dest.glob('*.json'):
                if obsolete.name not in written:obsolete.unlink()
        if len(summaries)%40==0:
            print(f'planned {len(summaries)}/248 sites; emitted tiles={all_tiles}',flush=True)
    totals['greenPocketsReserved']=sum(int(s.get('greenPocketsReserved',0)) for s in meta_sites)
    # Locations are user/data-provided. Do not silently move/rewrite markers to
    # resolve overlap at this offline phase; report all initial bbox conflicts.
    boxes=[s for s in meta_sites if 'bounds' in s]
    overlaps=[]
    for i,a in enumerate(boxes):
        for b in boxes[i+1:]:
            aa=a['bounds'];bb=b['bounds']
            if aa[0]<=bb[1] and bb[0]<=aa[1] and aa[2]<=bb[3] and bb[2]<=aa[3]:
                overlaps.append({'siteA':a['siteId'],'siteB':b['siteId'],
                                 'reason':'SOURCE_LAYOUT_BOUNDING_BOXES_OVERLAP'})
    totals['potentialSiteLayoutCollisions']=len(overlaps)
    report={'schemaVersion':1,'mode':'ACTIVATED_GLOBAL_RESOURCES' if save else 'READ_ONLY_AUDIT',
        'noMinecraftChunksChanged':True,'noNpcsCreated':True,
        'populationAndJobDataAuthoritativeSource':'C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json',
        'totals':dict(totals),'sites':summaries,'potentialOverlaps':overlaps,
        'note':'Physical placement remains terrain-dependent. Source demand never reduced; per-lot failures log in game.'}
    if save:
        payload={'schemaVersion':1,'globalEnabled':True,'noNpcCreation':True,
            'siteCount':len(meta_sites),'tileSize':TILE_SPAN,
            'sites':meta_sites}
        index.parent.mkdir(parents=True,exist_ok=True)
        tmp=index.with_suffix(index.suffix+'.tmp')
        tmp.write_text(json.dumps(payload,separators=(',',':'),ensure_ascii=False)+'\n',encoding='utf-8')
        tmp.replace(index)
    return report

_LAYOUT=None
def layout_module_cached():
    global _LAYOUT
    if _LAYOUT is None:_LAYOUT=layout_module()
    return _LAYOUT

def run(activate=False,source=SOURCE,architecture=ARCH,presets=PRESETS,pilots=PILOT,
        index=INDEX,tiles=TILES,audit=AUDIT):
    sites=load(source)['sites']
    templates={x['id']:x for x in load(architecture)['templates']}
    palette={x['siteId']:x.get('paletteId') or 'NORTHERN'
        for x in load(presets)['physicalSitePresets']}
    existing=load(pilots).get('pilots',[])
    result=build(sites,templates,palette,existing,activate,index,tiles)
    audit=Path(audit);audit.parent.mkdir(parents=True,exist_ok=True)
    audit.write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps({'status':result['mode'],'totals':result['totals'],
        'audit':str(audit),'index':str(index) if activate else 'NOT_WRITTEN'},indent=2))
    return result

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--install',action='store_true',help='WRITE all global tile/index resources')
    p.add_argument('--source',type=Path,default=SOURCE)
    p.add_argument('--architecture',type=Path,default=ARCH)
    p.add_argument('--presets',type=Path,default=PRESETS)
    p.add_argument('--pilots',type=Path,default=PILOT)
    p.add_argument('--index',type=Path,default=INDEX)
    p.add_argument('--tiles',type=Path,default=TILES)
    p.add_argument('--audit',type=Path,default=AUDIT)
    a=p.parse_args();run(a.install,a.source,a.architecture,a.presets,a.pilots,a.index,a.tiles,a.audit)

if __name__=='__main__':main()

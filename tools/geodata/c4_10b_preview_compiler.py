#!/usr/bin/env python3
"""C4.10B safe, explicit new-chunk preview selection / manifest compiler.

Use only with C4.9 and C4.10A on disk. Nothing is activated by a dry-run.
Activates at most 3 explicit *non-Winterfell*, non-C2-pilot ordinary sites;
no partial or capped city manifests, never auto-picks a site.
"""
from __future__ import annotations
import argparse
import importlib.util
import json
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[2]
SC=ROOT/'src/main/resources/data/knownworld/scenarios/agot_298_ac'
SOURCE=ROOT/'tools/geodata/output/c4_9_settlement_ledgers/C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json'
ARCH=SC/'architecture_templates_c3_v1.json'
PRESETS=SC/'settlement_presets_c1_v1.json'
PILOT=SC/'settlement_construction_c4_v1.json'
TARGET=SC/'universal_construction_c4_10b_preview_v1.json'
REPORT=ROOT/'tools/geodata/output/c4_10b_preview/C4_10B_PREVIEW_AUDIT.json'
PROTECTED={'site_nw_029','site_nw_031','site_sw_023'}
MAX_PREVIEW_SITES=3
MAX_PREVIEW_LOTS=12000  # preview-install safety budget, NOT population/building cap


def module():
    p=ROOT/'tools/geodata/c4_10_shared_layout.py'
    s=importlib.util.spec_from_file_location('c410a_shared_layout',p)
    m=importlib.util.module_from_spec(s)
    s.loader.exec_module(m)
    return m


def load(p):return json.loads(Path(p).read_text(encoding='utf-8-sig'))


def boxes_intersect(a,b,pad=0):
    return a[0]-pad<=b[1] and b[0]-pad<=a[1] and a[2]-pad<=b[3] and b[2]-pad<=a[3]


def plan_preview(sites, templates, pilot_sites, selected, palettes=None):
    palettes = palettes or {}
    m=module()
    by_id={s['siteId']:s for s in sites}
    if len(selected)!=len(set(selected)) or not 1<=len(selected)<=MAX_PREVIEW_SITES:
        raise ValueError('Select 1-3 DISTINCT sites explicitly')
    existing={s['siteId']:s for s in pilot_sites}
    manifest=[]; audit=[]; proposed_boxes=[]; total_lots=0
    for sid in selected:
        if sid in PROTECTED or sid in existing:
            raise ValueError(sid+': protected Winterfell or existing C2 pilot')
        if sid not in by_id:raise ValueError('Unknown site '+sid)
        site=by_id[sid]
        if site['tier'] not in m.CORE or site['blueprintMode']!='GENERAL_COMPACT_SETTLEMENT':
            raise ValueError(sid+': not an ordinary approved-mode SMALL/MEDIUM/MAJOR site')
        if site['populationTarget']<=0:raise ValueError(sid+': zero population')
        lots=[]
        summary=m.plan(site,templates,lots.append)
        if summary['status']!='GEOMETRY_PREVIEW_REQUIRES_TERRAIN_APPROVAL':
            raise ValueError(sid+': C4.10A did not return an ordinary layout preview')
        if len(lots)!=site['residentialBuildingsTarget']+site['workplaceBuildingsTarget']:
            raise ValueError(sid+': requested buildings not conserved')
        total_lots+=len(lots)
        if total_lots>MAX_PREVIEW_LOTS:
            raise ValueError('Preview safety budget '+str(MAX_PREVIEW_LOTS)+
                             ' exceeded: choose fewer/smaller sites, NO lots were truncated')
        ax,az=site['mapAnchor']['x'],site['mapAnchor']['z']
        minx=maxx=ax;minz=maxz=az
        outlots=[]; deferred=[]; IDs=set()
        for lot in lots:
            if lot['lotId'] in IDs:raise ValueError(sid+': duplicate lot ID')
            IDs.add(lot['lotId'])
            x,z=lot['x']-ax,lot['z']-az
            outlots.append({'id':lot['lotId'],'kind':lot['kind'],
                'template':lot['template'],'dx':x,'dz':z,'width':lot['width'],
                'depth':lot['depth'],'face':lot['face'],
                'staff':lot['reservedWorkerSlots'],
                'householdReservationIds':lot['householdReservationIds'],
                'physicalPermission':lot['physicalPermission'],
                'region':lot['region']})
            minx=min(minx,lot['x']);maxx=max(maxx,lot['x']+lot['width'])
            minz=min(minz,lot['z']);maxz=max(maxz,lot['z']+lot['depth'])
            if lot['physicalPermission']!='C3_MODULE_AVAILABLE':
                deferred.append({'id':lot['lotId'],'kind':lot['kind'],
                                 'reason':lot['physicalPermission']})
        radius=m.CORE[site['tier']]['outerWallHalfSpan']
        bbox=[min(minx,ax-radius-8),max(maxx,ax+radius+8),
              min(minz,az-radius-8),max(maxz,az+radius+8)]
        for other in pilot_sites:
            old=other['bounds']
            if boxes_intersect(bbox,old,pad=640):
                raise ValueError(sid+': proposed lots overlap pre-existing C2 site '+other['siteId'])
        for oid,ob in proposed_boxes:
            if boxes_intersect(bbox,ob,pad=896):
                raise ValueError(sid+': overlaps selected preview site '+oid)
        # The existing V3 estate is protected even if another site's
        # selected anchor later migrates within C4.9's 384-block preview search.
        winterfell=[-4050173-4200,-4050173+4200,-1976496-4200,-1976496+4200]
        if boxes_intersect(bbox,winterfell,pad=640):
            raise ValueError(sid+': approaches protected Winterfell V3 estate')
        proposed_boxes.append((sid,bbox))
        # Do not auto-approve coordinates. Java will run full central dry/relief
        # preflight at runtime and shift all relative lots as one coherent town.
        manifest.append({'siteId':sid,'name':site['name'],'tier':site['tier'],
             'palette':palettes.get(sid) or site.get('paletteId') or 'NORTHERN',
             'markerIds':site.get('markerIds',[]),'originalX':ax,'originalZ':az,
             'populationTarget':site['populationTarget'],
             'householdsTarget':site['householdsTarget'],
             'workersTarget':site['workingPopulationTarget'],
             'homeCount':site['residentialBuildingsTarget'],
             'workplaceCount':site['workplaceBuildingsTarget'],
             'coreWorkerSlots':site['mainComplex'].get('reservedKeepWorkerSlots',0),
             'lots':outlots})
        audit.append({'siteId':sid,'tier':site['tier'],'name':site['name'],
                      'mapAnchor':site['mapAnchor'],'layoutBoundingBox':bbox,
                      'plannedPhysicalLots':len(lots),
                      'deferredSpecialPhysicalLots':len(deferred),
                      'deferredExamples':deferred[:20],
                      'populationCreated':0,'coreTerrainApproved':False,
                      'safeLotSurveyed':False,'runtimeTerrainStatus':'UNSURVEYED'})
    return {'schemaVersion':1,'previewOnly':True,'worldgenEnabled':True,
            'siteCount':len(manifest),'noNpcCreation':True,
            'terrainApproval':'RUNTIME_C4_9_CORE_PREFLIGHT_AND_PER_LOT_SURVEY',
            'sites':manifest}, {'schemaVersion':1,'activation':'NOT_INSTALLED_YET',
            'sites':audit,'fullSiteDemandsConserved':True}


def run(site_ids,activate=False,source=SOURCE,architecture=ARCH,pilots=PILOT,
        output=TARGET,report=REPORT,presets=PRESETS):
    input_sites=load(source)['sites']
    if len(input_sites)!=248:raise ValueError('C4.9 source needs exactly 248 sites')
    templates={t['id']:t for t in load(architecture)['templates']}
    prior=load(pilots).get('pilots',[])
    palette_records={s['siteId']:s.get('paletteId','NORTHERN')
                     for s in load(presets)['physicalSitePresets']}
    manifest,audit=plan_preview(input_sites,templates,prior,site_ids,palette_records)
    audit['activation']='EXPLICIT_FRESH_CHUNK_PREVIEW_ENABLED' if activate else 'READ_ONLY_DRY_RUN'
    report=Path(report);report.parent.mkdir(parents=True,exist_ok=True)
    report.write_text(json.dumps(audit,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    if activate:
        output=Path(output);output.parent.mkdir(parents=True,exist_ok=True)
        if output.exists():
            import shutil
            backup=output.with_suffix(output.suffix+'.before_c410b.bak')
            if not backup.exists():shutil.copy2(output,backup)
        temp=output.with_name(output.name+'.tmp')
        temp.write_text(json.dumps(manifest,separators=(',',':'),ensure_ascii=False)+'\n',encoding='utf-8')
        temp.replace(output)
    print(json.dumps({'mode':audit['activation'],
        'selected':[v['siteId'] for v in audit['sites']],
        'lots':sum(s['plannedPhysicalLots'] for s in audit['sites']),
        'deferred':sum(s['deferredSpecialPhysicalLots'] for s in audit['sites']),
        'manifest':str(output) if activate else 'NOT_WRITTEN',
        'audit':str(report)},indent=2))
    return audit



def list_candidates(source=SOURCE,pilots=PILOT):
    sites=load(source)['sites']; old={s['siteId'] for s in load(pilots).get('pilots',[])}
    for tier in ('SMALL','MEDIUM','MAJOR'):
        valid=[s for s in sites if s['tier']==tier
               and s['blueprintMode']=='GENERAL_COMPACT_SETTLEMENT'
               and s['populationTarget']>0 and s['siteId'] not in PROTECTED
               and s['siteId'] not in old and
               s['residentialBuildingsTarget']+s['workplaceBuildingsTarget']<=MAX_PREVIEW_LOTS]
        valid.sort(key=lambda s:(s['residentialBuildingsTarget']+s['workplaceBuildingsTarget'],s['siteId']))
        print('\n'+tier+' candidate previews (NOT terrain-approved):')
        for s in valid[:12]:
            print('  '+s['siteId']+'  '+s['name']+' | people='+str(s['populationTarget'])+
                ' | homes='+str(s['residentialBuildingsTarget'])+
                ' | workplaces='+str(s['workplaceBuildingsTarget'])+
                ' | map='+str(s['mapAnchor']['x'])+','+str(s['mapAnchor']['z']))
        if not valid:print('  none under selected-site preview budget')

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--site',action='append',help='Explicit site_id; repeat for up to 3 sites')
    p.add_argument('--list',action='store_true',help='List SMALL/MEDIUM/MAJOR candidate IDs, without activation')
    p.add_argument('--activate',action='store_true',help='WRITE build-resource manifest; FRESH NEW CHUNKS ONLY')
    p.add_argument('--source',type=Path,default=SOURCE)
    p.add_argument('--architecture',type=Path,default=ARCH)
    p.add_argument('--pilots',type=Path,default=PILOT)
    p.add_argument('--output',type=Path,default=TARGET)
    p.add_argument('--report',type=Path,default=REPORT)
    p.add_argument('--presets',type=Path,default=PRESETS)
    a=p.parse_args()
    if a.list:
        if a.activate:p.error('--list cannot activate construction')
        list_candidates(a.source,a.pilots)
    else:
        if not a.site:p.error('select --site ID or --list')
        run(a.site,a.activate,a.source,a.architecture,a.pilots,a.output,a.report,a.presets)

if __name__=='__main__':main()

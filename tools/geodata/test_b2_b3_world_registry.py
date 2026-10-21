"""Standalone B2+B3 generator contract test; builds a 253/259 synthetic corpus.

This test does NOT verify lore or the user's map corrections. It verifies
coverage, exact X/Z preservation, deduplication and forbidden game mutations.
"""
from __future__ import annotations
import csv
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SCRIPT=Path(__file__).with_name('build_b2_b3_world_registry.py')
spec=importlib.util.spec_from_file_location('b2b3',SCRIPT)
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
OVERRIDE=Path(__file__).with_name('b2_b3_tier_overrides.json')


def dump(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8')


def dumpcsv(path,rows):
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('w',newline='',encoding='utf-8') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)


def evidence(value,grade='CONTEXT'):
    return {'value':value,'evidence':grade,'sources':['project:test_fixture']}


def fixture(root):
    override=json.loads(OVERRIDE.read_text(encoding='utf-8'))
    special_ids=sorted(set(override['sites']))
    ids=special_ids+['SW-010','SW-017','SE-028','SO-001']
    ids+=['SYN-%03d'%i for i in range(253-len(ids))]
    assert len(ids)==len(set(ids))==253
    holdings=['north_domain','winterfell_domain','red_keep_domain','new_castle_domain','hightower_domain','kings_landing_domain']
    holdings+=['test_%03d_domain'%i for i in range(259-len(holdings))]
    assert len(set(holdings))==259
    markers=[]; links=[];sites=[];raw_holdings=[];hcandidates=[];hresearch=[];refs=[]
    for i,mid in enumerate(ids):
        geo={'NW-029':'winterfell','SW-046':'kings_landing',
             'SE-028':'vahar','SO-001':'vahar','NW-039':'greywater_watch'}.get(mid,'location_%03d'%i)
        primary='SE-028' if mid=='SO-001' else mid
        name={'NW-029':'Winterfell','SW-046':'King’s Landing','NW-039':'Greywater Watch'}.get(mid,'Test '+mid)
        x,z=i*100-4050173,i*90-1976496
        if mid=='NW-029':x,z=-4050173,-1976496
        markers.append({'id':mid,'zone':mid.split('-')[0],'minecraft_x':x,'minecraft_z':z,'canonical_place_id':geo,
                        'name':name,'review_status':'PENDING','land_mask_status':'LAND_AT_RAW_X','pixel_x':i,'pixel_y':i,
                        'shared_site_key':'','group_id':''})
        links.append({'marker_id':mid,'link_status':'CANDIDATE_EXACT_NAME' if i<10 else 'NOT_LINKED_TO_HOLDING'})
        region='the_north' if mid.startswith('NW') else 'world_parent'
        refs.append({'id':geo,'parentId':region,'displayName':name,'kind':'SETTLEMENT','provenance':'CANON'})
        pop={'min':100,'baseline':500,'max':1500}
        if mid=='SO-001':pop={'min':0,'baseline':0,'max':0}
        kind='CASTLE_AND_TOWN'
        if mid.startswith('NEE-00') and mid in special_ids:kind='MONUMENTAL_FRONTIER_FORT'
        sites.append({'id':mid,'displayName':name,'map':{'observationId':mid,'candidateX':x,'candidateZ':z,
                       'primaryPopulationMarkerId':primary,'countsIndependentResidents':mid==primary},
                'geography':{'geographicId':evidence(geo)},
                'site':{'kind':evidence(kind),'habitationAt298AC':evidence('INHABITED'),
                        'historicalOrPhysicalNotes':evidence('fixture')},
                'politics':{'leadingStructure':evidence('scenario leader unknown'),'regionalInfluences':evidence('fixture')},
                'society':{'population':evidence({**pop,'baselineEvidence':'INVENTED','planningRangeEvidence':'INVENTED'}),
                 'culture':evidence('fixture culture'),'religion':evidence('fixture religion'),
                 'demographyShares':evidence({'adult':100}), 'laborParticipation':evidence(0.5),
                 'occupationsOfWorkers':evidence({'farming':100 if mid!='SO-001' else 0}),
                 'occupationRolesOfWorkers':evidence({'farmer':100 if mid!='SO-001' else 0}),
                 'occupationalArchetype':'rural_north'},'sourceIds':['project:test_fixture']})
    # Synthetic holders, some deliberately share real sites and others unplaced.
    for i,hid in enumerate(holdings):
        geo={'north_domain':'the_north','winterfell_domain':'winterfell','red_keep_domain':'red_keep',
           'new_castle_domain':'new_castle','hightower_domain':'hightower','kings_landing_domain':'kings_landing'}.get(hid,'test_geo_%03d'%i)
        typ='REGION' if hid=='north_domain' else 'CASTLE'
        raw_holdings.append({'id':hid,'name':hid,'type':typ,'worldLocationId':geo,
                             'ownerDynastyId':'house_test' if hid!='north_domain' else 'house_stark'})
        anchors={'winterfell_domain':['NW-029'],'kings_landing_domain':['SW-046']}.get(hid,[])
        hcandidates.append({'holdingId':hid,'worldLocationId':geo,'candidates':[{'markerId':m} for m in anchors]})
        hresearch.append({'id':hid,'worldLocationId':geo,'insideKnownPlace':
                          {'red_keep_domain':'kings_landing','new_castle_domain':'white_harbor',
                           'hightower_domain':'oldtown'}.get(hid),
                          'populationPlanning':evidence({'min':100,'baseline':300,'max':2000}),
                          'regionalCulture':evidence('fixture'),'regionalReligion':evidence('fixture'),
                          'geographicParentId':'the_north'})
    refs.append({'id':'the_north','displayName':'The North','kind':'REALM','parentId':'westeros'})
    refs.append({'id':'kings_landing','displayName':'King’s Landing','kind':'SETTLEMENT','parentId':'world_parent'})
    refs.append({'id':'red_keep','displayName':'Red Keep','kind':'SETTLEMENT','parentId':'kings_landing'})
    sources={
     'marker_catalog':markers,'marker_audit':links,'holding_links':{'holdings':hcandidates},
     'holdings':{'holdings':raw_holdings},'site_research':{'sites':sites},
     'holding_research':{'holdings':hresearch},'location_references':refs,
     'holding_references':[],'regional_influences':[],
     'regional_influence_extensions':[], 'territory_zone_catalog':[],
     'subregion_mask_catalog':[], 'tier_overrides':override}
    for key,value in sources.items():
        target=root/mod.SOURCE[key].relative_to(mod.ROOT)
        if key in ('marker_catalog','marker_audit'):dumpcsv(target,value)
        else:dump(target,value)
    return markers


class TestRegistry(unittest.TestCase):
    def test_worldwide_completeness_and_no_phantom_population(self):
        with tempfile.TemporaryDirectory() as d:
            originals=fixture(Path(d))
            r=mod.build(Path(d))
            self.assertEqual(253,len(r['markers']))
            self.assertEqual(259,len(r['holdings']))
            self.assertEqual(252,len(r['physicalSites'])) # deliberate double observation vahar
            self.assertEqual(253,sum(len(s['markerIds']) for s in r['physicalSites']))
            sourcecoords={m['id']:(int(m['minecraft_x']),int(m['minecraft_z'])) for m in originals}
            for m in r['markers']:
                self.assertEqual(sourcecoords[m['markerId']],(m['workingAnchor']['x'],m['workingAnchor']['z']))
                self.assertFalse(m['workingAnchor']['isBuildApproved'])
            for s in r['physicalSites']:
                self.assertFalse(s['canAutoGenerate'])
                self.assertFalse(s['createResidentsNow'])
            self.assertGreaterEqual(r['summary']['referencePlaceCount'],250)
            self.assertTrue(any(x['referenceId']=='winterfell' for x in r['referencePlaces']))
            self.assertEqual(0,r['summary']['generationApprovedCount'])
            self.assertGreaterEqual(r['summary']['regionProfileCount'],1)
            self.assertTrue(any(x['regionId']=='the_north' for x in r['regions']))
            m={m['markerId']:m for m in r['markers']}
            self.assertEqual(m['SE-028']['siteId'],m['SO-001']['siteId'])
            hs={x['holdingId']:x for x in r['holdings']}
            self.assertEqual('NO_SETTLEMENT',hs['north_domain']['tier'])
            self.assertEqual('MAJOR',hs['winterfell_domain']['tier'])
            self.assertEqual('kings_landing',hs['red_keep_domain']['parentCityGeographicId'])
            self.assertFalse(hs['red_keep_domain']['hasIndependentPopulation'])
            for i in range(5,10):
                site=next(s for s in r['physicalSites'] if s['primaryMarkerId']=='NEE-00'+str(i))
                self.assertEqual('SPECIAL',site['tier'])
                self.assertEqual('FIVE_FORTS_MONUMENTAL',site['archetype'])
            rows=mod.export_rows(r)
            self.assertEqual(253,len(rows[0]));self.assertEqual(259,len(rows[1]))


if __name__=='__main__':unittest.main()

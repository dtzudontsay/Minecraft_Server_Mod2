"""C4 data-only tests. No Minecraft bootstrap and no generated world needed."""
from __future__ import annotations
import collections
import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

HERE = Path(__file__).resolve().parent

def load(name):
    spec=importlib.util.spec_from_file_location(name,HERE / (name+'.py'))
    mod=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

layout=load('build_c4_settlement_layouts')
installer=load('install_c4_settlement_layouts')


def fixture():
    # Uses the complete real 43-template catalog when available on disk.
    catalog=layout.read(layout.C3)
    def site(sid,tier,marker,x,z):
        demand={'housingBuildingsDemand':170 if tier=='MEDIUM' else 60,
                'peopleTarget':1300 if tier=='MEDIUM' else 280,
                'firstPhaseWorkplaceCounts':{'WELL':1,'GRANARY':1,'BAKERY':2,
                    'MARKET':1,'SMITHY':1,'GUARD_TOWER':1},
                'workplaceCounts':{'FARM':4 if tier=='MEDIUM' else 2}}
        return {'siteId':sid,'tier':tier,'blueprintMode':'GENERAL_COMPACT_SETTLEMENT',
                'workingAnchor':{'x':x,'z':z},'markerIds':[marker],'buildingDemand':demand}
    small=site('pilot_small','SMALL','NW-031',-3454784,-1589008)
    medium=site('pilot_medium','MEDIUM','SW-023',-1100800,-760000)
    c1={'summary':{'markerCount':253,'holdingCount':259,'physicalSiteCount':2},
        'physicalSitePresets':[small,medium]}
    def pilot(s, name):
        return {'siteId':s['siteId'],'tier':s['tier'],'name':name,
                'mode':s['blueprintMode'],'anchor':s['workingAnchor'],
                'markerIds':s['markerIds'],'palette':'NORTHERN',
                'bounds':[0,0,0,0],'lots':[],'chunks':[],
                'housingBacklogAtLeast':0}
    c2={'schemaVersion':1,'policy':{'pilotsOnly':True,
              'doNotModifyExistingChunks':True,'noNpcCreation':True,
              'noOther253SiteActivation':True,'winterfellExcluded':True},
        'summary':{'pilotCount':2,'allMapMarkers':253,'allPoliticalHoldings':259,
                   'pilotMarkers':[['NW-031'],['SW-023']]},
        'pilots':[pilot(small,'Ramsgate'),pilot(medium,'Acorn Hall')]}
    return c1,c2,catalog


class LayoutTests(unittest.TestCase):
    def test_two_pilots_are_retained_with_exact_coordinates(self):
        c1,c2,c3=fixture(); out=layout.build(c1,c2,c3)
        self.assertEqual([p['siteId'] for p in out['pilots']],
                         [p['siteId'] for p in c2['pilots']])
        self.assertEqual([p['anchor'] for p in out['pilots']],
                         [p['anchor'] for p in c2['pilots']])
        self.assertTrue(out['policy']['pilotsOnly'])
        self.assertTrue(out['c4Policy']['winterfellUntouched'])

    def test_plan_is_reproducible(self):
        c1,c2,c3=fixture()
        a=json.dumps(layout.build(c1,c2,c3),sort_keys=True)
        b=json.dumps(layout.build(c1,c2,c3),sort_keys=True)
        self.assertEqual(a,b)

    def test_no_lot_collisions_or_road_overlaps_and_connected(self):
        c1,c2,c3=fixture(); out=layout.build(c1,c2,c3)
        for p in out['pilots']:
            roofs=set()
            for l in p['lots']:
                pixels={(x,z) for x in range(l['x'],l['x']+l['width'])
                        for z in range(l['z'],l['z']+l['depth'])}
                self.assertFalse(roofs & pixels)
                roofs.update(pixels)
            roads={(c['cx']*16+(r%16),c['cz']*16+(r//16))
                   for c in p['chunks'] for r in c['r']}
            self.assertFalse(roads & roofs)
            self.assertTrue(p['c4']['roadsConnectedInPlan'])
            self.assertGreater(len(roads),100)

    def test_building_staff_are_caps_not_people(self):
        c1,c2,c3=fixture(); out=layout.build(c1,c2,c3)
        for p in out['pilots']:
            self.assertEqual(p['c4']['actualWorkersAssigned'],0)
            self.assertEqual(p['c4']['actualNpcsCreated'],0)
            self.assertEqual(p['housingBacklogAtLeast'],
                max(0,p['planningHomesNeeded']-p['plannedHomesHere']))
            self.assertEqual(sum(l['kind']=='FARM' for l in p['lots']),
                             p['c4']['farmFields'])
            self.assertEqual(sum(l['kind']=='FIELD' for l in p['lots']),
                             p['c4']['farmFields'])

    def test_special_sites_and_winterfell_cannot_be_selected(self):
        c1,c2,c3=fixture()
        c2['pilots'][0]['markerIds']=['NW-029']
        with self.assertRaises(ValueError):layout.build(c1,c2,c3)
        c1,c2,c3=fixture();c1['physicalSitePresets'][0]['blueprintMode']='MONUMENTAL_FORTRESS_REQUIRE_AUTHORED_BLUEPRINT'
        with self.assertRaises(ValueError):layout.build(c1,c2,c3)

    def test_large_buildings_and_plaza_still_fit_lots(self):
        c1,c2,c3=fixture()
        c1['physicalSitePresets'][1]['buildingDemand']['firstPhaseWorkplaceCounts'].update(
            {'BARRACKS':7,'COURTYARD':1,'MARKET':8,'ADMINISTRATION':5})
        out=layout.build(c1,c2,c3)
        medium=out['pilots'][1]
        self.assertEqual(sum(l['kind']=='BARRACKS' for l in medium['lots']),7)
        self.assertEqual(sum(l['kind']=='COURTYARD' for l in medium['lots']),1)
        self.assertTrue(medium['c4']['roadsConnectedInPlan'])

    def test_chunk_indexes_cover_each_building(self):
        c1,c2,c3=fixture();out=layout.build(c1,c2,c3)
        for p in out['pilots']:
            by_chunk={(q['cx'],q['cz']):set(q['b']) for q in p['chunks']}
            for i,l in enumerate(p['lots']):
                self.assertTrue(all(i in by_chunk.get((cx,cz),set())
                    for cx in range(l['x']//16,(l['x']+l['width']-1)//16+1)
                    for cz in range(l['z']//16,(l['z']+l['depth']-1)//16+1)))

    def test_does_not_mutate_inputs(self):
        c1,c2,c3=fixture();old=copy.deepcopy(c2)
        layout.build(c1,c2,c3)
        self.assertEqual(c2,old)

    def test_guarded_java_patch_is_idempotent_and_does_not_touch_other_sources(self):
        snippet=('public final class KnownWorldUniversalWorldgenC2 {\n'
                 ' String x="settlement_construction_c2_v1.json";\n'
                 ' // C2 pilot manifest invalid\n'
                 ' void paint() { '+installer.OLD_CALL+' }\n }\n')
        with tempfile.TemporaryDirectory() as td:
            path=Path(td)/'C2.java';path.write_text(snippet,encoding='utf-8')
            with patch.object(installer,'JAVA',path),patch.object(installer,'BACKUP',Path(td)/'backups'):
                installer.integrate();once=path.read_text()
                installer.integrate();installer.integrate(check=True)
                self.assertEqual(path.read_text(),once)
                self.assertIn(installer.NEW_CALL,once)
                self.assertIn(installer.NEW_RESOURCE,once)


if __name__=='__main__':unittest.main()

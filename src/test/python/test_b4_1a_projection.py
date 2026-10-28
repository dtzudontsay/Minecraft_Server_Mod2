from __future__ import annotations
import gzip, json, sys, tempfile, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'tools/geodata'))
import b4_1a_projection_index as mod

class B41ProjectionTests(unittest.TestCase):
    def test_house_join_keeps_exact_person(self):
        with tempfile.TemporaryDirectory() as td:
            path=Path(td)/'people.gz'
            records=[
                {'siteId':'site_ne_001','personId':'b4:site_ne_001:00000001',
                 'homeLotId':'site_ne_001:home:0000001','displayName':'Edric','sex':'MALE',
                 'birthYearAC':260,'householdId':'site_ne_001:hh:000001'},
                {'siteId':'site_ne_001','personId':'b4:site_ne_001:00000002',
                 'homeLotId':'site_ne_001:home:0000001','displayName':'Alys','sex':'FEMALE',
                 'birthYearAC':265,'householdId':'site_ne_001:hh:000001'},
                {'siteId':'site_ne_001','personId':'b4:site_ne_001:00000003',
                 'homeLotId':'site_ne_001:home:0000001','displayName':'Kid','sex':'FEMALE',
                 'birthYearAC':295,'householdId':'site_ne_001:hh:000001'},
                {'siteId':'site_ne_001','personId':'b4:site_ne_001:00000004',
                 'homeLotId':'site_ne_001:home:0000002','displayName':'Blocked','sex':'MALE',
                 'birthYearAC':275,'householdId':'site_ne_001:hh:000002'}]
            with gzip.open(path,'wt',encoding='utf8') as w:
                for p in records:w.write(json.dumps(p)+'\n')
            rows=[]
            result=mod.index_people(path,'site_ne_001',{'site_ne_001:home:0000001':(123,456,9,10)},rows.append)
            self.assertEqual(result['personsIndexed'],2)
            self.assertEqual(result['homesRepresented'],1)
            self.assertEqual([x['personId'] for x in rows],['b4:site_ne_001:00000001','b4:site_ne_001:00000002'])
            self.assertEqual((rows[0]['x'],rows[0]['z']),(123,456))
    def test_dedupe_tiles_and_reject_mismatch(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td);(root/'site_ne_001').mkdir()
            site={'siteId':'site_ne_001','tiles':['0_0','1_0']}
            item={'id':'site_ne_001:home:0000001','kind':'HOUSE','x':10,'z':10,'w':8,'d':9,'permission':'C3_MODULE_AVAILABLE'}
            for name in site['tiles']:
                (root/'site_ne_001'/f'{name}.json').write_text(json.dumps({'siteId':'site_ne_001','lots':[item]}))
            self.assertEqual(len(mod.houses_for_site(root,site)),1)
            other=dict(item,x=11)
            (root/'site_ne_001'/'1_0.json').write_text(json.dumps({'siteId':'site_ne_001','lots':[other]}))
            with self.assertRaisesRegex(ValueError,'inconsistent duplicate'):mod.houses_for_site(root,site)
    def test_legitimate_descriptive_site_id(self):
        self.assertTrue(mod.SITE_RE.fullmatch('site_nw_missing_twins_01'))
    def test_protected_anchors(self):
        self.assertIn('site_nw_029',mod.PROTECTED)
        self.assertIn('site_nw_031',mod.PROTECTED)
        self.assertIn('site_sw_023',mod.PROTECTED)
    def test_deep_case_duplicate_person_is_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            f=Path(td)/'dup.gz'
            entry={'siteId':'site_ne_001','personId':'b4:site_ne_001:00000001','homeLotId':'A',
                   'displayName':'A','sex':'MALE','birthYearAC':260,'householdId':'H'}
            with gzip.open(f,'wt') as w:
                w.write(json.dumps(entry)+'\n'+json.dumps(entry)+'\n')
            with self.assertRaisesRegex(ValueError,'Duplicate projected person'):
                mod.index_people(f,'site_ne_001',{'A':(0,0,10,10)},lambda x:None)

if __name__=='__main__':unittest.main()

class B41FullManifestTest(unittest.TestCase):
    def test_build_248_site_metadata_and_real_projection(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td)
            paths=[mod.B4+'/manifest.json',mod.C4]
            for path in paths:(root/path).parent.mkdir(parents=True,exist_ok=True)
            siteids=['site_ne_001','site_nw_029']+[f'site_xz_{i:03d}' for i in range(246)]
            b4sites=[];global_sites=[]
            for sid in siteids:
                selected=sid=='site_ne_001';winter=sid=='site_nw_029'
                b4sites.append({'siteId':sid,'personCount':1 if selected else 5000 if winter else 0})
                global_sites.append({'siteId':sid,'name':sid,
                  'status':'GLOBAL_TERRAIN_SURVEY_PENDING' if selected else 'EXISTING_CONSTRUCTION_PROTECTED_NO_DUPLICATE',
                  'originalX':100,'originalZ':200,'bounds':[80,120,180,220],
                  'tiles':['0_0'] if selected else []})
            (root/mod.B4/'manifest.json').write_text(json.dumps({'schemaVersion':'knownworld.b4_0.identity_baseline.v1',
                'siteCount':248,'peopleTotal':3760684,'sites':b4sites}))
            (root/mod.C4).write_text(json.dumps({'schemaVersion':1,'globalEnabled':True,'siteCount':248,'sites':global_sites}))
            tileroot=root/mod.TILE/'site_ne_001';tileroot.mkdir(parents=True)
            tileroot.joinpath('0_0.json').write_text(json.dumps({'siteId':'site_ne_001','lots':[
                {'id':'site_ne_001:home:0000001','kind':'HOUSE','x':110,'z':210,'w':9,'d':10,
                 'permission':'C3_MODULE_AVAILABLE'}]}))
            person={'siteId':'site_ne_001','personId':'b4:site_ne_001:00000001',
                 'homeLotId':'site_ne_001:home:0000001','displayName':'Edric','sex':'MALE',
                 'birthYearAC':260,'householdId':'site_ne_001:hh:000001'}
            with gzip.open(root/mod.B4/'site_ne_001.jsonl.gz','wt') as w:w.write(json.dumps(person)+'\n')
            result=mod.build(root)
            self.assertEqual(result['sourceSiteCount'],248)
            self.assertEqual(result['projectionCandidateCount'],1)
            self.assertEqual(result['uniqueHousesRepresented'],1)
            with gzip.open(root/mod.OUT/'site_ne_001.jsonl.gz','rt') as f:
                self.assertEqual(json.loads(f.readline())['personId'],person['personId'])
            self.assertFalse((root/mod.OUT/'site_nw_029.jsonl.gz').exists())

from __future__ import annotations
import gzip,json,sys,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'tools/geodata'))
import b4_1a1_household_index as mod

class Households(unittest.TestCase):
    def test_every_member_of_same_home_is_indexed(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);persons=root/'people.gz';lot='site_ne_001:home:0000001'
            with gzip.open(persons,'wt',encoding='utf8') as w:
                for i in range(8):
                    w.write(json.dumps({'siteId':'site_ne_001','personId':f'b4:site_ne_001:{i+1:08}',
                        'homeLotId':lot,'givenName':'Alys','householdName':'Brook',
                        'householdId':'site_ne_001:hh:000001','sex':'FEMALE','birthYearAC':260,
                        'cultureId':'riverlander'})+'\n')
            n,h,tiles=mod.write_people(persons,'site_ne_001',{lot:(30,40,11,12)},root/'tiles',max_open=2)
            self.assertEqual((n,h), (8,1))
            self.assertEqual(sum(tiles.values()),8)
            records=[json.loads(s) for s in (root/'tiles'/'0_0.jsonl').read_text().splitlines()]
            self.assertEqual(len(records),8)
            self.assertEqual({x['householdId'] for x in records},{'site_ne_001:hh:000001'})
            self.assertEqual(len({x['personId'] for x in records}),8)
            self.assertEqual(len({x['name'] for x in records}),1)
            self.assertNotIn(' of ',records[0]['name'])
    def test_descriptive_site_ids_allowed(self):
        self.assertTrue(mod.safe_id('site_nw_missing_twins_01'))
        self.assertFalse(mod.safe_id('site_../evil'))
    def test_protect_original_3_pilots(self):
        self.assertEqual(mod.PROTECTED,{'site_nw_029','site_nw_031','site_sw_023'})
    def test_tile_negative_coords_and_no_collision(self):
        self.assertEqual(mod.tile_key((-513,-510,9,11)),'-1_-1')
        self.assertEqual(mod.tile_key((512,512,10,10)),'1_1')
    def test_c4_tile_duplicate_id_must_match(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);s=root/'site_ne_001';s.mkdir()
            item={'kind':'HOUSE','permission':'C3_MODULE_AVAILABLE','id':'h','x':10,'z':9,'w':9,'d':9}
            for tile in ['0_0','1_0']:(s/(tile+'.json')).write_text(json.dumps({'siteId':'site_ne_001','lots':[item]}))
            self.assertEqual(len(mod.available_houses(root,{'siteId':'site_ne_001','tiles':['0_0','1_0']})),1)
            item=dict(item,x=11);(s/'1_0.json').write_text(json.dumps({'siteId':'site_ne_001','lots':[item]}))
            with self.assertRaisesRegex(ValueError,'Conflicting repeat'):mod.available_houses(root,{'siteId':'site_ne_001','tiles':['0_0','1_0']})
    def test_culture_specific_naming_and_household_stability(self):
        p={'personId':'b4:site_ne_001:00000001','householdId':'same','cultureId':'yi_tish','givenName':'Zhi'}
        self.assertEqual(mod.family_name(p,'Lan'),'Lan Zhi')
        w=dict(p,cultureId='northman',givenName='Jon')
        x=mod.family_name(w,'Pine');self.assertEqual(x,mod.family_name(w,'Pine'))
        self.assertFalse(x.startswith('Jon of '))
    def test_duplicate_person_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);p={'siteId':'site_ne_001','personId':'x','homeLotId':'h','householdId':'hh','birthYearAC':270,'sex':'MALE'}
            with gzip.open(root/'p.gz','wt') as w:
                w.write(json.dumps(p)+'\n'+json.dumps(p)+'\n')
            with self.assertRaisesRegex(ValueError,'Duplicate person'):
                mod.write_people(root/'p.gz','site_ne_001',{'h':(1,2,3,4)},root/'tiles')
    def test_manifest_248_and_winterfell_preserved(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            ids=['site_ne_001','site_nw_029']+[f'site_xz_{i:03}' for i in range(246)]
            bs=[];gs=[];refs=[]
            for sid in ids:
                selected=sid=='site_ne_001'
                bs.append({'siteId':sid,'personCount':8 if selected else 5000 if sid=='site_nw_029' else 0})
                gs.append({'siteId':sid,'name':sid,'status':'GLOBAL_TERRAIN_SURVEY_PENDING' if selected else 'EXISTING_CONSTRUCTION_PROTECTED_NO_DUPLICATE',
                           'tiles':['0_0'] if selected else [],'bounds':[0,100,0,100],'originalX':20,'originalZ':20})
                refs.append({'siteId':sid,'regionalInfluences':{'effectiveSourceLocationId':'the_north',
                              'values':{'DUTY':{'value':0.5}},'socialNorms':{'RURAL':{'value':0.6}}}})
            def write(rel,v):
                path=root/rel;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(v));return path
            write(mod.B4/'manifest.json',{'schemaVersion':'knownworld.b4_0.identity_baseline.v1',
                'siteCount':248,'peopleTotal':3760684,'householdsTotal':799343,'sites':bs})
            write(mod.C4,{'siteCount':248,'globalEnabled':True,'sites':gs})
            write(mod.B2,{'physicalSites':refs})
            lot='site_ne_001:home:0000001'
            write(mod.TILES/'site_ne_001'/'0_0.json',{'siteId':'site_ne_001','lots':[
                {'id':lot,'kind':'HOUSE','permission':'C3_MODULE_AVAILABLE','x':20,'z':30,'w':11,'d':12}]})
            with gzip.open(root/mod.B4/'site_ne_001.jsonl.gz','wt',encoding='utf8') as f:
                for i in range(8):f.write(json.dumps({'siteId':'site_ne_001','personId':f'b4:site_ne_001:{i+1:08d}',
                    'homeLotId':lot,'givenName':'Edric','householdName':'Stone','householdId':'hh',
                    'birthYearAC':270,'sex':'MALE','cultureId':'northman','profileSeed':1234})+'\n')
            result=mod.build(root)
            self.assertEqual(result['indexedResidents'],8)
            self.assertEqual(result['indexedHouseholds'],1)
            self.assertEqual(result['sites'][0]['regionalValues']['DUTY'],.5)
            self.assertEqual(result['sites'][1]['status'],'DEFERRED_EXISTING_OR_NONBUILDABLE')
            with gzip.open(root/mod.OUT/'site_ne_001'/'0_0.jsonl.gz','rt') as f:
                self.assertEqual(len(f.readlines()),8)
            self.assertFalse((root/mod.OUT/'site_nw_029').exists())
if __name__=='__main__':unittest.main()

"""C4.11 planning/activation regression tests; no Minecraft runtime needed."""
import importlib.util,json,sys,tempfile,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]

def load_module(name,path):
    spec=importlib.util.spec_from_file_location(name,path)
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
    return m

roll=load_module('c411_rollout',ROOT/'tools/geodata/c4_11_global_rollout.py')
hook=load_module('c411_hooks',ROOT/'tools/geodata/c4_11_install_hooks.py')

def sample(siteid='site_sw_900',tier='SMALL',mode='GENERAL_COMPACT_SETTLEMENT',pop=50):
    return dict(siteId=siteid,name='Example',tier=tier,blueprintMode=mode,
        populationTarget=pop,workingPopulationTarget=20 if pop else 0,
        householdsTarget=10 if pop else 0,residentialBuildingsTarget=10 if pop else 0,
        residentialUnitsPerBuilding=1,workplaceBuildingsTarget=4 if pop else 0,
        occupationTargetsNonzero={'SMITH':20} if pop else {},
        buildingDemandByKind=[{'kind':'SMITHY','workersTarget':20,
            'requestedBuildings':4,'slotsPerBuilding':5}] if pop else [],
        mainComplex={'reservedKeepWorkerSlots':0},mapAnchor={'x':4000,'z':3000},
        markerIds=[siteid+'_marker'])

TEMPLATES={n:{'id':n,'c1Width':11,'c1Depth':11,'maxStaff':5} for n in
    ('HOUSE_SMALL','HOUSE_MEDIUM','HOUSE_URBAN','SMITHY')}

class C411GlobalTests(unittest.TestCase):
    def test_global_census_all_248_without_pilot_budget(self):
        records=[sample('site_sw_900')]+[sample(f'site_nw_{n:03d}',pop=0) for n in range(300,547)]
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)
            report=roll.build(records,TEMPLATES,{},[],True,p/'index.json',p/'tiles')
            index=json.loads((p/'index.json').read_text())
            self.assertEqual(index['siteCount'],248)
            self.assertEqual(report['totals']['homes'],10)
            self.assertEqual(report['totals']['workplaces'],4)
            self.assertEqual(report['totals']['activatedSiteAttempts'],1)
            site=index['sites'][0]
            self.assertEqual(site['physicalAvailableLots'],14)
            self.assertTrue(site['tiles'])
            for tile in site['tiles']:
                data=json.loads((p/'tiles'/site['siteId']/(tile+'.json')).read_text())
                self.assertEqual(data['siteId'],site['siteId'])
                self.assertGreaterEqual(len(data['lots']),1)
    def test_protected_pilot_preserved_without_duplicating(self):
        for sid in ('site_nw_029','site_nw_031','site_sw_023'):
            s=sample(sid)
            record,tiles=roll.site_record(s,TEMPLATES,'NORTHERN',roll.layout_module_cached(),set(roll.PROTECTED))
            self.assertEqual(record['status'],'EXISTING_CONSTRUCTION_PROTECTED_NO_DUPLICATE')
            self.assertFalse(tiles)
            self.assertEqual(record['homesTarget'],10)
    def test_special_major_is_attempted_not_discarded(self):
        s=sample('site_se_987',tier='MAJOR',mode='COASTAL_REQUIRE_SHORE_LAYOUT')
        rec,tiles=roll.site_record(s,TEMPLATES,'GHISCARI',roll.layout_module_cached(),set())
        self.assertEqual(rec['status'],'PROVISIONAL_SPECIAL_GEOMETRY')
        self.assertEqual(rec['physicalAvailableLots'],14)
        self.assertEqual(rec['homesTarget']+rec['workplacesTarget'],14)
        self.assertTrue(tiles)
    def test_inhabited_special_has_provisional_physical_tier(self):
        s=sample('site_si_888',tier='SPECIAL',mode='SOME_SITE_SPECIFIC')
        rec,_=roll.site_record(s,TEMPLATES,'SUMMER_ISLES',roll.layout_module_cached(),set())
        self.assertEqual(rec['tier'],'SPECIAL')
        self.assertEqual(rec['physicalTier'],'SMALL')
        self.assertEqual(rec['physicalAvailableLots'],14)
    def test_mobile_or_ruin_cannot_be_fabricated(self):
        s=sample('site_ee_123',tier='SPECIAL',mode='MOBILE_NO_FIXED_FOOTPRINT')
        rec,_=roll.site_record(s,TEMPLATES,'ESSOS',roll.layout_module_cached(),set())
        self.assertEqual(rec['status'],'NON_BUILDABLE_MODE_UNMET_DEMAND')
        self.assertEqual(rec['homesTarget'],10)
    def test_template_unavailable_keeps_full_quota(self):
        s=sample()
        m={k:v for k,v in TEMPLATES.items() if k!='SMITHY'}
        rec,tiles=roll.site_record(s,m,'NORTHERN',roll.layout_module_cached(),set())
        self.assertEqual(rec['homesTarget']+rec['workplacesTarget'],14)
        self.assertEqual(rec['physicalAvailableLots'],10)
        self.assertEqual(rec['deferredTemplateLots'],4)
        self.assertTrue(all(l['kind']=='HOUSE' for records in tiles.values() for l in records))
    def test_original_coordinates_preserved_in_tiles(self):
        s=sample()
        rec,tiles=roll.site_record(s,TEMPLATES,'NORTHERN',roll.layout_module_cached(),set())
        self.assertEqual((rec['originalX'],rec['originalZ']),(4000,3000))
        self.assertTrue(all(abs(l['x']-4000)<1000 for items in tiles.values() for l in items))
    def test_dry_run_does_not_install_resources(self):
        records=[sample('site_sw_900')]+[sample(f'site_nw_{n:03d}',pop=0) for n in range(300,547)]
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp);result=roll.build(records,TEMPLATES,{},[],False,p/'index.json',p/'tiles')
            self.assertEqual(result['mode'],'READ_ONLY_AUDIT')
            self.assertFalse((p/'index.json').exists())
            self.assertFalse((p/'tiles').exists())
    def test_demand_mismatch_rejected(self):
        s=sample();s['workplaceBuildingsTarget']=7
        with self.assertRaises(ValueError):
            roll.site_record(s,TEMPLATES,'NORTHERN',roll.layout_module_cached(),set())
    def test_hooks_patch_and_repeat(self):
        original='\n'.join([
            'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC410BPreviewWorldgen;',
            '                || KnownWorldC410BPreviewWorldgen.suppressVanillaStructures(centerChunk)) return;',
            '                && !KnownWorldC410BPreviewWorldgen.suppressNaturalDecoration(chunk)) {',
            '        KnownWorldC410BPreviewWorldgen.decorate(level, chunk);'])
        revised=hook.patch(original)
        self.assertEqual(revised.count('KnownWorldC411GlobalWorldgen.'),3)
        self.assertEqual(hook.patch(revised),revised)
    def test_stale_preview_deactivated_in_installer(self):
        original='\n'.join([
            'import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC410BPreviewWorldgen;',
            '                || KnownWorldC410BPreviewWorldgen.suppressVanillaStructures(centerChunk)) return;',
            '                && !KnownWorldC410BPreviewWorldgen.suppressNaturalDecoration(chunk)) {',
            '        KnownWorldC410BPreviewWorldgen.decorate(level, chunk);'])
        with tempfile.TemporaryDirectory() as td:
            f=Path(td)/'Source.java';f.write_text(original)
            preview=Path(td)/'preview.json';preview.write_text('{}')
            hook.run(f,True,preview)
            self.assertFalse(preview.exists())
            self.assertIn('KnownWorldC411GlobalWorldgen.',f.read_text())
    def test_kings_landing_scale_full_construction_demand_no_budget(self):
        s=sample('site_sw_046',tier='MAJOR',pop=500000)
        s['workingPopulationTarget']=250000
        s['occupationTargetsNonzero']={'SMITH':250000}
        s['householdsTarget']=106383
        s['residentialUnitsPerBuilding']=4
        s['residentialBuildingsTarget']=26596
        s['workplaceBuildingsTarget']=19475
        s['buildingDemandByKind']=[{'kind':'SMITHY','workersTarget':250000,
                                    'requestedBuildings':19475,'slotsPerBuilding':13}]
        record,buckets=roll.site_record(s,TEMPLATES,'WESTEROSI',roll.layout_module_cached(),
                                       set(),emit_tiles=False)
        self.assertEqual(record['homesTarget'],26596)
        self.assertEqual(record['workplacesTarget'],19475)
        self.assertEqual(record['physicalAvailableLots'],46071)
        self.assertFalse(buckets)
    def test_no_unbounded_tile_cache_risk_regression(self):
        s=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java').read_text()
        self.assertIn('TILE_CACHE_LIMIT=96',s)
        self.assertIn('private static List<Lot> tile(',s)
        self.assertNotIn('MAX_PREVIEW_LOTS',s)
        self.assertNotIn('MAX_PREVIEW_SITES',s)

if __name__=='__main__':unittest.main()

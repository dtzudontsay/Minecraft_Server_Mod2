"""C4.10B guarded site-selection, demand-conservation, and hook regressions."""
from __future__ import annotations
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[3]


def import_at(name,path):
    spec=importlib.util.spec_from_file_location(name,path)
    mod=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

preview=import_at('c410b_preview',ROOT/'tools/geodata/c4_10b_preview_compiler.py')
patcher=import_at('c410b_hook',ROOT/'tools/geodata/c4_10b_hook_install.py')

TEMPLATES={name:{'id':name,'c1Width':w,'c1Depth':d,'requiresShore':False,
                 'requiresMountainResource':False,'requiresPolicy':False}
           for name,w,d in [('HOUSE_SMALL',11,12),('HOUSE_MEDIUM',13,15),
                            ('HOUSE_URBAN',17,20),('SMITHY',13,11),('FARM',17,15)]}


def site(sid='site_demo',tier='SMALL',houses=8,hh=8,units=1,mode='GENERAL_COMPACT_SETTLEMENT'):
    return {'siteId':sid,'name':'Settlement '+sid,'tier':tier,'blueprintMode':mode,
       'mapAnchor':{'x':100000,'z':-200000},'markerIds':['DEMO-1'],
       'populationTarget':40,'workingPopulationTarget':12,
       'occupationTargetsNonzero':{'smiths':12},'residentialBuildingsTarget':houses,
       'householdsTarget':hh,'residentialUnitsPerBuilding':units,
       'workplaceBuildingsTarget':2,
       'buildingDemandByKind':[{'kind':'SMITHY','workersTarget':12,'requestedBuildings':2,
                                'slotsPerBuilding':6}],
       'mainComplex':{'required':True,'reservedKeepWorkerSlots':0}}

GENERATOR='''package demo;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldUniversalWorldgenC2;
class KnownWorldChunkGenerator {
    void one() {
        if (KnownWorldUniversalWorldgenC2.suppressVanillaStructures(centerChunk)) return;
    }
    void two() {
        if (!WinterfellWorldgenV3.suppressNaturalDecoration(chunk)
                && !KnownWorldUniversalWorldgenC2.suppressNaturalDecoration(chunk)) {
          superCall();
        }
        KnownWorldUniversalWorldgenC2.decorate(level, chunk);
    }
}'''


class ManifestTests(unittest.TestCase):
    def test_all_lots_kept_and_palette_from_authoritative_preset(self):
        manifest,audit=preview.plan_preview([site()],TEMPLATES,[],['site_demo'],
                                            {'site_demo':'DORNISH_SANDSTONE'})
        s=manifest['sites'][0]
        self.assertEqual(len(s['lots']),10)
        self.assertEqual(s['palette'],'DORNISH_SANDSTONE')
        self.assertEqual(sum(x['staff'] for x in s['lots']),12)
        self.assertEqual(sum(len(x['householdReservationIds']) for x in s['lots']),8)
        self.assertTrue(all('region' in x for x in s['lots']))
        self.assertEqual(audit['sites'][0]['plannedPhysicalLots'],10)
        self.assertEqual(audit['sites'][0]['deferredSpecialPhysicalLots'],0)
        self.assertFalse(audit['sites'][0]['coreTerrainApproved'])

    def test_medium_and_major_housing_templates(self):
        for tier,h,u in [('MEDIUM',4,2),('MAJOR',2,4)]:
            with self.subTest(tier=tier):
                s=site(tier=tier,houses=h,units=u)
                manifest,_=preview.plan_preview([s],TEMPLATES,[],['site_demo'])
                homes=[x for x in manifest['sites'][0]['lots'] if x['kind']=='HOUSE']
                self.assertEqual({x['template'] for x in homes},
                    {'HOUSE_'+('URBAN' if tier=='MAJOR' else tier)})
                self.assertEqual(sum(len(x['householdReservationIds']) for x in homes),8)

    def test_winterfell_and_pilots_rejected(self):
        for id in ('site_nw_029','site_nw_031','site_sw_023'):
            with self.subTest(id=id),self.assertRaisesRegex(ValueError,'protected'):
                preview.plan_preview([site(sid=id)],TEMPLATES,[],[id])
        p={'siteId':'site_demo','bounds':[5000,5020,5000,5020]}
        with self.assertRaisesRegex(ValueError,'existing C2'):
            preview.plan_preview([site()],TEMPLATES,[p],['site_demo'])

    def test_no_special_city_or_ruin_silent_fallback(self):
        for mode in ('COASTAL_REQUIRE_SHORE_LAYOUT','RUIN_OR_LANDMARK_ONLY'):
            with self.subTest(mode=mode),self.assertRaisesRegex(ValueError,'not an ordinary'):
                preview.plan_preview([site(mode=mode)],TEMPLATES,[],['site_demo'])
        with self.assertRaisesRegex(ValueError,'not an ordinary'):
            preview.plan_preview([site(tier='SPECIAL')],TEMPLATES,[],['site_demo'])

    def test_unavailable_buildings_counted_and_deferred(self):
        templates={k:v for k,v in TEMPLATES.items() if k!='SMITHY'}
        manifest,audit=preview.plan_preview([site()],templates,[],['site_demo'])
        self.assertEqual(len(manifest['sites'][0]['lots']),10)
        self.assertEqual(audit['sites'][0]['deferredSpecialPhysicalLots'],2)
        self.assertEqual(sum(x['physicalPermission']!='C3_MODULE_AVAILABLE'
                             for x in manifest['sites'][0]['lots']),2)

    def test_bounded_preview_never_truncates_building_demand(self):
        s=site(houses=preview.MAX_PREVIEW_LOTS+1,hh=preview.MAX_PREVIEW_LOTS+1)
        with self.assertRaisesRegex(ValueError,'safety budget'):
            preview.plan_preview([s],TEMPLATES,[],['site_demo'])

    def test_nonpreview_fails_for_missing_duplicate_ids(self):
        with self.assertRaisesRegex(ValueError,'DISTINCT'):
            preview.plan_preview([site()],TEMPLATES,[],['site_demo','site_demo'])
        with self.assertRaisesRegex(ValueError,'Unknown site'):
            preview.plan_preview([site()],TEMPLATES,[],['nonexisting'])

    def test_dryrun_vs_explicit_activate(self):
        with tempfile.TemporaryDirectory() as temp:
            d=Path(temp)
            dump=lambda name,obj:(d/name).write_text(json.dumps(obj),encoding='utf-8')
            dump('c49.json',{'sites':[site()]*248})
            # Need genuinely distinct site IDs in baseline for this test.
            src=json.loads((d/'c49.json').read_text())
            src['sites']=[site(sid=f'other_{i}') for i in range(247)]+[site()]
            dump('c49.json',src)
            dump('arch.json',{'templates':list(TEMPLATES.values())})
            dump('pilots.json',{'pilots':[]})
            dump('presets.json',{'physicalSitePresets':[
                {'siteId':'site_demo','paletteId':'GHISCARI'}]})
            args={'source':d/'c49.json','architecture':d/'arch.json',
                  'pilots':d/'pilots.json','output':d/'installed.json',
                  'report':d/'report.json','presets':d/'presets.json'}
            preview.run(['site_demo'],activate=False,**args)
            self.assertTrue((d/'report.json').exists())
            self.assertFalse((d/'installed.json').exists())
            preview.run(['site_demo'],activate=True,**args)
            installed=json.loads((d/'installed.json').read_text())
            self.assertTrue(installed['worldgenEnabled'])
            self.assertEqual(installed['sites'][0]['palette'],'GHISCARI')

class HookTests(unittest.TestCase):
    def test_exact_three_hooks_and_idempotency(self):
        s=patcher.patch(GENERATOR)
        self.assertEqual(s.count('KnownWorldC410BPreviewWorldgen.'),3)
        self.assertEqual(patcher.patch(s),s)

    def test_unsupported_generator_never_partly_mutated(self):
        with self.assertRaisesRegex(ValueError,'incompatible'):
            patcher.patch(GENERATOR.replace(
                'KnownWorldUniversalWorldgenC2.decorate(level, chunk);',
                '// unknown variation'))

    def test_no_write_without_install(self):
        with tempfile.TemporaryDirectory() as tmp:
            f=Path(tmp)/'KnownWorldChunkGenerator.java'
            f.write_text(GENERATOR,encoding='utf-8')
            self.assertTrue(patcher.run(f,install=False))
            self.assertEqual(f.read_text(encoding='utf-8'),GENERATOR)
            self.assertTrue(patcher.run(f,install=True))
            self.assertIn('KnownWorldC410BPreviewWorldgen.decorate',f.read_text(encoding='utf-8'))
            self.assertFalse(patcher.run(f,install=False))
            self.assertEqual(len(list(Path(tmp).glob('*.bak'))),1)

if __name__=='__main__': unittest.main()

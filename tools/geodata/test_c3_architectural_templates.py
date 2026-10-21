#!/usr/bin/env python3
"""C3 coverage and no-worldwide-deployment regression checks."""
import importlib.util
import json
import sys
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('c3_builder',Path(__file__).with_name('build_c3_architectural_templates.py'))
builder=importlib.util.module_from_spec(spec);sys.modules[spec.name]=builder;spec.loader.exec_module(builder)

class C3Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source=json.loads(builder.SOURCE.read_text(encoding='utf-8-sig'))
        cls.result=builder.build(cls.source)
        cls.all={t['id']:t for t in cls.result['templates']}

    def test_exact_43_modules(self):
        self.assertEqual(len(self.all),43)
        self.assertEqual(set(self.all),set(self.source['buildingModules']))
        self.assertEqual(self.result['templateCount'],43)

    def test_reproducible_catalog(self):
        self.assertEqual(builder.build(self.source),self.result)
        self.assertEqual(self.result['generationPolicy'],'C2_SELECTED_PILOTS_ONLY')
        self.assertTrue(self.result['doesNotAuthoriseNewSites'])

    def test_job_templates_differ(self):
        self.assertEqual(self.all['BAKERY']['stations'],['FURNACE','SMOKER','BARREL'])
        self.assertNotEqual(self.all['ARMORY']['stations'],self.all['BAKERY']['stations'])
        self.assertEqual(self.all['FARM']['maxStaff'],12)

    def test_special_cases_safe(self):
        self.assertTrue(self.all['DOCK']['requiresShore'])
        self.assertTrue(self.all['MINE']['requiresMountainResource'])
        self.assertTrue(self.all['BROTHEL']['requiresPolicy'])
        self.assertTrue(self.all['TEMPORARY_CAMP']['requiresPolicy'])
        self.assertTrue(self.all['RUIN']['openAir'])
        self.assertEqual(self.all['KEEP']['floors'],3)

    def test_no_household_lies(self):
        for t in self.all.values():
            self.assertNotIn('householdNames',t)
            self.assertNotIn('assignedWorkerIds',t)
            self.assertGreaterEqual(t['maxStaff'],0)

    def test_renderer_and_installer_fails_closed(self):
        js=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC3Architecture.java').read_text()
        inst=(ROOT/'tools/geodata/install_c3_architecture.py').read_text()
        self.assertIn('getPos().x()',js)
        self.assertIn('requiresMountainResource()',js)
        self.assertIn('template.openAir()',js)
        self.assertIn('C2_SELECTED_PILOTS_ONLY',js)
        self.assertIn('KnownWorldC3Architecture.buildLot(',inst)

    def test_bundled_output_matches_builder(self):
        output=json.loads(builder.OUTPUT.read_text(encoding='utf-8'))
        self.assertEqual(output,self.result)

if __name__=='__main__':unittest.main()

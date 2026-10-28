"""Regression for worldwide B4.0 canonical IDs, including recovered sites."""
import gzip
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]

def load(name, path):
    spec=importlib.util.spec_from_file_location(name, path)
    mod=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

fix=load('b40fix', ROOT/'tools/geodata/b4_0_fix_site_id_validation.py')
validator=load('b40validator', ROOT/'tools/geodata/b4_0_validate_runtime.py')


def make_manifest(root):
    base=root/validator.REL
    base.mkdir(parents=True)
    sites=[]
    for n in range(248):
        sid='site_nw_missing_twins_01' if n==0 else f'site_ne_{n:03d}'
        occupied=n in (0,1)
        p=3_760_683 if n==0 else 1 if n==1 else 0
        h=799_342 if n==0 else 1 if n==1 else 0
        w=1_880_340 if n==0 else 1 if n==1 else 0
        file=f'{sid}.jsonl.gz' if occupied else None
        sites.append({'siteId':sid, 'personCount':p,
                      'householdCount':h, 'modeledWorkerCount':w,
                      'file':file})
        if occupied:
            with gzip.open(base/file,'wt',encoding='utf-8') as handle:
                handle.write(json.dumps({'siteId':sid, 'personId':sid+':person:1'})+'\n')
    data={'schemaVersion':validator.SCHEMA, 'siteCount':248,
          'peopleTotal':3_760_684, 'householdsTotal':799_343,
          'modeledWorkersTotal':1_880_341, 'sites':sites}
    (base/'manifest.json').write_text(json.dumps(data),encoding='utf-8')
    return base

class B40SiteIdRepairTests(unittest.TestCase):
    def test_known_normal_and_recovered_site_ids(self):
        for sid in ('site_nw_029','site_sw_046','site_ne_001',
                    'site_nw_missing_twins_01','site_sw_extra_seat_02'):
            self.assertIsNotNone(validator.SITE_PATTERN.fullmatch(sid),sid)
    def test_rejects_path_traversal_or_path_separators(self):
        for sid in ('site_nw_../anything','site_nw_foo/bar','site_nw_foo\\bar',
                    'site_nw_a..b','site_nw_a b','site_nw__029','site_nw_-01',
                    'site_nw_01.jsonl.gz','site_nw_01\n'):
            self.assertIsNone(validator.SITE_PATTERN.fullmatch(sid), repr(sid))
    def test_generated_manifest_can_be_read_without_regeneration(self):
        with tempfile.TemporaryDirectory() as td:
            base=make_manifest(Path(td))
            before={p.name:p.read_bytes() for p in base.iterdir()}
            r=validator.validate(Path(td))
            self.assertEqual(r['people'],3_760_684)
            self.assertEqual(r['inhabitedShards'],2)
            self.assertEqual(before,{p.name:p.read_bytes() for p in base.iterdir()})
    def test_duplicate_canonical_id_detected(self):
        with tempfile.TemporaryDirectory() as td:
            base=make_manifest(Path(td))
            src=base/'manifest.json'; m=json.loads(src.read_text())
            m['sites'][1]['siteId']=m['sites'][0]['siteId']
            src.write_text(json.dumps(m))
            with self.assertRaisesRegex(ValueError,'Duplicate/malformed'):
                validator.validate(Path(td))
    def test_python_patcher_idempotent_and_rejects_unknown(self):
        content='prefix\n'+fix.OLD_PY+'\nsuffix'
        first,changed=fix.patched(content,fix.OLD_PY,fix.NEW_PY,'py')
        self.assertTrue(changed)
        second,changed=fix.patched(first,fix.OLD_PY,fix.NEW_PY,'py')
        self.assertFalse(changed)
        self.assertEqual(first,second)
        with self.assertRaisesRegex(RuntimeError,'unexpected validator version'):
            fix.patched('not a validator',fix.OLD_PY,fix.NEW_PY,'py')
    def test_java_rule_is_also_patched(self):
        source=(ROOT/fix.JAVA_REL).read_text(encoding='utf8')
        self.assertIn(fix.NEW_JAVA,source)
        self.assertNotIn(fix.OLD_JAVA,source)

if __name__=='__main__': unittest.main()

import gzip, importlib.util, json, tempfile, unittest
from pathlib import Path

HERE = Path(__file__).resolve()
ROOT = HERE.parents[3]
module_spec=importlib.util.spec_from_file_location('b4check',ROOT/'tools/geodata/b4_0_validate_runtime.py')
checker=importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(checker)


def fixture(root):
    base=Path(root)/checker.REL;base.mkdir(parents=True)
    sites=[]
    for i in range(1,249):
        sid=f'site_ne_{i:03d}'
        nonzero=i in (1,2)
        site={'siteId':sid, 'personCount':3_760_683 if i==1 else 1 if i==2 else 0,
              'householdCount':799_342 if i==1 else 1 if i==2 else 0,
              'modeledWorkerCount':1_880_340 if i==1 else 1 if i==2 else 0,
              'file':sid+'.jsonl.gz' if nonzero else None}
        sites.append(site)
        if nonzero:
            with gzip.open(base/site['file'],'wt',encoding='utf8') as f:
                f.write(json.dumps({'siteId':sid,'personId':sid+':1','displayName':'Somebody'})+'\n')
    (base/'manifest.json').write_text(json.dumps({'schemaVersion':checker.SCHEMA,
       'siteCount':248,'peopleTotal':3_760_684,'householdsTotal':799_343,
       'modeledWorkersTotal':1_880_341,'sites':sites}),encoding='utf8')
    return base

class B40CommandDiagnosisTests(unittest.TestCase):
    def test_valid_manifest_and_shards_readonly(self):
        with tempfile.TemporaryDirectory() as d:
            fixture(d)
            a=checker.validate(Path(d))
            self.assertEqual(a['inhabitedShards'],2)
            self.assertEqual(a['people'],3_760_684)
            self.assertEqual(a['recordsActuallyRead'],2)
    def test_missing_manifest_shows_full_path(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaisesRegex(FileNotFoundError,'manifest'):
                checker.validate(Path(d))
    def test_broken_shard_explicitly_identified(self):
        with tempfile.TemporaryDirectory() as d:
            base=fixture(d)
            (base/'site_ne_001.jsonl.gz').unlink()
            with self.assertRaisesRegex(FileNotFoundError,'site_ne_001.jsonl.gz'):
                checker.validate(Path(d))
    def test_wrong_total_not_accepted(self):
        with tempfile.TemporaryDirectory() as d:
            base=fixture(d)
            m=json.loads((base/'manifest.json').read_text())
            m['peopleTotal']+=1
            (base/'manifest.json').write_text(json.dumps(m))
            with self.assertRaisesRegex(ValueError,'totals incorrect'):
                checker.validate(Path(d))
    def test_runtime_java_handlers_are_guarded(self):
        source=(ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB40PopulationIndex.java').read_text()
        self.assertIn('guarded(ctx.getSource(), "status"',source)
        self.assertIn('guarded(ctx.getSource(), "site"',source)
        self.assertIn('Commands.literal("diagnose")',source)
        self.assertIn('KnownWorld.LOGGER.error',source)
        self.assertIn('source.sendFailure(Component.literal("[B4.0] "',source)

if __name__=='__main__':unittest.main()

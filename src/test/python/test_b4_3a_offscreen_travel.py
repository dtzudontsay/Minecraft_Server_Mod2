"""Pure model, patch, rollback and census compatibility checks."""
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

HERE=Path(__file__).resolve().parents[3]
JAVA='src/main/java/dev/dtzudontsay/knownworld/simulation/population/'
SCENARIO='src/main/resources/data/knownworld/scenarios/agot_298_ac/world_projection_b4_1a1_v1/manifest.json'
SPEC=importlib.util.spec_from_file_location('guard',HERE/'tools/geodata/b4_3a_guarded_install.py')
GUARD=importlib.util.module_from_spec(SPEC);SPEC.loader.exec_module(GUARD)


class TestB43(unittest.TestCase):
    def test_javac_pure_model_and_phase_interpolation(self):
        if shutil.which('javac') is None or shutil.which('java') is None:
            self.skipTest('JDK tools not on PATH; installer Gradle build compiles all Java sources')
        with tempfile.TemporaryDirectory() as td:
            folder=Path(td); base=HERE/'B4_3A_STAGE/updated'/JAVA
            schedule=HERE/'src/test/python/fixtures_b43/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42Schedule.java'
            # Pure model compiles against the exact B4.2 schedule from the earlier batch.
            # Test harness compiled into a separate directory, no Minecraft libraries.
            if not schedule.exists():
                self.skipTest('B4.2 schedule fixture not packaged')
            run=subprocess.run(['javac','-d',td,str(schedule),str(base/'KnownWorldB43TravelModel.java')],capture_output=True,text=True)
            self.assertEqual(run.returncode,0,run.stdout+run.stderr)
            harness=folder/'TravelProbe.java'
            harness.write_text('''import dev.dtzudontsay.knownworld.simulation.population.*;
import java.util.*;
public class TravelProbe {
 public static void main(String[] args) {
  var home=new KnownWorldB43TravelModel.Point(0,0);
  var work=new KnownWorldB43TravelModel.Point(100,0);
  var schedule=KnownWorldB42Schedule.evaluate("b4:site_ne_001:0001","blacksmith",270,298,298L*365,2700,298L*365,true,100);
  var r=KnownWorldB43TravelModel.waypoints(home,work,null);
  if(!KnownWorldB43TravelModel.atFraction(r,0).equals(home))throw new Error("departure");
  if(!KnownWorldB43TravelModel.atFraction(r,1).equals(work))throw new Error("arrival");
  if(KnownWorldB43TravelModel.atFraction(r,0.5).x()!=50)throw new Error("midpoint");
  var route=KnownWorldB43TravelModel.waypoints(home,work,new int[]{0,0,40,20});
  if(route.size()!=3)throw new Error("missing fort gate");
  var limited=KnownWorldB43TravelModel.reconcileRecent(home,1000L,work,1010L);
  if(limited.x()>2)throw new Error("teleport");
  if(!KnownWorldB43TravelModel.reconcileRecent(home,0,work,25000).equals(work))throw new Error("stale sample");
  System.out.println("OK");
 }
}''',encoding='utf8')
            run=subprocess.run(['javac','-cp',td,'-d',td,str(harness)],capture_output=True,text=True)
            self.assertEqual(run.returncode,0,run.stdout+run.stderr)
            run=subprocess.run(['java','-cp',td,'TravelProbe'],capture_output=True,text=True)
            self.assertEqual((run.returncode,run.stdout.strip()),(0,'OK'),run.stderr)

    def make_repo(self,td):
        repo=Path(td)
        for file in GUARD.EXISTING:
            p=repo/file;p.parent.mkdir(parents=True,exist_ok=True)
            payload=HERE/'B4_3A_STAGE/baseline'/file
            p.write_bytes(payload.read_bytes())
        for d in ['baseline','updated']:
            from_path=HERE/'B4_3A_STAGE'/d
            shutil.copytree(from_path,repo/'B4_3A_STAGE'/d)
        m=repo/SCENARIO;m.parent.mkdir(parents=True,exist_ok=True)
        m.write_text(json.dumps({'schemaVersion':'knownworld.b4_1a1.household_projection.v1',
          'sourcePopulationCount':3760684,'sourceHouseholdCount':799343,
          'physicalProjectionLimit':128,'sites':[{}]*248}),encoding='utf8')
        return repo

    def test_preflight_and_unmodified_sources(self):
        with tempfile.TemporaryDirectory() as td:
            repo=self.make_repo(td)
            result=GUARD.check(repo)
            self.assertEqual(set(result.values()),{'READY','NEW'})
            p=repo/GUARD.EXISTING[0]
            p.write_bytes(p.read_bytes().replace(b'\n',b'\r\n'))
            self.assertEqual(GUARD.check(repo)[GUARD.EXISTING[0]],'READY')
            p.write_bytes(p.read_bytes().replace(b'MAX_ACTORS = 128',b'MAX_ACTORS = 900'))
            self.assertEqual(GUARD.check(repo)[GUARD.EXISTING[0]],'UNKNOWN_LOCAL_EDITS')
            with self.assertRaises(ValueError):GUARD.install(repo,False)

    def test_apply_and_rollback(self):
        with tempfile.TemporaryDirectory() as td:
            repo=self.make_repo(td)
            originals={s:(repo/s).read_bytes() for s in GUARD.EXISTING}
            GUARD.install(repo,False)
            self.assertTrue(all(v=='ALREADY_INSTALLED' for v in GUARD.check(repo).values()))
            self.assertEqual(json.loads((repo/SCENARIO).read_text())['physicalProjectionLimit'],256)
            self.assertEqual((repo/GUARD.NEW[0]).is_file(),True)
            GUARD.restore(repo,'latest')
            for s,v in originals.items():self.assertEqual((repo/s).read_bytes(),v)
            for s in GUARD.NEW:self.assertFalse((repo/s).exists())
            self.assertEqual(json.loads((repo/SCENARIO).read_text())['physicalProjectionLimit'],128)

    def test_no_save_or_identity_mutations(self):
        src=(HERE/'tools/geodata/b4_3a_guarded_install.py').read_text()
        self.assertNotIn('world_population_b4_v1',str(GUARD.FILES))
        self.assertNotIn('run/saves',str(GUARD.FILES))
        self.assertNotIn('c4_11_global_tiles',str(GUARD.FILES))
        self.assertIn('restore(repo,backup.name)',src)

    def test_256_ceiling_requires_256_manifest(self):
        java=(HERE/'B4_3A_STAGE/updated'/JAVA/'KnownWorldB41Activation.java').read_text()
        generator=(HERE/'B4_3A_STAGE/updated/tools/geodata/b4_1a1_household_index.py').read_text()
        self.assertIn('MAX_ACTORS = 256',java)
        self.assertIn('physicalProjectionLimit\':256',generator)
        self.assertIn('root.get("physicalProjectionLimit").getAsInt()!=MAX_ACTORS',java)

    def test_handoff_storage_is_world_scoped_and_bounded(self):
        src=(HERE/'B4_3A_STAGE/updated'/JAVA/'KnownWorldB43OffscreenTravel.java').read_text()
        for phrase in ['MAX_SAMPLES = 4096','getWorldPath(LevelResource.ROOT)',
                'OBSERVED_PROJECTION_HANDOFF_ONLY','ATOMIC_MOVE','offscreen_travel_b4_3a_v1.json',
                'disabled = true','public static void appeared']:
            self.assertIn(phrase,src)

    def test_no_spawn_over_work_ai_lock_and_no_duplicate_census(self):
        src=(HERE/'B4_3A_STAGE/updated'/JAVA/'KnownWorldB41Activation.java').read_text()
        self.assertIn('freshlySpawned',src)
        self.assertIn('Winterfell',src)
        self.assertIn('public static DialogueIdentity dialogueIdentity',src)
        self.assertIn('KnownWorldB43OffscreenTravel.leftView',src)
        self.assertIn('KnownWorldB43OffscreenTravel.appeared',src)
        self.assertNotIn('registry().create',src)
        self.assertNotIn('new NpcState(',src)

if __name__=='__main__':unittest.main()

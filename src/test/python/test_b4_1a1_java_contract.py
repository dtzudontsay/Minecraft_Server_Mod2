from __future__ import annotations
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java'
class Contract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):cls.code=SRC.read_text(encoding='utf8')
    def test_budget_and_runtime_root(self):
        self.assertIn('MAX_ACTORS = 128',self.code)
        self.assertIn('world_projection_b4_1a1_v1/',self.code)
        self.assertIn('TILE_CACHE_LIMIT = 24',self.code)
    def test_moving_and_disjoint_spawns(self):
        self.assertIn('v.setNoAi(false)',self.code)
        self.assertIn('Set<Long> positions=new HashSet<>()',self.code)
        self.assertIn('positions.add(new BlockPos',self.code)
        self.assertIn('HOME_WANDER_LIMIT',self.code)
    def test_no_second_registry_import_or_ollama(self):
        self.assertNotIn('registry().create(',self.code)
        self.assertNotIn('Ollama',self.code)
    def test_rich_profile_shown(self):
        for k in ('"skills"','"aptitudes"','"dispositions"','"characterValues"',
                  '"socialNorms"','"personality"','"traits"','"languages"'):
            self.assertIn(k,self.code)
if __name__=='__main__':unittest.main()

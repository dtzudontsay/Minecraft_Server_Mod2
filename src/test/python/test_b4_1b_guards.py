"""Real Java 21 truth-guard test using the repository's actual Winterfell guard."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[3]
GUARD=ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/WinterfellDialogueGuard.java'
TRUTH=ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueTruth.java'

class GuardTests(unittest.TestCase):
    @unittest.skipUnless(shutil.which('javac') and shutil.which('java') and GUARD.is_file(), 'JDK or installed Winterfell guard unavailable')
    def test_real_java_guards(self):
        with tempfile.TemporaryDirectory() as directory:
            p=Path(directory)
            sample=p/'GuardRunner.java'
            sample.write_text('''
import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB41DialogueTruth;
public class GuardRunner {
    static void require(boolean value,String title){if(!value)throw new AssertionError(title);}
    public static void main(String[] args){
       require(!KnownWorldB41DialogueTruth.review("b4:test:1","I saw you steal the gold.",null,false).accepted(),"false eyewitness");
       require(!KnownWorldB41DialogueTruth.review("b4:test:1","We met yesterday in the tavern.",null,false).accepted(),"false acquaintance");
       require(!KnownWorldB41DialogueTruth.review("b4:test:1","King Aemon Snow commands me.","Robert Baratheon",false).accepted(),"fake king");
       require(!KnownWorldB41DialogueTruth.review("b4:test:1","Robert Baratheon is dead.","Robert Baratheon",false).accepted(),"false death");
       require(!KnownWorldB41DialogueTruth.review("b4:test:1","",null,false).accepted(),"empty");
       require(KnownWorldB41DialogueTruth.review("b4:test:1","I am a baker.",null,false).accepted(),"ordinary speech");
       System.out.println("B4.1B real Java speech guard tests passed");
    }
}
''',encoding='utf-8')
            result=subprocess.run(['javac','--release','21','-d',str(p),str(GUARD),str(TRUTH),str(sample)],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
            run=subprocess.run(['java','-cp',str(p),'GuardRunner'],capture_output=True,text=True)
            self.assertEqual(run.returncode,0,run.stderr)

if __name__=='__main__':unittest.main()

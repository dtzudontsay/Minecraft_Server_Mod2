#!/usr/bin/env python3
"""Self-contained local installer fixture with success/rollback/anchor refusal."""
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HERE=Path(__file__).resolve().parent
ACT='src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java'
DAILY='src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42DailyLife.java'
DIALOGUE='src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueService.java'
act='''package p;
import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4Decisions;
class KnownWorldB41Activation {
 void hooks(){ KnownWorldAv4Decisions.register();
        KnownWorldAv4Decisions.register();
 }
 void identity(){
        KnownWorldAv4Decisions.enrich(profile,p.id);
 }
 void sheet(){
        KnownWorldAv4Decisions.enrich(sheet,p.id);
 }
 void qolPatch(){String s="AV4_QOL_V3_BEGIN";}
}
'''.replace(' void hooks(){ KnownWorldAv4Decisions.register();\n',' void hooks(){\n')
dialogue='''package p;
import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2CExperienceJournal;
class KnownWorldB41DialogueService {
  void prompt(){
        prompt.append(KnownWorldAv2CExperienceJournal.promptFor(p.id()));
  }
  String QOL="qwen3.5:27b";
}
'''
daily='''package p;
import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4Decisions;
class KnownWorldB42DailyLife {
 void applyProjection(){
            int tx=home[0],tz=home[1];String role=activity;
            if(shift && work==null){unassignedWorksites++;return;}
            if(shift && work!=null) {
            }else if(activity.equals("FREE_TIME")||activity.equals("HOME_AND_FREE_TIME")||
                     activity.equals("REST_DAY")) {
                var av4=KnownWorldAv4Decisions.choose(personId,siteId,day());
                Work leisure=leisurePlace(personId,siteId,homeX,homeZ,offsetX,offsetZ,year()-birthYear<16);
                role="VISIT_NEIGHBOR:"+friend.getUUID();
                local.getEntitiesOfClass(Villager.class,villager.getBoundingBox().inflate(18.0));
            }
            int[] fort=KnownWorldC411GlobalWorldgen.b42FortificationForInspection(siteId);
 }
}
'''

def exercise():
    with tempfile.TemporaryDirectory() as dir:
        root=Path(dir)
        for f,data in [(ACT,act),(DAILY,daily),(DIALOGUE,dialogue)]:
            p=root/f;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data)
        dest=root/'tools/av4b'
        shutil.copytree(HERE,dest,ignore=shutil.ignore_patterns('__pycache__'))
        # Avoid recursively copying a test into itself (HERE is source, dest under tempfile).
        for f in (ACT,DAILY,DIALOGUE):assert (root/f).exists()
        exe=root/'gradlew';exe.write_text('#!/bin/sh\nexit 0\n');exe.chmod(0o755)
        cmd=[sys.executable,str(dest/'av4b_install.py')]
        run=subprocess.run(cmd+['--check'],cwd=root,capture_output=True,text=True)
        assert run.returncode==0,(run.stdout,run.stderr)
        assert (root/ACT).read_text()==act
        run=subprocess.run(cmd+['--install'],cwd=root,capture_output=True,text=True)
        assert run.returncode==0,(run.stdout,run.stderr)
        assert 'qwen3.5:27b' in (root/DIALOGUE).read_text()
        assert 'AV4_QOL_V3_BEGIN' in (root/ACT).read_text()
        assert 'AV4B_GENERAL_GOALS_BEGIN' in (root/ACT).read_text()
        assert 'KnownWorldAv4BGoals' in (root/DAILY).read_text()
        run=subprocess.run(cmd+['--check'],cwd=root,capture_output=True,text=True)
        assert run.returncode==0,(run.stdout,run.stderr)
        run=subprocess.run(cmd+['--install'],cwd=root,capture_output=True,text=True)
        assert run.returncode==0,(run.stdout,run.stderr)
        # Broken anchor must stop BEFORE modifications.
        with tempfile.TemporaryDirectory() as other:
            other=Path(other)
            for f,data in [(ACT,act),(DAILY,daily.replace('VISIT_NEIGHBOR:', 'SOMETHING_ELSE:')),(DIALOGUE,dialogue)]:
                p=other/f;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data)
            shutil.copytree(HERE,other/'tools/av4b',ignore=shutil.ignore_patterns('__pycache__'))
            cp=subprocess.run([sys.executable,str(other/'tools/av4b/av4b_install.py'),'--check'],capture_output=True,text=True)
            assert cp.returncode==1,(cp.stdout,cp.stderr)
            assert (other/ACT).read_text()==act
        # Test a Gradle failure -> all source and newly installed files revert.
        with tempfile.TemporaryDirectory() as failed:
            failed=Path(failed)
            for f,data in [(ACT,act),(DAILY,daily),(DIALOGUE,dialogue)]:
                p=failed/f;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(data)
            shutil.copytree(HERE,failed/'tools/av4b',ignore=shutil.ignore_patterns('__pycache__'))
            exe=failed/'gradlew';exe.write_text('#!/bin/sh\nexit 7\n');exe.chmod(0o755)
            cp=subprocess.run([sys.executable,str(failed/'tools/av4b/av4b_install.py'),'--install'],capture_output=True,text=True)
            assert cp.returncode==1,(cp.stdout,cp.stderr)
            assert (failed/ACT).read_text()==act
            assert (failed/DAILY).read_text()==daily
            assert (failed/DIALOGUE).read_text()==dialogue
            assert not (failed/'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv4BGoals.java').exists()
    print('AV4B INSTALLER FIXTURE PASSED: check, success, QOL preservation, repeat, fail-close, rollback')

if __name__=='__main__':exercise()

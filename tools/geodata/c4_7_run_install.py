#!/usr/bin/env python3
"""Single C4.7 installer. Runs preflights, snapshots source, Gradle, automatic rollback.
The Minecraft world and existing scenario data are never opened for write.
"""
from pathlib import Path
from datetime import datetime
import os,sys,subprocess,shutil,traceback
ROOT=Path(__file__).resolve().parents[2]
TOOLS=ROOT/'tools/geodata'
PKG=ROOT/'src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement'
C3=PKG/'KnownWorldC3Architecture.java'
DETAIL=PKG/'KnownWorldC47ArchitecturalDetails.java'
LOG=ROOT/'C4_7_1_INSTALL_AND_BUILD.log'
OUTPUT=TOOLS/'output/c4_7_1_architectural_review'

def main():
    if len(sys.argv)!=2 or sys.argv[1]!='NEW_WORLD':
        print('STOP: Type NEW_WORLD after the batch script prompt.');return 2
    if not (ROOT/'gradlew.bat').is_file() or not C3.is_file():
        print('STOP: C4.7 must be extracted into your mod root next to gradlew.bat');return 2
    OUTPUT.mkdir(parents=True,exist_ok=True)
    snapshot=None
    did_modify=False
    log=LOG.open('w',encoding='utf8',errors='replace')
    def emit(text):
        print(text,flush=True);log.write(text+'\n');log.flush()
    def stage(name,argv):
        emit('\n== '+name+' ==')
        emit(' '.join(str(x) for x in argv))
        cp=subprocess.run(argv,cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,errors='replace')
        for line in cp.stdout.splitlines():log.write(line+'\n')
        log.flush()
        if cp.returncode:
            emit('FAILED (exit '+str(cp.returncode)+')')
            for line in cp.stdout.splitlines()[-65:]:print(line)
            raise RuntimeError(name+' failed; full output: '+str(LOG))
        if 'Gradle' in name:
            for line in cp.stdout.splitlines()[-16:]:print(line)
        else:
            for line in cp.stdout.splitlines()[-18:]:print(line)
    try:
        emit('C4.7.1 corrected architecture batch, '+datetime.now().isoformat())
        emit('Existing NPCs / settlement locations / signs / saves unaffected by installation.')
        py=sys.executable
        stage('61-building catalogue/static safety checks',[py,str(TOOLS/'test_c4_7_static.py')])
        stage('Java source syntax via isolated stubs',[py,str(TOOLS/'test_c4_7_java_syntax.py')])
        stage('C4.6.1 preflight (read-only)',[py,str(TOOLS/'c4_7_install_architecture.py'),'--check'])
        # Each install gets an independent backup; earlier recovery sets are untouched.
        snapname=datetime.now().strftime('backup_%Y%m%d_%H%M%S_%f')
        snapshot=OUTPUT/snapname
        snapshot.mkdir(parents=True,exist_ok=False)
        shutil.copy2(C3,snapshot/'KnownWorldC3Architecture.java')
        existing=DETAIL.exists()
        if existing:shutil.copy2(DETAIL,snapshot/'KnownWorldC47ArchitecturalDetails.java')
        (snapshot/'detail_preexisting.txt').write_text('true' if existing else 'false',encoding='utf8')
        emit('Rollback snapshot: '+str(snapshot))
        did_modify=True
        stage('Install renderer',[py,str(TOOLS/'c4_7_install_architecture.py'),'--install'])
        stage('Minecraft 26.3 Gradle build + all regression tests',[str(ROOT/'gradlew.bat'),'build','--console=plain'])
        emit('\nSUCCESS: BUILD SUCCESSFUL. C4.7.1 corrected architecture active for newly generated chunks.')
        emit('Do not run older installers. Use /kwgallery build in a fresh superflat world.')
        emit('Full log '+str(LOG))
        return 0
    except Exception as e:
        emit('INSTALL STOPPED: '+str(e))
        if did_modify and snapshot:
            try:
                shutil.copy2(snapshot/'KnownWorldC3Architecture.java',C3)
                existing=(snapshot/'detail_preexisting.txt').read_text(encoding='utf8').strip()=='true'
                if existing:shutil.copy2(snapshot/'KnownWorldC47ArchitecturalDetails.java',DETAIL)
                elif DETAIL.exists():DETAIL.unlink()
                emit('ROLLBACK COMPLETE: restored original C3 code and gallery; new detail class reverted.')
            except Exception as rollback:
                emit('ROLLBACK ERROR: '+str(rollback)+'; backups are in '+str(snapshot))
        emit('Minecraft saved worlds were not modified.')
        return 1
    finally:log.close()
if __name__=='__main__':sys.exit(main())

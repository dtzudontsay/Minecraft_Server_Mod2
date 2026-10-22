#!/usr/bin/env python3
"""C4.6 guarded orchestrator: previews, source backups, build, rollback on failure."""
from __future__ import annotations
import os,sys,subprocess,shutil,datetime
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
GEO=ROOT/'tools/geodata'
SRC=ROOT/'src/main/java/dev/dtzudontsay/knownworld'
CASE=ROOT/'src/main/resources/data/knownworld/case_study'
TOUCH=[SRC/'KnownWorld.java',
       SRC/'world/generation/settlement/KnownWorldC3Architecture.java',
       SRC/'world/generation/settlement/KnownWorldStructureSigns.java',
       CASE/'winterfell_v2.json',CASE/'winterfell_v3_worldgen.json']
SNAP=(ROOT/'tools/geodata/output/c4_6_install/rollback_snapshots'/
      ('c4_6_1_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')))
ADDED=[SRC/'world/generation/settlement/KnownWorldBlueprintGallery.java',
       SRC/'world/generation/settlement/KnownWorldBlueprintGalleryDimensions.java']

def run(description,cmd,extra=None):
    print('STAGE:',description,flush=True)
    result=subprocess.run(cmd,cwd=ROOT,env=extra)
    if result.returncode:raise RuntimeError(f'{description} FAILED ({result.returncode})')

def main():
    if os.environ.get('KNOWNWORLD_C46_CONFIRM')!='NEW_WORLD':
        raise ValueError('C4.6 is a fresh-world change. Confirm NEW_WORLD in installer script.')
    if not (ROOT/'gradlew.bat').is_file():raise FileNotFoundError('Missing Gradle wrapper; wrong root')
    py=sys.executable
    run('Python conservation test',[py,str(GEO/'test_c4_6_synthetic.py')])
    run('Winterfell local geometry PREVIEW',[py,str(GEO/'c4_6_compact_winterfell.py')])
    run('Java patch compatibility PREVIEW',[py,str(GEO/'c4_6_install_java.py'),'--check'])
    run('Export all actual C3 template types',[py,str(GEO/'c4_6_blueprint_catalog.py')])
    for path in TOUCH:
        if not path.is_file():raise FileNotFoundError(path)
    if SNAP.exists():
        raise ValueError('Unexpected snapshot folder name collision: '+str(SNAP))
    SNAP.mkdir(parents=True,exist_ok=True)
    for i,path in enumerate(TOUCH):shutil.copy2(path,SNAP/(str(i)+'.backup'))
    existed=[x.exists() for x in ADDED]
    for i,path in enumerate(ADDED):
        if existed[i]:shutil.copy2(path,SNAP/(str(i)+'.added.backup'))
    try:
        run('Install C4.6 Java and safe bounded signage',[py,str(GEO/'c4_6_install_java.py'),'--install'])
        run('Install compact Winterfell ring geometry',[py,str(GEO/'c4_6_compact_winterfell.py'),'--install'])
        run('Minecraft 26.3 Gradle build and regression tests',['cmd','/c','gradlew.bat','build'])
    except Exception:
        print('BUILD/INSTALL ERROR — RESTORING ALL FIVE ORIGINAL SOURCE AND SCENARIO FILES',flush=True)
        for i,path in enumerate(TOUCH):shutil.copy2(SNAP/(str(i)+'.backup'),path)
        for i,path in enumerate(ADDED):
            if existed[i]:shutil.copy2(SNAP/(str(i)+'.added.backup'),path)
            elif path.exists():path.unlink()
        print('All five original files restored and any new gallery Java classes removed. No save edited.',flush=True)
        raise
    else:
        print('COMPLETE: Gradle passed; V2/V3/job data and sign code installed.',flush=True)
        print('Recovery originals remain in:',SNAP,flush=True)
        print('Create a fresh Known World and separate superflat gallery world.',flush=True)
if __name__=='__main__':
    try:main()
    except Exception as e:
        print('STOPPED:',type(e).__name__,e,file=sys.stderr)
        sys.exit(1)

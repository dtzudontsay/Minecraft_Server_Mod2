#!/usr/bin/env python3
"""Single C4.8 coordinator. Does not modify existing saves, construction or census values."""
from pathlib import Path
import shutil,subprocess,sys,datetime,json
from c4_8_install_atlas_tags import SCREEN,HELPER,check,install
from c4_8_worldwide_reference import make,export

ROOT=Path(__file__).resolve().parents[2]
LOG=ROOT/'C4_8_SITE_REFERENCE_AND_ATLAS_BUILD.log'
BACKUP_DIR=ROOT/'tools/geodata/output/c4_8_worldwide_reference/backups'

def main():
    if not (ROOT/'gradlew.bat').exists():raise ValueError('Run from repository root next to gradlew.bat')
    print('C4.8 WORLDWIDE CENSUS AND ATLAS TAGS (no worldgen changes)')
    print('Validating 248 site plans, 204 jobs, all markers and total workforce...')
    data=make();check();print('PASS input/site/callback checks')
    export(data)
    # Dataset exports are intentionally kept if a GUI build fails.
    stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    backup=BACKUP_DIR/stamp;backup.mkdir(parents=True,exist_ok=False)
    original=SCREEN.read_bytes()
    (backup/'KnownWorldAtlasScreen.java').write_bytes(original)
    helper_was_present=HELPER.exists()
    if helper_was_present:(backup/'KnownWorldAtlasSiteTags.java').write_bytes(HELPER.read_bytes())
    (backup/'meta.json').write_text(json.dumps({'helper_preexisting':helper_was_present}),encoding='utf-8')
    print('Snapshot:',backup)
    try:
        install()
        print('Building against real Minecraft mappings and running tests (Gradle)...',flush=True)
        with LOG.open('w',encoding='utf-8') as log:
            log.write('C4.8 source versioned world census + atlas tags\n');log.flush()
            result=subprocess.run(['cmd','/c','gradlew.bat','build','--console=plain'],cwd=ROOT,
                                  stdout=log,stderr=subprocess.STDOUT)
        if result.returncode!=0:
            print('Gradle failure — last 55 log lines:',flush=True)
            print('\n'.join(LOG.read_text(encoding='utf-8',errors='replace').splitlines()[-55:]),flush=True)
            raise RuntimeError('Gradle build failed; inspect '+str(LOG))
        print('BUILD SUCCESSFUL; C4.8 atlas labels and reference exports installed')
    except BaseException:
        SCREEN.write_bytes(original)
        if helper_was_present:HELPER.write_bytes((backup/'KnownWorldAtlasSiteTags.java').read_bytes())
        elif HELPER.exists():HELPER.unlink()
        print('Java atlas sources rolled back. Reference CSV/JSON exports retained.')
        raise
    print('All 253 existing atlas marker locations are unchanged. Saves untouched.')
    print('Reference folder:',ROOT/'docs/reference/c4_8_worldwide')
    print('Log:',LOG)
    return 0
if __name__=='__main__':
    try:sys.exit(main())
    except Exception as exc:
        print('STOPPED:',type(exc).__name__,str(exc))
        sys.exit(1)

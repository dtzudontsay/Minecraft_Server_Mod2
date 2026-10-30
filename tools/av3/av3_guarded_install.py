#!/usr/bin/env python3
"""AV3 guarded installer: one B41 integration, no save mutation.
Check-only compiles source on JDK21 stubs and validates 761 rows + AV1 enums.
"""
import argparse,os,shutil,subprocess,sys
from datetime import datetime,timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
STAGE=ROOT/'AV3_STAGE'
BASE=Path('src/main/java/dev/dtzudontsay/knownworld')
B41=BASE/'simulation/population/KnownWorldB41Activation.java'
PRIOR=BASE/'simulation/attributes/KnownWorldAv3RegionalPriors.java'
FORM=BASE/'simulation/attributes/KnownWorldAv3Formation.java'
DOC=Path('docs/architecture/24_36_AV3_CHARACTER_FORMATION_AND_REGIONAL_PRIORS.md')
COVER=Path('docs/architecture/AV3_ATTRIBUTE_COVERAGE.csv')
INVENTORY=Path('docs/architecture/AV3_RUNTIME_AXIS_INVENTORY.csv')
XLSX=Path('docs/architecture/AV3_Attribute_Coverage.xlsx')
TEST=Path('tools/av3/test_av3_source_contract.py')
SMOKE=Path('tools/av3/test_av3_java_smoke.py')
TARGETS=(PRIOR,FORM,DOC,COVER,INVENTORY,XLSX,TEST,SMOKE)
IMPORT='import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv3Formation;'
REG='        KnownWorldAv2DChangeJournal.register();'
NEW_REG='        KnownWorldAv3Formation.register();'
BASELINE='        KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);'
NEW_APPLY='        KnownWorldAv3Formation.applyToSheet(sheet,p.site,p.id,p.culture,p.religion,\n' \
          '            p.household,p.mother,p.father,p.birthYear);'
DELTA='        KnownWorldAv2DChangeJournal.applyToSheet(sheet,p.site,p.id);'
PSYCH='        KnownWorldAv2BPsychologyBridge.enrich(sheet);'


def patch(s):
    if IMPORT in s or NEW_REG in s or NEW_APPLY in s:
        if (s.count(IMPORT)==1 and s.count(NEW_REG)==1 and s.count(NEW_APPLY)==1 and
            s.index(NEW_APPLY)<s.index(BASELINE)<s.index(DELTA)<s.index(PSYCH)):
            return s
        raise RuntimeError('Partial/conflicting AV3 integration: source not modified')
    for marker in (REG,BASELINE,DELTA,PSYCH):
        if s.count(marker)!=1:
            raise RuntimeError('AV3 requires exactly one AV2B/C/D integration anchor: '+marker.strip())
    if not(s.index(BASELINE)<s.index(DELTA)<s.index(PSYCH)):
        raise RuntimeError('Existing AV2 read order changed: do not apply AV3')
    pack='package dev.dtzudontsay.knownworld.simulation.population;\n'
    if s.count(pack)!=1:raise RuntimeError('B41 Java package unexpectedly changed')
    s=s.replace(pack,pack+'\n'+IMPORT+'\n',1)
    s=s.replace(REG,REG+'\n'+NEW_REG,1)
    s=s.replace(BASELINE,NEW_APPLY+'\n'+BASELINE,1)
    return s


def run(args):subprocess.run(args,cwd=ROOT,check=True)


def verify():
    req=[BASE/('simulation/attributes/'+n+'.java') for n in (
        'KnownWorldAv1Vocabulary','KnownWorldAv2CharacterState',
        'KnownWorldAv2BPsychologyBridge','KnownWorldAv2CExperienceJournal',
        'KnownWorldAv2DChangePolicy','KnownWorldAv2DChangeJournal')]
    req +=[BASE/'simulation/npc/psychology/CharacterPsychologyRules.java',
           Path('src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json')]
    for p in req:
        if not (ROOT/p).is_file():raise RuntimeError('AV1/AV2A/B/C/D prerequisite missing: '+str(p))
    for p in TARGETS:
        staged=STAGE/p;installed=ROOT/p
        if not staged.is_file():raise RuntimeError('Missing staged AV3 file: '+str(staged))
        if installed.exists() and installed.read_bytes()!=staged.read_bytes():
            raise RuntimeError('Existing AV3 target differs; refuse overwrite: '+str(p))
    if not (ROOT/B41).is_file():raise RuntimeError('Missing original B41 source')
    old=(ROOT/B41).read_bytes()
    s=old.decode('utf-8');windows='\r\n' in s
    updated=patch(s.replace('\r\n','\n'))
    new=updated.replace('\n','\r\n').encode('utf-8') if windows else updated.encode('utf-8')
    run([sys.executable,str(STAGE/TEST),'--repo',str(ROOT),'--stage','AV3_STAGE'])
    run([sys.executable,str(STAGE/SMOKE),'--repo',str(ROOT),'--stage','AV3_STAGE'])
    return old,new


def compile_project():
    gradle=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
    if not gradle.is_file():raise RuntimeError('Gradle wrapper missing')
    if os.name=='nt':run(['cmd','/c',str(gradle),'compileJava','--no-daemon'])
    else:run(['bash',str(gradle),'compileJava','--no-daemon'])


def install():
    old,new=verify()
    meta=subprocess.run(['git','rev-parse','--git-dir'],cwd=ROOT,capture_output=True,text=True)
    if meta.returncode:raise RuntimeError('Git repository required for protected source backup')
    g=Path(meta.stdout.strip());g=g if g.is_absolute() else (ROOT/g).resolve()
    backup=g/'av3_backups'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    backup.mkdir(parents=True,exist_ok=False)
    saved=backup/B41;saved.parent.mkdir(parents=True,exist_ok=True);saved.write_bytes(old)
    created=[];modified=False
    try:
        for p in TARGETS:
            dest=ROOT/p
            if dest.exists():continue
            dest.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(STAGE/p,dest);created.append(dest)
        if old!=new:(ROOT/B41).write_bytes(new);modified=True
        print('AV3 staged source installed; invoking complete Fabric compileJava...')
        compile_project()
    except BaseException:
        if modified:(ROOT/B41).write_bytes(old)
        for p in reversed(created):
            if p.is_file():p.unlink()
        print('AV3 SOURCE ROLLBACK complete; backup: '+str(saved),file=sys.stderr)
        raise
    print('AV3 INSTALL PASS: source backup at '+str(saved))
    print('Deferred Minecraft checks: /kwformation status; right-click B4 projected person; inspect av3Formation')

if __name__=='__main__':
    p=argparse.ArgumentParser();mode=p.add_mutually_exclusive_group(required=True)
    mode.add_argument('--check-only',action='store_true');mode.add_argument('--install',action='store_true')
    a=p.parse_args()
    try:
        if a.check_only:
            before,after=verify()
            print('AV3 CHECK PASS: 761 source rows, 124 Java numeric dimensions, region catalog validation, Java21 smoke, AV2 order. No files changed.')
            print('AV3 Java source patch needed:',before!=after)
        else:install()
    except (RuntimeError,ValueError,OSError,AssertionError,UnicodeError,subprocess.CalledProcessError) as e:
        print('AV3 BLOCKED:',e,file=sys.stderr);sys.exit(1)

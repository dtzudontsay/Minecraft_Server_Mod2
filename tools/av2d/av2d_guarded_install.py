#!/usr/bin/env python3
"""AV2D guarded installer. Only patches the verified B41 integration points.
Check-only performs no edits. Source backups and exact rollback after failed build.
"""
import argparse
import csv
import json
import os
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
STAGE=ROOT/'AV2D_STAGE'
BASE=Path('src/main/java/dev/dtzudontsay/knownworld')
B41=BASE/'simulation/population/KnownWorldB41Activation.java'
POLICY=BASE/'simulation/attributes/KnownWorldAv2DChangePolicy.java'
JOURNAL=BASE/'simulation/attributes/KnownWorldAv2DChangeJournal.java'
DOC=Path('docs/architecture/24_35_AV2D_CHANGE_EVENTS_AND_RECONCILIATION.md')
COVER=Path('docs/architecture/AV2D_ATTRIBUTE_COVERAGE.csv')
TEST=Path('tools/av2d/test_av2d_source_contract.py')
SMOKE=Path('tools/av2d/test_av2d_java_smoke.py')
TARGETS=(POLICY,JOURNAL,DOC,COVER,TEST,SMOKE)
IMPORT='import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2DChangeJournal;'
REG='        KnownWorldAv2CExperienceJournal.register();'
PRE='        KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);'
POST='        KnownWorldAv2BPsychologyBridge.enrich(sheet);'
NEWREG='        KnownWorldAv2DChangeJournal.register();'
NEWAPPLY='        KnownWorldAv2DChangeJournal.applyToSheet(sheet,p.site,p.id);'


def patch(t):
    if NEWREG in t or NEWAPPLY in t or IMPORT in t:
        if t.count(NEWREG)==1 and t.count(NEWAPPLY)==1 and t.count(IMPORT)==1:
            order=(t.index(PRE),t.index(NEWAPPLY),t.index(POST))
            if not order[0]<order[1]<order[2]:raise RuntimeError('AV2D psychology read-order corrupted')
            return t
        raise RuntimeError('Incomplete/ambiguous AV2D integration detected')
    for s in (REG,PRE,POST):
        if t.count(s)!=1:raise RuntimeError('Expected exactly one AV2A/B/C integration point: '+s)
    if t.index(PRE)>t.index(POST):raise RuntimeError('AV2A/AV2B read order is wrong')
    pkg='package dev.dtzudontsay.knownworld.simulation.population;\n'
    if t.count(pkg)!=1:raise RuntimeError('Unexpected B41 package declaration')
    t=t.replace(pkg,pkg+'\n'+IMPORT+'\n',1)
    t=t.replace(REG,REG+'\n'+NEWREG,1)
    t=t.replace(PRE,PRE+'\n'+NEWAPPLY,1)
    return t


def run(cmd):subprocess.run(cmd,cwd=ROOT,check=True)

def verify():
    req=(BASE/'simulation/attributes/KnownWorldAv1Vocabulary.java',
         BASE/'simulation/attributes/KnownWorldAv2CharacterState.java',
         BASE/'simulation/attributes/KnownWorldAv2BPsychologyBridge.java',
         BASE/'simulation/attributes/KnownWorldAv2CEventPolicy.java',
         BASE/'simulation/attributes/KnownWorldAv2CExperienceJournal.java',
         BASE/'simulation/npc/psychology/CharacterPsychologyRules.java',
         BASE/'simulation/population/KnownWorldB42BPersonalEvents.java',
         Path('src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json'))
    for p in req:
        if not (ROOT/p).is_file():raise RuntimeError('Missing AV1/AV2A/AV2B/AV2C prerequisite: '+str(p))
    for p in TARGETS:
        src=STAGE/p;dst=ROOT/p
        if not src.is_file():raise RuntimeError('Incomplete AV2D staged package: '+str(p))
        if dst.exists() and dst.read_bytes()!=src.read_bytes():
            raise RuntimeError('AV2D target already differs; refuse overwrite: '+str(p))
    if not (ROOT/B41).is_file():raise RuntimeError('Missing B41 source')
    old=(ROOT/B41).read_bytes();s=old.decode('utf-8');win='\r\n' in s
    changed=patch(s.replace('\r\n','\n'))
    new=changed.replace('\n','\r\n').encode('utf-8') if win else changed.encode('utf-8')
    run([sys.executable,str(STAGE/TEST),'--repo',str(ROOT),'--stage','AV2D_STAGE'])
    run([sys.executable,str(STAGE/SMOKE),'--repo',str(ROOT),'--stage','AV2D_STAGE'])
    return (old,new)


def compile_project():
    gradle=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
    if not gradle.is_file():raise RuntimeError('Gradle wrapper not found')
    if os.name=='nt':run(['cmd','/c',str(gradle),'compileJava','--no-daemon'])
    else:run(['bash',str(gradle),'compileJava','--no-daemon'])


def install():
    old,new=verify()
    info=subprocess.run(['git','rev-parse','--git-dir'],cwd=ROOT,text=True,capture_output=True)
    if info.returncode:raise RuntimeError('Git worktree required for AV2D backup')
    g=Path(info.stdout.strip());g=g if g.is_absolute() else (ROOT/g).resolve()
    backup=g/'av2d_backups'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    backup.mkdir(parents=True,exist_ok=False)
    b=backup/B41;b.parent.mkdir(parents=True,exist_ok=True);b.write_bytes(old)
    created=[];modified=False
    try:
        for p in TARGETS:
            target=ROOT/p
            if target.exists():continue
            target.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(STAGE/p,target);created.append(target)
        if old!=new:(ROOT/B41).write_bytes(new);modified=True
        print('AV2D source installed; compiling complete Fabric Java project...')
        compile_project()
    except BaseException:
        if modified:(ROOT/B41).write_bytes(old)
        for p in reversed(created):
            if p.is_file():p.unlink()
        print('AV2D ROLLBACK complete; backed-up source: '+str(b),file=sys.stderr)
        raise
    print('AV2D INSTALL PASS; source backup: '+str(b))
    print('Optional deferred Minecraft diagnostics: /kwchanges status; /kwchanges reconcile "b4:site_...:..."')

if __name__=='__main__':
    a=argparse.ArgumentParser();g=a.add_mutually_exclusive_group(required=True)
    g.add_argument('--check-only',action='store_true');g.add_argument('--install',action='store_true')
    opts=a.parse_args()
    try:
        if opts.check_only:
            old,new=verify()
            print('AV2D CHECK PASS: typed outcomes, canonical AV1 vocabulary, 761 row coverage, Java smoke. No source changed.')
            print('B41 source modification needed:',old!=new)
        else:install()
    except (RuntimeError,OSError,subprocess.CalledProcessError,AssertionError,ValueError,UnicodeError) as e:
        print('AV2D BLOCKED:',e,file=sys.stderr);sys.exit(1)

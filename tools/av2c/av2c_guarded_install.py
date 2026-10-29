#!/usr/bin/env python3
"""AV2C guarded installer, base: AV2B already installed on current master.
No world/save changes during install; source rollback on build failure.
"""
import argparse
import os
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
STAGE=ROOT/'AV2C_STAGE'
BASE=Path('src/main/java/dev/dtzudontsay/knownworld/')
SRC_B4=BASE/'simulation/population/KnownWorldB42BPersonalEvents.java'
SRC_B41=BASE/'simulation/population/KnownWorldB41Activation.java'
SRC_CHAT=BASE/'simulation/population/KnownWorldB41DialogueService.java'
POLICY=BASE/'simulation/attributes/KnownWorldAv2CEventPolicy.java'
JOURNAL=BASE/'simulation/attributes/KnownWorldAv2CExperienceJournal.java'
DOC=Path('docs/architecture/24_34_AV2C_VERIFIED_EXPERIENCE_PROVENANCE.md')
COVER=Path('docs/architecture/AV2C_ATTRIBUTE_COVERAGE.csv')
TEST=Path('tools/av2c/test_av2c_source_contract.py')
SMOKE=Path('tools/av2c/test_av2c_java_smoke.py')
TARGETS=(POLICY,JOURNAL,DOC,COVER,TEST,SMOKE)
IMPORT='import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2CExperienceJournal;'
HOOK_WORK='        eventCount++;dirty=true;'
HOOK_B41='        KnownWorldB42BPersonalEvents.register();'
HOOK_SHEET='        KnownWorldB42BPersonalEvents.enrich(sheet,p.id);'
HOOK_CHAT='        JsonObject profile=p.profile();'


def patch_one(text,file):
    if file==SRC_B4:
        anchor=HOOK_WORK
        replacement=HOOK_WORK+'\n        KnownWorldAv2CExperienceJournal.observedPhysicalWork(server,id,workplace,day);'
    elif file==SRC_B41:
        anchor=HOOK_B41
        replacement=HOOK_B41+'\n        KnownWorldAv2CExperienceJournal.register();'
        sheet_hook='        KnownWorldAv2CExperienceJournal.enrich(sheet,p.site,p.id);'
        if sheet_hook not in text:
            if text.count(HOOK_SHEET)!=1:raise RuntimeError('Expected single B4 inspection point')
            text=text.replace(HOOK_SHEET,HOOK_SHEET+'\n'+sheet_hook,1)
    elif file==SRC_CHAT:
        anchor=HOOK_CHAT
        replacement='        prompt.append(KnownWorldAv2CExperienceJournal.promptFor(p.id()));\n'+HOOK_CHAT
    else:raise RuntimeError('Unexpected target')
    if replacement not in text:
        if text.count(anchor)!=1:raise RuntimeError('Unexpected source anchor in '+str(file))
        text=text.replace(anchor,replacement,1)
    elif text.count(replacement)!=1:raise RuntimeError('Duplicate or partial patch '+str(file))
    if IMPORT not in text:
        import_anchor='package dev.dtzudontsay.knownworld.simulation.population;\n'
        if text.count(import_anchor)!=1:raise RuntimeError('Package changed '+str(file))
        text=text.replace(import_anchor,import_anchor+'\n'+IMPORT+'\n',1)
    if text.count(IMPORT)!=1:raise RuntimeError('Duplicate AV2C imports')
    return text


def patches():
    changes={}
    for f in (SRC_B4,SRC_B41,SRC_CHAT):
        if not (ROOT/f).is_file():raise RuntimeError('Missing source '+str(f))
        before=(ROOT/f).read_bytes()
        orig=before.decode('utf-8')
        win='\r\n' in orig
        normalized=orig.replace('\r\n','\n')
        result=patch_one(normalized,f)
        # Patch AV2B explicitly, rather than quietly accepting stale prerequisites.
        if f==SRC_B41:
            if 'KnownWorldAv2BPsychologyBridge.enrich(sheet);' not in result or \
               'KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);' not in result:
                raise RuntimeError('Install AV2A and AV2B first; B41 bridge missing')
        if f==SRC_CHAT and '"derivedTendencies"' not in result:
            raise RuntimeError('AV2B chat profile missing')
        if f==SRC_B4 and 'if(previous!=null && day<=previous.latestDay())return;' not in result:
            raise RuntimeError('B4 observed attendance deduplication not found')
        encoded=result.replace('\n','\r\n').encode('utf-8') if win else result.encode('utf-8')
        changes[f]=(before,encoded)
    return changes


def execute(argv):subprocess.run(argv,cwd=ROOT,check=True)


def verify():
    prereqs=[
        BASE/'simulation/attributes/KnownWorldAv1Vocabulary.java',
        BASE/'simulation/attributes/KnownWorldAv2CharacterState.java',
        BASE/'simulation/attributes/KnownWorldAv2BPsychologyBridge.java',
        BASE/'simulation/npc/psychology/CharacterPsychologyRules.java',
        Path('src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json')]
    for p in prereqs:
        if not (ROOT/p).is_file():raise RuntimeError('Missing AV1/AV2A/AV2B prerequisite '+str(p))
    for p in TARGETS:
        s=STAGE/p; d=ROOT/p
        if not s.is_file():raise RuntimeError('Missing staged source '+str(s))
        if d.exists() and d.read_bytes()!=s.read_bytes():
            raise RuntimeError('AV2C target differs; refuse overwrite: '+str(p))
    edits=patches()
    execute([sys.executable,str(STAGE/TEST),'--repo',str(ROOT),'--stage','AV2C_STAGE'])
    execute([sys.executable,str(STAGE/SMOKE),'--repo',str(ROOT),'--stage','AV2C_STAGE'])
    return edits


def compile_project():
    wrapper=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
    if not wrapper.is_file():raise RuntimeError('Gradle wrapper missing')
    cmd=['cmd','/c',str(wrapper),'compileJava','--no-daemon'] if os.name=='nt' else \
        ['bash',str(wrapper),'compileJava','--no-daemon']
    execute(cmd)


def install():
    changes=verify()
    # Backup in the real git-dir, never outside the repo unexpectedly.
    meta=subprocess.run(['git','rev-parse','--git-dir'],cwd=ROOT,text=True,capture_output=True)
    if meta.returncode:raise RuntimeError('Protected installation requires Git repository')
    backup=Path(meta.stdout.strip());backup=backup if backup.is_absolute() else (ROOT/backup).resolve()
    backup=backup/'av2c_backups'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    backup.mkdir(parents=True,exist_ok=False)
    created=[];updated=[]
    for f,(before,_) in changes.items():
        old=backup/f.relative_to(BASE)
        old.parent.mkdir(parents=True,exist_ok=True)
        old.write_bytes(before)
    try:
        for f in TARGETS:
            d=ROOT/f
            if d.exists():continue
            d.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(STAGE/f,d);created.append(d)
        for f,(before,after) in changes.items():
            if before!=after:
                (ROOT/f).write_bytes(after);updated.append(f)
        print('AV2C source installed. Compiling Fabric Java project...')
        compile_project()
    except BaseException:
        for f in reversed(updated):(ROOT/f).write_bytes(changes[f][0])
        for f in reversed(created):
            if f.exists():f.unlink()
        print('AV2C ROLLBACK complete; pre-install source backups '+str(backup),file=sys.stderr)
        raise
    print('AV2C INSTALL PASS; backup '+str(backup))
    print('Optional test: /kwexperience status')

if __name__=='__main__':
    p=argparse.ArgumentParser();g=p.add_mutually_exclusive_group(required=True)
    g.add_argument('--check-only',action='store_true');g.add_argument('--install',action='store_true')
    a=p.parse_args()
    try:
        if a.check_only:
            changed=verify();print('AV2C CHECK PASS: validated canonical authority, evidence mirror, source hooks and Java smoke. No files changed.')
            print('Expected managed source changes: '+', '.join(str(f) for f,(a,b) in changed.items() if a!=b))
        else:install()
    except (RuntimeError, OSError, subprocess.CalledProcessError, AssertionError, ValueError, UnicodeError) as exc:
        print('AV2C BLOCKED:',exc,file=sys.stderr);sys.exit(1)

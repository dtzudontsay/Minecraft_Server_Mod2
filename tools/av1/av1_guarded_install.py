#!/usr/bin/env python3
"""Guarded AV1 source install. Safe check-only; rollback after any failed build."""
import argparse, hashlib, os, shutil, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
STAGE=ROOT/'AV1_STAGE'
MAIN=Path('src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java')
SRC_IMPORT='import dev.dtzudontsay.knownworld.simulation.NpcSimulation;'
AV1_IMPORT='import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv1Vocabulary;'
SRC_HOOK='        KnownWorldB41Activation.register();'
AV1_HOOK='        KnownWorldAv1Vocabulary.register();'
TARGETS=[
 'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv1Vocabulary.java',
 'src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json',
 'docs/architecture/AV1_VOCABULARY_CROSSWALK.csv',
 'docs/architecture/24_31_AV1_UNIFIED_ATTRIBUTE_VOCABULARY.md',
 'docs/architecture/AV1_Attribute_Coverage.xlsx',
 'tools/av1/test_av1_vocabulary.py',
]

def run(cmd,**kwargs):
    return subprocess.run(cmd,cwd=ROOT,check=True,**kwargs)

def verify_stage():
    for p in TARGETS:
        s=STAGE/p
        if not s.is_file():raise RuntimeError('Missing staged AV1 file '+str(s))
    run([sys.executable,str(STAGE/'tools/av1/test_av1_vocabulary.py'),'--repo',str(ROOT),'--stage','AV1_STAGE'])
    original=(ROOT/MAIN).read_text(encoding='utf-8')
    already=(AV1_IMPORT in original or AV1_HOOK in original)
    if already:
        if original.count(AV1_IMPORT)!=1 or original.count(AV1_HOOK)!=1:
            raise RuntimeError('Partial or unexpected AV1 hook in KnownWorld.java; no change permitted')
    else:
        if original.count(SRC_IMPORT)!=1 or original.count(SRC_HOOK)!=1:
            raise RuntimeError('Unexpected KnownWorld.java source hook; no mutation permitted')
    for p in TARGETS:
        target=ROOT/p
        if target.exists() and target.read_bytes()!=(STAGE/p).read_bytes():
            raise RuntimeError('Existing target differs; will not overwrite: '+p)
    return original,already

def git_clean_target():
    # Protect user edits to the single existing Java entrypoint that we patch.
    for args in [ ['git','diff','--quiet','--',str(MAIN)],
                  ['git','diff','--cached','--quiet','--',str(MAIN)] ]:
        r=subprocess.run(args,cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        if r.returncode!=0:
            raise RuntimeError('KnownWorld.java has uncommitted changes; commit/stash them before installing AV1')

def build():
    if os.name=='nt':
        wrapper=ROOT/'gradlew.bat'
        if not wrapper.is_file():raise RuntimeError('Missing gradlew.bat')
        run(['cmd','/c',str(wrapper),'compileJava','--no-daemon'])
    else:
        wrapper=ROOT/'gradlew'
        if not wrapper.is_file():raise RuntimeError('Missing gradlew')
        run(['bash',str(wrapper),'compileJava','--no-daemon'])

def install():
    original,already=verify_stage()
    if not already:git_clean_target()
    current_git=ROOT/'.git'
    # No snapshot under tracked paths; gitdir aware for regular clones/worktrees.
    r=subprocess.run(['git','rev-parse','--git-dir'],cwd=ROOT,check=True,capture_output=True,text=True)
    gitdir=Path(r.stdout.strip())
    if not gitdir.is_absolute():gitdir=(ROOT/gitdir).resolve()
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    backup=gitdir/'av1_backups'/stamp
    if backup.exists():raise RuntimeError('Snapshot already exists '+str(backup))
    backup.mkdir(parents=True)
    (backup/'KnownWorld.java').write_text(original,encoding='utf-8')
    installed=[]; entry_changed=False
    try:
        for rel in TARGETS:
            target=ROOT/rel
            if target.exists():continue # already-byte-identical, preserve
            target.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(STAGE/rel,target)
            installed.append(target)
        if not already:
            entry=ROOT/MAIN
            updated=original.replace(SRC_IMPORT,SRC_IMPORT+'\n'+AV1_IMPORT,1)
            updated=updated.replace(SRC_HOOK,SRC_HOOK+'\n'+AV1_HOOK,1)
            if updated.count(AV1_IMPORT)!=1 or updated.count(AV1_HOOK)!=1:
                raise RuntimeError('AV1 source patch invariant broken')
            entry.write_text(updated,encoding='utf-8',newline='')
            entry_changed=True
        print('AV1 source installed. Running full local Gradle compile...')
        build()
    except Exception:
        if entry_changed:(ROOT/MAIN).write_text(original,encoding='utf-8',newline='')
        for target in reversed(installed):
            if target.exists():target.unlink()
        print('AV1 install failed: restored original entrypoint and removed newly installed files. Snapshot: '+str(backup),file=sys.stderr)
        raise
    print('AV1 INSTALLED AND COMPILED. Snapshot: '+str(backup))
    print('In Minecraft use /kwattributes status, /kwattributes row 93, /kwattributes resolve CHARACTER_VALUE EGALITARISM')
    print('No save/profile/economy state was changed. Commit after checking git diff.')

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    group=parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check-only',action='store_true')
    group.add_argument('--install',action='store_true')
    args=parser.parse_args()
    try:
        if args.check_only:
            original,installed=verify_stage()
            if not installed:git_clean_target()
            print('AV1 CHECK OK — source/authority matches; 706 definitions; no files changed.')
        else:install()
    except (RuntimeError,OSError,subprocess.CalledProcessError,AssertionError) as e:
        print('AV1 BLOCKED:',str(e),file=sys.stderr);sys.exit(1)

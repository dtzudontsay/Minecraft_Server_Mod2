#!/usr/bin/env python3
"""AV2A source-only guarded installer; never touches Minecraft saves."""
import argparse, os, shutil, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
STAGE=ROOT/'AV2_STAGE'
B41=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java')
JAVA=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2CharacterState.java')
TARGETS=[JAVA,Path('docs/architecture/24_32_AV2A_WORLDWIDE_CHARACTER_STATE.md'),
         Path('docs/architecture/AV2A_ATTRIBUTE_COVERAGE.csv'),Path('tools/av2/test_av2_source_contract.py')]
IM='import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2CharacterState;'
IM_AT='import dev.dtzudontsay.knownworld.KnownWorld;'
HOOK='        KnownWorldB42DailyLife.register();'
VIEW='        sheet.add("motivations",motivations);'
METHOD='''    /** AV2A: only currently materialized canonical B4.0 identities are editable. */
    public static String av2VisibleSite(String personId) {
        Person person=visiblePeople.get(personId);
        UUID uuid=projected.get(personId);
        if(person==null || uuid==null || activeServer==null)return null;
        ServerLevel world=activeServer.getLevel(Level.OVERWORLD);
        Entity entity=world==null?null:world.getEntity(uuid);
        if(entity==null || !entity.isAlive() || !entity.entityTags().contains(TAG))return null;
        return person.site;
    }
'''

def run(argv):
    return subprocess.run(argv,cwd=ROOT,check=True)

def patch(content:bytes)->bytes:
    s=content.decode('utf-8');nl='\r\n' if '\r\n' in s else '\n'
    if IM in s or 'av2VisibleSite(String personId)' in s:
        if s.count(IM)==1 and s.count('KnownWorldAv2CharacterState.register();')==1 and s.count('KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);')==1 and s.count('av2VisibleSite(String personId)')==1:
            return content
        raise RuntimeError('Incomplete/conflicting AV2A patch in B41Activation.java')
    for text in [IM_AT,HOOK,VIEW,'    private static void inspect(ServerPlayer player,Entity actor) {']:
        if s.count(text)!=1:raise RuntimeError('Unexpected B4.1A.1 hook: '+text)
    s=s.replace(IM_AT,IM_AT+nl+IM,1)
    s=s.replace(HOOK,HOOK+nl+'        KnownWorldAv2CharacterState.register();',1)
    s=s.replace(VIEW,VIEW+nl+'        KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);',1)
    s=s.replace('    private static void inspect(ServerPlayer player,Entity actor) {',METHOD.replace('\n',nl)+
                '    private static void inspect(ServerPlayer player,Entity actor) {',1)
    return s.encode('utf-8')

def verify():
    for rel in TARGETS:
        if not (STAGE/rel).is_file():raise RuntimeError('Missing staged AV2A file '+str(rel))
        dst=ROOT/rel
        if dst.exists() and dst.read_bytes()!=(STAGE/rel).read_bytes():
            raise RuntimeError('Existing AV2A file differs: '+str(rel))
    for rel in [
        'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv1Vocabulary.java',
        'src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json']:
        if not (ROOT/rel).is_file():raise RuntimeError('AV1 prerequisite not installed: '+rel)
    existing=(ROOT/B41).read_bytes()
    updated=patch(existing)
    run([sys.executable,str(STAGE/'tools/av2/test_av2_source_contract.py'),
         '--repo',str(ROOT),'--stage','AV2_STAGE'])
    return existing,updated

def build():
    wrapper=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
    if not wrapper.is_file():raise RuntimeError('No Gradle wrapper')
    argv=['cmd','/c',str(wrapper),'compileJava','--no-daemon'] if os.name=='nt' else ['bash',str(wrapper),'compileJava','--no-daemon']
    run(argv)

def install():
    old,new=verify()
    if old!=new:
        for args in [ ['git','diff','--quiet','--',str(B41)],['git','diff','--cached','--quiet','--',str(B41)] ]:
            r=subprocess.run(args,cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
            if r.returncode!=0:raise RuntimeError('B41Activation.java has uncommitted edits; commit or stash before AV2A')
    gitdir=subprocess.run(['git','rev-parse','--git-dir'],cwd=ROOT,check=True,capture_output=True,text=True).stdout.strip()
    gitdir=Path(gitdir)
    if not gitdir.is_absolute():gitdir=(ROOT/gitdir).resolve()
    backup=gitdir/'av2a_backups'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    backup.mkdir(parents=True,exist_ok=False)
    (backup/'KnownWorldB41Activation.java').write_bytes(old)
    installed=[];changed=False
    try:
        for rel in TARGETS:
            dst=ROOT/rel
            if dst.exists():continue
            dst.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(STAGE/rel,dst);installed.append(dst)
        if new!=old:
            (ROOT/B41).write_bytes(new);changed=True
        print('AV2A installed. Running local Fabric Gradle compile...')
        build()
    except Exception:
        if changed:(ROOT/B41).write_bytes(old)
        for dst in reversed(installed):
            if dst.exists():dst.unlink()
        print('AV2A ROLLBACK: original B41 restored; created files removed. Backup: '+str(backup),file=sys.stderr)
        raise
    print('AV2A INSTALLED + COMPILE PASSED. Backup: '+str(backup))
    print('New command /kwprofile status ; no Minecraft saves touched by installer')

if __name__=='__main__':
    ap=argparse.ArgumentParser();g=ap.add_mutually_exclusive_group(required=True)
    g.add_argument('--check-only',action='store_true');g.add_argument('--install',action='store_true');a=ap.parse_args()
    try:
        if a.check_only:
            old,new=verify();print('AV2A CHECK OK: AV1 present; guarded B41 hooks; 706 AV1 definitions preserved. No files changed.')
        else:install()
    except (RuntimeError,OSError,subprocess.CalledProcessError,AssertionError) as e:
        print('AV2A BLOCKED:',str(e),file=sys.stderr);sys.exit(1)

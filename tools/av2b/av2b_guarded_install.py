#!/usr/bin/env python3
"""AV2B guarded source installer: shares legacy psychology with worldwide B4 profiles.
No world/save mutation. Works with AV2A installed but not necessarily committed.
"""
import argparse, os, re, shutil, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
STAGE=ROOT/'AV2B_STAGE'
LEG=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/npc/psychology/CharacterPsychologyService.java')
B41=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java')
CHAT=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueService.java')
RULE=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/npc/psychology/CharacterPsychologyRules.java')
BRIDGE=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2BPsychologyBridge.java')
DOC=Path('docs/architecture/24_33_AV2B_SHARED_PSYCHOLOGY_BRIDGE.md')
TEST=Path('tools/av2b/test_av2b_shared_psychology.py')
SMOKE=Path('tools/av2b/test_av2b_java_smoke.py')
TARGETS=(RULE,BRIDGE,DOC,TEST,SMOKE)
LEG_BEGIN='    public Map<CharacterDerivedTendency, Double> derivedTendencies('
LEG_END='    /*\n     * =========================================================\n     * RESISTANCE / SUSCEPTIBILITY'
LEG_REPLACE='''    public Map<CharacterDerivedTendency, Double> derivedTendencies(NpcId npc) {
        NpcState state = requireNpc(npc);
        CharacterProfile profile = profiles.getOrCreate(npc);
        return CharacterPsychologyRules.evaluate(
            CharacterPsychologyRules.legacy(state.personality(), profile));
    }

'''
B_IM='import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2CharacterState;'
B_IM_NEW='import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2BPsychologyBridge;'
B_REG='        KnownWorldAv2CharacterState.register();'
B_APPLY='        KnownWorldAv2CharacterState.applyToSheet(sheet,p.site,p.id);'
CHAT_LOOP='for(String key:new String[]{"personality","traits","dialogueStyle","characterValues","socialNorms","skills"})'
CHAT_NEXT='for(String key:new String[]{"personality","traits","dialogueStyle","characterValues","socialNorms","skills","derivedTendencies"})'


def alter_legacy(text):
    if 'CharacterPsychologyRules.evaluate(' in text:
        if text.count('CharacterPsychologyRules.evaluate(')==1 and 'public Map<CharacterDerivedTendency, Double> derivedTendencies' in text:return text
        raise RuntimeError('Conflicting legacy psychology patch')
    if text.count(LEG_BEGIN)!=1 or text.count(LEG_END)!=1:
        raise RuntimeError('Legacy psychology method/section differs from reviewed master')
    start=text.index(LEG_BEGIN);end=text.index(LEG_END,start)
    body=text[start:end]
    # Count only actual result-map entries, not the EnumMap constructor's
    # CharacterDerivedTendency.class reference. The original method contains
    # twelve result.put(...) calls AND one .class reference (13 in total).
    expected = (
        'DECEITFUL_ADVANCEMENT', 'INTERVENTION', 'IMPULSIVE_CONFRONTATION',
        'PEER_CONFORMITY', 'RISK_TAKING', 'MERCY', 'VENGEANCE', 'LOYAL_DUTY',
        'POLITICAL_ASSERTIVENESS', 'RELIGIOUS_RECEPTIVENESS',
        'CULTURAL_OPENNESS', 'MANIPULATION_SUSCEPTIBILITY',
    )
    actual = tuple(re.findall(
        r'\bresult\s*\.\s*put\s*\(\s*CharacterDerivedTendency\.([A-Z][A-Z0-9_]*)\s*,',
        body,
    ))
    if (actual != expected or body.count('result.put(') != 12
            or 'CharacterDerivedTendency.class' not in body
            or 'manipulationSusceptibility(' not in body
            or 'return Map.copyOf(' not in body):
        raise RuntimeError('Unexpected old derived tendency formulas; inspect manually')
    return text[:start]+LEG_REPLACE+text[end:]


def alter_b41(text):
    if B_IM_NEW in text or 'KnownWorldAv2BPsychologyBridge.enrich(sheet);' in text:
        if all(text.count(x)==1 for x in (B_IM_NEW,'KnownWorldAv2BPsychologyBridge.register();','KnownWorldAv2BPsychologyBridge.enrich(sheet);')):return text
        raise RuntimeError('Incomplete AV2B B41 patch')
    if any(text.count(marker)!=1 for marker in (B_IM,B_REG,B_APPLY)):
        raise RuntimeError('AV2A not installed or unexpected worldwide B41 hooks')
    text=text.replace(B_IM,B_IM+'\n'+B_IM_NEW,1)
    text=text.replace(B_REG,B_REG+'\n        KnownWorldAv2BPsychologyBridge.register();',1)
    text=text.replace(B_APPLY,B_APPLY+'\n        KnownWorldAv2BPsychologyBridge.enrich(sheet);',1)
    return text


def alter_chat(text):
    if CHAT_NEXT in text:
        if text.count(CHAT_NEXT)!=1:return text if text.count(CHAT_NEXT)==1 else _bad('Ambiguous chat patch')
        return text
    if text.count(CHAT_LOOP)!=1:raise RuntimeError('Worldwide chat context loop has changed')
    return text.replace(CHAT_LOOP,CHAT_NEXT,1)


def _bad(reason):raise RuntimeError(reason)


def patches():
    for f in (LEG,B41,CHAT):
        if not (ROOT/f).is_file():raise RuntimeError('Missing source '+str(f))
    updated={}
    for f,func in ((LEG,alter_legacy),(B41,alter_b41),(CHAT,alter_chat)):
        old=(ROOT/f).read_bytes();s=old.decode('utf-8');nl='\r\n' if '\r\n' in s else '\n'
        patch=func(s.replace('\r\n','\n')).replace('\n',nl).encode('utf-8')
        updated[f]=(old,patch)
    return updated


def run(args):subprocess.run(args,cwd=ROOT,check=True)


def verify():
    required=(
      'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv1Vocabulary.java',
      'src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json',
      'src/main/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv2CharacterState.java',
    )
    for p in required:
        if not (ROOT/p).is_file():raise RuntimeError('AV1/AV2A prerequisite missing: '+p)
    for t in TARGETS:
        src=STAGE/t;dst=ROOT/t
        if not src.is_file():raise RuntimeError('AV2B staged file missing: '+str(t))
        if dst.exists() and dst.read_bytes()!=src.read_bytes():
            raise RuntimeError('Existing AV2B file differs (will not overwrite): '+str(t))
    edits=patches()
    run([sys.executable,str(STAGE/TEST),'--repo',str(ROOT),'--stage','AV2B_STAGE'])
    run([sys.executable,str(STAGE/SMOKE),'--repo',str(ROOT),'--stage','AV2B_STAGE'])
    return edits


def compile_mod():
    wrapper=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
    if not wrapper.is_file():raise RuntimeError('Gradle wrapper not found')
    cmd=['cmd','/c',str(wrapper),'compileJava','--no-daemon'] if os.name=='nt' else ['bash',str(wrapper),'compileJava','--no-daemon']
    run(cmd)


def install():
    edits=verify()
    gd=subprocess.run(['git','rev-parse','--git-dir'],cwd=ROOT,capture_output=True,text=True)
    if gd.returncode!=0:raise RuntimeError('Git working tree is required for protected source backups')
    gitdir=Path(gd.stdout.strip());gitdir=gitdir if gitdir.is_absolute() else (ROOT/gitdir).resolve()
    backup=gitdir/'av2b_backups'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    backup.mkdir(parents=True,exist_ok=False)
    for path,(old,_) in edits.items():
        outfile=backup/path.name;outfile.write_bytes(old)
    created=[];modified=[]
    try:
        for path in TARGETS:
            dst=ROOT/path
            if dst.exists():continue
            dst.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(STAGE/path,dst);created.append(dst)
        for path,(old,new) in edits.items():
            if old!=new:
                (ROOT/path).write_bytes(new);modified.append(path)
        print('AV2B source installed; compiling full Fabric Java project...')
        compile_mod()
    except Exception:
        for path in reversed(modified):(ROOT/path).write_bytes(edits[path][0])
        for file in reversed(created):
            if file.exists():file.unlink()
        print('AV2B ROLLBACK completed. Original sources preserved in '+str(backup),file=sys.stderr)
        raise
    print('AV2B INSTALLED AND COMPILE PASSED; backup '+str(backup))
    print('Try /kwpsych status in a disposable Minecraft test world.')

if __name__=='__main__':
    ap=argparse.ArgumentParser(); g=ap.add_mutually_exclusive_group(required=True)
    g.add_argument('--check-only',action='store_true');g.add_argument('--install',action='store_true')
    args=ap.parse_args()
    try:
        if args.check_only:
            changes=verify();print('AV2B CHECK PASS: all 12 legacy tendencies; canonical B4 profile; AV1/AV2A prerequisites. No files changed.')
            print('Managed changes: '+', '.join(str(k) for k,(a,b) in changes.items() if a!=b))
        else:install()
    except (RuntimeError,OSError,subprocess.CalledProcessError,AssertionError,UnicodeError) as exc:
        print('AV2B BLOCKED:',exc,file=sys.stderr);sys.exit(1)

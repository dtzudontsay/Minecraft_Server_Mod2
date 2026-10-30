#!/usr/bin/env python3
"""AV4A: guarded, reversible source patch on top of AV3 and AV2A-D.
No saves or runtime world state are mutated by the installer.
"""
import argparse, os, shutil, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
STAGE=ROOT/'AV4_STAGE'
BASE=Path('src/main/java/dev/dtzudontsay/knownworld')
B41=BASE/'simulation/population/KnownWorldB41Activation.java'
DAILY=BASE/'simulation/population/KnownWorldB42DailyLife.java'
CHAT=BASE/'simulation/population/KnownWorldB41DialogueService.java'
RULES=BASE/'simulation/attributes/KnownWorldAv4DecisionRules.java'
DEC=BASE/'simulation/attributes/KnownWorldAv4Decisions.java'
DOC=Path('docs/architecture/24_37_AV4A_FREE_TIME_DECISIONS_AND_ACCEPTANCE.md')
COVER=Path('docs/architecture/AV4_ATTRIBUTE_COVERAGE.csv')
XLSX=Path('docs/architecture/AV4_Attribute_Coverage.xlsx')
TEST=Path('tools/av4/test_av4_contract.py')
SMOKE=Path('tools/av4/test_av4_java_smoke.py')
ADAPTER=Path('tools/av4/test_av4_adapter_smoke.py')
TARGETS=(RULES,DEC,DOC,COVER,XLSX,TEST,SMOKE,ADAPTER)
IMP='import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4Decisions;'
REG='        KnownWorldAv3Formation.register();'
NEWREG='        KnownWorldAv4Decisions.register();'
M1='        enrich(profile,p,site); // Same deterministic personality as right-click inspection.'
M2='        enrich(sheet,p,site);\n        b42Sheet(sheet,p,site);'
MARK='    /** AV2A: only currently materialized canonical B4.0 identities are editable. */'
PROF='''    /** AV4: current visible B4 identity only; never allocate a legacy NpcId. */
    public static JsonObject av4ProfileFor(String personId,String siteId) {
        if(siteId==null || !siteId.equals(av2VisibleSite(personId)))return null;
        Person person=visiblePeople.get(personId);
        Site site=visibleSites.get(personId);
        if(person==null || site==null || !siteId.equals(person.site) || !siteId.equals(site.id))return null;
        JsonObject profile=new JsonObject();
        enrich(profile,person,site);
        return profile;
    }
'''
D_IMPORT=IMP
D_ANCHOR='            Work leisure=leisurePlace(personId,siteId,homeX,homeZ,offsetX,offsetZ,'
D_REPLACE='''            var av4=KnownWorldAv4Decisions.choose(personId,siteId,day());
            Work leisure=(av4==null || av4.action()==
                    dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4DecisionRules.Action.PUBLIC_LEISURE)
                    ?leisurePlace(personId,siteId,homeX,homeZ,offsetX,offsetZ,
                                          year()-birthYear<16):null;
'''
D_ORIG='''            Work leisure=leisurePlace(personId,siteId,homeX,homeZ,offsetX,offsetZ,
                                          year()-birthYear<16);'''
D_NBR='''                    if(Math.floorMod(personId.hashCode()+(int)day(),7)==0 &&'''
D_NEW_NBR='''                    if((av4!=null && av4.action()==
                            dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4DecisionRules.Action.NEIGHBOR_VISIT ||
                         av4==null && Math.floorMod(personId.hashCode()+(int)day(),7)==0) &&'''

def patch_b41(s):
    if PROF in s and NEWREG in s and s.count('KnownWorldAv4Decisions.enrich(')==2 and s.count(IMP)==1:
        return s
    if PROF in s or NEWREG in s or 'KnownWorldAv4Decisions.enrich(' in s or IMP in s:
        raise RuntimeError('Partially applied AV4 B41 integration')
    for marker in (REG,M1,M2,MARK):
        if s.count(marker)!=1:raise RuntimeError('AV4 B41 anchor absent or duplicated: '+marker[:76])
    if s.count('KnownWorldAv3Formation.applyToSheet(sheet,p.site,p.id,')!=1:
        raise RuntimeError('AV3 not actually installed in B41')
    s=s.replace('package dev.dtzudontsay.knownworld.simulation.population;\n',
                'package dev.dtzudontsay.knownworld.simulation.population;\n\n'+IMP+'\n',1)
    s=s.replace(REG,REG+'\n'+NEWREG,1)
    s=s.replace(M1,M1+'\n        KnownWorldAv4Decisions.enrich(profile,p.id);',1)
    s=s.replace(M2,'        enrich(sheet,p,site);\n        KnownWorldAv4Decisions.enrich(sheet,p.id);\n        b42Sheet(sheet,p,site);',1)
    s=s.replace(MARK,PROF+'\n'+MARK,1)
    return s

def patch_daily(s):
    if D_REPLACE.strip() in s and D_NEW_NBR in s and s.count(IMP)==1:return s
    if D_REPLACE.strip() in s or D_NEW_NBR in s or IMP in s:raise RuntimeError('Partial AV4 daily-life integration')
    if s.count(D_ORIG)!=1 or s.count(D_NBR)!=1:raise RuntimeError('B4.2 movement/free-time anchor changed')
    s=s.replace('package dev.dtzudontsay.knownworld.simulation.population;\n',
                'package dev.dtzudontsay.knownworld.simulation.population;\n\n'+IMP+'\n',1)
    s=s.replace(D_ORIG,D_REPLACE.rstrip('\n'),1)
    s=s.replace(D_NBR,D_NEW_NBR,1)
    return s

DIALOGUE_LIST='for(String key:new String[]{"personality","traits","dialogueStyle","characterValues","socialNorms","skills","derivedTendencies"}) {'
DIALOGUE_AV4='for(String key:new String[]{"personality","traits","dialogueStyle","characterValues","socialNorms","skills","derivedTendencies","av4FreeTimeDecision"}) {'
DIALOGUE_POLICY='                Your spoken words do not execute trades, quests, movement, jobs or political actions.'
DIALOGUE_NEW_POLICY='                A free-time AV4 decision is only a preference/intent, NOT evidence of a completed visit or social encounter.'

def patch_chat(s):
    if DIALOGUE_AV4 in s and DIALOGUE_NEW_POLICY in s:return s
    if DIALOGUE_AV4 in s or DIALOGUE_NEW_POLICY in s:raise RuntimeError('Partial AV4 K-chat policy update')
    if s.count(DIALOGUE_LIST)!=1 or s.count(DIALOGUE_POLICY)!=1:
        raise RuntimeError('K-chat safety/context anchors changed')
    return s.replace(DIALOGUE_LIST,DIALOGUE_AV4,1).replace(DIALOGUE_POLICY,DIALOGUE_POLICY+'\n'+DIALOGUE_NEW_POLICY,1)

def run(args):subprocess.run(args,cwd=ROOT,check=True)

def verify():
    for p in TARGETS:
        staged=STAGE/p;actual=ROOT/p
        if not staged.is_file():raise RuntimeError('Missing staged AV4 file: '+str(p))
        if actual.exists() and actual.read_bytes()!=staged.read_bytes():
            raise RuntimeError('AV4 target already differs; never overwrite: '+str(p))
    needs=[BASE/'simulation/attributes'/n for n in (
        'KnownWorldAv1Vocabulary.java','KnownWorldAv2CharacterState.java',
        'KnownWorldAv2BPsychologyBridge.java','KnownWorldAv2CExperienceJournal.java',
        'KnownWorldAv2DChangeJournal.java','KnownWorldAv2DChangePolicy.java',
        'KnownWorldAv3Formation.java','KnownWorldAv3RegionalPriors.java')]
    for p in needs:
        if not (ROOT/p).is_file():raise RuntimeError('Required AV1-AV3 class missing: '+str(p))
    changes={}
    for p,patch in ((B41,patch_b41),(DAILY,patch_daily),(CHAT,patch_chat)):
        if not (ROOT/p).is_file():raise RuntimeError('Missing existing Java file: '+str(p))
        original=(ROOT/p).read_bytes();raw=original.decode('utf-8')
        updated=patch(raw.replace('\r\n','\n'))
        new=updated.replace('\n','\r\n').encode('utf-8') if '\r\n' in raw else updated.encode('utf-8')
        changes[p]=(original,new)
    run([sys.executable,str(STAGE/TEST),'--repo',str(ROOT),'--stage','AV4_STAGE'])
    run([sys.executable,str(STAGE/SMOKE),'--repo',str(ROOT),'--stage','AV4_STAGE'])
    run([sys.executable,str(STAGE/ADAPTER),'--repo',str(ROOT),'--stage','AV4_STAGE'])
    return changes

def install():
    changes=verify()
    r=subprocess.run(['git','rev-parse','--git-dir'],cwd=ROOT,text=True,capture_output=True)
    if r.returncode:raise RuntimeError('Git repository required for backup')
    g=Path(r.stdout.strip());g=g if g.is_absolute() else (ROOT/g).resolve()
    backup=g/'av4_backups'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    backup.mkdir(parents=True,exist_ok=False)
    for p,(original,_) in changes.items():
        copy=backup/p;copy.parent.mkdir(parents=True,exist_ok=True);copy.write_bytes(original)
    created=[];touched=[]
    try:
        for p in TARGETS:
            dest=ROOT/p
            if dest.exists():continue
            dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(STAGE/p,dest);created.append(dest)
        for p,(old,new) in changes.items():
            if old!=new:(ROOT/p).write_bytes(new);touched.append(p)
        gradle=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
        if not gradle.is_file():raise RuntimeError('Missing Gradle wrapper')
        command=['cmd','/c',str(gradle),'compileJava','--no-daemon'] if os.name=='nt' else ['bash',str(gradle),'compileJava','--no-daemon']
        run(command)
    except BaseException:
        for p in reversed(touched):(ROOT/p).write_bytes(changes[p][0])
        for p in reversed(created):
            if p.is_file():p.unlink()
        print('AV4 source rollback complete; backup: '+str(backup),file=sys.stderr)
        raise
    print('AV4 INSTALL PASS; backup: '+str(backup))
    print('Minecraft checkpoints: /kwdecisions status; /kwdecisions preview "<actual-person-id>"; right-click B4 projected actor.')

if __name__=='__main__':
    a=argparse.ArgumentParser();g=a.add_mutually_exclusive_group(required=True)
    g.add_argument('--check-only',action='store_true');g.add_argument('--install',action='store_true')
    opts=a.parse_args()
    try:
        if opts.check_only:
            changes=verify()
            print('AV4 CHECK PASS: canonical lineage, Java21 pure decision test, AV3 and B4.2 hooks, no files modified.')
            print('AV4 source patches needed:',sum(a!=b for a,b in changes.values()))
        else:install()
    except (RuntimeError,ValueError,OSError,AssertionError,UnicodeError,subprocess.CalledProcessError) as error:
        print('AV4 BLOCKED:',error,file=sys.stderr);sys.exit(1)

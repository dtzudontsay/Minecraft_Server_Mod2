#!/usr/bin/env python3
"""Guarded AV4B install into a LOCAL checkout. No Git operations or save changes."""
import argparse
import datetime
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
PKG = Path(__file__).resolve().parents[2]  # extracted package relative to checkout
REL = Path('src/main/java/dev/dtzudontsay/knownworld/simulation')
ACT = ROOT / REL / 'population/KnownWorldB41Activation.java'
DAILY = ROOT / REL / 'population/KnownWorldB42DailyLife.java'
DIALOGUE = ROOT / REL / 'population/KnownWorldB41DialogueService.java'
NEW = [
    REL / 'attributes/KnownWorldAv4BGoalRules.java',
    REL / 'attributes/KnownWorldAv4BGoals.java',
    Path('src/test/java/dev/dtzudontsay/knownworld/simulation/attributes/KnownWorldAv4BGoalRulesTest.java'),
    Path('docs/architecture/24_38_AV4B_GENERAL_AUTONOMOUS_GOALS.md'),
]
SCRIPT_DIR = Path(__file__).resolve().parent
# Shipped payloads live under tools/av4b/payload, never copied over a checkout
# until all source contracts have passed.
PAYLOAD = SCRIPT_DIR / 'payload'
BEGIN = '// AV4B_GENERAL_GOALS_BEGIN'

class Blocked(RuntimeError):
    pass

def one(s, old, new, label):
    n = s.count(old)
    if n != 1:
        raise Blocked(f'{label}: expected one exact anchor, found {n}; NO FILES MODIFIED')
    return s.replace(old, new, 1)

def read(path):
    if not path.is_file():
        raise Blocked(f'Missing {path.relative_to(ROOT) if path.is_relative_to(ROOT) else path}; NO FILES MODIFIED')
    return path.read_bytes().decode('utf-8').replace('\r\n','\n')

def preserve_line_endings(original, new):
    return new.replace('\n','\r\n') if b'\r\n' in original and b'\n' in original else new

def patch_activation(s):
    if BEGIN in s:
        if 'KnownWorldAv4BGoals.register();' not in s or s.count('KnownWorldAv4BGoals.enrich(')!=2:
            raise Blocked('AV4B activation partially installed or modified')
        return s
    s = one(s, 'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4Decisions;',
        'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4Decisions;\n'
        'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4BGoals;', 'activation import')
    s = one(s, '        KnownWorldAv4Decisions.register();',
        '        KnownWorldAv4Decisions.register();\n'
        '        '+BEGIN+'\n        KnownWorldAv4BGoals.register();', 'register AV4B once')
    s = one(s, '        KnownWorldAv4Decisions.enrich(profile,p.id);',
        '        KnownWorldAv4Decisions.enrich(profile,p.id);\n'
        '        KnownWorldAv4BGoals.enrich(profile,p.id);', 'dialogue identity enrich')
    s = one(s, '        KnownWorldAv4Decisions.enrich(sheet,p.id);',
        '        KnownWorldAv4Decisions.enrich(sheet,p.id);\n'
        '        KnownWorldAv4BGoals.enrich(sheet,p.id);', 'right-click character sheet')
    return s

def patch_dialogue(s):
    if BEGIN in s:
        if 'KnownWorldAv4BGoals.promptFor(p.id())' not in s:
            raise Blocked('AV4B dialogue partially installed')
        return s
    # This anchor is unrelated to the AV4-QOL model, prompt style or NPC lookup changes.
    s = one(s, 'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2CExperienceJournal;',
            'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv2CExperienceJournal;\n'
            'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4BGoals;', 'dialogue import')
    s = one(s, '        prompt.append(KnownWorldAv2CExperienceJournal.promptFor(p.id()));',
            '        prompt.append(KnownWorldAv2CExperienceJournal.promptFor(p.id()));\n'
            '        '+BEGIN+'\n'
            '        prompt.append(KnownWorldAv4BGoals.promptFor(p.id()));', 'dialogue verified goal context')
    return s

def patch_daily(s):
    if BEGIN in s:
        if 'KnownWorldAv4BGoals.choose(personId,siteId,day(),context)' not in s:
            raise Blocked('AV4B daily routine partially installed')
        return s
    s = one(s,'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4Decisions;',
            'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4Decisions;\n'
            'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4BGoals;\n'
            'import dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4BGoalRules;', 'daily import')
    # Keep the schedule, primary work path, event authority, route controller and diagnostics verbatim.
    start='            }else if(activity.equals("FREE_TIME")||activity.equals("HOME_AND_FREE_TIME")||\n                     activity.equals("REST_DAY")) {'
    end='\n            }\n            int[] fort=KnownWorldC411GlobalWorldgen.b42FortificationForInspection(siteId);'
    if s.count(start)!=1 or s.count(end)!=1:
        raise Blocked('B4.2 free-time/route contract changed; NO FILES MODIFIED')
    i=s.index(start); j=s.index(end,i)
    # Existing branch cannot be silently rewritten if a later local variant changed the policy.
    branch=s[i:j]
    for required in ['KnownWorldAv4Decisions.choose(personId,siteId,day())',
                     'leisurePlace(personId,siteId,homeX,homeZ,offsetX,offsetZ,',
                     'VISIT_NEIGHBOR:', 'getEntitiesOfClass(Villager.class,']:
        if required not in branch:
            raise Blocked('B4.2 free-time anchor absent '+required+'; NO FILES MODIFIED')
    replacement='''            }else if(activity.equals("FREE_TIME")||activity.equals("HOME_AND_FREE_TIME")||
                     activity.equals("REST_DAY")) {
                // AV4A remains available as a diagnostic / fallback decision, not
                // an authority that overrides the AV4B feasible-goal policy.
                var av4=KnownWorldAv4Decisions.choose(personId,siteId,day());
                Work leisure=leisurePlace(personId,siteId,homeX,homeZ,offsetX,offsetZ,
                                         year()-birthYear<16);
                Villager neighbor=null;
                if(villager.level() instanceof ServerLevel local) {
                    double closest=18.0*18.0;
                    for(Villager candidate:local.getEntitiesOfClass(Villager.class,
                            villager.getBoundingBox().inflate(18.0))) {
                        if(candidate==villager||!candidate.isAlive()||
                            !candidate.entityTags().contains("kwb41_actor"))continue;
                        double distance=villager.distanceToSqr(candidate);
                        if(distance>9 && distance<closest){neighbor=candidate;closest=distance;}
                    }
                }
                double recovery=activity.equals("HOME_AND_FREE_TIME") &&
                        phase((ServerLevel)villager.level())>=r.end()?0.63:
                        activity.equals("REST_DAY")?0.36:0.26;
                var context=new KnownWorldAv4BGoalRules.Context(activity,work!=null,
                        leisure!=null,neighbor!=null,year()-birthYear>=16,recovery);
                var goal=KnownWorldAv4BGoals.choose(personId,siteId,day(),context);
                KnownWorldAv4BGoalRules.Goal selected=goal==null?null:goal.goal();
                if(selected==KnownWorldAv4BGoalRules.Goal.OPTIONAL_WORK && work!=null) {
                    // Work opportunity means a registered worksite reservation only.
                    // THIS MUST NOT call observedAtWork, apply wages or mint receipts.
                    tx=work.x()+offsetX+work.w()/2;tz=work.z()+offsetZ+work.d()+2;
                    role="AV4B:OPTIONAL_WORK_INTENT:"+work.id();
                }else if(selected==KnownWorldAv4BGoalRules.Goal.PUBLIC_LEISURE && leisure!=null) {
                    tx=leisure.x()+leisure.w()/2+offsetX;
                    tz=leisure.z()+leisure.d()+2+offsetZ;
                    role="AV4B:PUBLIC_LEISURE_INTENT:"+leisure.id();
                }else if(selected==KnownWorldAv4BGoalRules.Goal.SOCIAL_VISIT && neighbor!=null) {
                    tx=(int)Math.floor(neighbor.getX())+2;
                    tz=(int)Math.floor(neighbor.getZ())+1;
                    role="AV4B:SOCIAL_VISIT_INTENT:"+neighbor.getUUID();
                }else if(selected!=null) {
                    role=selected==KnownWorldAv4BGoalRules.Goal.RECOVER?
                            "AV4B:RECOVERY_AT_HOME_INTENT:"+personId:
                            "AV4B:HOUSEHOLD_ACTIVITY_INTENT:"+personId;
                }else {
                    // Fail open: preserve AV4A movement if profile or state is blocked.
                    if(leisure!=null && (av4==null || av4.action()==
                         dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4DecisionRules.Action.PUBLIC_LEISURE)) {
                        tx=leisure.x()+leisure.w()/2+offsetX;
                        tz=leisure.z()+leisure.d()+2+offsetZ;role="VISIT:"+leisure.id();
                    }else if(neighbor!=null && (av4!=null && av4.action()==
                         dev.dtzudontsay.knownworld.simulation.attributes.KnownWorldAv4DecisionRules.Action.NEIGHBOR_VISIT ||
                         av4==null && Math.floorMod(personId.hashCode()+(int)day(),7)==0)) {
                        tx=(int)Math.floor(neighbor.getX())+2;
                        tz=(int)Math.floor(neighbor.getZ())+1;
                        role="VISIT_NEIGHBOR:"+neighbor.getUUID();
                    }else role="HOUSEHOLD:"+personId;
                }
'''
    s=s[:i]+replacement+s[j:]
    # Evaluate non-discretionary phases for explainable schedule-priority decisions,
    # without changing B4.2 work/rest/navigation code or work event receipts.
    hook='            int tx=home[0],tz=home[1];String role=activity;'
    inject='''            int tx=home[0],tz=home[1];String role=activity;
            '''+BEGIN+'''
            if(!(activity.equals("FREE_TIME")||activity.equals("HOME_AND_FREE_TIME")||
                 activity.equals("REST_DAY"))) {
                var context=new KnownWorldAv4BGoalRules.Context(activity,work!=null,
                        false,false,year()-birthYear>=16,0.35);
                KnownWorldAv4BGoals.choose(personId,siteId,day(),context);
            }'''
    s=one(s,hook,inject,'schedule-priority diagnostic')
    return s

def build():
    sources={ACT:patch_activation(read(ACT)),DAILY:patch_daily(read(DAILY)),
             DIALOGUE:patch_dialogue(read(DIALOGUE))}
    for rel in NEW:
        path=ROOT/rel
        supplied=PAYLOAD/rel
        if not supplied.is_file():raise Blocked(f'Missing packaged payload {rel}')
        current=path.read_bytes() if path.is_file() else None
        expected=supplied.read_bytes()
        if current is not None and current!=expected:
            raise Blocked(f'Existing user file differs from AV4B package: {rel}; NO FILES MODIFIED')
        sources[path]=expected.decode('utf-8')
    planned={}
    for path,updated in sources.items():
        prior=path.read_bytes() if path.is_file() else None
        payload=updated.encode('utf-8')
        if prior is not None and path in [ACT,DAILY,DIALOGUE]:
            if b'\r\n' in prior:
                payload=payload.replace(b'\n',b'\r\n')
        planned[path]=(prior,payload)
    return planned

def digest(data):return hashlib.sha256(data).hexdigest()[:16] if data is not None else 'NEW'

def install(planned):
    changed={p:x for p,x in planned.items() if x[0]!=x[1]}
    if not changed:
        print('AV4B already installed; no files modified.')
        return
    stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    backup=ROOT/'.av4b_backups'/stamp
    # Never overwrite prior backups if a retry happens within one second.
    counter=0
    while backup.exists():
        counter+=1;backup=ROOT/'.av4b_backups'/(stamp+'_'+str(counter))
    backup.mkdir(parents=True)
    for path,(before,after) in changed.items():
        if before is not None:
            dest=backup/path.relative_to(ROOT)
            dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(before)
    print('BACKUP:',backup,flush=True)
    try:
        for path,(before,after) in changed.items():
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_bytes(after)
        gradle=ROOT/'gradlew.bat' if os.name=='nt' else ROOT/'gradlew'
        if not gradle.exists():raise Blocked('Gradle wrapper not found; rolling back all source changes')
        commands=[[str(gradle),'compileJava','--no-daemon'],[str(gradle),'test','--no-daemon']]
        for command in commands:
            print('RUN:', ' '.join(command),flush=True)
            result=subprocess.run(command,cwd=ROOT,shell=False)
            if result.returncode:
                raise Blocked('Gradle '+command[1]+' exited '+str(result.returncode))
    except (Exception,KeyboardInterrupt) as exc:
        for path,(before,after) in changed.items():
            if before is None:path.unlink(missing_ok=True)
            else:path.write_bytes(before)
            print('RESTORED/REMOVED:',path.relative_to(ROOT))
        raise Blocked('AV4B INSTALL BLOCKED; rolled back modified files: '+str(exc))
    print('AV4B INSTALL+COMPILE+TEST PASSED. GitHub and worlds untouched.')


def main():
    ap=argparse.ArgumentParser()
    group=ap.add_mutually_exclusive_group(required=True)
    group.add_argument('--check',action='store_true');group.add_argument('--install',action='store_true')
    args=ap.parse_args()
    try:
        plan=build()
        for path,(before,after) in plan.items():
            print(('READY' if before!=after else 'UNCHANGED'),path.relative_to(ROOT),
                  '| oldSHA256='+digest(before),'| newSHA256='+digest(after))
        if args.check:print('AV4B CHECK PASSED: safe anchors located, no source modified')
        else: install(plan)
    except Blocked as exc:
        print('AV4B BLOCKED:',str(exc),file=sys.stderr)
        return 1
    return 0

if __name__=='__main__':sys.exit(main())

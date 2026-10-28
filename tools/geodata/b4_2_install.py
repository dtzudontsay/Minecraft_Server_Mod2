#!/usr/bin/env python3
"""B4.2 safe patch-only installer. Requires B4.1B and C4.15. No save changes."""
from __future__ import annotations
import argparse,datetime,json,pathlib,re,shutil
ROOT=pathlib.Path(__file__).resolve().parents[2]
ACT=pathlib.Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java')
DIALOG=pathlib.Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueService.java')
UI=pathlib.Path('src/main/java/dev/dtzudontsay/knownworld/client/casestudy/WinterfellCharacterScreen.java')
DAY=pathlib.Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42DailyLife.java')
SCHEDULE=pathlib.Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42Schedule.java')
MARK='// B4_2_DAILY_LIFE_INTEGRATION'

def once(src,old,new,tag):
    n=src.count(old)
    if n!=1:raise ValueError(f'{tag}: expected one anchor, found {n}. Nothing was changed.')
    return src.replace(old,new,1)

def patch_activation(src):
    if MARK in src:return src
    if 'public record DialogueIdentity(' not in src or 'kwb41_actor' not in src or 'b41ApprovedPlacementForInspection' not in src:
        raise ValueError('B4.1B/C4.15 projection integration missing')
    src=once(src,'    public static void register() {',
        '    public static void register() {\n        '+MARK+'\n        KnownWorldB42DailyLife.register();','activation register')
    # These two insertions use the candidate's verified C4.15 site displacement
    # without changing projection identity selection or NPC cap.
    anchor='''                homesOfProjected.put(p.id,new BlockPos(c.homeX,64,c.homeZ));
                wanted.add(p.id);admitted++;continue;'''
    src=once(src,anchor,'''                homesOfProjected.put(p.id,new BlockPos(c.homeX,64,c.homeZ));
                b42Move(v,c);
                wanted.add(p.id);admitted++;continue;''','existing actor')
    anchor='''            homesOfProjected.put(p.id,new BlockPos(c.homeX,64,c.homeZ));
            wanted.add(p.id);admitted++;spawned++;'''
    src=once(src,anchor,'''            homesOfProjected.put(p.id,new BlockPos(c.homeX,64,c.homeZ));
            b42Move(v,c);
            wanted.add(p.id);admitted++;spawned++;''','new actor')
    anchor='''    private static void removeMissing(ServerLevel level,Set<String> wanted) {'''
    helper='''    /** B4.2 bounded local pathfinding. C4.15 approved placement is reused. */
    private static void b42Move(Villager v,Candidate c) {
        int[] approved=KnownWorldC411GlobalWorldgen.b41ApprovedPlacementForInspection(c.site.id);
        if(approved==null)return;
        KnownWorldB42DailyLife.applyProjection(v,c.person.id,c.person.site,c.person.workplace,
                c.person.occupation,c.person.birthYear,c.homeX,c.homeZ,
                approved[0]-c.site.anchorX,approved[1]-c.site.anchorZ);
    }
    private static void b42Sheet(JsonObject sheet,Person p,Site site) {
        int[] approved=KnownWorldC411GlobalWorldgen.b41ApprovedPlacementForInspection(site.id);
        if(approved==null)return;
        int ox=approved[0]-site.anchorX,oz=approved[1]-site.anchorZ;
        int x=p.x+p.width/2+ox,z=p.z+p.depth+2+oz;
        KnownWorldB42DailyLife.enrichSheet(sheet,p.id,p.site,p.workplace,p.occupation,
                                            p.birthYear,x,z,ox,oz);
    }
'''
    src=once(src,anchor,helper+anchor,'helper insertion')
    src=once(src,'        enrich(sheet,p,site);','        enrich(sheet,p,site);\n        b42Sheet(sheet,p,site);','sheet details')
    # Include current simulated activity in the profile forwarded to K-chat.
    src=once(src,'        enrich(profile,p,site); // Same deterministic personality as right-click inspection.',
       '        enrich(profile,p,site); // Same deterministic personality as right-click inspection.\n        b42Sheet(profile,p,site);','dialogue profile details')
    return src

def patch_dialogue(src):
    if MARK in src:return src
    if 'KnownWorldB41Activation.DialogueIdentity' not in src or 'SCHEDULE MODEL ONLY' in src:
        raise ValueError('B4.1B dialogue service incompatible with B4.2')
    old='''                .append("Age at 298 AC: ").append(p.startAge()).append("\\n");'''
    return once(src,old,'''                .append("Age at 298 AC: ").append(p.startAge()).append("\\n");
        '''+MARK+'''
        prompt.append(KnownWorldB42DailyLife.promptFor(p.id(),p.workplace(),
                       p.occupation(),298-p.startAge()));''','LLM daily context')

def patch_ui(src):
    if MARK in src:return src
    anchor='''            case 4 -> {
                array(rows,"motivations","MOTIVATIONS");'''
    new='''            case 4 -> {
                // B4_2_DAILY_LIFE_INTEGRATION: inferred schedules, NOT observed actions.
                raw(rows,"Scheduled current activity","dailyActivity");
                raw(rows,"Work shift (Minecraft ticks)","dailyWorkSchedule");
                raw(rows,"Modeled workdays","modeledWorkdays");
                raw(rows,"Estimated travel ticks","commuteTicksEstimate");
                raw(rows,"Estimated practice gain","practiceGainEstimate");
                array(rows,"motivations","MOTIVATIONS");'''
    return once(src,anchor,new,'Life tab')

def apply(root=ROOT,check=False):
    root=pathlib.Path(root).resolve()
    for rel in (ACT,DIALOG,UI,DAY,SCHEDULE):
        if not (root/rel).is_file():raise FileNotFoundError('Missing required B4.2/B4.1B file: '+str(rel))
    old={p:(root/p).read_text(encoding='utf8') for p in (ACT,DIALOG,UI)}
    new={ACT:patch_activation(old[ACT]),DIALOG:patch_dialogue(old[DIALOG]),UI:patch_ui(old[UI])}
    changed=[p for p in old if new[p]!=old[p]]
    print(json.dumps({'root':str(root),'changes':[str(p) for p in changed],
                      'alreadyInstalled':len(changed)==0,'checkOnly':check},indent=2))
    if check or not changed:return
    backup=root/'tools/geodata/output/b4_2_source_backups'/datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    for p in changed:
        dest=backup/p;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(root/p,dest)
    print('Original source backups:',backup)
    # Validate and back up ALL inputs before first update. Atomic per-file replacement.
    for p in changed:
        dest=root/p;tmp=dest.with_suffix(dest.suffix+'.b42tmp')
        tmp.write_text(new[p],encoding='utf8');tmp.replace(dest)
    print('B4.2 integration complete; existing worlds/people unchanged.')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--root',type=pathlib.Path,default=ROOT)
    parser.add_argument('--check',action='store_true');a=parser.parse_args();apply(a.root,a.check)

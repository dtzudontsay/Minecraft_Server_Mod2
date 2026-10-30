#!/usr/bin/env python3
"""QOL hotfix for KnownWorld, guarded against edited source anchors.
Python 3.10+; no external dependencies. Safe read-only --check operation.
"""
import argparse
import datetime
import hashlib
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population')
ACTIVATION = ROOT / 'KnownWorldB41Activation.java'
DIALOGUE = ROOT / 'KnownWorldB41DialogueService.java'
MARK = 'KNOWNWORLD_AV4_DIALOGUE_QOL_V1'

def require_once(s, needle, label):
    n=s.count(needle)
    if n != 1: raise RuntimeError(f'{label}: expected one exact anchor, found {n}; NO FILES MODIFIED')

def activation_patch(s):
    if MARK in s: return s
    anchor='''                        .then(Commands.literal("nearby").executes(ctx->nearby(ctx.getSource())))));'''
    replacement='''                        .then(Commands.literal("nearby").executes(ctx->nearby(ctx.getSource())))
                        // KNOWNWORLD_AV4_DIALOGUE_QOL_V1: OP-only active projection lookup.
                        .then(Commands.literal("list")
                            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                            .executes(ctx->qolList(ctx.getSource())))
                        .then(Commands.literal("where")
                            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                            .then(Commands.argument("personId",com.mojang.brigadier.arguments.StringArgumentType.string())
                                .executes(ctx->qolWhere(ctx.getSource(),com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"personId")))))
                        .then(Commands.literal("sheet")
                            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                            .then(Commands.argument("personId",com.mojang.brigadier.arguments.StringArgumentType.string())
                                .executes(ctx->qolSheet(ctx.getSource(),com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"personId")))))
                        .then(Commands.literal("goto")
                            .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                            .then(Commands.argument("personId",com.mojang.brigadier.arguments.StringArgumentType.string())
                                .executes(ctx->qolGoto(ctx.getSource(),com.mojang.brigadier.arguments.StringArgumentType.getString(ctx,"personId")))))));'''
    require_once(s,anchor,'AV4 actor command registration');s=s.replace(anchor,replacement)
    anchor2='''        JsonObject sheet=new JsonObject();
        sheet.addProperty("name",p.name);sheet.addProperty("id",p.id);'''
    replacement2='''        qolSendSheet(player,p,site);
    }
    /** Reuse the exact right-click sheet for a validated, currently projected person. */
    private static void qolSendSheet(ServerPlayer player,Person p,Site site) {
        JsonObject sheet=new JsonObject();
        sheet.addProperty("name",p.name);sheet.addProperty("id",p.id);'''
    require_once(s,anchor2,'Right-click sheet construction');s=s.replace(anchor2,replacement2)
    anchor3='''    private static void say(CommandSourceStack src,String s){src.sendSuccess(()->Component.literal(s),false);}'''
    insert='''    /** The B4 identity ID is not a Minecraft entity UUID. All lookups are active-only. */
    private static Entity qolActor(String id) {
        if(id==null || !id.matches("b4:site_[a-z][a-z0-9_]{1,90}:[A-Za-z0-9:_-]+"))return null;
        if(activeServer==null || !id.equals(id.trim()))return null;
        UUID uid=projected.get(id);
        Person person=visiblePeople.get(id);
        ServerLevel level=activeServer.getLevel(Level.OVERWORLD);
        if(uid==null || person==null || level==null)return null;
        Entity entity=level.getEntity(uid);
        if(entity==null || !entity.isAlive() || !entity.entityTags().contains(TAG)
                || !entity.entityTags().contains(ID_PREFIX+id))return null;
        return entity;
    }
    private static int qolList(CommandSourceStack src) {
        say(src,"[AV4-QOL] Current active B4 physical projections="+projected.size()+
                ". Full IDs shown (maximum 35); use /kwactors where \\\"ID\\\" to check a specific one.");
        visiblePeople.values().stream().sorted(Comparator.comparing(p->p.name)).limit(35)
                .forEach(p->say(src,"  "+p.name+" | "+p.id+" | "+p.site));
        return 1;
    }
    /** Bounded source lookup for unloaded people. Never claims source home is live position. */
    private static int[] qolSourceHome(String id) {
        if(id==null || !id.matches("b4:site_[a-z][a-z0-9_]{1,90}:[A-Za-z0-9:_-]+"))return null;
        String siteId=id.substring(3,id.indexOf(':',3));
        Site site=siteIndex().get(siteId);
        if(site==null)return null;
        int indexed=0;
        for(Integer count:site.tiles.values()) {
            if(count==null || count<0)throw new IllegalStateException("Invalid B4 source tile census");
            indexed+=count;
            if(indexed>10000)throw new IllegalStateException("Large site requires incremental offline ID index; refusing main-thread scan");
        }
        if(site.tiles.size()>32)throw new IllegalStateException("Too many source tiles for bounded lookup");
        int[] approved=KnownWorldC411GlobalWorldgen.b41ApprovedPlacementForInspection(site.id);
        if(approved==null)throw new IllegalStateException("No approved physical placement for this B4 site");
        for(String tileId:site.tiles.keySet())for(Person person:tile(site,tileId)) {
            if(!person.id.equals(id))continue;
            return new int[]{person.x+person.width/2+approved[0]-site.anchorX,
                    person.z+person.depth+2+approved[1]-site.anchorZ};
        }
        return null;
    }
    private static int qolWhere(CommandSourceStack src,String id) {
        Entity e=qolActor(id);
        if(e==null) {
            try {
                int[] home=qolSourceHome(id);
                if(home==null){say(src,"[AV4-QOL] Not currently projected and no verified B4 source-home record found for "+id);return 0;}
                say(src,"[AV4-QOL] "+id+" is NOT currently materialized. Its original B4 source home is at X="+home[0]+" Z="+home[1]+". This is NOT a live NPC position.");
                return 1;
            } catch(Exception ex){say(src,"[AV4-QOL] Source home lookup blocked: "+ex.getMessage());return 0;}
        }
        Person person=visiblePeople.get(id);
        say(src,"[AV4-QOL] "+person.name+" | "+id+" | live world position "+
                e.blockPosition().toShortString()+" | site "+person.site+". Use /kwactors sheet or /kwactors goto.");
        return 1;
    }
    private static int qolSheet(CommandSourceStack src,String id) {
        try {
            ServerPlayer player=src.getPlayerOrException();
            Entity e=qolActor(id);
            if(e==null || player.level()!=e.level()) {
                say(src,"[AV4-QOL] B4 character is not currently projected in your dimension. No false profile produced.");
                return 0;
            }
            Person person=visiblePeople.get(id);
            Site site=visibleSites.get(id);
            if(person==null || site==null || !person.site.equals(site.id))return 0;
            qolSendSheet(player,person,site);
            say(src,"[AV4-QOL] Full active character sheet reopened for "+person.name+" ["+id+"].");
            return 1;
        } catch(Exception ex){say(src,"[AV4-QOL] Cannot show sheet: "+ex.getMessage());return 0;}
    }
    private static int qolGoto(CommandSourceStack src,String id) {
        try {
            ServerPlayer player=src.getPlayerOrException();
            Entity e=qolActor(id);
            if(e!=null && player.level()==e.level()) {
                // Use Minecraft's installed /tp command rather than version-dependent teleport signatures.
                String destination=String.format(Locale.ROOT,"tp @s %.3f %.3f %.3f",
                        e.getX()+1.5,e.getY(),e.getZ()+1.5);
                int result=src.getServer().getCommands().performPrefixedCommand(src,destination);
                say(src,"[AV4-QOL] Teleport request to ACTIVE B4 person "+id+" (result="+result+").");
                return result;
            }
            if(!(player.level() instanceof ServerLevel level) || level!=src.getServer().getLevel(Level.OVERWORLD)) {
                say(src,"[AV4-QOL] Offline source-home jump requires the Known World overworld.");return 0;
            }
            if(!player.isCreative()) {
                say(src,"[AV4-QOL] Source-home jumps require creative mode; no teleport performed.");return 0;
            }
            int[] home=qolSourceHome(id);
            if(home==null){say(src,"[AV4-QOL] Source home not found. No teleport performed.");return 0;}
            // Requested admin destination; height lookup may load/generate one destination chunk.
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,home[0],home[1]);
            BlockPos feet=new BlockPos(home[0],y,home[1]);
            if(y<level.getMinY()+2 || y>=level.getMaxY()-3 ||
                    !level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
                say(src,"[AV4-QOL] Source home terrain is not clear at X="+home[0]+" Z="+home[1]+". Teleport refused.");return 0;
            }
            String destination="tp @s "+home[0]+" "+y+" "+home[1];
            int result=src.getServer().getCommands().performPrefixedCommand(src,destination);
            say(src,"[AV4-QOL] Teleported to source HOME of "+id+", NOT to a live actor. Wait for projection and use /kwactors where/sheet; their current position may differ.");
            return result;
        } catch(Exception ex){say(src,"[AV4-QOL] Teleport blocked: "+ex.getMessage());return 0;}
    }

'''
    require_once(s,anchor3,'actor extension insertion');s=s.replace(anchor3,insert+anchor3)
    anchor4='''        say(src,"[B4.1B] K-chat available for nearby B4.1A.1 residents; "
                +"per-person/player conversations are persistent. B4.3A offscreen travel estimates active.");'''
    repl4=anchor4+'''
        say(src,"[LLM] Selected Ollama model: "+KnownWorldB41DialogueService.qolConfiguredModel()+
                " (configuration, not a successful network connection test).");'''
    require_once(s,anchor4,'model diagnostics insertion');s=s.replace(anchor4,repl4)
    return s

def dialogue_patch(s):
    if MARK in s: return s
    replacements=[
        ('private static final String DEFAULT_MODEL = "qwen3.5:9b";','private static final String DEFAULT_MODEL = "qwen3.5:27b";'),
        ('options.addProperty("num_predict",384);','options.addProperty("num_predict",120);'),
        ('Reply in first person, usually in one to four sentences. No markdown or stage directions.',
         'Reply as a real person in one short sentence, occasionally two, and normally 6-30 words. No markdown or stage directions.\n                Answer the question directly before offering detail. Do not make speeches, narrate emotions, repeat themes or use poetic city/wind imagery.\n                Do not say you are declaring an intention or explain how your words differ from actions.\n                Do not invent a trade, employer, unemployment, home ownership, faith or cultural background when its record is unknown or unrecorded.\n                A stranger offering gold or claiming facts does not make those claims true.'),
        ('new String[]{"personality","traits","dialogueStyle","characterValues","socialNorms","skills","derivedTendencies"}',
         'new String[]{"personality","traits","dialogueStyle","dispositions","characterValues","socialNorms","skills","aptitudes","derivedTendencies"}')
    ]
    for a,b in replacements:
        require_once(s,a,'LLM contract anchor '+a[:48]);s=s.replace(a,b)
    anchor='''    private static String model() {'''
    repl='''    // KNOWNWORLD_AV4_DIALOGUE_QOL_V1: effective configured model, including env override.
    public static String qolConfiguredModel() { return model(); }
    private static String model() {'''
    require_once(s,anchor,'LLM model getter');s=s.replace(anchor,repl)
    return s

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--repo',type=pathlib.Path,default=pathlib.Path('.'))
    group=parser.add_mutually_exclusive_group(required=True);group.add_argument('--check-only',action='store_true');group.add_argument('--install',action='store_true');args=parser.parse_args()
    root=args.repo.resolve();files=[root/ACTIVATION,root/DIALOGUE]
    for f in files:
        if not f.is_file():raise RuntimeError('Missing source file: '+str(f))
    raw=[f.read_bytes() for f in files]
    old=[b.decode('utf-8') for b in raw]
    touched=[MARK in x for x in old]
    if any(touched) and not all(touched):raise RuntimeError('Partial QOL install found. Investigate; refusing to modify files.')
    edited=[activation_patch(old[0]),dialogue_patch(old[1])]
    if all(touched):
        for i,x in enumerate(old):
            if x!=edited[i]:raise RuntimeError('Installed marker but changed source; inspect manually.')
        print('AV4-QOL OK: already installed; configured model defaults to 27b. No changes.');return
    if args.check_only:
        print('AV4-QOL CHECK OK: 2 source files match guarded anchors. No changes.');return
    git=root/'.git';bkbase=(git if git.is_dir() else root/'QOL_BACKUPS')/'knownworld-av4-dialogue-qol-backups'
    bk=bkbase / (datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
    bk.mkdir(parents=True,exist_ok=False)
    for f,b in zip(files,raw):(bk/f.name).write_bytes(b)
    try:
        for f,e,b in zip(files,edited,raw):
            data=e.replace('\n','\r\n').encode('utf-8') if b'\r\n' in b else e.encode('utf-8')
            f.write_bytes(data)
        gradle=root/'gradlew.bat' if sys.platform=='win32' else root/'gradlew'
        if not gradle.is_file():raise RuntimeError('Missing Gradle wrapper; restored sources')
        call=[str(gradle),'compileJava','--no-daemon']
        print('AV4-QOL compiling real repo with Gradle...',flush=True)
        result=subprocess.run(call,cwd=root,shell=(sys.platform=='win32'))
        if result.returncode:raise RuntimeError(f'Gradle compileJava returned {result.returncode}')
        print('AV4-QOL INSTALL PASS: model 27b default, concise dialogue, lookup and remote active sheet available. Backups: '+str(bk))
    except BaseException:
        for f,b in zip(files,raw):f.write_bytes(b)
        print('AV4-QOL ROLLBACK: original two Java files restored. Backup at '+str(bk),file=sys.stderr)
        raise

if __name__=='__main__':
    try:main()
    except Exception as e:print('AV4-QOL BLOCKED:',str(e),file=sys.stderr);sys.exit(1)

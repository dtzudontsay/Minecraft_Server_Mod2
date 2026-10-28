#!/usr/bin/env python3
"""B4.1B: idempotently patch ONLY packet routing and B4.1A.1 NPC projection bridge.

This installer never regenerates B4.0 identities, touches a Minecraft save, or
changes Winterfell's dedicated LLM service. It reads source files before any
write; a failed prerequisite/anchor check leaves every file untouched.
"""
from __future__ import annotations
import argparse
import datetime
import hashlib
import json
import shutil
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
PKT=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/WinterfellConversationPackets.java')
ACT=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java')
UI=Path('src/main/java/dev/dtzudontsay/knownworld/client/casestudy/WinterfellDialogueScreen.java')
SERVICE=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueService.java')
TRUTH=Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueTruth.java')
MARKER='// B4_1B_WORLDWIDE_DIALOGUE_ROUTING'


def swap(s, old, new, name):
    count=s.count(old)
    if count!=1: raise ValueError(f'{name}: expected exactly one anchor, found {count}: {old[:92]!r}. No files changed.')
    return s.replace(old,new,1)


def patch_packets(src):
    if MARKER in src:return src
    if 'import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB41DialogueService;' not in src:
        src=swap(src,'import dev.dtzudontsay.knownworld.simulation.NpcSimulation;',
                 'import dev.dtzudontsay.knownworld.simulation.NpcSimulation;\n'
                 'import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB41DialogueService;', 'packet import')
    src=swap(src,
        '    public static void registerCommon() {',
        '    public static void registerCommon() {\n'
        '        ' + MARKER + '\n'
        '        KnownWorldB41DialogueService.registerLifecycle();', 'packet lifecycle')
    src=swap(src,
        '''            WinterfellConversationService.open(player, request.entityId());
            lockActor(player, request.entityId());''',
        '''            KnownWorldB41DialogueService.invalidatePlayer(player);
            if (KnownWorldB41DialogueService.isWorldwideActor(player, request.entityId())) {
                // Worldwide B4.0 identities: separate authoritative conversation route.
                if (KnownWorldB41DialogueService.open(player, request.entityId())) {
                    lockActor(player, request.entityId());
                }
            } else {
                WinterfellConversationService.open(player, request.entityId());
                lockActor(player, request.entityId());
            }''', 'packet open')
    src=swap(src,
        '''        ServerPlayNetworking.registerGlobalReceiver(ChatRequest.TYPE, (request, context) ->
                WinterfellConversationService.send(context.player(),
                        request.session(), request.text()));''',
        '''        ServerPlayNetworking.registerGlobalReceiver(ChatRequest.TYPE, (request, context) -> {
            if (KnownWorldB41DialogueService.hasSession(context.player(), request.session())) {
                KnownWorldB41DialogueService.send(context.player(), request.session(), request.text());
            } else {
                WinterfellConversationService.send(context.player(), request.session(), request.text());
            }
        });''', 'packet chat')
    src=swap(src,
        '''            WinterfellConversationService.close(player, request.entityId());''',
        '''            KnownWorldB41DialogueService.close(player, request.entityId());
            WinterfellConversationService.close(player, request.entityId());''', 'packet close')
    src=swap(src,
        '''        if (!(actor instanceof Villager villager)
                || !actor.isAlive()
                || !actor.entityTags().contains(ACTOR_TAG)
                || player.distanceToSqr(actor) > MAX_RANGE_SQUARED) return;
        String authored = null;
        for (String tag : actor.entityTags()) {
            if (tag.startsWith(ID_PREFIX)) {
                authored = tag.substring(ID_PREFIX.length());
                break;
            }
        }
        if (authored == null) return;
        NpcSimulation sim = NpcSimulation.getNullable();
        NpcId npcId = sim == null ? null :
                sim.authoredIds().findNpc(authored).orElse(null);
        if (npcId == null || sim.registry().find(npcId)
                .filter(npc -> npc.isAlive()).isEmpty()) return;''',
        '''        if (!(actor instanceof Villager villager)
                || !actor.isAlive()
                || player.distanceToSqr(actor) > MAX_RANGE_SQUARED) return;
        if (actor.entityTags().contains("kwb41_actor")) {
            // Never freeze a forged or stale projection. Its identity must be live.
            if (!KnownWorldB41DialogueService.validActor(player, actor)) return;
        } else {
            if (!actor.entityTags().contains(ACTOR_TAG)) return;
            String authored = null;
            for (String tag : actor.entityTags()) {
                if (tag.startsWith(ID_PREFIX)) {
                    authored = tag.substring(ID_PREFIX.length());
                    break;
                }
            }
            if (authored == null) return;
            NpcSimulation sim = NpcSimulation.getNullable();
            NpcId npcId = sim == null ? null :
                    sim.authoredIds().findNpc(authored).orElse(null);
            if (npcId == null || sim.registry().find(npcId)
                    .filter(npc -> npc.isAlive()).isEmpty()) return;
        }''', 'actor lock')
    return src


def patch_activation(src):
    if 'public static DialogueIdentity dialogueIdentity(' in src:return src
    if 'B4.1A.1' not in src or 'kwb41_actor' not in src:
        raise ValueError('Installed projection runtime is not B4.1A.1/C4.15: do not install generic chat on older runtime.')
    src=swap(src,
        '''    private record Candidate(Person person, Site site, int homeX, int homeZ, double distance) { }''',
        '''    private record Candidate(Person person, Site site, int homeX, int homeZ, double distance) { }

    // B4.1B speech-only view of authoritative B4.0 data, NOT an NpcState import.
    public record DialogueIdentity(String id, String name, String occupation, String culture,
                                   String religion, String language, String siteName,
                                   String household, String home, String workplace,
                                   int startAge, JsonObject profile) { }
''', 'projection DTO')
    src=swap(src,
        '''    private static double cap(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}''',
        '''    /** Server-side only. Checks tag, entity UUID, distance and live projection ownership. */
    public static DialogueIdentity dialogueIdentity(ServerPlayer player,Entity actor) {
        if(actor==null || !actor.isAlive() || !actor.entityTags().contains(TAG)
                || !actor.level().equals(player.level()) || player.distanceToSqr(actor)>36) return null;
        Person p=forActor(actor);
        if(p==null || !actor.getUUID().equals(projected.get(p.id)))return null;
        Site site=visibleSites.get(p.id);
        if(site==null || !site.id.equals(p.site))return null;
        JsonObject profile=new JsonObject();
        enrich(profile,p,site); // Same deterministic personality as right-click inspection.
        return new DialogueIdentity(p.id,p.name,show(p.occupation),show(p.culture),
                show(p.religion),show(p.language),site.name,show(p.household),
                show(p.home),show(p.workplace),Math.max(0,298-p.birthYear),profile);
    }
    private static double cap(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}''', 'projection identity method')
    src=swap(src,
        '''        say(src,"[B4.1A.1] Nearby resources="+tiles.size()+" / "+TILE_CACHE_LIMIT+" cached tiles. "
                +"Winterfell V3 and 3.76m B4.0 identities untouched. K-chat/offscreen work not yet activated.");''',
        '''        say(src,"[B4.1B] K-chat available for nearby B4.1A.1 residents; "
                +"per-person/player conversations are persistent. Offscreen AI/work not implemented.");''',
        'projection status')
    return src


def patch_ui(src):
    if 'subtitle.startsWith("Known World | ")' in src:return src
    return swap(src,
        '"WINTERFELL  /  CONVERSATION",',
        'subtitle.startsWith("Known World | ") ? "KNOWN WORLD  /  CONVERSATION" : "WINTERFELL  /  CONVERSATION",',
        'shared dialogue screen title')

def apply(root:Path, check:bool):
    root=root.resolve()
    for rel in (PKT,ACT,UI,SERVICE,TRUTH):
        if not (root/rel).is_file():raise FileNotFoundError(f'Missing required file: {rel}')
    current={rel:(root/rel).read_text(encoding='utf-8') for rel in (PKT,ACT,UI)}
    new={PKT:patch_packets(current[PKT]),ACT:patch_activation(current[ACT]),UI:patch_ui(current[UI])}
    if MARKER not in new[PKT] or 'DialogueIdentity dialogueIdentity' not in new[ACT]:
        raise ValueError('Missing B4.1B install markers')
    changes=[rel for rel in (PKT,ACT,UI) if current[rel]!=new[rel]]
    print(json.dumps({'patchTargets':[str(x) for x in (PKT,ACT,UI)],
                      'changed':[str(x) for x in changes], 'checkOnly':check},indent=2))
    if check: return
    if not changes:
        print('B4.1B patch already installed; no rewrites needed.')
        return
    stamp=datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    backup=root/'tools/geodata/output/b4_1b_source_backups'/stamp
    for rel in changes:
        dest=backup/rel;dest.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(root/rel,dest)
    print('Source backups:',backup)
    # Every source file is read, validated and backed up BEFORE any replacement.
    for rel in changes:
        tmp=(root/rel).with_suffix('.java.b41b.tmp')
        try:
            tmp.write_text(new[rel],encoding='utf-8')
            tmp.replace(root/rel)
        finally:
            tmp.unlink(missing_ok=True)
    print('B4.1B routing installed. No population resources or world saves touched.')

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--root',type=Path,default=ROOT)
    parser.add_argument('--check',action='store_true')
    args=parser.parse_args()
    apply(args.root,args.check)

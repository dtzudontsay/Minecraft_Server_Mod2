#!/usr/bin/env python3
"""AV4-QOL v3 guarded local patch. Never changes GitHub or Minecraft saves.

Run from repository root with --check or --install. Source edits are restricted
by explicit anchors and separately backed up. No dynamic code from user text.
"""
import argparse
import datetime as dt
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
REL_POP = Path('src/main/java/dev/dtzudontsay/knownworld/simulation/population')
ACTIVATION = ROOT / REL_POP / 'KnownWorldB41Activation.java'
DIALOGUE = ROOT / REL_POP / 'KnownWorldB41DialogueService.java'
SNIPPET = Path(__file__).with_name('actor_methods.java.txt')
TAG = 'AV4_QOL_V3'

class Blocked(Exception):
    pass

def once(source, anchor, replacement, name):
    found = source.count(anchor)
    if found != 1:
        raise Blocked(f'{name}: expected one safe anchor, found {found}; NO FILES MODIFIED')
    return source.replace(anchor, replacement, 1)

def read(path):
    if not path.is_file():
        raise Blocked(f'Cannot find {path}; extract the package into the repository root.')
    return path.read_bytes().decode('utf-8')

def patched_activation(source):
    if 'AV4_QOL_V3_BEGIN' in source:
        return source
    if 'KnownWorldAv4Decisions.register();' not in source or 'KnownWorldAv3Formation.register();' not in source:
        raise Blocked('B4.1 activation lacks verified AV3/AV4 hooks; refuse a blind patch')
    for name in ['list', 'where', 'goto', 'sheet']:
        if f'Commands.literal("{name}")' in source:
            raise Blocked('Existing /kwactors lookup commands detected; cannot overwrite another implementation')
    if 'import com.mojang.brigadier.arguments.StringArgumentType;' not in source:
        source = once(source, 'import com.google.gson.JsonParser;',
                      'import com.google.gson.JsonParser;\nimport com.mojang.brigadier.arguments.StringArgumentType;',
                      'B41 StringArgumentType import')
    nearby = '.then(Commands.literal("nearby").executes(ctx->nearby(ctx.getSource())))));'
    extras = ''' .then(Commands.literal("list")
                            .requires(src -> Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                            .executes(ctx -> qolList(ctx.getSource())))
                        .then(Commands.literal("where")
                            .requires(src -> Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                            .then(Commands.argument("personId", StringArgumentType.string())
                                .executes(ctx -> qolWhere(ctx.getSource(), StringArgumentType.getString(ctx, "personId")))))
                        .then(Commands.literal("goto")
                            .requires(src -> Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                            .then(Commands.argument("personId", StringArgumentType.string())
                                .executes(ctx -> qolGoto(ctx.getSource(), StringArgumentType.getString(ctx, "personId")))))
                        .then(Commands.literal("sheet")
                            .requires(src -> Commands.LEVEL_GAMEMASTERS.check(src.permissions()))
                            .then(Commands.argument("personId", StringArgumentType.string())
                                .executes(ctx -> qolSheet(ctx.getSource(), StringArgumentType.getString(ctx, "personId")))))));'''
    source = once(source, nearby,
                  '.then(Commands.literal("nearby").executes(ctx->nearby(ctx.getSource())))\n                       '+extras,
                  'B41 existing /kwactors nearby chain')
    anchor = '    private static void say(CommandSourceStack src,String s){'
    snippet = read(SNIPPET).rstrip() + '\n\n'
    source = once(source, anchor, snippet + anchor, 'B41 QOL helper insertion')
    inspector = '    private static void inspect(ServerPlayer player,Entity actor) {\n        if(player.distanceToSqr(actor)>64)return;'
    source = once(source, inspector,
                  '    private static void inspect(ServerPlayer player,Entity actor) { inspect(player, actor, false); }\n'
                  '    private static void inspect(ServerPlayer player,Entity actor,boolean remoteAuthorized) {\n'
                  '        if(!remoteAuthorized && player.distanceToSqr(actor)>64)return;',
                  'B41 existing right-click inspector')
    return source

def patched_dialogue(source):
    if '// AV4_QOL_V3_DIALOGUE' in source:
        return source
    if 'av4FreeTimeDecision' not in source:
        raise Blocked('AV4 decision context missing from dialogue: cannot verify preservation')
    source = once(source, 'private static final String DEFAULT_MODEL = "qwen3.5:9b";',
                  'private static final String DEFAULT_MODEL = "qwen3.5:27b"; // AV4_QOL_V3_DIALOGUE',
                  'Ollama default (expected 9b)')
    source = once(source, 'options.addProperty("num_predict",384)',
                  'options.addProperty("num_predict",192)', 'generation token cap')
    old = 'Reply in first person, usually in one to four sentences. No markdown or stage directions.'
    new = ('Reply naturally in first person, generally one sentence, sometimes two. '
           'Routine answers should usually be about 6-30 words; go longer only when asked for detail.\n'
           '                Adapt vocabulary and directness to personal style, situation, education, and familiarity.\n'
           '                Avoid theatrical speeches, repeated metaphors, and listing your personality scores.\n'
           '                No markdown or stage directions.')
    source = once(source, old, new, 'existing prompt style instruction')
    guard = 'If asked about something unknown, admit uncertainty naturally.'
    source = once(source, guard, guard+'\n'
                  '                An unrecorded occupation does not prove unemployment or job-seeking; '
                  'you may express interest in paid work, but never invent an existing job.',
                  'dialogue grounding guard')
    pattern = r'for\s*\(String\s+key\s*:\s*new\s+String\[\]\s*\{(?P<values>[^{}]*)\}\)'
    matches = list(re.finditer(pattern, source))
    matching = []
    for m in matches:
        parts = re.findall(r'"([A-Za-z_0-9]+)"', m.group('values'))
        if {'personality','traits','dialogueStyle','av4FreeTimeDecision'}.issubset(parts):
            matching.append((m,parts))
    if len(matching)!=1:
        raise Blocked('LLM profile context: cannot uniquely identify current AV4-aware loop; do not drop attributes')
    m,parts = matching[0]
    for n in ('dispositions','aptitudes'):
        if n not in parts:
            parts.append(n)
    new_expression = 'for(String key:new String[]{'+','.join('"'+p+'"' for p in parts)+'})'
    source = source[:m.start()] + new_expression + source[m.end():]
    return source

def prepare():
    old_a, old_d = read(ACTIVATION), read(DIALOGUE)
    def patch_preserving_newlines(source, fn):
        if '\r' in source.replace('\r\n', ''):
            raise Blocked('Unsupported mixed line endings; do not rewrite local Java source')
        use_crlf = '\r\n' in source
        normalized = source.replace('\r\n', '\n')
        patched = fn(normalized)
        return patched.replace('\n', '\r\n') if use_crlf else patched
    if ('AV4_QOL_V3_BEGIN' in old_a) != ('// AV4_QOL_V3_DIALOGUE' in old_d):
        raise Blocked('Partial QOL v2 installation detected; restore prior backups before proceeding')
    new_a = patch_preserving_newlines(old_a, patched_activation)
    new_d = patch_preserving_newlines(old_d, patched_dialogue)
    return [(ACTIVATION, old_a, new_a), (DIALOGUE, old_d, new_d)]

def sha(s): return hashlib.sha256(s.encode('utf-8')).hexdigest()

def atomic_write(path, source):
    fd, temp = tempfile.mkstemp(prefix=path.name+'.qol2_', dir=path.parent)
    try:
        with os.fdopen(fd,'wb') as f:
            f.write(source.encode('utf-8'))
            f.flush(); os.fsync(f.fileno())
        os.replace(temp,path)
    finally:
        if os.path.exists(temp): os.unlink(temp)

def main():
    arg=argparse.ArgumentParser()
    group=arg.add_mutually_exclusive_group(required=True)
    group.add_argument('--check',action='store_true')
    group.add_argument('--install',action='store_true')
    args=arg.parse_args()
    if not (ROOT/'gradlew.bat').is_file():
        raise Blocked('Run with the package extracted beside gradlew.bat in repository root')
    edits=prepare()
    pending=[(p,old,new) for p,old,new in edits if new!=old]
    if not pending:
        print('AV4-QOL v3 ALREADY INSTALLED: both patches recognized; no files modified')
        return
    if args.check:
        for p,old,new in pending:
            print(f'READY {p.relative_to(ROOT)} | oldSHA256={sha(old)[:16]} | newSHA256={sha(new)[:16]}')
        print('AV4-QOL v3 CHECK PASSED: current AV4 context preserved; no files modified')
        return
    git=ROOT/'.git'
    if not git.is_dir():
        raise Blocked('Cannot locate .git for safe backups; will not install')
    stamp=dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    target=git/'av4_qol3_backups'/stamp
    if target.exists(): raise Blocked('Backup timestamp collision; retry later')
    target.mkdir(parents=True)
    for p,old,_ in pending:
        backup=target/p.relative_to(ROOT)
        backup.parent.mkdir(parents=True,exist_ok=True)
        backup.write_bytes(old.encode('utf-8'))
    print('BACKUP:',target)
    written=[]
    try:
        for p,old,new in pending:
            if read(p)!=old: raise Blocked(f'Source changed during installation: {p.name}')
            atomic_write(p,new)
            written.append((p,old,new))
        gradle=ROOT/'gradlew.bat'
        print('Running Windows Fabric Gradle compileJava --no-daemon ...',flush=True)
        result=subprocess.run(['cmd.exe','/c',str(gradle),'compileJava','--no-daemon'],cwd=ROOT,check=False)
        if result.returncode: raise Blocked(f'Gradle compileJava returned {result.returncode}')
        print('AV4-QOL v3 INSTALLED AND COMPILED. Backups:',target)
    except BaseException:
        for p,old,new in reversed(written):
            if read(p)==new:
                atomic_write(p,old)
                print('RESTORED:',p.relative_to(ROOT))
            else:
                print('WARNING: Concurrent modification: left intact',p)
        raise

if __name__=='__main__':
    try:
        main()
    except (Blocked,OSError,UnicodeError) as e:
        print('AV4-QOL v3 BLOCKED:',e, file=sys.stderr)
        sys.exit(1)

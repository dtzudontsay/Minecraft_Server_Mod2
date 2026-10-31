# AV4-QOL v3 — Minecraft 26.3 API-compatible Qwen 27B and NPC lookup patch

**Scope:** A local, guarded replacement for the previous AV4-QOL installer that blocked at the LLM attribute-array anchor. The original source anchors were validated against GitHub `master` at `3c54e81895d86c04783704b197f53b2e2fd001f6` (2026-10-30). GitHub and saved worlds are never written by this package. It changes only two existing Java files, provided the current local source matches the verified structural anchors. If your local files have other changes, a `BLOCKED` result is intentional: share the first error rather than forcing anything.

## 1. Install

Extract this ZIP's contents **into** `D:\GameOfThronesMod2` (the folder containing `gradlew.bat`), preserving the nested `tools\av4_qol3` directory. This **replaces the two older `RUN_AV4_QOL_*.bat` launchers**; the original staged installer can remain in place but is not called.

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV4_QOL_CHECK_ONLY.bat
.\RUN_AV4_QOL_INSTALL_AND_TEST.bat
```

`CHECK_ONLY` prints expected before/after SHA-256 hashes and does not edit any files or worlds. `INSTALL_AND_TEST` makes exact-byte backups of the two Java sources under `.git\av4_qol3_backups\<UTC timestamp>\`, patches them, and runs the actual `gradlew.bat compileJava --no-daemon` in your local Fabric checkout. If Java compilation fails, it restores the original Java source (unless another process changed it meanwhile). **A compile pass does not prove in-game behavior.**

The patch changes the existing default Ollama model from `qwen3.5:9b` to `qwen3.5:27b`. A `KNOWNWORLD_OLLAMA_MODEL` environment variable still takes precedence. Ollama must actually have the 27B model installed; the patch does not download or start Ollama. It changes the main speaking-style instruction to favor concise, natural answers, reduces `num_predict` to 192, and augments the AV4-era dialogue attribute list with dispositions and aptitudes **without losing** the existing `av4FreeTimeDecision` or any other listed fields.

## 2. Administrator commands

Only operators (the existing `LEVEL_GAMEMASTERS` permission level) can use the new commands:

```text
/kwactors list
/kwactors where "b4:site_ne_007:00000296"
/kwactors goto "b4:site_ne_007:00000296"
/kwactors sheet "b4:site_ne_007:00000296"
```

- `list` shows up to 40 **currently projected** worldwide B4 NPC names **and exact person IDs**.
- `where` shows an authoritative live position when the NPC is projected; otherwise, it scans *only the corresponding approved source site* and reports an **approximate source-home X/Z**, explicitly not the person's live position.
- `goto` teleports an in-game operator in the Overworld to a validated dry, collision-safe location **near a live projected NPC** or **near the recorded source home**. For the latter, this is **not proof that the person is physically there**. This explicit command can load/generate one destination chunk; if a safe location is not found, the command refuses the teleport.
- `sheet` opens the existing right-click inspection UI for an **actually projected** B4 NPC, without creating a fictional entity or fabricated live profile. It refuses if that NPC isn't currently projected.

The teleport now executes the vanilla command using its `void` API and checks the actual player position before reporting success. The previous `int result = performPrefixedCommand(...)` compile error is fixed.

Unloaded lookup is intentionally bounded at 80,000 people per site on the main server thread. Very large cities may return `BOUNDED_SCAN_LIMIT_80000` if the requested ID is beyond the safe scan, rather than freezing the server to search millions of records. No map index, world save, economy, household, relationship or person ID is changed. Winterfell V3 NPCs remain a separate subsystem.

## 3. In-game verification

After installation and a successful build, start your existing test world:

1. Run `/kwactors status` and `/kwactors list`.
2. Use `/kwactors where "b4:site_ne_007:00000296"`. The result should identify Aren's live position or clearly say `SOURCE HOME ONLY`.
3. Test `/kwactors goto "b4:site_ne_007:00000296"`. A safe destination is required; on arrival, permit the nearby actor projector to run before testing `sheet`.
4. Use `/kwactors sheet "b4:site_ne_007:00000296"` once the actual actor is projected. Right-click should still open the identical character inspector.
5. On the server machine run `ollama list` and `ollama ps` to see available and loaded models. With a **new** Ollama chat, check the log for errors and verify the responses are more natural; prompt effects are tendencies, not guaranteed constraints.
6. Then run the AV4A profile persistence and free-time decision/movement tests before AV4B.

If the check/install blocks, send the first `AV4-QOL v3 BLOCKED` line and your `git status -sb`. Do not try to repair the failed source check by blindly replacing the entire Java files. Source patch safety tests can be run manually with `py -3 tools\av4_qol3\test_patch.py`.

## 4. Tested here versus your machine

The package's synthetic source-contract unit tests pass, including the exact regression class (AV4's additional dialogue field), idempotence, unknown-source refusal and preservation of the normal right-click path. This environment cannot compile the full Minecraft 26.3 Fabric project or execute it in-game; that is why the Windows installer runs real `compileJava` and safely rolls back on failure.

## 5. Specific compatibility correction

The Minecraft 26.3/Fabric command API exposes `performPrefixedCommand(CommandSourceStack, String)` as `void` in your local compile. QOL v3 invokes it as a statement, then compares the server-side player position to the collision-safe landing point. A failure or blocked teleport does not print a false success. The complete QOL features and the existing AV3/AV4 source guards remain intact. The failed v2 installer already reported that it restored the original two Java files; do not manually restore them again.

# AV1 — Ready-to-install source + canonical vocabulary

**Target:** `D:\GameOfThronesMod2`, current GitHub master `28a2e02d` (including B4.2B.5).

AV1 is a READ-ONLY canonical vocabulary, NOT a worldwide NPC migration or new stat engine. It preserves every one of the original Excel rows, validates 706 value/field definitions against current Java enums and reference catalogs, and installs a Minecraft diagnostic command. No saves, NPC profiles, coins, grain, worlds or ownership are mutated.

## Exact Windows commands

1. Extract **all** ZIP contents directly to `D:\GameOfThronesMod2` (same folder as `gradlew.bat`). Do not copy the staged Java file manually.
2. In PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV1_CHECK_ONLY.bat
.\RUN_AV1_INSTALL_AND_TEST.bat
```

3. After a successful Gradle compile, launch a *test world* and run:

```
/kwattributes status
/kwattributes domain CHARACTER_VALUE
/kwattributes row 93
/kwattributes resolve CHARACTER_VALUE EGALITARISM
/kwattributes resolve HEALTH_STATE MINORLY_ILL
```

The input alias EGALITARISM is accepted for read-time compatibility, but the resulting canonical runtime ID is EGALITARIANISM.

4. Inspect `git --no-pager diff --stat` and `git status -sb`. If testing passes, you can `git add` only the AV1 staged installer assets, docs, reference resource, Java class and the entrypoint change, then commit/push. Avoid `git add -A` if your repository still has temporary/generated files.

## Files

- `AV1_STAGE/src/main/java/.../KnownWorldAv1Vocabulary.java` (will be installed under active `src/main/java/...`)
- `AV1_STAGE/src/main/resources/data/knownworld/reference/av1_attribute_vocabulary_v1.json`
- `AV1_STAGE/docs/architecture/24_31_AV1_UNIFIED_ATTRIBUTE_VOCABULARY.md`
- `AV1_STAGE/docs/architecture/AV1_VOCABULARY_CROSSWALK.csv`
- `AV1_STAGE/docs/architecture/AV1_Attribute_Coverage.xlsx`
- `AV1_STAGE/tools/av1/test_av1_vocabulary.py`
- `tools/av1/av1_guarded_install.py` (extracts beside stage; actual installer)
- `RUN_AV1_CHECK_ONLY.bat`, `RUN_AV1_INSTALL_AND_TEST.bat`

## Safety

Check-only makes no modifications. Installer requires `KnownWorld.java` to be clean and an exact hook match. It refuses to overwrite divergent existing target files. It stores the old entrypoint under the Git directory's `av1_backups/<UTC timestamp>` and automatically restores affected source if the local Gradle build fails. It doesn't modify Minecraft saves. GitHub is read-only to the assistant.

Please share the first failure log, if any, or the 3 AV1 debug command outputs after compiling. AV2 will then connect this vocabulary to persistent individual character state across the 3.76-million-person B4 baseline.

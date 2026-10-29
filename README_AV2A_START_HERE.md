# Known World AV2A — install and test

**Expected repository:** `D:\GameOfThronesMod2`, GitHub `master` at `28a2e02d` or later, **AV1 v1.1 successfully installed**. This is the first part of AV2 (sparse, persistent B4 numeric profile state), not the full legacy/B4 psychology reconciliation.

Extract the ZIP into the repo root, beside `gradlew.bat`, **preserving directories**. The ZIP contains `AV2_STAGE`, `tools/av2`, and two `RUN_...bat` launchers. Do NOT manually copy the stage Java source into your active code.

## Windows commands

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV2A_CHECK_ONLY.bat
.\RUN_AV2A_INSTALL_AND_TEST.bat
```

The installer verifies AV1, all 761 source lineage rows, no new identities, the B4 source hook, and then runs Gradle `compileJava`. The only existing source file patched is `KnownWorldB41Activation.java`; it snapshots that file under `.git/av2a_backups/<timestamp>`, rejects uncommitted changes to it, and restores its original bytes plus newly installed files on a failed build.

It never writes a Minecraft save during installation and never pushes to GitHub. If either command fails, **send the output and do not repeat installer steps blindly**.

## Quick commands after a successful build (on a disposable save)

```text
/kwprofile status
/kwactors nearby
```

Right-click a nearby B4 actor to find its stable `b4:...` person ID. When ready to test persistence, substitute that exact ID (including double quotes):

```text
/kwprofile inspect "b4:site_...:..."
/kwprofile set "b4:site_...:..." CHARACTER_VALUE DUTY 0.55
/kwprofile inspect "b4:site_...:..."
/kwprofile clear "b4:site_...:..." CHARACTER_VALUE DUTY
```

Use OP privileges for `set`/`clear`. Changes are explicit, checksum-validated, per-save, and visible in both inspection and K dialogue. No autonomous behavior, job productivity, child development or skill growth is activated yet. No Minecraft test is *required before beginning AV2B*, but the planned grouped in-game test should happen after AV3/AV4 and before enabling autonomous mutations.

## Files

- `AV2_STAGE/src/main/java/.../KnownWorldAv2CharacterState.java` — new implementation
- `AV2_STAGE/tools/av2/test_av2_source_contract.py` — source/coverage contract tests
- `AV2_STAGE/docs/architecture/24_32_AV2A_WORLDWIDE_CHARACTER_STATE.md` — rationale and acceptance
- `AV2_STAGE/docs/architecture/AV2A_ATTRIBUTE_COVERAGE.csv` — all **761** original source rows, their canonical AV1 IDs and AV2 readiness
- `tools/av2/av2_guarded_install.py` — guarded installation and rollback

## Scope gate

117 numeric dimensions can have persisted per-person overrides in AV2A. The other **589** definition records remain traced, not discarded. This changes **zero** B4.0 identity records, 799,343 household IDs, or 248 settlement planning authorities. No old `NpcRegistry`/`CharacterProfile` object is fabricated.

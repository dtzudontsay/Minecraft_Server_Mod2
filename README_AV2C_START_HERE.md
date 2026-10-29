# AV2C — Verified Work-Presence Experience & Provenance

This release continues the AV attribute program after AV1, AV2A and AV2B.

**Verified base:** GitHub master `1cdd11d964b54127eb7a62c2ed03a029e49ecfd6` + local **installed AV2B**. AV2B is a strict prerequisite even if not yet pushed to GitHub.

## Install

1. Extract this ZIP **into the repo root** (beside `gradlew.bat`) preserving the folder paths. Existing AV2A/AV2B code must already be installed.
2. From PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV2C_CHECK_ONLY.bat
.\RUN_AV2C_INSTALL_AND_TEST.bat
```

The installer refuses unknown source changes, backs up original source files under the actual `.git/av2c_backups/` directory, runs independent source+Java tests and `gradlew compileJava --no-daemon`, and restores managed source files on failure. It will not touch Minecraft saves or push GitHub. `check-only` does not edit repo files. Successful install may leave intended new/modified code in the working tree: review `git status` before committing.

## In-game (deferred grouped test)

- `/kwexperience status`
- `/kwexperience person "b4:site_ne_001:..."` (substitute an actual B4 person ID; use the exact quoted string)
- Right-click a B4 projected NPC after a physically observed workplace arrival: look for `av2cEvidenceStatus`, `av2cWorkPresenceDays`, `av2cLastVerifiedWorkDay`.
- K-chat may mention physically observed presence; it cannot claim completed work/outputs/wages/skill increases.

## Important distinctions

AV2C is evidence-only: it mirrors a narrow verified upstream `B4.2B` physical work-arrival event and does NOT auto-increase attributes or create new events from modeled offscreen schedules or conversation. It is **not a transactionally unified journal** with the existing B4 attendance journal; a crash can leave the two out of sync. Read `docs/architecture/24_34_AV2C_VERIFIED_EXPERIENCE_PROVENANCE.md` for safe recovery boundaries and test requirements.

All 706 canonical definitions and 761 original Excel source rows remain in `docs/architecture/AV2C_ATTRIBUTE_COVERAGE.csv`.

# Known World AV3 — Complete source-only install

Install after **AV1 v1.1 + AV2A + AV2B v1.1 + AV2C + AV2D** compile successfully. Expected upstream audit anchor: `1cdd11d964b54127eb7a62c2ed03a029e49ecfd6` plus local AV2B/C/D changes; do not require master to be exactly that commit.

Unzip into `D:\GameOfThronesMod2` (the same folder containing `gradlew.bat`). If any installer says BLOCKED, do **not** run the install; provide its exact console output. In PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_AV3_CHECK_ONLY.bat
.\RUN_AV3_INSTALL_AND_TEST.bat
```

The installer adds 2 Java classes and tests, appends one import, one registration, and one profile generation call in `KnownWorldB41Activation.java`, then runs your full Fabric `compileJava`. It backs up and rolls back source if compilation fails. No actual world save is loaded or migrated by the installer.

**In game, later**: `/kwformation status` and right-click any B4 projected NPC. Look for `av3State` and `av3Formation`. The same profile is used for K-key chat and feeds AV2B. All manual AV2A changes run AFTER AV3; no AV3 behavior can produce AV2D earned XP.

**Important limitations:** AV3 changes starting numeric estimates, not autonomous actions, completed jobs, hereditary trait proof, true parent-profile inheritance, conversion, laws or world events. Family-linked seed is a shared initial *prior*, not measured parental personality; cultural/faith biases are mod estimates inferred from existing regional profiles. The 20 skills are unchanged. The full 761-row spreadsheet audit and all 124 Java numeric axes are supplied in separate CSVs. The present major in-game checkpoint remains **after AV4**, contingent on successful per-batch `compileJava`.

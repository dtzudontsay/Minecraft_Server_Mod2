# B4.2B.5: Canonical Household-Economic Authority Preflight

Unzip **the contents** of this ZIP into `D:\GameOfThronesMod2` so that `RUN_B4_2B5_CHECK_ONLY.bat` is at the project root, alongside `gradlew.bat`.

**Run:**

```powershell
cd D:\GameOfThronesMod2
.\RUN_B4_2B5_CHECK_ONLY.bat
.\RUN_B4_2B5_INSTALL_AND_TEST.bat
```

The check-only script does not change project files. The installer adds one Java source, exposes existing canonical mapping as package-visible (no behavior change), registers one command from the existing B4.2B.2 bootstrap, and installs two architecture docs plus a 761-row source-value coverage CSV. It runs an offline manifest test and `gradlew.bat compileJava` with automatic source restoration if any part fails. Backups live in `.git/knownworld-b4-2b5-backups/<timestamp>/` (untracked by Git). No save/gameplay data or generated world is touched. The `B4_2B5_STAGE` folder can remain untracked for now; do not `git add .` indiscriminately.

**Test in a disposable Minecraft world:**

```text
/kwbridge status
/kwbridge site site_ne_001
/kwfinance site site_ne_001
/kwstocks audit site_ne_001
```

For a known B4 household ID, type `/kwbridge household site_ne_001 "<actual-household-id>"`. The household key must be a real key in B4.2B.4 source. Commands are **read-only**; they cannot activate production, wages, food consumption or grant accounts. A missing native settlement is an expected explicit blocker, not a reason to invent a new settlement.

**Expected test results:** The status reports the worldwide population baseline and current native settlement count. Site audit either passes source reconciliation then identifies the exact native economy blocker, or returns a diagnostic error. In particular, it does NOT claim that proposed grain/coins are actual balances.

**If a build fails:** the installer prints the error and restores the managed files. Save the terminal output for review. Do not run rollback scripts from old C4 packages.

**Next after this batch:** B4.2B.6 canonical settlement and household account bridging with persisted surrogate owner mapping, native economy validator support, idempotent initialization and single-settlement dry-run, before enabling a daily food/wage loop.

Also read `docs/architecture/KNOWN_WORLD_VALUE_INTEGRATION_CONTRACT.md`: it assigns every authoring value type to the existing roadmap rather than leaving them as LLM-only strings. `VALUE_COVERAGE_ALL_SOURCE_ROWS.csv` contains all 706 value/field rows and 55 section/subsection rows from the current `settlements.xlsx`, each with a roadmap gate and explicit unverified status (it is not a code-completeness claim).

# Known World B4.2B.1 — Position-Based NPC Discovery & Economic Readiness

**Applies to an installed and successfully built B4.3A, itself installed on top of C4.17.**

This is a *reversible first integration slice* of the larger B4.2B project, **not a claim that all economic systems are complete**. Existing economy saves, population shards, Winterfell and player conversation histories are outside this installer.

## 1. Install

Extract the entire archive into `D:\GameOfThronesMod2`, preserving `B4_2B_STAGE`.

From PowerShell in the repo root:

```powershell
.\RUN_B4_2B_CHECK_ONLY.bat
```

Expected: three existing Java files `READY`, three new Java classes `NEW`, two generated resource directories `NEW`. The check also requires the existing projection manifest's 256-person B4.3A cap. **STOP if any `UNKNOWN_LOCAL_EDITS`, or if the census/manifest precheck fails.** Do not manually replace individual files.

Then:

```powershell
.\RUN_B4_2B_POSITION_DISCOVERY_ECONOMY_FOUNDATION.bat
```

The installer executes the 10 dedicated regression tests, snapshots all managed source and resource targets, installs the code, builds the mobility index from the existing B4.1A.1/B4.2 resources, generates the read-only economic reference, validates the expected states, and runs `gradlew.bat clean build`. A failure triggers restoration of the managed snapshot. Resource index generation will process the existing projected census and can use considerable disk and memory for big cities. A failed index generator never authorizes starting the game with half-patched code.

The installer does **not** run B4.0/B4.1/B4.2/C4.15/C4.17 installation scripts, regenerate population shards, or modify Minecraft saves.

## 2. New commands

- `/kwactors status` reports the number of position-based mobility lookups and references examined, along with the 256 actor budget.
- `/kwprogress status` reports actual **observed** workplace-presence events. A work schedule by itself does not create this event.
- `/kweconomyplan status` reports the controlled read-only 298 AC starting-reserve *estimate*. It expressly credits **no** grain and executes **no** autonomous production/consumption.
- `/kwdaily status`, `/kwdaily attendance` still work as before.

When a worker actually reaches its work destination during its modeled working phase, the personal event ledger records no more than one verified attendance event per campaign day, deduped by B4.0 person ID. The inspector can receive `verifiedWorkplaceVisits`, `verifiedPracticeSkill` and `recordedPracticeOpportunities`; the client may need follow-up UI work to render these fields. K-key dialogue receives truthful verified-attendance context where available, and cannot safely claim a specific product or wage from that alone.

## 3. Geographic discovery

The existing B4.1A.1 source data groups people by their *homes*. That still provides identity lookup, but it is no longer the only discovery path. The B4.2B mobility generator produces **geographic references** for home, workplace, commute/gate waypoints and deterministic leisure destinations. When a player enters a new location, a 3x3-cell spatial query locates potential participants (even if their residence is far away); the authoritative-person record and campaign-time `virtualArrival` decide if they are actually within 180 blocks. Unloaded chunks are not forced into memory. The physical projection cap stays at **256**.

Recent-observation data remains saved for future exceptional-trip reconciliation, but last-seen position no longer overrides an NPC's modeled reappearance coordinate. A commuter you last observed 400 blocks away can reappear near the *current modeled position*, not where the player last saw them.

**Limit:** indexed scheduled commuting + known local leisure only. Cross-country travelers, migration and free-form journeys are not yet in the index; B4.3B will add durable route events, real road-network indexing and interception for those travelers. Because B4.2B currently uses known model routes, route estimates can diverge from blocked vanilla pathfinding or actual game geometry. It does not solve every castle-wall pathfinding issue.

## 4. Economy safety gates

The existing repository's `EconomyRuntime`, `InventoryManager`, `AgriculturalActivationReadiness` and durable journal remain authoritative over actual stock and money. The generated readiness manifest uses a provisional **90 day grain-equivalent buffer at 1kg/person/day** to make a baseline adequacy check possible. That is a theoretical figure **not** actually deposited anywhere; the world does not suddenly have 90 days of real grain. No transactions, hunger, payments, taxes, employer/owner promotions or wealth distribution are silently created.

**Still required for B4.2B's main activation:** calibrated initial real inventory provisioning, holding/settlement ownership mapping, wages and staffing ledger, verified worker-to-output rules for jobs, unified idempotent daily production/consumption, cross-domain atomic checkpointing or safe reconciliation, persistent full-skill effects, and economically valid NPC experiences. Do not enable autonomous farming in old code by changing a boolean.

**A title/holding/dynasty owner must be distinguished from an individual workplace owner or employer.** Do not fabricate lordly holders for the 248 sites with no individual holder mapping.

## 5. Rollback

Rollback snapshots are in `tools\geodata\output\b4_2b_safe_snapshots\<TIMESTAMP>`. If a generator, verification or Gradle build fails, the installer automatically restores its managed Java/source and generated resources. For voluntary rollback after a successful install:

```powershell
.\RUN_B4_2B_ROLLBACK_LATEST.bat
.\gradlew.bat clean build
```

Or use `py -3 tools\geodata\b4_2b_guarded_install.py --rollback <TIMESTAMP>` for an exact snapshot. Do not blindly use `latest` once newer batches have been installed. **The installer intentionally does not delete personal-event saves after game play**, so make a separate Minecraft save backup before testing a new world. Rollback restores managed code/resources, not world saves.

## 6. Tests and limitations

The ten Python regression tests pass in a small synthetic source-data fixture, including geographically distant worksite discovery and source/resource restore. The three new Java classes pass Java 21 **interface-stub compilation**. **The real Fabric build and the full 3.76-million-person index build have not run here** and must be verified by your installer in your checkout. Physical travel outcomes depend on actual loaded game terrain. The recorded personal-event journal is an explicitly bounded first implementation (20,000 people with verified encounters); a per-site sharded journal is required for complete worldwide lifelong personal events before expanding this globally.

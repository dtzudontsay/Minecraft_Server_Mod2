# B4.2B.2 — Worldwide household economic standing & workplace staffing

**For the current B4.2B.1 installation only.** Do not run previous BAT installers again.

## In-game results confirmed from your screenshots

- 256 / 256 physical worldwide NPCs, 0 failed spawns
- World-position candidate discovery / healthy offscreen store
- 20 verified workplace-presence days for 20 unique residents at time of screenshot
- 248 site readiness census: 3,760,684 people / 1,880,341 modeled workers
- Auto production and consumption disabled, and zero grain actually credited, **by design**.

## What this upgrade ACTUALLY does

1. Iterate ALL 248 B4.0 site records and conserve 3,760,684 starting persons, 799,343 households and 1,880,341 modeled workers. Prefer the later B4.1A.1/C4.15 workplace overlay when a place is physically projected. Preserve exceptional/unprojected site IDs from the B4.0 source. **Winterfell V3 stays authoritative and is left out of provisional family/owner assignments**; its existing B4.0 census counts are preserved.
2. Build a *derived, immutable 298 AC household-standing reference* with age-independent economic strata (PRECARIOUS/MODEST/COMFORTABLE/PROSPEROUS), a relative prosperity index, household head reference, dependents, assigned worker count, and residence ID.
3. Build workplace staffing groups for all currently assigned workers. Each group records staffing count and a stable **PROVISIONAL shift coordinator candidate**. This individual is **NOT declared an owner, employer, castellan, lord or legal holder**. Actual workplace and building access remain unverified.
4. Index by site and 16 hash partitions: inspect or LLM conversation loads only small chunks relevant to the NPC currently interacted with. No scanning of 3.76m people each tick.
5. Extend B4.1 right-click character data with `economicStanding`, `householdProsperityIndex`, `employmentRole`, `employerStatus` and `workplaceLegalOwnerStatus`. Also sends a visible summary chat line on right-click, because the existing character GUI may not render the new fields yet. Supply provisional values to the worldwide LLM profile **as estimates**, not as known cash reserves.
6. Provide `/kwstanding status` with a manifest integrity check and initial economic authority status.

## Important limitations — no false claims

- This **does not** credit grain, coin, wages or household inventory balances to the existing `EconomyRuntime`. No new wallet, currency ledger or competing goods registry is introduced. The person economic stratum is a starting *proposal*, not actual wealth/property.
- The 20 learned skills, verified events, offscreen interpersonal social decisions, autonomous production, household payments, validated initial inventory stock, and authenticated legal employer/owner linking remain subsequent B4.2B stages. We will not write a fake harvest or bankrupt villages by enabling food consumption before inventory provision is safe.
- The authoritative winterfell 5,000 do not receive duplicate provisional economic identities. They remain in the *global totals* but their specific B4.2B.2 reference shards are intentionally absent.
- A household's economic standing is inferred from occupation, work and dependents; it must not be treated as factual inherited noble status. All worldwide business legal owners remain `UNRESOLVED` until evidence is available.
- This is a **worldwide baseline/index**, not a fully activated economy. Future B4.2B.3 must bridge B4.0 string IDs to canonical NPC/household economic ownership, verify holding links and perform transactional startup grants exactly once.

## Installation (Windows PowerShell)

1. Extract ZIP contents DIRECTLY into `D:\GameOfThronesMod2`, preserving `B4_2B2_STAGE`.
2. Check first:

    `.\RUN_B4_2B2_CHECK_ONLY.bat`

   Expected: existing B4.1 Activation `READY`; new B4.2B.2 standing class `NEW`; new resource directory `NEW`. If `UNKNOWN_LOCAL_EDITS`, STOP and send output. No overwrites allowed.
3. Install:

    `.\RUN_B4_2B2_WORLDWIDE_ECONOMIC_STANDING.bat`

   The BAT runs tests, snapshots managed source/resource paths, installs small Java changes, generates all site shards and performs `gradlew.bat clean build`. **If anything fails it restores managed source/resources automatically**. Does not change Minecraft saves.
4. Start:

    `.\gradlew.bat runClient`

   Commands: `/kwstanding status`, `/kwactors status`, `/kwprogress status`, `/kweconomyplan status`. Right-click a generated worldwide NPC to see a role/standing chat summary, and inspect its character sheet and K-chat.

## Rollback and safety

- `tools\geodata\output\b4_2b2_safe_snapshots\<timestamp>` holds a source/resource backup with checksums.
- Voluntary rollback: `.\RUN_B4_2B2_ROLLBACK_LATEST.bat` then `.\gradlew.bat clean build`.
- Back up `run\saves` separately before major playtesting; source rollback does not undo in-world progress.
- Do NOT run old B4.0, B4.1, B4.2, C4.15/C4.16/C4.17, B4.3A or B4.2B.1 install BATs.
- Other files such as scenario politics, real inventory journals, existing chat memories and B4.0 identity shards are neither snapshotted nor modified by this batch.

## Verification performed here

- 7 self-contained Python regression tests: 248-site manifest fixture, deterministic partition bytes, two-household staffing, unknown edits fail-closed, inconsistent household rejection, backup restore and simulated post-backup generator failure.
- Java 21 interface-stub compilation of the new runtime class.
- **Not** a complete Fabric Gradle compilation or 3.76m-record real data run; the guarded installer must verify both on your machine.

# B4.2B.4 — Worldwide Household Daily Finance Bridge (SAFE)

## What this batch really implements

- 248-site worldwide **household-by-household** opening allocation plans, sourced from the B4.2B.2 economic households and the B4.2B.3 **public** reserve proposal. Existing B4.0 identities, households, jobs and Winterfell V3 are never rewritten.
- For each indexed household: 14-day prospective food transfer (750g/person/day, rounded upward to whole kg), prospective starting coin share, estimated gross daily wage liability, and explicit `nativeHouseholdOwnerId=null` / `authoritativeTransactions=false`. All proposals are **from**, not **additional to**, the existing settlement public reserve. 40% of proposed public treasury is apportioned across indexed households by a deterministic prosperity/worker weighting; the remainder stays public. Nothing is actually transferred during installation.
- Household economic proposals are visible in right-click status data, individual NPC sheet context, and in `/kwfinance status` / `/kwfinance site <siteId>`.
- Adds `KnownWorldB42B4NativeDailyTransactions`, a native `InventoryManager.transfer` integration **for verified, already registered NPC-owner inventories** and canonical native settlement public inventories. It provides atomic *preflight* of 1–64 transfers, native checkpointing and a durable pending/committed per-day receipt. No alternate currency/resource ledger and no resource creation.
- Native bridge is intentionally **not called by a server tick or by `/kwfinance`**: B4.0 household strings have not yet been mapped to the existing `EconomicOwner`/`NpcId` identity types. There is no safe worldwide automatic wages or food consumption until that canonical bridge is completed.

## Safe scope

**Not activated:** World-wide household wallets, individual worker payments, daily consumption, production or famine. No new generated NPCs. No fabricated owners or lords. No automatic tax/revenue and no skill increments for unverified work. Winterfell V3 remains separately authoritative and will show unresolved household plans when applicable.

**Important accounting nuance:** `estimatedDailyGrossWagesCoinUnits` is an expected *liability*; it is NOT financed by public treasury indefinitely. Employers must have real revenue or existing coin resources before any recurring wages are activated. A flat wage estimate must NEVER be turned into an unbacked coin grant.

**One-time opening balances:** The B4.2B.3 public stock model is the total starting pool. This batch only plans prospective transfers of part of that pool to existing households. This avoids double-crediting grain/coin when native wallets are implemented. Never directly credit every household's proposed reserve *on top of* B4.2B.3 public reserves.

**After source rollback:** Minecraft save files are never restored by this installer. Once future manual code starts making actual Native EconomyRuntime transactions, source rollback alone cannot undo world save mutations; back up `run/saves` independently before any economic operation.

## Installation

1. Only install after your **successful B4.2B.3 + command-permission compile hotfix**. Do not run older batch installers again.
2. Make a backup of `D:\GameOfThronesMod2\run\saves` as a precaution.
3. Extract this ZIP into `D:\GameOfThronesMod2`, retaining `B4_2B4_STAGE` and all paths.
4. Run `RUN_B4_2B4_CHECK_ONLY.bat`. Expected: `KnownWorldB42B2EconomicStanding.java = READY`, new Java services = `NEW`, new resource directory = `NEW`.
5. Run `RUN_B4_2B4_WORLDWIDE_HOUSEHOLD_ECONOMY.bat` once. It runs nine regression tests, backs up every managed source/resource path, generates the worldwide compressed household finance resource, then runs `gradlew.bat clean build`. On error it attempts automatic restoration.
6. On success, `./gradlew.bat runClient` and use:

   - `/kwfinance status`
   - `/kwfinance site site_ne_001` (or any valid site)
   - `/kwstanding status`
   - `/kwstocks status`

7. Inspect a nearby generated NPC to see `openingHouseholdAssetsStatus = PLANNED_NOT_CREDITED` and the proposed household pantry, cash share, daily food and wage fields.

**NEVER execute `/kwstocks grant` merely to test this batch.** Those are real native economy transactions from the previous stage, subject to strict opt-in/snapshot authority.

## Rollback

`RUN_B4_2B4_ROLLBACK_LATEST.bat` then `gradlew.bat clean build`. Snapshots are stored in `tools/geodata/output/b4_2b4_safe_snapshots/<timestamp>/`. If subsequent batches have been installed, restore the specific matching snapshot ID, not blindly `latest`.

The auto installer never touches Minecraft saves, B4.0 identities or existing native economic inventories. Its own generated derived output is regenerated from pre-existing B4.2B.2/B4.2B.3 resources and is safe to restore/remake.

## Verification (package development)

- Nine Python regression tests including synthetic 248-site input, demographic conservation, allotment conservation, deterministic output, no-write/no-overwrite checks, rollback, unknown local source edits, simulated generator failure.
- Java 21 interface-stub compilation of the new native transaction coordinator.
- **NOT tested**: full Fabric compilation against your exact Minecraft mappings, real 799,343-household generation, or performance on your local machine. The guarded installer builds and stops/rolls back if needed.

## What must be completed before automatic economic activation (next B4.2B stages)

1. Collision-free, canonical B4.0 household -> native EconomicOwner inventory identity and durable mapping; explicit treatment of Winterfell V3 and unmatched sites.
2. Confirm 248 physical-site -> canonical SettlementId links. Proposals are NOT proof of native economic ownership.
3. Starting stock bootstrap and household transfer checkpoints with restart idempotency across economy+household+campaign-day data; no 799,343-entry transaction blast per tick.
4. Calibrated employer revenues, farm yields, wages, taxes, prices, ration distribution, and trade flows; ensure sustainable multi-year simulations including crop failure scenarios.
5. Witnessed work outputs and character skill practice events, sharing the same experience store and B4.1B LLM knowledge authority.
6. Full long-distance route-based NPC interception and event-driven travel remain in B4.3B.

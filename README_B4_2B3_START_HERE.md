# B4.2B.3 — Opening reserves & native economy transaction bridge

**Applies ONLY after successful B4.2B.2.** No older installation BAT needs rerunning.

## What is implemented

* Immutable, auditable 298 AC **public stock proposal for all 248 sites**, conserving the exact 3,760,684 individuals, 799,343 households and 1,880,341 modeled workers of B4.0/B4.2B.2.
* Proposed opening **public granary grain** at 750 grams per resident per day, 90 days of public reserves; **public treasury coin** at 12 minor currency units per resident plus 45 per worker. **These are editable policy assumptions, not historical facts and not already-credited balances.** No automatic grant occurs at install or server start.
* Native `EconomyRuntime` is the ONLY resource authority. An explicit, server-operator-only `/kwstocks grant <site_id>` uses `EconomyInventoryProvisioner`, `InventoryManager.produce`, and a verified `SNAPSHOT_PRIMARY` checkpoint. The grant is permitted **only** for a unique site-to-actual-SettlementId match within 24 map blocks **and** matching normalized place name. Missing or ambiguous links fail closed.
* An exclusive durable receipt is created **before** the resource transaction. A committed receipt is published only after a verified snapshot. Interrupted grants leave a PENDING receipt and **must never be automatically retried**. You must audit the world save and receipt before taking any recovery action.
* `/kwstocks status` and `/kwstocks audit <site_id>` report model totals, current authority, native settlement linkage, previous receipt state and planned stock. 248 site economic plan is available even when all grants are blocked.
* Source edits: B4.2B.2 standing class (one registration call), EconomyRuntime (one explicitly guarded checkpoint method), plus a new service. 248-site plan resource is generated without reading every person again.
* Installer checks exact B4.2B.2 standing source and the GitHub-master `EconomyRuntime.java` blob (SHA `759c8ea2ddaa77382712fd63342d48535f237f3d`), allowing CRLF vs LF. If you have newer legitimate local economy code, installer deliberately stops; **send the two Java files for a compatibility merge**. Do NOT weaken the guards.

## Critical scope distinctions

This phase *can* perform authoritative, persistent, verified **public** opening-stock grants at eligible sites. **It does not** automatically seed all 248 economic owners because the simulation has not yet proved a 248-to-canonical-settlement mapping. It also does **not** create personal wallets for 799,343 households: `EconomicOwner` has no canonical B4.0 household ID type. Inventing household `NpcId`s or a second ledger would corrupt identity/economy ownership.

There is **no automatic production, harvest, trade, wages, food consumption, business ownership, taxation, skill progression, or starvation** in this batch. That requires new native B4.0 household->economy owner identity mapping, calibrated goods/production, event-driven work transactions, and cross-domain save atomicity. Provisional B4.2B.2 prosperity classes are unchanged.

**Winterfell V3 (`site_nw_029`) explicitly blocks auto-public grants**: its authored economy and politics must not be overwritten with an aggregate assumption.

The proposal is *not* proof of household access to food. Public stocks must not be consumed as a substitute for households' balances without actual transfers.

## Installation

1. Back up `D:\GameOfThronesMod2\run\saves` before economic experiments. Installer does not modify saves, but `/kwstocks grant` intentionally does.
2. Extract ZIP **directly** into `D:\GameOfThronesMod2`, preserving `B4_2B3_STAGE`.
3. Run `./RUN_B4_2B3_CHECK_ONLY.bat`. Expected `READY` for `KnownWorldB42B2EconomicStanding.java` and `EconomyRuntime.java`, `NEW` for `KnownWorldB42B3StartingStock.java` and `world_opening_reserves_b4_2b3_v1`.
4. Run `./RUN_B4_2B3_AUTHORITATIVE_OPENING_STOCK.bat`; this executes seven regression tests, snapshots managed sources/resource, generates the full 248-site plan, and runs `gradlew.bat clean build`. Failures attempt automatic managed-source rollback.
5. Start `./gradlew.bat runClient`. Run `/kwstocks status`, `/kwstocks audit site_ne_001` (replace with a real site ID), and `/kwstanding status`.
6. **Do NOT run `/kwstocks grant` yet** unless your economy is explicitly in `SNAPSHOT_PRIMARY`, you've saved a FULL COPY of the Minecraft world, you've confirmed the exact site's canonical mapping and initial policy amounts, and you are deliberately opting into an irreversible save mutation. The installer never executes grant for you. Normal starting saves will often be in `LEGACY_COMPATIBILITY` and **must be blocked**.

### Authoritative transaction limitations

`/kwstocks grant <siteId>` is a deliberately small **site-by-site administrator transaction**, not `grant-all`. Every grant requires a valid preflight. If interrupted after the PENDING marker but before a complete checkpoint/receipt publication, **do not delete the marker or run grant again**. Preserve the world save and request a recovery review. The running server may have partially mutated in-memory inventory. Even source rollback CANNOT undo transactions already saved in `run/saves`. World-save rollback must use your separate backup.

We intentionally exclude mass money printing, automatic initial stock grants, daily taxation/consumption and all person-level asset allocations until the authoritative linkage/transaction safety work is complete.

## Rollback

Sources/resources only: `./RUN_B4_2B3_ROLLBACK_LATEST.bat`, then `./gradlew.bat clean build`.

Source rollback **does not** undo Minecraft economy checkpoint changes; for such changes restore the independently backed-up world folder.

## Verification performed in this package

* Seven Python regression tests: 248-site census, conservation, initial-stock plan/no writes, deterministic builds, source checks/rollback and simulated generator failure.
* Native-Java source API cross-check against upstream EconomyRuntime/InventoryManager/EconomyInventoryProvisioner.
* **Full Fabric build and actual world-data generation have not been executed here.** Installer tests them on your local project.

## Next B4.2B work

Canonical household ownership identity bridge, calibrated local industries, daily production/consumption, wages, skills and verified personal events. World travel/interception work remains separate B4.3B.

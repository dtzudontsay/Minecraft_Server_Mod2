# B4.2B.5 — Canonical household/economic authority preflight

**Source baseline:** `master` 0bc8a4784422cb45a3ca1c7779e2880585d981b7 (2026-10-28).  **Status:** read-only runtime bridge, pending local Gradle/Minecraft verification.

## Why this is the next batch

B4.0 has 3,760,684 stable starting person IDs and 799,343 household IDs. B4.2B.4 has 798,207 indexed household economic forecasts; 1,136 Winterfell households are deliberately unresolved. However, `EconomyRuntime` owns all actual resource inventories, with owner types NPC / SETTLEMENT / HOLDING / ORGANIZATION, and the original native `SettlementManager` contains only the scenario's authored/runtime settlements, not a guaranteed identity for all 248 worldwide physical sites. B4.2B.3 already has an exact-name-and-anchor canonical settlement matching rule; reuse that rule rather than creating duplicate settlements or deriving IDs from coordinates/hashes.

## Implemented in this batch

- Registers `/kwbridge status`, `/kwbridge site <siteId>`, `/kwbridge household <siteId> "<householdId>"` through the existing B4.2B.2 registration route.
- Reads the existing B4.2B.4 frozen manifest; validates global census, 248 unique sites, authority flag and unchanged source schema.
- On an explicit per-site request, reads the site-specific 16-way compressed household partitions, recomputes the SHA-256 partition index, checks uniqueness, member/worker counts, head IDs, all grain/coin allocations, and daily demand/wage sums against the site manifest. Does **not** scan 799,343 households on ticks.
- Reuses B4.2B.3 canonical `siteId -> SettlementId` lookup; reports failure rather than allocating a native settlement. If mapping exists, inspects pre-existing native settlement-owned inventory (read-only). Never represents a B4 household as a fabricated NpcId or temporary organization.
- Diagnoses the next blocker precisely: unresolved native site, missing verified public stock, or unimplemented native household economic owner account.

## Explicit non-features / safety

No new B4 people, households, settlement IDs, owner kinds, inventories, balances, transactions, production, wages, household food consumption, automatic daily triggers or changes to `run/saves`. Existing Winterfell, Ramsgate, Acorn Hall and historic saves untouched. Existing `EconomyRuntime` agriculture remains MONITOR_ONLY. This batch does NOT mean the economy is autonomous or even that any site is ready for payouts. The class intentionally does not cache large city household shards; commands are explicit diagnostics only.

## Dependency decision for B4.2B.6

1. Build a collision-free and persisted **adapter** between canonical B4 household string keys and future native `EconomicOwner.Kind.HOUSEHOLD`, using real source IDs, not a guessed NPC owner. Do not create disposable surrogate families.
2. Reconcile physical site -> native settlement against existing authored data and provide safe migration for missing settlement entities. This must not overwrite established settlements or break saves. A generic fallback to any nearest settlement is forbidden.
3. Add an explicitly versioned household-account binding ledger whose recovery and inventory snapshot ordering are defined. Never grant food/coin without an established native inventory and an idempotent durable receipt.
4. Verify workplace staff and employer as different roles from a workplace coordinator; do not pay unverified workers.
5. Only then enable a single-site guarded end-to-end food/wage test. Make the same canonical event runner work in daily ticks and fast-forward. Scale after multi-year equivalence tests.

## Engineering acceptance

- `python tools/geodata/test_b4_2b5_authority.py` passes before and after install.
- `gradlew.bat compileJava` completes (run from installed BAT).
- `/kwbridge status`, `/kwbridge site site_ne_001`, and `/kwfinance site site_ne_001` in a disposable 298 AC world.
- Invalid household key and site ID rejected; no new economy journal entry appears after running these commands.
- No change to initial B4.0 population, C4.11 construction manifest, Winterfell roster, or gameplay save files.

## Long-term values/traits system

See `docs/architecture/KNOWN_WORLD_VALUE_INTEGRATION_CONTRACT.md` included in the batch. This is a **parallel data/behavior acceptance contract** that spans the B4.2B economy, B4.3 travel, B4.4 politics, B4.5 social life and later D/G/H integrations. It is not to be deferred until the end.

# B4.2B.4 architecture — source-authority boundaries

`B4.0 identity` → `B4.2B.2 provisional economic standing` → `B4.2B.3 native public opening-stock proposal` → `B4.2B.4 household prospective TRANSFER plan`. None of those last three is an authoritative wallet unless `InventoryManager` actually accepts/saves a transaction through `EconomyRuntime`.

## Data schema

- `world_household_daily_b4_2b4_v1/manifest.json`: 248 sites, conserved global input census, indexed and unresolved household counts, proposed asset allocations, estimated daily household demands, reserve residuals, source SHA-256 hashes, activation=false.
- `<siteId>/p00..p15.json.gz`: sparse household records with existing B4.0 household IDs and current B4.2B.2 head/standing, 14-day grain share, treasury coin share, daily food demand in grams, gross wage liability, and null native economic ownership. GZIP is byte-deterministic (mtime=0) and partitioned by first 16 bits of SHA-256(householdId) mod 16.
- Winterfell V3 and unresolved/deferred sites retain census rows with no minted or copied local household assets.

## Why no automatic daily tick?

`EconomyRuntime` only recognizes canonical `EconomicOwner` kinds `NPC`, `SETTLEMENT`, `HOLDING`, `ORGANIZATION`. B4.0 household IDs are strings and must not be invented as `NpcId` values, and naive hashing may collide. The native journal also has bounded capacity and the economy/campaign/household save domains are not yet atomic. Automatic daily debits would create poverty/famine merely because the simulator skipped a production event or a save interval. That would violate project invariants.

The new `KnownWorldB42B4NativeDailyTransactions` is the future shared authority for actual verified daily transfers. It:

- accepts **registered** recipient NPC inventory IDs at the **same canonical settlement** as the funding treasury;
- preflights all accounts, balances, overflow and journal capacity under the native manager lock;
- writes a CREATE_NEW pending receipt, transfers via `InventoryManager`, then calls a verified B4.2B.3 native snapshot before renaming receipt atomically;
- **never** produces grain or coin; cannot be invoked from chat or by a default tick;
- fails closed after any interrupted transaction; reconciliation/repair must inspect both receipt and native journal before a retry.

This is an interface-level integration, not a proof of global atomicity. A failure between the last transfer, snapshot and receipt rename requires manual reconciliation. A stronger cross-domain transaction coordinator is necessary before unattended worldwide execution.

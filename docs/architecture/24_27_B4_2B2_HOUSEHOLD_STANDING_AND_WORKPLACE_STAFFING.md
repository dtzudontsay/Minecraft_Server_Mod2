# B4.2B.2 — economic bridge without a second economy

## Invariants

- Stable `B4.0` person and household IDs; no duplicates.
- Authoritative real inventory remains `EconomyRuntime / InventoryManager` alone.
- `householdProsperityIndex` is a rank estimate, **not a currency amount**.
- `coordinatorPersonId` is a candidate shift coordinator, **not a deed, employer, administrator, legal office or succession claim**.
- Every record is traceable to the existing B4.0 population or C4.15-enhanced B4.1 projection; the output is reproducible.
- Winterfell V3 remains independent, with census preserved but without duplicate persons.
- All 248 sites included, exactly `3,760,684` B4.0 people / `799,343` households / `1,880,341` workers.

## Runtime architecture

`B4.0` + `B4.1A.1` + `B4.2` => deterministic 16-way site-partitioned immutable starting-state catalog => `KnownWorldB42B2EconomicStanding.enrich()` => GUI data and individual LLM context. The authoritative inventory and skill/personal events systems remain external and must not be double-recorded.

## Future batch B4.2B.3 — actual economy

1. Resolve canonical property and household economic identifiers without inventing owner persons; link actual workplace lots and verified holding ownership.
2. Provide validated starting stocks and coin allocations to the existing inventory manager through idempotent all-or-nothing startup transactions, with no grants on reopen.
3. Calibrate agriculture, imports and reserve ratios before any food consumption. Run simulation dry-runs for harvest failures/price shock and avoid automatic famine on day one.
4. Authoritative work shifts affect production and wages using verified labor activity and correct job mappings, with one transaction per event interval.
5. Write social-economic events to the same personal event journal and use the existing 20-skill `SkillPracticeService` or a compatible B4.0 identity bridge.
6. Expose actual `OWNER` and `EMPLOYER` tags **only after** proof of property and employment contracts.
7. Require cross-domain atomicity or explicit fail-closed protections for calendar, inventory, ownership and household state.

B4.3B adds route-spatial discovery for independently travelling people, beyond scheduled home/work mobility. B4.4 political/family continuity; B4.5 social influence and conversation subtext are not replaced by economic baselines.

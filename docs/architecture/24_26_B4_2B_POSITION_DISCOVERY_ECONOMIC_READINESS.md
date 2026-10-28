# B4.2B.1 — Position queries, objective personal events, economic readiness

## Ownership invariants

- B4.0 identities remain canonical, unchanged and untouched by the installer.
- B4.2 remains responsible for campaign-time schedules and workplace registry.
- C4.17 remains responsible for the single physical navigation controller.
- B4.3A supplies virtual commute interpolation; observed handoffs no longer override calculated encounter positions.
- The existing `EconomyRuntime` and `InventoryManager` remain authoritative for real goods and money. No B4.2B economic resource transaction is emitted yet.
- Verified physical workplace proximity is an objective event. It is not proof that the assigned worker produced goods or received wages.

## Intercepting an unloaded commuter

Site-local origin-coordinates geography indexed into 256-block cells. Each compressed part contains `cell -> [original B4.1A.1 sourceTile, B4 person ID]`. Routes include original door, assigned workplace, the fortification gate that the B4.3A model would choose, and known leisure sites. Player query uses approved construction displacement to convert world to site-local coordinates. Final candidate admission invokes `KnownWorldB42DailyLife.virtualArrival` and checks actual distance, loaded chunk, global 256 actor budget, and source B4.0 person record. Spatial refs do not create new identities. Large crowded squares limit query work to 12,000 references and rotate the selection over subsequent scans.

No NPC scan of the full 3.76m population occurs at runtime. Precompute time depends on the existing B4.1A.1 projected census and volume of commuters.

## Source versus historical reference

The 90-day reserve calculation is not a historical claim nor an autonomous endowment. It is an **illustrative parameter** to help bound initial economic readiness. Before crediting physical stocks, we need documented geography-to-holding mappings, market/granary allocation, consistent resource units, supply-route inputs, appropriate tax/employer ownership and transaction checkpointing across the calendar/NPC/economy systems.

## Future sequence

1. B4.2B.2: authoritative initial inventory provisioning and housing/wealth/ownership bootstrap with audited no-double-credit behavior.
2. B4.2B.3: durable economic daily cycle; controlled activation, verified work-output mappings, actual wage/stock transfers and economically generated memories.
3. B4.3B: location-indexed non-commute journeys, persistent road-network paths and interception anywhere along those routes.
4. B4.4: worldwide holders, dynastic/non-dynastic governance, regency and succession bridges.
5. B4.5: cross-NPC event propagation and persuasion/subtext dialogue influenced by genuine personal knowledge, standing, values and skills.

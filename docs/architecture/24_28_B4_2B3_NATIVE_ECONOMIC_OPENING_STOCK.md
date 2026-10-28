# B4.2B.3: Authority and rollback invariant

1. Authored B4.0 identity and B4.2B.2 household standing are independent of actual economic stock. No duplicate B4.0 social entities can be created.
2. The existing `InventoryManager` and `EconomyAuthorityService` alone own real quantities and journal entries. Newly implemented grants must use their APIs and validated `EconomicOwner.SETTLEMENT`.
3. Native public granary/treasury is not household food or personal income. Until B4 household IDs are canonical real economic owners, wages and household balances are blocked.
4. The source plan preserves every B4.0 site and conserves the world population. Proposed initial money and grain are explicitly scenario assumptions.
5. No automatic grant. An authorized grant requires all of: explicit admin command, snapshot authority, valid known site, unique canonical matching settlement, nonzero proposed resources, no prior receipt, no existing grain/coin at target, journal capacity, durability of a pre-grant PENDING receipt.
6. After producing both resources, the native economy's verified `EconomyAuthorityService.save` is invoked synchronously. A successfully published `.committed.json` receipt indicates verified checkpoint; `.pending.json` blocks replay and requires manual adjudication, even after crash.
7. Server/world backups are separate from source rollback. Once an economic transaction has been saved, source rollback is not a world-state reversal.
8. This stage does not activate consumption, economic production, food shortages, wage or property transfers, regular tax, or political succession. Those require verified linkage and cross-system atomicity.
9. Medieval cities must not start hungry because calendar ticks passed before the economy was initialized. Readiness/activation is explicit and fail-closed.
10. All work will feed the same B4.0 identities and information interface; provisional socioeconomic labels must not be substituted for actual coins or property.

# Known World C4.9 — read-only-source planning package

1. Back up the repo and Minecraft saves. **Only you** extract these files into the root of `Minecraft_Server_Mod2`. No GitHub files have been edited by the assistant.
2. Run `RUN_C4_9_PLAN_READONLY.bat` from the repo root, not inside this archive folder. This reads the repository's existing C4.8 CSV reference files.
3. Read `tools/geodata/output/c4_9_settlement_ledgers/C4_9_SUMMARY.json` and confirm 248 sites, 3,760,684 people, 1,880,341 workers. C4.8 nominal household count is corrected for Winterfell without regenerating its identities.
4. Optionally run `gradlew.bat test` and inspect any compile/test failures. Only the new self-contained terrain policy Java class was compiled locally, not the full Minecraft mod.
5. The output is **provisional planning data**. No newly generated NPCs or physical builds have been activated. The next physical integration batch must connect the policy to the live generator, and it must never double-spawn Winterfell.

## Exact files supplied

- `tools/geodata/c4_9_settlement_ledgers.py`
- `src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC49TerrainPolicy.java`
- `src/test/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC49TerrainPolicyTest.java`
- `src/test/python/test_c4_9_settlement_ledgers.py`
- `docs/architecture/24_13_C4_9_UNCAPPED_GLOBAL_SETTLEMENT_DEMAND_AND_TERRAIN_POLICY.md`
- `RUN_C4_9_PLAN_READONLY.bat`
- `README_C4_9_START_HERE.md`

The command produces FIVE materialized ledger files locally; use the `--summary-only` switch if you want just the small summary reports.

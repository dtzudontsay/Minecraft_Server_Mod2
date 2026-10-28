# B4.2B.1: mobility index performance and interrupted-run repair

## Why the installer stopped

The real run indexed about 1.97 million residents before the user interrupted a long-running `leisure_place` scan with Ctrl+C. The Python traceback is **KeyboardInterrupt**, not evidence of a corrupt population or failing economy. The managed source/resource snapshot was restored as reported in the user's log. The earlier FileNotFoundError inside the unit suite is an intentional fault-injection rollback test, not a real-world missing resource.

The original generator inspected every public venue separately for every projected resident. The optimized generator builds a spatial venue index once per settlement, then examines only nearby candidates. The selection rule, child restrictions and per-NPC deterministic tie breaker are unchanged. Progress output now appears every 50,000 residents within large sites, as well as for each 32 processed site records.

## Install from D:\GameOfThronesMod2

1. Exit the previous batch prompt with `J` if `Batchvorgang abbrechen (J/N)?` still appears.
2. Extract this ZIP into the repo root. Keep `B4_2B_FAST_PAYLOAD` in place.
3. Run `.\RUN_B4_2B_FAST_MOBILITY_FIX.bat`.
4. Run `.\RUN_B4_2B_CHECK_ONLY.bat` and confirm previous `READY`/`NEW` statuses.
5. Run `.\RUN_B4_2B_POSITION_DISCOVERY_ECONOMY_FOUNDATION.bat`.

Do **not** rerun B4.0 through B4.3A BATs. Don't manually overwrite any Java files. The original guarded B4.2B installation still performs its full snapshot, resource regeneration and Gradle build.

## Safety behavior

- The hotfix **only** replaces `tools/geodata/b4_2b_mobility_index.py`, and only if it exactly matches the original SHA-256 packaged with B4.2B.1. It fails closed on unknown edits.
- If an older interrupted run left `world_mobility_b4_2b_v1.pending`, the hotfix checks that it contains only expected generated index files, and then **moves the whole directory intact** into `tools/geodata/output/b4_2b_safe_snapshots/generator_hotfixes/<timestamp>/incomplete_previous_index`. No incomplete files are silently deleted.
- A backup of the exact original generator is saved in the same hotfix snapshot folder.
- The optimized generator cleans its own incomplete `.pending` directory after Python exceptions or Ctrl+C, so future retries do not fail due to stale temporary output.
- Full B4.0 population, B4.1 projection, B4.2 workplace source data, Minecraft saves, and Java code are **not modified by this hotfix**.
- Optional `RUN_B4_2B_FAST_MOBILITY_FIX_CHECK_ONLY.bat` is read-only.
- Optional `RUN_B4_2B_FAST_MOBILITY_FIX_ROLLBACK.bat` restores the backed-up original generator if there is no generated mobility index present.

## Verification

- 8 dedicated hotfix regression tests passed.
- 10 original B4.2B.1 regression tests passed using the new generator, including synthetic generator failure and rollback.
- In a randomized stress check of 2,500 people against 1,200 public venues, the **venue-selection portion** ran approximately 70x faster and returned identical destinations. This is not a prediction for the entire 3.76-million-NPC indexing job.
- Full worldwide generation and Fabric build still need to run on the user's machine.

If the repair prints `UNKNOWN_LOCAL_EDITS`, stop and send that output rather than forcing a patch. If the original installer stops again, share its final output; the progress prints should reveal the specific site where work paused.

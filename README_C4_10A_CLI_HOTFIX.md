# C4.10A command-line preflight hotfix

Fixes the C4.10A `AttributeError: 'Namespace' object has no attribute 'arch'` that occurs **after** the 11 original tests pass.

## Installation

Extract this ZIP over your `D:\GameOfThronesMod2` checkout, preserving paths and allowing replacement of the two files:

- `tools/geodata/c4_10_shared_layout.py` (complete updated script)
- `src/test/python/test_c4_10_shared_layout.py` (complete updated tests, with an additional CLI regression test)

No GitHub files, Minecraft chunks, saves, NPCs, or committed files have been changed by the assistant.

## Cause

The argparse option is `--architecture`, stored as `args.architecture`, but `main()` read the nonexistent `args.arch`. The only production-code change is replacing `args.arch` with `args.architecture` in the call to `run`.

## Retry

From the repository root, run `RUN_C4_10A_PREFLIGHT.bat` again. You should now see **12 tests** pass, followed by the attempt to generate `tools/geodata/output/c4_10_shared_layout/C4_10_SUMMARY.json` and companion planning summaries.

The additional regression test invokes the actual command line with `--architecture`, `--summary-only`, and a temporary sample site. The generator remains planning-only. This fix does not activate worldgen.

## Verified in the supplied package

12 Python tests passed in this environment, including the new real subprocess/CLI regression test. An end-to-end run against your checkout's full 248-site C4.9 ledger still needs to run locally.

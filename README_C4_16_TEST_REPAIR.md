# C4.16 regression test re-run hotfix

This hotfix changes **one Python test file only**:

`src/test/python/test_c4_16_local_compatibility.py`

It corrects `test_exact_stage_repairs_enable_original_installer` to pass both when the stage still needs compatibility repair and when the repair was successfully applied before the test. It does not relax runtime source safety checks, change Java, or regenerate population/settlement data.

## Instructions

1. Extract the contents of this ZIP into `D:\GameOfThronesMod2`, allowing the **one test file** to be overwritten. Do **not** undo or repeat the compatibility fix.
2. (Optional) Run: `py -3 -m unittest discover -s src\test\python -p "test_c4_16_*.py" -v`.
3. Run: `.\RUN_C4_16_ORGANIC_TOWNS_AND_WORK_ATTENDANCE.bat`.
4. If that completes, run: `.\gradlew.bat clean build`.

The C4.16 installer will make its own code-and-resource snapshot at stage [2/6] before installing updated code. Do not start a new generated-chunks visual comparison until the installer and build succeed. No B4.0 population regeneration is required.

Safety: do not manually overwrite Java sources from C4_16_STAGE; this patch only fixes the test's assumptions.

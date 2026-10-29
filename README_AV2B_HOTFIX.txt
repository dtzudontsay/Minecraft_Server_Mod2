Known World — AV2B Guard Hotfix v1.1 (October 29, 2026)

BLOCKER:
  AV2B BLOCKED: Unexpected old derived tendency formulas; inspect manually

ROOT CAUSE:
  The original legacy method contains 12 result.put(CharacterDerivedTendency.*)
  entries plus one EnumMap<>(CharacterDerivedTendency.class) reference.
  The original installer incorrectly treated all 13 occurrences as map entries.
  Its guard expected exactly 12 and failed on correct source.

FIX:
  Replace only tools/av2b/av2b_guarded_install.py with this version.
  The guard now validates the 12 expected enum keys in order, separately
  from the EnumMap class reference. Other safety checks remain in place.

INSTALL:
  1. Extract this ZIP to D:\GameOfThronesMod2\, preserving paths;
     overwrite tools\av2b\av2b_guarded_install.py when prompted.
  2. In PowerShell:
       cd D:\GameOfThronesMod2
       .\RUN_AV2B_CHECK_ONLY.bat
       .\RUN_AV2B_INSTALL_AND_TEST.bat
  3. Install AV2C only AFTER AV2B's compile succeeds.

SCOPE:
  No existing Minecraft save, Java source, world resource, AV1 or AV2A
  file is changed by this hotfix. The check-only command is read-only.

VALIDATION:
  JDK 21 AV2B smoke test and nonmutating coverage check passed in the
  local fixture; guarded installation, idempotency, rollback, and parser
  negative tests passed. Your actual Fabric Gradle build still must pass.

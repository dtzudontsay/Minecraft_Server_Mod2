KNOWN WORLD — C3 ARCHITECTURAL BUILDING TEMPLATES

WHAT THIS IS
C3 replaces the basic C2 pilot building painter with a data-driven renderer covering all
43 C1 modules. Real blocks, furniture, fields/pens, roofs and supported stairs/ladders.
Each C1 module has a distinct form + workstation configuration; they are not 43
unique, canon-accurate hand-sculpted mega-buildings.

IMPORTANT PREREQUISITES
- B2+B3 installed: 253 markers / 259 holdings indexed
- C1 installed: tools/geodata/c1_building_grammars.json exists
- C2 installed: KnownWorldUniversalWorldgenC2.java contains its original call
  to buildLot, or this C3 call was already installed
- Minecraft 26.3 / Fabric mod environment, Java 25

INSTALL
Extract to D:\GameOfThronesMod2 keeping folder structure.
From PowerShell run:
  .\INSTALL_AND_VALIDATE_C3_ARCHITECTURE.bat

INSTALLER ACTIONS
1. Build architecture_templates_c3_v1.json from existing C1 grammar.
2. Validate 43 modules, safety rules, deterministic generation.
3. Back up KnownWorldUniversalWorldgenC2.java and fabric.mod.json.
4. Change exactly ONE invocation inside C2 to the C3 renderer.
5. Register the read-only /kwc3 inspection command.
6. gradlew.bat clean build.

The installer optionally repairs the known old C2 FarmBlock.MOISTURE mapping error
if the previous C2 compilation fix was not installed yet.

IN GAME
  /kwc3 status
  /kwc3 template BAKERY
  /kwc3 template ARMORY
  /kwc3 template HOUSE_MEDIUM
  /kwc3 template KEEP
  /kwc2 sites

Only TWO C2 pilot sites physically build; other archetypes are ready for future
C4 site layouts. Previously generated chunks WILL NOT be changed. New test world
recommended. This is not full population-sized cities or comprehensive special
fortresses. No NPCs, household signs, production, governance decisions or dynamic
construction are created.

IF BUILD FAILS
Send the COMPLETE output. Do not push source until build passes.
Backups: tools/geodata/output/c3_architecture/backups/
GitHub remains read-only.

@echo off
setlocal
cd /d "%~dp0"
echo [C4] Check C1, C2 and C3 resources...
py -c "from pathlib import Path; p=Path('src/main/resources/data/knownworld/scenarios/agot_298_ac'); a=['settlement_presets_c1_v1.json','settlement_construction_c2_v1.json','architecture_templates_c3_v1.json']; missing=[v for v in a if not (p/v).exists()]; assert not missing, 'Missing C1/C2/C3 prerequisites: '+str(missing)" || goto :fail
echo [C4] Run deterministic layout tests...
py -m unittest discover -s tools/geodata -p test_c4_settlement_layouts.py -v || goto :fail
echo [C4] Generate separate C4 construction resource...
py tools\geodata\build_c4_settlement_layouts.py || goto :fail
py tools\geodata\build_c4_settlement_layouts.py --check || goto :fail
echo [C4] Connect C4 manifest and FIELD painter to existing C2 engine...
py tools\geodata\install_c4_settlement_layouts.py || goto :fail
py tools\geodata\install_c4_settlement_layouts.py --check || goto :fail
echo [C4] Compile and test Minecraft mod...
call gradlew.bat clean build || goto :fail
echo C4 SUCCESS: ONLY the existing two physical pilots are enabled.
exit /b 0
:fail
echo C4 FAILED. Fix the first error shown above; do not delete any world.
exit /b 1
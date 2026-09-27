\# Known World Geographic Data Tools



This directory contains development tooling for converting the official

Known World cartography into geographic data used by the mod.



\## Input



Source map artwork is stored locally under:



tools/geodata/input/



These source images are development inputs and should not be distributed

as runtime mod assets.



\## Output



Generated geographic data is stored under:



tools/geodata/output/



The first milestone is:



known\_world\_land\_mask\_preview.png



This is a visual debug image.



White = provisional land  

Black = provisional water



The first-pass mask is NOT final geography.



It exists so coastline classification problems can be visually identified

before generating Minecraft terrain.



\## Pipeline



Master source map



\-> coarse land/water classification



\-> visual inspection



\-> correction/refinement



\-> high-resolution regional masks



\-> regional-to-master transforms



\-> merged canonical land mask



\-> coastline geometry



\-> Minecraft terrain generation


# DISPOSABLE SUPERFLAT CREATIVE WORLD ONLY; no authoritative NpcIds allocated.
# Page 001 / 050 of the 1200-record Winterfell roster.
# Only remove test villagers near the executing player, never untagged NPCs.
kill @e[type=minecraft:villager,tag=kwf_case,distance=..80]
summon minecraft:villager ~0 ~ ~0 {Tags:["kwf_case","kwfid_eddard_stark"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Eddard Stark"}',CustomNameVisible:1b}
summon minecraft:villager ~4 ~ ~0 {Tags:["kwf_case","kwfid_catelyn_stark"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Catelyn Stark"}',CustomNameVisible:1b}
summon minecraft:villager ~8 ~ ~0 {Tags:["kwf_case","kwfid_robb_stark"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Robb Stark"}',CustomNameVisible:1b}
summon minecraft:villager ~12 ~ ~0 {Tags:["kwf_case","kwfid_sansa_stark"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Sansa Stark"}',CustomNameVisible:1b}
summon minecraft:villager ~16 ~ ~0 {Tags:["kwf_case","kwfid_arya_stark"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Arya Stark"}',CustomNameVisible:1b}
summon minecraft:villager ~20 ~ ~0 {Tags:["kwf_case","kwfid_bran_stark"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Bran Stark"}',CustomNameVisible:1b}
summon minecraft:villager ~0 ~ ~5 {Tags:["kwf_case","kwfid_rickon_stark"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Rickon Stark"}',CustomNameVisible:1b}
summon minecraft:villager ~4 ~ ~5 {Tags:["kwf_case","kwfid_jon_snow"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Jon Snow"}',CustomNameVisible:1b}
summon minecraft:villager ~8 ~ ~5 {Tags:["kwf_case","kwfid_theon_greyjoy"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Theon Greyjoy"}',CustomNameVisible:1b}
summon minecraft:villager ~12 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_luwin"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Luwin"}',CustomNameVisible:1b}
summon minecraft:villager ~16 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_vayon_poole"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Vayon Poole"}',CustomNameVisible:1b}
summon minecraft:villager ~20 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_jeyne_poole"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Jeyne Poole"}',CustomNameVisible:1b}
summon minecraft:villager ~0 ~ ~10 {Tags:["kwf_case","kwfid_wf_named_rodrik_cassel"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Rodrik Cassel"}',CustomNameVisible:1b}
summon minecraft:villager ~4 ~ ~10 {Tags:["kwf_case","kwfid_wf_named_beth_cassel"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Beth Cassel"}',CustomNameVisible:1b}
summon minecraft:villager ~8 ~ ~10 {Tags:["kwf_case","kwfid_wf_named_jory_cassel"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Jory Cassel"}',CustomNameVisible:1b}
summon minecraft:villager ~12 ~ ~10 {Tags:["kwf_case","kwfid_wf_named_septa_mordane"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Septa Mordane"}',CustomNameVisible:1b}
summon minecraft:villager ~16 ~ ~10 {Tags:["kwf_case","kwfid_wf_named_septon_chayle"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Septon Chayle"}',CustomNameVisible:1b}
summon minecraft:villager ~20 ~ ~10 {Tags:["kwf_case","kwfid_wf_named_hullen"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Hullen"}',CustomNameVisible:1b}
summon minecraft:villager ~0 ~ ~15 {Tags:["kwf_case","kwfid_wf_named_harwin"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Harwin"}',CustomNameVisible:1b}
summon minecraft:villager ~4 ~ ~15 {Tags:["kwf_case","kwfid_wf_named_joseth"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Joseth"}',CustomNameVisible:1b}
summon minecraft:villager ~8 ~ ~15 {Tags:["kwf_case","kwfid_wf_named_farlen"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Farlen"}',CustomNameVisible:1b}
summon minecraft:villager ~12 ~ ~15 {Tags:["kwf_case","kwfid_wf_named_palla"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Palla"}',CustomNameVisible:1b}
summon minecraft:villager ~16 ~ ~15 {Tags:["kwf_case","kwfid_wf_named_old_nan"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Old Nan"}',CustomNameVisible:1b}
summon minecraft:villager ~20 ~ ~15 {Tags:["kwf_case","kwfid_wf_named_hodor"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Hodor"}',CustomNameVisible:1b}
tellraw @s {"text":"Winterfell case study: showing page 001 / 050 (24 of 1200) -- right-click villagers; no authoritative simulation materialization.","color":"yellow"}

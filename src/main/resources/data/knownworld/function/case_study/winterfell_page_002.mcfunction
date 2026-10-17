# DISPOSABLE SUPERFLAT CREATIVE WORLD ONLY; no authoritative NpcIds allocated.
# Page 002 / 050 of the 1200-record Winterfell roster.
# Only remove test villagers near the executing player, never untagged NPCs.
kill @e[type=minecraft:villager,tag=kwf_case,distance=..80]
summon minecraft:villager ~0 ~ ~0 {Tags:["kwf_case","kwfid_wf_named_gage"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Gage"}',CustomNameVisible:1b}
summon minecraft:villager ~4 ~ ~0 {Tags:["kwf_case","kwfid_wf_named_mikken"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Mikken"}',CustomNameVisible:1b}
summon minecraft:villager ~8 ~ ~0 {Tags:["kwf_case","kwfid_wf_named_hallis_mollen"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Hallis Mollen"}',CustomNameVisible:1b}
summon minecraft:villager ~12 ~ ~0 {Tags:["kwf_case","kwfid_wf_named_desmond"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Desmond"}',CustomNameVisible:1b}
summon minecraft:villager ~16 ~ ~0 {Tags:["kwf_case","kwfid_wf_named_jacks"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Jacks"}',CustomNameVisible:1b}
summon minecraft:villager ~20 ~ ~0 {Tags:["kwf_case","kwfid_wf_named_porther"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Porther"}',CustomNameVisible:1b}
summon minecraft:villager ~0 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_quent"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Quent"}',CustomNameVisible:1b}
summon minecraft:villager ~4 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_alyn"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Alyn"}',CustomNameVisible:1b}
summon minecraft:villager ~8 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_tomard"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Tomard"}',CustomNameVisible:1b}
summon minecraft:villager ~12 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_varly"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Varly"}',CustomNameVisible:1b}
summon minecraft:villager ~16 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_heward"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Heward"}',CustomNameVisible:1b}
summon minecraft:villager ~20 ~ ~5 {Tags:["kwf_case","kwfid_wf_named_cayn"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Cayn"}',CustomNameVisible:1b}
summon minecraft:villager ~0 ~ ~10 {Tags:["kwf_case","kwfid_wf_named_wyl"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Wyl"}',CustomNameVisible:1b}
summon minecraft:villager ~4 ~ ~10 {Tags:["kwf_case","kwfid_wf298_00001"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Janna Tanner"}',CustomNameVisible:1b}
summon minecraft:villager ~8 ~ ~10 {Tags:["kwf_case","kwfid_wf298_00002"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Willis Miller"}',CustomNameVisible:1b}
summon minecraft:villager ~12 ~ ~10 {Tags:["kwf_case","kwfid_wf298_00003"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Lymond Snowbrook"}',CustomNameVisible:1b}
summon minecraft:villager ~16 ~ ~10 {Tags:["kwf_case","kwfid_wf298_00004"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Sarra Hearth"}',CustomNameVisible:1b}
summon minecraft:villager ~20 ~ ~10 {Tags:["kwf_case","kwfid_wf298_00005"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Osmund Timber"}',CustomNameVisible:1b}
summon minecraft:villager ~0 ~ ~15 {Tags:["kwf_case","kwfid_wf298_00006"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Wenna Rowan"}',CustomNameVisible:1b}
summon minecraft:villager ~4 ~ ~15 {Tags:["kwf_case","kwfid_wf298_00007"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Myranda Barley"}',CustomNameVisible:1b}
summon minecraft:villager ~8 ~ ~15 {Tags:["kwf_case","kwfid_wf298_00008"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Lew Reedbank"}',CustomNameVisible:1b}
summon minecraft:villager ~12 ~ ~15 {Tags:["kwf_case","kwfid_wf298_00009"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Dacey Whitebark"}',CustomNameVisible:1b}
summon minecraft:villager ~16 ~ ~15 {Tags:["kwf_case","kwfid_wf298_00010"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Elyn Flintwell"}',CustomNameVisible:1b}
summon minecraft:villager ~20 ~ ~15 {Tags:["kwf_case","kwfid_wf298_00011"],NoAI:1b,PersistenceRequired:1b,CustomName:'{"text":"Walder Hale"}',CustomNameVisible:1b}
tellraw @s {"text":"Winterfell case study: showing page 002 / 050 (24 of 1200) -- right-click villagers; no authoritative simulation materialization.","color":"yellow"}

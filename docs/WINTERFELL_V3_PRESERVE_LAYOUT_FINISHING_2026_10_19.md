# Known World — Winterfell V3 Preserve-Layout Finishing Pass

**Date:** 2026-10-19. **Target:** 26.3, Fabric, Java 25. **Site:** atlas NW-029, Minecraft X −4,050,173 / Z −1,976,496. **Provenance:** implementation refinement of current V3 test, *not* a redesigned settlement plan.

## Locked geometry / census

- **5,000** permanent persons and **five separate visiting characters** are still fed by the existing `winterfell_v2.json` registry import.
- **1,136** occupied household residence plans, **378** workplace plans = **1,514** total building footprints, no changes to their positions or capacities.
- **One castle**, **one 10× winter town**, **four 5× villages**, **15 1× hamlets**.
- Worldgen index contains **6,669** planned chunks and unchanged 3-wide converted road pixels from original **77,213** centre-line points.
- NPC projection budgets remain **128 detailed + 128 simplified**. No changes to NPC lifecycles or save/persistence architecture.

## Diagnosed defects and implementation

1. **Castle roads hit walls.** Previously the wall gate mask had only east-aligned gaps. New outer-wall gates are at existing crossings near east z=15 and z=−23, west z=15, north x=158, south x=158. New inner-wall crossings are east z=15,94,118 and west z=15. All are relative to atlas NW-029. Wall and road coordinates are otherwise untouched.
2. **Flat cross-shaped tower tops.** Tower roofs now have tiered dark-slate caps, crenellation detail and arrow-slit bars; they remain inside the existing tower footprints in X/Z.
3. **Dark, hollow keep.** The existing keep receives a two-panel door, two staircase flights (ground to first and first to second upper deck), and ground/deck lantern groups. No exterior keep footprint or roof outline moved. Further authored interiors and facilities remain future work.
4. **Unlit ordinary houses.** All planned building templates receive four ground-floor lantern placements; upper-status houses receive a second-floor lantern and a ladder in the pre-existing floor shaft.
5. **Vanilla pillager outposts.** An installer overrides `createStructures` **only in `KnownWorldChunkGenerator`**, rejecting vanilla structure starts within 4,200 blocks along both axes of NW-029. Other dimensions, ordinary Minecraft worlds and other Known World territories keep vanilla structure generation for now. This is conservative until global cultural settlement replacement is ready.
6. **Lava lakes near settlement.** New chunks in the castle 240-block landscaping zone and in building-bearing chunks now replace exposed lava fluid with stone. The worldgen index and normal rivers/lakes remain intact. It is not a global lava ban or a retrofit of old chunks.
7. **Unclickable UI X and tabs.** Switched from manually painted click regions to `Button.builder(...).bounds(...).build()` registered widgets. Custom rendering runs before the widgets so their controls appear on top and receive normal focus/hit testing.
8. **Partial/unfriendly NPC profile.** Server packet now includes major existing `CharacterProfile` sections: education, background, health, legal status, appearance, skills, aptitudes, values, dispositions, social norms, languages, traits, offices, roles, social standing, dialogue style and life motivations. Strings are formatted for gameplay only, leaving authoritative keys unchanged. GM-only tab provides IDs, raw affiliation keys, private facts and secrets; non-GM players never receive secrets/private facts over the wire.

## Known limitations

No existing structure in saved chunks changes by updating this code. Use a new test save or deliberate supported chunk regeneration; deleting region files or running V2's destructive construction command is not advised. The keep still requires detailed furnishing and lore-accurate design. Connected roads are not yet fully AI-navigation-certified on steep terrain. `CharacterProfile` is not the complete collection of knowledge, memories, genealogy, relationship graphs and secrets known by the player; these exist in other runtime managers and require a knowledge/visibility policy before being displayed.

This patch has passed deterministic offline structure-index/roster checks and an idempotent installer test. It has not been compiled in the user's Java 25 / Fabric 26.3 runtime; collect `clean build` output and in-game screenshots before expanding elsewhere.

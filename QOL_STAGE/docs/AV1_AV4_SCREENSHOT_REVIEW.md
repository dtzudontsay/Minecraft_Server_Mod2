# Known World AV1–AV4 screenshot review — 30 October 2026

**Basis:** user-supplied `sc.zip`, 86 PNGs, plus repo source and `agot_298_ac` B4 projection / population manifests at GitHub master `f40215b5...`. The screenshots do not include a controlled before/after save-reload comparison or a trace of actual movement, so this is not a complete acceptance test.

## Diagnostic screenshots

| Screenshot | Evidence | Assessment |
|---|---|---|
| `2026-10-30_01.13.45.png` | AV1: 706 definitions, 30 domains, 761 original rows, 37 aliases; no gameplay activation | Registry loaded successfully |
| `2026-10-30_01.14.06.png`, `01.14.43.png` | AV3 profiles and catalogs validated; AV2A overlay ready; AV2B twelve shared derived tendencies; AV2C journal active | Initialization passed; not proof every location has plausible authoring |
| `2026-10-30_01.15.20.png` | AV2D session accepted 0 completed outcomes; AV4A free-time decisions evaluated 222, 0 blocked, 101 home / 59 public / 62 neighbor intents; 248 B4 daily-life sites; 1 day advanced; 67 observed workplace days | AV4 decision evaluation is happening; physical movement outcome and save/reload remain unverified |
| `2026-10-30_01.15.34.png` | `/kwactors nearby` list has labels but not canonical person IDs; several entries `unknown_culture/unknown_religion` | Diagnostic usability issue and scenario-data gap |
| `2026-10-30_01.37.07.png` | Aren Fieldwick has canonical ID `b4:site_ne_007:00000296`, household and home IDs, workplace `Unrecorded` | Identity stable; real occupation unknown |
| `2026-10-30_01.27.51.png`–`01.35.15.png` | Multiple Ollama responses received, but verbose/repetitive and sometimes echo prompt policy | Dialogue is functioning, quality needs improvement |
| `2026-10-30_01.35.42.png`–`01.37.02.png` | Extended Aren six-tab profile | Present/inspectable, but missing canonical cultural/faith/job facts |

## Vaes Dothrak: source-data gap

`world_projection_b4_1a1_v1/manifest.json` explicitly associates `site_ne_007` with **Vaes Dothrak**. It has 450 indexed people and 113 households and a regional profile `unknown` with no regional values or norms. More importantly, the authoritative `world_population_b4_v1/manifest.json` for the same site has `regionProfileId=UNRESOLVED`, `cultureCounts={unknown_culture:450}`, `religionCounts={unknown_religion:450}`, and `languages={unknown_language:1}`. This is a systematic data-authoring deficiency, not a bug that should be hidden by making Ollama pretend all 450 residents share one culture/religion/language. The identities need audited, plausible local and visitor distributions and durable migration from provisional identifiers.

## Aren Fieldwick conversation: assessment

His screenshoted values include: age 28; courage 0.24, ambition 0.61, compassion 0.30, honor 0.50, patience 0.33, sociability 0.44, empathy approximately 0.01, curiosity approximately -0.06, and friendship loyalty approximately 0.03; unrecorded trade/workplace; unknown culture and religion. His resistance to a risky offer fits low courage; concern about finding stability fits moderate ambition. His elaborate metaphors about the city and wind, frequent theatrical refusals, and talking about utterances as mere declarations of intention are unnatural meta/dialogue leakage, not psychologically evidenced qualities. He also implies unverified job prospects and household resources.

Subjective scores: **personality consistency 6/10**, **conversational realism 3/10**, **factual grounding 5/10**. The conversation is not evidence that the simulated city contained specific social events, strangers or work offers, beyond the player's own text.

## What passes and what does not

- PASS, initialization observed: AV1 vocabulary; AV2A overlay; AV2B psychology; AV2C journal; AV2D ledger; AV3 rule readiness; AV4 decision evaluation; B4 daily lifecycle.
- NOT YET VERIFIED: AV2A saved edits survive a shutdown/restart; AV2D idempotent application with a real completed outcome; AV4 different choices lead to observed walking targets; NPCs remain accessible after projection eviction; LLM actual model identity (not printed in screenshots).
- DATA ISSUE: Vaes Dothrak all 450 provisional culture and faith assignments unresolved. AV3 cannot solve this by tuning formulas alone.
- DIALOGUE ISSUE: source default remains `qwen3.5:9b` absent environment override; outputs are overlong and often repeat invented motifs.

## QoL patch and next checks

Install the sibling `KnownWorld_AV4QOL_Ollama27B_NPCLookup.zip` after checking Java compile. It changes only the worldwide B4 K-dialogue default model to `qwen3.5:27b`, adds concise-grounded speech instructions, and adds admin `/kwactors list`, `where`, `sheet`, `goto` for B4 NPCs. When one is unloaded, the `where`/`goto` commands can use its **original source home** for small indexed sites, not its unknown live position.

When model installed, run `ollama list` and `ollama ps` on the server machine, `/kwactors status` in game, `/kwactors sheet "b4:site_ne_007:00000296"` if Aren is still projected. On a disposable world, set a value, inspect / preview, restart game, verify persistence, and clear it. If Aren has unloaded, use another listed active B4 NPC for the persistence test. No need to test with Ollama for purely mechanical attributes.

## Future integration contract

Do not insert all 706 source definitions into every LLM prompt. Only the NPC's applicable live character dimensions, actual culture/religion/language and norms, verified knowledge/relationships, measured disposition and current intent/outcome should be relevant. Reconcile source records, derived tendencies and individual memory with bounded context windows; LLM speech remains descriptive and must not invent verified world actions. When a value gains a gameplay consumer (AV5–AV12), expose its consequence to the conversational context with provenance; no speech-only 'complete' status.

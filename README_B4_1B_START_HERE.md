# B4.1B — Worldwide Individual LLM Conversations

## Purpose

Adds K-key local-Ollama dialogue to **existing projected B4.1A.1/C4.15 residents** across worldwide C4.11 settlements. Keeps B4.0's 3,760,684 stable person identities, all households, original jobs, and C4.15 roads/commutes intact. **Does not instantiate or simulate millions of Minecraft entities.** Winterfell's V3 actors and its own dialogue behavior are preserved.

## Installation — important

1. Extract **the whole ZIP with its directory structure** into `D:\GameOfThronesMod2`.
2. The new dialogue Java classes go directly under `src/main/java/.../simulation/population/`. There is **no separate PAYLOAD folder** in this batch.
3. The installer will patch, back up and verify **three existing Java sources**. Do **not** copy the files from an older ZIP over the updated sources.
4. Run in PowerShell at the project root:

```powershell
.\RUN_B4_1B_WORLDWIDE_DIALOGUE.bat
.\gradlew.bat clean build
```

5. Launch `runClient` as usual. No fresh save, B4.0 regeneration, or C4.15 rerun is required for conversations.

### Exact prerequisite

Your locally installed B4.1A.1/C4.15 `KnownWorldB41Activation.java`, `WinterfellConversationPackets.java` and `WinterfellDialogueScreen.java` must match the expected integration anchors. The BAT performs a read-only check **before modifying anything** and backs up every existing source it changes. If an anchor check fails, **stop and send the first error**, rather than running older installers or a Git reset.

Automatic source backups (per installation run):
`tools/geodata/output/b4_1b_source_backups/<timestamp>/...`

## In-game checks

- Teleport to a newly constructed or previously generated C4.11 settlement with B4.1A.1 villagers. A new world is unnecessary for NPC dialogue.
- Right-click a villager: the existing rich inspection screen stays unchanged.
- Aim at the villager within approximately six blocks and press **K**. The dialogue screen says **KNOWN WORLD / CONVERSATION** for worldwide residents; Winterfell continues saying **WINTERFELL / CONVERSATION**.
- Ask: `What is your name? What work do you do? What faith do you follow?`
- Close and reopen K dialogue with **the same person**. It should report the existing count of completed exchanges. Ask what you spoke about.
- Speak to **another** villager. They should not know the other person's private conversation.
- Try `!relations`, `!praise`, `!insult`, `!threaten`. These update validated per-NPC, per-player relationship records, **not** world-event facts.
- Repeat in Winterfell to confirm the existing author-authored NPC chat is unaffected.

### Local Ollama

Uses the *same model and endpoint settings* as existing Winterfell chat:

| Environment variable | Default |
|---|---|
| `KNOWNWORLD_OLLAMA_MODEL` | `qwen3.5:9b` |
| `KNOWNWORLD_OLLAMA_URL` | `http://127.0.0.1:11434/api/chat` |
| `KNOWNWORLD_OLLAMA_CONTEXT` | `8192` |

Ollama must be running and the model pulled, e.g. `ollama pull qwen3.5:9b`. If Ollama is unavailable, the server uses conservative basic character dialogue and reports the reason; it cannot silently turn a failed response into a simulated world event. One worldwide Ollama request is serviced at a time (a separate limit from Winterfell's own pipeline).

## Persistence and privacy

- Uses existing `WinterfellConversationLedger`, shared storage implementation, keyed by **the actual B4 person ID + player UUID**; each pair gets independent persisted completed exchanges. At most twelve recent exchanges are stored, and only six are sent as old prompt context.
- Uses the existing `WinterfellPlayerRelationshipStore` for validated social reactions, again scoped to NPC + player.
- Conversation state lives in the world's own `data/knownworld/conversation_memory_v1/` and `data/knownworld/player_relationships_v1/`, not globally across worlds.
- Source history is explicitly **speech, not truth**. Incoming player claims, LLM claims, and remembered dialogue NEVER create verified NpcKnowledgeManager beliefs, WorldEvent records or memories automatically.
- No NPC can read another NPC's conversation ledger merely by belonging to the same household.
- The server validates the physical actor, current projection UUID, range, and session token on open, send and asynchronous completion. Closing or moving away invalidates late replies; those are not saved as heard.
- A corrupt memory file stops a conversation rather than being overwritten. A failure to persist a completed reply produces a warning.

## Boundaries — do not mistake B4.1B for B4.2/B4.3

Worldwide person **profiles** and regional differences come from the deterministic B4.1A.1 projection. They are not yet persistent, organically evolving `CharacterProfile` instances in the large `NpcRegistry`.

Real personal **witnesses**, simulated meetings between NPCs, learned world news, travel, work schedules, skill learning and offscreen life are **not yet connected to the worldwide population**. NPCs must admit they lack evidence. The speech guard blocks a selected set of invented eyewitness/player statements and known historical contradictions, but **no deterministic filter can catch every LLM hallucination**.

Only residents that the current B4.1A.1/C4.15 projector physically materializes (up to 128 at once) can be chatted with by K. Unbuilt or unplaced sites and the separate C2 Ramsgate/Acorn Hall mappings still need their physical projection adapters. Winterfell keeps using its separate V3 system.

## Build/testing status

The new service and guard passed a Java 21 interface compilation against local Minecraft/Fabric stubs. Python installer regression tests passed, including safe-backup/repeat-install and conversation storage-contract checks. The full Fabric build and actual in-game local-Ollama responses **must be verified in your checkout**.

## Source inventory

New:
- `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueService.java`
- `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41DialogueTruth.java`
- `tools/geodata/b4_1b_install.py`
- `src/test/python/test_b4_1b_install.py`
- `src/test/python/test_b4_1b_guards.py`
- `docs/architecture/24_21_B4_1B_WORLDWIDE_DIALOGUE_BRIDGE.md`
- `RUN_B4_1B_WORLDWIDE_DIALOGUE.bat`

The BAT patches and backs up these existing files **locally**:
- `src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/WinterfellConversationPackets.java`
- `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java`
- `src/main/java/dev/dtzudontsay/knownworld/client/casestudy/WinterfellDialogueScreen.java`

# AV4-QOL Hotfix — Model, Speech and Active B4 NPC Lookup

Base expected: KnownWorld GitHub master around f40215b5 (AV4A installed). Guarded anchors must match; otherwise installer fails without editing.

1. Extract package into repository root alongside `gradlew.bat`.
2. Run `RUN_AV4_QOL_CHECK_ONLY.bat`.
3. Run `RUN_AV4_QOL_INSTALL_AND_TEST.bat` (runs the actual Gradle `compileJava`).
4. Back up world before testing persistence edits.

## Qwen3.5:27b

Changes the default *requested* Ollama model from `qwen3.5:9b` to `qwen3.5:27b`.
If `KNOWNWORLD_OLLAMA_MODEL` is set on the running Minecraft server process, **that environment variable still overrides the default**. `/kwactors status` prints the selected configured model but does not test Ollama connectivity.

Use `ollama list` to check installed model, `ollama pull qwen3.5:27b` to install as needed and `Invoke-RestMethod http://127.0.0.1:11434/api/tags` to check Ollama availability (run on server machine). No automatic model download is attempted.

## Shorter, grounded dialogue

Adds specific concise speech instructions, reduces the generation token budget from 384 to 120, and adds dispositions and aptitudes to the LLM's world-profile context. The prompt still disallows invented historical events and does not let speech perform simulated actions. Because outputs are probabilistic, this is not a formal response-length guarantee.

## Commands (admin)

- `/kwactors status` — shows configured model and active actor diagnostics
- `/kwactors list` — up to 35 currently projected people with full B4 IDs
- `/kwactors where "b4:site_ne_007:00000296"` — active actor coordinates OR bounded original B4 source-home X/Z if unloaded
- `/kwactors sheet "b4:site_ne_007:00000296"` — opens the **full** six-tab sheet remotely, active actor only
- `/kwactors goto "b4:site_ne_007:00000296"` — teleports to a **currently projected** B4 actor, OR (creative, overworld, site <=10,000 residents/32 source tiles) to its *original home point*, provided terrain is clear. A source-home jump may load/generate its destination chunk; not a live NPC locator.

Important: The worldwide B4 entity may unload when the player leaves. Source-home fallback scans only a bounded small site and may transport you near its authored residence, **not** its live location. Very large sites are deliberately blocked until an indexed asynchronous lookup exists. Do not use source-home teleport in survival mode. Wait about a minute for nearby NPC projections after teleport.

Existing `/kwprofile inspect` is a sparse-edit report, **not** the whole profile. The patched `/kwactors sheet` opens the full sheet for a currently projected actor regardless of right-click distance.

## Integration and safety

Only two existing files are modified, both under `src/main/java/dev/dtzudontsay/knownworld/simulation/population/`:
- `KnownWorldB41DialogueService.java`
- `KnownWorldB41Activation.java`

No changes to NPC identity, player saves, economy, conversations already recorded, or world files during installation. The installer stores backups under `.git/knownworld-av4-dialogue-qol-backups/`, validates exact anchors, is idempotent, runs `compileJava` and restores original sources upon a failing compile. It does not touch `WinterfellConversationService` or V3-specific dialogs; only worldwide B4 K-dialogue is changed.

## Validation boundary

The patch source guard and Python tests are run outside Minecraft. **Only your local full Fabric build and in-game test can confirm compatibility with every dependency**. If compile fails, provide terminal output and do not stack new batches before fixing.

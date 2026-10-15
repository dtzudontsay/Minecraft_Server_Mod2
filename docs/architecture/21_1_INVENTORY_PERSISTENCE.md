
# Known World - Batch 21.1 Inventory Persistence

Status: Initial implementation
Scope: Economy inventory save/load

## 1. Purpose

Introduce versioned persistent inventory storage.

The implementation must preserve:

- Inventory identity
- Ownership reference
- Settlement location
- Resource quantities
- Next inventory ID

## 2. File Format

File:
economy_inventories.tsv

Version:
KNOWNWORLD_ECONOMY_INVENTORIES 1

Records:
NEXT_ID
INVENTORY
BALANCE
CHECKSUM

Text encoding:
UTF-8

The file uses tab-separated records.

## 3. Integrity

A SHA-256 checksum protects the serialized payload.

Malformed or mismatched records are rejected.

The checksum detects accidental content changes
but does not authenticate the save.

## 4. Save Behavior

Inventory persistence writes a temporary file.

The implementation attempts atomic file replacement
where supported by the filesystem.

The caller must prevent concurrent inventory mutation
while constructing the save snapshot.

No cross-domain transaction exists yet.

## 5. Load Behavior

Loading constructs a new InventoryManager.

A malformed save is rejected with IOException.

Existing live inventory state is never partially
modified by the loader.

An absent save produces an empty manager.

It does not generate resources or inhabitants.

## 6. Compatibility

Existing Known World save files are unchanged.

The new file is not created during normal server
saving because runtime integration is not enabled.

The new persistence contract can be tested without
launching Minecraft.

## 7. Remaining Requirements

Before runtime integration:

- Validate owner references against existing managers.
- Validate settlement locations.
- Define canonical resource catalogs and units.
- Define first-run economy initialization.
- Establish save snapshot boundaries.
- Decide how corrupted economic saves are recovered.
- Provide migration and backup procedures.
- Integrate persistence with the server lifecycle.
- Add transaction provenance and economic reports.

## 8. Acceptance Criteria

- JUnit tests pass.
- Saving and loading preserve resource balances.
- Inventory IDs survive loading.
- Corrupted files are rejected.
- Empty inventories survive round-trip.
- Loaded managers continue accepting valid transfers.
- Existing Minecraft saves remain unchanged.

## 9. Next Batch

Batch 21.2:

Economy reference integrity and runtime integration.

Requirements:

- Attach inventory ownership to existing identities.
- Introduce controlled simulation lifecycle wiring.
- Keep existing saves compatible.
- Do not create arbitrary starting resources.
- Add persistence and initialization smoke tests.

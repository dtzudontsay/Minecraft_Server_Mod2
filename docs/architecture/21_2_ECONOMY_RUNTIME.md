
# Known World - Batch 21.2 Economy Runtime

Status: Initial runtime integration
Scope: Inventory loading, validation and saving

## 1. Purpose

Connect the existing economy inventory model to
the Minecraft server lifecycle without automatically
introducing resources into existing worlds.

## 2. Runtime Lifecycle

EconomyRuntime registers Fabric callbacks for:

- Server started
- Before save
- Server stopping
- Server stopped

The existing NPC simulation must initialize before
the economy runtime loads saved inventories.

## 3. Existing World Compatibility

A world without economy_inventories.tsv begins
with an empty InventoryManager.

It does not receive:

- Automatically created inventories
- Starting grain
- Starting currency
- New population
- Economic production

An empty economy without an existing save file
is not automatically written to disk.

## 4. Reference Validation

Economic inventories refer to existing
authoritative identities.

Supported live validation:

- NPC ownership
- Settlement ownership
- Organization ownership
- Settlement location

Holding ownership is rejected until an
authoritative holding lookup is connected.

Reference validation is required when loading
and before saving inventories.

Invalid references fail closed.

## 5. Failure Policy

Malformed economy save files must not be silently
replaced by empty inventories.

If loading or reference validation fails, economy
initialization throws an error.

If saving fails, the error is logged and propagated.

Manual recovery must begin from a backup rather
than deleting or overwriting the damaged data.

## 6. Persistence

The economy continues using the versioned
economy_inventories.tsv format from Batch 21.1.

No format migration is introduced in this batch.

Inventory persistence is separate from NPC,
holding, and title persistence.

Cross-domain atomic snapshots are not implemented.

## 7. Scheduling

No economic progression is scheduled yet.

Campaign speed remains configurable.

Offline progression remains disabled.

Separate historical and physical pause controls
remain planned.

No economic production or consumption is
automatically executed by this runtime.

## 8. Tests

EconomyReferenceValidatorTest verifies:

- Existing NPC owners
- Missing NPC owners
- Existing settlements
- Missing settlement locations
- Existing organizations
- Missing organizations
- Holding validation
- Unsupported holding references
- Whole-manager validation

InventoryPersistenceTest from Batch 21.1
continues testing file round-trips.

## 9. In-Game Smoke Test

Use a backed-up development world.

1. Launch Minecraft.
2. Load the world.
3. Verify NPC simulation initializes.
4. Verify the economy runtime logs readiness.
5. Save and quit.
6. Verify preexisting save files remain intact.
7. Confirm that an empty economy does not
   generate an inventory save.
8. Reload and check that initialization succeeds.

## 10. Known Limitations

- No household economy
- No production or consumption scheduler
- No transaction journal
- No cross-domain save transaction
- No direct holding-manager integration
- No player-facing economic UI
- No new inventory initialization command
- No automatic economic migration

## 11. Next Batch

Batch 21.3:

- Full holding-owner validation
- Resource catalog policy
- First-run economy initialization
- Controlled inventory creation
- Migration and backup safeguards
- Persistence recovery tests

Runtime initialization must remain idempotent.

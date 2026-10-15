
# Known World - Batch 21.3

## Holding Integration and Economic Initialization

Status: Initial implementation

### 1. Purpose

Extend the economy runtime to support real holding
ownership and safe, explicit account initialization.

### 2. Holding References

Holding IDs remain authoritative in the existing
LandedHoldingManager.

EconomyRuntime references the manager through
SocietyStructureRuntime.

The economy does not create a parallel holding
registry or reinterpret authored holding identifiers.

Every inventory references a valid owner and
settlement location at load time and save time.

### 3. Controlled Initialization

EconomyInventoryProvisioner is responsible for
provisioning empty inventories.

It:

- Validates owner references
- Validates settlement locations
- Checks for an existing matching inventory
- Reuses a matching inventory
- Creates an empty inventory only when necessary

Provisioning never generates resource balances.

### 4. No Automatic Scenario Seeding

Launching an existing world does not:

- Create inventories for all holdings
- Give households starting grain
- Generate gold
- Derive population from holding weights
- Begin economic production
- Create arbitrary wages or taxes

Scenario initial balances need a separate,
explicit, validated initialization transaction.

### 5. Persistence

Provisioned empty inventories use the existing
versioned economy_inventories.tsv format.

Inventory identity and ownership survive reload.

Loading must not provision new accounts implicitly.

### 6. Idempotency

Calling the provisioner twice for the same
owner/location pair returns the existing account.

This policy applies to the provisioning service.

The InventoryManager still allows multiple
inventories for the same owner and location.

Future inventory roles must explicitly distinguish:

- Household stores
- Estate granaries
- Merchant stock
- Treasury
- Military supplies
- Workplaces

The permanent identity policy for multiple
inventories per owner remains to be implemented.

### 7. Resource Unit Policy

Canonical resource units remain a design decision.

The benchmark catalog currently uses:

- Grain: kilograms
- Timber: kilograms
- Coin: integer minor currency units

A future scenario resource catalog must explicitly
define physical and monetary units.

Historical trade measures should be convertible
display conventions rather than changing canonical
quantity meanings between regions.

No resource unit migration occurs in this batch.

### 8. Safety

Existing world saves remain unchanged unless an
economic account is explicitly provisioned.

Invalid owner or settlement references are rejected.

Corrupt inventory saves are not reset automatically.

Cross-domain atomic persistence is still missing.

### 9. Validation

Required acceptance checks:

- Gradle regression tests pass
- Existing save loads
- Existing society structure runtime loads
- Economy holding validation initializes
- Empty economy stays empty
- Restart does not create duplicate accounts
- Provisioned accounts round-trip in isolated tests

### 10. Remaining Work

- Canonical resource catalog
- Explicit initialization plan
- Account roles and stable provisioning keys
- Resource transaction history
- Economic reference reconciliation
- Cross-domain save consistency
- Household and population integration
- Production and consumption scheduling

### 11. Next Batch

Batch 21.4:

Economic transaction provenance and resource
accounting audit.

The next system should establish traceable causes
for every resource creation, removal and transfer
before activating autonomous economic production.

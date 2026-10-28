# Batch 24.23: C4.16 / B4.2A

## Truth boundaries

B4.0 remains the unmodified source of durable personal IDs and the 204 occupation totals. C4.15 remains the initial spatial allocator; assignment distances are Manhattan proxies, **not verified routes**. C4.16 changes the physical layout and route requests only. It neither proves that all reserved buildings were built nor grants the simulated economy a new production ledger.

## Spatial contract

Retain lot ID, householdReservationIds, workplaceReservationId, nominal street-cell grid and capped physical chunk painter. Width/depth and centering may vary per lot; keep building entirely inside its reserved cell(s). Green pockets are separate features with zero worker slots and no additional population. Worldgen reads green features alongside ordinary buildings but never treats them as a workplace.

Long country roads must have a painter gate in `touchesSite` for *empty chunks*, not just chunks containing a lot. Road paint occurs before buildings, with building footprints masked out. Roads still reject water/coastal failures. Any future universal navigation graph should consume the built roads, not assume that a planned edge survives terrain inspection.

## Behavioral contract

A projected worker's `MODELED_WORK_SHIFT` and `COMMUTING_TO_WORK` override the B4.1A.1 home's 48-block wander return. The same entity remains subject to Winterfell conversation lock priority. Route requests are limited to actually loaded chunks, move toward small waypoints, and have observable failure counters. A requested path is not a completed journey. Full daily travel, shift attendance and inventories are future work.

## Rollback

Installer snapshots four source files and all affected generated resources before mutating either. Avoid manually moving `C4_16_STAGE/updated` contents: bypasses version guards and backup. The rollback script requires the timestamped snapshot directory and restores only managed source/resource paths.

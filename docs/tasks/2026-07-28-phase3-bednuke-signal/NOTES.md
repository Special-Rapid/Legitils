# Phase 3 Research Notes

## Confirmed local observation paths

- `WorldClient.invalidateRegionAndSetBlock(BlockPos, IBlockState)` receives old
  world state and new server-applied state. It covers S23/S22 update handling,
  but its arguments contain no actor identity.
- `S25PacketBlockBreakAnim` contains `breakerId`, position and progress. Its
  handler has already applied the animation when a RETURN injection runs.
- Other players' dig packets are not delivered to this client. Client-side
  local destroy methods observe only this player's prediction, not another
  player's server-accepted action.

## Selected signal

Block state alone cannot establish a reliable breaker, and the product will
not infer one. The user selected an unassigned local notification instead of a
player-named alert. It uses no S25/packet handler observation.

## 3D defense-volume contract

Immediately before the first server-applied removal of a bed half, the adapter
captures a small, fully loaded cuboid around both halves. After both halves
become non-bed, the pure-Java check flood-fills OPEN blocks from all six faces
of that cuboid. The anomaly condition is that no path reaches either former
bed half after the settling window. A valid opening from any side suppresses
the alert.

## Required future correlation conditions

1. Both bed halves are inside a complete, bounded, currently loaded cuboid.
2. A server-applied bed-to-non-bed transition occurs within a short window.
3. After the settling window, no OPEN path from the cuboid exterior reaches
   either former bed half.
4. No world transition, chunk reload/large update, global lag, or missing data
   occurs.

`S21PacketChunkData` can update an already-loaded chunk without calling
`doPreChunk`. The `WorldClient.invalidateBlockReceiveRegion` adapter therefore
invalidates all BedNuke state whenever it receives a region larger than one
block; the ordinary one-block S22/S23 path remains observable.

For this conservative voxel test, a block is a sealed cell only when it both
blocks movement and reports a full cube. Partial or state-dependent collision
shapes are treated as OPEN, which can miss a signal but cannot turn an
ambiguous path into an alert.

If any condition fails, the outcome is no evidence and no notification.

## Newly observed block-in ambiguity

Replay observation on 2026-07-28 showed that a legitimate player can enter a
defense, place blocks to close the entrance, and then break the bed. The
current post-break flood-fill sees the same sealed final geometry as its target
condition and can therefore alert incorrectly. A recent placement-only
suppression would be safer than the current behaviour but is insufficient as a
final design because it can hide unrelated target-like events.

The next rule must require independent temporal evidence that no visible player
plausibly entered the volume while a route existed and remained inside or
adjacent through the break. This is an ambiguity guard only; it does not assign
the block break to that player. Missing position history or any uncertainty
means no alert. The full validation matrix is in the
[detector quality review](../2026-07-28-detection-quality-review/NOTES.md).

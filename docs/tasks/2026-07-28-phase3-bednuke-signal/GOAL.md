# Goal

## Objective

Implement conservative, unassigned BedNuke evidence only when a locally
observed bed destruction, a complete bounded 3D block snapshot, no open
exterior-to-bed route, and no plausible legitimate block-in occupancy history
all agree.

## Hard safety rule

Any incomplete history, chunk reload, delayed/multiple update ambiguity, world
transition, global lag condition, or plausible legitimate entry/block-in must
produce no alert. The evidence never infers or names a breaker.

## Current decision gate

The selected signal is an unassigned local-world anomaly. `WorldClient`
block-state updates are sufficient; no S25 handler or other packet observation
surface is permitted or required.

## Non-goals

- Packet cancellation, delay, replay, fabrication, mutation or sending.
- Inferring a breaker from proximity, facing, raycast or block removal.
- BedWars-specific assumptions without observed client evidence.

## Current quality status

The initial sealed-volume rule falsely flags a legitimate block-in: a player
can enter through an opening, close it, then destroy the bed from inside. The
rule is experimental until a temporal entry/occupancy ambiguity guard and the
controlled no-alert block-in test are complete. See the
[quality review](../2026-07-28-detection-quality-review/NOTES.md).

# Goal

## Objective

Replace NoSlow's wall-clock, sensitivity-preset rule with a clean-room,
Meowtils-compatible world-tick movement streak while correcting the reference
implementation's missing previous-position update.

## Contract

For every visible non-local player and each continuous world tick, compare
current horizontal position to the previous accepted position. While sprinting,
using an item, and not riding, increment a streak only when horizontal distance
is strictly greater than `0.05 * Speed-adjustment`. Reset on any other state.
Emit after the 21st consecutive exceeding tick, then reset only the streak.

## Non-goals

- Copying the reference source or inheriting its unmaintained previous-position
  bug.
- Team/friend filtering, WDR/reporting, blacklist, punishment, packets, input,
  or gameplay operations.

## Stop condition

Boundary, position-update, tick-discontinuity, missing-state, Speed, reset and
cooldown tests pass with Java 8 artifact verification. A normal item-use
movement trace in Lunar then determines default enablement.

# Plan

## Objective

Deliver a BedNuke signal that never alerts on incomplete local evidence or a
plausible legitimate block-in.

## Scope

- Bounded pure-Java 3D bed-volume and exterior-route state.
- A read-only `MixinWorldClient` block-transition adapter.
- Positive, normal-route, incomplete-snapshot, delayed-update and reset tests.

## Non-goals

- Any packet manipulation, input manipulation, breaker attribution,
  or server/proxy integration.

## Steps

1. Confirm client-visible block update and attribution limits. Completed.
2. Select the unassigned 3D local-world signal. Completed.
3. Implement the bounded history/correlation contract and WorldClient adapter.
4. Add temporal defense-entry/occupancy ambiguity handling without using it to
   attribute a breaker.
5. Add pure trace tests and inspect generated Mixin mappings.
6. Stop for Lunar manual verification, including block-in no-alert.

## Risks

- Bed halves, chunk updates, explosions and delayed state updates can make a
  seemingly simple block transition ambiguous.
- A final sealed defense is not sufficient evidence: legitimate block-in can
  create it. Missing player-position history must favour no alert.
- The detector must prefer false negatives whenever the client cannot form a
  complete loaded cuboid or a stable post-break state.

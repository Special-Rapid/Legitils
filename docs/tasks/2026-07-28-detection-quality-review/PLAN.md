# Plan

## Objective

Turn the currently observed false positives and missing attribution into
bounded, testable follow-up work without weakening the product's local-only
anti-cheat boundary.

The first implementation priority is a clean-room behavioural compatibility
rewrite for the four detector families that have Meowtils references.

## Scope

- Alert presentation wording and player-name resolution.
- AutoBlock and Legit Scaffold evidence quality.
- BedNuke block-in ambiguity and manual verification.
- Roadmap/specification status.

## Non-goals

- Modifying packets, inputs, reach, combat, or block breaking.
- Automatic reporting players or asserting that a detector result proves
  cheating. A later, explicitly approved user-clicked WDR control is scoped in
  `docs/tasks/2026-07-29-mock-flag-messages/`.
- Copying code from `sample/` into the product.

## Steps

1. Freeze the current four Meowtils-corresponding detector rules as known
   non-compatible baselines; do not tune their current millisecond thresholds.
2. Write a per-check behavioural contract from the reference's observable
   inputs, world-tick timing, threshold, cool-down and reset semantics.
3. Verify that the clean local observation adapter exposes every required
   input. If an input is unavailable, leave that detector disabled rather than
   substitute a looser proxy.
4. Implement one clean-room detector at a time with positive, normal, near
   miss, missing-state, world-reset and cool-down trace tests.
5. Replace alert prose only after an accepted evidence item has been mapped to
   a short presentation label and, where present, a currently observed display
   name.
6. Redesign BedNuke around temporal defense and local-position evidence, then
   run a controlled block-in no-alert test before treating it as releasable.

## Risks

- The local client often sees state changes but not the authoritative actor.
- A detector can be mechanically reproducible in synthetic tests yet still
  match normal Minecraft animation/state behaviour.
- The reference contains product-incompatible actions and at least one known
  faulty position-history detail. Behavioural compatibility must not import
  either one.
- Making BedNuke more conservative reduces recall; this is preferable to
  falsely accusing an ordinary block-in.

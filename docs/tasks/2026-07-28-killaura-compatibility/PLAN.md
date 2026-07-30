# Plan

## Objective

Make KillAura follow the supplied reference's observable world-tick contract
without adopting its product-incompatible outer handling.

## Scope

- `MixinMinecraft` and `PlayerSample` for explicit consumable and active
  attack-animation observations.
- `KillAuraSignalCheck`, `DetectionEngine`, and obsolete threshold plumbing.
- Pure-Java trace tests and the living detection documentation.

## Non-goals

- Any server interaction, reporting, player marker, team/friend policy, or
  modification of movement/combat/item use.

## Steps

1. Add explicit immutable observation fields for `ItemFood`/`ItemPotion`/
   `ItemBucketMilk` and `swingProgressInt > 0`.
2. Replace generic score decay with the six-tick use, 33-tick recent-use, and
   eight-VL state machine.
3. Test boundaries, discontinuities, missing visible players, global lag,
   reset, per-player isolation and post-alert clearing under Java 8.
4. Record the Lunar manual trace protocol without attempting unconsented or
   unpermitted test behavior.

## Risks

- `lastEatTick` has unintuitive reference semantics and must be tested at the
  precise end-of-use transition.
- Observation is advisory client-side evidence, not server truth.
- The current default enablement is not release proof; a normal-play false
  positive must disable it by default before release.

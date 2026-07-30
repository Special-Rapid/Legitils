# Plan

## Objective

Make LegitScaffold behaviourally compatible with the supplied Meowtils signal
without copying reference code or its external side effects.

## Scope

- POST-client-tick visible-player observation timing.
- `PlayerSample` world tick and exact swing-start input.
- LegitScaffold's bounded per-player tick state and tests.
- Default-enable safety and documentation.

## Non-goals

- Changes to AutoBlock, NoSlow, KillAura, BedNuke, packets, inputs, reports,
  player punishment, or gameplay state.

## Steps

1. Confirm 1.8.9 public field/method access and POST tick semantics.
2. Move player observation to one POST `runTick` injection and add world tick
   plus exact swing-start data to the immutable adapter sample.
3. Replace the detector with the defined tick state machine and fixed 60-tick
   cool-down; remove its old sensitivity threshold use.
4. Add boundary, missing-state, duplicate/gap tick, global-lag and reset tests.
5. Disable it in new/default configuration until private Lunar normal-bridge
   validation completes.

## Risks

- The POST injection changes the observed frame. It needs a Lunar smoke test.
- Paused worlds can repeat a world tick; duplicates must not become synthetic
  crouch timing.
- The reference tracks some state outside scaffold context. This product resets
  on missing/ambiguous observations and must document any intentional
  divergence.

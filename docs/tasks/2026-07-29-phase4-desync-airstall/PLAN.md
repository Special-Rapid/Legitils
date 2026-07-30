# Plan

## Objective

Add conservative visible-state evidence for Blink and Timer patterns while retaining the existing local-only EvidencePolicy
and 30-second Timer cooldown.

## Scope

- Extend the immutable `PlayerSample` contract with read-only support-state
  and nearby movement comparison inputs.
- Add pure-Java `CombatDesyncSignalCheck` and `AirStallSignalCheck` state.
- Extend the Minecraft post-world-tick adapter with conservative support,
  liquid, ladder, and nearby-player movement observations.
- Expose both existing detector IDs through default-disabled config and local
  `.legitils anticheat` commands.
- Add tests and update detection/product documentation.

## Non-goals

- No packet interception, automated action, Timer/Blink verdict, or direct
  identification of an F3+T reload.
- No BedNuke redesign in this task.
- No Companion implementation.

## Steps

1. Extend the sample contract so incomplete support/nearby evidence is
   represented explicitly rather than guessed.
2. Implement bounded, per-player pure-Java signals:
   - Blink: two repeated combat stall-and-snap episodes with a lower
     non-combat baseline and moving nearby comparison players.
   - Timer: sustained stationary airborne samples only where all local
     support checks are loaded and conclusively absent.
3. Collect the needed state in the existing `Minecraft.runTick` RETURN Mixin
   and reset all adapter history across world/tick discontinuities.
4. Mark the two IDs implemented but remove them from new/default settings.
5. Test positives plus normal movement, normal stationary combat, support,
   liquids/ladders/vehicles, incomplete state, global lag, and reset paths.
6. Build, inspect Mixin mapping/artifacts, review, then stop for manual Lunar
   verification.

## Risks

- Remote entities are interpolated client representations; the signals cannot
  prove Blink, Timer, or packet intent.
- Partial collision blocks and unloaded support must suppress Timer.
- Nearby movement is only a safe anti-global-freeze comparison, not server TPS
  telemetry.

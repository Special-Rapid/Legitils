# Summary

## Delivered

- Added default-disabled `Blink` and `Timer` detector choices. Existing
  `COMBAT_DESYNC` and `AIR_STALL` config identifiers remain internal/backward
  compatible; local commands and alerts use the new names.
- Blink requires two combat-correlated visible stall-and-snap episodes, each
  corroborated by nearby visible player movement. An ambiguous episode clears
  the sequence.
- Timer requires 40 continuous, unsupported, stationary mid-air samples with
  nearby visible player movement. Liquid, ladder, riding, support, unloaded
  support, global lag, and world/tick discontinuity suppress or reset it.
- A remote F3+T-like pattern remains an intended Timer advisory positive when
  the comparison and support observations are complete.

## Validation

- Added deterministic Phase 4 positive, negative, no-stitch, broad-stall,
  support, liquid, ladder, vehicle, incomplete-state, and reset traces.
- Added a coordinator-level Timer global-lag reset trace.
- Java 8 `:mod:test verifyBootstrapArtifacts` passed on 2026-07-29.
- Final review found no remaining blocker.

## Remaining manual gate

- Restart Lunar only to load the rebuilt JAR, then explicitly enable Blink/Timer without another restart and verify direct and local-proxy normal play before relying on the alerts.

# Summary

## Implemented

- Replaced the old sensitivity-preset AutoBlock rule with the clean-room
  Meowtils-compatible core condition: eleven consecutive client-tick samples
  where visible blocking and swing animation overlap.
- Removed the non-reference swing-edge condition and AutoBlock-specific preset
  thresholds.
- Kept the product safety reset for observation gaps, missing state and world
  reset.
- Disabled AutoBlock in newly created/default configuration while its
  normal-play validation gate is open.

## Compatibility boundary

The reference's outer violation counter, WDR/report button, sound, party
notifier and blacklist are intentionally not implemented. This MOD emits only
local advisory evidence, subject to its existing one-second presentation
cooldown.

## Existing configuration

An existing configuration that explicitly lists `AUTO_BLOCK` is intentionally
not rewritten. Remove `AUTO_BLOCK` from `enabledDetectors` and restart Lunar
to disable it immediately for that existing configuration.

## Verification

Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passed on 2026-07-28.
The remaining release gate is a controlled Lunar trace of ordinary sword
blocking while AutoBlock is explicitly enabled.

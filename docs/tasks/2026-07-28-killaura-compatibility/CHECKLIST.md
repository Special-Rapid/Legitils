# KillAura compatibility checklist

## Automated

- [x] Food, potion and milk bucket are accepted; non-consumables are not.
- [x] Use duration must be strictly greater than six ticks.
- [x] Attack animation must be active and a prior consumable-use end must be
  fewer than 33 ticks ago.
- [x] Eight violating ticks alert; other ticks decay VL by one.
- [x] Unreliable/global-lag, skipped/duplicate tick, world reset and
  missing-player frame discard partial state.
- [x] Per-player state remains isolated and bounded.
- [x] Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passes.

## Lunar manual gate

- [ ] Normal consumable use and ordinary combat create no unacceptable alert
  rate.
- [ ] A controlled and consented comparison trace is recorded before release.

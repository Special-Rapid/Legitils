# NoSlow Compatibility Check List

## Automated

- [x] Twenty high-distance ticks: no evidence; twenty-first: evidence.
- [x] Exact threshold, non-sprint, non-use and riding reset the streak.
- [x] Airborne state remains eligible, matching the reference contract.
- [x] Speed amplifier boundary and prior-position update are correct.
- [x] Duplicate/skipped tick, global lag, unavailable sample, world reset and
  missing-player frame discard partial state.
- [x] Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passes.

## Lunar manual gate

- [ ] Normal item-use movement produces no unacceptable NoSlow alert rate.
- [ ] A controlled/consented comparison trace is recorded before release.

# LegitScaffold Compatibility Check List

## Automated

- [x] World tick and exact swing-start adapter inputs compile against 1.8.9.
- [x] Exact positive trace emits once.
- [x] Crouch durations 0 and 3, inconsistent three-crouch history, early/late
  swing, no block, low pitch and airborne context do not emit.
- [x] Tick 59 is cool-down suppressed; tick 60 can emit again.
- [x] Duplicate/skipped tick, unavailable sample, global lag/observation
  discontinuity and world reset
  discard partial state.
- [x] LegitScaffold is absent from new/default enabled detectors.
- [x] Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passes.

## Lunar manual gate

- [ ] Lunar starts with no Mixin/refmap errors after the POST observation
  change.
- [ ] With LegitScaffold disabled by default, normal bridging emits no alert.
- [ ] In a private/consented ordinary-bridging trace with explicit opt-in,
  record alerts and retain screenshots/log lines.

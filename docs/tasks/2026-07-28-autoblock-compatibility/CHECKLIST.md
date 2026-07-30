# AutoBlock Compatibility Check List

## Automated

- [x] Ten consecutive `blocking && swinging` world-tick samples: no evidence.
- [x] Eleventh consecutive qualifying sample: one AutoBlock evidence item.
- [x] A non-blocking or non-swinging tick resets the count.
- [x] World reset, unavailable samples, and an observation gap discard partial
  state. The gap is the AutoBlock-specific global-lag safety boundary.
- [x] The detector emits again after each independent 11-tick sequence; the
  product's notification cooldown only suppresses presentation.
- [x] AutoBlock is absent from the default enabled-detector set while the
  normal-play validation gate is open.
- [x] Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passes.

## Lunar manual gate

- [ ] With AutoBlock disabled by default, ordinary play shows no AutoBlock
  output.
- [ ] In a private/consented trace, enable AutoBlock and record ordinary sword
  blocking plus attacks. If the 11-tick sequence flags normal play, keep the
  detector disabled by default.
- [ ] If ordinary play produces an AutoBlock alert, retain a screenshot or log
  line. It must remain local-only; no server action is sent.

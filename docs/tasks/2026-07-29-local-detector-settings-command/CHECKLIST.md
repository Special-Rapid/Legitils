# Local detector settings command checklist

## Automated

- [x] Only the documented `.legitils anticheat` forms are parsed locally.
- [x] Each detector and `all` can be persisted on/off with a revision increase.
- [x] Invalid detector names and future/unimplemented detectors are rejected.
- [x] A successful save applies only the detector set to the current session.
- [x] Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passes.

## Lunar manual gate

- [ ] `list` and `on/off` appear only locally and are not sent as server chat.
- [ ] A successful save immediately updates `status` to the expected active count without restart.

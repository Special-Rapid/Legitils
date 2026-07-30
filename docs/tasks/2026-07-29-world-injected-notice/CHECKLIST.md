# Injected notice checklist

## Automated

- [x] The fixed notice format is tested.
- [x] Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passes.

## Lunar manual gate

- [ ] Joining a world shows exactly one local `Legitils Injected!` notice.
- [ ] The notice is not visible as server chat.
- [ ] Changing worlds shows one new notice, without duplicates in one world.

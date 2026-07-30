# Local status command checklist

## Automated

- [x] `.legitils status` is handled and returns current status.
- [x] `.legitils` and unknown subcommands return local usage.
- [x] `/legitils status`, unknown dot text, and normal chat pass through.
- [x] A clickable server-chat command cannot enter the local namespace.
- [x] No command changes configuration or detector state.
- [x] Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passes.
- [x] The packaged refmap resolves the `GuiChat.keyTyped` manual-send redirect.

## Lunar manual gate

- [ ] Lunar completes a full restart with the new Mixin applied.
- [ ] `.legitils status` prints exactly one local response and is not seen as
  server chat.
- [ ] Normal chat and server `/` commands still work.

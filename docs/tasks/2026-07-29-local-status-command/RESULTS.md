# Local status command results

## Automated

- 2026-07-29: Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passed.
- The original `MixinGuiScreen.sendChatMessage` refmap was valid but Lunar
  still allowed manual input through, consistent with GuiScreen having loaded
  before Ichor registered the configuration. The implementation now redirects
  the later manual `GuiChat.keyTyped` one-argument send call; its generated
  refmap must resolve before Lunar smoke is accepted.
- Pure-Java tests cover status, usage, pass-through and unavailable-status
  behavior, including exclusion of clickable server-chat command input.
- Static review confirms the replacement hook excludes the clickable
  two-argument RUN_COMMAND path, so WDR remains a normal user click.

## Lunar manual gate

- Pending: full Lunar restart / Mixin application smoke.
- Pending: `.legitils status` local-only output and normal-chat pass-through.

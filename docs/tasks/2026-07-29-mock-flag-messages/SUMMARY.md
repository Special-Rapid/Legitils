# Summary

## Delivered

- Replaced generic confidence/evidence prose with the supplied colour-coded
  flag format for current detector labels.
- Resolves attribution only against the current visible Minecraft player list;
  no display name is persisted in `Evidence`.
- Adds an independently clickable `[WDR]` sibling that runs exactly
  `/wdr <validated raw player name>` only after a deliberate user click.
- Keeps BedNuke and all missing/invalid attribution anonymous, without a WDR
  button.
- Keeps the optional Action Bar short and non-interactive.

## Validation

- `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home/bin:$PATH ./gradlew :mod:test verifyBootstrapArtifacts` passed on 2026-07-29.
- Pure-Java tests cover exact formatter colours, valid/invalid WDR target
  handling, anonymous BedNuke, alert expiry/settings, and the local-command
  pass-through for a clicked `/wdr` command.
- Final review found no blocking implementation issue.

## Remaining manual gate

- In Lunar, verify one attributable flag's colours and that only `[WDR]` is
  clickable.
- On a server that supports it, confirm a click sends `/wdr <shown player>`
  and no command is sent before that click.
- Confirm BedNuke stays anonymous with no WDR component.

# Summary

## Implemented

- Moved visible-player sampling from `Minecraft.runTick` HEAD to RETURN so the
  detector observes the completed client-world tick used by the reference.
- Added immutable world-tick and exact swing-start (`swingProgressInt == 1`)
  adapter inputs.
- Replaced broad millisecond sneak-toggle scoring with the clean-room
  crouch-duration, three-history, swing-window and 60-world-tick state machine.
- Invalidated partial timing patterns on global lag, duplicate/skipped tick,
  unreliable sample, world transition, and a player missing from a complete
  visible frame.
- Kept the 60-tick LegitScaffold cooldown through partial observation resets;
  a world load still performs a complete reset.
- Disabled LegitScaffold and the separately experimental BedNuke in new/default
  configurations. Existing explicit configuration remains opt-in.

## Verification

Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passed on 2026-07-28.
Final review found no remaining blocking static issue.

## Remaining manual gate

Lunar must start without Mixin errors after the POST injection change, and an
ordinary private/consented bridging trace must be captured before considering
default enablement. See [MANUAL-TEST.md](MANUAL-TEST.md).

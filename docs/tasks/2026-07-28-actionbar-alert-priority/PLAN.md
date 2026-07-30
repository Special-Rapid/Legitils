# Plan

## Objective

Make Action Bar output alert-only and add an on-demand local status command.

## Scope

- Local alert presentation and its unit tests.
- The existing `GuiIngame` rendering mixin.
- Current product documentation that promises persistent Action Bar status.

## Non-goals

- Altering server packets or server messages.
- Modifying alert detection, cool-down values, or configuration format.
- Restoring a persistent HUD status.

## Steps

1. Confirm the 1.8.9 client-side command registration path.
2. Make idle alert presentation empty and retain only active alert Action Bar text.
3. Default notifications to chat, and render the optional MOD Action Bar alert
   only when the existing server Action Bar is inactive.
4. Record the in-game status-command idea as deferred because Lunar's obfuscated
   Forge runtime cannot safely consume the ForgeGradle command API.
5. Update tests, documentation, and run the Java 8 artifact gate.

## Risks

- A mapping/signature mismatch can cause the GUI chat mixin to fail at Lunar startup.
- Lunar injects into the server Action Bar draw call. Redirecting or replacing
  that call prevents Lunar's Mixin from applying and crashes startup, so the
  MOD must leave it untouched and give server text priority.
- Long combined text may be clipped by the vanilla viewport, but it remains advisory-only.

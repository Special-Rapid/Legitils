# Plan

## Objective

Replace generic confidence prose with the mock-up's local flag presentation,
while preserving evidence-policy filtering and safe anonymous behavior.

## Scope

- Current visible-player display lookup at chat-render time.
- A pure-Java alert formatter with exact legacy colour strings.
- Structured chat presentation fields for an optional WDR target.
- Mixin-only Minecraft click component construction and policy/docs/tests.

## Non-goals

- Any detector signal tuning, team inference beyond server-provided display
  metadata, or report behavior other than a user click on one validated target.

## Steps

1. Resolve exact 1.8.9 display-name and click-component APIs.
2. Carry only `Evidence.playerId` through the local presentation, then resolve
   it against the current visible-player list at chat-render time without
   storing player names in `Evidence`.
3. Format accepted attributable/anonymous alerts and expose only a validated
   WDR target to the chat Mixin.
4. Build the clickable sibling in the Mixin; keep Action Bar non-interactive.
5. Add exact-string, target validation, anonymous, lifecycle and Java 8 tests.
6. Run Lunar visual and manual-click smoke.

## Risks

- Server-provided display text may not include a Bed Wars team prefix outside
  an active match, so it must be displayed as supplied rather than fabricated.
- WDR is an intentional manual server command: only a click event with a
  validated target may create it.
- Long mock-up chat text is unsuitable for the Action Bar; that surface needs
  a plain non-interactive fallback.

# Goal

## Objective

Render accepted local anti-cheat alerts in the supplied `docs/mock-up` chat
format, with current visible-player attribution and a user-clicked `[WDR]`
button for attributable alerts.

## Approved interaction

- The local chat message follows the supplied legacy-colour format:
  `[Legitils] <team-formatted name> flagged <detector> | [WDR]`.
- `[WDR]` is clickable only when a current, validated Minecraft player name is
  available. A deliberate user click runs exactly `/wdr <name>`.
- No alert automatically sends a command, report, or packet. The MOD never
  chooses the action or clicks for the user.

## Safety invariants

- BedNuke remains anonymous because local block-state evidence has no reliable
  breaker identity. It has no WDR button.
- Missing/stale/invalid player metadata falls back to an anonymous local flag;
  it must not show a UUID, guess a name, or produce a command target.
- The WDR target is the validated raw player name, never formatted display
  text. The button is chat-only; Action Bar remains non-interactive.

## Non-goals

- Automatic reports, click automation, report queues, report history,
  punishments, blacklists, server-message modification, or detector changes.

## Stop condition

Exact formatter, metadata fallback, anonymous BedNuke, and command-target
tests pass with Java 8 artifact verification. Lunar manual smoke proves
colours, local attribution, and a user-clicked WDR action without automatic
command output.

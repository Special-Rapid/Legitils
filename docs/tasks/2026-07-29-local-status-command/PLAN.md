# Plan

## Objective

Intercept only recognised dot-prefixed local commands at the final client chat
send entrypoint, before the outgoing packet is constructed.

## Scope

- A Java-only parser/dispatcher for `.legitils status`.
- A bootstrap bridge for the existing status string.
- A `GuiChat.keyTyped` redirect at the one-argument manual-send callsite and
  its Mixin config entry.
- Unit tests and a Lunar manual smoke checklist.

## Non-goals

- Forge `ClientCommandHandler`, slash-command interception, or server-side
  command handling.

## Steps

1. Parse only the exact local prefix and return `null` for pass-through text.
2. Return local status or usage text without mutating configuration.
3. Redirect only the manual `GuiChat.keyTyped` one-argument send call and
   consume a handled command before Forge command routing and
   `C01PacketChatMessage` construction. Keep the clickable two-argument
   RUN_COMMAND path untouched.
4. Verify Java 8 output, then restart Lunar and prove local-only behavior.

## Risks

- Lunar/Ichor must apply the new GuiChat Mixin; static ForgeGradle
  compilation alone cannot prove that.
- A slash prefix could swallow a server command, so this command deliberately
  uses `.legitils` and unknown dot text remains pass-through.

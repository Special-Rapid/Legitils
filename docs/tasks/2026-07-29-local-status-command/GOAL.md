# Goal

## Objective

Provide an on-demand local status command that works in Lunar without Forge's
runtime `ClientCommandHandler` API.

## Command contract

- `.legitils status` displays the current local anti-cheat status in client
  chat.
- `.legitils` and an unrecognised `.legitils` subcommand display concise local
  usage.
- A handled local command is consumed at the user-entered `GuiChat` Enter
  callsite before the one-argument chat send path runs; it never creates,
  sends, cancels, delays, replays, or fabricates a packet.
- All other chat text, including unknown dot-prefixed text and every `/`
  command, passes through unchanged.
- Clickable server-chat `RUN_COMMAND` input is not user chat entry and always
  passes through unchanged because it uses the separate two-argument send path.

## Non-goals

- Server command registration, packet manipulation, configuration changes,
  gameplay operations, or a persistent HUD.
- Reintroducing `ClientCommandHandler`.

## Approved narrow exception

This user-entered local diagnostic command is the sole permitted input-path
consumption. It has no packet object to cancel and may only return status or
usage text. It must not expand into automatic/synthetic chat, server commands,
or settings/gameplay controls.

## Stop condition

Pure-Java parser/dispatch tests and Java 8 artifact verification pass. A Lunar
restart smoke must then prove the Mixin applies and a local command neither
crashes nor reaches server chat.

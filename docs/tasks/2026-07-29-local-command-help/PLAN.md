# Plan

## Objective

Make the local command interface discoverable and readable in Minecraft chat.

## Scope

- Local command parser and response presentation
- Manual-chat Mixin rendering
- Command tests

## Non-goals

- No server command is sent.
- No detector or configuration behavior changes.
- No changes to the WDR click action.

## Steps

1. Parse `.legitils help` in the existing local-only command namespace.
2. Return colour-coded, short help lines instead of a long one-line usage string.
3. Render each local response as its own Minecraft chat component.
4. Test parsing, formatting, and normal-chat pass-through under Java 8.

## Risks

Chat redirection must stay limited to manually typed `.legitils` input; clickable and regular server commands must not enter the local handler.

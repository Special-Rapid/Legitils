# Plan

## Objective

Resolve the current silent Nick/NoBreakDelay runtime failure modes without
loosening remote anti-cheat evidence requirements.

## Scope

- Nick name-tag Mixin and session-only marker state.
- Development-only `PlayerControllerMP` observer diagnostics.
- Focused detector/coordinator tests and the manual Lunar validation notes.

## Non-goals

- Changing remote NoBreakDelay thresholds or turning packet observation into
  an unverified remote player verdict.
- Reusing Meowtils code or altering any gameplay/network packet.

## Steps

1. Replace the fragile renderer-local-variable marker hook with
   `EntityPlayer#getDisplayName` return composition.
2. Remove the controller's shadowed Minecraft field and use the canonical
   Minecraft singleton, then add a one-time development hook diagnostic.
3. Log the first UUID-v1 Nick session observation to prove whether detection
   or rendering is the missing stage.
4. Match the reference mod's concrete EntityPlayer/Tab Mixin declarations so
   unsupported silent optional injections cannot hide a visible-marker failure.
5. Add regression tests and run the Java 8 build gate.
6. Record the exact restart/manual checks needed to diagnose any remaining
   cheat implementation that does not modify the vanilla controller delay.

## Risks

- Meowtils is currently loaded in the same Lunar session and patches the
  same Tab/controller classes; manual verification must distinguish its
  messages from Legitils' own `[NICK]` suffix and `[HypixelLegitils]` logs.
- Some NoBreakDelay clients may bypass the vanilla controller entirely. This
  task will make that distinction observable rather than infer a remote
  verdict from incomplete data.

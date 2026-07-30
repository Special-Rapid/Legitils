# Summary

## Result

- Replaced the fragile `RendererLivingEntity.renderName` local-variable hook
  with an `EntityPlayer#getDisplayName` return hook. This is the same stable
  name-tag composition point used by the compatible reference mod.
- Kept the existing Tab marker and added terminal-only proof for the first
  UUID-v1 Nick observation and first actual marker rendering. Neither emits a
  Nick chat notification or writes nick data to the Blacklist.
- Changed the development-only NoBreakDelay controller adapter to obtain the
  live Minecraft instance directly rather than relying on its shadowed field.
  The first real mining call now logs a terminal-only hook proof.
- Added the regression that a zero delay alone cannot flag without a recorded
  completed break.
- Runtime logs proved UUID-v1 Nick detection was working while the optional
  display injections were silent. The EntityPlayer and Tab declarations now
  match the reference mod's concrete Mixin syntax and require a real target
  instead of silently skipping it.

## Verification

- Java 8: `./gradlew :mod:test verifyBootstrapArtifacts` passed.
- The generated JAR contains `MixinEntityPlayer.class` and no longer packages
  `MixinRendererLivingEntity.class`.
- Review found no blocking issue. Lunar runtime hook order is intentionally a
  manual gate because both Meowtils and Legitils patch display names and the
  controller path.

## Manual gate

1. Fully quit Lunar, then launch again with the existing Legitils JVM option.
2. Run `.l nickdetect on` and `.l dev on`; confirm both in `.l status`.
3. Join a world with a known nick. In the terminal log, expect
   `Nick session marker observed for ...` and then
   `Marker render hook observed: [NICK]`; check Tab and the name tag.
4. Break one normal survival block. The terminal must show
   `NoBreakDelay development controller hook observed.` once.
5. Test the target NoBreakDelay client. If the hook line appears but no flag
   follows, that implementation does not make the vanilla `blockHitDelay`
   become zero; it needs a separate, packet-level dev observation design.
6. Press F3+T during mining as a control: it must remain unflagged.

## Remaining risk

When Meowtils remains installed, both mods compose `getDisplayName`; suffix
order can differ, but the review found no recursion or crash path. Test the
combined display once before treating the visual order as fixed.

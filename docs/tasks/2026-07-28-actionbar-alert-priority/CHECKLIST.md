# Checklist

## Automatic

- [x] Idle presentation has no MOD Action Bar text.
- [x] Active overlay alert retains its local chat notification.
- [x] Default configuration enables chat and disables the optional Action Bar alert.
- [x] Java 8 tests pass.
- [x] Bootstrap artifact gate passes.

## Lunar manual

- [x] Lunar starts without a Mixin application error. (Confirmed by user 2026-07-28 after removing the incompatible Forge command registration.)
- [ ] Idle server Action Bar remains visible without a MOD status line.
- [x] Replace the deferred Forge command route with the separately scoped
  `.legitils status` GUI command adapter; Lunar validation remains tracked in
  the [local command task](../2026-07-29-local-status-command/MANUAL-TEST.md).
- [ ] With the optional Action Bar alert enabled, a genuine local alert appears only while no server Action Bar is active.
- [ ] An active server Action Bar has priority without a crash or overlapping MOD text.

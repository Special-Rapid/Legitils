# Goal

Make manually typed `.legitils anticheat on/off` update the active local
detector set immediately, without a Lunar restart.

## Safety invariants

- Only the detector enable set changes at runtime; sensitivity, notification,
  cooldown, Mixin, loader, and all other settings remain restart-owned.
- The saved JSON configuration and active in-memory configuration advance
  together only after a successful atomic write.
- Every detector pattern is reset after a successful runtime toggle, so no
  alert can combine samples observed before and after the setting change.
- Normal chat, WDR, gameplay, packets, and inputs remain unchanged.

## Stop conditions

- `status` and `list` immediately reflect a successful on/off toggle.
- A failed or conflicted write leaves the active configuration unchanged.
- Java 8 tests and Bootstrap artifact verification pass.

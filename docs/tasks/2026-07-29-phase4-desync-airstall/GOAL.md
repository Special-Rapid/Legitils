# Goal

Deliver two observation-only, local advisory detectors for Minecraft 1.8.9 Lunar:

- `Blink`: repeated combat-correlated visible stall-and-resume episodes,
  often externally similar to Blink or a resource-reload stall.
- `Timer`: sustained, unsupported visible mid-air stationarity, often
  externally similar to Timer/Fly or a remote player's F3+T reload.

## Product decisions

- The detectors report observed patterns, never a cheat verdict.
- A remote player's F3+T-like air-stall pattern is an intended `Timer`
  positive signal, not a false positive to filter out.
- A stall of the observing local client, global lag, a world transition,
  missing samples, or uncertain support state suppresses/reset detection.
- Both detectors are default-disabled and take effect immediately after a successful `.legitils anticheat on` toggle.

## Stop conditions

- Both detectors have deterministic positive, negative, global-lag, missing
  state, and reset traces.
- No gameplay/input/packet behavior is introduced.
- Java 8 tests and Bootstrap artifact verification pass.
- Lunar manual smoke remains an explicit release gate.

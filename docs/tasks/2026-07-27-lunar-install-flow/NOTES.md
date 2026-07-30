# Decisions

- Ship all seven advisory anti-cheat detectors in the first release.
- Build a clean, first-party Java Agent loader as a separate product; do not redistribute, fork, or depend on the third-party lunar-agent.
- Build the Companion as a native macOS 13+ application and ship it Developer ID signed and notarized from the first public release.
- Expose only detector enablement, notification choices, sensitivity presets, and cool-downs in the Companion.
- Every Companion change affecting MOD behaviour requires a full Lunar restart. There is no live reload, process attachment, or running-client injection.
- Action Bar status is always displayed. Normal alerts use a one-second re-alert cool-down; air-stall (Timer-like) alone uses thirty seconds.
- Do not persist player identities, raw evidence, packet data, or alert history. Detection calibration and false-positive traces are developer acceptance tests, not user settings.

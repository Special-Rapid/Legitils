# Hypixel Legitils Companion for macOS

## Product role

Hypixel Legitils Companion is the native macOS control application for the in-game anti-cheat MOD. It is not a second anti-cheat engine and it never interacts with the running Minecraft process. Its responsibilities are installation, repair, updates, configuration editing, and local status presentation.

The first target is macOS 13 or later. Build it as a SwiftUI app in a separate `apps/macos-companion/` Swift Package Manager package. The public distribution is Developer ID signed and notarized from the first release.

## User-facing screens

| Screen | Purpose | Main actions |
| --- | --- | --- |
| **Overview** | Explain whether the MOD installation and settings are valid. | Install/Repair, Copy Lunar setup, Open settings folder. |
| **Detectors** | Enable/disable each advisory detector and select a validated sensitivity preset. | Toggle detector, choose Conservative/Balanced/Sensitive, restore recommended defaults. |
| **Alerts** | Select supplementary local notification behaviour. | Chat message, sound, optional alert-only Action Bar notification. |
| **Advanced** | Expose safe diagnostic controls without gameplay settings. | Local debug mode, reset local config, export configuration diagnostics. |
| **Updates** | Show the installed MOD/Companion versions and prepare a manual update. | Check package validity, replace the stable MOD JAR after user confirmation. |

The app must clearly label every detector as advisory. It cannot expose reach, movement, combat, packet, target-selection, camera, or automation controls because the MOD has no such capabilities.

## First-release defaults

- All eight planned detectors start enabled at the **Balanced** sensitivity preset once their detector phases are released; the Companion must not expose a not-yet-implemented detector as active.
- Local chat is enabled by default; sound and the optional Action Bar notification are disabled by default.
- The Action Bar is not persistent. When the optional Action Bar notification is enabled, a new alert shows advisory anomaly text only while the server has no active Action Bar; Lunar owns that server draw path. The Companion provides the on-demand local configuration summary.
- The default re-alert cool-down is 1 second per detector/player. Air-stall (Timer-like) alone uses a 30-second cool-down because it is the lowest-confidence signal.
- No alert/evidence history is persisted after Lunar closes. The Companion shows configuration and installation state, not a player watchlist.
- Controlled false-positive traces (normal bridging, F3+T/resource reload, global lag, and proxy-induced delay) are developer release tests and are not exposed as user controls.

## Shared configuration contract

The Companion and MOD communicate through a versioned JSON file, not a shared Java/Swift library and not IPC:

```text
~/Library/Application Support/HypixelLegitils/
  config.json                 # Companion writes; MOD reads
  runtime-status.json         # MOD writes; Companion reads
  hypixel-legitils.jar        # installed MOD artifact
  loader-config.json          # generated loader configuration
```

`config.json` contains only:

- schema version and monotonically increasing configuration revision;
- enabled detector set;
- alert modality and local cool-downs;
- Blacklist display/threshold settings, Nick-detection setting, alert channels, and development self-detect setting;
- sensitivity presets (`conservative`, `balanced`, `sensitive`) that map to validated internal threshold ranges;
- local debug mode.

The Companion and the narrow command settings write a complete replacement file to a temporary sibling path, validate it against the documented schema, then atomically replace `config.json`. Companion changes are marked **Restart required**. `.legitils anticheat on/off`, `.legitils nickdetect on/off`, `.legitils notify <channel> on/off`, `.legitils dev on/off`, and `.legitils blacklist on/off/threshold` are narrow exceptions: after a successful write, they immediately apply only the enabled-detector set, Nick display setting (clearing existing Nick markers when disabled), alert-channel setting, self-observation switch, or Blacklist display/threshold state respectively. The MOD otherwise reads and validates configuration only at startup, keeps its last valid immutable configuration if a file is incomplete, malformed, unsupported, or outside the allowed range, and writes only its installed version plus the startup-loaded revision to `runtime-status.json`.

Neither file contains raw packets, chat text, account tokens, player UUIDs, player names, evidence traces, or automatic-report data.

## Installation boundary

The Companion may copy the MOD and first-party loader into its Application Support directory, generate `loader-config.json`, validate file hashes/JAR contents, and copy the required Lunar JVM argument. It must not modify Lunar's settings database, patch launcher/client files, attach to a process, or offer an Inject button.

The first-party loader is part of this product but remains a technical release gate until it passes its independent Lunar Client 1.8.9 compatibility tests.

## Validation

1. Test config migration/defaulting and atomic replacement without Minecraft.
2. Test malformed/out-of-range config rejection while preserving the MOD's prior configuration.
3. Test that Companion and non-detector settings changed during a run are marked Restart required and only acknowledged after the next clean MOD start; separately test that local detector on/off applies immediately after a successful write.
4. Test rollback, uninstall, and both direct-Hypixel/local-proxy connection paths with the same configuration.

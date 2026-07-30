# Lunar Manual Test — Ordinary Bridging Only

## Purpose

Confirm that the tick-accurate LegitScaffold signal starts correctly in Lunar
and does not alert during ordinary bridging. Do not use a cheat client or
automated input for this test.

## Preparation

1. Fully quit Lunar Client.
2. Confirm Lunar still has the current project loader JVM argument.
3. For an isolated opt-in test, create or temporarily replace:

   `~/Library/Application Support/HypixelLegitils/config.json`

   ```json
   {
     "schemaVersion": 1,
     "revision": 1,
     "enabledDetectors": ["LEGIT_SCAFFOLD"],
     "sensitivity": "balanced",
     "notifications": { "chat": true, "overlay": false, "sound": false },
     "cooldowns": { "normalMillis": 1000, "airStallMillis": 30000 },
     "debug": false
   }
   ```

   Preserve any existing configuration before replacing it. Configuration is
   read only at startup, so every edit requires a full Lunar restart.
4. Launch Lunar 1.8.9. In `latest.log`, confirm the normal bootstrap and
   visible-player-observation lines, and confirm no Mixin/refmap error appears.

## Test and decision

1. Join a private/consented world or replay.
2. Observe several ordinary bridging styles, including natural crouch-assisted
   bridging, with no modified client or automated input.
3. Record each run in [RESULTS.md](RESULTS.md). Keep a screenshot or matching
   `latest.log` line for every alert.

- Any LegitScaffold alert during ordinary bridging keeps the detector disabled
  by default; record the context rather than assuming a player cheated.
- Several no-alert runs are encouraging but require more contexts before a
  default-enable decision.

## Rollback

Remove `LEGIT_SCAFFOLD` from `enabledDetectors` (or restore the saved config),
fully restart Lunar, and confirm no LegitScaffold notification appears. The JVM
argument does not change for this configuration-only rollback.

# Lunar Manual Test — Normal Sword Blocking Only

## Purpose

Determine whether the Meowtils-compatible 11-tick AutoBlock signal falsely
flags ordinary sword blocking. This test must use only normal gameplay; do not
install or run a cheat client.

## Preparation

1. Fully quit Lunar Client.
2. Confirm the current loader JVM argument still points at the newly built
   loader JAR.
3. Create or temporarily replace this file:

   `~/Library/Application Support/HypixelLegitils/config.json`

   with the following isolated test configuration:

   ```json
   {
     "schemaVersion": 1,
     "revision": 1,
     "enabledDetectors": ["AUTO_BLOCK"],
     "sensitivity": "balanced",
     "notifications": {
       "chat": true,
       "overlay": false,
       "sound": false
     },
     "cooldowns": {
       "normalMillis": 1000,
       "airStallMillis": 30000
     },
     "debug": false
   }
   ```

   Keep a copy of any existing configuration before replacing it. The MOD reads
   this file only at startup, so every edit requires a full Lunar restart.
4. Start Lunar Minecraft 1.8.9 and confirm the normal bootstrap log with
   `configuration loaded`.

## Test

1. Join a private/consented world or replay where normal sword blocking can be
   observed.
2. Perform ordinary sword blocking and normal attacks for several short and
   sustained exchanges. Do not use a modified client or automated input.
3. Watch local chat and retain a screenshot or the matching lines from Lunar's
   `latest.log` if an AutoBlock notification appears.
4. Record each run in [RESULTS.md](RESULTS.md), including a no-alert result.

## Decision

- **Any AutoBlock alert during ordinary play:** the detector is not
  distinguishable from normal sword blocking under this observable contract.
  Keep it disabled by default and attach the screenshot/log to this task.
- **No alert:** record the approximate duration and scenario. This is useful
  evidence but does not yet prove universal safety; repeat across more than
  one normal combat situation before considering default enablement.

## Rollback

Remove `AUTO_BLOCK` from `enabledDetectors` (or remove the temporary config
file if there was no prior file), fully restart Lunar, and confirm no AutoBlock
messages are emitted. The JVM argument does not need to change for this
configuration-only rollback.

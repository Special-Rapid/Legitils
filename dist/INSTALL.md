# Hypixel Legitils bootstrap alpha

This alpha validates a pre-launch Java Agent and a minimal observation-only Mixin bootstrap. It does not establish production compatibility or detector accuracy.

1. Put `hypixel-legitils-loader-<version>.jar`, `hypixel-legitils-<version>.jar`, and a generated `loader-config.json` in a permanent directory outside this source checkout.
2. Replace the `modJar` placeholder in `loader-config.json` with the absolute MOD JAR path.
3. In Lunar Client, open **Settings → Game Settings → JVM Arguments** (the Advanced field shown in the launcher) and add exactly one JVM argument:

   ```text
   -javaagent:/absolute/path/hypixel-legitils-loader-<version>.jar=/absolute/path/loader-config.json
   ```

   For a source-checkout smoke test, build with Java 8 first and print the exact local argument instead of transcribing paths:

   ```text
   ./gradlew :mod:printLunarSmokeArgument
   ```

4. Fully quit and relaunch Lunar 1.8.9. A successful bootstrap smoke has a successful loader `status=...` diagnostic before Minecraft starts, plus `Bootstrap Mixin reached Minecraft.startGame; ...` in the active 1.8 profile log (for example, `~/.lunarclient/profiles/1.8/logs/latest.log`). Entering and leaving a local world should produce `World lifecycle reset.` without a crash. A different profile name can use a different `profiles/<name>/logs/latest.log` path. New configurations intentionally have all detector signals and the Action Bar disabled, so no persistent overlay or alert is expected from this smoke alone. If the status, bootstrap line, lifecycle line, or client stability is absent, remove the argument and do not treat this build as compatible.
5. To roll back, fully quit Lunar, remove that one JVM argument, and delete the current Ichor class cache before restarting Lunar. For the current macOS Lunar 1.8 profile, the observed cache is:

   ```text
   ~/.lunarclient/offline/multiver/cache/72c13d03/6b7680a4/bake.zip
   ```

   Lunar will rebuild this cache on the next launch. The exact hash directories can change after a Lunar update; use the current 1.8 `ichor-boot.log` to identify the active `cache/.../bake.zip` path. The Hypixel Legitils log lines must no longer appear after the clean restart.

This bootstrap test validates initialisation only. Detector validation is separate; packet, input, proxy, and gameplay-modifying functionality are out of scope.
Do not use Lunar's Pre-Launch Command or Wrapper Command fields, and do not use runtime injection.

# Installation and Update Plan

## Decision

Hypixel Legitils is delivered as a native macOS Companion app, a clean first-party Java Agent loader, and an in-game anti-cheat MOD. The Companion is the normal user entry point; it owns installation, repair, update discovery, configuration editing/validation, and recovery instructions. The loader starts the MOD before Minecraft, and the MOD owns only in-game observation and local alerts.

The first alpha may expose the same flow as manual steps while the Companion is under development. Neither route patches Lunar Client files, modifies launcher settings automatically, injects into a running Minecraft process, or depends on the local proxy.

The loader is a clean-room, first-party Java Agent with its own source, license, tests, and release artifact. It must be compiled as Java 8 bytecode, contain only the narrow loading/Mixin-registration responsibility, and never copy, include, or depend on the third-party `curxxed-mc/lunar-agent` source or binary.

## Loading approaches

| Approach | Fit for anti-cheat | Decision |
| --- | --- | --- |
| First-party pre-launch Java Agent configured through Lunar JVM arguments | MOD starts before Minecraft and has a repeatable, reversible lifecycle. | required loading path |
| Native macOS Companion | Can prepare the directory/configuration, edit safe MOD settings, and present the one required launch argument without changing the running client. | required product UI |
| External desktop overlay | Can display independently collected information, but cannot safely obtain the client-world observations required by these checks. | not an anti-cheat loading path |
| Attach/inject into an already-running Minecraft process | Requires post-launch process manipulation, has no stable client lifecycle, and is likely to break with client/runtime updates or conflict with client protections. | explicitly rejected |

The reference screenshot's `Inject` label only shows an application UI; it does not establish how or whether that application loads code safely. Hypixel Legitils will not implement, document, or require runtime process injection.

## Companion application scope

The first Companion screen should have four visible sections:

| Section | User action | Result |
| --- | --- | --- |
| **Status** | Open the app | Shows installed MOD version, configuration validity, selected runtime directory, and clear repair state. |
| **Install / Repair** | Choose the permanent directory and confirm | Copies the MOD and loader, generates `loader-config.json`, and validates expected JAR entries. |
| **Lunar setup** | Press Copy | Shows the complete JVM argument and copies it for the user to paste once in Lunar Advanced Settings. |
| **Update / Remove** | Check or remove | Replaces the stable MOD JAR, or shows the exact one-line rollback and directory removal steps. |

The app may inspect only its own files and configuration. It must not inspect Minecraft memory, attach to a process, start Minecraft, alter Lunar files/settings, control the local proxy, send telemetry, or implement gameplay features. It targets macOS 13+ and the public app is Developer ID signed and notarized.

## Supported connection paths

Install the MOD once, then choose either server address in Minecraft:

```text
Direct:  Lunar Client + Hypixel Legitils -> mc.hypixel.net
Proxied: Lunar Client + Hypixel Legitils -> 127.0.0.1:<proxy-port> -> mc.hypixel.net
```

No installation step, MOD setting, or JVM argument changes between these paths. The local proxy is started separately only when the player chooses the proxied route.

## Alpha manual installation

### Preconditions

1. Lunar Client 1.8.9 launches successfully without Hypixel Legitils.
2. The first-party loader and MOD JAR are present in the selected permanent directory.
3. The first-party loader's Java-version compatibility with the Lunar runtime has been tested. Both the loader and MOD remain Java 8 bytecode.
4. The tester chooses a permanent directory without spaces or non-ASCII path components for this alpha, for example `~/hypixel-legitils-runtime` on macOS. Moving it later invalidates the absolute paths.

### Files

The permanent directory contains only the runtime inputs:

```text
hypixel-legitils-runtime/
  hypixel-legitils.jar
  hypixel-legitils-loader.jar
  loader-config.json              # generated for this exact directory
```

`loader-config.json` uses the installed absolute MOD path and never a path under `sample/`:

```json
{
  "schemaVersion": 1,
  "modJar": "/absolute/path/hypixel-legitils-runtime/hypixel-legitils.jar",
  "mixinConfig": "mixins.hypixellegitils.json",
  "injectedProperty": "hypixellegitils.agent.injected"
}
```

### Launch sequence

1. Close Lunar Client completely.
2. Copy the released `hypixel-legitils.jar` and `hypixel-legitils-loader.jar` to the permanent directory and create `loader-config.json` from the supplied template, replacing only the absolute MOD path.
3. In Lunar Client 1.8.9, open **Settings → Game Settings → JVM Arguments**. The target macOS Lunar build exposes this field in its Advanced section; this was visually confirmed on 2026-07-27. Do not use Pre-Launch Command or Wrapper Command for this product.
4. Add one JVM argument, substituting the two absolute paths:

   ```text
   -javaagent:/absolute/path/hypixel-legitils-runtime/hypixel-legitils-loader.jar=/absolute/path/hypixel-legitils-runtime/loader-config.json
   ```

5. Launch Lunar Client 1.8.9 and verify the local Hypixel Legitils startup indicator before joining a server.
6. Join either `mc.hypixel.net` or the local proxy address. The same detector configuration must be active in both cases.

If the indicator is absent or Lunar fails to launch, remove only this JVM argument and relaunch. This is the immediate rollback path.

## Updates and uninstall

### Update

1. Exit Lunar Client.
2. Replace `hypixel-legitils.jar` in the same permanent directory; keep its stable filename so `loader-config.json` does not change.
3. Start Lunar Client and verify the displayed MOD version and startup indicator.

There is no automatic updater, background downloader, or launcher patching. A loader update is a separate compatibility event and is not silently applied with a MOD update.

### Uninstall

1. Remove the Hypixel Legitils JVM argument from Lunar Client.
2. Launch Lunar once to verify it starts normally.
3. Delete the dedicated runtime directory if the user no longer needs it.

## Future installer and public-release gate

A first-party macOS Companion creates the dedicated directory, copies the MOD and loader JARs, generates the absolute-path JSON, validates the JAR contents, and displays a copyable JVM argument. It must not silently edit Lunar settings, patch Lunar files, install a proxy, upload data, or launch Minecraft.

Its user-facing flow can still be as simple as a Seraph-style companion application:

1. **Install / Repair**: choose or validate the permanent directory and generate `loader-config.json`.
2. **Copy Lunar argument**: display and copy the single JVM argument after showing the exact paths.
3. **Status**: show only local file/configuration validation and the installed MOD version.
4. **Uninstall instructions**: show the one argument to remove and the directory to delete.

It must not offer an `Inject` action or load code into a running Minecraft process. The public app is distributed as a Developer ID-signed and notarized macOS artifact; updates remain explicit user-confirmed replacements in the first release.

Do not ship that installer or a public release until all of the following are true:

1. The first-party loader is independently designed, implemented, and compatibility-tested without reference-source reuse.
2. Lunar Client 1.8.9 startup has been smoke-tested on macOS 13+ and its supported runtime Java version.
3. Both direct and local-proxy connection paths have passed the same detector and rollback checks.
4. The release archive has been checked to exclude `sample/`, Meowtils packages, Agent source, and unlicensed binaries.
5. The Companion app is Developer ID signed, notarized, and installed/tested from a fresh macOS user directory.

## Evidence basis and open compatibility question

The third-party Agent remains behavioural reference only; its JVM-argument/config shape does not authorize source reuse. Lunar Client's own current UI/API documentation and the first-party loader's runtime compatibility remain verification gates. Treat the manual route as an alpha test protocol, not a public compatibility promise, until the public-release gate passes.

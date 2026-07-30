# Legitils

Legitils is a clean-room, observation-only Minecraft 1.8.9 client-side advisory MOD for Lunar Client. It presents local signals about suspicious patterns; it does not change gameplay, automate player actions, or make definitive cheating claims.

> **Alpha / not release-ready.** The build and automated checks cover the loader and MOD artifacts, but compatibility with a real Lunar Client restart and normal gameplay has not yet been fully verified. Do not treat this repository as an endorsed anti-cheat, a cheat detector, or a production download.

## What it does

- starts a first-party Java 8 Agent loader before Lunar launches;
- registers a Mixin-based client MOD;
- observes local client state and presents advisory alerts locally;
- keeps configuration and observations local to the device.

The project deliberately excludes packet manipulation, synthetic input, automated reports or actions, combat or movement bypasses, ESP/free-look features, remote telemetry, and arbitrary extension loading. The detailed product boundary is in [the product scope](app-plan/product-scope.md).

## Status

The bootstrap, safe foundation, and several detector cores have automated coverage. Some detector behaviour is intentionally disabled by default or awaiting controlled normal-play traces. See [the current roadmap](app-plan/README.md#current-roadmap) and [the detection quality review](docs/tasks/2026-07-28-detection-quality-review/NOTES.md).

## Build

This legacy Minecraft 1.8.9 toolchain requires Java 8.

```sh
./gradlew build
```

The build verifies Java 8 bytecode, required manifests and Mixin resources, and that reference material is absent from generated artifacts. For the manual Lunar bootstrap procedure and rollback, see [the installation guide](dist/INSTALL.md).

## Repository contents

- `loader/` — standalone Java Agent loader
- `mod/` — Mixin-based observation MOD and tests
- `app-plan/` — product, architecture, and packaging decisions
- `docs/tasks/` — implementation records and validation checklists
- `dist/` — installation template and bootstrap instructions

The local `sample/` directory is intentionally ignored. It is reference-only material and is neither source code nor a redistributable dependency of Legitils.

## License

This project is released under the [MIT License](LICENSE).

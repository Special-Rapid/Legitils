# Build and Packaging

## Build decision gate

The bootstrap proof uses a new Gradle multi-project build with separate `loader` and `mod` artifacts. The exact, tested development combination is:

| Component | Selected version / source |
| --- | --- |
| Java compiler/runtime | Java 8 (class-file major version 52) |
| Gradle wrapper | 2.7 |
| ForgeGradle | `2.1-SNAPSHOT`, enforced SHA-256 `29f4f9a4b7ad917937d6ca761404ed4c56ee2a716cbfdd190b9aa99f25eb4695` |
| Minecraft / Forge | `1.8.9-11.15.1.2318-1.8.9` |
| MCP mappings | `stable_20`, as specified by the official Forge 1.8.9 MDK |
| Mixin | `org.spongepowered:mixin:0.7.11-SNAPSHOT` (2018-07-03 snapshot line), enforced SHA-256 `da3d6e47b9c12b5a312d89b67bc27e2429d823c09cde8a90299e9fdcc4eefc20` |

Mixin 0.8.x is deliberately not used with this ForgeGradle 2.1 runtime: it requires newer ASM classes that conflict with Forge 1.8.9's ASM 5 transformation stack. The selected 0.7.11 line registers and applies the minimal `Minecraft` Mixin in the documented LaunchWrapper development path.

The legacy toolchain has no immutable ForgeGradle 2.1 release coordinate. `verifyToolchainInputs` therefore makes the exact resolved ForgeGradle and Mixin JAR hashes a mandatory build/check gate; a changed SNAPSHOT cannot silently pass.

The proof must establish all of the following in a clean checkout:

1. Java 8-compatible compilation.
2. A development dependency set capable of compiling against Minecraft 1.8.9 client classes and Sponge Mixin annotations.
3. A JAR that contains only new project classes/resources and `mixins.hypixellegitils.json`.
4. Successful registration by the clean first-party loader without copying third-party Agent sources into the product.
5. A safe `Minecraft.startGame` bootstrap log/notification in a controlled local run.

The selection comes from the official Forge 1.8.9 MDK and the compatibility result above, not from the decompiled reference. The final game-window confirmation remains a manual macOS runtime gate because the automated development launch reaches Mixin application but cannot complete the host's AWT application registration.

## Artifact contract

The build emits:

```text
mod/build/libs/hypixel-legitils-<version>.jar
loader/build/libs/hypixel-legitils-loader-<version>.jar
dist/loader-config.example.json
dist/INSTALL.md
```

`loader-config.example.json` contains placeholders only:

```json
{
  "schemaVersion": 1,
  "modJar": "/absolute/path/to/hypixel-legitils.jar",
  "mixinConfig": "mixins.hypixellegitils.json",
  "injectedProperty": "hypixellegitils.agent.injected"
}
```

The project ships a clean, first-party loader as a separately built Java 8 Agent JAR. It must have no code or binary dependency on the investigated third-party Agent, and packaging checks must prove that its output does not contain `net/curxxed/dev/agent/` or any reference artifact. The public macOS Companion bundle contains only the first-party MOD, loader, configuration template, documentation, and required licensing information.

## Packaging checks

- fail the build if a `sample/` file is included in the output;
- fail the build if `wtf/tatp/meowtils/` or `net/curxxed/dev/agent/` is present in the output;
- inspect the output JAR for the new Mixin JSON and the expected bootstrap class;
- inspect the first-party loader JAR for Java 8 bytecode, its expected agent manifest, and only the documented loader/configuration classes;
- include license/attribution documentation for any dependency or copied asset. Prefer new source and no copied assets.
- verify `INSTALL.md` documents the manual rollback: remove one JVM argument, then restart Lunar Client;
- verify direct Hypixel and local-proxy paths use the exact same MOD JAR and generated configuration.

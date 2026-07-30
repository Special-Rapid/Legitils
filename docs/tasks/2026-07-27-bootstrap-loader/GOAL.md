# Goal

## Objective

Create a clean-room bootstrap proof for Hypixel Legitils: a separately built Java 8 loader plus a minimal Java 8 MOD JAR that can register only the project's startup/tick Mixin in Lunar Client 1.8.9.

## Stop conditions

- The exact build/Mixin dependency combination is documented and reproducible.
- The loader and MOD are new source artifacts with no `sample/`, Meowtils, or third-party Agent code/binary dependency.
- Static JAR-content checks pass.
- A controlled Lunar smoke-test procedure exists; actual runtime compatibility remains unconfirmed until the client is tested.

## Safety boundaries

- No runtime attach/injection into an already-running process.
- No packet interception, synthetic input, gameplay automation, or external network access.
- No copying from decompiled reference source.

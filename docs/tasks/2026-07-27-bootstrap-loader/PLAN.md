# Plan

## Objective

Establish the independently built Java 8 loader and minimal MOD bootstrap required before detector implementation.

## Scope

- New Gradle build and source/resource roots
- `loader/` Java Agent module
- `mod/` minimal Mixin bootstrap module
- JAR-content verification and controlled Lunar smoke-test instructions

## Non-goals

- No detector implementation beyond a local startup/tick proof.
- No Companion application implementation.
- No runtime attach, packet hooks, or client/proxy integration.

## Steps

1. Map the minimum clean-room loading and Mixin registration requirements.
2. Validate Java Agent, Mixin, and Minecraft 1.8.9 build compatibility from primary sources.
3. Implement a compatibility spike: a Java 8 loader and a no-feature, one-Mixin MOD pair.
4. Prove the Mixin in a documented Minecraft 1.8.9 development runtime, then test the same pair in Lunar.
5. Run unit/static packaging checks and record the exact controlled Lunar smoke-test result.

## Risks

- Lunar's class loader and runtime Java version may reject an otherwise valid Java 8 loader.
- Mixin registration can be version/classloader-sensitive.
- The reference Agent must remain behavioural reference only, including its class names and implementation.
- The product must not ship an unproven reflective classloader workaround if the Lunar compatibility spike fails.

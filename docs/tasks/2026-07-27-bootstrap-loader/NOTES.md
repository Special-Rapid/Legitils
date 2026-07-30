# Research Notes

## Confirmed constraints

- A Java 8 pre-launch agent needs `Premain-Class` and `premain(String, Instrumentation)`; retransformation/redefinition manifest permissions are not needed for the initial design.
- Mixin's documented Minecraft 1.8.9/LaunchWrapper path is `MixinTweaker` plus a manifest `MixinConfigs` entry, registered before a target class loads.
- Mixin does not document a general Java Instrumentation service that bridges a `premain` agent into a third-party client's existing Mixin classloader.
- Lunar Client's actual Mixin classloader, load timing, and acceptance of the first-party loader are therefore runtime gates, not assumptions that a local build can prove.
- The verified ForgeGradle 2.1 development pair is Forge 1.8.9 `11.15.1.2318` plus Mixin `0.7.11-SNAPSHOT`. Mixin 0.8.x requires newer ASM classes and conflicts with Forge 1.8.9's ASM 5 transformer stack.

## Loader compatibility spike

The first executable deliverable is an isolated compatibility spike, not the anti-cheat feature set:

1. Build the clean Java 8 loader with only `Premain-Class`, strict `loader-config.json` validation, and explicit local diagnostic output.
2. Build a minimal Java 8 MOD with one startup Mixin and no detection, networking, input, rendering, or block/entity changes.
3. Prove the Mixin first in a legal Minecraft 1.8.9 development runtime using the documented MixinTweaker path.
4. Test the same first-party loader/MOD pair in the intended Lunar 1.8.9 runtime by adding one explicit pre-launch JVM argument.
5. Continue to detector implementation only if the MOD emits its local one-time startup indication and the Mixin is confirmed before target-class load.

If step 4 fails, the product does not ship an unsupported loader workaround. Record the exact Lunar build/runtime behaviour and revisit the loading approach before writing detector or Companion code.

## Current evidence

- `:loader:test`, `:mod:jar`, `verifyToolchainInputs`, `verifyBootstrapArtifacts`, and normal `build` pass under Java 8.
- The generated refmap resolves `Minecraft.startGame` to `func_71384_a` and `runTick` to `func_71407_l`.
- The ForgeGradle development launch registered `mixins.hypixellegitils.json` and logged `Mixing MixinMinecraft ... into net.minecraft.client.Minecraft`.
- The same launch with `-PloaderDevSmoke` logged the first-party agent's `config-valid` and `mixin-config-registered` statuses before Mixin applied `MixinMinecraft`.
- The non-interactive macOS development process aborted while registering an AWT application before a game window could be verified. This was a development-runtime limitation, not Lunar evidence.
- Before the live smoke, the planned Lunar gate was: paste the documented JVM argument, fully restart Lunar 1.8.9, verify the bootstrap output, then remove the argument and verify rollback. This gate is now complete; the result is recorded below.
- `:mod:runClient -PloaderDevSmoke` is the controlled development equivalent. It generates an absolute-path config under `mod/build/` and adds the first-party loader through Java's normal pre-launch `-javaagent` option; it never attaches to an existing process.
- The local Lunar launcher persists a `jvmArgs` value using the same `-javaagent:<agent-JAR>=<config-JSON>` form. Its installed ARM64 runtime reports OpenJDK 17.0.3. This established the manual launcher entry point before the live smoke.
- The built loader was invoked directly with that installed OpenJDK 17.0.3 and the generated configuration. It reported `status=config-valid`; this established that the Java Agent entry point and strict configuration parser run on Lunar's installed Java version before Minecraft was launched.
- Current Lunar 1.8 profile logs show that the actual game runs on Azul OpenJDK 17.0.18 and initialises Sponge Mixin 0.8.7 through Lunar's Ichor service and its own classloader. The Java 8 ForgeGradle/LaunchWrapper development path cannot be launched directly on Java 17 because its old launcher assumes `AppClassLoader` extends `URLClassLoader`; that is a development-launcher limitation, not evidence about Lunar's Ichor runtime.
- To avoid treating a public `addURL` method as a requirement, the first-party loader appends the configured MOD JAR to Java's system classloader search during `premain`, then treats direct Mixin-classloader `addURL` as an optional optimization before calling `Mixins.addConfiguration`. The Java 8 LaunchWrapper smoke reaches `mixin-config-registered`, and the live Lunar smoke later proved Ichor can resolve and apply this configuration.
- A metadata-only signature inspection of the current Ichor Mixin classloader found that it extends `URLClassLoader` and exposes public `addURL(URL)`. The loader does not name or link against any Ichor class: it dynamically obtains the already-loaded standard `org.spongepowered.asm.mixin.Mixins` classloader and looks for that method. The live smoke confirmed this path on the tested Lunar build; Lunar updates can still change the implementation.
- The same metadata-only check confirmed Lunar's bundled Mixin 0.8.7 exposes public `Mixins.addConfiguration(String)`, the only Mixin API the loader invokes. No Lunar classes, source, mappings, or implementation code are bundled or linked by the project.

## First Lunar smoke result and correction

- The first live Lunar 1.8.9 smoke reached `[HypixelLegitils] Bootstrap Mixin reached Minecraft.startGame.`, proving the first-party loader registered the configuration and Lunar applied the startup Mixin.
- The next `runTick` injection crashed with `NoSuchFieldError: field_71439_g` from the compatibility-only chat indication. The failed code directly accessed `Minecraft.thePlayer` from an ordinary MOD class. Lunar's Ichor transformation handled the Mixin target but did not remap that ordinary MOD bytecode field reference.
- The bootstrap now contains no direct Minecraft fields, methods, chat, rendering, networking, or input access. Both injected callbacks only call Java-only methods that write one-time startup/tick proof lines to standard output. This is the required compatibility boundary before implementing mapped detector code.
- The corrected Lunar retry showed the documented startup/tick lines in the game profile log and remained running. Pre-launch loader status diagnostics need not be forwarded into that log. The previous crashing artifact is invalid and must not be used for later detector work.
- The successful retry registered the project configuration before Lunar's Ichor bake phase. The current `ichor-boot.log` records `bake.zip` as missing, then registers the configuration, then rebuilds that cache. Therefore rollback requires both removing the `-javaagent` argument and deleting the active Ichor `cache/.../bake.zip`; otherwise a cached transformed Minecraft class can remain. This matches the operational rollback requirement observed with Meowtils, but is now confirmed for the first-party loader too.
- Rollback was executed successfully: the JVM argument was removed, the active Ichor `bake.zip` was deleted, and Lunar rebuilt the cache without any Hypixel Legitils log output. The manual bootstrap gate is complete.

## Sources

- Java SE 8 Instrumentation API: https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html
- Sponge Mixin environment documentation: https://github.com/SpongePowered/Mixin/wiki/Introduction-to-Mixins---The-Mixin-Environment
- Sponge Mixin 0.7.11 snapshot metadata: https://repo.spongepowered.org/maven/org/spongepowered/mixin/0.7.11-SNAPSHOT/maven-metadata.xml
- Forge 1.8.9 official distribution: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html

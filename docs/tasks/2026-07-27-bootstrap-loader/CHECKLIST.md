# Bootstrap verification checklist

## Automated evidence

- [x] `:loader:test` passes with Java 8.
- [x] `:mod:jar` emits the MOD JAR with the Mixin JSON and generated refmap.
- [x] `verifyBootstrapArtifacts` confirms Java 8 bytecode, the Agent manifest, and absence of reference namespaces/artifacts.
- [x] Normal `./gradlew build` runs the artifact gate and verifies the exact legacy ForgeGradle/Mixin SHA-256 inputs.
- [x] The ForgeGradle 1.8.9 development launch registers `mixins.hypixellegitils.json` and logs application of `MixinMinecraft` to `net.minecraft.client.Minecraft`.
- [x] `:mod:runClient -PloaderDevSmoke` reports `config-valid` then `mixin-config-registered`; its log registers the config and applies `MixinMinecraft` to `net.minecraft.client.Minecraft`.
- [x] Lunar's installed ARM64 OpenJDK 17.0.3 accepts the built first-party `-javaagent` and reports `config-valid` with the generated configuration.
- [x] The Java 8 LaunchWrapper smoke still reports `mixin-config-registered` after the loader appends the MOD JAR to the system classloader search.
- [x] The current Lunar Ichor Mixin classloader exposes public `addURL(URL)` by metadata inspection; the loader discovers this dynamically rather than using Lunar implementation names.
- [x] Lunar's bundled Mixin 0.8.7 exposes public `Mixins.addConfiguration(String)`, the loader's only reflected Mixin API.

## Environment limitation recorded

- [x] The automated macOS run reaches Mixin application but exits before a game window because the temporary Java 8 runtime aborts during AWT application registration in this non-interactive execution context. This is not treated as a MOD or Mixin failure.

## Manual Lunar gate

- [x] Prepared the built JARs and generated absolute-path smoke configuration from the workspace outputs. A release installation must copy the same three files into a permanent directory as documented in `dist/INSTALL.md`.
- [x] In Lunar, used **Settings → Game Settings → JVM Arguments** to paste the generated `-javaagent` argument for the smoke test.
- [x] Initial Lunar smoke reached the startup Mixin and exposed an unmapped ordinary-MOD field reference; the invalid chat indication was removed before retry.
- [x] Corrected Lunar smoke reached both startup and tick Mixins without a crash; the current 1.8 runtime is Azul OpenJDK 17.0.18, Lunar launcher 3.7.12-ow, and Ichor Mixin 0.8.7.
- [x] Removed the JVM argument, deleted the active Ichor `cache/.../bake.zip`, restarted Lunar, and confirmed no Hypixel Legitils output while a clean cache was rebuilt.

# Phase 3 Check List

## Build prerequisite

- [x] Use a **Java 8 JDK**, not a Java 8 JRE. `javac -version` must succeed
  and the selected `JAVA_HOME` must contain `lib/tools.jar` for ForgeGradle
  2.1. The current local Java installation is a JRE and fails this requirement.
- [x] Run `./gradlew :mod:test verifyBootstrapArtifacts`.
- [x] Confirm `mod/build/libs/hypixel-legitils-0.1.0-SNAPSHOT.jar` contains
  `MixinWorldClient.class` and `BedNukeSignalCheck*.class`.
- [x] Confirm its `mixins.hypixellegitils.json` names all three client Mixins
  and its refmap includes `invalidateRegionAndSetBlock`, `doPreChunk`, and
  `invalidateBlockReceiveRegion`.
- [x] Confirm all Phase 3 trace tests pass: blocked positive, a normal
  horizontal/vertical route, incomplete volume, delayed bed halves, world
  reset, world transition, and global lag.

## Lunar manual gate

- [x] Run `./gradlew :mod:printLunarSmokeArgument` and copy the one printed
  `-javaagent:` argument into Lunar's JVM arguments field.
- [x] Fully quit and relaunch Lunar Client; do not attach to an already-running
  Minecraft process.
- [x] Start Lunar Minecraft 1.8.9 and confirm the normal bootstrap/tick logs;
  confirm no Mixin failure mentions `MixinWorldClient`,
  `invalidateRegionAndSetBlock`, or `doPreChunk`. (Confirmed from `latest.log`
  on 2026-07-28.)
- [ ] Join a harmless local/private test world first. Ordinary bed placement,
  bed removal with an open route, chunk transitions, and world/disconnect
  transitions must produce no BedNuke alert.
- [ ] Have a visible player enter a defense through an open route, close that
  route with a normal block-in, then remove the bed from inside. This must
  produce no BedNuke alert.
- [ ] Test only with a controlled, consented scenario where a bed is removed
  while the surrounding fully loaded defense volume has no open route. The
  alert must say `unassigned blocked-bed break anomaly` and name no player.
- [ ] Remove the JVM argument, delete Lunar's active Ichor `bake.zip` as in
  the previously verified rollback flow, restart Lunar, and confirm no
  Hypixel Legitils output remains.

## Stop condition

Do not mark Phase 3 complete until the Java 8 JDK build/test gate and every
applicable Lunar manual item above have evidence. If a cuboid is partial,
world/chunk transition occurs, either bed half is delayed, or a global-lag
frame occurs, no notification is an expected result.

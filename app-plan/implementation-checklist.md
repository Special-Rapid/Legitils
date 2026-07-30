# Implementation Checklist

## Phase 0 — bootstrap proof

- [x] Create Gradle project, wrapper, Java 8 toolchain, source/resource roots, and project license.
- [x] Set new identifiers: `com.snkisk.hypixellegitils`, `Hypixel Legitils`, `mixins.hypixellegitils.json`, and `hypixellegitils.agent.injected`.
- [x] Resolve and record a tested Minecraft 1.8.9/Mixin build combination.
- [x] Design and build the clean first-party Java 8 loader as its own artifact, with only loader/config parsing/Mixin registration responsibilities.
- [x] Build a minimal MOD JAR and loader JAR, then verify that no reference files are packaged.
- [x] Register only the startup/tick Mixin through the first-party loader and verify local startup/tick log evidence.
- [x] Run the manual alpha installation and verify that removing the JVM argument plus the active Ichor bake cache cleanly rolls back the MOD.
- [x] Record the tested Lunar Client build, loader version/license, and runtime Java version; do not claim a public compatibility matrix before this evidence exists.

## Phase 1 — safe foundation

- [x] Implement config loading/saving and local alert output.
- [x] Replace persistent Action Bar status with chat-default alert output and optional alert-only Action Bar output.
- [x] Implement bounded UUID observation storage and world-unload reset.
- [x] Implement `Evidence`, confidence levels, cool-downs, and global-lag suppression.
- [x] Verify normal alert re-notification after one second and Timer re-notification only after thirty seconds.
- [x] Add pure-Java unit tests for state cleanup, cool-downs, and evidence policy.

## Phase 2 — existing check families

- [x] Replace AutoBlock with its clean-room tick-level overlap progression; keep it default-disabled pending a Lunar normal-play trace.
- [x] Replace NoSlow with its clean-room 21-tick movement progression and corrected prior-position tracking; Lunar validation remains open.
- [x] Replace KillAura's generic score/decay rule with its clean-room consumable-use/attack-animation contract; keep it default-disabled pending Lunar validation.
- [x] Replace Legit Scaffold with its clean-room world-tick crouch/swing progression; keep it default-disabled pending a Lunar normal-bridging trace.
- [ ] Record the required normal-play and controlled comparison traces for all four check families before release.

## Phase 3 — BedNuke signal

Current priority defers this experimental phase until after Phase 4. The
existing signal remains default-disabled because normal block-in can match its
current final geometry.

- [ ] Define the minimum complete 3D local block-state snapshot and unassigned-event contract.
- [ ] Add the read-only `WorldClient` block-observation Mixin.
- [ ] Implement positive, negative, incomplete-snapshot, delayed-update, and reset test traces.
- [ ] Verify that the detector never alerts when evidence is incomplete.

## Phase 4 — Blink and Timer signals

Current implementation priority. A remote F3+T-like air-stall is an intended
Timer advisory signal; a freeze of the observing client is suppression, not
player evidence.

- [x] Implement player cadence and global-lag baseline collection.
- [x] Implement Blink evidence with repeated-episode thresholding.
- [x] Implement Timer evidence with support-block and world-transition exclusions.
- [x] Add global-lag, client-freeze-like, and normal-movement negative traces.
- [ ] Confirm all eight detectors are enabled in the first release; none is deferred as an optional post-release check.

## Phase 5 — NoBreakDelay signal

- [x] Define a conservative actor-attribution and local block-history contract before adding any observation Mixin.
- [x] Implement positive, normal breaking, missing-attribution, and global-lag/reset traces.
- [x] Verify anonymous or incomplete block updates never emit evidence.
- [ ] Record controlled Lunar traces for ordinary mining, interruptions, Haste/enchantments, creative-like mechanics, lag, and chunk transitions before default enablement.

## Phase 6 — macOS Companion

- [ ] Create the separate Swift Package Manager macOS Companion package and record the tested macOS/Xcode/Swift version.
- [ ] Implement Overview, Detectors, Alerts, Advanced, and Updates screens using the documented JSON schema.
- [ ] Implement atomic config replacement, schema validation, Restart required state, and last-known-good configuration recovery.
- [ ] Verify the Companion never attaches to Minecraft, modifies Lunar files/settings, or accesses the local proxy.
- [ ] Add Companion unit tests for defaults, migration, invalid input, and runtime-status presentation.
- [ ] Verify the Companion does not offer a gameplay-affecting alert control or a control that overrides the fixed cool-down policy.

## Local Blacklist visual marker

- [x] Define a default-off threshold-three accepted-alert automatic Blacklist policy with no detector weighting.
- [x] Add bounded, atomically persisted local UUID history only for non-anonymous `EvidencePolicy.shouldAlert` decisions and manual local edits.
- [x] Add optional local-only yellow `§e⚠` suffixes for visible Blacklist entries in Tab and NameTag rendering.
- [x] Verify global-lag, proxy-delay, and anonymous-evidence traces cannot automatically blacklist in pure-Java tests.
- [ ] Verify Tab/NameTag compatibility with Lunar UI modules in a manual world.

## Phase 7 — release verification

- [ ] Run unit tests and output-JAR content checks.
- [ ] Verify no forbidden APIs or reference packages appear in the production source/output.
- [ ] Perform a Lunar Client 1.8.9 smoke test with a local controlled environment.
- [ ] Verify installation documentation using a fresh absolute JAR path.
- [ ] Verify the MOD starts and all eight checks remain available when connected directly to `mc.hypixel.net`.
- [ ] Verify the same MOD JAR and detector configuration remain available when the client connects through the local proxy; no proxy IPC, configuration, or endpoint-specific branch may be required.
- [ ] Add a proxied-connection negative trace showing that proxy-induced global delay suppresses rather than creates a player-specific alert.
- [ ] Sign and notarize the macOS Companion with Developer ID credentials; test installation on a clean macOS 13+ account.
- [ ] Perform a final policy review: local-only alerts, no automatic action, no packet/input manipulation.

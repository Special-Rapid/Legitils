# Mock flag message checklist

## Automated

- [x] Attributed flags match the mock-up prefix, detector label and colours.
- [x] Current server display metadata is used without inferred team/name data.
- [x] WDR target accepts only a valid raw Minecraft player name.
- [x] Unknown/stale metadata and anonymous BedNuke contain no name/UUID/WDR.
- [x] Rejected/cooldown evidence creates no presentation.
- [x] Action Bar has no clickable-report semantics.
- [x] Java 8 `./gradlew :mod:test verifyBootstrapArtifacts` passes.

## Lunar manual gate

- [ ] Team-coloured mock format displays correctly for a visible flagged player.
- [ ] A deliberate WDR click produces only `/wdr <name>` for that player.
- [ ] BedNuke remains anonymous and has no WDR button.
- [ ] No alert sends a command until the user clicks its button.

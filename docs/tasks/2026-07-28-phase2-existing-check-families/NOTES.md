# Phase 2 Design Notes

## Observation boundary

`MixinMinecraft` reads the client-visible `WorldClient.playerEntities` list at
the existing `Minecraft.runTick` hook and passes immutable primitive samples to
ordinary Java code. The local player is excluded: Phase 2 looks only for
patterns visible from other players and does not inspect input or packets.

An observation frame with a client tick gap above 250 ms is marked global lag;
evidence from that frame is suppressed by the existing policy. A sample gap
above 150 ms resets every continuity-based pattern for that player.

## Balanced thresholds

| Detector | Evidence condition |
| --- | --- |
| AutoBlock | At least 8 continuous overlap samples and 2 visible swing rising edges while blocking. |
| NoSlow | At least 6 continuous grounded sprint/item-use samples with a potion-adjusted horizontal movement rate above 0.095 blocks/tick. |
| KillAura | A bounded score reaches 6 through at least 3 visible item-use + swing-edge samples with nearby hurt-animation combat context; ordinary samples decay the score. |
| Legit Scaffold | At least 3 short sneak cycles in block-holding, grounded, downward-pitch, swing-visible context. |

Conservative and sensitive presets respectively require more and fewer repeated
samples. Missing held-item, use, swing, potion or position state produces no
evidence and clears the corresponding pattern.

## Limitations

Remote animation, held-item and item-use states are client-visible hints, not
proof of a player input or a server-accepted action. Alert strings describe the
pattern only, never a cheat name or verdict.

## Deferred NoBreakDelay clarification

The later NoBreakDelay anti-cheat should detect and locally notify on repeated
absence of the expected inter-break delay (reported as approximately five ticks
in the target gameplay context), not implement or alter that behaviour. Its
actual threshold remains unselected until controlled 1.8.9 traces cover block,
tool, effect and server-mechanic variation.

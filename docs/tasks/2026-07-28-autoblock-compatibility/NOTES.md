# Research Notes

## Confirmed source mapping

The reference condition maps directly to currently observable local fields:

| Meowtils reference | Current visible sampling |
| --- | --- |
| attack animation (`field_82175_bq`) | `EntityPlayer.isSwingInProgress` |
| blocking (`func_70632_aY`) | `EntityPlayer.isBlocking()` |
| client world tick | `Minecraft.runTick` observation frame |

The reference increments a counter only while both states are true and exposes
a failure when `counter > 10`. It does not require separate swing edges.

## Why this does not solve false positives by itself

The current implementation's extra swing-edge requirement is not in Meowtils.
Removing it improves reference compatibility but also means ordinary sword
blocking that visibly overlaps the swing animation can meet the same 11-tick
sequence. The safe product decision is default-off during validation, not an
invented replacement threshold.

## Implementation boundary

The existing `MixinMinecraft` already observes the two required fields once
per `runTick`; no packet hook or runtime attach is needed. The clean rewrite
must use those inputs only and must not copy the reference source.

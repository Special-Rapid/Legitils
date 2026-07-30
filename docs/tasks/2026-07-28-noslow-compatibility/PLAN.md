# Plan

## Objective

Implement the intended, observable Meowtils NoSlow rule on the existing POST
world-tick sampling path.

## Steps

1. Replace millisecond normalisation, ground gating and sensitivity thresholds
   with fixed world-tick progression.
2. Preserve the correctly updated previous position and bounded UUID state.
3. Add timing, boundary, Speed, missing-frame, reset and multi-player traces.
4. Keep NoSlow explicit opt-in until the existing manual gate is extended and
   passes.

## Risks

- Item-use/sprint state is client-visible advisory evidence, not server truth.
- Reference threshold compatibility does not prove acceptable false-positive
  behaviour in Lunar, so manual normal-play validation remains required.

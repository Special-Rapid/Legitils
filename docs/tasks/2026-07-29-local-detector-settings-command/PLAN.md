# Plan

## Objective

Add a narrow local settings command that applies only its persisted detector
set immediately, while preserving the restart-owned boundary for every other
runtime setting.

## Scope

- Local command parsing and Bootstrap dispatch.
- A small config persistence service built on `LegitilsConfigStore`.
- Status text, unit tests, policy documentation, and Lunar checklist.

## Non-goals

- Applying non-detector configuration changes in the current session.
- Editing any detector scoring logic or packet/input path.

## Steps

1. Parse only the documented manual command forms and detector aliases.
2. Copy the startup config with a monotonic revision and changed enabled set.
3. Write through the existing atomic config store and retain it as the
   current detector configuration.
4. Show the immediately active detector state and persistence result.
5. Test parser, persistence, no-op updates, invalid inputs, and Java 8 JAR
   verification.

## Risks

- A runtime toggle must never combine partial detector observations produced
  before and after the change.
- Existing defaults intentionally disable all five checks pending their false
  positive gates; the command must not silently enable them.

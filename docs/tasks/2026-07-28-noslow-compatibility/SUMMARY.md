# Summary

NoSlow now follows a clean-room, visible-client-state interpretation of the
Meowtils tick contract: 21 consecutive per-world-tick horizontal movements
strictly above the Speed-adjusted threshold while sprinting, using an item, and
not riding. The code intentionally corrects the reference's stale
previous-position bug and preserves the product's observation-only boundary.

Static Java 8 verification passes. NoSlow is disabled in new/default
configurations until the Lunar normal-play and controlled comparison traces
pass; existing explicit configuration remains opt-in.

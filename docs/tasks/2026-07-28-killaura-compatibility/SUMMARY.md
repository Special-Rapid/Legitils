# Summary

KillAura now follows a clean-room visible-state interpretation of the supplied
Meowtils rule: consumable-use duration, a completed recent use, active attack
animation, one-step violation decay, and alert at level eight. The former
generic nearby-hurt and millisecond score heuristic is removed from the
detector path.

Static Java 8 verification passes. New/default configurations keep KillAura
explicit opt-in until the Lunar manual gates pass.

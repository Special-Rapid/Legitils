package com.snkisk.hypixellegitils.config;

import java.util.EnumSet;
import java.util.Set;

/** Planned detector identifiers; only released identifiers are configurable in this build. */
public enum DetectorId {
    AUTO_BLOCK,
    NO_SLOW,
    KILL_AURA,
    LEGIT_SCAFFOLD,
    BED_NUKE,
    COMBAT_DESYNC,
    AIR_STALL,
    NO_BREAK_DELAY;

    public boolean isImplementedInCurrentBuild() {
        return this == AUTO_BLOCK || this == NO_SLOW || this == KILL_AURA || this == LEGIT_SCAFFOLD
            || this == BED_NUKE || this == COMBAT_DESYNC || this == AIR_STALL || this == NO_BREAK_DELAY;
    }

    public String displayName() {
        if (this == AUTO_BLOCK) return "AutoBlock";
        if (this == NO_SLOW) return "NoSlow";
        if (this == KILL_AURA) return "KillAura";
        if (this == LEGIT_SCAFFOLD) return "LegitScaffold";
        if (this == BED_NUKE) return "BedNuke";
        if (this == COMBAT_DESYNC) return "Blink";
        if (this == AIR_STALL) return "Timer";
        if (this == NO_BREAK_DELAY) return "NoBreakDelay";
        return name();
    }

    public static Set<DetectorId> implementedInCurrentBuild() {
        EnumSet<DetectorId> implemented = EnumSet.noneOf(DetectorId.class);
        for (DetectorId detector : values()) if (detector.isImplementedInCurrentBuild()) implemented.add(detector);
        return implemented;
    }
}

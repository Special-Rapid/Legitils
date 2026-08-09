package com.snkisk.hypixellegitils.stats;

/** Keeps only a mode visibly confirmed in the current client world for post-start Stats requests. */
public final class BedwarsModeTracker {
    private BedwarsMode lastVisibleMode = BedwarsMode.UNKNOWN;

    public void observe(BedwarsMode visibleMode) {
        if (visibleMode != null && visibleMode != BedwarsMode.UNKNOWN) lastVisibleMode = visibleMode;
    }

    public BedwarsMode resolve(BedwarsMode visibleMode) {
        if (visibleMode != null && visibleMode != BedwarsMode.UNKNOWN) return visibleMode;
        return lastVisibleMode;
    }

    public void reset() {
        lastVisibleMode = BedwarsMode.UNKNOWN;
    }
}

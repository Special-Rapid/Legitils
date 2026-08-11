package com.snkisk.hypixellegitils.mixin;

/** Builds local Tab-only markers before the Stats column is measured. */
public final class TabStatsMarkers {
    private TabStatsMarkers() {
    }

    public static String forPlayer(boolean nicked, boolean acceptedAlert) {
        String markers = "";
        if (nicked) markers += " §c[NICK]";
        if (acceptedAlert) markers += " §e⚠";
        return markers;
    }
}

package com.snkisk.hypixellegitils.stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Retains already-resolved match members when a later `/who` omits eliminated players. */
public final class StatsMatchResultRetention {
    private StatsMatchResultRetention() {
    }

    /**
     * A `/who` response lists only currently online players. Keep normalized display data for
     * eliminated match members, while letting the fresh result replace every returned member.
     */
    public static StatsBridgeLookupResult mergeWhoRefresh(
        StatsBridgeLookupResult current,
        StatsBridgeLookupResult incoming
    ) {
        if (incoming == null) return current == null ? StatsBridgeLookupResult.unavailable() : current;
        if (current == null || current.status != StatsBridgeLookupResult.Status.READY) return incoming;
        if (incoming.status != StatsBridgeLookupResult.Status.READY) return current;
        Map<String, StatsBridgePlayerResult> players = new LinkedHashMap<String, StatsBridgePlayerResult>();
        for (StatsBridgePlayerResult player : current.players) put(players, player);
        for (StatsBridgePlayerResult player : incoming.players) put(players, player);
        return StatsBridgeLookupResult.ready(new ArrayList<StatsBridgePlayerResult>(players.values()));
    }

    private static void put(Map<String, StatsBridgePlayerResult> players, StatsBridgePlayerResult player) {
        if (player == null || player.name == null) return;
        players.put(player.name.toLowerCase(Locale.ROOT), player);
    }
}

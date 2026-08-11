package com.snkisk.hypixellegitils.stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        for (StatsBridgePlayerResult player : current.players) put(players, player, false);
        for (StatsBridgePlayerResult player : incoming.players) put(players, player, true);
        return StatsBridgeLookupResult.ready(new ArrayList<StatsBridgePlayerResult>(players.values()));
    }

    /**
     * Produces a display result for exactly the players returned by the current server roster,
     * while preserving advisory tags already known for those returned players.
     */
    public static StatsBridgeLookupResult returnedMembersWithRetainedTags(
        StatsBridgeLookupResult current,
        StatsBridgeLookupResult incoming
    ) {
        if (incoming == null || incoming.status != StatsBridgeLookupResult.Status.READY) {
            return incoming == null ? StatsBridgeLookupResult.unavailable() : incoming;
        }
        if (current == null || current.status != StatsBridgeLookupResult.Status.READY) return incoming;
        Map<String, StatsBridgePlayerResult> prior = new LinkedHashMap<String, StatsBridgePlayerResult>();
        for (StatsBridgePlayerResult player : current.players) put(prior, player, false);
        List<StatsBridgePlayerResult> returned = new ArrayList<StatsBridgePlayerResult>();
        for (StatsBridgePlayerResult player : incoming.players) {
            if (player == null || player.name == null) continue;
            StatsBridgePlayerResult existing = prior.get(player.name.toLowerCase(Locale.ROOT));
            returned.add(existing == null ? player : mergePlayer(existing, player));
        }
        return StatsBridgeLookupResult.ready(returned);
    }

    private static void put(Map<String, StatsBridgePlayerResult> players, StatsBridgePlayerResult player, boolean fresh) {
        if (player == null || player.name == null) return;
        String key = player.name.toLowerCase(Locale.ROOT);
        StatsBridgePlayerResult existing = players.get(key);
        players.put(key, fresh && existing != null ? mergePlayer(existing, player) : player);
    }

    /** Provider tags are advisory match context: a later empty reply must not erase a tag already shown in this match. */
    private static StatsBridgePlayerResult mergePlayer(StatsBridgePlayerResult existing, StatsBridgePlayerResult fresh) {
        return new StatsBridgePlayerResult(
            fresh.name,
            fresh.nickStatus,
            fresh.stars == null ? existing.stars : fresh.stars,
            fresh.finalKillDeathRatio == null ? existing.finalKillDeathRatio : fresh.finalKillDeathRatio,
            fresh.modeWinStreak == null ? existing.modeWinStreak : fresh.modeWinStreak,
            mergedTags(existing.communityTags, fresh.communityTags)
        );
    }

    private static List<StatsBridgePlayerResult.CommunityTag> mergedTags(
        List<StatsBridgePlayerResult.CommunityTag> existing,
        List<StatsBridgePlayerResult.CommunityTag> fresh
    ) {
        Map<String, StatsBridgePlayerResult.CommunityTag> tags = new LinkedHashMap<String, StatsBridgePlayerResult.CommunityTag>();
        addTags(tags, existing);
        // A matching current tag replaces its prior tooltip, while a current empty tag list preserves known match context.
        addTags(tags, fresh);
        return new ArrayList<StatsBridgePlayerResult.CommunityTag>(tags.values());
    }

    private static void addTags(Map<String, StatsBridgePlayerResult.CommunityTag> target, List<StatsBridgePlayerResult.CommunityTag> source) {
        if (source == null) return;
        for (StatsBridgePlayerResult.CommunityTag tag : source) {
            if (tag == null || tag.source == null || tag.label == null) continue;
            target.put(tag.source.toLowerCase(Locale.ROOT) + "\u0000" + tag.label.toLowerCase(Locale.ROOT), tag);
        }
    }
}

package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.StatsSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable local Tab ordering. It receives no network data and never changes team membership. */
public final class StatsTabSorter {
    public static final double NICK_TEAM_VALUE = 5D;

    private StatsTabSorter() {
    }

    public static <T> List<T> sort(List<Entry<T>> original, StatsSettings settings) {
        if (original == null || original.isEmpty() || settings == null || !settings.enabled) return values(original);
        List<Group<T>> groups = groups(original);
        if (settings.tabPlayerSortingEnabled) for (Group<T> group : groups) sortEntries(group.entries, PLAYER_ORDER);
        if (settings.tabTeamSortingEnabled) sortTeamSegments(groups);
        return values(groups, original.size());
    }

    /** Chat always puts a Nick at the top of its team, while retaining the two existing optional sort controls. */
    public static <T> List<T> sortForChat(List<Entry<T>> original, StatsSettings settings) {
        if (original == null || original.isEmpty() || settings == null || !settings.enabled) return values(original);
        List<Group<T>> groups = groups(original);
        for (Group<T> group : groups) {
            sortEntries(group.entries, settings.tabPlayerSortingEnabled ? PLAYER_ORDER : NICK_FIRST_ORDER);
        }
        if (settings.tabTeamSortingEnabled) sortTeamSegments(groups);
        return values(groups, original.size());
    }

    private static <T> List<T> values(List<Entry<T>> entries) {
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        List<T> values = new ArrayList<T>(entries.size());
        for (Entry<T> entry : entries) if (entry != null) values.add(entry.value);
        return values;
    }

    private static <T> List<T> values(List<Group<T>> groups, int capacity) {
        List<T> values = new ArrayList<T>(Math.max(0, capacity));
        for (Group<T> group : groups) for (Entry<T> entry : group.entries) values.add(entry.value);
        return values;
    }

    private static <T> List<Group<T>> groups(List<Entry<T>> original) {
        Map<String, Group<T>> byTeam = new LinkedHashMap<String, Group<T>>();
        int unteamed = 0;
        for (Entry<T> entry : original) {
            if (entry == null) continue;
            // A missing server team must never join an inferred group or move across other entries.
            boolean hasServerTeam = entry.teamKey != null && !entry.teamKey.isEmpty();
            String key = hasServerTeam ? entry.teamKey : "\u0000" + unteamed++;
            Group<T> group = byTeam.get(key);
            if (group == null) {
                group = new Group<T>(hasServerTeam, entry.originalIndex);
                byTeam.put(key, group);
            }
            group.entries.add(entry);
        }
        return new ArrayList<Group<T>>(byTeam.values());
    }

    private static final Comparator<Entry<?>> PLAYER_ORDER = new Comparator<Entry<?>>() {
        @Override
        public int compare(Entry<?> left, Entry<?> right) {
            if (left.nicked != right.nicked) return left.nicked ? -1 : 1;
            double leftFkdr = usableFkdr(left.fkdr);
            double rightFkdr = usableFkdr(right.fkdr);
            if (leftFkdr != rightFkdr) return leftFkdr > rightFkdr ? -1 : 1;
            return left.originalIndex - right.originalIndex;
        }
    };

    private static final Comparator<Entry<?>> NICK_FIRST_ORDER = new Comparator<Entry<?>>() {
        @Override
        public int compare(Entry<?> left, Entry<?> right) {
            if (left.nicked != right.nicked) return left.nicked ? -1 : 1;
            return left.originalIndex - right.originalIndex;
        }
    };

    private static final Comparator<Group<?>> TEAM_ORDER = new Comparator<Group<?>>() {
        @Override
        public int compare(Group<?> left, Group<?> right) {
            double leftScore = teamScore(left.entries);
            double rightScore = teamScore(right.entries);
            if (leftScore != rightScore) return leftScore > rightScore ? -1 : 1;
            return left.serverFirstIndex - right.serverFirstIndex;
        }
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void sortEntries(List<Entry<T>> entries, Comparator<Entry<?>> comparator) {
        Collections.sort((List) entries, (Comparator) comparator);
    }

    private static <T> void sortTeamSegments(List<Group<T>> groups) {
        int start = 0;
        while (start < groups.size()) {
            if (!groups.get(start).hasServerTeam) {
                start++;
                continue;
            }
            int end = start + 1;
            while (end < groups.size() && groups.get(end).hasServerTeam) end++;
            sortGroups(groups.subList(start, end));
            start = end;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void sortGroups(List<Group<T>> groups) {
        Collections.sort((List) groups, (Comparator) TEAM_ORDER);
    }

    private static double teamScore(List<? extends Entry<?>> entries) {
        double score = 0D;
        for (Entry<?> entry : entries) score += entry.nicked ? NICK_TEAM_VALUE : usableFkdr(entry.fkdr);
        return score;
    }

    private static double usableFkdr(Double fkdr) {
        return fkdr == null || Double.isNaN(fkdr.doubleValue()) || Double.isInfinite(fkdr.doubleValue()) || fkdr.doubleValue() < 0D
            ? 0D : fkdr.doubleValue();
    }

    public static final class Entry<T> {
        public final T value;
        public final String teamKey;
        public final boolean nicked;
        public final Double fkdr;
        public final int originalIndex;

        public Entry(T value, String teamKey, boolean nicked, Double fkdr, int originalIndex) {
            this.value = value;
            this.teamKey = teamKey;
            this.nicked = nicked;
            this.fkdr = fkdr;
            this.originalIndex = originalIndex;
        }
    }

    private static final class Group<T> {
        private final boolean hasServerTeam;
        private final int serverFirstIndex;
        private final List<Entry<T>> entries = new ArrayList<Entry<T>>();

        private Group(boolean hasServerTeam, int serverFirstIndex) {
            this.hasServerTeam = hasServerTeam;
            this.serverFirstIndex = serverFirstIndex;
        }
    }
}

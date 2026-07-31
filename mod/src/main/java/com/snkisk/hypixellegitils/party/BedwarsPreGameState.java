package com.snkisk.hypixellegitils.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

/** Reads only the visible sidebar to limit Party Detector parsing to Bed Wars pre-game. */
public final class BedwarsPreGameState {
    private static final Pattern PLAYER_COUNT = Pattern.compile("^players:\\s*(\\d+)\\s*/\\s*(\\d+)$");

    private BedwarsPreGameState() {
    }

    public static boolean isActive(WorldClient world) {
        return playerCount(world).preGame;
    }

    /** Returns the visible pre-game player counter without reading hidden server data. */
    public static PlayerCount playerCount(WorldClient world) {
        if (world == null) return PlayerCount.inactive();
        Scoreboard scoreboard = world.getScoreboard();
        if (scoreboard == null) return PlayerCount.inactive();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return PlayerCount.inactive();
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        List<String> lines = new ArrayList<String>(scores.size());
        for (Score score : scores) {
            if (score == null) continue;
            String entry = score.getPlayerName();
            lines.add(ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(entry), entry));
        }
        return playerCount(objective.getDisplayName(), lines);
    }

    static boolean isActive(String title, Iterable<String> lines) {
        return playerCount(title, lines).preGame;
    }

    static PlayerCount playerCount(String title, Iterable<String> lines) {
        if (!normalized(title).contains("bed wars")) return PlayerCount.inactive();
        if (lines == null) return PlayerCount.inactive();
        boolean preGame = false;
        int current = -1;
        int maximum = -1;
        for (String line : lines) {
            String normalized = normalized(line);
            if (normalized.contains("waiting...") || normalized.contains("starting in")) preGame = true;
            Matcher playerCount = PLAYER_COUNT.matcher(normalized);
            if (playerCount.matches()) {
                current = Integer.parseInt(playerCount.group(1));
                maximum = Integer.parseInt(playerCount.group(2));
            }
        }
        return preGame ? new PlayerCount(current, maximum, true) : PlayerCount.inactive();
    }

    private static String normalized(String value) {
        return value == null ? "" : value.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "").toLowerCase(Locale.ROOT).trim();
    }

    /** A parsed visible sidebar count; -1 means that the sidebar did not expose a count. */
    public static final class PlayerCount {
        public final int current;
        public final int maximum;
        public final boolean preGame;

        private PlayerCount(int current, int maximum, boolean preGame) {
            this.current = current;
            this.maximum = maximum;
            this.preGame = preGame;
        }

        private static PlayerCount inactive() {
            return new PlayerCount(-1, -1, false);
        }
    }
}

package com.snkisk.hypixellegitils.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

/** Reads only the visible sidebar to limit Party Detector parsing to Bed Wars pre-game. */
public final class BedwarsPreGameState {
    private BedwarsPreGameState() {
    }

    public static boolean isActive(WorldClient world) {
        if (world == null) return false;
        Scoreboard scoreboard = world.getScoreboard();
        if (scoreboard == null) return false;
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return false;
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        List<String> lines = new ArrayList<String>(scores.size());
        for (Score score : scores) {
            if (score == null) continue;
            String entry = score.getPlayerName();
            lines.add(ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(entry), entry));
        }
        return isActive(objective.getDisplayName(), lines);
    }

    static boolean isActive(String title, Iterable<String> lines) {
        if (!normalized(title).contains("bed wars")) return false;
        if (lines == null) return false;
        for (String line : lines) {
            String normalized = normalized(line);
            if (normalized.contains("waiting...") || normalized.contains("starting in")) return true;
        }
        return false;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "").toLowerCase(Locale.ROOT).trim();
    }
}

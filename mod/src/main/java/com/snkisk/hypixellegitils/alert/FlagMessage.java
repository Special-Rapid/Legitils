package com.snkisk.hypixellegitils.alert;

import com.snkisk.hypixellegitils.config.DetectorId;
import java.util.regex.Pattern;

/**
 * Pure local flag-message formatting. Minecraft components and click events
 * are intentionally built only by the Mixin UI adapter.
 */
public final class FlagMessage {
    private static final String PREFIX = ChatFormat.PREFIX + " ";
    private static final String FLAGGED = " \u00a7cflagged ";
    private static final String WDR_PREFIX = " \u00a77| ";
    private static final String WDR_LABEL = "\u00a74[WDR]";
    private static final Pattern PLAYER_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

    /** Chat text before the optional independently clickable WDR component. */
    public final String chatPrefixText;
    /** Short, non-interactive text for the optional Action Bar. */
    public final String actionBarText;
    /** Validated raw player name, or null when the alert must remain anonymous. */
    public final String wdrTarget;

    private FlagMessage(String chatPrefixText, String actionBarText, String wdrTarget) {
        this.chatPrefixText = chatPrefixText;
        this.actionBarText = actionBarText;
        this.wdrTarget = wdrTarget;
    }

    public static FlagMessage anonymous(DetectorId detector) {
        String detectorText = detectorText(detector);
        return new FlagMessage(PREFIX + "\u00a7cflagged " + detectorText, PREFIX + "\u00a7cflagged " + detectorText, null);
    }

    /**
     * Uses only the server-provided formatted display string. A malformed raw
     * profile name or a missing display value becomes anonymous.
     */
    public static FlagMessage attributed(DetectorId detector, String formattedDisplayName, String rawPlayerName) {
        return attributed(detector, formattedDisplayName, rawPlayerName, true);
    }

    /** Shows a visible player identity; the caller may forbid a WDR affordance for local development samples. */
    public static FlagMessage attributed(DetectorId detector, String formattedDisplayName, String rawPlayerName, boolean allowWdr) {
        if (!isUsableDisplayName(formattedDisplayName) || !isValidPlayerName(rawPlayerName)) {
            return anonymous(detector);
        }
        String detectorText = detectorText(detector);
        String base = PREFIX + whiteTeamPrefix(formattedDisplayName) + FLAGGED + detectorText;
        return allowWdr ? new FlagMessage(base + WDR_PREFIX, base, rawPlayerName) : new FlagMessage(base, base, null);
    }

    public String completeChatText() {
        return wdrTarget == null ? chatPrefixText : chatPrefixText + WDR_LABEL;
    }

    public static boolean isValidPlayerName(String rawPlayerName) {
        return rawPlayerName != null && PLAYER_NAME.matcher(rawPlayerName).matches();
    }

    /** Uses the server's team-formatted display name when it is available. */
    public static String teamFormattedName(String formattedDisplayName, String fallbackName) {
        return isUsableDisplayName(formattedDisplayName) ? whiteTeamPrefix(formattedDisplayName) : fallbackName;
    }

    /** Identifies the normalized Bed Wars team prefix used for a local player identity. */
    public static boolean hasBedWarsTeamPrefix(String formattedDisplayName) {
        if (formattedDisplayName == null) return false;
        if (formattedDisplayName.startsWith("§7W ")) return true;
        if (formattedDisplayName.length() >= 4 && formattedDisplayName.charAt(0) == '§'
            && isBedWarsTeamInitial(formattedDisplayName.charAt(2)) && formattedDisplayName.charAt(3) == ' ') return true;
        return formattedDisplayName.length() >= 6 && formattedDisplayName.charAt(0) == '§'
            && formattedDisplayName.charAt(2) == '§' && formattedDisplayName.charAt(3) == 'l'
            && isBedWarsTeamInitial(formattedDisplayName.charAt(4)) && formattedDisplayName.charAt(5) == ' ';
    }

    private static boolean isUsableDisplayName(String formattedDisplayName) {
        return formattedDisplayName != null && !formattedDisplayName.trim().isEmpty();
    }

    /** Normalizes the white-team colour and makes only Bed Wars' team initial bold. */
    private static String whiteTeamPrefix(String formattedDisplayName) {
        if (formattedDisplayName.startsWith("§7W ")) {
            return "§f§lW §f" + formattedDisplayName.substring(4);
        }
        if (formattedDisplayName.length() >= 4 && formattedDisplayName.charAt(0) == '§'
            && isBedWarsTeamInitial(formattedDisplayName.charAt(2)) && formattedDisplayName.charAt(3) == ' ') {
            String teamColor = formattedDisplayName.substring(0, 2);
            return teamColor + "§l" + formattedDisplayName.charAt(2) + " " + teamColor + formattedDisplayName.substring(4);
        }
        return formattedDisplayName;
    }

    private static boolean isBedWarsTeamInitial(char value) {
        return value == 'R' || value == 'G' || value == 'B' || value == 'Y' || value == 'W';
    }

    private static String detectorText(DetectorId detector) {
        if (detector == DetectorId.AUTO_BLOCK) return "\u00a76AutoBlock";
        if (detector == DetectorId.NO_SLOW) return "\u00a7bNoSlow";
        if (detector == DetectorId.KILL_AURA) return "\u00a7cKillAura";
        if (detector == DetectorId.LEGIT_SCAFFOLD) return "\u00a75LegitScaffold";
        if (detector == DetectorId.BED_NUKE) return "\u00a74BedNuke";
        if (detector == DetectorId.COMBAT_DESYNC) return "\u00a7dBlink";
        if (detector == DetectorId.AIR_STALL) return "\u00a7fTimer";
        if (detector == DetectorId.NO_BREAK_DELAY) return "\u00a7fNoBreakDelay";
        return "\u00a7fUnknown";
    }
}

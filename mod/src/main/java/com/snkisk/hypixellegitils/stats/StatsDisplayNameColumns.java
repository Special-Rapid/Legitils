package com.snkisk.hypixellegitils.stats;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Keeps one local Tab name column that includes display text added by other client MODs. */
public final class StatsDisplayNameColumns {
    private static final int MAXIMUM_TRACKED_NAMES = 256;
    private static final int FALLBACK_SPACE_WIDTH = 4;
    private static final int FALLBACK_BOLD_SPACE_WIDTH = 5;
    private final Map<String, RenderedName> activeRenderedNames = new LinkedHashMap<String, RenderedName>();
    private final Map<String, RenderedName> nextRenderedNames = new LinkedHashMap<String, RenderedName>();
    private int activeColumnPixelWidth;
    private int activeSpacePixelWidth = FALLBACK_SPACE_WIDTH;
    private int activeBoldSpacePixelWidth = FALLBACK_BOLD_SPACE_WIDTH;
    private int activeStarPixelWidth;
    private int nextColumnPixelWidth;
    private int nextSpacePixelWidth = FALLBACK_SPACE_WIDTH;
    private int nextBoldSpacePixelWidth = FALLBACK_BOLD_SPACE_WIDTH;
    private int nextStarPixelWidth;

    /** Starts a fresh Tab roster snapshot; the previous complete snapshot remains available to render it. */
    public synchronized void beginTabRender() {
        nextRenderedNames.clear();
        nextColumnPixelWidth = 0;
        nextSpacePixelWidth = FALLBACK_SPACE_WIDTH;
        nextBoldSpacePixelWidth = FALLBACK_BOLD_SPACE_WIDTH;
        nextStarPixelWidth = 0;
    }

    /** Records this render's actual FontRenderer width and pads to the previous complete roster width. */
    public synchronized String observeTabName(
        String profileName,
        String renderedName,
        int renderedPixelWidth,
        int spacePixelWidth,
        int boldSpacePixelWidth,
        String starText,
        int starPixelWidth
    ) {
        if (isUsable(profileName, renderedName)) {
            if (nextRenderedNames.size() < MAXIMUM_TRACKED_NAMES || nextRenderedNames.containsKey(key(profileName))) {
                nextRenderedNames.put(key(profileName), new RenderedName(renderedName, renderedPixelWidth, starText, starPixelWidth));
            }
            if (renderedPixelWidth > nextColumnPixelWidth) nextColumnPixelWidth = renderedPixelWidth;
            if (spacePixelWidth > 0) nextSpacePixelWidth = spacePixelWidth;
            if (boldSpacePixelWidth > 0) nextBoldSpacePixelWidth = boldSpacePixelWidth;
            if (starPixelWidth > nextStarPixelWidth) nextStarPixelWidth = starPixelWidth;
        }
        return spacesFor(activeColumnPixelWidth, renderedPixelWidth, activeSpacePixelWidth, activeBoldSpacePixelWidth);
    }

    /** Publishes the complete current roster for the next Tab frame and for automatic Chat output. */
    public synchronized void finishTabRender() {
        activeRenderedNames.clear();
        activeRenderedNames.putAll(nextRenderedNames);
        activeColumnPixelWidth = nextColumnPixelWidth;
        activeSpacePixelWidth = nextSpacePixelWidth;
        activeBoldSpacePixelWidth = nextBoldSpacePixelWidth;
        activeStarPixelWidth = nextStarPixelWidth;
    }

    /** Returns the latest complete Tab field and its dynamic roster padding for automatic Chat. */
    public synchronized String nameForChat(String profileName, String fallbackName) {
        RenderedName rendered = profileName == null ? null : activeRenderedNames.get(key(profileName));
        if (rendered == null || !isUsable(profileName, rendered.text)) return fallbackName == null ? "" : fallbackName;
        return rendered.text + spacesFor(
            activeColumnPixelWidth, rendered.pixelWidth, activeSpacePixelWidth, activeBoldSpacePixelWidth
        );
    }

    /** Returns the matching current Star-column padding for Tab or Chat, never for a stale different value. */
    public synchronized String starPadding(String profileName, String starText) {
        if (profileName == null || starText == null || starText.isEmpty()) return "";
        RenderedName rendered = activeRenderedNames.get(key(profileName));
        if (rendered == null || !starText.equals(rendered.starText)) return "";
        return starPadding(profileName, starText, rendered.starPixelWidth);
    }

    /** Keeps FKDR anchored even when a newly rendered Star differs from the prior Tab snapshot. */
    public synchronized String starPadding(String profileName, String starText, int starPixelWidth) {
        if (profileName == null || starText == null || starText.isEmpty()) return "";
        return spacesFor(activeStarPixelWidth, Math.max(0, starPixelWidth), activeSpacePixelWidth, activeBoldSpacePixelWidth);
    }

    public synchronized void clear() {
        activeRenderedNames.clear();
        nextRenderedNames.clear();
        activeColumnPixelWidth = 0;
        activeSpacePixelWidth = FALLBACK_SPACE_WIDTH;
        activeBoldSpacePixelWidth = FALLBACK_BOLD_SPACE_WIDTH;
        activeStarPixelWidth = 0;
        nextColumnPixelWidth = 0;
        nextSpacePixelWidth = FALLBACK_SPACE_WIDTH;
        nextBoldSpacePixelWidth = FALLBACK_BOLD_SPACE_WIDTH;
        nextStarPixelWidth = 0;
    }

    private static String spacesFor(
        int columnPixelWidth,
        int renderedPixelWidth,
        int spacePixelWidth,
        int boldSpacePixelWidth
    ) {
        int missingPixels = columnPixelWidth - renderedPixelWidth;
        if (missingPixels <= 0) return "";
        int usableSpaceWidth = spacePixelWidth > 0 ? spacePixelWidth : FALLBACK_SPACE_WIDTH;
        int usableBoldSpaceWidth = boldSpacePixelWidth > 0 ? boldSpacePixelWidth : FALLBACK_BOLD_SPACE_WIDTH;
        int maximumSpaces = (missingPixels + Math.min(usableSpaceWidth, usableBoldSpaceWidth) - 1)
            / Math.min(usableSpaceWidth, usableBoldSpaceWidth) + 1;
        int bestWidth = Integer.MAX_VALUE;
        int normalSpaces = 0;
        int boldSpaces = 0;
        for (int normal = 0; normal <= maximumSpaces; normal++) {
            int remainingPixels = missingPixels - normal * usableSpaceWidth;
            int bold = remainingPixels <= 0 ? 0 : (remainingPixels + usableBoldSpaceWidth - 1) / usableBoldSpaceWidth;
            int candidateWidth = normal * usableSpaceWidth + bold * usableBoldSpaceWidth;
            if (candidateWidth < missingPixels || candidateWidth >= bestWidth) continue;
            bestWidth = candidateWidth;
            normalSpaces = normal;
            boldSpaces = bold;
        }
        StringBuilder padding = new StringBuilder(normalSpaces + boldSpaces + 4).append("§r");
        for (int index = 0; index < normalSpaces; index++) padding.append(' ');
        if (boldSpaces > 0) {
            padding.append("§l");
            for (int index = 0; index < boldSpaces; index++) padding.append(' ');
        }
        padding.append("§r");
        return padding.toString();
    }

    private static boolean isUsable(String profileName, String renderedName) {
        return profileName != null && renderedName != null && !renderedName.trim().isEmpty()
            && stripFormatting(renderedName).toLowerCase(Locale.ROOT).contains(profileName.toLowerCase(Locale.ROOT));
    }

    private static String key(String profileName) {
        return profileName.toLowerCase(Locale.ROOT);
    }

    private static String stripFormatting(String text) {
        StringBuilder visible = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '§' && index + 1 < text.length()) {
                index++;
            } else visible.append(text.charAt(index));
        }
        return visible.toString();
    }

    private static final class RenderedName {
        private final String text;
        private final int pixelWidth;
        private final String starText;
        private final int starPixelWidth;

        private RenderedName(String text, int pixelWidth, String starText, int starPixelWidth) {
            this.text = text;
            this.pixelWidth = Math.max(0, pixelWidth);
            this.starText = starText == null ? "" : starText;
            this.starPixelWidth = Math.max(0, starPixelWidth);
        }
    }
}

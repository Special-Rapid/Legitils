package com.snkisk.hypixellegitils.stats;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Keeps one local Tab name column that includes display text added by other client MODs. */
public final class StatsDisplayNameColumns {
    private static final int MAXIMUM_TRACKED_NAMES = 256;
    private final Map<String, String> activeRenderedNames = new LinkedHashMap<String, String>();
    private final Map<String, String> nextRenderedNames = new LinkedHashMap<String, String>();
    private int activeColumnWidth;
    private int nextColumnWidth;

    /** Starts a fresh Tab roster snapshot; the previous complete snapshot remains available to render it. */
    public synchronized void beginTabRender() {
        nextRenderedNames.clear();
        nextColumnWidth = 0;
    }

    /** Records this render's unmodified Tab text and pads it to the previous complete roster width. */
    public synchronized String observeTabName(String profileName, String renderedName) {
        if (isUsable(profileName, renderedName)) {
            if (nextRenderedNames.size() < MAXIMUM_TRACKED_NAMES || nextRenderedNames.containsKey(key(profileName))) {
                nextRenderedNames.put(key(profileName), renderedName);
            }
            int width = visibleLength(renderedName);
            if (width > nextColumnWidth) nextColumnWidth = width;
        }
        return spacesFor(activeColumnWidth, renderedName);
    }

    /** Publishes the complete current roster for the next Tab frame and for automatic Chat output. */
    public synchronized void finishTabRender() {
        activeRenderedNames.clear();
        activeRenderedNames.putAll(nextRenderedNames);
        activeColumnWidth = nextColumnWidth;
    }

    /** Returns the latest complete Tab field and its dynamic roster padding for automatic Chat. */
    public synchronized String nameForChat(String profileName, String fallbackName) {
        String rendered = profileName == null ? null : activeRenderedNames.get(key(profileName));
        if (!isUsable(profileName, rendered)) rendered = fallbackName;
        return rendered == null ? "" : rendered + spacesFor(activeColumnWidth, rendered);
    }

    public synchronized void clear() {
        activeRenderedNames.clear();
        nextRenderedNames.clear();
        activeColumnWidth = 0;
        nextColumnWidth = 0;
    }

    private static String spacesFor(int columnWidth, String renderedName) {
        int count = columnWidth - visibleLength(renderedName);
        if (count <= 0) return "";
        StringBuilder padding = new StringBuilder(count);
        for (int index = 0; index < count; index++) padding.append(' ');
        return padding.toString();
    }

    private static boolean isUsable(String profileName, String renderedName) {
        return profileName != null && renderedName != null && !renderedName.trim().isEmpty()
            && stripFormatting(renderedName).toLowerCase(Locale.ROOT).contains(profileName.toLowerCase(Locale.ROOT));
    }

    private static String key(String profileName) {
        return profileName.toLowerCase(Locale.ROOT);
    }

    private static int visibleLength(String text) {
        return stripFormatting(text == null ? "" : text).length();
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
}
